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
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private var validatedUserRole: String? = null
    private var validatedUserId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        Log.d("DEBUG_PASTI_BISA", "[MainActivity] onCreate: Memulai validasi profil.")
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

    private fun setupMenuVisibilityForRole(role: String?) {
        Log.d("DEBUG_PASTI_BISA", "[MainActivity] setupMenuVisibilityForRole: Mengatur menu untuk role: $role")
        val menu = binding.navigationView.menu
        val logMenuItem = menu.findItem(R.id.nav_log)
        logMenuItem.isVisible = role == "admin"
    }

    private fun fetchUserProfile() {
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val role = sharedPreferences.getString("user_role", null)
        val token = sharedPreferences.getString("auth_token", null)

        Log.d("DEBUG_PASTI_BISA", "[MainActivity] fetchUserProfile: Initial role from SharedPreferences: $role")

        if (token == null || role == null) {
            Log.e("DEBUG_PASTI_BISA", "[MainActivity] fetchUserProfile: No token or role found, forcing logout")
            Toast.makeText(this, "Sesi tidak valid, silakan login kembali", Toast.LENGTH_LONG).show()
            logoutUser(forceLogout = true)
            return
        }

        val apiService = RetrofitClient.getInstance(this)

        when (role) {
            "cashier" -> {
                apiService.getCashierProfile().enqueue(object : Callback<CashierProfileResponse> {
                    override fun onResponse(call: Call<CashierProfileResponse>, response: Response<CashierProfileResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Log.d("DEBUG_PASTI_BISA", "[MainActivity] fetchUserProfile: Profil KASIR berhasil didapat.")
                            val cashierData = response.body()!!.data

                            validatedUserRole = "cashier"
                            validatedUserId = cashierData.id
                            Log.d("DEBUG_PASTI_BISA", "[MainActivity] fetchUserProfile: Variabel diset ke role=$validatedUserRole, id=$validatedUserId")

                            saveUserData("cashier", cashierData.id, cashierData.name, cashierData.username, cashierData.no_telp, cashierData.salary, cashierData.status)
                            setupUIForUser("cashier", cashierData.name, cashierData.id)
                        } else {
                            Log.e("DEBUG_PASTI_BISA", "[MainActivity] fetchUserProfile: Profil KASIR gagal, kode: ${response.code()}, error: ${response.errorBody()?.string()}")
                            Toast.makeText(this@MainActivity, "Gagal memvalidasi profil kasir, silakan login kembali", Toast.LENGTH_LONG).show()
                            logoutUser(forceLogout = true)
                        }
                    }

                    override fun onFailure(call: Call<CashierProfileResponse>, t: Throwable) {
                        Log.e("DEBUG_PASTI_BISA", "[MainActivity] fetchUserProfile: Panggilan KASIR gagal (network): ${t.message}", t)
                        Toast.makeText(this@MainActivity, "Error Jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
                        logoutUser(forceLogout = true)
                    }
                })
            }
            "admin" -> {
                apiService.getAdminProfile().enqueue(object : Callback<AdminProfileResponse> {
                    override fun onResponse(call: Call<AdminProfileResponse>, response: Response<AdminProfileResponse>) {
                        if (response.isSuccessful && response.body()?.success == true) {
                            Log.d("DEBUG_PASTI_BISA", "[MainActivity] fetchUserProfile: Profil ADMIN berhasil didapat.")
                            val adminData = response.body()!!.data

                            validatedUserRole = "admin"
                            validatedUserId = adminData.id
                            Log.d("DEBUG_PASTI_BISA", "[MainActivity] fetchUserProfile: Variabel diset ke role=$validatedUserRole, id=$validatedUserId")

                            saveUserData("admin", adminData.id, adminData.name)
                            setupUIForUser("admin", adminData.name, adminData.id)
                        } else {
                            Log.e("DEBUG_PASTI_BISA", "[MainActivity] fetchUserProfile: Profil ADMIN gagal, kode: ${response.code()}, error: ${response.errorBody()?.string()}")
                            Toast.makeText(this@MainActivity, "Gagal memvalidasi profil admin, silakan login kembali", Toast.LENGTH_LONG).show()
                            logoutUser(forceLogout = true)
                        }
                    }

                    override fun onFailure(call: Call<AdminProfileResponse>, t: Throwable) {
                        Log.e("DEBUG_PASTI_BISA", "[MainActivity] fetchUserProfile: Panggilan ADMIN gagal (network): ${t.message}", t)
                        Toast.makeText(this@MainActivity, "Error Jaringan: ${t.message}", Toast.LENGTH_SHORT).show()
                        logoutUser(forceLogout = true)
                    }
                })
            }
            else -> {
                Log.e("DEBUG_PASTI_BISA", "[MainActivity] fetchUserProfile: Role tidak dikenali: $role, forcing logout")
                Toast.makeText(this, "Role tidak valid, silakan login kembali", Toast.LENGTH_LONG).show()
                logoutUser(forceLogout = true)
            }
        }
    }

    private fun saveUserData(
        role: String,
        id: Int,
        name: String,
        username: String? = null,
        phone: String? = null,
        salary: Int? = null,
        status: String? = null
    ) {
        Log.d("DEBUG_PASTI_BISA", "[MainActivity] saveUserData: Menyimpan ke SharedPreferences role=$role, id=$id")
        val sharedPreferences = getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().apply {
            putString("user_role", role)
            putInt("user_id", id)
            putString("user_name", name)
            if (role == "cashier") {
                putString("user_username", username)
                putString("user_phone", phone)
                putInt("user_salary", salary ?: 0)
                putString("user_status", status)
            }
            apply()
        }
    }

    private fun setupUIForUser(role: String, name: String, userId: Int) {
        Log.d("DEBUG_PASTI_BISA", "[MainActivity] setupUIForUser: Memulai setup UI untuk role=$role")
        updateNavHeader(name, userId, if (role == "admin") "admin@kantin.com" else "cashier@kantin.com", role)
        setupMenuVisibilityForRole(role)
        setupNavigationDrawer(null)
    }

    private fun updateNavHeader(name: String, userId: Int, email: String, role: String) {
        val headerView = binding.navigationView.getHeaderView(0)
        val usernameTextView = headerView.findViewById<TextView>(R.id.username)
        val emailTextView = headerView.findViewById<TextView>(R.id.email)
        val profileImageView = headerView.findViewById<ImageView>(R.id.profile_image)

        usernameTextView.text = name
        emailTextView.text = email

        Log.d("DEBUG_PASTI_BISA", "[MainActivity] updateNavHeader: Memuat foto profil untuk role=$role, userId=$userId")

        if (role == "cashier") {
            val apiService = RetrofitClient.getInstance(this)
            apiService.getCashierPhotoProfile(userId).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val imageBytes = response.body()!!.bytes()
                            Glide.with(this@MainActivity)
                                .load(imageBytes)
                                .placeholder(R.drawable.img_avatar)
                                .error(R.drawable.img_avatar)
                                .circleCrop()
                                .into(profileImageView)
                            Log.d("DEBUG_PASTI_BISA", "[MainActivity] updateNavHeader: Foto profil kasir berhasil dimuat")
                        } catch (e: Exception) {
                            Log.e("DEBUG_PASTI_BISA", "[MainActivity] updateNavHeader: Error memuat bytes gambar: ${e.message}", e)
                            profileImageView.setImageResource(R.drawable.img_avatar)
                        }
                    } else {
                        Log.w("DEBUG_PASTI_BISA", "[MainActivity] updateNavHeader: Gagal memuat foto profil kasir, kode: ${response.code()}, error: ${response.message()}")
                        profileImageView.setImageResource(R.drawable.img_avatar)
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("DEBUG_PASTI_BISA", "[MainActivity] updateNavHeader: Gagal memuat foto profil kasir (network): ${t.message}", t)
                    profileImageView.setImageResource(R.drawable.img_avatar)
                }
            })
        } else {
            // Fallback untuk admin karena tidak ada endpoint getAdminPhotoProfile
            Log.d("DEBUG_PASTI_BISA", "[MainActivity] updateNavHeader: Tidak ada endpoint foto profil untuk admin, menggunakan default")
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
        sharedPreferences.edit().clear().commit()
        Log.d("DEBUG_PASTI_BISA", "[MainActivity] handleLogoutSuccess: SharedPreferences cleared")
        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}