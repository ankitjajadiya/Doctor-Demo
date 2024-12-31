package com.ayursh.android.models

data class UserModel(
    val phone_number: String,
    val first_name: String,
    val last_name: String,
    val display_name: String,
    val gender: String,
    val experience_in_months: Int,
    val consultation_fee: Int,
    val is_clinic_associated: Boolean,
    val email: String,
    val dob: String,
    val address: String,
//    val registration_number: Long,
    val qualification: String,
    val is_active: Boolean,
    val internal: Boolean,
    val doctor_id: String
)
