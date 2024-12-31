package com.ayursh.android.network.responses


import com.ayursh.android.models.UserModel

data class UserResponse(
    val data: UserModel,
    val error_code: String,
    val message: String,
    val success: Boolean
)
