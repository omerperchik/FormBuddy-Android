package com.formbuddy.android.data.model

import com.formbuddy.android.data.local.db.entity.BusinessProfileEntity
import com.formbuddy.android.data.local.db.entity.ProfileEntity

sealed class Profile {
    data class Personal(val entity: ProfileEntity) : Profile()
    data class Family(val entity: ProfileEntity) : Profile()
    data class Business(val entity: BusinessProfileEntity) : Profile()

    val displayName: String
        get() = when (this) {
            is Personal -> "${entity.firstName} ${entity.lastName}".trim().ifBlank { "Personal" }
            is Family -> "${entity.firstName} ${entity.lastName}".trim().ifBlank { "Family Member" }
            is Business -> entity.companyName.ifBlank { "Business" }
        }

    val isPrivate: Boolean
        get() = when (this) {
            is Personal -> entity.isPrivate
            is Family -> entity.isPrivate
            is Business -> entity.isPrivate
        }

    fun suggestedValue(field: FormField): String? {
        return when (this) {
            is Personal, is Family -> {
                val e = if (this is Personal) entity else (this as Family).entity
                when (field.fieldSubType) {
                    FieldSubType.FIRST_NAME -> e.firstName.takeIf { it.isNotBlank() }
                    FieldSubType.MIDDLE_NAME -> e.middleName?.takeIf { it.isNotBlank() }
                    FieldSubType.LAST_NAME -> e.lastName.takeIf { it.isNotBlank() }
                    FieldSubType.BIRTH_DATE -> e.birthDate
                    FieldSubType.HOME_ADDRESS -> e.homeAddress?.takeIf { it.isNotBlank() }
                    FieldSubType.WORK_ADDRESS -> e.workAddress?.takeIf { it.isNotBlank() }
                    FieldSubType.CITY -> e.city?.takeIf { it.isNotBlank() }
                    FieldSubType.STATE -> e.state?.takeIf { it.isNotBlank() }
                    FieldSubType.COUNTRY -> e.country?.takeIf { it.isNotBlank() }
                    FieldSubType.POSTAL_CODE -> e.postalCode?.takeIf { it.isNotBlank() }
                    else -> when (field.fieldType) {
                        FieldType.EMAIL -> e.email?.takeIf { it.isNotBlank() }
                        FieldType.PHONE -> e.phone?.takeIf { it.isNotBlank() }
                        FieldType.SIGNATURE -> e.signaturePath?.takeIf { it.isNotBlank() }
                        else -> null
                    }
                }
            }
            is Business -> {
                when (field.fieldSubType) {
                    FieldSubType.COMPANY_NAME -> entity.companyName.takeIf { it.isNotBlank() }
                    FieldSubType.TAX_ID -> entity.taxId?.takeIf { it.isNotBlank() }
                    FieldSubType.REGISTRATION_NUMBER -> entity.registrationNumber?.takeIf { it.isNotBlank() }
                    FieldSubType.WEBSITE -> entity.website?.takeIf { it.isNotBlank() }
                    FieldSubType.INDUSTRY -> entity.industry?.takeIf { it.isNotBlank() }
                    else -> when (field.fieldType) {
                        FieldType.EMAIL -> entity.businessEmail?.takeIf { it.isNotBlank() }
                        FieldType.PHONE -> entity.businessPhone?.takeIf { it.isNotBlank() }
                        FieldType.ADDRESS -> entity.businessAddress?.takeIf { it.isNotBlank() }
                        else -> null
                    }
                }
            }
        }
    }
}

enum class FormMode {
    CHAT, VOICE, AGENT
}
