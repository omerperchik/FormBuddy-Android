package com.formbuddy.android.domain.analysis

import com.formbuddy.android.data.model.FieldSubType
import com.formbuddy.android.data.model.FieldType
import com.formbuddy.android.data.model.FormTemplate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FieldTypeInferencer @Inject constructor() {

    private val emailPatterns = listOf("email", "e-mail", "correo", "courriel", "メール", "البريد", "электронная")
    private val phonePatterns = listOf("phone", "tel", "mobile", "cell", "fax", "teléfono", "téléphone", "telefon", "電話", "هاتف", "телефон")
    private val datePatterns = listOf("date", "birth", "dob", "born", "fecha", "datum", "日付", "تاريخ", "дата", "nascimento")
    private val addressPatterns = listOf("address", "addr", "street", "dirección", "adresse", "住所", "عنوان", "адрес", "endereço")
    private val signaturePatterns = listOf("signature", "sign", "firma", "unterschrift", "署名", "توقيع", "подпись", "assinatura")

    private val firstNamePatterns = listOf("first name", "given name", "nombre", "prénom", "vorname", "名", "الاسم الأول", "имя")
    private val middleNamePatterns = listOf("middle name", "segundo nombre", "deuxième prénom", "中間名")
    private val lastNamePatterns = listOf("last name", "surname", "family name", "apellido", "nom de famille", "nachname", "姓", "اسم العائلة", "фамилия")
    private val cityPatterns = listOf("city", "ciudad", "ville", "stadt", "市", "مدينة", "город", "cidade")
    private val statePatterns = listOf("state", "province", "estado", "état", "staat", "県", "ولاية", "область")
    private val countryPatterns = listOf("country", "nation", "país", "pays", "land", "国", "بلد", "страна")
    private val postalCodePatterns = listOf("zip", "postal", "código postal", "code postal", "postleitzahl", "郵便番号", "الرمز البريدي", "почтовый индекс")
    private val companyPatterns = listOf("company", "business", "organization", "empresa", "entreprise", "firma", "会社", "شركة", "компания")
    private val taxIdPatterns = listOf("tax id", "tax number", "tin", "ssn", "ein", "nif", "steuer", "税番号", "رقم ضريبي", "инн")
    private val registrationPatterns = listOf("registration", "reg number", "número de registro", "numéro d'enregistrement", "registrierung")
    private val websitePatterns = listOf("website", "web", "url", "sitio web", "site web", "ウェブサイト", "موقع", "веб-сайт")
    private val industryPatterns = listOf("industry", "sector", "industria", "industrie", "業種", "صناعة", "отрасль")

    fun inferFieldTypes(template: FormTemplate): FormTemplate {
        template.allFields.forEach { field ->
            val label = field.label.lowercase().trim()

            // Infer field type
            if (field.fieldType == FieldType.TEXT) {
                field.fieldType = when {
                    emailPatterns.any { label.contains(it) } -> FieldType.EMAIL
                    phonePatterns.any { label.contains(it) } -> FieldType.PHONE
                    datePatterns.any { label.contains(it) } -> FieldType.DATE
                    addressPatterns.any { label.contains(it) } -> FieldType.ADDRESS
                    signaturePatterns.any { label.contains(it) } -> FieldType.SIGNATURE
                    else -> FieldType.TEXT
                }
            }

            // Infer sub-type
            field.fieldSubType = when {
                firstNamePatterns.any { label.contains(it) } -> FieldSubType.FIRST_NAME
                middleNamePatterns.any { label.contains(it) } -> FieldSubType.MIDDLE_NAME
                lastNamePatterns.any { label.contains(it) } -> FieldSubType.LAST_NAME
                datePatterns.any { p -> label.contains(p) && label.contains("birth") } -> FieldSubType.BIRTH_DATE
                cityPatterns.any { label.contains(it) } -> FieldSubType.CITY
                statePatterns.any { label.contains(it) } -> FieldSubType.STATE
                countryPatterns.any { label.contains(it) } -> FieldSubType.COUNTRY
                postalCodePatterns.any { label.contains(it) } -> FieldSubType.POSTAL_CODE
                companyPatterns.any { label.contains(it) } -> FieldSubType.COMPANY_NAME
                taxIdPatterns.any { label.contains(it) } -> FieldSubType.TAX_ID
                registrationPatterns.any { label.contains(it) } -> FieldSubType.REGISTRATION_NUMBER
                websitePatterns.any { label.contains(it) } -> FieldSubType.WEBSITE
                industryPatterns.any { label.contains(it) } -> FieldSubType.INDUSTRY
                label.contains("home") && addressPatterns.any { label.contains(it) } -> FieldSubType.HOME_ADDRESS
                label.contains("work") && addressPatterns.any { label.contains(it) } -> FieldSubType.WORK_ADDRESS
                else -> null
            }
        }
        return template
    }
}
