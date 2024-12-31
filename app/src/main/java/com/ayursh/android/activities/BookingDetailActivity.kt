package com.ayursh.android.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ayursh.android.R
import com.ayursh.android.activities.agora.ChatActivity
import com.ayursh.android.databinding.ActivityBookingDetailBinding
import com.ayursh.android.models.MyBookingsData
import com.ayursh.android.network.RetrofitClient
import com.ayursh.android.network.responses.MyBookingsResponse
import com.ayursh.android.network.responses.RtcTokenResponse
import com.ayursh.android.utils.*
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


private const val TAG = "BookingDetailActivity"

class BookingDetailActivity : AppCompatActivity() {

    private lateinit var binder: ActivityBookingDetailBinding
    private var bookingData: MyBookingsData? = null
    private var bookingId: String = ""
    private var status: String = ""



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder = DataBindingUtil.setContentView(this, R.layout.activity_booking_detail)
        init()
    }

    override fun onResume() {
        super.onResume()
        getBookingDetail()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun init() {
        initElements()
        getBookingDetail()
        initListeners()

    }

    private fun initElements() {
        bookingId = intent.getStringExtra("booking_id").toString()
        status=intent.getStringExtra("status").toString()
        if(status=="Completed"){
            binder.audCallBtn.visibility=View.GONE
            binder.vidCallBtn.visibility=View.GONE
        }
        else{

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initListeners() {
        binder.backBtn.setOnClickListener { finish() }
        binder.audCallBtn.setOnSingleClickListener { initAudioCall() }

        binder.vidCallBtn.setOnSingleClickListener { initVideoCall() }
       // binder.audCallBtn.setOnClickListener { initAudioCall() }

        binder.btnChat.setOnClickListener {
            Log.e(TAG, "initListeners: " + bookingData)
            startActivity(
                Intent(this, ChatActivity::class.java)
                    .putExtra("booking", bookingData)
            )
        }
        binder.presc.setOnClickListener {
            startActivity(
                Intent(this, Prescription::class.java)
                    .putExtra("booking_id", bookingData?.booking_id)
                    .putExtra("Prescription Model", bookingData?.prescription)
                    .putExtra("user_fcm_token", bookingData?.fcm_token)
            )
        }
    }

    private fun getBookingDetail() {
        binder.bookingDetailLay.visibility = View.GONE
        if (checkInternetConnection()) {
            binder.progress.visibility = View.VISIBLE
            RetrofitClient.create(this)
                .getBookingDetail("Bearer ${SharedPref.User.AUTH_TOKEN}", bookingId)
                .enqueue(object : Callback<MyBookingsResponse> {
                    override fun onResponse(call: Call<MyBookingsResponse>, response: Response<MyBookingsResponse>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            if (res.success) {
                                Log.e(TAG, "onResponse: ${res.data[0]}")
                                bookingData = res.data[0]
                                setData()
                            } else {
                                showToast(res.message)
                            }
                        } else {
                            showToast(response.errorBody()?.string().toString())
                        }
                        binder.progress.visibility = View.GONE
                    }

                    override fun onFailure(call: Call<MyBookingsResponse>, t: Throwable) {
                        Log.e(TAG, "onFailure(getBookingDetail): ${t.localizedMessage}")
                        binder.progress.visibility = View.GONE
                    }

                })
        }
    }


    private fun setData() {
        binder.bookingDetailLay.visibility = View.VISIBLE
        if (bookingData?.prescription != null) {
            binder.bookingForName.text = bookingData?.prescription?.patient_name
            binder.bookingForGenderAge.text = "${bookingData?.prescription?.patient_gender}, ${bookingData?.prescription?.patient_age} yrs "
        } else {
            binder.bookingForName.text = bookingData?.booking_for_name
            binder.bookingForGenderAge.text = "${bookingData?.booking_for_gender}, ${bookingData?.booking_for_age} yrs "
        }
        binder.scheduleAt.text = bookingData?.scheduled_at_str
        binder.amountPaid.text = "Rs. ${bookingData?.consultation_fee_paid}"


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initVideoCall() {

        val invite = JsonObject()
        Log.e(TAG, "initVideoCall: " + SharedPref.User.fcm_token)

        invite.addProperty("title", "Video Call")
        invite.addProperty("channel", bookingData?.user_consultation_booking_id)
        invite.addProperty("doc_name", SharedPref.User.USER.display_name)
        invite.addProperty("doc_id", bookingData?.doctor_id)
        invite.addProperty("doc_image", bookingData?.doctor_display_image)
        invite.addProperty("booking_id", bookingData?.booking_id)
        invite.addProperty("type", "remoteinvitationrecieved")
        invite.addProperty("fcm", SharedPref.User.fcm_token)
        invite.addProperty("time", java.time.Instant.now().getEpochSecond())

        Log.e(TAG, "initVideoCall: " + SystemClock.elapsedRealtime() + " " + SystemClock.uptimeMillis() + " " + SystemClock.currentThreadTimeMillis())
        if (checkInternetConnection()) {
            showProgress()


        RetrofitClient
                .create(this)
                .getRTCToken("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}", bookingData?.user_consultation_booking_id.toString())
                .enqueue(object : Callback<RtcTokenResponse> {
                    override fun onResponse(call: Call<RtcTokenResponse>, response: Response<RtcTokenResponse>) {

                        if (response.isSuccessful) {
                            val res = response.body()
                            if (res?.success == true) {
                                SendLocalInvitation(this@BookingDetailActivity, "Video Call", bookingData, res, invite)

                            } else {
                                showToast(res?.message.toString())
                            }

                        } else {
                            showToast(response.errorBody()
                                ?.string().toString())
                        }
                        dismissProgress()
                    }

                    override fun onFailure(call: Call<RtcTokenResponse>, t: Throwable) {
                        Log.e(TAG, "onFailure: ${t.localizedMessage}")
                        dismissProgress()
                        showToast("Fail Error: ${t.localizedMessage}")
                    }

                })

        }

    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SimpleDateFormat")
    private fun initAudioCall() {
        val invite = JsonObject()


        invite.addProperty("title", "Audio Call")
        invite.addProperty("channel", bookingData?.user_consultation_booking_id)
        invite.addProperty("doc_name", SharedPref.User.USER.display_name)
        invite.addProperty("doc_id", bookingData?.doctor_id)
        invite.addProperty("doc_image", bookingData?.doctor_display_image)
        invite.addProperty("booking_id", bookingData?.booking_id)
        invite.addProperty("type", "remoteinvitationrecieved")
        invite.addProperty("fcm", SharedPref.User.fcm_token)
        invite.addProperty("time", java.time.Instant.now().getEpochSecond())

        Log.e(TAG, "initAudioCall: " + invite)
        Restarter.incoming=false
        if (checkInternetConnection()) {
            showProgress()
            Log.e(TAG, "initAudioCall: ${bookingData?.user_consultation_booking_id}")
            val currentDateAndTime: String = SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Date())
            Log.e(TAG, "initAudioCall: " + currentDateAndTime + " " + currentDateAndTime.takeLast(2) + " " + java.time.Instant.now()
                .getEpochSecond())

            RetrofitClient
                .create(this)
                .getRTCToken("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}", bookingData?.user_consultation_booking_id.toString())
                .enqueue(object : Callback<RtcTokenResponse> {
                    override fun onResponse(call: Call<RtcTokenResponse>, response: Response<RtcTokenResponse>) {

                        if (response.isSuccessful) {
                            val res = response.body()
                            if (res?.success == true) {
                                Log.e(TAG, "onResponse: Audio Call " + bookingData)

                                SendLocalInvitation(this@BookingDetailActivity, "Audio Call", bookingData, res, invite)  //send json


                            } else {
                                showToast(res?.message.toString())
                            }

                        } else {
                            showToast(response.errorBody()
                                ?.string().toString())
                        }
                        dismissProgress()
                    }

                    override fun onFailure(call: Call<RtcTokenResponse>, t: Throwable) {
                        Log.e(TAG, "onFailure: ${t.localizedMessage}")
                        dismissProgress()
                        showToast("Fail Error: ${t.localizedMessage}")
                    }
                })
        }
    }



}

class OnSingleClickListener(private val block: () -> Unit) : View.OnClickListener {

    private var lastClickTime = 0L

    override fun onClick(view: View) {
        if (SystemClock.elapsedRealtime() - lastClickTime < 2000) {
            return
        }
        lastClickTime = SystemClock.elapsedRealtime()

        block()
    }
}

fun View.setOnSingleClickListener(block: () -> Unit) {
    setOnClickListener(OnSingleClickListener(block))
}

