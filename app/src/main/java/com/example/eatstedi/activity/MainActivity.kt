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
                val intent = if (role == "admin") {
                    Intent(this@MainActivity, ProfileAdminActivity::class.java)
                } else {
                    Intent(this@MainActivity, ProfileEmployeeActivity::class.java)
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

        // --- PERBAIKAN UTAMA DI SINI ---
        // Pisahkan panggilan API menjadi dua blok terpisah
        if (currentRole == "admin") {
            apiService.getAdminProfile().enqueue(object : Callback<AdminProfileResponse> {
                override fun onResponse(call: Call<AdminProfileResponse>, response: Response<AdminProfileResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        val adminData = response.body()!!.data
                        // Simpan data
                        sharedPreferences.edit().apply {
                            putString("user_name", adminData.name)
                            putString("user_role", adminData.role)
                            putString("profile_picture", adminData.profile_picture)
                            putInt("user_id", adminData.id)
                            apply()
                        }
                        // Update UI setelah data disimpan
                        updateNavHeader(adminData.name, adminData.profile_picture, "admin@kantin.com")
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
                            putString("profile_picture", cashierData.profile_picture)
                            putInt("user_id", cashierData.id)
                            // Anda bisa menyimpan data tambahan jika perlu
                            apply()
                        }
                        // Update UI setelah data disimpan
                        updateNavHeader(cashierData.name, cashierData.profile_picture, "cashier@kantin.com")
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

    private fun updateNavHeader(name: String, profilePicture: String?, email: String) {
        val headerView = binding.navigationView.getHeaderView(0)
        val usernameTextView = headerView.findViewById<TextView>(R.id.username)
        val emailTextView = headerView.findViewById<TextView>(R.id.email)
        val profileImageView = headerView.findViewById<ImageView>(R.id.profile_image)

        usernameTextView.text = name
        emailTextView.text = email

        if (!profilePicture.isNullOrEmpty()) {
            Glide.with(this)
                .load(profilePicture)
                .placeholder(R.drawable.img_avatar)
                .error(R.drawable.img_avatar)
                .circleCrop()
                .into(profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.img_avatar)
        }
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