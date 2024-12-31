package com.ayursh.android.activities

import android.content.*
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.ayursh.android.R
import com.ayursh.android.activities.agora.CallingNotificationActivity
import com.ayursh.android.activities.service.MyFirebaseMessagingService
import com.ayursh.android.databinding.ActivityMainBinding
import com.ayursh.android.fragments.Changebadge
import com.ayursh.android.fragments.MyBookingsFragment
import com.ayursh.android.fragments.ProfileFragment
import com.ayursh.android.models.NotificationModel
import com.ayursh.android.network.RetrofitClient
import com.ayursh.android.network.responses.FCMResponse
import com.ayursh.android.network.responses.RtcTokenResponse
import com.ayursh.android.network.responses.UserResponse
import com.ayursh.android.utils.*
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    var gson=Gson()
    companion object{
        var token :String?=null
        val INTENT_NOTIFICATION_RECEIVE="INTENT_NOTIFICATION_RECEIVE"
    }

    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateNotificationBadge()
        }
    }

    private var binder: ActivityMainBinding? = null
    private var callend: Boolean=false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(broadcastReceiver, IntentFilter(INTENT_NOTIFICATION_RECEIVE))
        binder = DataBindingUtil.setContentView(this, R.layout.activity_main)
        if(CallingNotificationActivity.activity!=null){
            CallingNotificationActivity.activity?.finish()
            CallingNotificationActivity.activity=null
        }

        SAVE_FCM_TOKEN()
        init()
        Log.e(TAG, "onCreate: Auth ${SharedPref.User.AUTH_TOKEN}")

    }

    fun updateNotificationBadge() {

        val notificationListString: String = SharedPref.Patient.NotificationList
        if (!TextUtils.isEmpty(notificationListString)) {
            val type = object : TypeToken<List<NotificationModel?>?>() {}.type

            if(!Restarter.vdomap.isEmpty() or !Restarter.audmap.isEmpty() or (MyFirebaseMessagingService.msgs?.isEmpty() == false)){
               Changebadge().showbadge()
            }
            val notifications:List<NotificationModel>  = gson.fromJson(notificationListString, type);
            var builder:StringBuilder
            var count = 0
            for(model in notifications) {
                builder= java.lang.StringBuilder(model.toString()).append("\n\n");
//                count = count + !model.isRead() ? 1 : 0;
                if (!model.isRead){
                    count+=1
                }
            }

            if(count > 0 || Constants.msgPending.equals("yes")) {
                Changebadge().showbadge()
            } else {
                Changebadge().hidebadge()
            }
        } else {
            Changebadge().hidebadge()
        }

    }


    private fun init() {
        addToken()
        initListeners()
        getProfile()
        openFragment(MyBookingsFragment.newInstance())
        updateNotificationBadge()

    }

    private fun getProfile() {
        RetrofitClient.create(this).getProfile("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}")
            .enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {

                    if (response.isSuccessful) {
                        val userRes: UserResponse? = response.body()
                        if (userRes?.success == true) {
                            Log.e(TAG, "onResponse: PROFILE OF DOCTOR" + userRes.data)
                            SharedPref.User.USER = userRes.data
                        }
                    } else {
                        Log.e(TAG, "onResponse: FAILURE "+response.message() )
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    Log.e(TAG, "onFailure: ${t.localizedMessage}")

                }

            })
    }

    private fun addToken() {
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task: Task<String?> ->
                if (!task.isSuccessful) {
                    Log.w(SplashActivity::class.java.simpleName, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                token = task.result
                if (token != null) {
                    Log.e("Extension", "FCM: " + token.toString())
                    SharedPref.User.fcm_token= token.toString()
                    val data = JsonObject()
                    data.addProperty("fcm_token",token)

                    RetrofitClient.create(this).Postfcm("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}",data).enqueue(object : Callback<FCMResponse> {
                        override fun onResponse(call: Call<FCMResponse>, response: Response<FCMResponse>) {

                            if (response.isSuccessful) {

                            } else {
                                when {
                                    response.code() == 500 -> {
                                        showToast(response.errorBody()
                                            ?.string().toString())

                                    }
                                    response.code() == 422 -> {
                                        val errorRes = JSONObject(response.errorBody()?.string().toString())
                                        if (errorRes.has("message")) {
                                            showToast(errorRes.getString("message"))
                                        } else {
                                            showToast("Something went wrong, Try Again.")
                                        }
                                    }
                                    else -> {
                                        showToast(response.message())
                                    }
                                }
                            }

                        }
                        override fun onFailure(call: Call<FCMResponse>, t: Throwable) {
                        }
                    })
                }
            }
    }

    private fun initListeners() {
        callend=intent.getBooleanExtra("callend",false)
        Log.e(TAG, "initListeners: Profileeee" )
        binder?.bottomTab?.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_mybooking -> {
                    openFragment(MyBookingsFragment.newInstance())
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.navigation_profile -> {
                    Log.e(TAG, "initListeners: Profile" )
                    openFragment(ProfileFragment.newInstance())
                    return@setOnNavigationItemSelectedListener true
                }
                else -> return@setOnNavigationItemSelectedListener false
            }
        }
    }

    private fun openFragment(fragment: Fragment) {
        Log.e(TAG, "openFragment: Fragment "+fragment )
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frame, fragment)
        transaction.addToBackStack(null)
        transaction.commitAllowingStateLoss()
    }

    override fun onBackPressed() {

    }
}