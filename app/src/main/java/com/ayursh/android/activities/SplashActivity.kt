package com.ayursh.android.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.ayursh.android.R
import com.ayursh.android.activities.auth.LoginActivity
import com.ayursh.android.activities.service.MyFirebaseMessagingService
import com.ayursh.android.utils.RemoteInvitationRecieved
import com.ayursh.android.utils.Restarter
import com.ayursh.android.utils.SharedPref
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class SplashActivity : AppCompatActivity() {

    companion object{
        var splashActivity:Activity?=null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("LongLogTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        splashActivity=this;
        if(MyFirebaseMessagingService.forsplash){
            RemoteInvitationRecieved(MyFirebaseMessagingService.splashJson)
        }
        startService(Intent(this,MyFirebaseMessagingService::class.java))
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task: Task<String?> ->
                if (!task.isSuccessful) {
                    Log.e(SplashActivity::class.java.simpleName, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                if (token != null) {

                    Log.d("SplashActivity", token)
                }
            }


        init()

    }


    private fun init() {
       // serviceStart()
        Handler().postDelayed({
            when {
                SharedPref.isFirstRun -> {
                    introScreen()
                }
                SharedPref.User.isLoggedIn -> {
                    Log.d("SplashActivity", "token")
                    goToMain()
                }
                else -> {
                    goToLogin()
                }
            }
        }, 3000)

    }

    private fun introScreen() {
        startActivity(Intent(this, IntroActivity::class.java))
        finish()
    }

    private fun goToMain() {
        if(!MyFirebaseMessagingService.forsplash) {
            Log.e("SplashActivity", "goToMain: INSIDE MAIN" )
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

}