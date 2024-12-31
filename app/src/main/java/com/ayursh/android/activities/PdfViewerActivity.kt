//package com.ayursh.android.activities
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.content.pm.PackageManager
//import android.os.AsyncTask
//import android.os.Bundle
//import android.os.Environment
//import android.util.Log
//import android.view.View
//import android.webkit.WebView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.ayursh.android.R
//import com.ayursh.android.network.RetrofitClient
//import com.ayursh.android.utils.SharedPref
//import com.github.barteksc.pdfviewer.PDFView
//import okhttp3.ResponseBody
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import java.io.*
//import java.lang.Exception
//import java.net.HttpURLConnection
//import java.net.URL
//
//
//private const val TAG = "PdfViewerActivity"
//
//class PdfViewerActivity : AppCompatActivity() {
//    private var filename: String = ""
//    private lateinit var pdfViewer: PDFView
//    var file: File? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_pdf_viewer)
//        init()
//    }
//
//    private fun init() {
//        initElements()
//        showPdfFromFile()
//    }
//
//
//    private fun initElements() {
//        pdfViewer = findViewById(R.id.pdfViewer)
//        filename = intent.getStringExtra("filename").toString()
//        file = File("/sdcard/", filename)
//    }
//
//    private fun showPdfFromFile() {
//        pdfViewer.fromFile(file)
//            .password(null)
//            .defaultPage(0)
//            .enableSwipe(true)
//            .swipeHorizontal(false)
//            .enableDoubletap(true)
//            .onPageError { page, _ ->
//                Toast.makeText(
//                    this,
//                    "Error at page: $page", Toast.LENGTH_LONG
//                ).show()
//            }
//            .load()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//    }
//}
//
