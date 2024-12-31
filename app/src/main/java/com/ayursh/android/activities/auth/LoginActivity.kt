package com.ayursh.android.activities.auth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.ayursh.android.R
import com.ayursh.android.databinding.ActivityLoginBinding
import com.ayursh.android.network.RetrofitClient
import com.ayursh.android.network.responses.auth.AuthResponse
import com.ayursh.android.utils.*
import com.google.gson.JsonObject
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


private const val TAG = "LoginActivity"

class LoginActivity : AppCompatActivity() {
    private var binder: ActivityLoginBinding? = null
    private var mobile: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FULLSCREEN()
        binder = DataBindingUtil.setContentView(this, R.layout.activity_login)
        init()
    }

    private fun init() {
        initListeners()
    }

    private fun initListeners() {
        binder?.getOtpBtn?.setOnClickListener {
            mobile = binder?.etMobilenumber?.text?.trim().toString()
            if (mobile?.length != 10) {
                binder?.etMobilenumber?.error = "Enter Valid Number"
                return@setOnClickListener
            }
            Log.e(TAG, "initListeners: " )
            sendOtp()
        }
        binder?.tvSignup?.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    private fun sendOtp() {
        if (checkInternetConnection()) {
            showProgress()
            val data = JsonObject()
            data.addProperty(Constants.PHONE_NUMBER, "+91$mobile")
            RetrofitClient.create(this).login(data).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                    if (response.isSuccessful) {
                        Log.e(TAG, "onResponse: ***"+response)
                        val res: AuthResponse? = response.body()
                        if (res?.success == true) {
                            Log.e(TAG, "onResponse: ++++" + res.data)
                            proceedOtpVerification(res.data.doctor_id, res.data.sms_token_session_id)
                        } else {
                            Log.e(TAG, "onResponse: -----"+res?.data )
                            // showToast(res?.message.toString())
                        }
                    } else {
                        Log.e(TAG, "onResponse: >>>" + response)

                        when {
                            response.code() == 500 -> {
//                                showToast(response.errorBody()
//                                    ?.string().toString())
                            }
                            response.code() == 422 -> {
                                needRegistration()
//                                val errorRes = JSONObject(response.errorBody()?.string().toString())
//                                if (errorRes.has("success")) {
//                                    if (errorRes.getString("error_code")
//                                            .equals("AYOU0007") && errorRes.getString("message")
//                                            .contains("register")) {
//                                        needRegistration()
//                                    } else {
//                                        //                                    showToast(errorRes.getString("message"))
//                                    }
//                                } else {
//                                    showToast("Something went wrong, Try Again.")
//                                }
                            }
                            else -> {
                                //        showToast(response.message())
                            }
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

    private fun proceedOtpVerification(doc_id: String, sms_token: String) {
        Log.e(TAG, "proceedOtpVerification: " )
        startActivity(Intent(this, VerifyOtpActivity::class.java)
            .putExtra("doc_id", doc_id)
            .putExtra("sms_token", sms_token)
            .putExtra("mobile", mobile)
        )

    }

    private fun needRegistration() {
        showToast("You're not registered")
        startActivity(Intent(this, SignUpActivity::class.java))
        finish()
    }
}