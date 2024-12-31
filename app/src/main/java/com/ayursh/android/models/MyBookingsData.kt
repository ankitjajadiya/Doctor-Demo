package com.ayursh.android.models

import java.io.Serializable

data class MyBookingsData(
    var booking_id: String,
    var doctor_id: String,
    var user_id: String,
    var user_consultation_booking_id: String,
    var doctor_display_image: String,
    var status: String,
    var booking_for_age: String,
    var booking_for_gender: String,
    var booking_for_name: String,
    var scheduled_at: Long,
    var scheduled_at_str: String,
    var consultation_fee_paid: String,
    var prescription: PrescriptionModel,
    var fcm_token: String
) : Serializable
