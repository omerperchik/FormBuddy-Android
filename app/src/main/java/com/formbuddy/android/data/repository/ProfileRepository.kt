package com.formbuddy.android.data.repository

import com.formbuddy.android.data.local.db.dao.ProfileDao
import com.formbuddy.android.data.local.db.entity.BusinessProfileEntity
import com.formbuddy.android.data.local.db.entity.ProfileEntity
import com.formbuddy.android.data.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileDao: ProfileDao
) {
    fun getPersonalProfile(): Flow<ProfileEntity?> = profileDao.getPersonalProfile()
    fun getFamilyProfiles(): Flow<List<ProfileEntity>> = profileDao.getFamilyProfiles()
    fun getBusinessProfiles(): Flow<List<BusinessProfileEntity>> = profileDao.getBusinessProfiles()

    fun getAllProfiles(): Flow<List<Profile>> = combine(
        getPersonalProfile(),
        getFamilyProfiles(),
        getBusinessProfiles()
    ) { personal, family, business ->
        buildList {
            personal?.let { add(Profile.Personal(it)) }
            family.forEach { add(Profile.Family(it)) }
            business.forEach { add(Profile.Business(it)) }
        }
    }

    suspend fun getProfileById(id: String): ProfileEntity? = profileDao.getProfileById(id)
    suspend fun getBusinessProfileById(id: String): BusinessProfileEntity? = profileDao.getBusinessProfileById(id)

    suspend fun createPersonalProfile(): ProfileEntity {
        val profile = ProfileEntity(isFamily = false)
        profileDao.insertProfile(profile)
        return profile
    }

    suspend fun saveProfile(profile: ProfileEntity) {
        profileDao.insertProfile(profile.copy(modifiedAt = System.currentTimeMillis()))
    }

    suspend fun saveBusinessProfile(profile: BusinessProfileEntity) {
        profileDao.insertBusinessProfile(profile.copy(modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProfile(profile: ProfileEntity) = profileDao.deleteProfile(profile)
    suspend fun deleteBusinessProfile(profile: BusinessProfileEntity) = profileDao.deleteBusinessProfile(profile)

    suspend fun createFamilyProfile(): ProfileEntity {
        val profile = ProfileEntity(isFamily = true)
        profileDao.insertProfile(profile)
        return profile
    }

    suspend fun createBusinessProfile(): BusinessProfileEntity {
        val profile = BusinessProfileEntity()
        profileDao.insertBusinessProfile(profile)
        return profile
    }
}
