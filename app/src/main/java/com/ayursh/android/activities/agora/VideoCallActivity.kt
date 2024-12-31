package com.ayursh.android.activities.agora

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.ayursh.android.R
import com.ayursh.android.databinding.ActivityVideoCallBinding
import com.ayursh.android.models.MyBookingsData
import com.ayursh.android.utils.*
import com.google.gson.JsonObject
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoEncoderConfiguration
import io.agora.rtc2.video.VideoCanvas
import org.json.JSONException
import org.json.JSONObject
import java.util.*


private const val TAG = "VideoCallActivity"

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class VideoCallActivity : AppCompatActivity() {
    private var mRtcEngine: RtcEngine? = null
    private val PERMISSION_REQ_ID = 22
    private val REQUESTED_PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    private lateinit var binder: ActivityVideoCallBinding
    var endcall: ImageView? = null
    var rtcToken: String? = null
    var channelName: String? = null
    var user: String? = null
    var bookingData: MyBookingsData? = null
    var fcm: String? = null
    var json: JSONObject? = null
    var isConnected = false
    var audioManager: AudioManager? = null
    var beforeConnection = Timer()

    companion object {
        @SuppressLint("StaticFieldLeak")
        var videoCall: Activity? = null
        var videoPlayer: MediaPlayer? = null
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onFirstRemoteVideoDecoded(uid: Int, width: Int, height: Int, elapsed: Int) {
            Log.e(TAG, "onFirstRemoteVideoDecoded: VIDEO ESTABLISHED")
            isConnected = true
            if (videoPlayer != null && videoPlayer?.isPlaying == true) {
                videoPlayer?.stop()
            }
            runOnUiThread { // set first remote user to the main bg video container
                if (binder.bgVideoContainer.childCount >= 1) {
                    return@runOnUiThread
                }
                val videoSurface = RtcEngine.CreateRendererView(baseContext)
                binder.bgVideoContainer.addView(videoSurface)
                mRtcEngine?.setupRemoteVideo(VideoCanvas(videoSurface, VideoCanvas.RENDER_MODE_FIT, uid))
                mRtcEngine?.setRemoteSubscribeFallbackOption(2)
                Log.e(TAG, "onFirstRemoteVideoDecoded: videocall connected")
            }
        }

        // remote user has left channel
        override fun onUserOffline(uid: Int, reason: Int) { // Tutorial Step 7
            runOnUiThread {
                removeVideo(R.id.bg_video_container)
                onLeaveChannel()
            }
        }

        // remote user has toggled their video
        override fun onUserMuteVideo(uid: Int, toggle: Boolean) { // Tutorial Step 10
            runOnUiThread {
                Log.e(TAG, "onUserMuteVideo: " + toggle)
                try {
                    val videoSurface = binder.bgVideoContainer.getChildAt(0) as SurfaceView
                    videoSurface.visibility = if (toggle) View.GONE else View.VISIBLE
                    if (toggle) {
                        val noCamera = ImageView(this@VideoCallActivity)
                        noCamera.setImageResource(R.drawable.ic_videocam_off)
                        binder.bgVideoContainer.addView(noCamera)
                    } else {
                        val noCamera = binder.bgVideoContainer.getChildAt(1) as ImageView
                        if (noCamera != null) {
                            binder.bgVideoContainer.removeView(noCamera)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onUserMuteVideo: " + e.localizedMessage)
                }

            }
        }
    }

    @SuppressLint("WrongViewCast", "WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate: ViedoCall Activity")
        binder = DataBindingUtil.setContentView(this, R.layout.activity_video_call)
        videoPlayer = MediaPlayer.create(this, R.raw.basic_tone)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager?.mode = AudioManager.STREAM_MUSIC
        audioManager?.isSpeakerphoneOn = false
        videoCall = this
        Restarter.audvid = true
        if (Restarter.incoming) {
            isConnected = true
        }

        init()
    }

    private fun init() {
        val bitmap = BlurImage(R.drawable.img_user)
        binder.bgVideoContainer.background = BitmapDrawable(resources, bitmap)

        initElements()
        initIntentData()
        initAgora()
        initListeners()
    }

    override fun onBackPressed() {
        // super.onBackPressed()
    }

    private fun initElements() {
        binder.audioBtn.visibility = View.GONE // set the audio button hidden
        binder.leaveBtn.visibility = View.GONE // set the leave button hidden
        binder.videoBtn.visibility = View.GONE // set the video button hidden
        endcall = findViewById(R.id.leaveBtn)

    }

    private fun initIntentData() {
        if (intent.getStringExtra("rtcToken") == null) {
            showToast("Rtc Token Required")
            finish()
            return
        }
        if (intent.getStringExtra("channel") == null) {
            showToast("Channel Required")
            finish()
            return
        }
        try {
            json = JSONObject(intent.getStringExtra("json"))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        Log.e(TAG, "initIntentData: " + json)
        rtcToken = intent.getStringExtra("rtcToken")
        channelName = intent.getStringExtra("channel")
        bookingData = intent.getSerializableExtra("booking") as MyBookingsData?
        Log.e(TAG, "initIntentData: " + bookingData)
        fcm = intent.getStringExtra("fcm")
        user = SharedPref.User.DOC_ID
    }


    private fun initAgora() {
        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) && checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID)) {
            mRtcEngine = try {
                Log.e(TAG, "initAgora: ================")
                RtcEngine.create(baseContext, getString(R.string.agora_app_id), mRtcEventHandler)
            } catch (e: Exception) {
                Log.e(TAG, Log.getStackTraceString(e))
                throw RuntimeException(""" NEED TO check rtc sdk init fatal error : ${Log.getStackTraceString(e)} """.trimIndent())
            }
            mRtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            mRtcEngine?.enableVideo()
            mRtcEngine?.setVideoEncoderConfiguration(VideoEncoderConfiguration(VideoEncoderConfiguration.VD_640x480, VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT))
            Handler().postDelayed({
                binder.joinBtn.performClick()
            }, 700)
        }

        if (!Restarter.incoming) {
            videoPlayer?.isLooping = true
            videoPlayer?.start()
            Log.e(TAG, "initAgora: Test call")
            this.beforeConnection = Timer()
            beforeConnection.schedule(object : TimerTask() {

                override fun run() {
                    Log.e(TAG, "run: CANCEL CALL AUTO ")
                    if (!isConnected) {
                        val sendJson = JsonObject()
                        Log.e(TAG, "initListeners: " + json)
                        val Json = JsonObject()
                        if (json?.has("data")!!) {
                            val token = JSONObject(json?.getString("message"))

                            val message = JsonObject()
                            message.addProperty("token", token.getString("token"))
                            Json.add("message", message)
                            Json.addProperty("title", "Video Call")
                            val tempjson = JSONObject(json?.getString("data"))
                            Json.addProperty("channel", tempjson.getString("channel"))
                            Json.addProperty("doc_name", tempjson.getString("doc_name"))
                            Json.addProperty("doc_id", tempjson.getString("doc_id"))
                            Json.addProperty("doc_image", tempjson.getString("doc_image"))
                            Json.addProperty("booking_id", tempjson.getString("booking_id"))
                            Json.addProperty("type", "cancelremoteinvitation")
                            sendJson.add("data", Json)

                            Log.e(TAG, "initListeners: " + sendJson)

                            LocalInvitationCancel(sendJson);
                        }

                        mRtcEngine?.leaveChannel()
                        RtcEngine.destroy()
                        mRtcEngine = null
                        finish()
                    }
                }
            }, 20000)
        } else {
            Log.e(TAG, "initAgora: ELSE PART")
        }
    }

    private fun initListeners() {
        Log.e(TAG, "initListeners: ++++++++++++++++++++++++++++++")

        //tvName.setText(rtcToken);
        binder.leaveBtn.setOnClickListener {
            isConnected = true
            if (videoPlayer != null && videoPlayer?.isPlaying == true) {
                videoPlayer?.stop()
            }
            Log.e(TAG, "initListeners: ENDCALL")
            val sendJson = JsonObject()
            Log.e(TAG, "initListeners: " + json)
            val Json = JsonObject()
            val token = JSONObject(json?.getString("message"))

            val message = JsonObject()
            message.addProperty("token", token.getString("token"))
            Json.add("message", message)
            Json.addProperty("title", "Video Call")
            Json.addProperty("channel", json?.getString("channel"))
            Json.addProperty("doc_name", json?.getString("doc_name"))
            Json.addProperty("doc_id", json?.getString("doc_id"))
            Json.addProperty("doc_image", json?.getString("doc_image"))
            Json.addProperty("booking_id", json?.getString("booking_id"))
            Json.addProperty("type", json?.getString("cancelremoteinvitation"))
            sendJson.add("data", Json)
            LocalInvitationCancel(sendJson);
            removeVideo(R.id.floating_video_container)
            removeVideo(R.id.bg_video_container)
            mRtcEngine?.leaveChannel()
            RtcEngine.destroy()
            mRtcEngine = null
            finish()
        }

        binder.bgVideoContainer.setOnClickListener {

            if (binder.tvName.visibility == View.VISIBLE) {
                binder.tvName.visibility = View.GONE
            } else {
                binder.tvName.visibility = View.VISIBLE
            }
            if (binder.llButtons.visibility == View.VISIBLE) {
                binder.llButtons.visibility = View.GONE
            } else {
                binder.llButtons.visibility = View.VISIBLE
            }
        }
        try {
            binder.joinBtn.setOnClickListener {
                Log.e(TAG, "initListeners: JointButton**********")
                onJoinChannel()
            }
            binder.leaveBtn.setOnClickListener {
                isConnected = true
                Log.e(TAG, "initListeners: leftChannel**********")
                val sendJson = JsonObject()
                Log.e(TAG, "initListeners: " + json)
                val Json = JsonObject()
                if (json?.has("data")!!) {
                    val token = JSONObject(json?.getString("message"))

                    val message = JsonObject()
                    message.addProperty("token", token.getString("token"))
                    Json.add("message", message)
                    Json.addProperty("title", "Video Call")
                    val tempjson = JSONObject(json?.getString("data"))
                    Json.addProperty("channel", tempjson.getString("channel"))
                    Json.addProperty("doc_name", tempjson.getString("doc_name"))
                    Json.addProperty("doc_id", tempjson.getString("doc_id"))
                    Json.addProperty("doc_image", tempjson.getString("doc_image"))
                    Json.addProperty("booking_id", tempjson.getString("booking_id"))
                    Json.addProperty("type", "cancelremoteinvitation")
                    sendJson.add("data", Json)
                    LocalInvitationCancel(sendJson);

                } else {
                    json?.getString("fcm")
                        ?.let { it1 -> RefuseRemoteInvitation(this, "Video Call", it1) }
                }

                removeVideo(R.id.floating_video_container)
                removeVideo(R.id.bg_video_container)
                mRtcEngine?.leaveChannel()
                RtcEngine.destroy()
                mRtcEngine = null
                finish()
            }
            binder.videoBtn.setOnClickListener { onVideoMuteUnMute(it) }
            binder.audioBtn.setOnClickListener { onAudioMuteUnMute(it) }
        } catch (e: Exception) {
            Log.e(TAG,
                "initListeners: ${e.printStackTrace()}")
        }

    }

    private fun onAudioMuteUnMute(view: View) {
        val btn = view as ImageView
        if (btn.isSelected) {
            btn.isSelected = false
            btn.setImageResource(R.drawable.ic_mic_unmute)
        } else {
            btn.isSelected = true
            btn.setImageResource(R.drawable.ic_mic_mute)
        }

        mRtcEngine?.muteLocalAudioStream(btn.isSelected)
    }

    private fun onVideoMuteUnMute(view: View) {
        val btn = view as ImageView
        if (btn.isSelected) {
            btn.isSelected = false
            btn.setImageResource(R.drawable.ic_videocam_on)
        } else {
            btn.isSelected = true
            btn.setImageResource(R.drawable.ic_videocam_off)
        }

        mRtcEngine?.muteLocalVideoStream(btn.isSelected)

        binder.floatingVideoContainer.visibility = if (btn.isSelected) View.GONE else View.VISIBLE
        val videoSurface = binder.floatingVideoContainer.getChildAt(0) as SurfaceView
        videoSurface.setZOrderMediaOverlay(!btn.isSelected)
        videoSurface.visibility = if (btn.isSelected) View.GONE else View.VISIBLE
    }

    private fun onLeaveChannel() {
        //startService(Intent(this@VideoCallActivity, CallServiceActivity::class.java))
        Log.e(TAG, "onLeaveChannel: ________________")
        removeVideo(R.id.floating_video_container)
        removeVideo(R.id.bg_video_container)
        rtcToken = null
        leaveChannel()
        finish() // set the video button hidden
    }

    private fun leaveChannel() {
        if (mRtcEngine != null)
            mRtcEngine?.leaveChannel()
    }

    private fun onJoinChannel() {

        mRtcEngine?.joinChannelWithUserAccount(rtcToken, channelName, user) // if you do not specify the uid, Agora will assign one.
        Log.e(TAG, "onJoinChannel: CHANNEL JOINEDDD")
        val videoSurface = RtcEngine.CreateRendererView(baseContext)
        videoSurface.setZOrderMediaOverlay(true)
        binder.floatingVideoContainer.addView(videoSurface)
        mRtcEngine?.setupLocalVideo(VideoCanvas(videoSurface, VideoCanvas.RENDER_MODE_FIT, 0))
        binder.joinBtn.visibility = View.GONE // set the join button hidden
        binder.audioBtn.visibility = View.VISIBLE // set the audio button hidden
        binder.leaveBtn.visibility = View.VISIBLE // set the leave button hidden
        binder.videoBtn.visibility = View.VISIBLE
    }

    private fun removeVideo(containerID: Int) {
        val videoContainer = findViewById<FrameLayout>(containerID)
        videoContainer.removeAllViews()
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        Log.e(TAG, "checkSelfPermission $permission $requestCode")
        if (ContextCompat.checkSelfPermission(this,
                permission)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                REQUESTED_PERMISSIONS,
                requestCode)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQ_ID -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Need permissions " + Manifest.permission.RECORD_AUDIO + "/" + Manifest.permission.CAMERA)
                }
                initAgora()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveChannel()
        RtcEngine.destroy()
        beforeConnection.cancel()
        if (videoPlayer != null && videoPlayer?.isPlaying == true) {
            videoPlayer?.stop()
        }
        mRtcEngine = null
    }

}