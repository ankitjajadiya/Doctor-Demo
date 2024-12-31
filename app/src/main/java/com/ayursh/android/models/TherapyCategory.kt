package com.ayursh.android.models

import com.ayursh.android.network.responses.TherapyHeadlineMap

data class TherapyCategory(
    val therapies: List<String>,
    val therapy_categories: List<String>,
    val therapy_category_map: List<TherapyCategoryMap>,
    val therapy_headline_map: TherapyHeadlineMap,
    val therapy_headlines: List<String>
)