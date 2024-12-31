package com.ayursh.android.activities.agora

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.ayursh.android.R
import com.ayursh.android.databinding.ActivityAudioCallBinding
import com.ayursh.android.models.MyBookingsData
import com.ayursh.android.network.RetrofitClient
import com.ayursh.android.network.responses.MyBookingsResponse
import com.ayursh.android.network.responses.RtcTokenResponse
import com.ayursh.android.utils.*
import com.google.gson.JsonObject
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.Constants
import io.agora.rtc2.RtcEngineConfig
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.math.roundToInt


private const val TAG = "AudioCallActivity"

class AudioCallActivity : AppCompatActivity(), SensorEventListener {

    private var fcm: String?=null
    private lateinit var binder: ActivityAudioCallBinding
    private val PERMISSION_REQ_ID_RECORD_AUDIO = 22
    private val REQUESTED_PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var mRtcEngine: RtcEngine? = null

    var json: JSONObject?=null

    var rtcToken: String? = null
    var channelName: String? = null
    var user: String? = null
    var patientName: String? = null
    val isSwitching = false
    var endcall: ImageView?=null
    var dtmfGenerator : ToneGenerator ?= null
    private var mAudioManager: AudioManager?=null
    var bookingData: MyBookingsData?=null
    var isConnected=false
    private var audioPlayer: MediaPlayer?=null
    var beforeConnection= Timer()




    var time = 0.0

    companion object{
        var audioCall: Activity?=null
        var auto_disconnect = 20000
    }

    private val timer = Timer()
    private val timerTask = object : TimerTask() {
        override fun run() {
            runOnUiThread {
                time++
                binder.callTime.setText(getTimerText())
            }
        }

    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserOffline(uid: Int, reason: Int) { // Tutorial Step 4
            runOnUiThread {
                audioPlayer?.stop()
                Log.e(TAG, "initElements: 1000 " + audioPlayer?.isPlaying)
                binder.status.text = "Ended"
                finish()
                if(timerTask!=null) {
                    timerTask.cancel()
                }
            }
        }

        override fun onUserMuteAudio(uid: Int, muted: Boolean) { // Tutorial Step 6
            runOnUiThread { }
        }

        override fun onUserJoined(i: Int, i1: Int) {
            super.onUserJoined(i, i1)
            isConnected=true
            runOnUiThread {
                Log.e(TAG, "onUserJoined: ")
                audioPlayer?.stop()
                Log.e(TAG, "initElements: 1100 " + audioPlayer?.isPlaying)
                binder.status.text = "Connected"
                timer.scheduleAtFixedRate(timerTask, 0, 1000)
            }
        }

        override fun onLeaveChannel(rtcStats: RtcStats) {
            super.onLeaveChannel(rtcStats)
            if (isSwitching) {
            }
        }
    }


    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null

    private lateinit var pm: PowerManager
    private lateinit var wl: PowerManager.WakeLock

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder = DataBindingUtil.setContentView(this, R.layout.activity_audio_call)
        init()
        audioPlayer = MediaPlayer.create(this, R.raw.basic_tone)
        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mAudioManager?.mode = AudioManager.STREAM_MUSIC
        mAudioManager?.isSpeakerphoneOn = false
        if(!Restarter.incoming) {
            Log.e(TAG, "onCreate: SENDING CALL")
            audioPlayer?.isLooping=true
            audioPlayer?.start()
            Log.e(TAG, "onCreate: " + audioPlayer?.isPlaying)

        } else{
            isConnected=true
        }
        pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "CHESS :")
        audioCall=this
        Restarter.audvid=true


    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun init() {
        initElements()
        initAgora()
        initListeners()
    }


    private fun initElements() {


        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager?;
        mAccelerometer = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        endcall=findViewById(R.id.endCall)


        val bitmap = BlurImage(R.drawable.img_user)
        binder.backgroundImage.setImageDrawable(BitmapDrawable(resources, bitmap))

        if (intent.getStringExtra("rtcToken") == null) {
            showToast("Rtc Token Required")
            audioPlayer?.stop()
            Log.e(TAG, "initElements: 1 " + audioPlayer?.isPlaying)
            finish()
            return
        }
        if (intent.getStringExtra("channel") == null) {
            showToast("Channel Required")
            audioPlayer?.stop()
            Log.e(TAG, "initElements: 2 " + audioPlayer?.isPlaying)
            finish()
            return
        }
        if (intent.getStringExtra("patient_name") == null) {
            showToast("Patient Name Required")
            audioPlayer?.stop()
            Log.e(TAG, "initElements: 3 " + audioPlayer?.isPlaying)
            finish()
            return
        }
        patientName = intent.getStringExtra("patient_name")
        rtcToken = intent.getStringExtra("rtcToken")
        channelName = intent.getStringExtra("channel")
        json=JSONObject(intent.getStringExtra("json"))
        bookingData= intent.getSerializableExtra("booking") as MyBookingsData?
        Log.e(TAG, "initElements: " + json)

        fcm=intent.getStringExtra("fcm")
        user = SharedPref.User.DOC_ID
        binder.user.text = patientName


    }

    private fun initAgora() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
           try {
               mRtcEngine =   RtcEngine.create(baseContext, getString(R.string.agora_app_id), mRtcEventHandler)
            } catch (e: Exception) {
                Log.e(TAG, Log.getStackTraceString(e))
                throw RuntimeException("""NEED  TO check rtc sdk init fatal error ${Log.getStackTraceString(e)} """.trimIndent())
            }
        }

        try {

            Log.e(TAG, "initElements: Chan $channelName")
            Log.e(TAG, "initElements: User $user")
            Log.e(TAG, "initElements: rtcToken $rtcToken")
            Log.e(TAG, "initElements: state ${mRtcEngine!!.connectionState}")

            mRtcEngine!!.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            mRtcEngine!!.joinChannelWithUserAccount(rtcToken, channelName, user)
        } catch (e: Exception){
            showToast("Unable to set profile", false)
            Log.e(TAG, "initAgora: " + e)
        }

        try {
            if (!Restarter.incoming) {
                Log.e(TAG, "initAgora: Test call")
                beforeConnection = Timer()
                beforeConnection.schedule(object : TimerTask() {
                    override fun run() {
                        Log.e(TAG, "run: CANCEL CALL AUTO ")
                        if (!isConnected) {
                            mRtcEngine?.leaveChannel()
                            RtcEngine.destroy()
                            timerTask.cancel()
                            Log.e(TAG, "run: --->" + audioPlayer?.isPlaying)
                            mRtcEngine = null
                            audioPlayer?.stop();
                            Log.e(TAG, "run: " + audioPlayer?.isPlaying)
                            val sendJson = JsonObject()
                            Log.e(TAG, "initListeners: " + json)
                            val Json = JsonObject()

                            if (json?.has("data")!!) {
                                Log.e(TAG, "run: LOCAL INVITATION CANCEL")
                                val token = JSONObject(json?.getString("message"))

                                val message = JsonObject()
                                message.addProperty("token", token.getString("token"))
                                Json.add("message", message)
                                Json.addProperty("title", "Audio Call")
                                val tempjson = JSONObject(json?.getString("data"))
                                Json.addProperty("channel", tempjson.getString("channel"))
                                Json.addProperty("doc_name", tempjson.getString("doc_name"))
                                Json.addProperty("doc_id", tempjson.getString("doc_id"))
                                Json.addProperty("doc_image", tempjson.getString("doc_image"))
                                Json.addProperty("booking_id", tempjson.getString("booking_id"))
                                Json.addProperty("type", "cancelremoteinvitation")
                                sendJson.add("data", Json)
                                LocalInvitationCancel(sendJson);

                            }
                            finish()
                        }
                    }
                }, 20000)
            } else {
                Log.e(TAG, "initAgora: ELSE PART")
            }
        } catch (e: Exception){
            e.printStackTrace();
        }
        // Sets the channel profile of the Agora RtcEngine.
        // CHANNEL_PROFILE_COMMUNICATION(0): (Default) The Communication profile. Use this profile in one-on-one calls or group calls, where all users can talk freely.
        // CHANNEL_PROFILE_LIVE_BROADCASTING(1): The Live-Broadcast profile. Users in a live-broadcast channel have a role as either broadcaster or audience. A broadcaster can both send and receive streams; an audience can only receive streams.
        // Sets the channel profile of the Agora RtcEngine.
        // CHANNEL_PROFILE_COMMUNICATION(0): (Default) The Communication profile. Use this profile in one-on-one calls or group calls, where all users can talk freely.
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initListeners() {
        binder.btnMuteUnMute.setOnClickListener { onLocalAudioMuteClicked(it) }
        binder.btnSpeakerOnOff.setOnClickListener { onSwitchSpeakerphoneClicked(it) }


        binder.endCall.setOnClickListener {
            Log.e(TAG, "initListeners: ENDCALL")
            isConnected=true
            if(Restarter.incoming){
                audioPlayer?.stop()
                Log.e(TAG, "initElements: 100 " + audioPlayer?.isPlaying)
                RefuseRemoteInvitation(this, "Audio Call", fcm.toString())
                finish()
            }
            else {
                // send fcm to cancel remote invitatio
                mRtcEngine?.leaveChannel()
                RtcEngine.destroy()
                timerTask.cancel()
                mRtcEngine = null
                dtmfGenerator?.stopTone()
                audioPlayer?.stop()
                val sendJson= JsonObject()
                Log.e(TAG, "initListeners: " + json)
                val Json= JsonObject()
                Log.e(TAG, "initElements: 200 " + audioPlayer?.isPlaying)

                if(json?.has("data")!!){
                    val token = JSONObject(json?.getString("message"))

                    val message = JsonObject()
                    message.addProperty("token", token.getString("token"))
                    Json.add("message", message)
                    Json.addProperty("title", "Audio Call")
                    val tempjson=JSONObject(json?.getString("data"))
                    Json.addProperty("channel", tempjson.getString("channel"))
                    Json.addProperty("doc_name", tempjson.getString("doc_name"))
                    Json.addProperty("doc_id", tempjson.getString("doc_id"))
                    Json.addProperty("doc_image", tempjson.getString("doc_image"))
                    Json.addProperty("booking_id", tempjson.getString("booking_id"))
                    Json.addProperty("type", "cancelremoteinvitation")
                    sendJson.add("data", Json)
                    LocalInvitationCancel(sendJson);

                } else{

                    json?.getString("fcm")?.let { it1 -> RefuseRemoteInvitation(this, "Audio Call", it1) }
                }

                finish()


            }
        }

    }

    private fun getTimerText(): String? {
        val rounded = time.roundToInt()
        val seconds = rounded % 86400 % 3600 % 60
        val minutes = rounded % 86400 % 3600 / 60
        val hours = rounded % 86400 / 3600
        return formatTime(seconds, minutes, hours)
    }

    private fun formatTime(seconds: Int, minutes: Int, hours: Int): String {
        return if (hours > 0) String.format("%02d", hours) + " : " + String.format("%02d", minutes) + " : " + String.format("%02d", seconds) else String.format("%02d", minutes) + " : " + String.format("%02d", seconds)
    }

    override fun onBackPressed() {

    }


    @SuppressLint("UseCompatLoadingForDrawables")
    fun onLocalAudioMuteClicked(view: View) {
        val iv = view as ImageView
        if (iv.isSelected) {
            iv.isSelected = false
            iv.setImageDrawable(resources.getDrawable(R.drawable.ic_mic_unmute))
        } else {
            iv.isSelected = true
            iv.setImageDrawable(resources.getDrawable(R.drawable.ic_mic_mute))
        }

        // Stops/Resumes sending the local audio stream.
        mRtcEngine!!.muteLocalAudioStream(iv.isSelected)
    }

    private fun onSwitchSpeakerphoneClicked(view: View) {
        val iv = view as ImageView
        if (iv.isSelected) {
            iv.isSelected = false
            iv.setImageDrawable(resources.getDrawable(R.drawable.ic_speaker_off))
        } else {
            iv.isSelected = true
            iv.setImageDrawable(resources.getDrawable(R.drawable.ic_speaker_on))
        }
        mRtcEngine!!.setEnableSpeakerphone(view.isSelected())
    }


    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        Log.e(TAG, "checkSelfPermission $permission $requestCode")
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode)
            return false
        }
        return true
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(requestCode: Int,
        permissions: Array<String?>, grantResults: IntArray) {
        Log.e(TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode)
        when (requestCode) {
            PERMISSION_REQ_ID_RECORD_AUDIO -> {
                if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initAgora()
                } else {
                    showToast("No permission for " + Manifest.permission.RECORD_AUDIO)
                    dtmfGenerator?.stopTone()
                    audioPlayer?.stop()
                    Log.e(TAG, "initElements: 400 " + audioPlayer?.isPlaying)
                    finish()
                }
            }
        }
    }

    private fun leaveChannel() {
        if (mRtcEngine != null) {
            mRtcEngine?.leaveChannel()
            RtcEngine.destroy()
            timerTask.cancel()
            mRtcEngine = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveChannel()
        beforeConnection.cancel()
        Log.e(TAG, "onDestroy: " + audioPlayer?.isPlaying)
        audioPlayer?.stop();
       // audioPlayer?.release()
        Log.e(TAG, "onDestroy: " + audioPlayer?.isPlaying)
    }


    override fun onSensorChanged(event: SensorEvent?) {

        if (event!!.values[0] < event.sensor.maximumRange) {

        } else {
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.e(TAG, "onAccuracyChanged: $sensor  $accuracy")
    }


    override fun onResume() {
        super.onResume()
        mSensorManager?.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager?.unregisterListener(this)
    }


}