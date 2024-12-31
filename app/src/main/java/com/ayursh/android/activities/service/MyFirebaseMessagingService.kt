package com.ayursh.android.activities.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.os.Vibrator
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.ayursh.android.R
import com.ayursh.android.activities.MainActivity.Companion.INTENT_NOTIFICATION_RECEIVE
import com.ayursh.android.activities.agora.CallingNotificationActivity
import com.ayursh.android.activities.agora.ChatActivity
import com.ayursh.android.activities.agora.VideoCallActivity
import com.ayursh.android.models.MyBookingsData
import com.ayursh.android.models.NotificationModel
import com.ayursh.android.network.RetrofitClient
import com.ayursh.android.network.responses.MyBookingsResponse
import com.ayursh.android.network.responses.RtcTokenResponse
import com.ayursh.android.utils.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MyFirebaseMessagingService : FirebaseMessagingService(), LifecycleObserver {

    private val TAG = MyFirebaseMessagingService::class.java.simpleName
    private val NOTIFICATION_ID = 0
    private val CHANNEL_ID = 121
    private var token: String? = null
    public var bookingData: MyBookingsData? = null
    var notifications= mutableListOf<NotificationModel>()


    companion object{
        var isChatActivityOpened: Boolean=false
        var msgs: HashMap<String, Int>?=null
        var forsplash=false
        var splashJson=JSONObject()
        var notificationhascount = HashMap<String, Int>()
    }


    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate: LifeCycle Observer")
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }


        override fun onNewToken(token: String) {
        Log.e(TAG, "onNewToken: " + token)
        if (!TextUtils.isEmpty(token)) {
            SAVE_FCM_TOKEN(token)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val gson = Gson()
        Log.e(TAG, "onMessageReceived: "+remoteMessage.data )

        val listNotificationString: String =SharedPref.Patient.NotificationList

        if (!TextUtils.isEmpty(listNotificationString)) {
            val type = object : TypeToken<List<NotificationModel>>() {}.type
            notifications = gson.fromJson(listNotificationString, type)!!
        }

        msgs?.put(remoteMessage.data.get("booking_id")
            .toString(), msgs?.getOrDefault(remoteMessage.data.get("booking_id")
            .toString(), 0)!! + 1)


        if (remoteMessage.data.size > 0){

            if( remoteMessage.data.get("title").equals("Audio Call") || remoteMessage.data.get("title").equals("Video Call")){
                if(remoteMessage.data.get("type").equals("remoteinvitationcancelled")) {
                    val model = NotificationModel(false, remoteMessage.data["title"], remoteMessage.data["user"], remoteMessage.data["booking_id"])

                    notifications.add(model)
                    SharedPref.Patient.NotificationList = gson.toJson(notifications)
                    sendBroadcast(Intent(INTENT_NOTIFICATION_RECEIVE))
                }

            } else {


                //  notifications.put(remoteMessage.getData().get("booking_id").toString(),false);
                val model = NotificationModel(false, remoteMessage.data["title"], remoteMessage.data["body"], remoteMessage.data["booking_id"])

                notifications.add(model)

            }
        }

        if( remoteMessage.data.get("title").equals("Audio Call") || remoteMessage.data.get("title").equals("Video Call")){
            Log.e(TAG, "onMessageReceived: calling message "+remoteMessage.data.get("title") )
            val Json = JSONObject()
            if (remoteMessage.data["type"] == "remoteinvitationrecieved") {
                forsplash=true
                Restarter.mediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI)

                try {
                    Json.put("title", remoteMessage.data["title"])
                    Json.put("doc_image", remoteMessage.data["doc_image"])
                    Json.put("booking_id", remoteMessage.data["booking_id"])
                    Json.put("fcm", remoteMessage.data["fcm"])
                    Json.put("toUser", remoteMessage.data["toUser"])
                    Json.put("user", remoteMessage.data["user"])
                    Json.put("channel", remoteMessage.data["channel"])
                    Json.put("time", remoteMessage.data["time"])

                    splashJson=Json
                    if(remoteMessage.data.containsKey("direct")){
                        Log.e(TAG, "onMessageReceived: DIRECT CALL" )
                        RetrofitClient
                            .create(this)
                            .getRTCToken("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}", remoteMessage.data["channel"].toString())
                            .enqueue(object : Callback<RtcTokenResponse> {
                                override fun onResponse(call: Call<RtcTokenResponse>, response: Response<RtcTokenResponse>) {
                                    if (response.isSuccessful) {
                                        val res = response.body()
                                        if (res?.success == true) {
                                            Restarter.vdomap.remove(remoteMessage.data["booking_id"])
                                            Log.e(TAG, "onResponse: VideoCall")
                                            startActivity(
                                                Intent(this@MyFirebaseMessagingService, VideoCallActivity::class.java)
                                                    .putExtra("rtcToken", res.data)
                                                    .putExtra("channel",remoteMessage.data["channel"])
                                                    .putExtra("fcm", remoteMessage.data["fcm"])
                                                    .putExtra("json", Json.toString())
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                            forsplash=false

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

                    } else {
                        Restarter.mediaPlayer.isLooping=true
                        Restarter.mediaPlayer.start()
                        RemoteInvitationRecieved(Json)
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else if (remoteMessage.data["type"] == "remoteinvitationcancelled") {
                forsplash=false
                try {

                    Json.put("title", remoteMessage.data["title"])
                    Json.put("fcm", remoteMessage.data["fcm"])
                    Json.put("user", remoteMessage.data["user"])
                    CancelRemoteInvitation(Json)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                try {

                    LocalInvitationRefused()
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            }
        else{
            notificationhascount.put(remoteMessage.data.get("booking_id").toString(),
                notificationhascount.getOrDefault(remoteMessage.data.get("booking_id")
                    .toString(), 0) + 1);
            val dataNoti = remoteMessage.data
            if (SharedPref.User.isLoggedIn && isChatActivityOpened == false) {
                createNotificationChannel()
                RetrofitClient.create(this)
                    .getBookingDetail("Bearer ${SharedPref.User.AUTH_TOKEN}", dataNoti["booking_id"].toString())
                    .enqueue(object : Callback<MyBookingsResponse> {
                        override fun onResponse(call: Call<MyBookingsResponse>, response: Response<MyBookingsResponse>) {
                            if (response.isSuccessful) {
                                val res = response.body()!!
                                if (res.success) {
                                    //              Log.e(TAG, "onResponse:******************** ${res.data[0]}")
                                    bookingData = res.data[0]
                                    sendNotification(res.data[0], dataNoti)

                                } else {
                                    showToast(res.message)
                                }
                            } else {
                                showToast(response.errorBody()?.string().toString())
                            }

                        }

                        override fun onFailure(call: Call<MyBookingsResponse>, t: Throwable) {
                            Log.e(TAG, "onFailure(getBookingDetail): ${t.localizedMessage}")

                        }

                    })
            }



            SharedPref.Patient.NotificationList = gson.toJson(notifications)
            sendBroadcast(Intent(INTENT_NOTIFICATION_RECEIVE))
        }

    }

    private fun sendNotification(myBookingsData: MyBookingsData, dataNoti: MutableMap<String, String>) {
        val contentIntent = Intent(applicationContext, ChatActivity::class.java)
        Log.e(TAG, "sendNotification: ----" + myBookingsData)
        contentIntent.putExtra("booking", myBookingsData)
        contentIntent.putExtra("booking_id", dataNoti["booking_id"].toString())
        val contentPendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
      //  Log.e(TAG, "onMessageReceived: Booking Data " + bookingData)

        // Build the notification
        val builder = NotificationCompat.Builder(
            applicationContext, CHANNEL_ID.toString() + "")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentTitle(dataNoti["title"])
            .setContentText(dataNoti["body"])
            .setContentIntent(contentPendingIntent)
            .setSmallIcon(R.drawable.bg1_splash)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.img_user))
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
        builder.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
        val pm = this.getSystemService(POWER_SERVICE) as PowerManager


        val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 100, 500, 300)
        v.vibrate(pattern, -1)

        val isScreenOn = pm.isScreenOn

        Log.e("screen on......", "" + isScreenOn)

            @SuppressLint("InvalidWakeLockTag") val wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE, "MyLock")
            wl.acquire(10000)
            @SuppressLint("InvalidWakeLockTag") val wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyCpuLock")
            wl_cpu.acquire(10000)

        if(isChatActivityOpened==false) {
            notificationManager.notify(CHANNEL_ID, builder.build())
        }

        // Build the notification


    }

    @SuppressLint("WrongConstant")
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isChatActivityOpened==false) {
            val name: CharSequence = "LoginPoc"
            val description = "LoginPoc chancel"
            val importance = NotificationManager.IMPORTANCE_MAX
            val channel = NotificationChannel(CHANNEL_ID.toString() + "", name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        Log.e("ServiceApp", "App in background --->" + Restarter.appState)
        Restarter.appState = "Background"
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        Log.e("ServiceApp", "App in foreground --->" + Restarter.appState)
        Restarter.appState = "Foreground"
        val nm = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(Restarter.NOTIFICATION_ID_PRIMARY)

        if(Restarter.autoTime && CallingNotificationActivity.activity==null){
            Log.e(TAG, "onAppForegrounded: CALL HERE")
            startActivity(Restarter.CallIntent)
        }
    }
}