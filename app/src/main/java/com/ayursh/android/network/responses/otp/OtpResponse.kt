package com.ayursh.android.network.responses.otp

data class OtpResponse(var success: Boolean, var error_code: String, var message: String, var data: OtpDataResponse)
