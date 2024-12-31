package com.ayursh.android.models

import java.io.Serializable

data class PrescriptionModel(
    var doctor_booking_id:String,
    var patient_name: String,
    var patient_age: String,
    var patient_gender: String,
    var main_complaints: String,
    var associated_complaints: String,
    var history_of_main_complaints: String,
    var if_any_allergies: String,
    var dosha_analysis: String,
    var diagnosis: String,
    var prescription: String,
    var prescription_url: String,
    var is_therapy_assigned: Boolean,
    var is_consultation_completed: Boolean,
    var assigned_therapy_category: String,
    var assigned_therapy_title: String,
    var assigned_therapy_total_sessions: Int,
    var fcm_token: String

) : Serializable