package com.ayursh.android.network.responses

data class GeneratePrescription(
    val `data`: String,
    val error_code: String,
    val message: String,
    val success: Boolean
)