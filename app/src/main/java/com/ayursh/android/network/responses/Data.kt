package com.ayursh.android.network.responses

data class Data(
    val category_id: String,
    val descriptions: List<String>,
    val faq: List<Faq>,
    val name: String,
    val therapies: List<Therapy>,
    val title: String,
    val why_this_therapy: WhyThisTherapy
)