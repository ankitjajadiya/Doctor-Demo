package com.ayursh.android.network


import com.ayursh.android.network.responses.*
import com.ayursh.android.network.responses.auth.AuthResponse
import com.ayursh.android.network.responses.otp.OtpResponse
import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


interface MyApis {

    @Headers("Content-Type: application/json")
    @POST("onboarding/api/v1/doctor/fcm")
    fun Postfcm(
        @Header("Authorization") auth: String,
        @Body body: JsonObject?
    ): Call<FCMResponse>

    @GET("consultation/api/v1/doctor-profile")
    fun getProfile(
        @Header("Authorization") auth: String): Call<UserResponse>

    @Headers("Content-Type: application/json")
    @POST("onboarding/api/v1/doctor/login")
    fun login(
        @Body body: JsonObject?
    ): Call<AuthResponse>

    @Headers("Content-Type: application/json")
    @POST("onboarding/api/v1/doctor/register")
    fun signup(
        @Body body: JsonObject?
    ): Call<AuthResponse>

    @Headers("Content-Type: application/json")
    @POST("onboarding/api/v1/doctor/validate-token")
    fun validateOtp(
        @Body body: JsonObject?
    ): Call<OtpResponse>

    @Headers("Content-Type: application/json")
    @POST("onboarding/api/v1/fcm/send")
    fun sendnotification(
        @Header("Authorization") auth: String,
        @Body body: JsonObject
    ): Call<ResponseBody>

    @GET("consultation/api/v1/doctor/bookings/list")
    fun getMyBooking(
        @Header("Authorization") auth: String,
        @Query("consultation_date") date: String
    ): Call<MyBookingsResponse>

    @GET("consultation/api/v1/rtc/token")
    fun getRTCToken(
        @Header("Authorization") auth: String,
        @Query("channel_name") channel: String
    ): Call<RtcTokenResponse>

    @GET("consultation/api/v1/rtm/token")
    fun getRTMToken(
        @Header("Authorization") auth: String
    ): Call<RtcTokenResponse>

    @GET("therapy/api/v1/categories?fetch_all_therapies=true")
    fun getSpinnerCategories(
        @Header("Authorization") auth: String
    ): Call<TherapyResponse>

    @GET("therapy/api/v1/category/{therapy_category}")
    fun getTherapy(
        @Header("Authorization") auth: String,
        @Path("therapy_category") therapy_category: String
    ): Call<TherapyCategoryResponse>

    //@Headers({"Authorization: key="+"", "Content-Type:application/json"})
    @Headers("Content-Type: application/json")
    @POST("fcm/send")
    fun sendNotification(
        @Header("Authorization") auth: String,
        @Body root: JsonObject
    ): Call<ResponseBody>

    @Multipart
    @POST("consultation/api/v1/internal/upload-prescription?action=generate-prescription")
    fun generatePrescription(
        @Header("Authorization") auth: String,
        @Part("doctor_booking_id") booking_id: String,
        @Part("patient_name") patient_name: String,
        @Part("patient_age") patient_age: String,
        @Part("patient_gender") patient_gander: String,
        @Part("main_complaints") main_complaints: String,
        @Part("associated_complaints") associated_complaints: String,
        @Part("history_of_main_complaints") history_of_main_complaints: String,
        @Part("if_any_allergies") if_any_allergies: String,
        @Part("dosha_analysis") dosha_analysis: String,
        @Part("diagnosis") diagnosis: String,
        @Part("prescription") prescription: String,
        @Part("is_therapy_assigned") is_therapy_assigned: Boolean,
        @Part("is_consultation_completed") is_consultation_completed: Boolean,
        @Part("assigned_therapy_category") assigned_therapy_category: String,
        @Part("assigned_therapy_title") assigned_therapy_title: String,
        @Part("assigned_therapy_total_sessions") assigned_therapy_total_sessions: Int,
        @Part("user_fcm_token") user_fcm_token: String
    ): Call<ResponseBody>

    @Multipart
    @POST("consultation/api/v1/internal/upload-prescription?action=generate-prescription")
    fun generatePrescriptionWithoutTherapy(
        @Header("Authorization") auth: String,
        @Part("doctor_booking_id") booking_id: String,
        @Part("patient_name") patient_name: String,
        @Part("patient_age") patient_age: String,
        @Part("patient_gender") patient_gander: String,
        @Part("main_complaints") main_complaints: String,
        @Part("associated_complaints") associated_complaints: String,
        @Part("history_of_main_complaints") history_of_main_complaints: String,
        @Part("if_any_allergies") if_any_allergies: String,
        @Part("dosha_analysis") dosha_analysis: String,
        @Part("diagnosis") diagnosis: String,
        @Part("prescription") prescription: String,
        @Part("is_therapy_assigned") is_therapy_assigned: Boolean,
        @Part("is_consultation_completed") is_consultation_completed: Boolean,
        @Part("user_fcm_token") user_fcm_token: String //change this
    ): Call<ResponseBody>

    @Multipart
    @POST("consultation/api/v1/internal/upload-prescription?action=save-prescription")
    fun savePrescription(
        @Header("Authorization") auth: String,
        @Part("doctor_booking_id") doctor_booking_id: String,
        @Part("patient_name") patient_name: String,
        @Part("patient_age") patient_age: String,
        @Part("patient_gender") patient_gender: String,
        @Part("main_complaints") main_complaints: String,
        @Part("associated_complaints") associated_complaints: String,
        @Part("history_of_main_complaints") history_of_main_complaints: String,
        @Part("if_any_allergies") if_any_allergies: String,
        @Part("dosha_analysis") dosha_analysis: String,
        @Part("diagnosis") diagnosis: String,
        @Part("prescription") prescription: String,
        @Part("is_therapy_assigned") is_therapy_assigned: Boolean,
        @Part("is_consultation_completed") is_consultation_completed: Boolean,
        @Part("assigned_therapy_category") assigned_therapy_category: String,
        @Part("assigned_therapy_title") assigned_therapy_title: String,
        @Part("assigned_therapy_total_sessions") assigned_therapy_total_sessions: Int,
        @Part("user_fcm_token") user_fcm_token: String

    ): Call<SavePrescription>
    @Multipart
    @POST("consultation/api/v1/internal/upload-prescription?action=save-prescription")
    fun savePrescriptionWithoutTherapy(
        @Header("Authorization") auth: String,
        @Part("doctor_booking_id") doctor_booking_id: String,
        @Part("patient_name") patient_name: String,
        @Part("patient_age") patient_age: String,
        @Part("patient_gender") patient_gender: String,
        @Part("main_complaints") main_complaints: String,
        @Part("associated_complaints") associated_complaints: String,
        @Part("history_of_main_complaints") history_of_main_complaints: String,
        @Part("if_any_allergies") if_any_allergies: String,
        @Part("dosha_analysis") dosha_analysis: String,
        @Part("diagnosis") diagnosis: String,
        @Part("prescription") prescription: String,
        @Part("is_therapy_assigned") is_therapy_assigned: Boolean,
        @Part("is_consultation_completed") is_consultation_completed: Boolean,
        @Part("user_fcm_token") user_fcm_token: String


    ): Call<SavePrescription>

    @GET("/consultation/api/v1/doctor/booking/{id}")
    fun getBookingDetail(
        @Header("Authorization") auth: String,
        @Path("id") booking_id: String
    ): Call<MyBookingsResponse>

    @Streaming
    @GET("onboarding/api/v1/consultation/booking/{id}/prescription")
    fun getFile(
        @Header("Authorization") auth: String,
        @Path("id") bookingId: String
    ): Call<ResponseBody>
}