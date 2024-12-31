package com.ayursh.android.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.ayursh.android.R
import com.ayursh.android.activities.auth.LoginActivity
import com.ayursh.android.adapters.IntroSliderAdapter
import com.ayursh.android.databinding.ActivityIntroBinding
import com.ayursh.android.fragments.IntroSliderFragment
import com.ayursh.android.utils.SharedPref
import com.ayursh.android.utils.showToast

class IntroActivity : AppCompatActivity() {

    private val fragmentList = ArrayList<Fragment>()
    private var binder: ActivityIntroBinding? = null
    private var adapter: IntroSliderAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder = DataBindingUtil.setContentView(this, R.layout.activity_intro)
        init()
    }

    private fun init() {
        initElements()
        initIntroSlider()
        registerListeners()
    }

    private fun initElements() {
        adapter = IntroSliderAdapter(this)
        fragmentList.addAll(
            listOf(
                IntroSliderFragment.newInstance(
                    "Consult Doctor Online",
                    "At Ayursh you get an option to take doctor's video consultation through the app",
                    "R",
                    R.drawable.intro_bg1
                ),
                IntroSliderFragment.newInstance(
                    "Book The Therapy",
                    "Book the ayurvedic therapy as per the advice of the doctor",
                    "T",
                    R.drawable.intro_bg2
                ),
                IntroSliderFragment.newInstance(
                    "At Home Service",
                    "Get the therapist at your door and enjoy the therapy at home",
                    "L",
                    R.drawable.intro_bg3
                )
            )
        )
    }


    private fun initIntroSlider() {
        binder?.vpIntroSlider?.adapter = adapter
        adapter?.setFragmentList(fragmentList)
        binder?.indicatorLayout?.setIndicatorCount(adapter!!.itemCount)
        binder?.indicatorLayout?.selectCurrentPosition(0)
    }


    private fun registerListeners() {
        binder?.vpIntroSlider?.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binder?.indicatorLayout?.selectCurrentPosition(position)
                if (position < fragmentList.lastIndex) {
                    binder?.tvSkip?.visibility = View.VISIBLE
                    binder?.getStartedBtn?.visibility = View.GONE
                } else {
                    binder?.tvSkip?.visibility = View.GONE
                    binder?.getStartedBtn?.visibility = View.VISIBLE
                }
            }
        })
        binder?.tvSkip?.setOnClickListener {
            Proceed()
        }
        binder?.getStartedBtn?.setOnClickListener {
            val position = binder?.vpIntroSlider?.currentItem
            if (position != null) {
                if (position < fragmentList.lastIndex) {
                    binder?.vpIntroSlider?.currentItem = position + 1
                } else {
                    Proceed()
                }
            } else {
                showToast("Something Went Wrong.", true)
                finish()
            }
        }
    }

    fun Proceed() {
        SharedPref.isFirstRun = false
        if (SharedPref.User.isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}