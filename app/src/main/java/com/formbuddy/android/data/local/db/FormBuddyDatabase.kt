package com.formbuddy.android.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.formbuddy.android.data.local.db.dao.FormDao
import com.formbuddy.android.data.local.db.dao.ProfileDao
import com.formbuddy.android.data.local.db.entity.BusinessProfileEntity
import com.formbuddy.android.data.local.db.entity.FormReferenceEntity
import com.formbuddy.android.data.local.db.entity.ProfileEntity

@Database(
    entities = [
        FormReferenceEntity::class,
        ProfileEntity::class,
        BusinessProfileEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FormBuddyDatabase : RoomDatabase() {
    abstract fun formDao(): FormDao
    abstract fun profileDao(): ProfileDao
}
