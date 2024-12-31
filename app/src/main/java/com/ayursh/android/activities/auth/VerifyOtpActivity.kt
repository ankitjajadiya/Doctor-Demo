package com.ayursh.android.activities.auth

import `in`.aabhasjindal.otptextview.OTPListener
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ayursh.android.R
import com.ayursh.android.activities.MainActivity
import com.ayursh.android.databinding.ActivityVerifyOtpBinding
import com.ayursh.android.network.RetrofitClient
import com.ayursh.android.network.responses.UserResponse
import com.ayursh.android.network.responses.auth.AuthResponse
import com.ayursh.android.network.responses.otp.OtpResponse
import com.ayursh.android.utils.*
import com.google.gson.JsonObject
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit

private const val TAG = "VerifyOtpActivity"

class VerifyOtpActivity : AppCompatActivity() {
    private var binder: ActivityVerifyOtpBinding? = null
    private var timer: Int = 15
    private var docId = ""
    private var smsToken = ""
    private var mobile = ""
    private var otp = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FULLSCREEN()
        binder = DataBindingUtil.setContentView(this, R.layout.activity_verify_otp)

        init()
    }

    private fun init() {
        getIntentData()
        beginCountdown()
        initListeners()

    }


    private fun getIntentData() {
        if (intent.getStringExtra("doc_id") == null) {
            showToast("Requires Doc Id")
            finish()
        }

        if (intent.getStringExtra("sms_token") == null) {
            showToast("Requires Sms Token")
            finish()
        }
        if (intent.getStringExtra("mobile") == null) {
            showToast("Requires Sms Token")
            finish()
        }
        docId = intent.getStringExtra("doc_id").toString()
        smsToken = intent.getStringExtra("sms_token").toString()
        mobile = intent.getStringExtra("mobile").toString()
        binder?.tvTitle1?.text = "OTP sent to $mobile"
    }

    private fun initListeners() {
        binder?.ivNext?.setOnClickListener {
            otp = binder?.otpView?.otp.toString()
            if (otp.isNullOrEmpty() || otp.length < 1) {
                showToast("Enter OTP")
                return@setOnClickListener
            }
            verifyOTP()
        }
        binder?.tvResend?.setOnClickListener {
            resendOtp()
        }
        binder?.otpView?.otpListener = object : OTPListener {
            override fun onInteractionListener() {
            }

            override fun onOTPComplete(OTP: String?) {
                otp=OTP.toString()
                verifyOTP()
            }
        }
    }

    private fun beginCountdown() {

        binder?.tvResendTimer?.visibility = View.VISIBLE
        binder?.tvResend?.visibility = View.GONE
        val timer = object: CountDownTimer(60000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                Log.e(TAG, "onTick: $millisUntilFinished")
                binder?.tvResendTimer?.text = "" + String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(
                TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)))
            }
            override fun onFinish() {
                binder?.tvResendTimer?.visibility = View.GONE
                binder?.tvResend?.visibility = View.VISIBLE
            }
        }
        timer.start()
    }

    private fun verifyOTP() {
        if (checkInternetConnection()) {
            if (binder?.otpView?.otp?.length!! < 6) {
                Toast.makeText(this, "Please enter valid OTP", Toast.LENGTH_SHORT).show()
                binder?.otpView?.requestFocusOTP()
                binder?.otpView?.resetState()
            } else {
                showProgress()
                val data = JsonObject()
                data.addProperty("doctor_id", docId)
                data.addProperty("phone_number", mobile)
                data.addProperty("sms_token_session_id", smsToken)
                data.addProperty("sms_token", otp)
                Log.e(TAG, "verifyOTP: $data")
                Log.e(TAG, "Doctor ID= ${docId}")
                RetrofitClient.create(this).validateOtp(data)
                    .enqueue(object : Callback<OtpResponse> {
                        override fun onResponse(call: Call<OtpResponse>, response: Response<OtpResponse>) {

                            if (response.isSuccessful) {
                                val res: OtpResponse? = response.body()
                                if (res?.success == true && res.data.token_approved) {
                                    dismissProgress()
                                    //Get token then save token api
                                    // -> response, calll proceed to main
                                    proceedToMain(res)
                                } else {
                                    dismissProgress()
                                    showToast(res?.message.toString())
                                }
                            } else {
                                try {
                                    val errorRes = JSONObject(response.errorBody()?.string()
                                        .toString())
                                    if (errorRes.has("success")) {
                                        showToast(errorRes.getString("message"))
                                    } else {
                                        showToast("Something went wrong, Try Again.")
                                    }
                                } catch (e: Exception) {
                                    showToast(response.errorBody()
                                        ?.string().toString())
                                }
                                dismissProgress()
                            }
                        }

                        override fun onFailure(call: Call<OtpResponse>, t: Throwable) {
                            Log.e(TAG, "onFailure: ${t.localizedMessage}")
                            dismissProgress()
                        }
                    })
            }
        }
    }

    private fun resendOtp() {
        if (checkInternetConnection()) {
            showProgress()
            val obj = JsonObject()
            obj.addProperty("phone_number", "+91$mobile")
            RetrofitClient.create(this).login(obj).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful) {
                        val res: AuthResponse? = response.body()
                        if (res?.success == true) {
                            showToast("Sent Again")
                            smsToken = res.data.sms_token_session_id
                            beginCountdown()
                        } else {
                            showToast(res?.message.toString())
                        }
                    } else {
                        try {
                            val errorRes = JSONObject(response.errorBody()?.string().toString())
                            if (errorRes.has("success")) {
                                if (errorRes.getString("error_code")
                                        .equals("AYOU0007") && errorRes.getString("message")
                                        .contains("register")) {
                                    showToast("You're not registered")
                                    startActivity(Intent(this@VerifyOtpActivity, SignUpActivity::class.java))
                                    finish()
                                } else {
                                    showToast(errorRes.getString("message"))
                                }
                            } else {
                                showToast("Something went wrong, Try Again.")
                            }
                        } catch (e: Exception) {
                            showToast(response.errorBody()
                                ?.string().toString())
                        }
                    }
                    dismissProgress()
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    Log.e(TAG, "onFailure: ${t.localizedMessage}")
                    dismissProgress()
                }

            })
        }
    }

    private fun proceedToMain(res: OtpResponse) {
        SharedPref.User.login(res.data.auth_token, docId)

        showProgress("Setting up profile")
        RetrofitClient.create(this).getProfile("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}")
            .enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {

                    if (response.isSuccessful) {
                        val userRes: UserResponse? = response.body()
                        Log.e(TAG, "onResponse:$userRes ")
                        if (userRes?.success == true) {
                            SharedPref.User.USER = userRes.data
                            SAVE_FCM_TOKEN()
                            startActivity(Intent(this@VerifyOtpActivity, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .putExtra("firstActivity",true)
                            )
                            finishAffinity()
                        } else {
                         //   showToast(userRes?.message.toString())
                        }
                    } else {
                        Log.e(TAG, "Exception: ${
                            response.errorBody()
                                ?.string().toString()
                        }")
//                       showToast(response.errorBody()
//                            ?.string().toString())

                    }
                    dismissProgress()
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    Log.e(TAG, "onFailure: ${t.localizedMessage}")
                    dismissProgress()
                }

            })
    }
}