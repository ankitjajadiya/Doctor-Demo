package com.ayursh.android.utils


import android.annotation.SuppressLint
import android.app.*
import android.content.ActivityNotFoundException
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Vibrator
import android.provider.Settings
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ayursh.android.NotificationActivity
import com.ayursh.android.R
import com.ayursh.android.activities.MainActivity
import com.ayursh.android.activities.agora.AudioCallActivity
import com.ayursh.android.activities.agora.CallingNotificationActivity
import com.ayursh.android.activities.agora.CallingNotificationActivity.Companion.activity
import com.ayursh.android.activities.agora.ChatActivity
import com.ayursh.android.activities.agora.VideoCallActivity
import com.ayursh.android.models.MessageModel
import com.ayursh.android.models.MyBookingsData
import com.ayursh.android.network.RetrofitClient
import com.ayursh.android.network.responses.FCMResponse
import com.ayursh.android.network.responses.RtcTokenResponse
import com.ayursh.android.utils.Restarter.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.StorageReference
import com.google.gson.JsonObject
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern


var progressDialog: ProgressDialog? = null

fun Context.showToast(msg: String, isLong: Boolean = false) {
    Toast.makeText(this, msg, if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun AppCompatActivity.FULLSCREEN() {
    window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    window.statusBarColor = Color.TRANSPARENT
}


fun Context.checkInternetConnection(): Boolean {
    val cm: ConnectivityManager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val ni: NetworkInfo? = cm.activeNetworkInfo
    val res = ni != null && ni.isConnectedOrConnecting
    if (!res) {
        showToast("No Internet Connection.")
    }
    return res
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun Context.showProgress(msg: String = "Please Wait..") {
    progressDialog = ProgressDialog.show(this, "", msg)
    progressDialog?.setCancelable(true)
}

fun dismissProgress() {
    if (progressDialog != null && progressDialog?.isShowing == true) {
        progressDialog?.dismiss()
    }
}

fun String.validateEmail(): Boolean {
    val emailPattern = "^[a-zA-Z0-9_+&*-]+(?:\\." +
            "[a-zA-Z0-9_+&*-]+)*@" +
            "(?:[a-zA-Z0-9-]+\\.)+[a-z" +
            "A-Z]{2,7}$"
    val pat = Pattern.compile(emailPattern)
    return pat.matcher(this).matches()
}

fun EditText.showDatePicker(ctx: Context) {
    val c = Calendar.getInstance()
    val dpd =
        DatePickerDialog(ctx, R.style.datePickerDialog, { _, year, monthOfYear, dayOfMonth ->
            var day = dayOfMonth.toString()
            if (dayOfMonth < 10) {
                day = "0$dayOfMonth"
            }
            var month = (monthOfYear + 1).toString()
            if ((monthOfYear + 1) < 10) {
                month = "0$monthOfYear"
            }
            this.setText("$day-$month-$year")
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    dpd.datePicker.maxDate = c.timeInMillis
    dpd.show()
}

fun Long.toDate(format: String): String {
    return SimpleDateFormat(format, Locale.getDefault()).format(Date(this)).toString()
}

fun Context.BlurImage(drawable: Int, blurRadius: Float = 7.0f): Bitmap {
    val image = BitmapFactory.decodeResource(resources, drawable)
    val width = Math.round(image.getWidth() * 0.4f).toInt()
    val height = Math.round(image.getHeight() * 0.4f).toInt()

    val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
    val outputBitmap = Bitmap.createBitmap(inputBitmap)

    val rs = RenderScript.create(this)
    val theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
    val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
    val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)
    theIntrinsic.setRadius(blurRadius)
    theIntrinsic.setInput(tmpIn)
    theIntrinsic.forEach(tmpOut)
    tmpOut.copyTo(outputBitmap)

    return outputBitmap
}


fun Context.SAVE_FCM_TOKEN(fcm_token: String = "") {
    val TAG = "SAVE FCM TOKEN";
    if (fcm_token.equals("")) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(object : OnCompleteListener<String> {
            override fun onComplete(task: Task<String>) {
                if (task.isComplete) {
                    if (task.result != null) {
                        val token = task.result;
                        sendToServer(this@SAVE_FCM_TOKEN, token.toString())
                        SharedPref.User.fcm_token = token.toString()
                        Log.e("SAVEFCMTOKEN", "onComplete: " + token.toString() + "-->" + SharedPref.User.fcm_token)
                    }
                }
            }

        })
    } else {
        sendToServer(this, fcm_token)
    }
    Log.e(TAG, "SAVE_FCM_TOKEN: " + SharedPref.User.fcm_token)
}

private fun Context.sendToServer(context: Context, token: String) {
    val TAG = "SendingFCM"
    val data = JsonObject()
    data.addProperty("fcm_token", token)
    RetrofitClient.create(context)
        .Postfcm("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}", data)
        .enqueue(object : Callback<FCMResponse> {
            override fun onResponse(call: Call<FCMResponse>, response: Response<FCMResponse>) {

                if (!response.isSuccessful) {
                    when {
                        response.code() == 500 -> {
                            Log.e(TAG, "onResponse: " + response.message())
                        }

                        response.code() == 422 -> {
                            val errorRes = JSONObject(response.errorBody()?.string()
                                .toString())
                            if (errorRes.has("message")) {
                                showToast(TAG + " " + errorRes.getString("message"))
                            } else {
                                showToast(TAG + " Something went wrong, Try Again.")
                            }
                        }

                        else -> {
                            Log.e(TAG, "onResponse: " + response.message())
                            SharedPref.User.fcm_token = token
                            Log.e("SAVEFCMTOKEN", "onComplete: " + token.toString() + "-->" + SharedPref.User.fcm_token)

                        }
                    }
                }
            }

            override fun onFailure(call: Call<FCMResponse>, t: Throwable) {

            }

        })

}

fun Context.SendLocalInvitation(context: Context, callType: String, bookingData: MyBookingsData?, res: RtcTokenResponse, notijson: JsonObject) {        //doctor to user
    val user_fcm = bookingData?.fcm_token

    val json = JsonObject()
    val TAG = "Extension";
    val token = JsonObject()
    token.addProperty("token", user_fcm)
    json.add("message", token)
    json.add("data", notijson)

    Log.e("Extension", "SendLocalInvitation: " + json)
    if (checkInternetConnection()) {
        RetrofitClient.create(this)
            .sendnotification("Bearer ${SharedPref.User.AUTH_TOKEN}", json)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.e(TAG, "onResponse: sdf " + response)

                    if (!notijson.has("direct")) {
                        try {
                            if (callType == "Audio Call") {
                                startActivity(
                                    Intent(context, AudioCallActivity::class.java)
                                        .putExtra("rtcToken", res.data)
                                        .putExtra("channel", bookingData?.user_consultation_booking_id)
                                        .putExtra("patient_name", if (bookingData?.prescription != null) bookingData.prescription.patient_name else bookingData?.booking_for_name)
                                        .putExtra("booking", bookingData)
                                        .putExtra("json", json.toString())
                                )
                            } else {
                                startActivity(
                                    Intent(context, VideoCallActivity::class.java)
                                        .putExtra("rtcToken", res.data)
                                        .putExtra("channel", bookingData?.user_consultation_booking_id)
                                        .putExtra("user", bookingData?.doctor_id)
                                        .putExtra("booking", bookingData)
                                        .putExtra("json", json.toString())
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("Extension", "onSuccess:Exception ${e.printStackTrace()}")
                        }
                    }
                }

                override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                    Log.e("Extension", "onFailure: ")
                }
            })
    } else {
        showToast("Please check your Internet Connection", false)
    }


}

fun Context.LocalInvitationCancel(json: JsonObject) {      //doctor to user

    Log.e(TAG, "LocalInvitationCancel: " + json)


    if (checkInternetConnection()) {
        RetrofitClient.create(this)
            .sendnotification("Bearer ${SharedPref.User.AUTH_TOKEN}", json)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.e("TAG", "done " + response.body()?.string().toString())
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    showToast("Something went wrong, Please Try again")
                    Log.e("TAG", "onFailure: " + t.localizedMessage)
                }
            })
    } else {
        showToast("Please check your Internet Connection", false)
    }


}

fun Context.LocalInvitationRefused() {                       //user to doctor
    if (AudioCallActivity.audioCall != null) {
        AudioCallActivity.audioCall!!.finish();
    }
    if (VideoCallActivity.videoCall != null) {
        VideoCallActivity.videoCall!!.finish();
    }

}

@SuppressLint("NewApi")
@RequiresApi(Build.VERSION_CODES.O)
fun Context.RemoteInvitationRecieved(json: JSONObject) {                     //user to doctor
    Log.e("Extension", "onRemoteInvitationReceived: ")
    var callType = ""
    incoming = true
    audvid = false
    v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    autoTime = true

    try {
        Log.e("TAG", "onRemoteInvitationReceived: $json")


        if (json.getString("title") == "Audio Call") {
            audmap[json.getString("booking_id")] = Restarter.audmap.getOrDefault(json.getString("booking_id"), 0) + 1
            callType = "Audio"
            Log.e(TAG, "onRemoteInvitationReceived: " + callType)
        } else {
            vdomap[json.getString("booking_id")] = vdomap.getOrDefault(json.getString("booking_id"), 0) + 1
            callType = "Video"
            Log.e(TAG, "onRemoteInvitationReceived: " + callType)
        }


        try {
            val intent = Intent(this, CallingNotificationActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("callType", callType)
            intent.putExtra("channel", json.getString("channel"))
            intent.putExtra("user", json.getString("user"))
            intent.putExtra("toUser", json.getString("toUser"))
            intent.putExtra("Notification_id", NOTIFICATION_ID_PRIMARY)
            intent.putExtra("flag", 0)
            intent.putExtra("booking_id", json.getString("booking_id"))
            intent.putExtra("Notification_id", NOTIFICATION_ID_PRIMARY)
            intent.putExtra("fcm", json.getString("fcm"))
            intent.putExtra("json", json.toString())
            CallIntent = intent

            Log.e(TAG, "RemoteInvitationRecieved: AppState " + appState)
            if (appState == "Foreground") {

                startActivity(intent)
            } else {
                notificationPop(json, callType)
            }
            val pattern = longArrayOf(0, 1000, 800, 1000, 800, 1000, 800, 1000, 100)
            try {
                Log.e(TAG, "RemoteInvitationRecieved: INSIDE TRY")
                v.vibrate(pattern, 0)
            } catch (e: Exception) {
                Log.e(TAG, "RemoteInvitationRecieved: Exception" + e.localizedMessage)
            }


        } catch (e: ActivityNotFoundException) {
            Log.e("TAG", "onRemoteInvitationReceived:----- \${e.printStackTrace()}")
        }
    } catch (e: JSONException) {
        Log.e("TAG", "onRemoteInvitationReceived:****** " + e.localizedMessage)
        e.printStackTrace()
    }


}


fun Context.RefuseRemoteInvitation(context: Context, callType: String, fcm: String) {     //doctor to user
    Log.e(TAG, "RefuseRemoteInvitation: " + fcm)
    val json = JsonObject()
    val notiJson = JsonObject()
    val token = JsonObject()
    token.addProperty("token", fcm)
    json.add("message", token)
    notiJson.addProperty("title", callType)
    notiJson.addProperty("type", "refuselocalinvitation")
    json.add("data", notiJson)
    Log.e(TAG, "RefuseRemoteInvitation: " + json)

    if (checkInternetConnection()) {
        RetrofitClient.create(this)
            .sendnotification("Bearer ${SharedPref.User.AUTH_TOKEN}", json)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.e("TAG", "done -----" + response.body()?.string().toString() + " " + json)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    showToast("Something went wrong, Please Try again")
                    Log.e("TAG", "onFailure: " + t.localizedMessage)
                }

            })
    } else {
        showToast("Please check your Internet Connection", false)
    }


}

fun Context.CancelRemoteInvitation(json: JSONObject) {                       //user to doctor
    Restarter.autoTime = false
    Log.e("TAG", "onRemoteInvitationCanceled: ");
    if (Service_timer != null) {
        Service_timer.cancel();
    }
    val notificationManager: NotificationManager = getApplicationContext().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(NOTIFICATION_ID_PRIMARY);
    AudioCallActivity.audioCall?.finish()
    VideoCallActivity.videoCall?.finish()

    if (CallCancelled && activity != null) {
        Log.e(TAG, "onRemoteInvitationCanceled: Inside CallCancelled");
        activity?.finish();
        activity = null

    } else {
        val notificationManager: NotificationManager = getApplicationContext().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_PRIMARY);
    }

    if (!audvid) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        if (v != null) {
            v.cancel();
        }
        mediaPlayer = null;
        pushNotification(json);
//        startActivity(Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        CallCancelled = false;
    }

}


fun Context.pushNotification(json: JSONObject) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val intent: Intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        var builder: NotificationCompat.Builder? = null;
        try {
            builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_PRIMARY)
                .setSmallIcon(R.drawable.bg1_splash)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentTitle("Missed " + json.getString("title") + "... ")
                .setContentText(json.getString("user"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 1000, 800, 900))
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.img_user));
        } catch (e: JSONException) {
            e.printStackTrace();
        }


        Log.e("TAG", "notificationPop: onRemote------------");
        val name = "Incoming Call";
        val importance = NotificationManager.IMPORTANCE_MAX;
        builder?.setColor(ContextCompat.getColor(this, R.color.accent));
        builder?.setSound(Uri.parse(Settings.System.NOTIFICATION_SOUND));

        @SuppressLint("WrongConstant") val channel: NotificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_PRIMARY, name, importance);
        channel.enableVibration(true)
        channel.vibrationPattern = longArrayOf(0, 1000, 800, 500)
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        // Register the channel with the system
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel);

        builder?.let { (NotificationManagerCompat.from(this)).notify(NOTIFICATION_ID, it.build()) };
    }


}

@SuppressLint("InvalidWakeLockTag")
fun Context.notificationPop(json: JSONObject, callType: String) {
    notification = true
    Log.e("TAG", "onRemoteInvitationReceived   notificationPop: ")
    val intent = Intent(this, CallingNotificationActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)


    val callIntent = NotificationActivity.getDismissIntent(NOTIFICATION_ID_PRIMARY, this, callType, json.getString("fcm"))
    try {
        intent.putExtra("callType", callType)
        intent.putExtra("channel", json.getString("channel"))
        intent.putExtra("user", json.getString("user"))
        intent.putExtra("toUser", json.getString("toUser"))
        intent.putExtra("booking_id", json.getString("booking_id"))
        intent.putExtra("fcm", json.getString("fcm"))
        intent.putExtra("json", json.toString())
    } catch (e: JSONException) {
        e.printStackTrace()
    }
    intent.putExtra("flag", 0)
    val pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)


    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Log.e("TAG", "notificationPop: onRemote------------")
        val remoteViews = RemoteViews(packageName, R.layout.activity_notification)
        var status: String? = null
        try {
            status = "Incoming " + json.getString("title") + " ..."
            remoteViews.setTextViewText(R.id.callStatus, status)
            remoteViews.setTextViewText(R.id.name, json.getString("user"))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        Log.e("TAG", "notificationPop: onRemote------------")
        remoteViews.setOnClickPendingIntent(R.id.acceptCall, pendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.endCall, callIntent)
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_PRIMARY)
            .setSmallIcon(R.drawable.bg1_splash)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.img_user))
            .setColor(ContextCompat.getColor(this, R.color.accent))
            .setFullScreenIntent(pendingIntent, true)

        Log.e("TAG", "notificationPop: onRemote------------")
        val name = getString(R.string.channel_name)
        val importance = NotificationManager.IMPORTANCE_MAX
        @SuppressLint("WrongConstant") val channel = NotificationChannel(NOTIFICATION_CHANNEL_PRIMARY, name, importance)
        channel.enableVibration(true)
        channel.vibrationPattern = longArrayOf(0, 1000, 800, 1000, 800, 1000, 800, 1000, 100)
        val pm = this.getSystemService(Context.POWER_SERVICE) as PowerManager
        screenLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag")
        screenLock.acquire()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)


        val var9 = NotificationManagerCompat.from(this)
        var9.notify(NOTIFICATION_ID_PRIMARY, builder.build())
    }

}

fun putImageInStorage(storageReference: StorageReference, uri: Uri, key: String, ctx: Context, fromUser: String,
    toUser: String, mDatabase: FirebaseDatabase, MESSAGES_CHILD: String, channel: String, chatActivity: Activity?, intent: Intent) {
    // First upload the image to Cloud Storage
    Log.e("ChatActivity", "putImageInStorage: ")

    storageReference.putFile(uri)
        .addOnSuccessListener(OnSuccessListener { it ->
            it.metadata!!.reference!!.downloadUrl
                .addOnSuccessListener { uri ->
                    Log.e("ChatActivity", "putImageInStorage: " + uri)
                    val c = Calendar.getInstance().time
                    val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                    val formattedDate = df.format(c)
                    val messageModel = MessageModel(
                        fromUser,
                        toUser,
                        uri.toString(),
                        "media-image", System.currentTimeMillis(),
                        false, false, intent
                    )
                    Log.e("ChatActivity", "putImageInStorage: " + uri)
                    if (ChatActivity.chatActivity != null) {
                        ChatActivity.chatActivity!!.finish()
                        ctx.startActivity(intent)
                    }

                    mDatabase.getReference()
                        .child(MESSAGES_CHILD)
                        .child(channel)
                        .child(key)
                        .setValue(messageModel)
                }
        })

        .addOnFailureListener { OnFailureListener { exception -> Log.e(TAG, "Image upload task was not successful.", exception) } }
}
