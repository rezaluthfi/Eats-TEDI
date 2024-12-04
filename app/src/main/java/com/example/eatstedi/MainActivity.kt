package com.example.eatstedi

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.eatstedi.databinding.ActivityMainBinding
import com.example.eatstedi.fragment.DashboardFragment
import com.example.eatstedi.fragment.HistoryFragment
import com.example.eatstedi.fragment.LogFragment
import com.example.eatstedi.fragment.MenuFragment
import com.example.eatstedi.fragment.RecapFragment
import com.example.eatstedi.login.LoginActivity

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        setupNavigationDrawer(savedInstanceState)
    }

    private fun setupNavigationDrawer(savedInstanceState: Bundle?) {
        with(binding) {
            // Kunci drawer selalu terbuka untuk tablet
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)

            // Nonaktifkan overlay transparan
            drawerLayout.setScrimColor(resources.getColor(android.R.color.transparent, theme))

            // Menampilkan DashboardFragment secara default saat aplikasi dibuka
            if (savedInstanceState == null) {
                openFragment(DashboardFragment())
                navigationView.setCheckedItem(R.id.nav_dashboard)
            }

            // Mengatur warna item menu
            navigationView.setItemTextColor(resources.getColorStateList(R.color.selector_menu_item_text_color))

            // Mengakses nav_header dari NavigationView
            val headerView = navigationView.getHeaderView(0) // Mendapatkan View dari nav_header.xml
            val navHeader = headerView.findViewById<LinearLayout>(R.id.nav_header)

            // Membuka ProfileAdminActivity saat nav_header diklik
            navHeader.setOnClickListener {
                val intent = Intent(this@MainActivity, ProfileAdminActivity::class.java)
                startActivity(intent)
            }

            navigationView.setNavigationItemSelectedListener { menuItem ->
                // Set item yang dipilih sebagai aktif
                navigationView.setCheckedItem(menuItem.itemId)

                when (menuItem.itemId) {
                    R.id.nav_dashboard -> openFragment(DashboardFragment())
                    R.id.nav_menu -> openFragment(MenuFragment())
                    R.id.nav_history -> openFragment(HistoryFragment())
                    R.id.nav_recap -> openFragment(RecapFragment())
                    R.id.nav_log -> openFragment(LogFragment())
                    R.id.nav_logout -> {
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                // Jangan tutup drawer ketika memilih item
                true
            }
        }
    }

    // Fungsi untuk membuka fragment
    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.content_frame, fragment)
        transaction.addToBackStack(null) // Optional: Menambahkan ke back stack jika Anda ingin bisa kembali ke fragment sebelumnya
        transaction.commit()
    }
}