package com.example.eatstedi

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentFrame) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        with(binding) {

            // jika sidebar aktif, padding start pada contentFrame diatur menjadi 307dp
//            drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
//                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
//                    contentFrame.setPadding(307, 0, 0, 0)
//                }
//
//                override fun onDrawerOpened(drawerView: View) {
//                    contentFrame.setPadding(307, 0, 0, 0)
//                }
//
//                override fun onDrawerClosed(drawerView: View) {
//                    contentFrame.setPadding(0, 0, 0, 0)
//                }
//
//                override fun onDrawerStateChanged(newState: Int) {
//                    // Do nothing
//                }
//            })

            // Mengatur sidebar agar selalu terbuka
            //drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)

            // Mengatur sidebar agar terbuka di awal, jika tidak maka sidebar akan tertutup
            drawerLayout.openDrawer(GravityCompat.START)


            // Mengatur overlay agar tidak menutupi konten
            drawerLayout.setScrimColor(resources.getColor(android.R.color.transparent, theme))

            // Menampilkan DashboardFragment secara default saat aplikasi dibuka
            if (savedInstanceState == null) {  // Pastikan fragment tidak ditambahkan ulang saat rotasi
                openFragment(DashboardFragment())
                //set item yang dipilih sebagai aktif
                navigationView.setCheckedItem(R.id.nav_dashboard)
            }
            // Mengatur warna item menu
           navigationView.setItemTextColor(resources.getColorStateList(R.color.selector_menu_item_text_color))


            navigationView.setNavigationItemSelectedListener { menuItem ->
                // Set item yang dipilih sebagai aktif
                navigationView.setCheckedItem(menuItem.itemId) // Menandai item yang dipilih sebagai aktif

                when (menuItem.itemId) {
                    R.id.nav_dashboard -> {
                        val fragment = DashboardFragment()
                        openFragment(fragment)
                    }
                    R.id.nav_menu -> {
                        val fragment = MenuFragment()
                        openFragment(fragment)
                    }
                    R.id.nav_history -> {
                        val fragment = HistoryFragment()
                        openFragment(fragment)
                    }
                    R.id.nav_recap -> {
                        val fragment = RecapFragment()
                        openFragment(fragment)
                    }
                    R.id.nav_log -> {
                        val fragment = LogFragment()
                        openFragment(fragment)
                    }
                    R.id.nav_logout -> {
                        // pindah ke halaman login
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
                true
            }
        }
    }

    // Fungsi untuk membuka fragment
    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.content_frame, fragment)
        transaction.addToBackStack(null)  // Optional: menambah fragment ke backstack
        transaction.commit()
    }
}
