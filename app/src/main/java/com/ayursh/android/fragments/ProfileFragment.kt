package com.ayursh.android.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ayursh.android.R
import com.ayursh.android.activities.MainActivity
import com.ayursh.android.activities.auth.LoginActivity
import com.ayursh.android.models.UserModel
import com.ayursh.android.network.responses.UserResponse
import com.ayursh.android.utils.SharedPref
import com.google.android.material.button.MaterialButton
/*
class ProfileFragment : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        Log.e("ProfileFragment", "onCreateView: " )
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        view.findViewById<MaterialButton>(R.id.logout).setOnClickListener {
            SharedPref.User.logout()
            startActivity(Intent(context, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            (context as Activity).finishAffinity()
        }

        return view
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            ProfileFragment()
    }
}*/

class ProfileFragment : Fragment() {
    private var name: String?=null
    private var email: String?=null
    private var phone: String?=null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        view.findViewById<TextView>(R.id.name).setText(SharedPref.User.USER.display_name)
        view.findViewById<TextView>(R.id.email).setText(SharedPref.User.USER.email)
        view.findViewById<TextView>(R.id.ph_num).setText(SharedPref.User.USER.phone_number)
        view.findViewById<MaterialButton>(R.id.logout).setOnClickListener {
            SharedPref.User.logout()
            startActivity(Intent(context, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
            (context as Activity).finishAffinity()
        }

        return view
    }

    companion object {

        @JvmStatic
        fun newInstance() =
            ProfileFragment()
    }
}
