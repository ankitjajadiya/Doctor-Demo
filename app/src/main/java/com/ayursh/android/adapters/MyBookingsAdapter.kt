package com.ayursh.android.adapters

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ayursh.android.R
import com.ayursh.android.activities.BookingDetailActivity
import com.ayursh.android.activities.MainActivity.Companion.INTENT_NOTIFICATION_RECEIVE
import com.ayursh.android.models.MyBookingsData

class MyBookingsAdapter(var list: List<MyBookingsData>, var context: Context) : RecyclerView.Adapter<MyBookingsAdapter.ViewHolder>() {

    public lateinit var cntnoti: ImageView




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_bookings, parent, false))

    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.e("MyBookingsAdapter", "onBindViewHolder: "+holder )
        val bookingDataModel: MyBookingsData = list[position]



        if (bookingDataModel.prescription != null) {
            holder.patientName.text = bookingDataModel.prescription.patient_name
            holder.patientAge.text = "${bookingDataModel.prescription.patient_age} yrs"
            holder.patientGender.text = bookingDataModel.prescription.patient_gender
            holder.consultationStatus.text = bookingDataModel.status
            holder.consultationStatus.setTextColor(Color.rgb(76, 175, 80))
        } else {
            holder.patientName.text = bookingDataModel.booking_for_name
            holder.patientAge.text = "${bookingDataModel.booking_for_age} yrs"
            holder.patientGender.text = bookingDataModel.booking_for_gender
            holder.consultationStatus.text = bookingDataModel.status
        }


        holder.consTime.text = bookingDataModel.scheduled_at_str
        holder.itemView.setOnClickListener {
            context.startActivity(
                Intent(context, BookingDetailActivity::class.java)
                    .putExtra("booking_id", bookingDataModel.booking_id)
                    .putExtra("status", bookingDataModel.status)
            )
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val patientName: TextView = itemView.findViewById(R.id.patientName)
        val patientAge: TextView = itemView.findViewById(R.id.patientAge)
        val patientGender: TextView = itemView.findViewById(R.id.patientGender)
        val consTime: TextView = itemView.findViewById(R.id.bookingTime)
        val consultationStatus: TextView = itemView.findViewById(R.id.consultationStatus)
     //   val cntnoti :ImageView =itemView.findViewById(R.id.cntnoti)
    }

}