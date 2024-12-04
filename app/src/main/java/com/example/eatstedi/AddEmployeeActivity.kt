package com.example.eatstedi

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.databinding.ActivityAddEmployeeBinding

class AddEmployeeActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityAddEmployeeBinding.inflate(layoutInflater)
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

            tvSchedule.setOnClickListener {
                val intent = Intent(this@AddEmployeeActivity, AddScheduleEmployeeActivity::class.java)
                startActivity(intent)
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