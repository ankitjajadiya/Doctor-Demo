package com.ayursh.android.network.responses

data class TherapyCategoryResponse(
    val `data`: List<Data>,
    val error_code: String,
    val message: String,
    val success: Boolean
)