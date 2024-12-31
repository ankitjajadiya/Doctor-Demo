package com.ayursh.android.network.responses

import com.ayursh.android.models.TherapyCategory

data class TherapyResponse(
    val `data`: TherapyCategory,
    val error_code: String,
    val message: String,
    val success: Boolean
)