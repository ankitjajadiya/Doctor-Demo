package com.ayursh.android.network.responses

import com.ayursh.android.models.FCMModel

data class FCMResponse(
    val `data`: FCMModel,
    val error_code: String,
    val message: String,
    val success: Boolean
)