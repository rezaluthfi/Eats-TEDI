package com.example.eatstedi.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.AdminProfileResponse
import com.example.eatstedi.api.service.CashierProfileResponse
import com.example.eatstedi.api.service.LogoutResponse
import com.example.eatstedi.databinding.ActivityMainBinding
import com.example.eatstedi.fragment.DashboardFragment
import com.example.eatstedi.fragment.HistoryFragment
import com.example.eatstedi.fragment.LogFragment
import com.example.eatstedi.fragment.MenuFragment
import com.example.eatstedi.fragment.RecapFragment
import com.example.eatstedi.login.LoginActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Panggil fetchUserProfile untuk mendapatkan data dan kemudian setup UI
        fetchUserProfile()
    }

    private fun setupNavigationDrawer(savedInstanceState: Bundle?) {
        with(binding) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
            drawerLayout.setScrimColor(resources.getColor(android.R.color.transparent, theme))

            if (savedInstanceState == null) {
                openFragment(DashboardFragment())
                navigationView.setCheckedItem(R.id.nav_dashboard)
            }

            navigationView.setItemTextColor(resources.getColorStateList(R.color.selector_menu_item_text_color, theme))

            val headerView = navigationView.getHeaderView(0)
            val navHeader = headerView.findViewById<LinearLayout>(R.id.nav_header)

            navHeader.setOnClickListener {
                val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val role = sharedPreferences.getString("user_role", "admin") ?: "admin"
                val userId = sharedPreferences.getInt("user_id", 0)
                val userName = sharedPreferences.getString("user_name", "") ?: ""
                val username = sharedPreferences.getString("user_username", "")
                val phone = sharedPreferences.getString("user_phone", "")
                val salary = sharedPreferences.getInt("user_salary", 0)
                val status = sharedPreferences.getString("user_status", "")

                val intent = if (role == "admin") {
                    Intent(this@MainActivity, ProfileAdminActivity::class.java)
                } else {
                    Intent(this@MainActivity, ProfileEmployeeActivity::class.java).apply {
                        putExtra("EMPLOYEE_ID", userId)
                        putExtra("EMPLOYEE_NAME", userName)
                        putExtra("EMPLOYEE_USERNAME", username)
                        putExtra("EMPLOYEE_PHONE", phone)
                        putExtra("EMPLOYEE_SALARY", salary)
                        putExtra("EMPLOYEE_STATUS", status)
                    }
                }
                startActivity(intent)
            }

            navigationView.setNavigationItemSelectedListener { menuItem ->
                navigationView.setCheckedItem(menuItem.itemId)

                when (menuItem.itemId) {
                    R.id.nav_dashboard -> openFragment(DashboardFragment())
                    R.id.nav_menu -> openFragment(MenuFragment())
                    R.id.nav_history -> openFragment(HistoryFragment())
                    R.id.nav_recap -> openFragment(RecapFragment())
                    R.id.nav_log -> openFragment(LogFragment())
                    R.id.nav_logout -> logoutUser()
                }
                true
            }
        }
    }

    private fun setupMenuVisibilityForRole(role: String) {
        val menu = binding.navigationView.menu
        val logMenuItem = menu.findItem(R.id.nav_log)

        if (role == "cashier") {
            logMenuItem.isVisible = false
        } else {
            logMenuItem.isVisible = true
        }
    }

    private fun fetchUserProfile() {
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val currentRole = sharedPreferences.getString("user_role", "admin") ?: "admin"

        val apiService = RetrofitClient.getInstance(this)

        if (currentRole == "admin") {
            apiService.getAdminProfile().enqueue(object : Callback<AdminProfileResponse> {
                override fun onResponse(call: Call<AdminProfileResponse>, response: Response<AdminProfileResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val adminData = response.body()!!.data
                        // Simpan data
                        sharedPreferences.edit().apply {
                            putString("user_name", adminData.name)
                            putString("user_role", adminData.role)
                            putInt("user_id", adminData.id)
                            apply()
                        }
                        // Update UI setelah data disimpan
                        updateNavHeader(adminData.name, adminData.id, "admin@kantin.com")
                        setupMenuVisibilityForRole(adminData.role)
                        setupNavigationDrawer(null) // Setup drawer setelah role diketahui
                    } else {
                        Toast.makeText(this@MainActivity, "Sesi tidak valid, silakan login kembali", Toast.LENGTH_LONG).show()
                        logoutUser(forceLogout = true)
                    }
                }

                override fun onFailure(call: Call<AdminProfileResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error Jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else { // 'cashier'
            apiService.getCashierProfile().enqueue(object : Callback<CashierProfileResponse> {
                override fun onResponse(call: Call<CashierProfileResponse>, response: Response<CashierProfileResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val cashierData = response.body()!!.data
                        // Simpan data
                        sharedPreferences.edit().apply {
                            putString("user_name", cashierData.name)
                            putString("user_role", cashierData.role)
                            putInt("user_id", cashierData.id)
                            putString("user_username", cashierData.username)
                            putString("user_phone", cashierData.no_telp)
                            putInt("user_salary", cashierData.salary)
                            putString("user_status", cashierData.status)
                            apply()
                        }
                        // Update UI setelah data disimpan
                        updateNavHeader(cashierData.name, cashierData.id, "cashier@kantin.com")
                        setupMenuVisibilityForRole(cashierData.role)
                        setupNavigationDrawer(null) // Setup drawer setelah role diketahui
                    } else {
                        Toast.makeText(this@MainActivity, "Sesi tidak valid, silakan login kembali", Toast.LENGTH_LONG).show()
                        logoutUser(forceLogout = true)
                    }
                }

                override fun onFailure(call: Call<CashierProfileResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error Jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun updateNavHeader(name: String, userId: Int, email: String) {
        val headerView = binding.navigationView.getHeaderView(0)
        val usernameTextView = headerView.findViewById<TextView>(R.id.username)
        val emailTextView = headerView.findViewById<TextView>(R.id.email)
        val profileImageView = headerView.findViewById<ImageView>(R.id.profile_image)

        usernameTextView.text = name
        emailTextView.text = email

        // Load profile picture menggunakan endpoint get-cashier-photo-profile
        val apiService = RetrofitClient.getInstance(this)
        apiService.getCashierPhotoProfile(userId).enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        // Konversi ResponseBody ke ByteArray
                        val imageBytes = response.body()!!.bytes()

                        // Load ByteArray ke ImageView menggunakan Glide
                        Glide.with(this@MainActivity)
                            .load(imageBytes)
                            .placeholder(R.drawable.img_avatar)
                            .error(R.drawable.img_avatar)
                            .circleCrop()
                            .into(profileImageView)

                        Log.d("MainActivity", "Profile picture loaded successfully")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error converting ResponseBody to bytes: ${e.message}", e)
                        profileImageView.setImageResource(R.drawable.img_avatar)
                    }
                } else {
                    // Jika gagal, gunakan gambar default
                    profileImageView.setImageResource(R.drawable.img_avatar)
                    Log.w("MainActivity", "Failed to load profile picture: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                // Jika error jaringan, gunakan gambar default
                profileImageView.setImageResource(R.drawable.img_avatar)
                Log.e("MainActivity", "Error loading profile picture: ${t.message}", t)
            }
        })
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }

    private fun logoutUser(forceLogout: Boolean = false) {
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

        if (forceLogout) {
            handleLogoutSuccess()
            return
        }

        val token = sharedPreferences.getString("auth_token", null)
        if (token != null) {
            val apiService = RetrofitClient.getInstance(this)
            apiService.logout("Bearer $token").enqueue(object : Callback<LogoutResponse> {
                override fun onResponse(call: Call<LogoutResponse>, response: Response<LogoutResponse>) {
                    handleLogoutSuccess()
                }

                override fun onFailure(call: Call<LogoutResponse>, t: Throwable) {
                    handleLogoutSuccess()
                }
            })
        } else {
            handleLogoutSuccess()
        }
    }

    private fun handleLogoutSuccess() {
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        Toast.makeText(this@MainActivity, "Logout berhasil", Toast.LENGTH_SHORT).show()
        val intent = Intent(this@MainActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}