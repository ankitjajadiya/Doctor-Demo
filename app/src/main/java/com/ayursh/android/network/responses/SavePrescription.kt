package com.ayursh.android.network.responses

data class SavePrescription(
    val `data`: Boolean,
    val error_code: String,
    val message: String,
    val success: Boolean
)