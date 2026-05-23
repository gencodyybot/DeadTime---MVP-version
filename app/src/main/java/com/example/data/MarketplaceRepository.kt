package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlin.math.*
import kotlin.random.Random

class MarketplaceRepository(
    private val dealDao: DealDao,
    private val claimedDealDao: ClaimedDealDao,
    private val businessProfileDao: BusinessProfileDao,
    private val applicationContext: Context
) {
    val activeDeals: Flow<List<Deal>>
        get() = dealDao.getActiveDeals(System.currentTimeMillis())

    val allDeals: Flow<List<Deal>>
        get() = dealDao.getAllDeals()

    val claimedDeals: Flow<List<ClaimedDeal>>
        get() = claimedDealDao.getAllClaimedDeals()

    val businessProfile: Flow<BusinessProfile?>
        get() = businessProfileDao.getBusinessProfileFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        repositoryScope.launch {
            // Seed profile and mock deals if empty
            initializeProfile()
            initializeMockDeals()
            startSimulationLoop()
        }
    }

    private suspend fun initializeProfile() {
        val current = businessProfileDao.getBusinessProfile()
        if (current == null) {
            val defaultProfile = BusinessProfile(
                id = 1,
                businessName = "",
                category = "Restaurant",
                address = "",
                slowHours = "Tuesday 14:00-17:00, Thursday 14:00-17:00",
                stripeConnected = false,
                hasCompletedSetup = false,
                weeklyPayoutEarned = 0.0
            )
            businessProfileDao.insertProfile(defaultProfile)
        }
    }

    private suspend fun initializeMockDeals() {
        val existing = dealDao.getActiveDeals(System.currentTimeMillis()).first()
        if (existing.isEmpty()) {
            val now = System.currentTimeMillis()
            val seedDeals = listOf(
                Deal(
                    businessName = "Tony's Pizzeria",
                    category = "Restaurant",
                    offer = "35% off any large artisanal pizza",
                    originalSpots = 12,
                    spotsRemaining = 8,
                    startTime = now,
                    endTime = now + 94 * 60 * 1000, // 1h 34m
                    latitude = 40.7138,
                    longitude = -74.0080,
                    address = "124 Lafayette St, New York, NY",
                    viewsCount = 42,
                    claimsCount = 4,
                    scansCount = 2
                ),
                Deal(
                    businessName = "Glow Nail Spa",
                    category = "Nail Studio",
                    offer = "30% off premium gel manicures",
                    originalSpots = 6,
                    spotsRemaining = 3,
                    startTime = now,
                    endTime = now + 54 * 60 * 1000, // 54m
                    latitude = 40.7102,
                    longitude = -74.0031,
                    address = "45 Broad St, New York, NY",
                    viewsCount = 28,
                    claimsCount = 3,
                    scansCount = 1
                ),
                Deal(
                    businessName = "Zen Yoga Studio",
                    category = "Yoga Studio",
                    offer = "50% off mid-day restorative flow",
                    originalSpots = 15,
                    spotsRemaining = 5,
                    startTime = now,
                    endTime = now + 145 * 60 * 1000, // 2h 25m
                    latitude = 40.7156,
                    longitude = -74.0095,
                    address = "98 Franklin St, New York, NY",
                    viewsCount = 76,
                    claimsCount = 10,
                    scansCount = 6
                ),
                Deal(
                    businessName = "Classic Barber Shop",
                    category = "Barbershop",
                    offer = "25% off styling haircut + hot towel shave",
                    originalSpots = 8,
                    spotsRemaining = 4,
                    startTime = now,
                    endTime = now + 72 * 60 * 1000, // 1h 12m
                    latitude = 40.7082,
                    longitude = -74.0048,
                    address = "12 William St, New York, NY",
                    viewsCount = 35,
                    claimsCount = 4,
                    scansCount = 3
                ),
                Deal(
                    businessName = "Iron Gym",
                    category = "Gym",
                    offer = "40% off daily trainer pass + protein shake",
                    originalSpots = 10,
                    spotsRemaining = 2,
                    startTime = now,
                    endTime = now + 41 * 60 * 1000, // 41m
                    latitude = 40.7118,
                    longitude = -74.0010,
                    address = "21 Maiden Ln, New York, NY",
                    viewsCount = 59,
                    claimsCount = 8,
                    scansCount = 5
                )
            )
            for (deal in seedDeals) {
                dealDao.insertDeal(deal)
            }
        }
    }

    suspend fun saveBusinessProfile(profile: BusinessProfile) {
        businessProfileDao.insertProfile(profile)
    }

    suspend fun postDeal(offer: String, spots: Int, durationHours: Double): Long {
        val profile = businessProfileDao.getBusinessProfile() ?: return -1L
        if (!profile.hasCompletedSetup) return -2L

        val now = System.currentTimeMillis()
        val durationMs = (durationHours * 60 * 60 * 1000).toLong()

        // Place business at a centered coordinate
        val newDeal = Deal(
            businessName = profile.businessName,
            category = profile.category,
            offer = offer,
            originalSpots = spots,
            spotsRemaining = spots,
            startTime = now,
            endTime = now + durationMs,
            latitude = 40.7128,
            longitude = -74.0060,
            address = profile.address,
            viewsCount = 0,
            claimsCount = 0,
            scansCount = 0
        )
        return dealDao.insertDeal(newDeal)
    }

    suspend fun claimDeal(dealId: Int, userLat: Double, userLng: Double): String? {
        val deal = dealDao.getDealById(dealId) ?: return null
        if (deal.spotsRemaining <= 0) return null

        val updatedRows = dealDao.decrementSpots(dealId)
        if (updatedRows > 0) {
            val randomSuffix = Random.nextInt(100, 999)
            val claimId = "CLAIM_${dealId}_$randomSuffix"
            val claimedDeal = ClaimedDeal(
                id = claimId,
                dealId = dealId,
                businessName = deal.businessName,
                offer = deal.offer,
                category = deal.category,
                claimedAt = System.currentTimeMillis(),
                scanned = false,
                qrCodeData = claimId
            )
            claimedDealDao.insertClaimedDeal(claimedDeal)
            return claimId
        }
        return null
    }

    suspend fun redeemClaim(claimId: String): Boolean {
        val claimed = claimedDealDao.getClaimedDealById(claimId) ?: return false
        if (claimed.scanned) return false // Already scanned

        // Mark claimed deal as scanned
        val updatedClaim = claimed.copy(
            scanned = true,
            scannedAt = System.currentTimeMillis()
        )
        claimedDealDao.insertClaimedDeal(updatedClaim)

        // Increment scan count on the original deal
        val deal = dealDao.getDealById(claimed.dealId)
        if (deal != null) {
            val updatedDeal = deal.copy(scansCount = deal.scansCount + 1)
            dealDao.updateDeal(updatedDeal)
        }

        // Add 4.5% commission fee handling Simulation
        // A deal redemed generates revenue for the platform & earnings for the business. Let's assume a typical service is valued at $40.
        // Business payout: $40 - 4.5% = $38.20. So we increment the business balance!
        val profile = businessProfileDao.getBusinessProfile()
        if (profile != null) {
            val payoutIncrement = 38.20
            val updatedProfile = profile.copy(
                weeklyPayoutEarned = profile.weeklyPayoutEarned + payoutIncrement
            )
            businessProfileDao.insertProfile(updatedProfile)
        }

        return true
    }

    fun startSimulationLoop() {
        repositoryScope.launch {
            while (isActive) {
                delay(12000) // update metrics every 12 seconds
                val now = System.currentTimeMillis()
                val deals = dealDao.getActiveDeals(now).first()

                for (deal in deals) {
                    var updated = false
                    var viewCount = deal.viewsCount
                    var spotRemaining = deal.spotsRemaining
                    var claimsCount = deal.claimsCount

                    // 1. Simulating occasional browser/view updates
                    if (Random.nextFloat() < 0.4f) {
                        viewCount += Random.nextInt(1, 3)
                        updated = true
                    }

                    // 2. Simulating other random consumers in the area claiming a deal
                    // Higher chances if spotsRemaining is high and views are dropping
                    if (spotRemaining > 1 && Random.nextFloat() < 0.15f) {
                        spotRemaining -= 1
                        claimsCount += 1
                        updated = true
                    }

                    // If changed, persist in database
                    if (updated) {
                        dealDao.updateDeal(
                            deal.copy(
                                viewsCount = viewCount,
                                spotsRemaining = spotRemaining,
                                claimsCount = claimsCount
                            )
                        )
                    }
                }

                // 3. Occasionally generate a NEW third-party fast deal to simulate actual live neighborhood vibe!
                // Keep total active deals around 5-7 max to prevent overflow
                if (deals.size < 6 && Random.nextFloat() < 0.1f) {
                    val randomStoreNames = listOf(
                        "Sweat & Flow Gyms" to Pair("Gym", "45% off open gym access slot"),
                        "Bloom Hair Studio" to Pair("Hair Salon", "35% off express blowout package"),
                        "Noodle Bowl" to Pair("Restaurant", "30% off hot ramen ramen selection"),
                        "Velvet Nails" to Pair("Nail Studio", "25% off basic pedicure set"),
                        "Core Pilates" to Pair("Gym", "50% off intro private class")
                    )
                    val chosen = randomStoreNames.random()
                    val durationMin = Random.nextInt(45, 120)
                    val originalSpots = Random.nextInt(5, 15)

                    // Pick random offset around our NY coordinate (within 1km radius limit)
                    // 1 degree lat is ~111km. 1km is ~0.009 degree.
                    val offsetLat = (Random.nextDouble() - 0.5) * 0.012
                    val offsetLng = (Random.nextDouble() - 0.5) * 0.012

                    val newDeal = Deal(
                        businessName = chosen.first,
                        category = chosen.second.first,
                        offer = chosen.second.second,
                        originalSpots = originalSpots,
                        spotsRemaining = originalSpots - Random.nextInt(0, 3),
                        startTime = now,
                        endTime = now + durationMin * 60 * 1000,
                        latitude = 40.7128 + offsetLat,
                        longitude = -74.0060 + offsetLng,
                        address = "Near Chambers St, New York, NY",
                        viewsCount = Random.nextInt(5, 20),
                        claimsCount = Random.nextInt(0, 3),
                        scansCount = 0
                    )
                    dealDao.insertDeal(newDeal)
                }
            }
        }
    }

    // Haversine formula to compute actual local walking distance in kilometers
    fun getDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // earth radius
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
