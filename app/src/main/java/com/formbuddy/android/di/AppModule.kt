package com.formbuddy.android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * App-wide bindings live here. The vast majority of services in this project use
 * `@Inject constructor` + `@Singleton`, so Hilt can construct them automatically and
 * we don't need to repeat them as `@Provides`. Add bindings here only when:
 *  - the type lives in a third-party library (no source-level `@Inject`), or
 *  - construction needs custom logic that can't be expressed in a constructor.
 *
 * Database/encryption bindings live in [com.formbuddy.android.data.local.db.DatabaseModule].
 * Billing bindings live in [BillingModule].
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
