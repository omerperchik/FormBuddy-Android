package com.formbuddy.android.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "business_profiles")
data class BusinessProfileEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "company_name")
    val companyName: String = "",

    @ColumnInfo(name = "tax_id")
    val taxId: String? = null,

    @ColumnInfo(name = "registration_number")
    val registrationNumber: String? = null,

    @ColumnInfo(name = "business_address")
    val businessAddress: String? = null,

    @ColumnInfo(name = "business_phone")
    val businessPhone: String? = null,

    @ColumnInfo(name = "business_email")
    val businessEmail: String? = null,

    val website: String? = null,
    val industry: String? = null,

    @ColumnInfo(name = "is_private")
    val isPrivate: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long = System.currentTimeMillis()
) {
    val completionPercentage: Float
        get() {
            val fields = listOf(companyName, taxId, registrationNumber, businessAddress, businessPhone, businessEmail, website, industry)
            val filled = fields.count { !it.isNullOrBlank() }
            return filled.toFloat() / fields.size
        }
}
