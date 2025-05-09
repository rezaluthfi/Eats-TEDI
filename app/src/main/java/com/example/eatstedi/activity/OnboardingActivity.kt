package com.example.eatstedi.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.eatstedi.R
import com.example.eatstedi.adapter.OnboardingAdapter
import com.example.eatstedi.databinding.ActivityOnboardingBinding
import com.example.eatstedi.login.LoginActivity
import com.example.eatstedi.model.OnboardingItem

class OnboardingActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityOnboardingBinding.inflate(layoutInflater)
    }

    private val onboardingItems = listOf(
        OnboardingItem(
            imageRes = R.drawable.img_onboarding_1,
            title = "Selamat Datang",
            description = "Aplikasi kasir praktis untuk kantin modern"
        ),
        OnboardingItem(
            imageRes = R.drawable.img_onboarding_2,
            title = "Cepat, Ringan, Efisien",
            description = "Transaksi tercatat otomatis dan stok terkontrol"
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Periksa token di SharedPreferences
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("auth_token", null)
        if (!savedToken.isNullOrEmpty()) {
            // Jika token ada, arahkan ke MainActivity, bukan LoginActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val adapter = OnboardingAdapter(onboardingItems)
        binding.viewPager.adapter = adapter

        // Setup custom dots indicator
        val dotsIndicator = binding.dotsIndicator
        dotsIndicator.setDotCount(onboardingItems.size)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // Update custom dots indicator
                dotsIndicator.setSelectedDot(position)

                if (position == onboardingItems.size - 1) {
                    binding.skipButton.text = "Selesai"
                } else {
                    binding.skipButton.text = "Skip"
                }
            }
        })

        binding.skipButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}