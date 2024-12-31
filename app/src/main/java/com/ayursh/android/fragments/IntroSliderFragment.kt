package com.ayursh.android.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ayursh.android.R
import com.ayursh.android.databinding.FragmentIntroSliderBinding

class IntroSliderFragment : Fragment() {

    private var binder: FragmentIntroSliderBinding? = null
    private var title: String? = null
    private var desc: String? = null
    private var bgVisibleElem: String? = null
    private var bgImage: Int? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binder = DataBindingUtil.inflate(inflater, R.layout.fragment_intro_slider, container, false)
        setElements()
        return binder?.root
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setElements() {
        try {
            binder?.title?.text = title
            binder?.desc?.text = desc
            binder?.bgLeftCorner?.visibility = View.GONE
            binder?.bgRightCorner?.visibility = View.GONE
            binder?.bgTop?.visibility = View.GONE
            when {
                bgVisibleElem.equals("T") -> binder?.bgTop?.visibility = View.VISIBLE
                bgVisibleElem.equals("L") -> binder?.bgLeftCorner?.visibility = View.VISIBLE
                bgVisibleElem.equals("R") -> binder?.bgRightCorner?.visibility = View.VISIBLE
            }
            binder?.bgImage?.setImageResource(bgImage!!)
        } catch (e: Exception){
            Log.e("IntroSliderFragment", "setElements: "+e.localizedMessage )
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(title: String, desc: String, bgVisibleElem: String, bgImage: Int) =
            IntroSliderFragment().apply {
                this.title = title
                this.desc = desc
                this.bgVisibleElem = bgVisibleElem
                this.bgImage = bgImage
            }
    }
}