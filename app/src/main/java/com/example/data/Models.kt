package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deals")
data class Deal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val businessName: String,
    val category: String,
    val offer: String,
    val originalSpots: Int,
    val spotsRemaining: Int,
    val startTime: Long,
    val endTime: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val viewsCount: Int = 0,
    val claimsCount: Int = 0,
    val scansCount: Int = 0,
    val isCompleted: Boolean = false
)

@Entity(tableName = "claimed_deals")
data class ClaimedDeal(
    @PrimaryKey val id: String, // format: "CLAIM_{DEAL_ID}_{RANDOM_STRING}"
    val dealId: Int,
    val businessName: String,
    val offer: String,
    val category: String,
    val claimedAt: Long = System.currentTimeMillis(),
    val scanned: Boolean = false,
    val scannedAt: Long? = null,
    val qrCodeData: String
)

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1, // Single profile for the business owner side of the app
    val businessName: String = "",
    val category: String = "Restaurant",
    val address: String = "",
    val slowHours: String = "Tuesday 14:00-17:00, Thursday 14:00-17:00", // comma-separated
    val stripeConnected: Boolean = false,
    val hasCompletedSetup: Boolean = false,
    val weeklyPayoutEarned: Double = 0.0
)
