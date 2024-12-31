package com.ayursh.android.network.responses.auth

data class AuthResponse(var success: Boolean, var error_code: String, var message: String, var data: AuthDataResponse)
