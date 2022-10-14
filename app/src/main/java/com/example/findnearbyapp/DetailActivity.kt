package com.example.findnearbyapp

import android.os.Build.VERSION
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.findnearbyapp.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // View binding
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from fragment
        val data = if (VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("value", MainCardDTO::class.java)
        } else {
            intent.getParcelableExtra<MainCardDTO>("value")
        }

        binding.textView.text = data?.title
    }
}