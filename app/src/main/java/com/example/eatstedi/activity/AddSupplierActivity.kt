package com.example.eatstedi.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.databinding.ActivityAddSupplierBinding

class AddSupplierActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityAddSupplierBinding.inflate(layoutInflater)
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

            btnSave.setOnClickListener {
                // Handle save button click
            }
        }
    }
}