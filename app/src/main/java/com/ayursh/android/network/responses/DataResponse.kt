package com.ayursh.android.network.responses

data class DataResponse (
    var success: Boolean, var error_code: String, var message: String, var data: String
        )