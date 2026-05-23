package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DealDao {
    @Query("SELECT * FROM deals WHERE isCompleted = 0 AND endTime > :nowMilli ORDER BY createdAt DESC")
    fun getActiveDeals(nowMilli: Long): Flow<List<Deal>>

    @Query("SELECT * FROM deals ORDER BY createdAt DESC")
    fun getAllDeals(): Flow<List<Deal>>

    @Query("SELECT * FROM deals WHERE id = :id")
    suspend fun getDealById(id: Int): Deal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeal(deal: Deal): Long

    @Update
    suspend fun updateDeal(deal: Deal)

    @Query("UPDATE deals SET spotsRemaining = spotsRemaining - 1, claimsCount = claimsCount + 1 WHERE id = :dealId AND spotsRemaining > 0")
    suspend fun decrementSpots(dealId: Int): Int
}

@Dao
interface ClaimedDealDao {
    @Query("SELECT * FROM claimed_deals ORDER BY claimedAt DESC")
    fun getAllClaimedDeals(): Flow<List<ClaimedDeal>>

    @Query("SELECT * FROM claimed_deals WHERE id = :id")
    suspend fun getClaimedDealById(id: String): ClaimedDeal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaimedDeal(claimedDeal: ClaimedDeal)

    @Update
    suspend fun updateClaimedDeal(claimedDeal: ClaimedDeal)
}

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = 1")
    fun getBusinessProfileFlow(): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profile WHERE id = 1")
    suspend fun getBusinessProfile(): BusinessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: BusinessProfile)
}
