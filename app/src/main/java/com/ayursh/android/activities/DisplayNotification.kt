package com.ayursh.android.activities

import android.content.*
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ayursh.android.R
import com.ayursh.android.activities.MainActivity.Companion.INTENT_NOTIFICATION_RECEIVE
import com.ayursh.android.adapters.NotificationAdapter
import com.ayursh.android.models.NotificationModel
import com.ayursh.android.utils.SharedPref
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import kotlin.collections.HashMap

class DisplayNotification: AppCompatActivity() {

    var TAG="DisplayNotification"

    var rvNotifications: RecyclerView? = null
    var notificationAdapter: NotificationAdapter? = null
    var gson=Gson()


    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onReceive(context: Context, intent: Intent) {
            updateNotificationBadge()
            Log.e(TAG, "onReceive: MSG RECEIVED")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun updateNotificationBadge() {
        val type = object : TypeToken<MutableList<NotificationModel?>?>() {}.type
        val notificationListString: MutableList<NotificationModel>
        val notifications: MutableList<NotificationModel> = mutableListOf()
        val tempList= mutableListOf<String>()
        if(!SharedPref.Patient.NotificationList.isEmpty()) {
            notificationListString = gson.fromJson(SharedPref.Patient.NotificationList, type)!!
            if(notificationListString.size>=50){
                notificationListString.dropLast(notificationListString.size-40)
            }
            Log.e(TAG, "updateNotificationBadge: \n" + notificationListString)
            if (!notificationListString.isEmpty()) {

                for (i in 0..notificationListString.size-1) {
                    Log.e(TAG, "updateNotificationBadge: "+notificationListString.size+" "+i )
                    if (!notificationListString[i].isRead && notificationListString[i].title.equals("Text Message")) {
                        if(!tempList.contains(notificationListString[i].booking_id)){
                            tempList.add(notificationListString[i].booking_id.toString())
                        }
                    } else{
                        notifications.add(notificationListString[i])
                    }
                }
                for (i in 0..notificationListString.size-1) {
                    if(!notificationListString[i].isRead){
                        if(tempList.contains(notificationListString[i].booking_id.toString())){
                            notifications.add(notificationListString[i])
                            tempList.remove(notificationListString[i].booking_id.toString())
                        }
                    }
                }
                Log.e(TAG, "updateNotificationBadge:\n" + notifications)
                Collections.reverse(notifications)
                notificationAdapter?.setListData(notifications)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.notification_list)
        Log.e(TAG, "onCreate: " )
        registerReceiver(broadcastReceiver, IntentFilter(INTENT_NOTIFICATION_RECEIVE))
        init()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun init() {
        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        toolbar.setNavigationOnClickListener { v: View? -> onBackPressed() }
        rvNotifications = findViewById<View>(R.id.rvNotification) as RecyclerView
        getNotifications()
        updateNotificationBadge()
    }

    private fun getNotifications() {

        val gson = Gson()
        val notificationListString: String= SharedPref.Patient.NotificationList.toString()
        if (!TextUtils.isEmpty(notificationListString)) {
            val type = object : TypeToken<List<NotificationModel?>?>() {}.type
            val notifications: List<NotificationModel?> = gson.fromJson<List<NotificationModel?>>(notificationListString, type)
            Collections.reverse(notifications)
            notificationAdapter = NotificationAdapter(this, notifications as MutableList<NotificationModel>)
            rvNotifications!!.layoutManager = LinearLayoutManager(this)
            rvNotifications!!.adapter = notificationAdapter
            notificationAdapter!!.notifyDataSetChanged()
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

}
