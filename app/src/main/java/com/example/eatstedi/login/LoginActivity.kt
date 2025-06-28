package com.example.eatstedi.login

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.eatstedi.R
import com.example.eatstedi.activity.MainActivity
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.LoginRequest
import com.example.eatstedi.api.service.LoginResponse
import com.example.eatstedi.databinding.ActivityLoginBinding
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.EOFException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LoginActivity : AppCompatActivity() {

    private val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }
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
        setContentView(binding.root)

        checkExistingSession()
        binding.btnLoginProgress.visibility = View.GONE
        setupPasswordToggle()

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validateInput(username, password)) {
                currentLoginCall?.cancel()
                if (isNetworkAvailable()) {
                    initiateLogin(username, password)
                } else {
                    showError("Tidak ada koneksi internet.")
                }
            }
        }
    }

    private fun checkExistingSession() {
        val sharedPreferences = getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        val savedToken = sharedPreferences.getString(KEY_AUTH_TOKEN, null)
        if (!savedToken.isNullOrEmpty() && isValidToken(savedToken)) {
            Log.d(TAG, "Valid session found, redirecting to MainActivity")
            goToMainActivity()
        } else {
            clearUserSession()
        }
    }

    private fun isValidToken(token: String): Boolean = token.isNotBlank() && token.length > 10

    private fun validateInput(username: String, password: String): Boolean {
        if (username.isEmpty()) {
            showError("Username tidak boleh kosong")
            binding.etUsername.requestFocus()
            return false
        }
        if (password.isEmpty()) {
            showError("Password tidak boleh kosong")
            binding.etPassword.requestFocus()
            return false
        }
        return true
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun initiateLogin(username: String, password: String) {
        clearUserSession()
        loginUser(username, password, 0)
    }

    private fun setupPasswordToggle() {
        binding.ivTogglePassword.setOnClickListener {
            if (binding.etPassword.transformationMethod is PasswordTransformationMethod) {
                binding.etPassword.transformationMethod = null
                binding.ivTogglePassword.setImageResource(R.drawable.icon_eye_open)
            } else {
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.ivTogglePassword.setImageResource(R.drawable.icon_eye_closed)
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
    }

    private fun loginUser(username: String, password: String, retryCount: Int) {
        if (retryCount > maxRetryAttempts) {
            setLoginButtonState(false)
            showError("Gagal login setelah beberapa percobaan.")
            return
        }
        setLoginButtonState(true)
        val apiService = RetrofitClient.getInstance(this)
        currentLoginCall = apiService.login(LoginRequest(username, password))
        currentLoginCall?.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (call.isCanceled) return
                setLoginButtonState(false)
                handleLoginResponse(response, username, password, retryCount)
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                if (call.isCanceled) return
                handleLoginFailure(t, username, password, retryCount)
            }
        })
    }

    private fun handleLoginResponse(response: Response<LoginResponse>, username: String, password: String, retryCount: Int) {
        when {
            response.isSuccessful && response.body()?.success == true -> {
                val data = response.body()!!
                if (data.token != null && data.data != null && data.token.isNotBlank() && isValidUser(data.data)) {
                    goToMainActivity(data.token, data.data)
                } else {
                    clearUserSession()
                    showError("Data login tidak valid dari server.")
                }
            }
            response.code() == 401 -> {
                clearUserSession()
                val errorBodyString = response.errorBody()?.string()
                val parsedMessage = errorBodyString?.let { parseErrorMessage(it) }
                Log.d(TAG, "Received 401 Unauthorized. Parsed message: '$parsedMessage'")
                if (parsedMessage?.contains("already logged in", ignoreCase = true) == true) {
                    showError("Pengguna ini sudah login atau sesi tersangkut. Hubungi admin untuk mereset sesi.")
                } else {
                    showError("Username atau password salah")
                }
            }
            response.code() >= 500 -> {
                if (retryCount < maxRetryAttempts) {
                    retryLogin(username, password, retryCount)
                } else {
                    showError("Server tidak dapat dijangkau.")
                }
            }
            else -> {
                clearUserSession()
                showError("Login gagal. Kode: ${response.code()}")
            }
        }
    }

    private fun handleLoginFailure(throwable: Throwable, username: String, password: String, retryCount: Int) {
        clearUserSession()
        setLoginButtonState(false)
        when (throwable) {
            is EOFException, is SocketTimeoutException -> {
                if (retryCount < maxRetryAttempts) {
                    retryLogin(username, password, retryCount)
                } else {
                    showError("Koneksi tidak stabil. Periksa jaringan Anda.")
                }
            }
            is UnknownHostException -> showError("Tidak dapat terhubung ke server.")
            else -> showError("Gagal terhubung: ${throwable.message}")
        }
    }

    private fun isValidUser(user: com.example.eatstedi.api.service.User): Boolean {
        val validRoles = listOf("admin", "cashier")
        return !user.role.isNullOrBlank() && validRoles.contains(user.role.lowercase()) &&
                user.id > 0 && !user.name.isNullOrBlank()
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
        binding.etUsername.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    private fun saveUserSession(token: String, user: com.example.eatstedi.api.service.User) {
        val editor = getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE).edit()
        editor.putString(KEY_AUTH_TOKEN, token)
        editor.putString(KEY_USER_ROLE, user.role)
        editor.putString(KEY_USER_NAME, user.name)
        editor.putInt(KEY_USER_ID, user.id)
        editor.putString(KEY_PROFILE_PICTURE, user.profile_picture ?: "")
        if (!editor.commit()) {
            throw IllegalStateException("Gagal menyimpan sesi.")
        }
    }

    private fun clearUserSession() {
        getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        Log.d(TAG, "User session cleared")
    }

    private fun goToMainActivity(token: String? = null, user: com.example.eatstedi.api.service.User? = null) {
        if (token != null && user != null) {
            try {
                saveUserSession(token, user)
            } catch (e: Exception) {
                clearUserSession()
                showError("Gagal menyimpan sesi login. Coba lagi.")
                return
            }
        }
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            if (token != null) showSuccess("Login berhasil")
        } catch (e: Exception) {
            showError("Gagal membuka aplikasi.")
            if (token != null) clearUserSession()
        }
    }

    private fun parseErrorMessage(errorBody: String): String? {
        return try {
            Gson().fromJson(errorBody, LoginResponse::class.java)?.message
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error parsing JSON error message", e)
            null
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.w(TAG, "Error shown: $message")
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        currentLoginCall?.cancel()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }
}