package com.example.eatstedi.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.eatstedi.databinding.ActivitySplashScreenBinding
import com.example.eatstedi.login.LoginActivity

class SplashScreenActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivitySplashScreenBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Menunggu beberapa detik sebelum beralih ke OnboardingActivity
        Handler(Looper.getMainLooper()).postDelayed({
            // Pindah ke MainActivity setelah 3 detik
            val intent = Intent(this@SplashScreenActivity, OnboardingActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000) // Durasi splash screen (3000 ms = 3 detik)
    }
}
