package com.ayursh.android.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.ayursh.android.R
import com.ayursh.android.databinding.ActivityPrescriptionBinding
import com.ayursh.android.models.PrescriptionModel
import com.ayursh.android.models.TherapyCategoryMap
import com.ayursh.android.network.RetrofitClient
import com.ayursh.android.network.responses.SavePrescription
import com.ayursh.android.network.responses.Therapy
import com.ayursh.android.network.responses.TherapyCategoryResponse
import com.ayursh.android.network.responses.TherapyResponse
import com.ayursh.android.utils.*
import com.google.android.material.button.MaterialButton
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.util.*


private const val TAG = "Prescription File"

class Prescription : AppCompatActivity() {

    private var booking_id: String = ""
    private lateinit var binding: ActivityPrescriptionBinding // ViewBinding object

    private var first_name: String = ""
    private var last_name: String = ""
    private var patient_gender: String = ""
    private var patient_age: String = ""
    private var gender: String = "Male"
    var myspinner1: Spinner? = null
    var main_complain: String = ""
    var associated_complain: String = ""
    var complain_history: String = ""
    var dosh_analysis: String = ""
    var allergies: String = ""
    var diagnosis: String = ""
    var prescriptionText: String = ""
    var dialog: Dialog? = null
    var afterCompletionDialog: Dialog? = null
    var therapy_category: String = ""
    var therapy_title: String = ""
    var therapy_session: String = ""
    var bool: Boolean = false
    var therapyCategories = mutableListOf<String>()
    var therapyHeadlines = mutableListOf<String>() //mutableListOf("Select Headline")
    var therapySession = mutableListOf<String>()//mutableListOf("Select Therapy Session")
    var filename = ""
    var prescriptionModel: PrescriptionModel? = null
    lateinit var therapyCat: List<TherapyCategoryMap>
    lateinit var therapies: List<Therapy>
    var isSaved=false;
    var user_fcm_token:String=""

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrescriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //Adding Scrollers to the text views
//        main_cmp.setScroller(Scroller(this))
        binding.mainCmp.setOnTouchListener(OnTouchListener { v, event ->
            if (binding.mainCmp.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_SCROLL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        return@OnTouchListener true
                    }
                }
            }
            false
        })

//        main_cmp.movementMethod = ScrollingMovementMethod()
        binding.cmplHistory.setOnTouchListener(OnTouchListener { v, event ->
            if (binding.cmplHistory.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_SCROLL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        return@OnTouchListener true
                    }
                }
            }
            false
        })
        binding.boolAllergy.setOnTouchListener(OnTouchListener { v, event ->
            if (binding.boolAllergy.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_SCROLL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        return@OnTouchListener true
                    }
                }
            }
            false
        })
        val allergiesEt: EditText = findViewById(R.id.allergies)
        allergiesEt.setOnTouchListener(OnTouchListener { v, event ->
            if (allergiesEt.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_SCROLL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        return@OnTouchListener true
                    }
                }
            }
            false
        })
        val diagnosisEt: EditText = findViewById(R.id.diagnosis)
        diagnosisEt.setOnTouchListener(OnTouchListener { v, event ->
            if (diagnosisEt.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_SCROLL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        return@OnTouchListener true
                    }
                }
            }
            false
        })
        binding.assoCmpl.setOnTouchListener(OnTouchListener { v, event ->
            if (binding.assoCmpl.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_SCROLL -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        return@OnTouchListener true
                    }
                }
            }
            false
        })


        myspinner1 = binding?.spinner1
        init()
    }

    private fun init() {
        askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 101);
        getIntentData()
        if (prescriptionModel != null) {
            Log.e(TAG, "init: Inside PPPP")
            prefillData()
            binding?.backBtn?.setOnClickListener { finish() }
        } else {
            focusable()
            initListeners()
        }
    }

    private fun focusable() {
        binding?.etFname?.requestFocus()
        binding?.etFname?.isCursorVisible=true
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            binding?.etFname?.setTextCursorDrawable(0)
        }
        binding?.etLname?.requestFocus()
        binding?.etLname?.isCursorVisible=true
    }


    private fun askForPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            }
        } else if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(applicationContext, "Permission was denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getIntentData() {
        booking_id = intent.getStringExtra("booking_id").toString()
        if (intent.getSerializableExtra("Prescription Model") !== null)
            prescriptionModel = intent.getSerializableExtra("Prescription Model") as PrescriptionModel
        user_fcm_token=intent.getStringExtra("user_fcm_token").toString()
    }

    private fun prefillData() {
        binding?.savePres?.visibility = View.GONE
        val name = prescriptionModel?.patient_name!!.split(" ")
        first_name = name[0]
        try {
            last_name = name[1]
        } catch (e: Exception) {

        }
        binding?.etFname?.setText(first_name)
        if (last_name.isNotEmpty()) {
            binding?.etLname?.setText(last_name)
        }
        binding?.age?.setText(prescriptionModel?.patient_age)
        binding?.mainCmp?.setText(prescriptionModel?.main_complaints)
        binding?.assoCmpl?.setText(prescriptionModel?.associated_complaints)
        binding?.cmplHistory?.setText(prescriptionModel?.history_of_main_complaints)
        binding?.boolAllergy?.setText(prescriptionModel?.if_any_allergies)
        binding?.allergies?.setText(prescriptionModel?.diagnosis)
        binding?.doshaAnalysis?.setText(prescriptionModel?.dosha_analysis)
        prescriptionModel?.patient_gender?.let { toggleGenBtn(it) }
        binding?.diagnosis?.setText(prescriptionModel?.prescription)
        if (prescriptionModel?.is_therapy_assigned == true) {
            binding?.yes?.isChecked = true
            binding!!.spinnerLayout.visibility = View.VISIBLE
            setData(0)
        } else {
            binding?.no?.isChecked = true
        }
        disableTouch()
    }

    private fun disableTouch() {
        binding?.etFname?.keyListener = null
        binding?.etLname?.keyListener = null
        binding?.diagnosis?.keyListener = null
        binding?.allergies?.keyListener = null
        binding?.boolAllergy?.keyListener = null
        binding?.mainCmp?.keyListener = null
        binding?.assoCmpl?.keyListener = null
        binding?.cmplHistory?.keyListener = null
        binding?.age?.keyListener = null
        binding?.doshaAnalysis?.keyListener = null
        binding?.yes?.keyListener = null
        binding?.no?.keyListener = null
        binding?.genMale?.keyListener = null
        binding?.genFemale?.keyListener = null
        binding?.genOthers?.keyListener = null
        binding?.spinner1?.isEnabled = false
        binding?.spinner2?.isEnabled = false
        binding?.spinner3?.isEnabled = false
    }

    private fun initListeners() {

        binding?.backBtn?.setOnClickListener { finish() }
        binding?.genMale?.setOnClickListener {
            toggleGenBtn("Male")
        }
        binding?.genFemale?.setOnClickListener {
            toggleGenBtn("Female")
        }
        binding?.genOthers?.setOnClickListener {
            toggleGenBtn("Others")
        }
        dialog = Dialog(this)
        afterCompletionDialog = Dialog(this)


        binding?.yesno?.setOnCheckedChangeListener { radioGroup, i ->
            val selected = binding?.yesno?.checkedRadioButtonId
            if (selected == R.id.yes) {
                bool = true
                binding!!.spinnerLayout.visibility = View.VISIBLE
                setData(1)
                selectTherapyCategory(therapyCategories)
                Log.e(TAG, "initListeners: ")

            } else {
                bool = false
                binding?.spinnerLayout?.visibility = View.GONE

            }
        }
        binding?.frame0?.setOnClickListener {
            Log.e(TAG, "initListeners: Changing Spinner 1 ${therapyCat}")
            selectTherapyCategory(therapyCategories)
        }
        binding?.frame1?.setOnClickListener {
            Log.e(TAG, "initListeners: Changing spinner 2 ${therapies}")
            selectTherapyTitle(therapies)
        }


        binding?.savePres?.setOnClickListener {
            first_name = binding?.etFname?.text.toString()
            last_name = binding?.etLname?.text.toString()
            patient_age = binding?.age?.text.toString()
            patient_gender = this.gender
            main_complain = binding?.mainCmp?.text.toString()
            associated_complain = binding?.assoCmpl?.text.toString()
            complain_history = binding?.cmplHistory?.text.toString()
            allergies = binding?.boolAllergy?.text.toString()
            prescriptionText = binding?.diagnosis?.text.toString()
            diagnosis = binding?.allergies?.text.toString()
            dosh_analysis = binding?.doshaAnalysis?.text.toString()
            Log.e(TAG, "generatePrescription: ${first_name + " " + last_name} ${patient_age}" +
                    " ${patient_gender} ${main_complain.toString()} ${bool} ${therapy_category}")
            if (
                first_name.isNotEmpty() && last_name.isNotEmpty()
                && patient_age.isNotEmpty() && main_complain.isNotEmpty()
                && associated_complain.isNotEmpty() && complain_history.isNotEmpty()
                && allergies.isNotEmpty() && prescriptionText.isNotEmpty()
                && diagnosis.isNotEmpty() && dosh_analysis.isNotEmpty()
            ) {

                generatePrescription()

            } else {
                Log.e(TAG, "initListeners: ${main_complain}" )
                var i=0
                while(i<main_complain.length){
                    Log.e(TAG, "initListeners: ${main_complain[i].toByte().toInt()} ${main_complain[i].toInt()}" )
                    i+=1
                }
                if (first_name.isEmpty())
                    binding?.etFname?.error = "Required"
                if (last_name.isEmpty())
                    binding?.etLname?.error = "Required"
                if (patient_age.isEmpty())
                    binding?.age?.error = "Required"
                if (main_complain.isEmpty())
                    binding?.mainCmp?.error = "Required"
                if (associated_complain.isEmpty())
                    binding?.assoCmpl?.error = "Required"
                if (complain_history.isEmpty())
                    binding?.cmplHistory?.error = "Required"
                if (allergies.isEmpty())
                    binding?.boolAllergy?.error = "Required"
                if (prescriptionText.isEmpty())
                    binding?.diagnosis?.error = "Required"
                if (diagnosis.isEmpty())
                    binding?.allergies?.error = "Required"
                if (dosh_analysis.isEmpty())
                    binding?.doshaAnalysis?.error = "Required"
            }

        }


    }

    private fun generatePrescription() {
        if (checkInternetConnection()) {
            showProgress()
            therapy_session = if (binding?.spinner3?.selectedItem.toString().isEmpty()) "0"
            else binding?.spinner3?.selectedItem.toString()
            if (bool==false) {
                RetrofitClient.create(this)
                    .generatePrescriptionWithoutTherapy("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}",
                        booking_id, "$first_name $last_name", patient_age, patient_gender, main_complain, associated_complain,
                        complain_history, allergies, dosh_analysis, diagnosis,
                        prescriptionText, bool,
                        true, user_fcm_token
                    )
                    .enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            if (response.isSuccessful) {
                                dismissProgress()
                                Log.e(TAG, "onResponse: Success")
                                DownloadFileFromUrl().execute(response.body()?.byteStream())
                            } else {
                                dismissProgress()
                                showToast(response.errorBody()?.string().toString())
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.e(TAG, "onFailure: ${t.localizedMessage}")
                            dismissProgress()
                        }
                    })

            }
            else{
                RetrofitClient.create(this)
                    .generatePrescription("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}",
                        booking_id, "$first_name $last_name", patient_age, patient_gender, main_complain, associated_complain,
                        complain_history, allergies, dosh_analysis, diagnosis,
                        prescriptionText, bool,
                        true, therapy_category, therapy_title, therapy_session.toInt(), user_fcm_token
                    )
                    .enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            if (response.isSuccessful) {
                                dismissProgress()
                                Log.e(TAG, "onResponse: Success")
                                DownloadFileFromUrl().execute(response.body()?.byteStream())
                            } else {
                                dismissProgress()
                                showToast(response.errorBody()?.string().toString())
                            }
                        }

                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            Log.e(TAG, "onFailure: ${t.localizedMessage}")
                            dismissProgress()
                        }
                    })
            }
        }
    }

    private fun openConfirmPDFdialog() {
        dialog?.setContentView(R.layout.prescription_dialog)
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.show()
        val confirm = dialog?.findViewById<MaterialButton>(R.id.confirm)
        val editPdf = dialog?.findViewById<MaterialButton>(R.id.edit)
        val openPDF = dialog?.findViewById<LinearLayoutCompat>(R.id.pdfViewer)
        openPDF?.setOnClickListener {
//            startActivity(
//                Intent(Intent(this, PdfViewerActivity::class.java))
//                    .putExtra("filename", filename)
//            )
        }
        confirm?.setOnClickListener {
            val file: File=File("/sdcard/", filename)
            if(file.exists()){
                file.delete()
            }
            dialog?.dismiss()
            savePrescription()
        }
        editPdf?.setOnClickListener {
            val file: File=File("/sdcard/", filename)
            if(file.exists()){
                file.delete()
            }
            dialog?.dismiss() }
    }

    private fun savePrescription() {
        showProgress()
        if(bool==false) {
            RetrofitClient.create(this)
                .savePrescriptionWithoutTherapy("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}",
                    booking_id, "$first_name $last_name", patient_age, patient_gender, main_complain, associated_complain,
                    complain_history, allergies, dosh_analysis, diagnosis,
                    prescriptionText, bool,
                    true, user_fcm_token
                ).enqueue(object : Callback<SavePrescription> {
                    override fun onResponse(call: Call<SavePrescription>, response: Response<SavePrescription>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            if (res.success) {

                                dismissProgress()
                                afterCompletionDialog?.setContentView(R.layout.save_prescription_dialog)
                                afterCompletionDialog?.show()
                                val timer = Timer()
                                timer.schedule(object : TimerTask() {
                                    override fun run() {
                                        afterCompletionDialog?.dismiss()
                                        timer.cancel()
                                        finish()
                                    }
                                }, 1500)
                            } else {
                                showToast(res.message)
                            }
                        } else {

                            dismissProgress()
                            Log.e(TAG, "onResponseeeee: ${
                                response.errorBody()?.string().toString()
                            } ")
                        }
                    }


                    override fun onFailure(call: Call<SavePrescription>, t: Throwable) {
                        Log.e(TAG, "onFailure: Failedd")
                        dismissProgress()
                    }


                })
        }
        else{
            RetrofitClient.create(this)
                .savePrescription("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}",
                    booking_id, "$first_name $last_name", patient_age, patient_gender, main_complain, associated_complain,
                    complain_history, allergies, dosh_analysis, diagnosis,
                    prescriptionText, bool,
                    true, therapy_category, therapy_title, therapy_session.toInt(), user_fcm_token
                ).enqueue(object : Callback<SavePrescription> {
                    override fun onResponse(call: Call<SavePrescription>, response: Response<SavePrescription>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            if (res.success) {

                                dismissProgress()
                                afterCompletionDialog?.setContentView(R.layout.save_prescription_dialog)
                                afterCompletionDialog?.show()
                                val timer = Timer()
                                timer.schedule(object : TimerTask() {
                                    override fun run() {
                                        afterCompletionDialog?.dismiss()
                                        timer.cancel()
                                        finish()
                                    }
                                }, 1500)
                            } else {
                                showToast(res.message)
                            }
                        } else {

                            dismissProgress()
                            Log.e(TAG, "onResponseeeee: ${
                                response.errorBody()?.string().toString()
                            } ")
                        }
                    }


                    override fun onFailure(call: Call<SavePrescription>, t: Throwable) {
                        Log.e(TAG, "onFailure: Failedd")
                        dismissProgress()
                    }


                })
        }

    }

    private fun setData(check: Int) {
        RetrofitClient.create(this)
            .getSpinnerCategories("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}")
            .enqueue(object : Callback<TherapyResponse> {
                override fun onResponse(call: Call<TherapyResponse>, response: Response<TherapyResponse>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        if (res.success) {
                            Log.e(TAG, "onResponse: ${res.data.therapy_category_map[1].headline}")
                            therapyCat = res.data.therapy_category_map
                            Log.e(TAG, "onResponse: Spinner 1 data loaded")
                            addadapterOnSpinner(therapyCat, check)
                        } else {
                            showToast(res.message)
                        }

                    } else {
                        showToast(response.errorBody()
                            ?.string().toString())
                    }
                    dismissProgress()
                }

                override fun onFailure(call: Call<TherapyResponse>, t: Throwable) {
                    Log.e(TAG, "Failure111111 ${call}  ${t.localizedMessage}")
                }

            })
    }

    private fun addadapterOnSpinner(therapyCat: List<TherapyCategoryMap>, check: Int) {
        var i = 0
        var temp = -1
        var str = ""
        if (check == 0) {
            temp = prescriptionModel?.assigned_therapy_category!!.indexOf("title")
            str = prescriptionModel?.assigned_therapy_category!!
            if (str != null) {
                Log.e(TAG, "addadapterOnSpinner: ${temp} ${str.substring(temp + 6, str.length - 1)}")
                str = str.substring(temp + 6, str.length - 1)
            }
            if (temp == -1) {
                str = prescriptionModel?.assigned_therapy_category!!
                Log.e(TAG, "addadapterOnSpinner: ${str}")
            }
        }
        while (i < therapyCat.size) {
            therapyCategories.add(therapyCat[i].headline)
            if (check == 0 && (therapyCat[i].title.toString() == str || therapyCat[i].headline == str)) {
                temp = i
            }

            Log.e(TAG, "addadapterOnSpinner: ${therapyCat[i].headline}")
            i += 1
        }
        val aa = therapyCategories.let { ArrayAdapter(this, android.R.layout.simple_spinner_item, it) }
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        myspinner1?.adapter = aa
        if (check == 1) {
            return
            Log.e(TAG, "addadapterOnSpinner:------------------------------- ")
            binding?.frame0?.setOnClickListener {
                Log.e(TAG, "addadapterOnSpinner: ")
                selectTherapyCategory(therapyCategories)
            }

        } else {
            Log.e(TAG, "addadapterOnSpinner: ${temp} ${therapyCat[temp].headline}")
            myspinner1?.setSelection(temp)
            myspinner1?.isClickable = false
            retroRetrieveHeadlines(therapyCat[temp].title, check)

        }


    }

    private fun selectTherapyCategory(therapyCategories: MutableList<String>) {
        Log.e(TAG, "selectTherapyCategory: =====================")
        myspinner1?.setOnItemSelectedListener(object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                Log.e("onItemSelected  ${therapyCategories[position]}", position.toString())
                val selItem = therapyCat[position].title.toString()
                binding?.spinner2?.visibility = View.VISIBLE
                therapy_category = therapyCat[position].title.toString()
                Log.e(TAG, "onItemSelected: ${therapy_category} Spinner 1")

                retroRetrieveHeadlines(selItem, 1)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        })
    }

    private fun retroRetrieveHeadlines(selectedItem: String, check: Int) {
        RetrofitClient.create(this)
            .getTherapy("Bearer ${SharedPref.User.AUTH_TOKEN.toString()}", selectedItem)
            .enqueue(object : Callback<TherapyCategoryResponse> {
                override fun onResponse(call: Call<TherapyCategoryResponse>, response: Response<TherapyCategoryResponse>) {
                    Log.d("Varun", "${SharedPref.User.AUTH_TOKEN.toString()}")
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        if (res.success) {

                            try {
                                therapies = res.data[0].therapies
                                Log.e(TAG, "onResponse: Spinner 2 data loaded")
                                addHeadlinetospinner(therapies, check)
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Log.e(TAG, "onResponse: ${res.data}")
                            }

                        } else {
                            showToast(res.message)
                        }

                    } else {
                        showToast(response.errorBody()
                            ?.string().toString())
                    }
                    dismissProgress()
                }

                override fun onFailure(call: Call<TherapyCategoryResponse>, t: Throwable) {
                    Log.e(TAG, "onFailure: ${t.localizedMessage}")
                }


            })

    }

    private fun addHeadlinetospinner(therapies: List<Therapy>, check: Int) {
        var index = -1
        therapyHeadlines.clear()
        for (i in 0..therapies.lastIndex) {
            therapyHeadlines.add(therapies[i].headline)
            if (check == 0 && therapies[i].title == prescriptionModel?.assigned_therapy_title) {
                index = i
            }
        }
        Log.e(TAG, "addHeadlinetospinner: Headline to new list")
        val aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, therapyHeadlines)
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding?.spinner2?.adapter = aa
        if (check == 1) {
            selectTherapyTitle(therapies)
        } else {

            Log.e(TAG, "addadapterOnSpinner: ${index} ${prescriptionModel?.assigned_therapy_title}")
            binding?.spinner2?.setSelection(index)
            therapySession = mutableListOf<String>()
            for (i in 0..therapies[index].session_options.lastIndex) {
                therapySession.add(therapies[index].session_options[i].toString())
            }
            addSessionToSpinner(therapySession, check)
        }

    }

    private fun selectTherapyTitle(therapies: List<Therapy>) {
        binding?.spinner2?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {

                therapy_title = therapies[position].title.toString()
                Log.e(TAG, "onItemSelected: ${therapies[position].headline} ${therapy_title}")
                therapySession = mutableListOf<String>()
                for (i in 0..therapies[position].session_options.lastIndex) {
                    therapySession.add(therapies[position].session_options[i].toString())
                }
                addSessionToSpinner(therapySession, 1)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }


    }

    private fun addSessionToSpinner(therapySession: MutableList<String>, check: Int) {
        Log.e(TAG, "addSessionToSpinner: ==========================")
        val aa = ArrayAdapter(this, android.R.layout.simple_spinner_item, therapySession)
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding?.spinner3?.adapter = aa
        if (check == 1) {
            therapy_session = binding?.spinner3?.selectedItem.toString()
            Log.e(TAG, "addSessionToSpinner: ${therapy_session}")
            return
        } else {
            val item = therapySession.indexOf(prescriptionModel?.assigned_therapy_total_sessions.toString())
            Log.e(TAG, "addSessionToSpinner: ${item}  ${prescriptionModel?.assigned_therapy_total_sessions.toString()}")
            binding?.spinner3?.setSelection(item)
        }
    }

    private fun toggleGenBtn(gender: String) {
        binding?.genMale?.setBackgroundColor(resources.getColor(android.R.color.transparent))
        binding?.genMale?.setTextColor(resources.getColor(R.color.black))
        binding?.genFemale?.setBackgroundColor(resources.getColor(android.R.color.transparent))
        binding?.genFemale?.setTextColor(resources.getColor(R.color.black))
        binding?.genOthers?.setBackgroundColor(resources.getColor(android.R.color.transparent))
        binding?.genOthers?.setTextColor(resources.getColor(R.color.black))
        this.gender = gender
        when (gender) {
            "Male", "MALE" -> {
                binding?.genMale?.setBackgroundColor(resources.getColor(R.color.accent))
                binding?.genMale?.setTextColor(resources.getColor(R.color.white))
            }
            "Female", "FEMALE" -> {
                binding?.genFemale?.setBackgroundColor(resources.getColor(R.color.accent))
                binding?.genFemale?.setTextColor(resources.getColor(R.color.white))
            }
            else -> {
                binding?.genOthers?.setBackgroundColor(resources.getColor(R.color.accent))
                binding?.genOthers?.setTextColor(resources.getColor(R.color.white))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ActivityCompat.checkSelfPermission(this, permissions[0]!!) == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 101) {}
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("StaticFieldLeak")
    @Suppress("DEPRECATION")
    inner class DownloadFileFromUrl : AsyncTask<InputStream?, InputStream?, String?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            showProgress()
        }

        @SuppressLint("SdCardPath")
        override fun doInBackground(vararg f_url: InputStream?): String? {
            var count: Int
            try {
                filename = "${System.currentTimeMillis()}.pdf"
                val input: InputStream = BufferedInputStream(f_url[0], 8192)
                val output: OutputStream = FileOutputStream("/sdcard/$filename")
                val data = ByteArray(1024)
                var total: Long = 0
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    output.write(data, 0, count)
                }
                output.flush()
                output.close()
                input.close()
            } catch (e: Exception) {
                e.message?.let { Log.e("Error: ", it) }
            }
            return null
        }

        @SuppressLint("SdCardPath")
        override fun onPostExecute(file_url: String?) {
            dismissProgress()
            openConfirmPDFdialog()
        }
    }

    //    first_name = binding?.etFname?.text.toString()
//    last_name = binding?.etLname?.text.toString()
//    patient_age = binding?.age?.text.toString()
//    patient_gender = this.gender
//    main_complain = binding?.mainCmp?.text.toString()
//    associated_complain = binding?.assoCmpl?.text.toString()
//    complain_history = binding?.cmplHistory?.text.toString()
//    allergies = binding?.boolAllergy?.text.toString()
//    prescriptionText = binding?.diagnosis?.text.toString()
//    diagnosis = binding?.allergies?.text.toString()
//    dosh_analysis = binding?.doshaAnalysis?.text.toString()
    override fun onBackPressed() {
        if (prescriptionModel == null) {
            if (
                binding?.etFname?.text.toString().isNotEmpty() || binding?.etLname?.text.toString()
                    .isNotEmpty()
                || binding?.age?.text.toString().isNotEmpty() || binding?.mainCmp?.text.toString()
                    .isNotEmpty()
                || binding?.assoCmpl?.text.toString()
                    .isNotEmpty() || binding?.cmplHistory?.text.toString().isNotEmpty()
                || binding?.boolAllergy?.text.toString()
                    .isNotEmpty() || binding?.diagnosis?.text.toString().isNotEmpty()
                || binding?.allergies?.text.toString()
                    .isNotEmpty() || binding?.doshaAnalysis?.text.toString().isNotEmpty()
            ) {
                val builder = AlertDialog.Builder(this, R.style.AlertDialogCustom)
                builder.setTitle("Go Back")
                builder.setMessage("Prescription data entered will be lost. Are you sure you want to go back?")
                builder.setPositiveButton("Yes") { _, _ ->
                    super.onBackPressed()
                }
                builder.setNegativeButton("No") { _, _ ->

                }
                val alertDialog: AlertDialog = builder.create()
                alertDialog.setCancelable(false)
                alertDialog.show()
            } else {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }
}