package com.ayursh.android.network.responses

import com.ayursh.android.models.MyBookingsData

data class MyBookingsResponse(var success: Boolean, var error_code: String, var message: String, var data: List<MyBookingsData>)
