package com.formbuddy.android.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.formbuddy.android.data.local.db.entity.BusinessProfileEntity
import com.formbuddy.android.data.local.db.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE is_family = 0 LIMIT 1")
    fun getPersonalProfile(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE is_family = 1 ORDER BY created_at")
    fun getFamilyProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("SELECT * FROM business_profiles ORDER BY created_at")
    fun getBusinessProfiles(): Flow<List<BusinessProfileEntity>>

    @Query("SELECT * FROM business_profiles WHERE id = :id")
    suspend fun getBusinessProfileById(id: String): BusinessProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinessProfile(profile: BusinessProfileEntity)

    @Update
    suspend fun updateBusinessProfile(profile: BusinessProfileEntity)

    @Delete
    suspend fun deleteBusinessProfile(profile: BusinessProfileEntity)
}
