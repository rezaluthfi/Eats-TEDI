package com.example.eatstedi

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.databinding.ActivityProfileAdminBinding

class ProfileAdminActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityProfileAdminBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        with(binding) {

            ivArrowBack.setOnClickListener {
                finish()
            }

            btnCancel.setOnClickListener {
                finish()
            }

            btnEdit.setOnClickListener {
                // Handle edit button click
            }
        }
    }
}