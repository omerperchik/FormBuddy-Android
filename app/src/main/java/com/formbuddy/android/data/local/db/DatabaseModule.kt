package com.formbuddy.android.data.local.db

import android.content.Context
import androidx.room.Room
import com.formbuddy.android.data.local.db.dao.FormDao
import com.formbuddy.android.data.local.db.dao.ProfileDao
import com.formbuddy.android.data.local.encryption.EncryptionManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideEncryptionManager(@ApplicationContext context: Context): EncryptionManager {
        return EncryptionManager(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        encryptionManager: EncryptionManager
    ): FormBuddyDatabase {
        System.loadLibrary("sqlcipher")

        val passphrase = encryptionManager.getDatabasePassphrase()
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context,
            FormBuddyDatabase::class.java,
            "formbuddy.db"
        )
            .openHelperFactory(factory)
            // WAL mode: concurrent reads during writes, 2-4x faster queries
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFormDao(database: FormBuddyDatabase): FormDao = database.formDao()

    @Provides
    fun provideProfileDao(database: FormBuddyDatabase): ProfileDao = database.profileDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context = context
}
