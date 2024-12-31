package com.ayursh.android.activities.auth

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ayursh.android.R
import com.ayursh.android.databinding.ActivitySignUpBinding
import com.ayursh.android.network.RetrofitClient
import com.ayursh.android.network.responses.auth.AuthResponse
import com.ayursh.android.utils.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


private const val TAG = "SignUpActivity"

class SignUpActivity : AppCompatActivity() {
    private var binder: ActivitySignUpBinding? = null
    private var firstname: String = ""
    private var lastname: String = ""
    private var email: String = ""
    private var mobile: String = ""
    private var gender: String = "Male"
    private var exp: Int? = 0
    private var consFee: Int? = 0
    private var dob: String = ""
    private var address: String = ""
    private var qual: String = ""
    private var isClinicAssoc: Boolean = false
    private var expertise: List<String> = ArrayList<String>()
    private var datePicker: DatePicker? = null
    private val c = Calendar.getInstance()
    private var mYear = c.get(Calendar.YEAR)
    private var mMonth = c.get(Calendar.MONTH)
    private var mDay = c.get(Calendar.DAY_OF_MONTH)
    private lateinit var bottomSheet: BottomSheetDialog
    private lateinit var sheetView: View
    private lateinit var sheetListView: ListView
    private lateinit var sheetAdapter: ArrayAdapter<String>
    private lateinit var expertiseList: Array<String>

    override
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FULLSCREEN()
        binder = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)
        init()
    }

    private fun init() {
        initElements()
        initListener()
    }

    private fun initElements() {
        bottomSheet = BottomSheetDialog(this, R.style.BottomSheetDialog)
        sheetView = LayoutInflater.from(this)
            .inflate(R.layout.signup_expertise_bottomsheet_layout, null, false)
        sheetListView = sheetView.findViewById(R.id.expertiseList)
        expertiseList = resources.getStringArray(R.array.Expertise)
        sheetAdapter = ArrayAdapter<String>(this, R.layout.list_item_multiple_choice, expertiseList)
        sheetListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE;
        sheetListView.adapter = sheetAdapter
        bottomSheet.setContentView(sheetView)
    }

    private fun initListener() {

        binder?.tvLogin?.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        bottomSheet.setOnDismissListener { setSelectedExpertise() }

        sheetView.findViewById<MaterialButton>(R.id.doneBtn).setOnClickListener {
            setSelectedExpertise()
        }

        binder?.genMale?.setOnClickListener {
            toggleGenBtn("Male")
        }
        binder?.genFemale?.setOnClickListener {
            toggleGenBtn("Female")
        }
        binder?.genOthers?.setOnClickListener {
            toggleGenBtn("Others")
        }
        binder?.clinYes?.setOnClickListener {
            toggleClinicBtn("Yes")
        }
        binder?.clinNo?.setOnClickListener {
            toggleClinicBtn("No")
        }
        binder?.etDob?.setOnClickListener {
            selectDate(binder?.etDob!!)
        }

        binder?.etExpertise?.setOnClickListener {
            bottomSheet.show()
        }

        binder?.getOtpBtn?.setOnClickListener {
            firstname = binder?.etFname?.text.toString().trim()
            lastname = binder?.etLname?.text.toString().trim()
            email = binder?.etEmail?.text.toString().trim()
            mobile = binder?.etMobile?.text.toString().trim()
            exp = binder?.etExp?.text.toString().toIntOrNull()
            consFee = binder?.etConsFee?.text.toString().toIntOrNull()
            address = binder?.etAddress?.text.toString().trim()
            qual = binder?.etQual?.text.toString().trim()
            dob = binder?.etDob?.text.toString().trim()

            if (firstname.isEmpty()) {
                binder?.etFname?.error = getString(R.string.field_required)
                return@setOnClickListener
            }
            if (lastname.isEmpty()) {
                binder?.etLname?.error = getString(R.string.field_required)
                return@setOnClickListener
            }
            if (email.isEmpty() || !email.validateEmail()) {
                binder?.etEmail?.error = getString(R.string.valid_email)
                return@setOnClickListener
            }
            if (mobile.isEmpty() || mobile.length != 10) {
                binder?.etMobile?.error = getString(R.string.field_required)
                return@setOnClickListener
            }
            if (exp == 0 || exp == null) {
                binder?.etExp?.error = getString(R.string.field_required)
                return@setOnClickListener
            }
            if (consFee == 0 || consFee == null) {
                binder?.etConsFee?.error = getString(R.string.field_required)
                return@setOnClickListener
            }
            if (dob.isEmpty()) {
                binder?.etDob?.error = getString(R.string.field_required)
                return@setOnClickListener
            }
            if (address.isEmpty()) {
                binder?.etAddress?.error = getString(R.string.field_required)
                return@setOnClickListener
            }
            if (qual.isEmpty()) {
                binder?.etQual?.error = getString(R.string.field_required)
                return@setOnClickListener
            }
            if (binder?.etExpertise?.text.isNullOrEmpty()) {
                binder?.etExpertise?.error = getString(R.string.field_required)
                return@setOnClickListener
            }
            proceedRegistration()
        }

    }

    private fun setSelectedExpertise() {
        expertise = ArrayList<String>()
        val checked: SparseBooleanArray = sheetListView.checkedItemPositions
        binder?.etExpertise?.setText("")
        var selectedItems = ""
        if (checked.size() > 0) {

            for (i in 0 until checked.size()) {
                if (checked.valueAt(i)) {
                    (expertise as ArrayList<String>).add(expertiseList[checked.keyAt(i)])
                    selectedItems += if (i == (checked.size() - 1)) {
                        "${expertiseList[checked.keyAt(i)]}"
                    } else {
                        "${expertiseList[checked.keyAt(i)]}, "
                    }
                }
            }
            if (expertise.size <= 3) {
                binder?.etExpertise?.setText(selectedItems)
                bottomSheet.dismiss()
            } else {
                showToast("Please select only 3")
            }
        } else {
            bottomSheet.dismiss()
        }

        Log.e(TAG, "setSelectedExpertise: ${JSONArray(expertise)}")

    }


    private fun proceedRegistration() {
        if (checkInternetConnection()) {
            showProgress()
            val data = JsonObject()
            data.addProperty("first_name", firstname)
            data.addProperty("last_name", lastname)
            data.addProperty("phone_number", "+91$mobile")
            data.addProperty("email", email)
            data.addProperty("dob", dob)
            data.addProperty("gender", gender)
            data.addProperty("address", address)
            data.addProperty("experience_in_months", exp)
            data.addProperty("consultation_fee", consFee)
            data.addProperty("is_clinic_associated", isClinicAssoc)
            data.addProperty("qualification", qual)
            data.addProperty("display_name", "Dr. $firstname $lastname")
            data.addProperty("registration_number", System.currentTimeMillis())


            val jsonarray: JsonArray = Gson().toJsonTree(expertise).asJsonArray
            data.add("expertise", jsonarray)
            Log.e(TAG, "proceedRegistration: $data")

            RetrofitClient.create(this).signup(data).enqueue(object : Callback<AuthResponse> {
                override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {

                    if (response.isSuccessful) {
                        val res: AuthResponse? = response.body()
                        Log.e(TAG, "onResponse(Success): ${res.toString()}")
                        if (res?.success == true) {
                            proceedOtpVerification(res.data.doctor_id, res.data.sms_token_session_id)
                        } else {
                            showToast(res?.message.toString())
                        }
                    } else {
                        when {
                            response.code() == 500 -> {
                                showToast(response.errorBody()
                                    ?.string().toString())
                            }
                            response.code() == 422 -> {
                                val errorRes = JSONObject(response.errorBody()?.string().toString())
                                if (errorRes.has("message")) {
                                    showToast(errorRes.getString("message"))
                                } else {
                                    showToast("Something went wrong, Try Again.")
                                }
                            }
                            else -> {
                                showToast(response.message())
                            }
                        }
                        /*     try {

                                 val errorRes = JSONObject(response.errorBody()?.string().toString())
                                 if (errorRes.has("success")) {
                                     showToast(errorRes.getString("message"))
                                 } else {
                                     showToast("Something went wrong, Try Again.")
                                     startActivity(Intent(this@SignUpActivity, LoginActivity::class.java))
                                     finish()
                                 }
                             } catch (e: Exception) {
                                 showToast("Something went wrong, Try Again.")
                                 showToast("Error : ${e.localizedMessage}", true)
                             }*/
                        Log.e(TAG, "onResponse: ${response.errorBody()}")
                        Log.e(TAG, "onResponse: ${response}")
                    }
                    dismissProgress()
                }

                override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                    dismissProgress()
                    Log.e(TAG, "onFailure: ${t.localizedMessage}")
                    showToast("Something went wrong, Try Again.")
                    showToast("Fail Error : ${t.localizedMessage}", true)
                }

            })
        }
    }

    private fun proceedOtpVerification(doc_id: String, sms_token: String) {
        startActivity(Intent(this, VerifyOtpActivity::class.java)
            .putExtra("doc_id", doc_id)
            .putExtra("sms_token", sms_token)
            .putExtra("mobile", mobile)
        )
    }

    private fun toggleClinicBtn(isClinicAssoc: String) {
        binder?.clinYes?.setBackgroundColor(resources.getColor(android.R.color.transparent))
        binder?.clinYes?.setTextColor(resources.getColor(R.color.white))
        binder?.clinNo?.setBackgroundColor(resources.getColor(android.R.color.transparent))
        binder?.clinNo?.setTextColor(resources.getColor(R.color.white))
        when (isClinicAssoc) {
            "Yes" -> {
                binder?.clinYes?.setBackgroundColor(resources.getColor(R.color.white))
                binder?.clinYes?.setTextColor(resources.getColor(R.color.black))
                this.isClinicAssoc = true
            }
            else -> {
                this.isClinicAssoc = false
                binder?.clinNo?.setBackgroundColor(resources.getColor(R.color.white))
                binder?.clinNo?.setTextColor(resources.getColor(R.color.black))
            }
        }
    }

    private fun toggleGenBtn(gender: String) {
        binder?.genMale?.setBackgroundColor(resources.getColor(android.R.color.transparent))
        binder?.genMale?.setTextColor(resources.getColor(R.color.white))
        binder?.genFemale?.setBackgroundColor(resources.getColor(android.R.color.transparent))
        binder?.genFemale?.setTextColor(resources.getColor(R.color.white))
        binder?.genOthers?.setBackgroundColor(resources.getColor(android.R.color.transparent))
        binder?.genOthers?.setTextColor(resources.getColor(R.color.white))
        this.gender = gender
        when (gender) {
            "Male" -> {
                binder?.genMale?.setBackgroundColor(resources.getColor(R.color.white))
                binder?.genMale?.setTextColor(resources.getColor(R.color.black))
            }
            "Female" -> {
                binder?.genFemale?.setBackgroundColor(resources.getColor(R.color.white))
                binder?.genFemale?.setTextColor(resources.getColor(R.color.black))
            }
            else -> {
                binder?.genOthers?.setBackgroundColor(resources.getColor(R.color.white))
                binder?.genOthers?.setTextColor(resources.getColor(R.color.black))
            }
        }
    }
    private fun selectDate(etDob: EditText) {
        val datePickerDialog = DatePickerDialog(this, R.style.datePickerDialog,
            { _, year, monthOfYear, dayOfMonth ->
                datePicker = DatePicker(this)
                datePicker?.init(year, monthOfYear + 1, dayOfMonth, null)
                var day = dayOfMonth.toString()
                if (dayOfMonth < 10) {
                    day = "0$dayOfMonth"
                }
                var month = (monthOfYear + 1).toString()
                if ((monthOfYear + 1) < 10) {
                    month = "0${monthOfYear+1}"
                }
                etDob.setText("$day-$month-$year")
            }, mYear, mMonth, mDay)
        if (datePicker != null) {
            datePickerDialog.updateDate(datePicker?.year!!, datePicker?.month!! - 1, datePicker?.dayOfMonth!!)
        }
        datePickerDialog.show()
    }
    override fun onBackPressed() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}