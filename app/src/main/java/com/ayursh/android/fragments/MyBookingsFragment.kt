package com.ayursh.android.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ayursh.android.R
import com.ayursh.android.activities.DisplayNotification
import com.ayursh.android.activities.auth.LoginActivity
import com.ayursh.android.activities.service.MyFirebaseMessagingService
import com.ayursh.android.adapters.MyBookingsAdapter
import com.ayursh.android.databinding.FragmentMyBookingsBinding
import com.ayursh.android.models.NotificationModel
import com.ayursh.android.network.RetrofitClient
import com.ayursh.android.network.responses.MyBookingsResponse
import com.ayursh.android.utils.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kal.rackmonthpicker.RackMonthPicker
import com.michalsvec.singlerowcalendar.calendar.CalendarChangesObserver
import com.michalsvec.singlerowcalendar.calendar.CalendarViewManager
import com.michalsvec.singlerowcalendar.calendar.SingleRowCalendarAdapter
import com.michalsvec.singlerowcalendar.selection.CalendarSelectionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*


private const val TAG = "MyBookingsFragment"

class MyBookingsFragment : Fragment() {

    private lateinit var binder: FragmentMyBookingsBinding
    private lateinit var ctx: Context

    private val currentCalendar = Calendar.getInstance()
    private var currentMonth = 0

    private lateinit var bookingDate: String
    private lateinit var bookingMonth: String
    private lateinit var bookingYear: String

    private lateinit var myCalendarViewManager: CalendarViewManager
    private lateinit var mySelectionManager: CalendarSelectionManager
    private lateinit var myCalendarChangesObserver: CalendarChangesObserver


    var gson = Gson()
    val REQUEST_NOTIFICATION_LIST=112


    override fun onAttach(context: Context) {
        super.onAttach(context)
        ctx = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binder = DataBindingUtil.inflate(inflater, R.layout.fragment_my_bookings, container, false)
        currentCalendar.time = Date()
        currentMonth = currentCalendar[Calendar.MONTH]
        MyBookingsFragment.activity=activity as Activity
        init()
        return binder.root
    }

    @SuppressLint("UseRequireInsteadOfGet")
    private fun init() {

        initElements()
        initListeners()
        setCalenderRow()
        getBookings()
        updateNotificationBadge()

    }

    @SuppressLint("WrongViewCast")
    open fun updateNotificationBadge() {

        val notificationListString: String = SharedPref.Patient.NotificationList
        if (!TextUtils.isEmpty(notificationListString)) {
            val type = object : TypeToken<List<NotificationModel?>?>() {}.type

            if(!Restarter.vdomap.isEmpty() or !Restarter.audmap.isEmpty() or (MyFirebaseMessagingService.msgs?.isEmpty() == false)){
                binder.popNoti.visibility= View.VISIBLE
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

            if(count > 0 || Constants.msgPending.equals("yes")) {
                binder.popNoti.setVisibility(View.VISIBLE);
            } else {
                binder.popNoti.setVisibility(View.GONE);
            }
        } else {
            binder.popNoti.setVisibility(View.GONE);
        }

    }



    private fun setCalenderRow() {
        val singleRowCalendar = binder.mainSingleRowCalendar.apply {
            calendarViewManager = myCalendarViewManager
            calendarChangesObserver = myCalendarChangesObserver
            calendarSelectionManager = mySelectionManager
            setDates(getListOfDates())
            init()
        }

        try {
            singleRowCalendar.select(bookingDate.toInt() - 1)
        } catch (e: Exception){
            Toast.makeText(ctx, "Pick a valid date", Toast.LENGTH_LONG).show()
        }
    }

    private fun initElements() {
        binder.bookingsRecyc.layoutManager = LinearLayoutManager(ctx)
        val c = Calendar.getInstance()
        bookingDate = if (c.get(Calendar.DATE) < 10) "0${c.get(Calendar.DATE)}" else c.get(Calendar.DATE)
            .toString()
        bookingMonth = if ((c.get(Calendar.MONTH) + 1) < 10) "0${(c.get(Calendar.MONTH) + 1)}" else (c.get(Calendar.MONTH) + 1).toString()
        bookingYear = c.get(Calendar.YEAR).toString()

        binder.monthYearPicker.text = "${DateFormatSymbols.getInstance().months[(bookingMonth.toInt()) - 1]}, $bookingYear"
    }


    private fun initListeners() {
        binder.monthYearPicker.setOnClickListener {
            RackMonthPicker(ctx)
                .setColorTheme(com.kal.rackmonthpicker.R.color.color_primary)
                .setLocale(Locale.ENGLISH)
                .setNegativeButton({ })
                .setPositiveButton { month, startDate, endDate, year, monthLabel ->

                    bookingMonth = if (month < 10) "0$month" else month.toString()
                    bookingYear = year.toString()
                    binder.monthYearPicker.text = monthLabel
                    setCalenderRow()
                }.show()
        }
        binder.todaysBtn.setOnClickListener {
            val c = Calendar.getInstance()
            bookingDate = if (c.get(Calendar.DATE) < 10) "0${c.get(Calendar.DATE)}" else c.get(Calendar.DATE)
                .toString()
            bookingMonth = if ((c.get(Calendar.MONTH) + 1) < 10) "0${(c.get(Calendar.MONTH) + 1)}" else (c.get(Calendar.MONTH) + 1).toString()
            bookingYear = c.get(Calendar.YEAR).toString()
            binder.monthYearPicker.text = "${DateFormatSymbols.getInstance().months[(bookingMonth.toInt()) - 1]}, $bookingYear"
            setCalenderRow()
        }

        binder.bellIcon.setOnClickListener {
            startActivity(Intent(ctx, DisplayNotification::class.java))
        }

        myCalendarViewManager = object : CalendarViewManager {
            override fun setCalendarViewResourceId(position: Int, date: Date, isSelected: Boolean): Int {
                return R.layout.calender_row
            }

            override fun bindDataToCalendarView(holder: SingleRowCalendarAdapter.CalendarViewHolder, date: Date, position: Int, isSelected: Boolean) {
                // bind data to calendar item views
                val dateTextView: TextView = holder.itemView.findViewById(R.id.date)
                val pointerImageView: ImageView = holder.itemView.findViewById(R.id.pointer)

                dateTextView.text = SimpleDateFormat("dd", Locale.getDefault()).format(date)
                    .toString()
                if (isSelected) {
                    pointerImageView.show()
                    dateTextView.setTextColor(resources.getColor(R.color.white))
                    holder.itemView.backgroundTintList = resources.getColorStateList(R.color.accent, null)
                } else {
                    pointerImageView.hide()
                    dateTextView.setTextColor(resources.getColor(R.color.black))
                    pointerImageView.backgroundTintList = resources.getColorStateList(R.color.white, null)
                }
            }
        }

        mySelectionManager = object : CalendarSelectionManager {
            override fun canBeItemSelected(position: Int, date: Date): Boolean {
                // return true if item can be selected
                return true
            }
        }
        myCalendarChangesObserver = object : CalendarChangesObserver {
            override fun whenWeekMonthYearChanged(weekNumber: String, monthNumber: String, monthName: String, year: String, date: Date) {
                super.whenWeekMonthYearChanged(weekNumber, monthNumber, monthName, year, date)
            }

            override fun whenSelectionChanged(isSelected: Boolean, position: Int, date: Date) {
                super.whenSelectionChanged(isSelected, position, date)
                binder.mainSingleRowCalendar.scrollToPosition(position)
                if (isSelected) {
                    val newDate = SimpleDateFormat("dd", Locale.getDefault()).format(date)

                    bookingDate = newDate
                    getBookings()

                }

            }

            override fun whenCalendarScrolled(dx: Int, dy: Int) {
                super.whenCalendarScrolled(dx, dy)
            }

            override fun whenSelectionRestored() {
                super.whenSelectionRestored()
                Log.e(TAG, "whenSelectionRestored: ")
            }

            override fun whenSelectionRefreshed() {
                super.whenSelectionRefreshed()
                Log.e(TAG, "whenSelectionRestored: ")
            }
        }

    }


    private fun getListOfDates(): List<Date> {
        val list = mutableListOf<Date>()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.add(Calendar.MONTH, (bookingMonth.toInt() + 6))

        calendar.add(Calendar.DAY_OF_MONTH, -1)
        for (i in 1..calendar.get(Calendar.DAY_OF_MONTH)) {
            calendar.set(Calendar.DATE, i)
            list.add(calendar.time)
        }
        return list
    }

    private fun getBookings() {
        if (ctx.checkInternetConnection()) {
            val date = "$bookingYear-$bookingMonth-$bookingDate"
            binder.progress.show()
            binder.bookingsRecyc.adapter = null
            binder.noBookingsLay.hide()
            RetrofitClient.create(ctx)
                .getMyBooking("Bearer ${SharedPref.getString(Constants.AUTH_TOKEN)}", date)
                .enqueue(object : Callback<MyBookingsResponse> {
                    override fun onResponse(call: Call<MyBookingsResponse>, response: Response<MyBookingsResponse>) {
                        if (response.isSuccessful) {
                            val res = response.body()
                            if (res?.success == true) {
                                if (res.data.isNotEmpty()) {
                                    binder.bookingsRecyc.show()
                                    binder.bookingsRecyc.adapter = MyBookingsAdapter(res.data, ctx)
                                } else {
                                    binder.noBookingsLay.show()
                                    binder.bookingsRecyc.hide()
                                }
                            } else {
                                ctx.showToast(res?.message.toString())
                            }

                        } else {
                            when {
                                response.code() == 500 -> {
                                    ctx.showToast(response.errorBody()
                                        ?.string().toString())
                                }
                                response.code() == 401 -> {
                                    ctx.showToast(response.message())
                                    SharedPref.User.logout()
                                    startActivity(Intent(ctx, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                                    (ctx as Activity).finish()
                                }
                                else -> {
                                    ctx.showToast(response.message())
                                }
                            }
                            binder.noBookingsLay.show()
                            binder.bookingsRecyc.hide()
                        }
                        binder.progress.hide()
                    }

                    override fun onFailure(call: Call<MyBookingsResponse>, t: Throwable) {
                        Log.e(TAG, "onFailure: ${t.localizedMessage}")
                        binder.progress.hide()
                        binder.noBookingsLay.show()
                        binder.bookingsRecyc.hide()
                    }

                })
        }

    }

    companion object {
        @JvmStatic
        fun newInstance() = MyBookingsFragment()

        @SuppressLint("StaticFieldLeak")
        var activity:Activity?=null

//        fun showbadge(context: Context){
//            val notibadge=(context as Activity).findViewById<View>(R.id.pop_noti) as ImageView
//            notibadge.visibility=View.VISIBLE
//        }
//        fun hidebadge(context: Context){
//            val notibadge=(context as Activity).findViewById<View>(R.id.pop_noti) as ImageView
//            notibadge.visibility=View.GONE
//        }
    }
}

class Changebadge() {
    fun showbadge(){
        Log.e(TAG, "showbadge: "+(MyBookingsFragment.activity)+" "+(MyBookingsFragment.activity)?.findViewById<View>(R.id.pop_noti))
        val notibadge=(MyBookingsFragment.activity)?.findViewById<View>(R.id.pop_noti)
        notibadge?.visibility=View.VISIBLE
    }
    fun hidebadge(){
        val notibadge=(MyBookingsFragment.activity)?.findViewById<View>(R.id.pop_noti)
        notibadge?.visibility=View.GONE
    }
}