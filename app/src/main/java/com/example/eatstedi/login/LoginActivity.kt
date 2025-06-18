package com.example.eatstedi.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.eatstedi.R
import com.example.eatstedi.activity.MainActivity
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.LoginRequest
import com.example.eatstedi.api.service.LoginResponse
import com.example.eatstedi.databinding.ActivityLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.EOFException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LoginActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    private var currentLoginCall: Call<LoginResponse>? = null
    private val maxRetryAttempts = 2

    companion object {
        private const val TAG = "LoginActivity"
        private const val AUTH_PREFS = "auth_prefs"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PROFILE_PICTURE = "profile_picture"
        private const val RETRY_DELAY_MS = 1500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Periksa session yang tersimpan dengan validasi ketat
        checkExistingSession()

        binding.btnLoginProgress.visibility = View.GONE
        setupPasswordToggle()

        with(binding) {
            btnLogin.setOnClickListener {
                val username = etUsername.text.toString().trim()
                val password = etPassword.text.toString().trim()

                if (validateInput(username, password)) {
                    // Cancel previous login attempt if exists
                    currentLoginCall?.cancel()

                    if (isNetworkAvailable()) {
                        initiateLogin(username, password)
                    } else {
                        showError("Tidak ada koneksi internet. Periksa koneksi Anda.")
                    }
                }
            }
        }
    }

    private fun checkExistingSession() {
        val sharedPreferences = getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        val savedToken = sharedPreferences.getString(KEY_AUTH_TOKEN, null)

        // Hanya redirect jika token ada dan valid
        if (!savedToken.isNullOrEmpty() && isValidToken(savedToken)) {
            Log.d(TAG, "Valid session found, redirecting to MainActivity")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            // Clear invalid session data
            clearUserSession()
        }
    }

    private fun isValidToken(token: String): Boolean {
        // Basic token validation - check if it's not just whitespace and has reasonable length
        return token.isNotBlank() && token.length > 10
    }

    private fun validateInput(username: String, password: String): Boolean {
        when {
            username.isEmpty() -> {
                showError("Username tidak boleh kosong")
                binding.etUsername.requestFocus()
                return false
            }
            password.isEmpty() -> {
                showError("Password tidak boleh kosong")
                binding.etPassword.requestFocus()
                return false
            }
            username.length < 3 -> {
                showError("Username minimal 3 karakter")
                binding.etUsername.requestFocus()
                return false
            }
            password.length < 6 -> {
                showError("Password minimal 6 karakter")
                binding.etPassword.requestFocus()
                return false
            }
        }
        return true
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun initiateLogin(username: String, password: String) {
        // Clear any existing session before attempting login
        clearUserSession()
        loginUser(username, password, 0)
    }

    private fun setupPasswordToggle() {
        binding.ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun togglePasswordVisibility() {
        if (binding.etPassword.transformationMethod is PasswordTransformationMethod) {
            binding.etPassword.transformationMethod = null
            binding.ivTogglePassword.setImageResource(R.drawable.icon_eye_open)
        } else {
            binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.ivTogglePassword.setImageResource(R.drawable.icon_eye_closed)
        }
        binding.etPassword.setSelection(binding.etPassword.text.length)
    }

    private fun loginUser(username: String, password: String, retryCount: Int) {
        if (retryCount > maxRetryAttempts) {
            setLoginButtonState(isLoading = false)
            showError("Gagal login setelah beberapa percobaan. Silakan coba lagi nanti.")
            return
        }

        setLoginButtonState(isLoading = true)

        val apiService = RetrofitClient.getInstance(this)
        val loginRequest = LoginRequest(username, password)

        Log.d(TAG, "Attempting login - Attempt: ${retryCount + 1}")

        currentLoginCall = apiService.login(loginRequest)
        currentLoginCall?.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                // Check if the call was cancelled
                if (call.isCanceled) {
                    Log.d(TAG, "Login call was cancelled")
                    return
                }

                setLoginButtonState(isLoading = false)
                handleLoginResponse(response, username, password, retryCount)
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                // Check if the call was cancelled
                if (call.isCanceled) {
                    Log.d(TAG, "Login call was cancelled")
                    return
                }

                Log.e(TAG, "Login failed - Attempt: ${retryCount + 1}, Error: ${t.message}", t)

                // Ensure user session is cleared on any failure
                clearUserSession()

                handleLoginFailure(t, username, password, retryCount)
            }
        })
    }

    private fun handleLoginResponse(response: Response<LoginResponse>, username: String, password: String, retryCount: Int) {
        when {
            response.isSuccessful && response.body()?.success == true -> {
                val loginResponse = response.body()!!
                val token = loginResponse.token
                val user = loginResponse.data

                if (token != null && user != null && token.isNotBlank()) {
                    Log.d(TAG, "Login successful")
                    saveUserSession(token, user)
                    goToMainActivity()
                } else {
                    Log.e(TAG, "Invalid response data: token or user is null/empty")
                    clearUserSession()
                    showError("Data login tidak valid dari server")
                }
            }

            response.code() == 401 -> {
                // Unauthorized - wrong credentials
                clearUserSession()
                showError("Username atau password salah")
            }

            response.code() >= 500 -> {
                // Server error - might be temporary, try retry
                if (retryCount < maxRetryAttempts) {
                    showError("Server sedang bermasalah, mencoba lagi...")
                    retryLogin(username, password, retryCount)
                } else {
                    clearUserSession()
                    showError("Server tidak dapat dijangkau. Coba lagi nanti.")
                }
            }

            else -> {
                // Other client errors
                clearUserSession()
                val errorMessage = response.body()?.message
                    ?: response.errorBody()?.string()?.let { parseErrorMessage(it) }
                    ?: "Login gagal. Kode error: ${response.code()}"
                showError(errorMessage)
            }
        }
    }

    private fun handleLoginFailure(throwable: Throwable, username: String, password: String, retryCount: Int) {
        when (throwable) {
            is EOFException -> {
                Log.w(TAG, "EOFException occurred during login")
                if (retryCount < maxRetryAttempts) {
                    showError("Koneksi terputus, mencoba lagi...")
                    retryLogin(username, password, retryCount)
                } else {
                    setLoginButtonState(isLoading = false)
                    showError("Koneksi tidak stabil. Periksa jaringan Anda dan coba lagi.")
                }
            }

            is SocketTimeoutException -> {
                if (retryCount < maxRetryAttempts) {
                    showError("Koneksi timeout, mencoba lagi...")
                    retryLogin(username, password, retryCount)
                } else {
                    setLoginButtonState(isLoading = false)
                    showError("Koneksi timeout. Periksa jaringan Anda.")
                }
            }

            is UnknownHostException -> {
                setLoginButtonState(isLoading = false)
                showError("Tidak dapat terhubung ke server. Periksa koneksi internet Anda.")
            }

            else -> {
                if (retryCount < maxRetryAttempts && isNetworkAvailable()) {
                    showError("Terjadi kesalahan, mencoba lagi...")
                    retryLogin(username, password, retryCount)
                } else {
                    setLoginButtonState(isLoading = false)
                    showError("Gagal terhubung ke server: ${throwable.message}")
                }
            }
        }
    }

    private fun retryLogin(username: String, password: String, retryCount: Int) {
        binding.root.postDelayed({
            if (!isFinishing && !isDestroyed) {
                loginUser(username, password, retryCount + 1)
            }
        }, RETRY_DELAY_MS)
    }

    private fun setLoginButtonState(isLoading: Boolean) {
        binding.btnLoginProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLoginText.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.btnLogin.isEnabled = !isLoading

        // Disable input fields during login
        binding.etUsername.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    private fun saveUserSession(token: String, user: com.example.eatstedi.api.service.User) {
        try {
            val validRoles = listOf("admin", "cashier")
            if (user.role.isNullOrBlank() || !validRoles.contains(user.role)) {
                Log.e(TAG, "Invalid user role: ${user.role}")
                showError("Role pengguna tidak valid dari server")
                clearUserSession()
                return
            }

            val sharedPreferences = getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            editor.putString(KEY_AUTH_TOKEN, token)
            editor.putString(KEY_USER_ROLE, user.role)
            editor.putString(KEY_USER_NAME, user.name ?: "")
            editor.putInt(KEY_USER_ID, user.id)
            editor.putString(KEY_PROFILE_PICTURE, user.profile_picture ?: "")

            val success = editor.commit()

            if (success) {
                Log.d(TAG, "User session saved successfully: role=${user.role}, id=${user.id}, name=${user.name}")
            } else {
                Log.e(TAG, "Failed to save user session")
                showError("Gagal menyimpan data login")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user session", e)
            showError("Gagal menyimpan data login")
        }
    }

    private fun clearUserSession() {
        try {
            val sharedPreferences = getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.commit() // Use commit() for immediate effect
            Log.d(TAG, "User session cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing user session", e)
        }
    }

    private fun goToMainActivity() {
        try {
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            showSuccess("Login berhasil")
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to MainActivity", e)
            showError("Terjadi kesalahan saat membuka aplikasi")
        }
    }

    private fun parseErrorMessage(errorBody: String): String? {
        return try {
            val json = errorBody.replace(Regex("[\\n\\r]"), "")
            val messageMatch = Regex(""""message":"(.*?)"""").find(json)
            messageMatch?.groupValues?.get(1)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing error message", e)
            null
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
        Log.w(TAG, "Error shown to user: $message")
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        // Cancel any ongoing login request
        currentLoginCall?.cancel()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        // Don't clear session on pause, but log the state
        Log.d(TAG, "Activity paused")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Activity resumed")
    }
}