package com.example.eatstedi

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.eatstedi.databinding.ActivityMainBinding
import com.example.eatstedi.fragment.DashboardFragment
import com.example.eatstedi.fragment.MenuFragment

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        with(binding) {

            // Menjaga sidebar tetap terbuka secara default
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN)
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
