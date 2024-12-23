package com.example.eatstedi.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.util.TypedValueCompat.dpToPx
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eatstedi.R
import com.example.eatstedi.adapter.MenuAdapter
import com.example.eatstedi.adapter.OrderAdapter
import com.example.eatstedi.databinding.FragmentMenuBinding
import com.example.eatstedi.databinding.ViewItemMenuBinding
import com.example.eatstedi.databinding.ViewModalAddEditMenuBinding
import com.example.eatstedi.databinding.ViewModalInvoiceOrderBinding
import com.example.eatstedi.model.MenuItem
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!
    private val selectedMenuItems = mutableListOf<MenuItem>()
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var menuAdapter: MenuAdapter

    companion object {
        private const val REQUEST_CODE_MANAGE_STOCK = 1001
    }

    // Original menu list (without filter)
     val originalMenuList = mutableListOf(
        MenuItem(1,"Nasi Goreng Spesial", "Warung Makan Bu Tuti", "Makanan", "Rp15.000", 1, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(2,"Ayam Bakar Madu", "Dapoer Ayu", "Makanan", "Rp20.000", 12, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(3,"Mie Ayam Bakso", "Bakso Pak Slamet", "Makanan", "Rp18.000", 11, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(4,"Sate Ayam", "Sate Cak Udin", "Makanan", "Rp22.000", 21, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(5,"Es Jeruk", "Warung Es Jeruk", "Minuman", "Rp6.000", 6, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(6,"Keripik Singkong", "Dapoer Ayu", "Camilan", "Rp5.000", 5, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(7,"Es Teh Manis", "Warung Es Teh", "Minuman", "Rp5.000", 5, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(8,"Es Campur", "Warung Es Campur", "Minuman", "Rp7.000", 7, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(7,"Es Kelapa Muda", "Warung Es Kelapa Muda", "Minuman", "Rp8.000", 8, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(8,"Es Cincau", "Warung Es Cincau", "Minuman", "Rp9.000", 9, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(9,"Es Doger", "Warung Es Doger", "Minuman", "Rp10.000", 10, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(10,"Keripik Singkong", "Dapoer Ayu", "Camilan", "Rp5.000", 5, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(11,"Keripik Pisang", "Dapoer Ayu", "Camilan", "Rp6.000", 6, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(12,"Keripik Tempe", "Dapoer Ayu", "Camilan", "Rp7.000", 7, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
        MenuItem(13,"Keripik Kentang", "Dapoer Ayu", "Camilan", "Rp8.000", 8, "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg")
    )

    private var filteredMenuList: List<MenuItem> = originalMenuList

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        println("Debug: Original list size = ${originalMenuList.size}")

        setupMenuRecyclerView()
        setupOrderRecyclerView()
        setupInputPayment()
        setupChipFilter()
        setupButtons()
        setupSpinner()
        setupSearchView()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MANAGE_STOCK && resultCode == Activity.RESULT_OK) {
            val menuItemId = data?.getIntExtra("MENU_ITEM_ID", 0) ?: 0
            val newStock = data?.getIntExtra("NEW_STOCK", 0) ?: 0

            // Log untuk debugging
            Log.d("MenuFragment", "Menu Item ID: $menuItemId, New Stock: $newStock")

            // Temukan menu item berdasarkan ID
            val menuItem = originalMenuList.find { it.id == menuItemId }

            menuItem?.let {
                // Memperbarui stok menu item
                it.stock = newStock // Update stok
                // Memperbarui tampilan menu
                val index = originalMenuList.indexOf(it)
                originalMenuList[index] = it // Pastikan kita memperbarui daftar asli
                menuAdapter.updateList(originalMenuList) // Memperbarui adapter

                // Log untuk memastikan menu item diperbarui
                Log.d("MenuFragment", "Updated Menu Item: $it")
            } ?: run {
                Log.d("MenuFragment", "Menu item not found for ID: $menuItemId")
            }
        }
    }

    private fun setupSearchView() {
        binding.etSearchMenu.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterMenus(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterMenus(query: String) {
        filteredMenuList = originalMenuList.filter { it.menuName.contains(query, ignoreCase = true) }
        menuAdapter.updateList(filteredMenuList)
        showNoDataMessage(filteredMenuList.isEmpty())
    }

    private fun setupChipFilter() {
        // Pastikan chip group dan chips ada
        println("Debug: Setting up chip filter")

        // Set default chip
        binding.chipAllMenu.isChecked = true

        // Tambahkan listener untuk setiap chip
        binding.apply {
            chipAllMenu.setOnClickListener {
                println("Debug: All menu chip clicked")
                menuAdapter.updateList(originalMenuList)
                showNoDataMessage(originalMenuList.isEmpty())
                println("Debug: Showing all menu items, size = ${originalMenuList.size}")
            }

            chipFoods.setOnClickListener {
                println("Debug: Foods chip clicked")
                val foodList = originalMenuList.filter { it.category == "Makanan" }
                menuAdapter.updateList(foodList)
                showNoDataMessage(foodList.isEmpty())
                println("Debug: Showing food items, size = ${foodList.size}")
            }

            chipDrinks.setOnClickListener {
                println("Debug: Drinks chip clicked")
                val drinkList = originalMenuList.filter { it.category == "Minuman" }
                menuAdapter.updateList(drinkList)
                showNoDataMessage(drinkList.isEmpty())
                println("Debug: Showing drink items, size = ${drinkList.size}")
            }

            chipSnacks.setOnClickListener {
                println("Debug: Snacks chip clicked")
                val snackList = originalMenuList.filter { it.category == "Camilan" }
                menuAdapter.updateList(snackList)
                showNoDataMessage(snackList.isEmpty())
                println("Debug: Showing snack items, size = ${snackList.size}")
            }
        }

        // Tambahkan juga listener untuk chip group
        binding.chipGroup.setOnCheckedChangeListener { group, checkedId ->
            println("Debug: ChipGroup checked changed to $checkedId")
            val selectedSupplier = binding.spFilterName.selectedItem.toString()
            when (checkedId) {
                R.id.chip_all_menu -> {
                    filterMenuBySupplierAndCategory(selectedSupplier, null)
                }
                R.id.chip_foods -> {
                    filterMenuBySupplierAndCategory(selectedSupplier, "Makanan")
                }
                R.id.chip_drinks -> {
                    filterMenuBySupplierAndCategory(selectedSupplier, "Minuman")
                }
                R.id.chip_snacks -> {
                    filterMenuBySupplierAndCategory(selectedSupplier, "Camilan")
                }
            }
        }
    }

    private fun filterMenuBySupplierAndCategory(supplierName: String, category: String?) {
        filteredMenuList = if (supplierName == "Semua Pemasok") {
            if (category != null) {
                originalMenuList.filter { it.category == category }
            } else {
                originalMenuList
            }
        } else {
            if (category != null) {
                originalMenuList.filter { it.ownerName == supplierName && it.category == category }
            } else {
                originalMenuList.filter { it.ownerName == supplierName }
            }
        }
        menuAdapter.updateList(filteredMenuList)
        showNoDataMessage(filteredMenuList.isEmpty())
        println("Debug: Supplier and category filter applied, new size = ${filteredMenuList.size}")
    }

    private fun showNoDataMessage(show: Boolean) {
        binding.tvNoData.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEditMenuDialog(menuItem: MenuItem) {
        val dialogBinding = ViewModalAddEditMenuBinding.inflate(layoutInflater)

        val categories = resources.getStringArray(R.array.menu_category)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerMenuCategory.adapter = spinnerAdapter

        // Isi form dengan data menu saat ini
        dialogBinding.etSupplierName.setText(menuItem.ownerName)
        dialogBinding.etSupplierName.setBackgroundResource(R.drawable.bg_input_disabled)
        //set etSupplierName to non editable
        dialogBinding.etSupplierName.isEnabled = false
        dialogBinding.etMenuName.setText(menuItem.menuName)
        dialogBinding.etMenuPrice.setText(menuItem.price)
        dialogBinding.spinnerMenuCategory.setSelection(categories.indexOf(menuItem.category))

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.apply {
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnSave.setOnClickListener {
                val menuName = etMenuName.text.toString()
                val menuPrice = etMenuPrice.text.toString()
                val category = spinnerMenuCategory.selectedItem.toString()

                // Lakukan pembaruan data menu di sini
                updateMenu(menuItem, menuName, menuPrice, category)
                Toast.makeText(requireContext(), "Berhasil memperbarui menu", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun updateMenu(
        menuItem: MenuItem,
        newMenuName: String,
        newMenuPrice: String,
        newCategory: String
    ) {
        // Lakukan pembaruan data menu di sini
        val updatedMenuItem = menuItem.copy(
            menuName = newMenuName,
            price = newMenuPrice,
            category = newCategory
        )

        // Perbarui daftar menu
        val index = originalMenuList.indexOf(menuItem)
        originalMenuList[index] = updatedMenuItem
        filteredMenuList = originalMenuList//.filter { it.menuName == newMenuName }
        menuAdapter.updateList(filteredMenuList)
    }

    private fun deleteMenu(menuItem: MenuItem) {
        // Hapus menu dari daftar menu
        originalMenuList.remove(menuItem)
        filteredMenuList = originalMenuList.filter { it.menuName != menuItem.menuName }
        menuAdapter.updateList(filteredMenuList)

        // Perbarui order jika ada menu yang dihapus
        selectedMenuItems.remove(menuItem)
        orderAdapter.notifyDataSetChanged()
    }

    private fun setupMenuRecyclerView() {
        menuAdapter = MenuAdapter(
            filteredMenuList,
            onEditMenu = { menuItem -> showEditMenuDialog(menuItem) },
            onDeleteMenu = { menuItem -> deleteMenu(menuItem) },
            onItemClick = { menuItem -> addItemToOrder(menuItem) }
        )

        binding.rvAllMenu.apply {
            adapter = menuAdapter

            // Dynamically adjust layout manager based on sidebar visibility
            val layoutManager = if (binding.nestedScrollView.visibility == View.VISIBLE) {
                GridLayoutManager(context, 2)
            } else {
                GridLayoutManager(context, 3)
            }

            this.layoutManager = layoutManager

            // Adjust RecyclerView's width to ensure it doesn't get hidden behind the sidebar
            this.layoutParams.width = if (binding.nestedScrollView.visibility == View.VISIBLE) {
                ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupOrderRecyclerView() {
        // Inisialisasi adapter untuk RecyclerView
        orderAdapter = OrderAdapter(selectedMenuItems) {
            // Callback untuk memperbarui total pembayaran dan memeriksa pesan
            updateTotalPayment() // Memperbarui total pembayaran
            showNoOrderMessage(selectedMenuItems.isEmpty()) // Periksa dan tampilkan pesan jika kosong
        }

        // Setup RecyclerView
        binding.rvOrderMenu.apply {
            adapter = orderAdapter
            layoutManager = LinearLayoutManager(context)
        }

        // Tambahkan listener untuk radio button
        binding.rbCash.setOnClickListener {
            binding.tvInputPayment.visibility = View.VISIBLE // Tampilkan label pembayaran
            binding.etInputPayment.visibility = View.VISIBLE // Tampilkan input pembayaran
        }

        binding.rbQris.setOnClickListener {
            binding.tvInputPayment.visibility = View.GONE // Sembunyikan label pembayaran
            binding.etInputPayment.visibility = View.GONE // Sembunyikan input pembayaran
        }

        // Listener untuk tombol bayar
        binding.btnPayNow.setOnClickListener {
            // Cek apakah tidak ada pesanan
            if (selectedMenuItems.isEmpty()) {
                Toast.makeText(requireContext(), "Tidak ada menu yang dipilih", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Jika metode pembayaran adalah Cash
            if (binding.rbCash.isChecked) {
                // Ambil input pembayaran dan hapus "Rp" untuk validasi
                val paymentInput = binding.etInputPayment.text.toString().replace("Rp", "").replace(".", "").trim()

                // Cek apakah input pembayaran kosong
                if (paymentInput.isEmpty()) {
                    Toast.makeText(requireContext(), "Mohon masukkan jumlah pembayaran", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Cek apakah jumlah pembayaran kurang dari total pesanan
                val totalPayment = selectedMenuItems.sumOf { it.price.replace("Rp", "").replace(".", "").toInt() }
                if (paymentInput.toInt() < totalPayment) {
                    Toast.makeText(requireContext(), "Jumlah pembayaran kurang", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // Jika semua validasi berhasil
                Toast.makeText(requireContext(), "Pembayaran cash berhasil!", Toast.LENGTH_SHORT).show()

            } else if (binding.rbQris.isChecked) {
                // Jika metode pembayaran adalah QRIS
                if (selectedMenuItems.isEmpty()) {
                    Toast.makeText(requireContext(), "Tidak ada menu yang dipilih", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // Jika semua validasi berhasil
                Toast.makeText(requireContext(), "Pembayaran QRIS berhasil!", Toast.LENGTH_SHORT).show()
            }

            // Tampilkan dialog invoice
            showInvoiceDialog()

            // Tampilkan pesan setelah membayar
            showNoOrderMessage(selectedMenuItems.isEmpty())

            // Reset input pembayaran
            binding.etInputPayment.setText("")
            //binding.etInputPayment.setText("Rp") // Set kembali ke default "Rp" jika menggunakan cash
        }

        // Panggil callback untuk memperbarui total pembayaran dan memeriksa pesan
        updateTotalPayment() // Memperbarui total pembayaran
        // Tampilkan pesan jika tidak ada pesanan
        showNoOrderMessage(selectedMenuItems.isEmpty())
    }

    // Inisialisasi TextWatcher untuk memformat input pembayaran
    private fun setupInputPayment() {
        // Set default text "Rp" di depan EditText
        //binding.etInputPayment.setText("Rp")

        // Tambahkan TextWatcher untuk memformat input
        binding.etInputPayment.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false // Flag untuk mencegah loop

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isFormatting) return // Jika sedang memformat, keluar dari fungsi

                // Hapus "Rp" dan format angka
                val input = s.toString().replace("Rp", "").replace(".", "").trim()
                if (input.isNotEmpty()) {
                    isFormatting = true // Set flag sebelum memformat
                    val amount = input.toInt()
                    binding.etInputPayment.setText(formatPrice(amount))
                    binding.etInputPayment.setSelection(binding.etInputPayment.text.length) // Set cursor di akhir
                    isFormatting = false // Reset flag setelah memformat
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }


    private fun showNoOrderMessage(empty: Boolean) {
        Log.d("OrderDebug", "selectedMenuItems.isEmpty(): $empty")
        binding.tvEmptyOrder.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun setupSpinner() {
        val arrayNames = resources.getStringArray(R.array.supplier_name)
        val nameAdapter = ArrayAdapter(requireContext(), R.layout.custom_item_spinner, arrayNames)
        nameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set default spinner
        binding.spFilterName.setSelection(0)

        binding.spFilterName.apply {
            adapter = nameAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedName = arrayNames[position]
                    val selectedCategory = when (binding.chipGroup.checkedChipId) {
                        R.id.chip_foods -> "Makanan"
                        R.id.chip_drinks -> "Minuman"
                        R.id.chip_snacks -> "Camilan"
                        else -> null
                    }
                    filterMenuBySupplierAndCategory(selectedName, selectedCategory)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun filterMenuBySupplier(supplierName: String) {
        filteredMenuList = if (supplierName == "Semua Pemasok") {
            originalMenuList
        } else {
            originalMenuList.filter { it.ownerName == supplierName }
        }
        menuAdapter.updateList(filteredMenuList)
        showNoDataMessage(filteredMenuList.isEmpty())
        println("Debug: Supplier filter applied, new size = ${filteredMenuList.size}")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupButtons() {
        binding.apply {
            arrowToggle.setOnClickListener { toggleSidebar() }
            btnAddNewMenu.setOnClickListener { showMenuDialog() }
        }
    }

    private fun addItemToOrder(menuItem: MenuItem) {
        // Simpan posisi scroll saat ini
        val layoutManager = binding.rvAllMenu.layoutManager as LinearLayoutManager

        // Cek apakah item sudah ada dalam daftar pesanan
        val existingItem = selectedMenuItems.find { it.id == menuItem.id }

        if (existingItem != null) {
            // Jika item sudah ada, tambahkan kuantitasnya
            existingItem.quantity += 1
            orderAdapter.notifyItemChanged(selectedMenuItems.indexOf(existingItem)) // Update item yang ada
            Toast.makeText(requireContext(), "${menuItem.menuName} sudah ditambahkan, kuantitas bertambah", Toast.LENGTH_SHORT).show()
        } else {
            // Jika item belum ada, tambahkan ke daftar pesanan
            selectedMenuItems.add(menuItem.copy(quantity = 1)) // Set kuantitas awal ke 1
            orderAdapter.notifyItemInserted(selectedMenuItems.size - 1) // Notifikasi item baru
            Toast.makeText(requireContext(), "${menuItem.menuName} ditambahkan ke pesanan", Toast.LENGTH_SHORT).show()
        }

        orderAdapter.notifyDataSetChanged()
        binding.nestedScrollView.visibility = View.VISIBLE
        binding.rvAllMenu.layoutManager = GridLayoutManager(context, 2)

        // Hitung total pembayaran setelah menambahkan item
        updateTotalPayment()

        // Tampilkan pesan jika tidak ada pesanan
        showNoOrderMessage(selectedMenuItems.isEmpty())
    }

    fun formatPrice(price: Int): String {
        val numberFormat = NumberFormat.getInstance(Locale("id", "ID"))
        return numberFormat.format(price)
    }

    private fun updateTotalPayment() {
        // Hitung total pembayaran
        val totalPayment = selectedMenuItems.sumOf {
            it.price.replace("Rp", "").replace(".", "").toInt() * it.quantity
        }

        // Tampilkan total pembayaran dengan format yang benar
        binding.tvTotalPaymentAmount.text = "Rp${formatPrice(totalPayment)}"
    }

    private fun toggleSidebar() {
        binding.nestedScrollView.visibility = if (binding.nestedScrollView.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }

        // Update GridLayout columns based on sidebar visibility
        binding.rvAllMenu.layoutManager = if (binding.nestedScrollView.visibility == View.VISIBLE) {
            GridLayoutManager(context, 2)
        } else {
            GridLayoutManager(context, 3)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showInvoiceDialog() {
        val dialogBinding = ViewModalInvoiceOrderBinding.inflate(layoutInflater)

        // Hitung total pembayaran
        val totalPayment = selectedMenuItems.sumOf {
            it.price.replace("Rp", "").replace(".", "").toInt() * it.quantity
        }

        dialogBinding.apply {
            // Atur total pembayaran
            tvTotalPaymentAmount.text = "Rp${formatPrice(totalPayment)}"

            // Tampilkan menu, harga, dan jumlah
            val menuNames = selectedMenuItems.joinToString("\n") { "${it.menuName} (x${it.quantity})" }
            val menuPrices = selectedMenuItems.joinToString("\n") {
                "Rp${formatPrice(it.price.replace("Rp", "").replace(".", "").toInt() * it.quantity)}"
            }
            tvMenuName.text = menuNames
            tvMenuPrice.text = menuPrices

            // Tampilkan tanggal, waktu, dan kasir
            val currentDateTime = LocalDateTime.now()
            val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            tvDateOrder.text = currentDateTime.format(dateFormatter)
            tvTimeOrder.text = currentDateTime.format(timeFormatter)
            tvNameEmployee.text = "Kasir"

            // Tampilkan metode pembayaran
            val paymentMethod = if (binding.rbCash.isChecked) "Cash" else "QRIS"
            tvPaymentMethodLabel.text = "Bayar ($paymentMethod)"
            tvMoneyPay.text = "Rp${formatPrice(totalPayment)}" // Tampilkan total pembayaran

            // Hitung kembalian jika menggunakan cash
            if (binding.rbCash.isChecked) {
                val paymentInput = binding.etInputPayment.text.toString().replace("Rp", "").replace(".", "").trim()
                val moneyPay = if (paymentInput.isNotEmpty()) paymentInput.toInt() else 0
                val change = moneyPay - totalPayment
                tvMoneyChange.text = "Rp${formatPrice(change)}"
            } else {
                tvMoneyChange.text = "Rp0" // Tidak ada kembalian untuk QRIS
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Mengatur agar dialog dapat dibatalkan dengan menekan di luar area dialog
        dialog.setCancelable(true)

        // Listener untuk menutup dialog dan mereset data ketika dialog dibatalkan
        dialog.setOnCancelListener {
            resetOrderData()
        }

        // Listener untuk tombol tutup
        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss() // Menutup dialog
            resetOrderData() // Mereset data pesanan
        }

        dialog.show()

        // Atur dimensi dialog (lebar penuh, tinggi sesuai konten)
        val dialogWindow = dialog.window
        dialogWindow?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Tambahkan padding di dalam dialog
        dialogWindow?.setBackgroundDrawableResource(android.R.color.transparent) // Menghilangkan background default
        dialogBinding.root.setPadding(24, 48, 24, 24) // Menambahkan padding di sekitar konten
    }

    // Metode untuk mereset data pesanan
    private fun resetOrderData() {
        selectedMenuItems.clear()
        orderAdapter.notifyDataSetChanged()
        binding.tvTotalPaymentAmount.text = "Rp0" // Reset total payment

        // Tampilkan pesan tidak ada menu ketika pesanan sudah dibayar
        showNoOrderMessage(selectedMenuItems.isEmpty())
    }

    private fun showMenuDialog() {
        val dialogBinding = ViewModalAddEditMenuBinding.inflate(layoutInflater)

        val categories = resources.getStringArray(R.array.menu_category)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerMenuCategory.adapter = spinnerAdapter

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.apply {
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnSave.setOnClickListener {
                val supplierName = etSupplierName.text.toString()
                val menuName = etMenuName.text.toString()
                val menuPrice = etMenuPrice.text.toString()
                val category = spinnerMenuCategory.selectedItem.toString()

                if (supplierName.isNotEmpty() && menuName.isNotEmpty() && menuPrice.isNotEmpty()) {
                    // Add new menu item logic here if needed
                    val newMenuItem = MenuItem(
                        originalMenuList.size + 1,
                        menuName,
                        supplierName,
                        category,
                        menuPrice,
                        0,
                        "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"
                    )
                    // Add new menu item to the list
                    originalMenuList.add(newMenuItem)
                    filteredMenuList = originalMenuList
                    menuAdapter.updateList(filteredMenuList)

                    Toast.makeText(requireContext(), "Berhasil menambahkan menu baru", Toast.LENGTH_SHORT).show()

                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Mohon isi semua data", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}