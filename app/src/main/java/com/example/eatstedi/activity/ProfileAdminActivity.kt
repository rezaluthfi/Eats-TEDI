package com.example.eatstedi.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.AdminProfileData
import com.example.eatstedi.api.service.AdminProfileResponse
import com.example.eatstedi.databinding.ActivityProfileAdminBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileAdminActivity : AppCompatActivity() {

    private val binding by lazy { ActivityProfileAdminBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
        fetchAdminProfile()
    }

    private fun setupClickListeners() {
        binding.ivArrowBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun fetchAdminProfile() {
        // Biarkan shimmer berjalan saat API call
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.scrollViewContent.visibility = View.GONE

        val apiService = RetrofitClient.getInstance(this)
        apiService.getAdminProfile().enqueue(object : Callback<AdminProfileResponse> {
            override fun onResponse(call: Call<AdminProfileResponse>, response: Response<AdminProfileResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val adminData = response.body()!!.data
                    updateUI(adminData)
                } else {
                    showErrorState("Gagal mengambil data profil")
                }
                // Hentikan shimmer dan tampilkan konten
                showContent()
            }

            override fun onFailure(call: Call<AdminProfileResponse>, t: Throwable) {
                showErrorState("Error jaringan: ${t.message}")
                // Hentikan shimmer dan tampilkan konten (yang sudah diisi pesan error)
                showContent()
            }
        })
    }

    // Fungsi baru untuk menghentikan shimmer dan menampilkan konten asli
    private fun showContent() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
        binding.scrollViewContent.visibility = View.VISIBLE
    }

    private fun updateUI(adminData: AdminProfileData) {
        with(binding) {
            imgMenu.setImageResource(R.drawable.img_avatar)
            tvAdminName.text = adminData.name
            etName.setText(adminData.name)
            etName.isEnabled = false
        }
    }

    private fun showErrorState(message: String) {
        with(binding) {
            imgMenu.setImageResource(R.drawable.img_avatar) // tetap tampilkan gambar default
            tvAdminName.text = "Error"
            etName.setText("Gagal memuat data")
        }
        Toast.makeText(this@ProfileAdminActivity, message, Toast.LENGTH_LONG).show()
    }
}