package com.formbuddy.android.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "first_name")
    val firstName: String = "",

    @ColumnInfo(name = "middle_name")
    val middleName: String? = null,

    @ColumnInfo(name = "last_name")
    val lastName: String = "",

    val email: String? = null,
    val phone: String? = null,

    @ColumnInfo(name = "birth_date")
    val birthDate: String? = null,

    @ColumnInfo(name = "home_address")
    val homeAddress: String? = null,

    @ColumnInfo(name = "work_address")
    val workAddress: String? = null,

    val city: String? = null,
    val state: String? = null,
    val country: String? = null,

    @ColumnInfo(name = "postal_code")
    val postalCode: String? = null,

    @ColumnInfo(name = "signature_path")
    val signaturePath: String? = null,

    @ColumnInfo(name = "is_private")
    val isPrivate: Boolean = false,

    @ColumnInfo(name = "is_family")
    val isFamily: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "modified_at")
    val modifiedAt: Long = System.currentTimeMillis()
) {
    val completionPercentage: Float
        get() {
            val fields = listOf(firstName, lastName, email, phone, birthDate, homeAddress, city, state, country, postalCode)
            val filled = fields.count { !it.isNullOrBlank() }
            return filled.toFloat() / fields.size
        }
}
