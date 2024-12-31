package com.ayursh.android.adapters

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ayursh.android.R
import com.ayursh.android.activities.service.MyFirebaseMessagingService
import com.ayursh.android.fragments.Changebadge
import com.ayursh.android.models.NotificationModel
import com.ayursh.android.utils.Constants
import com.ayursh.android.utils.Restarter
import com.ayursh.android.utils.SharedPref
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class NotificationAdapter(private var context: Context, listData: MutableList<NotificationModel>) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    private var listData= listData
    private val gson = Gson()

//    fun NotificationAdapter(aContext: Context?, listData: List<NotificationModel?>?) {
//        context = aContext!!
//        this.listData = listData as MutableList<NotificationModel>
//    }

    fun getListData(): MutableList<NotificationModel> {
        return listData
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.item_notification, parent, false))


    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification: NotificationModel = listData[position]


 //       Log.e("NotificationAdapter", "onBindViewHolder: " + listData)

        holder.vStatus.visibility =if (notification.isRead()) View.INVISIBLE else View.VISIBLE
        holder.tvBody.setText(notification.getBody())
        holder.tvTitle.text = notification.getTitle()
        var title=""

        if (notification.getTitle().equals("Text Message")) { //&& MyFirebaseMessagingService.notificationhascount.get(notification.getBooking_id())==1 || notification.isRead()
         //   Log.e("NotificationAdapter", "onBindViewHolder: " + MyFirebaseMessagingService.notificationhascount + " " + position)

             if(!notification.isRead) {
                 if (MyFirebaseMessagingService.notificationhascount.containsKey(notification.getBooking_id())) {
                     title = if (MyFirebaseMessagingService.notificationhascount[notification.getBooking_id()] === 1) {

                         MyFirebaseMessagingService.notificationhascount[notification.getBooking_id()].toString() + " Text message"
                     } else {
                         MyFirebaseMessagingService.notificationhascount[notification.getBooking_id()].toString() + " Text messages"
                     }
                     holder.tvTitle.text = title

                 }
             }
        } else {
            Log.e("CALL_LIST", "onBindViewHolder: "+notification )

            holder.tvTitle.text = notification.getTitle()
        }


        if (notification.getTitle().equals("payment", true)) {
            holder.tvTitle.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        } else if (notification.getTitle().equals("therapy", true)) {
            holder.tvTitle.setBackgroundColor(ContextCompat.getColor(context, com.kal.rackmonthpicker.R.color.lite_blue))
        } else {
            holder.tvTitle.setBackgroundColor(ContextCompat.getColor(context, R.color.accent))
        }
        holder.itemView.setOnClickListener { view: View? ->
            notification.isRead=true
            notification.setRead(true)
            if(MyFirebaseMessagingService.notificationhascount!=null) {
                MyFirebaseMessagingService.notificationhascount[notification.getBooking_id()!!] = 0
            }
            SharedPref.Patient.NotificationList=gson.toJson(listData)
            notifyItemChanged(position)
            updateBadge()
        }

    }

    private fun updateBadge() {
        val notificationListString: String = SharedPref.Patient.NotificationList
        if (!TextUtils.isEmpty(notificationListString)) {
            val type = object : TypeToken<List<NotificationModel?>?>() {}.type
            Log.e("TAG", "updateNotificationBadge: $notificationListString")
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

            if(count > 0) {
                Changebadge().showbadge()
            } else {
                Changebadge().hidebadge()
            }
        } else {
            Changebadge().hidebadge()
        }

    }

    override fun getItemCount(): Int {
        return listData.size
    }

    fun setListData(notifications: MutableList<NotificationModel>) {
        listData.clear()
        listData = notifications
        notifyDataSetChanged()
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var vStatus: View = itemView.findViewById(R.id.vStatus)
        var tvTitle: TextView =  itemView.findViewById(R.id.tvTitle)

        var tvBody: TextView =  itemView.findViewById(R.id.tvBody)

        var tvTimeStamp: TextView =  itemView.findViewById(R.id.tvTimeStamp)

    }

    init {
        this.listData = listData
    }
}
