package com.ayursh.android.utils

import android.content.Context
import android.content.SharedPreferences
import com.ayursh.android.models.NotificationModel
import com.ayursh.android.models.UserModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import java.util.ArrayList

object SharedPref {
    private const val NAME = "MyPref"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = context.getSharedPreferences(NAME, MODE)
    }


    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = edit()
        operation(editor)
        editor.apply()
    }

    var isFirstRun: Boolean
        get() = preferences.getBoolean(Constants.IS_FIRST_RUN, true)
        set(value) = preferences.edit {
            it.putBoolean(Constants.IS_FIRST_RUN, value)
        }

    fun getString(key: String): String {
        return preferences.getString(key, "")!!
    }
    object Patient{
        var NotificationList: String
            get()= preferences.getString(Constants.NotificationList, "").toString()
            set(value)= preferences.edit() {
                it.putString(Constants.NotificationList, value)
            }
        var msgPending: Boolean
        get()= preferences.getBoolean(Constants.msgPending,false)
        set(value)= preferences.edit(){
            it.putBoolean(Constants.msgPending,value)
        }
    }

    //USER PREF
    object User {

        val isLoggedIn: Boolean
            get() = AUTH_TOKEN != ""

        fun login(authToken: String, docId: String) {
            AUTH_TOKEN = authToken
            DOC_ID = docId
        }

        fun logout() {
            AUTH_TOKEN = ""
        }

        var AUTH_TOKEN: String?
            get() = preferences.getString(Constants.AUTH_TOKEN, "")
            set(value) = preferences.edit {
                it.putString(Constants.AUTH_TOKEN, value)
            }

        var DOC_ID: String?
            get() = preferences.getString(Constants.DOC_ID, "")
            set(value) = preferences.edit {
                it.putString(Constants.DOC_ID, value)
            }
        var fcm_token:String?
        get() = preferences.getString(Constants.FCM_TOKEN,"")
        set(value) = preferences.edit{
            it.putString(Constants.FCM_TOKEN,value)
        }
        var USER: UserModel
            get() = UserModel(
                getString(Constants.USER.PHONE_NUMBER),
                getString(Constants.USER.FIRST_NAME),
                getString(Constants.USER.LAST_NAME),
                getString(Constants.USER.DISPLAY_NAME),
                getString(Constants.USER.GENDER),
                preferences.getInt(Constants.USER.EXP_IN_MON, 0),
                preferences.getInt(Constants.USER.CONSULTATION_FEE, 0),
                preferences.getBoolean(Constants.USER.IS_CLINIC_ASSOC, false),
                getString(Constants.USER.EMAIL),
                getString(Constants.USER.DOB),
                getString(Constants.USER.ADDRESS),
//                preferences.getLong(Constants.USER.REGISTRATION_NUMBER, 1),
                getString(Constants.USER.QUALIFICATION),
                preferences.getBoolean(Constants.USER.IS_ACTIVE, false),
                preferences.getBoolean(Constants.USER.INTERNAL, false),
                getString(Constants.USER.DOC_ID)
            )
            set(user) = preferences.edit {
                it.putString(Constants.USER.PHONE_NUMBER, user.phone_number)
                it.putString(Constants.USER.FIRST_NAME, user.first_name)
                it.putString(Constants.USER.LAST_NAME, user.last_name)
                it.putString(Constants.USER.DISPLAY_NAME, user.display_name)
                it.putString(Constants.USER.GENDER, user.gender)
                it.putInt(Constants.USER.EXP_IN_MON, user.experience_in_months)
                it.putInt(Constants.USER.CONSULTATION_FEE, user.consultation_fee)
                it.putBoolean(Constants.USER.IS_CLINIC_ASSOC, user.is_clinic_associated)
                it.putString(Constants.USER.EMAIL, user.email)
                it.putString(Constants.USER.DOB, user.dob)
                it.putString(Constants.USER.ADDRESS, user.address)
//                it.putLong(Constants.USER.REGISTRATION_NUMBER, user.registration_number)
                it.putString(Constants.USER.QUALIFICATION, user.qualification)
                it.putBoolean(Constants.USER.IS_ACTIVE, user.is_active)
                it.putBoolean(Constants.USER.INTERNAL, user.internal)
                it.putString(Constants.USER.DOC_ID, user.doctor_id)
            }


    }

}