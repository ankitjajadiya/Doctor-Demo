package com.ayursh.android.activities.agora

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.ayursh.android.R
import com.ayursh.android.activities.SplashActivity
import com.ayursh.android.activities.service.MyFirebaseMessagingService
import com.ayursh.android.adapters.MessageModelViewHolder
import com.ayursh.android.adapters.MyButtonObserver
import com.ayursh.android.adapters.MyScrollToBottomObserver
import com.ayursh.android.databinding.ActivityChatBinding
import com.ayursh.android.models.*
import com.ayursh.android.network.RetrofitClient
import com.ayursh.android.utils.*
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.JsonObject
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


private const val TAG = "ChatActivity"

class ChatActivity : AppCompatActivity() {
    private var mSharedPreferences: SharedPreferences? = null
    private lateinit var mBinding: ActivityChatBinding
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private lateinit var mFirebaseAuth: FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase
    private lateinit var mFirebaseAdapter: FirebaseRecyclerAdapter<MessageModel, MessageModelViewHolder>

    private lateinit var user: String
    private lateinit var channel: String
    private lateinit var fromUser: String
    private lateinit var toUser: String
    private lateinit var bookingData: MyBookingsData

    private val MESSAGES_CHILD = "channels"
    private val DEFAULT_MSG_LENGTH_LIMIT = 10
    private val REQUEST_INVITE = 1
    private val REQUEST_IMAGE = 2
    private val MESSAGE_URL = "http://friendlychat.firebase.google.com/message/"
    private val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
    private val MESSAGE_SENT_EVENT = "message_sent"
    public var result: String?=null
    public val header: MutableMap<String, String> = HashMap()
    val timer=Timer()
    var handler: Handler = Handler()

    companion object{
        var chatActivity: Activity? =null
        var intentchat: Intent?=null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MyFirebaseMessagingService.isChatActivityOpened=true
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_chat)
        chatActivity=this
        init()
    }

    private fun init() {
      //  autorefresh()
        initElements()
        initListeners()
    }



    private fun initElements() {
        intentchat=intent
        if (intent.getSerializableExtra("booking") == null) {
            showToast("Requires Booking Data")
            finish()
            return
        }
        bookingData = intent.getSerializableExtra("booking") as MyBookingsData
        user = if (bookingData.prescription!=null) bookingData.prescription.patient_name else bookingData.booking_for_name
        mBinding.userName.text = user
        channel = bookingData.user_consultation_booking_id
        fromUser = bookingData.doctor_id
        toUser = bookingData.user_id

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mFirebaseAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
        mLinearLayoutManager = LinearLayoutManager(this)
        mLinearLayoutManager.stackFromEnd = true
        mBinding.messageRecyclerView.layoutManager = mLinearLayoutManager
        initFirebaseAdapter()
    }

    private fun initListeners() {
        mBinding.sendButton.setOnClickListener {
            val c = Calendar.getInstance().time
            val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
            val messageModel = MessageModel(fromUser,
                toUser,
                mBinding.messageEditText.text.toString(),
                "text", c.time,
                false, false, intent
            )
            FirebaseApp.initializeApp(this)
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task: Task<String?> ->
                    if (!task.isSuccessful) {
                        Log.w(SplashActivity::class.java.simpleName, "Fetching FCM registration token failed", task.exception)
                        return@addOnCompleteListener
                    }

                    // Get new FCM registration token
                    var token = task.result
                    if (token != null) {
                        Log.e(TAG, token)
                        //SharedPref.User.fcm_token= token.toString()
                    }
                }
            mDatabase.reference.child(MESSAGES_CHILD).child(channel).push().setValue(messageModel)
                .addOnCompleteListener(object : OnCompleteListener<Void> {
                    override fun onComplete(task: Task<Void>) {
                        if (task.isComplete) {
                            Log.e(TAG, "onComplete: Inside notification")
                            val user_fcm = bookingData.fcm_token

                            val json = JsonObject()
                            val notiJson = JsonObject()
                            notiJson.addProperty("body", SharedPref.User.USER.display_name)
                            notiJson.addProperty("title", "Text Message")
                            notiJson.addProperty("booking_id", bookingData.booking_id)
                            notiJson.addProperty("doc_image", bookingData.doctor_display_image)
                            notiJson.addProperty("fcm", bookingData.fcm_token)
                            notiJson.addProperty("toUser", bookingData.doctor_id)
                            notiJson.addProperty("consultation_id", bookingData.user_consultation_booking_id)
                            json.addProperty("to", user_fcm)

                            val message = JsonObject()
                            message.addProperty("token", user_fcm)
                            json.add("message", message)
                            json.add("data", notiJson)
                            if (checkInternetConnection()) {
                                RetrofitClient.create(this@ChatActivity)
                                    .sendnotification("Bearer ${SharedPref.User.AUTH_TOKEN}", json)
                                    .enqueue(object : Callback<ResponseBody> {
                                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                            Log.e(TAG, "done " + response.body()?.string()
                                                .toString())
                                        }

                                        override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                                            Log.e(TAG, "onFailure: ")
                                        }
                                    })
                            } else {
                                showToast("Please check your Internet Connection", false)
                            }

                        } else {
                            Log.e(TAG, "onComplete: incomplete")

                        }

                    }

                })
            mBinding.messageEditText.setText("")
        }
        mBinding.addMessageImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE)
        }
    }

    private fun initFirebaseAdapter() {

        val messagesRef: DatabaseReference = mDatabase.reference.child(MESSAGES_CHILD)
            .child(channel)
//        val query:Query = messagesRef.orderByChild("from_uid").equalTo("");
        val options: FirebaseRecyclerOptions<MessageModel> = FirebaseRecyclerOptions.Builder<MessageModel>()
            .setQuery(messagesRef, MessageModel::class.java)
            .build()

        messagesRef.get().addOnSuccessListener {
            if (!it.exists()) {
                mBinding.progressBar.visibility = View.GONE
            }
        }
        mFirebaseAdapter = object : FirebaseRecyclerAdapter<MessageModel, MessageModelViewHolder>(options) {

            override fun getItemCount(): Int {
                if (super.getItemCount() >= 1) {
                    mBinding.progressBar.visibility = View.GONE
                }
                return super.getItemCount()
            }

            override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MessageModelViewHolder {
                val inflater: LayoutInflater = LayoutInflater.from(viewGroup.context)
                Log.e(TAG, "onCreateViewHolder: message recieved")
                return MessageModelViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false))
            }

            override fun onBindViewHolder(vh: MessageModelViewHolder, position: Int, message: MessageModel) {
                if(message.message!=null || message.message!="")
                vh.bindMessage(message, this@ChatActivity, fromUser)
            }
        }
        mFirebaseAdapter.startListening()
        mBinding.messageRecyclerView.adapter = mFirebaseAdapter
        initScrollObserver()
    }

    private fun initScrollObserver() {
        mFirebaseAdapter.registerAdapterDataObserver(MyScrollToBottomObserver(mBinding.messageRecyclerView, mFirebaseAdapter, mLinearLayoutManager))
        mBinding.messageEditText.addTextChangedListener(MyButtonObserver(mBinding.sendButton))
    }



    override fun onPause() {
        //  mFirebaseAdapter.stopListening()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
//        mFirebaseAdapter.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        chatActivity=null

        try {

            val bookingData: MyBookingsData = intent.getSerializableExtra("booking") as MyBookingsData


            channel = bookingData.user_consultation_booking_id
            val messagesRef: DatabaseReference = FirebaseDatabase.getInstance().reference.child(MESSAGES_CHILD)
                .child(channel)
            val options: FirebaseRecyclerOptions<MessageModel> = FirebaseRecyclerOptions.Builder<MessageModel>()
                .setQuery(messagesRef, MessageModel::class.java)
                .build()
            mFirebaseAdapter = object : FirebaseRecyclerAdapter<MessageModel, MessageModelViewHolder>(options) {

                override fun getItemCount(): Int {
                    if (super.getItemCount() >= 1) {
                        mBinding.progressBar.visibility = View.GONE
                    }
                    return super.getItemCount()
                }

                override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MessageModelViewHolder {
                    val inflater: LayoutInflater = LayoutInflater.from(viewGroup.context)
                    return MessageModelViewHolder(inflater.inflate(R.layout.item_message, viewGroup, false))
                }

                override fun onBindViewHolder(vh: MessageModelViewHolder, position: Int, message: MessageModel) {
                    vh.bindMessage(message, this@ChatActivity, fromUser)
                }
            }
            mFirebaseAdapter.stopListening()
        } catch (e: Exception){
            Toast.makeText(this, "Couldn't load Chat", Toast.LENGTH_LONG).show()
        }
    }

    fun onBackBtn(view: View?) {
        MyFirebaseMessagingService.isChatActivityOpened=false
        chatActivity=null
        finish()
        if(timer!=null){
            timer.cancel()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val uri: Uri = data.data!!
                val c = Calendar.getInstance().time
                val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
                val formattedDate = df.format(c)
                val messageModel = MessageModel(fromUser,
                    toUser,
                    "",
                    "media-image", System.currentTimeMillis(),
                    false, false, intent
                )
                mDatabase.reference.child(MESSAGES_CHILD).child(channel).push()
                    .setValue(messageModel, object : DatabaseReference.CompletionListener {
                        override fun onComplete(databaseError: DatabaseError?,
                            databaseReference: DatabaseReference) {
                            Log.e(TAG, "onComplete: " + uri)

                            if (databaseError != null) {
                                Log.w(TAG, "Unable to write message to database.",
                                    databaseError.toException())
                                return
                            }

                            // Build a StorageReference and then upload the file
                            val key: String = databaseReference.key!!
                            val storageReference: StorageReference = FirebaseStorage.getInstance()
                                .getReference(channel)
                                .child(key)
                                .child(uri.lastPathSegment.toString())

                            putImageInStorage(storageReference, uri, key, this@ChatActivity, fromUser,
                                toUser, mDatabase, MESSAGES_CHILD, channel, chatActivity, intent)

                        }
                    })


            }
        }
    }

}