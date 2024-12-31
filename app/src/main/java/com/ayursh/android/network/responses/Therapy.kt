package com.ayursh.android.network.responses

data class Therapy(
    val english_name: String,
    val headline: String,
    val image_url: String,
    val name: String,
    val session_duration_in_min: Int,
    val session_options: List<Int>,
    val title: String
)