package com.ayursh.android.activities.agora

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.JobIntentService
import com.ayursh.android.R
import com.ayursh.android.activities.MainActivity
import com.ayursh.android.activities.SplashActivity
import com.ayursh.android.activities.service.MyFirebaseMessagingService
import com.ayursh.android.network.RetrofitClient
import com.ayursh.android.network.responses.RtcTokenResponse
import com.ayursh.android.utils.*
import com.google.android.material.button.MaterialButton
import io.agora.rtc2.RtcEngine
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.system.exitProcess


private const val TAG = "CallingNotificationActi"

class CallingNotificationActivity : AppCompatActivity() {
    private var vib: Vibrator? = null
    private var channel: String? = ""
    private var bookingForName: String? = ""
    private var toUser: String? = ""
    var fromUser: Int = 0
    private var callType: String? = ""
    private var materialButton: MaterialButton? = null
    private var endCall: ImageView? = null
    private var callingStatus: TextView? = null
    private var callerName: TextView? = null
    private var callTypeTV: TextView? = null
    private var isCallEndedByButton = false
    var activity: Activity? = null
    var notificationManager: NotificationManager?=null
    var booking_id:String?=null
    var fcm: String?=null
    var json = JSONObject()


    var Notification_id=1
    private lateinit var taskList: List<ActivityManager.RunningTaskInfo>

    companion object {
        @SuppressLint("StaticFieldLeak")
        var activity: Activity?=null
        var active = false

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_calling_screen)
        Restarter.autoTime=false
        try {
            json = JSONObject(intent.getStringExtra("json"))

        } catch (e: JSONException) {
            e.printStackTrace()
        }


        notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        CallingNotificationActivity.activity=this
        Restarter.CallCancelled=true

        Log.e(TAG, "onCreate: onRemoteInvitationReceived Inside Create  ")
        notificationManager?.cancel(Restarter.NOTIFICATION_ID_PRIMARY)
        notificationManager?.cancel(Restarter.NOTIFICATION_ID)
        init()

        val mngr = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        taskList = mngr.getRunningTasks(10)

    }

    
    private fun init() {
        notificationManager?.cancel(Restarter.NOTIFICATION_ID_PRIMARY)
        notificationManager?.cancel(Restarter.NOTIFICATION_ID)
        initElements()
        initListeners()
    }

    private fun initElements() {
        callerName = findViewById(R.id.callerName)
        callTypeTV = findViewById(R.id.callType)
        materialButton = findViewById(R.id.materialButton)
        endCall = findViewById(R.id.endBtn)
        callingStatus = findViewById(R.id.callingStatus)
//        vib = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//        vib?.vibrate(longArrayOf(0, 1000, 800, 1000, 800, 1000, 800, 1000, 100),0)
        channel = intent.getStringExtra("channel")
        bookingForName = intent.getStringExtra("user")
        toUser = intent.getStringExtra("toUser")
        fromUser = intent.getIntExtra("flag", 0)
        Notification_id=intent.getIntExtra("Notification_id", 1)
        notificationManager?.cancel(Notification_id)
        booking_id=intent.getStringExtra("booking_id")
        fcm=intent.getStringExtra("fcm")

    //    remoteinvitation= intent.getSerializableExtra("invitation") as RemoteInvitation
        if(fromUser ==1){
            MyFirebaseMessagingService.forsplash=false
            finish()
        }
        callType = intent.getStringExtra("callType")
        Log.e(TAG, "initElements:---------> "+callType )
        if (callType == "Video") {
            callTypeTV?.text = "Video Call"
        } else {
            callTypeTV?.text = "Audio Call"
        }
        callerName?.text = bookingForName

    }


    private fun initListeners() {
        Log.e(TAG, "initListeners: onRemote1")
        endCall!!.setOnClickListener {
            RefuseRemoteInvitation(this, callType.toString() + " Call", fcm.toString())

            isCallEndedByButton = true
            if (Restarter.Service_timer != null) {
                Restarter.Service_timer.cancel()
            }
            if (Restarter.v != null) {
                Restarter.v.cancel()
            }
            if(Restarter.mediaPlayer!=null){
                Restarter.mediaPlayer.stop()
            }

            startActivity(
                Intent(this, MainActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("callend", true)
            )
        }

        materialButton!!.setOnClickListener {
            if(Restarter.v!=null){
                Restarter.v.cancel()
            }
            callingStatus!!.text = "Connecting...."
            if (callType == "Audio") {
                audioCall()
            } else if (callType == "Video") {
                videoCall()
            } else {
                showToast("Not a valid call type")
                CallingNotificationActivity.activity=null
                MyFirebaseMessagingService.forsplash=false
                finish()
            }
        }

    }


    private fun videoCall() {
        RetrofitClient
            .create(this)
            .getRTCToken("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}", channel.toString())
            .enqueue(object : Callback<RtcTokenResponse> {
                override fun onResponse(call: Call<RtcTokenResponse>, response: Response<RtcTokenResponse>) {

                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.success == true) {
                            Restarter.vdomap.remove(booking_id)
                            Log.e(TAG, "onResponse: VideoCall")
                            startActivity(
                                Intent(this@CallingNotificationActivity, VideoCallActivity::class.java)
                                    .putExtra("rtcToken", res.data)
                                    .putExtra("channel", channel)
                                    .putExtra("fcm", fcm)
                                    .putExtra("json", json.toString())
                            )
                            MyFirebaseMessagingService.forsplash=false
                            finish()

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

    private fun audioCall() {
        RetrofitClient
            .create(this)
            .getRTCToken("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}", channel.toString())
            .enqueue(object : Callback<RtcTokenResponse> {
                override fun onResponse(call: Call<RtcTokenResponse>, response: Response<RtcTokenResponse>) {

                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.success == true) {
                            Restarter.audmap.remove(booking_id)
                            Log.e(TAG, "onResponse: AudioCall")
                            startActivity(
                                Intent(this@CallingNotificationActivity, AudioCallActivity::class.java)
                                    .putExtra("rtcToken", res.data)
                                    .putExtra("channel", channel)
                                    .putExtra("patient_name", bookingForName)
                                    .putExtra("fcm", fcm)
                                    .putExtra("json", json.toString())
                            )
                            MyFirebaseMessagingService.forsplash=false
                            finish()
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

    override fun onBackPressed() {
        //super.onBackPressed();
    }

    override fun onDestroy() {
        super.onDestroy()
        MyFirebaseMessagingService.forsplash=false
        activity=null
        vib?.cancel()
        if(SplashActivity.splashActivity!=null){
            SplashActivity.splashActivity!!.finish()
        }
        Restarter.mediaPlayer?.stop()
      //  startActivity(Intent(this,MainActivity::class.java))
        if (isCallEndedByButton) {
            if (taskList[0].numActivities == 1 && taskList[0].topActivity!!.className == this.javaClass.name) {
//                try {
//                    Log.e(TAG, "This is last activity in the stack")
//                    CallingNotificationActivity.activity=null
//                    finishAffinity()
//                    exitProcess(0);
//                }catch (e:Exception){
//                    Log.e(TAG, "onDestroy: "+e.localizedMessage )
//                }

            } else {
                Log.e(TAG, "onCreate: This is not last.")
                CallingNotificationActivity.activity=null
                finish()
            }
        }
    }
}