package com.example.eatstedi.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eatstedi.R
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.AdminProfileResponse
import com.example.eatstedi.api.service.CashierProfileResponse
import com.example.eatstedi.api.service.LogoutResponse
import com.example.eatstedi.databinding.ActivityMainBinding
import com.example.eatstedi.fragment.*
import com.example.eatstedi.login.LoginActivity
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.ConnectException
import java.net.UnknownHostException

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    // Launcher untuk menangani hasil dari ProfileAdminActivity atau ProfileEmployeeActivity
    private val profileUpdateLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Jika profil diupdate, panggil lagi fetchUserProfile untuk muat ulang data
                Log.d("MainActivity", "Profile updated. Refreshing user data.")
                fetchUserProfile()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.navigationView.outlineProvider = ViewOutlineProvider.BACKGROUND
        binding.navigationView.clipToOutline = true
        (binding.navigationView.getChildAt(0) as? RecyclerView)?.isVerticalScrollBarEnabled = false

        Log.d("SESSION_VALIDATION", "onCreate: Memulai validasi sesi.")
        fetchUserProfile()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupNavigationDrawer(savedInstanceState: Bundle?) {
        with(binding) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
            drawerLayout.setScrimColor(getColor(android.R.color.transparent))

            if (savedInstanceState == null) {
                openFragment(DashboardFragment())
                navigationView.setCheckedItem(R.id.nav_dashboard)
            }

            navigationView.setItemTextColor(getColorStateList(R.color.selector_menu_item_text_color))
            val headerView = navigationView.getHeaderView(0)

            headerView.findViewById<LinearLayout>(R.id.nav_header).setOnClickListener {
                val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                val role = sharedPreferences.getString("user_role", null)
                val intent = if (role == "admin") {
                    Intent(this@MainActivity, ProfileAdminActivity::class.java)
                } else {
                    Intent(this@MainActivity, ProfileEmployeeActivity::class.java).apply {
                        putExtra("EMPLOYEE_ID", sharedPreferences.getInt("user_id", 0))
                        putExtra("EMPLOYEE_NAME", sharedPreferences.getString("user_name", ""))
                        putExtra("EMPLOYEE_USERNAME", sharedPreferences.getString("user_username", ""))
                        putExtra("EMPLOYEE_PHONE", sharedPreferences.getString("user_phone", ""))
                        putExtra("EMPLOYEE_EMAIL", sharedPreferences.getString("user_email", ""))
                        putExtra("EMPLOYEE_ADDRESS", sharedPreferences.getString("user_address", ""))
                        putExtra("EMPLOYEE_STATUS", sharedPreferences.getString("user_status", ""))
                    }
                }
                // Panggil activity menggunakan launcher
                profileUpdateLauncher.launch(intent)
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
        binding.navigationView.menu.findItem(R.id.nav_log).isVisible = role == "admin"
    }

    private fun fetchUserProfile() {
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val role = sharedPreferences.getString("user_role", null)
        val token = sharedPreferences.getString("auth_token", null)

        if (token == null || role == null) {
            forceLogout("Sesi tidak ditemukan. Silakan login.")
            return
        }

        val apiService = RetrofitClient.getInstance(this)
        val onProfileFailure: (String, Throwable?) -> Unit = { context, t ->
            Log.e("SESSION_VALIDATION", "Validasi gagal: $context", t)
            forceLogout("Tidak dapat memvalidasi sesi. Periksa koneksi dan login kembali.")
        }

        when (role) {
            "cashier" -> {
                apiService.getCashierProfile().enqueue(object : Callback<CashierProfileResponse> {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onResponse(call: Call<CashierProfileResponse>, response: Response<CashierProfileResponse>) {
                        handleProfileResponse(response, "kasir",
                            onSuccess = {
                                val cashierData = it.data
                                saveUserData("cashier", cashierData.id, cashierData.name, cashierData.username, cashierData.no_telp, cashierData.email, cashierData.alamat, cashierData.status)
                                setupUIForUser("cashier", cashierData.name, cashierData.id, cashierData.email)
                            },
                            onFailure = { onProfileFailure("Gagal mendapatkan profil kasir", null) }
                        )
                    }
                    override fun onFailure(call: Call<CashierProfileResponse>, t: Throwable) = onProfileFailure("Network error kasir", t)
                })
            }
            "admin" -> {
                apiService.getAdminProfile().enqueue(object : Callback<AdminProfileResponse> {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onResponse(call: Call<AdminProfileResponse>, response: Response<AdminProfileResponse>) {
                        handleProfileResponse(response, "admin",
                            onSuccess = {
                                val adminData = it.data
                                saveUserData("admin", adminData.id, adminData.name, username = adminData.username, email = adminData.email, phone = adminData.no_telp)
                                setupUIForUser("admin", adminData.name, adminData.id, adminData.email)
                            },
                            onFailure = { onProfileFailure("Gagal mendapatkan profil admin", null) }
                        )
                    }
                    override fun onFailure(call: Call<AdminProfileResponse>, t: Throwable) = onProfileFailure("Network error admin", t)
                })
            }
            else -> {
                forceLogout("Role pengguna tidak valid.")
            }
        }
    }

    private fun <T> handleProfileResponse(response: Response<T>, roleContext: String, onSuccess: (T) -> Unit, onFailure: () -> Unit) {
        if (response.code() == 401) {
            forceLogout("Sesi Anda telah berakhir. Silakan login kembali.")
            return
        }
        val body = response.body()
        val isSuccess = when (body) {
            is CashierProfileResponse -> body.success
            is AdminProfileResponse -> body.success
            else -> false
        }
        if (response.isSuccessful && isSuccess && body != null) {
            onSuccess(body)
        } else {
            onFailure()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupUIForUser(role: String, name: String, userId: Int, email: String?) {
        updateNavHeader(name, email, userId, role)
        setupMenuVisibilityForRole(role)
        setupNavigationDrawer(null)
    }

    private fun saveUserData(role: String, id: Int, name: String, username: String? = null, phone: String? = null, email: String? = null, address: String? = null, status: String? = null) {
        getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().apply {
            putString("user_role", role)
            putInt("user_id", id)
            putString("user_name", name)
            putString("user_email", email)
            putString("user_username", username)
            putString("user_phone", phone)

            if (role == "cashier") {
                putString("user_address", address)
                putString("user_status", status)
            }
            apply()
        }
    }

    private fun updateNavHeader(name: String, email: String?, userId: Int, role: String) {
        val headerView = binding.navigationView.getHeaderView(0)
        (headerView.findViewById<TextView>(R.id.username)).text = name
        (headerView.findViewById<TextView>(R.id.email)).text = email ?: if (role == "admin") "admin@kantin.com" else "cashier@kantin.com"

        val profileImageView = headerView.findViewById<ImageView>(R.id.profile_image)

        if (role == "cashier") {
            RetrofitClient.getInstance(this).getCashierPhotoProfile(userId).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful && response.body() != null) {
                        Glide.with(this@MainActivity).load(response.body()!!.bytes())
                            .placeholder(R.drawable.img_avatar).error(R.drawable.img_avatar)
                            .circleCrop().into(profileImageView)
                    } else profileImageView.setImageResource(R.drawable.img_avatar)
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    profileImageView.setImageResource(R.drawable.img_avatar)
                }
            })
        } else {
            profileImageView.setImageResource(R.drawable.img_avatar)
        }
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .commit()
    }

    private fun logoutUser() {
        val token = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).getString("auth_token", null)
        if (token != null) {
            RetrofitClient.getInstance(this).logout("Bearer $token").enqueue(object : Callback<LogoutResponse> {
                override fun onResponse(call: Call<LogoutResponse>, response: Response<LogoutResponse>) = handleLogoutSuccess()
                override fun onFailure(call: Call<LogoutResponse>, t: Throwable) = handleLogoutSuccess()
            })
        } else {
            handleLogoutSuccess()
        }
    }

    private fun forceLogout(message: String) {
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val stuckToken = sharedPreferences.getString("auth_token", null)
        sharedPreferences.edit().clear().apply()
        if (stuckToken != null) {
            RetrofitClient.getInstance(this).logout("Bearer $stuckToken").enqueue(object : Callback<LogoutResponse> {
                override fun onResponse(call: Call<LogoutResponse>, response: Response<LogoutResponse>) { navigateToLogin(message) }
                override fun onFailure(call: Call<LogoutResponse>, t: Throwable) {
                    val errorMsg = if (t is ConnectException || t is UnknownHostException) "Tidak dapat terhubung ke server." else message
                    navigateToLogin(errorMsg)
                }
            })
        } else {
            navigateToLogin(message)
        }
    }

    private fun navigateToLogin(message: String) {
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    private fun handleLogoutSuccess(message: String = "Logout berhasil") {
        getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        navigateToLogin(message)
    }
}