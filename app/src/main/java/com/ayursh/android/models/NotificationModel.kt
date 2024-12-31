package com.ayursh.android.models

import java.text.SimpleDateFormat
import java.util.*


class NotificationModel(b: Boolean, s: String?, s1: String?, s3: String?) {
    var isRead = b
    var title = s
    var body = s1
   // var timestamp= s2
    var booking_id = s3
    var count=0

    @JvmName("isRead1")
    fun isRead(): Boolean {
        return isRead
    }

    @JvmName("setRead1")
    fun setRead(read: Boolean) {
        isRead = read
    }

    @JvmName("getTitle1")
    fun getTitle(): String? {
        return title
    }

    @JvmName("setTitle1")
    fun setTitle(title: String?) {
        this.title = title
    }

    @JvmName("getBody1")
    fun getBody(): String? {
        return body
    }

    @JvmName("setBody1")
    fun setBody(body: String?) {
        this.body = body
    }

//    @JvmName("getTimestamp1")
//    fun getTimestamp(): String? {
//        val c=Calendar.getInstance().time
//        val df = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
//        return  df.format(c)
//    }
//
//    @JvmName("setTimestamp1")
//    fun setTimestamp(timestamp: String?) {
//        this.timestamp = timestamp
//    }
    @JvmName("setBookingId")
    fun setBooking_id(booking_id: String) {
        this.booking_id = booking_id
    }

    @JvmName("getBookingId")
    fun getBooking_id(): String? {
        return booking_id
    }

    override fun toString(): String {
        return "NotificationModel{" +
                "isRead=" + isRead +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
//                ", timeStamp='" + timestamp + '\'' +
                ", booking_id='" + booking_id + '\'' +
                '}'
    }
}
