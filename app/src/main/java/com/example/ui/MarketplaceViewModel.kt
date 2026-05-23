package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class MarketplaceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MarketplaceRepository

    // Current mock user location in New York, NY
    val userLatitude = 40.7128
    val userLongitude = -74.0060

    // Filter states
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _radiusKm = MutableStateFlow(2.5f)
    val radiusKm: StateFlow<Float> = _radiusKm.asStateFlow()

    // Active screen navigation (simulated layout toggle so user can easily play on both sides)
    private val _isMerchantMode = MutableStateFlow(false)
    val isMerchantMode: StateFlow<Boolean> = _isMerchantMode.asStateFlow()

    // Seconds ticker for countdown timers
    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    // Message system for notifications and feed alerts
    private val _alertMessages = MutableStateFlow<List<String>>(emptyList())
    val alertMessages: StateFlow<List<String>> = _alertMessages.asStateFlow()

    // Active claims state (for the modal QR code display or details review)
    private val _selectedClaimId = MutableStateFlow<String?>(null)
    val selectedClaimId: StateFlow<String?> = _selectedClaimId.asStateFlow()

    // Form inputs for Merchant setup and posting
    var setupNameState = ""
    var setupCategoryState = "Restaurant"
    var setupAddressState = "120 Broadway, New York, NY"
    var setupTimeSelection = mutableMapOf(
        "Monday" to false, "Tuesday" to true, "Wednesday" to false,
        "Thursday" to true, "Friday" to false, "Saturday" to false, "Sunday" to false
    )

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MarketplaceRepository(
            dealDao = database.dealDao(),
            claimedDealDao = database.claimedDealDao(),
            businessProfileDao = database.businessProfileDao(),
            applicationContext = application
        )

        // Launch real-time ticker
        viewModelScope.launch {
            while (true) {
                _currentTime.value = System.currentTimeMillis()
                delay(1000)
            }
        }

        // Periodically monitor database for new deals and issue user notifications
        viewModelScope.launch {
            var lastKnownDealsCount = 0
            repository.activeDeals.collect { deals ->
                if (deals.size > lastKnownDealsCount && lastKnownDealsCount > 0) {
                    val lastAdded = deals.firstOrNull()
                    if (lastAdded != null) {
                        pushAlert("New Flash Deal Live! ${lastAdded.businessName} posted format: '${lastAdded.offer}'")
                    }
                }
                lastKnownDealsCount = deals.size
            }
        }
    }

    // Expose filtered active deals based on category and distance
    val filteredDeals: StateFlow<List<Deal>> = combine(
        repository.activeDeals,
        _selectedCategory,
        _radiusKm
    ) { deals, category, radius ->
        deals.filter { deal ->
            val matchCategory = category == "All" || deal.category == category
            val distance = repository.getDistanceKm(
                userLatitude, userLongitude,
                deal.latitude, deal.longitude
            )
            val matchRadius = distance <= radius
            matchCategory && matchRadius
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val claimedDeals: StateFlow<List<ClaimedDeal>> = repository.claimedDeals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val businessProfile: StateFlow<BusinessProfile?> = repository.businessProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allDealsHistory: StateFlow<List<Deal>> = repository.allDeals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setRadius(radius: Float) {
        _radiusKm.value = radius
    }

    fun setMerchantMode(active: Boolean) {
        _isMerchantMode.value = active
    }

    fun setSelectedClaimId(claimId: String?) {
        _selectedClaimId.value = claimId
    }

    fun pushAlert(msg: String) {
        viewModelScope.launch {
            val updated = _alertMessages.value.toMutableList()
            updated.add(0, msg)
            if (updated.size > 5) updated.removeAt(updated.size - 1)
            _alertMessages.value = updated
        }
    }

    fun removeAlert(index: Int) {
        val updated = _alertMessages.value.toMutableList()
        if (index in updated.indices) {
            updated.removeAt(index)
            _alertMessages.value = updated
        }
    }

    // Actions
    fun claimDeal(dealId: Int, callback: (String) -> Unit) {
        viewModelScope.launch {
            val claimId = repository.claimDeal(dealId, userLatitude, userLongitude)
            if (claimId != null) {
                _selectedClaimId.value = claimId
                pushAlert("Success! Deal secured. Scan QR code at business to redeem!")
                callback(claimId)
            } else {
                pushAlert("Failed to claim: the spot was occupied or expired.")
            }
        }
    }

    fun completeMerchantSetup(name: String, category: String, address: String) {
        viewModelScope.launch {
            val commaSlowHours = setupTimeSelection.filter { it.value }.keys.joinToString(", ") { "$it 14:00-17:00" }
            val profile = BusinessProfile(
                id = 1,
                businessName = name.ifBlank { "Noodle Town" },
                category = category,
                address = address.ifBlank { "120 Broadway, New York, NY" },
                slowHours = commaSlowHours.ifBlank { "Tuesday 14:00-17:00" },
                stripeConnected = true, // Auto-connect bank link
                hasCompletedSetup = true,
                weeklyPayoutEarned = 0.0
            )
            repository.saveBusinessProfile(profile)
            pushAlert("Welcome setup! Your slow hours are set to alert you on slow periods.")
        }
    }

    fun postMerchantDeal(offer: String, spotsStr: String, durationStr: String) {
        viewModelScope.launch {
            val spots = spotsStr.toIntOrNull() ?: 8
            val durationHours = durationStr.toDoubleOrNull() ?: 2.0
            val rowId = repository.postDeal(offer, spots, durationHours)
            if (rowId > 0) {
                pushAlert("Flash Deal is Live! ${spots} spots available for nearby consumers.")
            } else {
                pushAlert("Error posting deal. Please finalize setup or verify Stripe state.")
            }
        }
    }

    fun redeemCode(claimId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.redeemClaim(claimId)
            if (success) {
                pushAlert("Redemption Validated! Stripe balance updated successfully (+ $38.20)")
                callback(true)
            } else {
                pushAlert("Redemption Failed: Code is invalid or already scanned.")
                callback(false)
            }
        }
    }

    fun disconnectStripe() {
        viewModelScope.launch {
            val current = repository.businessProfile.first()
            if (current != null) {
                repository.saveBusinessProfile(
                    current.copy(stripeConnected = false, hasCompletedSetup = false)
                )
            }
        }
    }

    // Helper functions for displaying remaining durations
    fun getFormattedDistance(deal: Deal): String {
        val dist = repository.getDistanceKm(userLatitude, userLongitude, deal.latitude, deal.longitude)
        return String.format("%.1f km away", dist)
    }
}
