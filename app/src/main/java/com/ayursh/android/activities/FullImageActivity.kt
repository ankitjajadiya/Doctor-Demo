package com.ayursh.android.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ayursh.android.R
import com.ayursh.android.databinding.ActivityFullImageBinding
import com.ayursh.android.utils.FULLSCREEN
import com.ayursh.android.utils.showToast
import com.bumptech.glide.Glide


class FullImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullImageBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FULLSCREEN()
        binding = ActivityFullImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.getStringExtra("image_url") == null) {
            showToast("Image Url Required.")
            finish()
            return
        }
        Glide.with(this).load(intent.getStringExtra("image_url")).into(binding.image)
    }
}