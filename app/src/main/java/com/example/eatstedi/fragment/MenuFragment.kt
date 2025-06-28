package com.example.eatstedi.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.eatstedi.R
import com.example.eatstedi.activity.ManageStockMenuActivity
import com.example.eatstedi.adapter.MenuAdapter
import com.example.eatstedi.adapter.OrderAdapter
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.ApiService
import com.example.eatstedi.api.service.CreateReceiptRequest
import com.example.eatstedi.api.service.CreateReceiptResponse
import com.example.eatstedi.api.service.GenericResponse
import com.example.eatstedi.api.service.MenuResponse
import com.example.eatstedi.api.service.SingleMenuResponse
import com.example.eatstedi.api.service.SearchRequest
import com.example.eatstedi.api.service.SupplierResponse
import com.example.eatstedi.databinding.FragmentMenuBinding
import com.example.eatstedi.databinding.ViewDialogConfirmDeleteMenuBinding
import com.example.eatstedi.databinding.ViewModalAddEditMenuBinding
import com.example.eatstedi.databinding.ViewModalInvoiceOrderBinding
import com.example.eatstedi.model.MenuItem
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!
    private val selectedMenuItems = mutableListOf<MenuItem>()
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var menuAdapter: MenuAdapter
    private lateinit var apiService: ApiService
    private var originalMenuList: MutableList<MenuItem> = mutableListOf()
    private var filteredMenuList: List<MenuItem> = originalMenuList
    private var selectedImageUri: Uri? = null
    private var isInitialFetchComplete: Boolean = false
    private var currentRequestIdMenu: Map<String, Int>? = null
    private var supplierList: List<Pair<Int, String>> = listOf(Pair(0, "Semua Pemasok"))
    private var userRole: String? = null

    // TAMBAHKAN KODE INI DI BAGIAN ATAS CLASS
    private var lastRecyclerViewWidth = 0
    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        // Cek jika binding masih ada dan fragment ter-attach
        _binding?.takeIf { isAdded }?.let {
            val currentWidth = it.rvAllMenu.width
            // Hanya hitung ulang jika lebar berubah secara signifikan (mis > 10px)
            // dan lebarnya tidak nol.
            if (currentWidth > 0 && currentWidth != lastRecyclerViewWidth) {
                lastRecyclerViewWidth = currentWidth // Simpan lebar terakhir

                val columnWidth = resources.getDimensionPixelSize(R.dimen.menu_column_width)
                if (columnWidth > 0) {
                    val spanCount = max(1, currentWidth / columnWidth)
                    val currentLayoutManager = it.rvAllMenu.layoutManager as? GridLayoutManager
                    // Hanya set jika span count berubah untuk efisiensi
                    if (currentLayoutManager?.spanCount != spanCount) {
                        it.rvAllMenu.layoutManager = GridLayoutManager(context, spanCount)
                        Log.d("MenuFragment", "Layout automatically recalculated. New SpanCount: $spanCount")
                    }
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_MANAGE_STOCK = 1001
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Izin akses galeri ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            Log.d("MenuFragment", "Image selected: $uri")
            selectedImageUri = uri
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .error(R.color.white)
                .into(dialogBinding.imgMenu)
        } else {
            Log.w("MenuFragment", "No image selected")
            Toast.makeText(requireContext(), "Tidak ada gambar yang dipilih", Toast.LENGTH_SHORT).show()
            dialogBinding.imgMenu.setImageResource(R.color.white)
        }
    }

    private lateinit var dialogBinding: ViewModalAddEditMenuBinding

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

        // Retrieve user role from SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        userRole = sharedPreferences.getString("user_role", "cashier")

        binding.tvNoData.visibility = View.GONE
        binding.rvAllMenu.visibility = View.GONE
        Log.d("MenuFragment", "onViewCreated: tv_no_data set to GONE, rvAllMenu set to GONE")

        // Adjust UI based on user role
        adjustUIBasedOnRole()

        apiService = RetrofitClient.getInstance(requireContext())

        // Atur LayoutManager sementara untuk mencegah crash
        binding.rvAllMenu.layoutManager = GridLayoutManager(context, 2)

        // Inisialisasi adapter terlebih dahulu
        setupMenuAdapter()

        // Lisener untuk mengatur ulang layout RecyclerView saat ukuran berubah
        binding.rvAllMenu.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)

        if (userRole?.lowercase() == "cashier") {
            setupOrderRecyclerView()
            setupInputPayment()
        }
        setupChipFilter()
        setupButtons()

        setupSpinner()
        fetchMenuFromServer()
        setupSearchView()
    }

    private fun adjustUIBasedOnRole() {
        when (userRole?.lowercase()) {
            "admin" -> {
                binding.btnAddNewMenu.visibility = View.VISIBLE
                // Sembunyikan elemen UI terkait transaksi untuk Admin
                binding.nestedScrollView.visibility = View.GONE
                binding.arrowToggle.visibility = View.GONE
                binding.rgPaymentMethod.visibility = View.GONE
                binding.btnPayNow.visibility = View.GONE
                binding.tvInputPayment.visibility = View.GONE
                binding.etInputPayment.visibility = View.GONE
                binding.rvOrderMenu.visibility = View.GONE
                binding.tvEmptyOrder.visibility = View.GONE
                binding.tvTotalPaymentAmount.visibility = View.GONE
                binding.tvTotalPaymentLabel.visibility = View.GONE
            }
            "cashier" -> {
                binding.btnAddNewMenu.visibility = View.GONE
                binding.nestedScrollView.visibility = View.GONE
                binding.arrowToggle.visibility = View.VISIBLE
                binding.rgPaymentMethod.visibility = View.VISIBLE
                binding.btnPayNow.visibility = View.VISIBLE
                binding.tvInputPayment.visibility = View.VISIBLE
                binding.etInputPayment.visibility = View.VISIBLE
                binding.rvOrderMenu.visibility = View.VISIBLE
                binding.tvEmptyOrder.visibility = View.VISIBLE
                binding.tvTotalPaymentAmount.visibility = View.VISIBLE
                binding.tvTotalPaymentLabel.visibility = View.VISIBLE
            }
            else -> {
                Log.w("MenuFragment", "Unknown user role: $userRole, defaulting to cashier permissions")
                binding.btnAddNewMenu.visibility = View.GONE
                binding.nestedScrollView.visibility = View.GONE
                binding.arrowToggle.visibility = View.VISIBLE
                binding.rgPaymentMethod.visibility = View.VISIBLE
                binding.btnPayNow.visibility = View.VISIBLE
                binding.tvInputPayment.visibility = View.VISIBLE
                binding.etInputPayment.visibility = View.VISIBLE
                binding.rvOrderMenu.visibility = View.VISIBLE
                binding.tvEmptyOrder.visibility = View.VISIBLE
                binding.tvTotalPaymentAmount.visibility = View.VISIBLE
                binding.tvTotalPaymentLabel.visibility = View.VISIBLE
            }
        }
    }

    private fun setupMenuAdapter() {
        menuAdapter = MenuAdapter(
            mutableListOf(),
            userRole ?: "cashier",
            onItemClick = { menuItem -> addItemToOrder(menuItem) },
            onEditMenu = { menuItem ->
                if (userRole?.lowercase() == "admin") {
                    showEditMenuDialog(menuItem)
                }
            },
            onDeleteMenu = { menuItem -> showConfirmDeleteDialog(menuItem) },
            onSetStock = { menuItem ->
                val intent = Intent(requireContext(), ManageStockMenuActivity::class.java).apply {
                    putExtra("MENU_ITEM_ID", menuItem.id)
                    putExtra("MENU_ITEM_STOCK", menuItem.stock)
                    putExtra("MENU_ITEM_NAME", menuItem.menuName)
                    putExtra("MENU_ITEM_IMAGE_URL", menuItem.imageUrl)
                }
                startActivityForResult(intent, REQUEST_CODE_MANAGE_STOCK)
            }
        )
        binding.rvAllMenu.adapter = menuAdapter
        Log.d("MenuFragment", "setupMenuAdapter: MenuAdapter initialized with role=$userRole")
    }

    private fun showProgressBar() {
        _binding?.let { binding ->
            binding.progressBar.visibility = View.VISIBLE
            binding.tvNoData.visibility = View.GONE
            binding.rvAllMenu.visibility = View.GONE
            Log.d("MenuFragment", "showProgressBar: Showing progress bar, tv_no_data GONE, rvAllMenu GONE")
        }
    }

    private fun hideProgressBar() {
        _binding?.let { binding ->
            binding.progressBar.visibility = View.GONE
            Log.d("MenuFragment", "hideProgressBar: Hiding progress bar")
        }
    }

    private fun fetchSuppliers(onSuppliersFetched: (List<Pair<Int, String>>) -> Unit) {
        // Tidak perlu progress bar di sini karena fetchMenuFromServer sudah menanganinya
        apiService.getSuppliers().enqueue(object : Callback<SupplierResponse> {
            override fun onResponse(call: Call<SupplierResponse>, response: Response<SupplierResponse>) {
                if (!isAdded || _binding == null) return

                if (response.isSuccessful && response.body()?.success == true) {
                    val suppliers = response.body()?.data?.map { Pair(it.id, it.name) } ?: emptyList()
                    onSuppliersFetched(suppliers)

                    // PERUBAHAN: Setelah supplier ter-fetch, update ulang menu yang mungkin sudah tampil
                    // agar nama supplier yang tadinya "Memuat..." menjadi nama aslinya.
                    if (originalMenuList.isNotEmpty()) {
                        originalMenuList.forEach { menuItem ->
                            val supplierName = supplierList.find { it.first == menuItem.idSupplier }?.second
                            menuItem.supplierName = supplierName ?: "Pemasok Tdk Dikenal"
                        }
                        // Update adapter jika ada perubahan
                        menuAdapter.updateList(filteredMenuList)
                    }

                } else {
                    handleErrorResponse(response, "Gagal mengambil daftar pemasok")
                    onSuppliersFetched(emptyList())
                }
            }

            override fun onFailure(call: Call<SupplierResponse>, t: Throwable) {
                if (!isAdded || _binding == null) return
                handleNetworkError(t, "Error fetching suppliers")
                onSuppliersFetched(emptyList())
            }
        })
    }

    private fun fetchMenuFromServer() {
        // Tampilkan progress bar hanya jika ini adalah fetch pertama kali
        if (originalMenuList.isEmpty()) {
            showProgressBar()
        }

        apiService.getMenu().enqueue(object : Callback<MenuResponse> {
            override fun onResponse(call: Call<MenuResponse>, response: Response<MenuResponse>) {
                if (!isAdded || _binding == null) return

                hideProgressBar()
                if (response.isSuccessful && response.body()?.success == true) {
                    originalMenuList.clear()
                    originalMenuList.addAll(mapMenuItemsWithSuppliers(response.body()?.data ?: emptyList()))
                    filteredMenuList = originalMenuList
                    menuAdapter.updateList(filteredMenuList)
                    isInitialFetchComplete = true
                    updateVisibility()

                    Log.d("MenuFragment", "Menu fetched: ${originalMenuList.size} items, IDs: ${originalMenuList.map { it.id }}, Suppliers: ${originalMenuList.map { it.supplierName }}")
                    if (originalMenuList.isNotEmpty()) {
                        Toast.makeText(requireContext(), "Berhasil mengambil ${originalMenuList.size} menu", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    originalMenuList.clear()
                    filteredMenuList = originalMenuList
                    menuAdapter.updateList(filteredMenuList)
                    isInitialFetchComplete = true
                    updateVisibility()
                    handleErrorResponse(response, "Gagal mengambil menu")
                }
            }

            override fun onFailure(call: Call<MenuResponse>, t: Throwable) {
                if (!isAdded || _binding == null) return

                hideProgressBar()
                originalMenuList.clear()
                filteredMenuList = originalMenuList
                menuAdapter.updateList(filteredMenuList)
                isInitialFetchComplete = true
                updateVisibility()
                handleNetworkError(t, "Error fetching menu")
            }
        })
    }

    private fun mapMenuItemsWithSuppliers(menuItems: List<MenuItem>): List<MenuItem> {
        // Cek jika supplierList masih kosong (race condition)
        if (supplierList.size <= 1) { // Hanya berisi "Semua Pemasok"
            Log.w("MenuFragment", "Mapping menu items when supplierList is not ready yet.")
        }
        return menuItems.map { menuItem ->
            val supplierName = supplierList.find { it.first == menuItem.idSupplier }?.second
            // Tampilkan "Memuat..." jika nama supplier belum tersedia, atau nama aslinya jika sudah.
            menuItem.copy(supplierName = supplierName ?: "Memuat...")
        }
    }

    private fun handleErrorResponse(response: Response<*>, defaultMessage: String) {
        val message = try {
            response.errorBody()?.string()
                ?: response.body()?.javaClass?.getDeclaredField("message")?.apply { isAccessible = true }?.get(response.body())?.toString()
                ?: defaultMessage
        } catch (e: Exception) {
            defaultMessage
        }
        Log.e("MenuFragment", "$defaultMessage: $message")
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        if (response.code() == 401) {
            Toast.makeText(requireContext(), "Sesi habis, silakan login kembali", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleNetworkError(t: Throwable, context: String) {
        Log.e("MenuFragment", "$context: ${t.message}", t)
        Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_LONG).show()
    }

    private fun updateVisibility() {
        _binding?.let { binding ->
            Log.d("MenuFragment", "updateVisibility: filteredMenuList size=${filteredMenuList.size}, initialFetchComplete=${isInitialFetchComplete}")
            // Tampilkan "No Data" hanya jika fetch sudah selesai dan listnya benar-benar kosong.
            val isEmpty = filteredMenuList.isEmpty() && isInitialFetchComplete
            binding.rvAllMenu.visibility = if (filteredMenuList.isEmpty()) View.GONE else View.VISIBLE
            binding.tvNoData.visibility = if (isEmpty) View.VISIBLE else View.GONE

            // Sembunyikan progress bar jika fetch awal selesai
            if (isInitialFetchComplete) {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MANAGE_STOCK && resultCode == Activity.RESULT_OK) {
            val menuItemId = data?.getIntExtra("MENU_ITEM_ID", 0) ?: 0
            val newStock = data?.getIntExtra("NEW_STOCK", 0) ?: 0
            Log.d("MenuFragment", "Menu Item ID: $menuItemId, New Stock: $newStock")
            val menuItem = originalMenuList.find { it.id == menuItemId }
            menuItem?.let {
                it.stock = newStock
                val index = originalMenuList.indexOf(it)
                originalMenuList[index] = it
                menuAdapter.updateList(filteredMenuList)
                updateVisibility()
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
        if (query.isEmpty()) {
            binding.chipGroup.setOnCheckedChangeListener(null)
            binding.spFilterName.onItemSelectedListener = null

            filteredMenuList = originalMenuList
            menuAdapter.updateList(filteredMenuList)
            binding.chipAllMenu.isChecked = true
            binding.spFilterName.setSelection(0)
            updateVisibility()
            Log.d("MenuFragment", "filterMenus: Reset to original list, originalSize=${originalMenuList.size}, filteredSize=${filteredMenuList.size}")

            setupChipFilterListener()
            setupSpinnerListener()
            return
        }
        showProgressBar()
        apiService.searchMenu(SearchRequest(query)).enqueue(object : Callback<MenuResponse> {
            override fun onResponse(call: Call<MenuResponse>, response: Response<MenuResponse>) {
                // PERBAIKAN 1: Tambahkan pengecekan keamanan
                if (!isAdded || _binding == null) return

                hideProgressBar()
                if (response.isSuccessful && response.body()?.success == true) {
                    filteredMenuList = mapMenuItemsWithSuppliers(response.body()?.data ?: emptyList())
                    menuAdapter.updateList(filteredMenuList)
                    updateVisibility()
                    Log.d("MenuFragment", "Search result for query='$query': ${filteredMenuList.size} items")
                } else {
                    filteredMenuList = emptyList()
                    menuAdapter.updateList(filteredMenuList)
                    updateVisibility()
                    Log.e("MenuFragment", "Search failed for query='$query': ${response.errorBody()?.string() ?: "Unknown error"}")
                }
            }

            override fun onFailure(call: Call<MenuResponse>, t: Throwable) {
                // PERBAIKAN 1: Tambahkan pengecekan keamanan
                if (!isAdded || _binding == null) return

                hideProgressBar()
                filteredMenuList = emptyList()
                menuAdapter.updateList(filteredMenuList)
                updateVisibility()
                Log.e("MenuFragment", "Search network error: ${t.message}", t)
            }
        })
    }

    private fun setupChipFilter() {
        binding.chipAllMenu.isChecked = true
        binding.apply {
            chipAllMenu.setOnClickListener {
                filteredMenuList = originalMenuList
                menuAdapter.updateList(filteredMenuList)
                updateVisibility()
                Log.d("MenuFragment", "Showing all menu items, size=${filteredMenuList.size}")
            }
            chipFoods.setOnClickListener { filterByFoodType("food") }
            chipDrinks.setOnClickListener { filterByFoodType("drink") }
            chipSnacks.setOnClickListener { filterByFoodType("snack") }
        }
        setupChipFilterListener()
    }

    private fun setupChipFilterListener() {
        binding.chipGroup.setOnCheckedChangeListener { _, checkedId ->
            if (!isInitialFetchComplete) return@setOnCheckedChangeListener
            val selectedSupplier = binding.spFilterName.selectedItemPosition
            when (checkedId) {
                R.id.chip_all_menu -> filterMenuBySupplierAndCategory(selectedSupplier, null)
                R.id.chip_foods -> filterMenuBySupplierAndCategory(selectedSupplier, "food")
                R.id.chip_drinks -> filterMenuBySupplierAndCategory(selectedSupplier, "drink")
                R.id.chip_snacks -> filterMenuBySupplierAndCategory(selectedSupplier, "snack")
            }
        }
    }

    private fun filterByFoodType(foodType: String) {
        showProgressBar()
        apiService.filterByFoodType(foodType).enqueue(object : Callback<MenuResponse> {
            override fun onResponse(call: Call<MenuResponse>, response: Response<MenuResponse>) {
                // PERBAIKAN 1: Tambahkan pengecekan keamanan
                if (!isAdded || _binding == null) return

                hideProgressBar()
                if (response.isSuccessful && response.body()?.success == true) {
                    filteredMenuList = mapMenuItemsWithSuppliers(response.body()?.data ?: emptyList())
                    menuAdapter.updateList(filteredMenuList)
                    updateVisibility()
                    Log.d("MenuFragment", "Filtered by food type '$foodType': ${filteredMenuList.size} items")
                } else {
                    filteredMenuList = emptyList()
                    menuAdapter.updateList(filteredMenuList)
                    updateVisibility()
                    handleErrorResponse(response, "Gagal memfilter berdasarkan tipe makanan")
                }
            }

            override fun onFailure(call: Call<MenuResponse>, t: Throwable) {
                // PERBAIKAN 1: Tambahkan pengecekan keamanan
                if (!isAdded || _binding == null) return

                hideProgressBar()
                filteredMenuList = emptyList()
                menuAdapter.updateList(filteredMenuList)
                updateVisibility()
                handleNetworkError(t, "Error filtering by food type")
            }
        })
    }

    private fun filterMenuBySupplierAndCategory(supplierId: Int, foodType: String?) {
        if (!isInitialFetchComplete) {
            Log.d("MenuFragment", "Skipping filterMenuBySupplierAndCategory: Initial fetch not complete")
            return
        }
        if (supplierId == 0 && foodType == null) {
            filteredMenuList = originalMenuList
            menuAdapter.updateList(filteredMenuList)
            updateVisibility()
            Log.d("MenuFragment", "Reset to all menu items, size=${filteredMenuList.size}")
            return
        }
        if (supplierId == 0) {
            filterByFoodType(foodType!!)
            return
        }
        showProgressBar()
        apiService.filterBySupplier(supplierId).enqueue(object : Callback<MenuResponse> {
            override fun onResponse(call: Call<MenuResponse>, response: Response<MenuResponse>) {
                // PERBAIKAN 1: Tambahkan pengecekan keamanan
                if (!isAdded || _binding == null) return

                hideProgressBar()
                if (response.isSuccessful && response.body()?.success == true) {
                    var result = mapMenuItemsWithSuppliers(response.body()?.data ?: emptyList())
                    if (foodType != null) {
                        result = result.filter { it.foodType == foodType }
                    }
                    filteredMenuList = result
                    menuAdapter.updateList(filteredMenuList)
                    updateVisibility()
                    Log.d("MenuFragment", "Filtered by supplier $supplierId and food type '$foodType': ${filteredMenuList.size} items")
                } else {
                    filteredMenuList = emptyList()
                    menuAdapter.updateList(filteredMenuList)
                    updateVisibility()
                    handleErrorResponse(response, "Gagal memfilter berdasarkan pemasok")
                }
            }

            override fun onFailure(call: Call<MenuResponse>, t: Throwable) {
                // PERBAIKAN 1: Tambahkan pengecekan keamanan
                if (!isAdded || _binding == null) return

                hideProgressBar()
                filteredMenuList = emptyList()
                menuAdapter.updateList(filteredMenuList)
                updateVisibility()
                handleNetworkError(t, "Error filtering by supplier")
            }
        })
    }

    private fun applyCurrentFilter() {
        val selectedSupplierId = supplierList[binding.spFilterName.selectedItemPosition].first
        val selectedFoodType = when (binding.chipGroup.checkedChipId) {
            R.id.chip_foods -> "food"
            R.id.chip_drinks -> "drink"
            R.id.chip_snacks -> "snack"
            else -> null
        }
        if (selectedSupplierId == 0 && selectedFoodType == null) {
            filteredMenuList = originalMenuList
        } else if (selectedSupplierId == 0) {
            filteredMenuList = originalMenuList.filter { it.foodType == selectedFoodType }
        } else {
            filteredMenuList = originalMenuList.filter { it.idSupplier == selectedSupplierId }
            if (selectedFoodType != null) {
                filteredMenuList = filteredMenuList.filter { it.foodType == selectedFoodType }
            }
        }
        Log.d("MenuFragment", "applyCurrentFilter: filteredMenuList size=${filteredMenuList.size}")
    }

    private fun showEditMenuDialog(menuItem: MenuItem) {
        dialogBinding = ViewModalAddEditMenuBinding.inflate(layoutInflater)
        Log.d("MenuFragment", "showEditMenuDialog for: ${menuItem.menuName}")

        // Setup Spinner
        val supplierNames = supplierList.map { it.second }.toTypedArray()
        val supplierAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, supplierNames)
        supplierAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerSupplierName.adapter = supplierAdapter
        val supplierIndex = supplierList.indexOfFirst { it.first == menuItem.idSupplier }
        if (supplierIndex != -1) {
            dialogBinding.spinnerSupplierName.setSelection(supplierIndex)
        }
        dialogBinding.spinnerSupplierName.isEnabled = false

        // Setup data teks
        dialogBinding.etMenuName.setText(menuItem.menuName)
        dialogBinding.etMenuPrice.setText(menuItem.price.toString())
        setupCategorySelection(dialogBinding, menuItem.foodType)

        // Pertama, set placeholder
        dialogBinding.imgMenu.setImageResource(R.drawable.ic_launcher_background)
        // Kemudian, panggil API untuk memuat gambar asli
        apiService.getMenuPhoto(menuItem.id).enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    // Pastikan fragment dan view masih ada untuk menghindari crash
                    if (isAdded && _binding != null) {
                        val imageBytes = response.body()!!.bytes()
                        Glide.with(requireContext())
                            .load(imageBytes)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .centerCrop()
                            .into(dialogBinding.imgMenu)
                    }
                }
            }
            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                if (isAdded && _binding != null) {
                    dialogBinding.imgMenu.setImageResource(R.drawable.ic_launcher_background)
                    Log.e("MenuFragment", "Failed to load image for edit dialog", t)
                }
            }
        })

        dialogBinding.btnCameraMenu.setOnClickListener { checkStoragePermission() }

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogBinding.root).create()

        // Setup listener tombol
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnSave.setOnClickListener {
            val menuName = dialogBinding.etMenuName.text.toString().trim()
            val menuPrice = dialogBinding.etMenuPrice.text.toString().toIntOrNull() ?: 0
            val foodType = getSelectedFoodType(dialogBinding)

            if (menuName.isNotEmpty() && menuPrice > 0 && foodType.isNotEmpty()) {
                updateMenu(menuItem.id, menuName, menuPrice, foodType)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Mohon isi semua data dengan benar", Toast.LENGTH_SHORT).show()
            }
        }

        // Disable fields jika bukan admin
        if (userRole?.lowercase() != "admin") {
            dialogBinding.etMenuName.isEnabled = false
            dialogBinding.etMenuPrice.isEnabled = false
            dialogBinding.btnCameraMenu.visibility = View.GONE
            dialogBinding.categoryFood.isEnabled = false
            dialogBinding.categorySnack.isEnabled = false
            dialogBinding.categoryDrink.isEnabled = false
            dialogBinding.btnSave.isEnabled = false
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }

    private fun setupCategorySelection(binding: ViewModalAddEditMenuBinding, foodType: String?) {
        binding.categoryFood.setBackgroundResource(R.drawable.bg_outline)
        binding.categorySnack.setBackgroundResource(R.drawable.bg_outline)
        binding.categoryDrink.setBackgroundResource(R.drawable.bg_outline)

        when (foodType?.lowercase()) {
            "food" -> binding.categoryFood.setBackgroundResource(R.drawable.bg_selected_category)
            "snack" -> binding.categorySnack.setBackgroundResource(R.drawable.bg_selected_category)
            "drink" -> binding.categoryDrink.setBackgroundResource(R.drawable.bg_selected_category)
            else -> binding.categoryFood.setBackgroundResource(R.drawable.bg_selected_category)
        }

        binding.categoryFood.setOnClickListener {
            setupCategorySelection(binding, "food")
        }

        binding.categorySnack.setOnClickListener {
            setupCategorySelection(binding, "snack")
        }

        binding.categoryDrink.setOnClickListener {
            setupCategorySelection(binding, "drink")
        }
    }

    private fun getSelectedFoodType(binding: ViewModalAddEditMenuBinding): String {
        return when {
            binding.categoryFood.background.constantState ==
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_selected_category)?.constantState -> "food"
            binding.categorySnack.background.constantState ==
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_selected_category)?.constantState -> "snack"
            binding.categoryDrink.background.constantState ==
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_selected_category)?.constantState -> "drink"
            else -> "food"
        }
    }

    private fun updateMenu(menuId: Int, newMenuName: String, newMenuPrice: Int, newFoodType: String) {
        val namePart = newMenuName.toRequestBody("text/plain".toMediaTypeOrNull())
        val pricePart = newMenuPrice.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val foodTypePart = newFoodType.toRequestBody("text/plain".toMediaTypeOrNull())

        var imagePart: MultipartBody.Part? = null
        selectedImageUri?.let { uri ->
            val file = File(requireContext().cacheDir, "menu_image_${System.currentTimeMillis()}.jpg")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            imagePart = MultipartBody.Part.createFormData("menu_picture", file.name, requestFile)
        }

        Log.d("MenuFragment", "Updating menu: id=$menuId, name=$newMenuName, price=$newMenuPrice, foodType=$newFoodType")

        apiService.updateMenu(menuId, namePart, pricePart, foodTypePart, imagePart)
            .enqueue(object : Callback<SingleMenuResponse> {
                override fun onResponse(call: Call<SingleMenuResponse>, response: Response<SingleMenuResponse>) {
                    // PERBAIKAN 1: Tambahkan pengecekan keamanan
                    if (!isAdded || _binding == null) return

                    Log.d("MenuFragment", "API Response: ${response.body()}")
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { updatedMenu ->
                            Log.d("MenuFragment", "Updated menu received: $updatedMenu")
                            val mappedMenu = updatedMenu.copy(
                                supplierName = supplierList.find { it.first == updatedMenu.idSupplier }?.second ?: "Pemasok Tidak Diketahui"
                            )
                            val index = originalMenuList.indexOfFirst { it.id == menuId }
                            if (index != -1) {
                                originalMenuList[index] = mappedMenu
                                applyCurrentFilter()
                                menuAdapter.updateList(filteredMenuList)
                                updateVisibility()
                                Toast.makeText(requireContext(), "Berhasil memperbarui menu", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("MenuFragment", "Menu with ID $menuId not found in originalMenuList")
                                Toast.makeText(requireContext(), "Menu tidak ditemukan di daftar", Toast.LENGTH_SHORT).show()
                            }
                        } ?: run {
                            Log.e("MenuFragment", "No data in updateMenu response: ${response.body()}")
                            Toast.makeText(requireContext(), "Menu tidak ditemukan di respons", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        handleErrorResponse(response, "Gagal memperbarui menu")
                    }
                    selectedImageUri = null
                }

                override fun onFailure(call: Call<SingleMenuResponse>, t: Throwable) {
                    // PERBAIKAN 1: Tambahkan pengecekan keamanan
                    if (!isAdded || _binding == null) return

                    handleNetworkError(t, "Error updating menu")
                    selectedImageUri = null
                }
            })
    }

    private fun showConfirmDeleteDialog(menuItem: MenuItem) {
        // 1. Inflate layout custom dialog dan buat binding
        val dialogBinding = ViewDialogConfirmDeleteMenuBinding.inflate(layoutInflater)

        // 2. Buat AlertDialog menggunakan layout custom
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // 3. Atur listener untuk tombol-tombol di dalam dialog
        dialogBinding.btnCancelDelete.setOnClickListener {
            // Jika "Batal" diklik, tutup dialog
            dialog.dismiss()
        }

        dialogBinding.btnConfirmDelete.setOnClickListener {
            // Jika "Hapus" diklik, panggil fungsi deleteMenu yang asli
            deleteMenu(menuItem)
            // Tutup dialog setelahnya
            dialog.dismiss()
        }

        // 4. INI BAGIAN PENTING: Membuat background dialog menjadi transparan
        // Ini akan menghilangkan kotak putih di belakang layout rounded Anda.
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 5. Tampilkan dialog
        dialog.show()
    }

    private fun deleteMenu(menuItem: MenuItem) {
        apiService.deleteMenu(menuItem.id).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                // PERBAIKAN 1: Tambahkan pengecekan keamanan
                if (!isAdded) return

                if (response.isSuccessful && response.body()?.success == true) {
                    originalMenuList.remove(menuItem)
                    filteredMenuList = originalMenuList.filter { it.id != menuItem.id }
                    menuAdapter.updateList(filteredMenuList)

                    if (userRole?.lowercase() == "cashier") {
                        selectedMenuItems.remove(menuItem)
                        if (::orderAdapter.isInitialized) {
                            orderAdapter.notifyDataSetChanged()
                        }
                    }

                    updateVisibility()
                    Toast.makeText(requireContext(), "Berhasil menghapus menu", Toast.LENGTH_SHORT).show()
                } else {
                    handleErrorResponse(response, "Gagal menghapus menu")
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                // PERBAIKAN 1: Tambahkan pengecekan keamanan
                if (!isAdded) return
                handleNetworkError(t, "Error deleting menu")
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupOrderRecyclerView() {
        orderAdapter = OrderAdapter(selectedMenuItems) {
            updateTotalPayment()
            showNoOrderMessage(selectedMenuItems.isEmpty())
        }
        binding.rvOrderMenu.apply {
            adapter = orderAdapter
            layoutManager = LinearLayoutManager(context)
        }
        binding.rbCash.setOnClickListener {
            binding.tvInputPayment.visibility = View.VISIBLE
            binding.etInputPayment.visibility = View.VISIBLE
        }
        binding.rbQris.setOnClickListener {
            binding.tvInputPayment.visibility = View.GONE
            binding.etInputPayment.visibility = View.GONE
        }
        binding.btnPayNow.setOnClickListener {
            if (selectedMenuItems.isEmpty()) {
                Toast.makeText(requireContext(), "Tidak ada menu yang dipilih", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val invalidItems = selectedMenuItems.filter { item ->
                val menu = originalMenuList.find { it.id == item.id }
                menu == null || !menu.isValidForTransaction() || menu.stock < item.quantity
            }
            if (invalidItems.isNotEmpty()) {
                val message = invalidItems.joinToString("\n") {
                    if (originalMenuList.find { m -> m.id == it.id } == null) {
                        "${it.menuName} tidak valid"
                    } else {
                        "${it.menuName} stok tidak cukup (sisa: ${originalMenuList.find { m -> m.id == it.id }?.stock})"
                    }
                }
                Toast.makeText(
                    requireContext(),
                    "Gagal: $message\nMenyegarkan daftar menu.",
                    Toast.LENGTH_LONG
                ).show()
                fetchMenuFromServer()
                return@setOnClickListener
            }

            Log.d("MenuFragment", "Selected menu IDs: ${selectedMenuItems.map { it.id }}, Available in originalMenuList: ${originalMenuList.map { it.id }}")
            val paymentType = if (binding.rbCash.isChecked) "cash" else "qris"
            var paymentAmount = 0

            if (binding.rbCash.isChecked) {
                val paymentInput = binding.etInputPayment.text.toString().replace("Rp", "").replace(".", "").trim()
                if (paymentInput.isEmpty()) {
                    Toast.makeText(requireContext(), "Mohon masukkan jumlah pembayaran", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                paymentAmount = paymentInput.toIntOrNull() ?: 0
                val totalPayment = selectedMenuItems.sumOf { it.price * it.quantity }
                if (paymentAmount < totalPayment) {
                    Toast.makeText(requireContext(), "Jumlah pembayaran kurang", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                paymentAmount = selectedMenuItems.sumOf { it.price * it.quantity }
            }

            val idMenu = selectedMenuItems.associate { it.id.toString() to it.quantity }
            val request = CreateReceiptRequest(
                id_menu = idMenu,
                payment_type = paymentType,
                payment = paymentAmount
            )

            currentRequestIdMenu = idMenu
            sendCreateReceiptRequest(request)
        }
        updateTotalPayment()
        showNoOrderMessage(selectedMenuItems.isEmpty())
    }

    private fun sendCreateReceiptRequest(request: CreateReceiptRequest) {
        val sharedPreferences = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Sesi habis, silakan login kembali", Toast.LENGTH_LONG).show()
            return
        }

        Log.d("MenuFragment", "Sending create-receipt request body: $request")
        Log.d("MenuFragment", "Sending create-receipt request: $request, Token: $token")

        showProgressBar()
        apiService.createReceipt(request).enqueue(object : Callback<CreateReceiptResponse> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<CreateReceiptResponse>, response: Response<CreateReceiptResponse>) {
                // PERBAIKAN 1: Tambahkan pengecekan keamanan
                if (!isAdded || _binding == null) return

                hideProgressBar()
                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(
                        requireContext(),
                        "Transaksi berhasil: ${response.body()?.message ?: "Receipt created"}",
                        Toast.LENGTH_SHORT
                    ).show()
                    showInvoiceDialog(response.body())
                    request.id_menu.forEach { (menuIdStr, quantity) ->
                        val menuId = menuIdStr.toIntOrNull()
                        if (menuId != null) {
                            val foundMenuItem = originalMenuList.find { it.id == menuId }
                            if (foundMenuItem != null) {
                                foundMenuItem.stock -= quantity
                            } else {
                                Log.w("MenuFragment", "Menu item with ID $menuId not found in originalMenuList")
                            }
                        }
                    }
                    menuAdapter.updateList(filteredMenuList)
                    updateVisibility()
                    resetOrderData()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("MenuFragment", "Error response: $errorBody, Code: ${response.code()}")
                    when (response.code()) {
                        401 -> Toast.makeText(requireContext(), "Sesi habis, silakan login kembali", Toast.LENGTH_LONG).show()
                        409 -> {
                            try {
                                val errorJson = org.json.JSONObject(errorBody)
                                val message = errorJson.optString("message", "Transaksi gagal: Konflik data")
                                when {
                                    message.contains("No query results for model") -> {
                                        Toast.makeText(
                                            requireContext(),
                                            "Transaksi gagal: Data menu tidak ditemukan untuk ID ${request.id_menu.keys}. Menyegarkan daftar menu.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    message.contains("The id_menu.0 field must be an integer") -> {
                                        Toast.makeText(
                                            requireContext(),
                                            "Transaksi gagal: Format ID menu tidak sesuai. Menyegarkan daftar menu.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    message.contains("The selected id_menu") || message.contains("is invalid") -> {
                                        Toast.makeText(
                                            requireContext(),
                                            "Transaksi gagal: Menu tidak valid untuk ID ${request.id_menu.keys}. Menyegarkan daftar menu.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    else -> Toast.makeText(
                                        requireContext(),
                                        "Transaksi gagal: $message",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    requireContext(),
                                    "Transaksi gagal: $errorBody",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            fetchMenuFromServer()
                        }
                        else -> handleErrorResponse(response, "Gagal membuat transaksi")
                    }
                }
                currentRequestIdMenu = null
            }

            override fun onFailure(call: Call<CreateReceiptResponse>, t: Throwable) {
                // PERBAIKAN 1: Tambahkan pengecekan keamanan
                if (!isAdded || _binding == null) return

                hideProgressBar()
                Log.e("MenuFragment", "Network error: ${t.message}", t)
                handleNetworkError(t, "Error creating receipt")
                currentRequestIdMenu = null
            }
        })
    }

    private fun setupInputPayment() {
        binding.etInputPayment.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isFormatting) return
                val input = s.toString().replace("Rp", "").replace(".", "").trim()
                if (input.isNotEmpty()) {
                    isFormatting = true
                    val amount = input.toIntOrNull() ?: 0
                    binding.etInputPayment.setText(formatPrice(amount))
                    binding.etInputPayment.setSelection(binding.etInputPayment.text.length)
                    isFormatting = false
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showNoOrderMessage(empty: Boolean) {
        _binding?.let { binding ->
            binding.tvEmptyOrder.visibility = if (empty) View.VISIBLE else View.GONE
        }
    }

    private fun setupSpinner() {
        fetchSuppliers { suppliers ->
            _binding?.let { binding ->
                // Logika untuk mempersiapkan data spinner
                supplierList = listOf(Pair(0, "Semua Pemasok")) + suppliers
                val arrayNames = supplierList.map { it.second }.toTypedArray()
                val nameAdapter = ArrayAdapter(requireContext(), R.layout.custom_item_spinner, arrayNames)
                nameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                binding.spFilterName.apply {
                    adapter = nameAdapter
                    setSelection(0)
                }
                setupSpinnerListener()
            }
        }
    }

    private fun setupSpinnerListener() {
        binding.spFilterName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isInitialFetchComplete) return
                val selectedSupplierId = supplierList[position].first
                val selectedFoodType = when (binding.chipGroup.checkedChipId) {
                    R.id.chip_foods -> "food"
                    R.id.chip_drinks -> "drink"
                    R.id.chip_snacks -> "snack"
                    else -> null
                }
                filterMenuBySupplierAndCategory(selectedSupplierId, selectedFoodType)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupButtons() {
        binding.apply {
            arrowToggle.setOnClickListener { toggleSidebar() }
            btnAddNewMenu.setOnClickListener { showMenuDialog() }
        }
    }

    private fun addItemToOrder(menuItem: MenuItem) {
        if (userRole?.lowercase() == "admin") {
            Log.d("MenuFragment", "Admin attempted to add item to order: ${menuItem.menuName}, action blocked")
            Toast.makeText(requireContext(), "Admin tidak dapat menambahkan menu ke pesanan", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("MenuFragment_Debug", "Item diklik: ID=${menuItem.id}, Nama=${menuItem.menuName}, URL Gambar=${menuItem.imageUrl}")

        Log.d("MenuFragment", "Adding menu item: ID=${menuItem.id}, Name=${menuItem.menuName}, Stock=${menuItem.stock}")
        val menu = originalMenuList.find { it.id == menuItem.id }
        if (menu == null) {
            Toast.makeText(
                requireContext(),
                "Menu ${menuItem.menuName} tidak valid, menyegarkan daftar menu",
                Toast.LENGTH_LONG
            ).show()
            fetchMenuFromServer()
            return
        }
        if (!menu.isValidForTransaction()) {
            Toast.makeText(
                requireContext(),
                "Menu ${menuItem.menuName} kehabisan stok",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Simpan status visibilitas sidebar sebelum ada perubahan
        val sidebarWasVisible = binding.nestedScrollView.visibility == View.VISIBLE

        val existingItem = selectedMenuItems.find { it.id == menuItem.id }
        if (existingItem != null) {
            if (menu.stock < existingItem.quantity + 1) {
                Toast.makeText(
                    requireContext(),
                    "Stok ${menuItem.menuName} tidak cukup (sisa: ${menu.stock})",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            existingItem.quantity += 1
            orderAdapter.notifyItemChanged(selectedMenuItems.indexOf(existingItem))
            Toast.makeText(requireContext(), "${menuItem.menuName} sudah ditambahkan, kuantitas bertambah", Toast.LENGTH_SHORT).show()
        } else {
            if (menu.stock < 1) {
                Toast.makeText(
                    requireContext(),
                    "Stok ${menuItem.menuName} tidak cukup (sisa: ${menu.stock})",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            selectedMenuItems.add(menuItem.copy(quantity = 1))
            orderAdapter.notifyItemInserted(selectedMenuItems.size - 1)
            Toast.makeText(requireContext(), "${menuItem.menuName} ditambahkan ke pesanan", Toast.LENGTH_SHORT).show()
        }

        orderAdapter.notifyDataSetChanged()
        updateTotalPayment()
        showNoOrderMessage(selectedMenuItems.isEmpty())

        // Hanya tampilkan sidebar jika sebelumnya tidak terlihat.
        // Listener akan menangani kalkulasi ulang secara otomatis.
        if (!sidebarWasVisible) {
            binding.nestedScrollView.visibility = View.VISIBLE
        }
    }

    fun formatPrice(price: Int): String {
        val numberFormat = NumberFormat.getInstance(Locale("id", "ID"))
        return numberFormat.format(price)
    }

    private fun updateTotalPayment() {
        _binding?.let { binding ->
            val totalPayment = selectedMenuItems.sumOf { it.price * it.quantity }
            binding.tvTotalPaymentAmount.text = "Rp${formatPrice(totalPayment)}"
        }
    }

    private fun toggleSidebar() {
        if (userRole?.lowercase() != "cashier") return

        binding.nestedScrollView.visibility = if (binding.nestedScrollView.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showInvoiceDialog(receiptResponse: CreateReceiptResponse? = null) {
        _binding?.let { binding ->
            val dialogBinding = ViewModalInvoiceOrderBinding.inflate(layoutInflater)
            dialogBinding.apply {
                if (receiptResponse != null && receiptResponse.success && receiptResponse.data != null) {
                    val receiptData = receiptResponse.data
                    tvTotalPaymentAmount.text = "Rp${formatPrice(receiptData.total)}"
                    tvPaymentMethodLabel.text = "Bayar (${receiptData.payment_type.replaceFirstChar { it.uppercase() }})"
                    tvMoneyPay.text = "Rp${formatPrice(receiptData.payment)}"

                    try {
                        val createdAt = LocalDateTime.parse(receiptData.created_at, DateTimeFormatter.ISO_DATE_TIME)
                        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                        tvDateOrder.text = createdAt.format(dateFormatter)
                        tvTimeOrder.text = createdAt.format(timeFormatter)
                    } catch (e: Exception) {
                        Log.e("MenuFragment", "Error parsing date: ${receiptData.created_at}, ${e.message}")
                        tvDateOrder.text = "N/A"
                        tvTimeOrder.text = "N/A"
                    }
                    val idMenu = currentRequestIdMenu ?: emptyMap()
                    val menuDetails = idMenu.mapNotNull { (menuIdStr, quantity) ->
                        val menuId = menuIdStr.toIntOrNull()
                        menuId?.let { id ->
                            originalMenuList.find { it.id == id }?.let { menu ->
                                "${menu.menuName} (x$quantity)" to "Rp${formatPrice(menu.price * quantity)}"
                            }
                        }
                    }
                    tvMenuName.text = menuDetails.joinToString("\n") { it.first }
                    tvMenuPrice.text = menuDetails.joinToString("\n") { it.second }

                    val sharedPreferences = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    val cashierName = sharedPreferences.getString("user_name", "Kasir") ?: "Kasir"
                    tvNameEmployee.text = cashierName

                    tvMoneyChange.text = "Rp${formatPrice(receiptData.returns)}"
                }
                else {
                    val totalPayment = selectedMenuItems.sumOf { it.price * it.quantity }
                    tvTotalPaymentAmount.text = "Rp${formatPrice(totalPayment)}"
                    val menuNames = selectedMenuItems.joinToString("\n") { "${it.menuName} (x${it.quantity})" }
                    val menuPrices = selectedMenuItems.joinToString("\n") { "Rp${formatPrice(it.price * it.quantity)}" }
                    tvMenuName.text = menuNames
                    tvMenuPrice.text = menuPrices
                    val currentDateTime = LocalDateTime.now()
                    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    tvDateOrder.text = currentDateTime.format(dateFormatter)
                    tvTimeOrder.text = currentDateTime.format(timeFormatter)
                    val sharedPreferences = requireContext().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    val cashierName = sharedPreferences.getString("user_name", "Kasir") ?: "Kasir"
                    tvNameEmployee.text = cashierName

                    val paymentType = if (binding.rbCash.isChecked) "Cash" else "QRIS"
                    tvPaymentMethodLabel.text = "Bayar ($paymentType)"
                    tvMoneyPay.text = "Rp${formatPrice(totalPayment)}"

                    if (binding.rbCash.isChecked) {
                        val paymentInput = binding.etInputPayment.text.toString().replace("Rp", "").replace(".", "").trim()
                        val moneyPay = paymentInput.toIntOrNull() ?: totalPayment
                        val change = moneyPay - totalPayment
                        tvMoneyChange.text = "Rp${formatPrice(change)}"
                    } else {
                        tvMoneyChange.text = "Rp0"
                    }
                }
            }

            val dialog = AlertDialog.Builder(requireContext()).setView(dialogBinding.root).create()
            dialog.setCancelable(true)
            dialog.setOnCancelListener { resetOrderData() }
            dialogBinding.btnClose.setOnClickListener {
                dialog.dismiss()
                resetOrderData()
            }
            dialog.show()
            dialog.window?.setLayout(android.view.WindowManager.LayoutParams.WRAP_CONTENT, android.view.WindowManager.LayoutParams.WRAP_CONTENT)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialogBinding.root.setPadding(32)
        }
    }

    private fun resetOrderData() {
        selectedMenuItems.clear()
        _binding?.let { binding ->
            orderAdapter.notifyDataSetChanged()
            binding.tvTotalPaymentAmount.text = "Rp0"

            // Kosongkan field input pembayaran.
            binding.etInputPayment.text.clear()

            showNoOrderMessage(selectedMenuItems.isEmpty())

            // Sembunyikan sidebar setelah transaksi selesai.
            binding.nestedScrollView.visibility = View.GONE
        }
    }

    private fun checkStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun fetchSuppliersForDialog(onSuppliersFetched: (List<Pair<Int, String>>) -> Unit) {
        apiService.getSuppliers().enqueue(object : Callback<SupplierResponse> {
            override fun onResponse(call: Call<SupplierResponse>, response: Response<SupplierResponse>) {
                if (!isAdded) return

                if (response.isSuccessful && response.body()?.success == true) {
                    val suppliers = response.body()?.data?.map { Pair(it.id, it.name) } ?: emptyList()
                    Log.d("MenuFragment", "fetchSuppliersForDialog: Fetched ${suppliers.size} suppliers")
                    onSuppliersFetched(suppliers)
                } else {
                    handleErrorResponse(response, "Gagal mengambil daftar pemasok")
                    onSuppliersFetched(listOf(Pair(0, "Semua Pemasok")))
                }
            }

            override fun onFailure(call: Call<SupplierResponse>, t: Throwable) {
                if (!isAdded) return

                handleNetworkError(t, "Error fetching suppliers")
                onSuppliersFetched(listOf(Pair(0, "Semua Pemasok")))
            }
        })
    }

    private fun showMenuDialog() {
        Log.d("MenuFragment", "showMenuDialog: Adding new menu")
        dialogBinding = ViewModalAddEditMenuBinding.inflate(layoutInflater)

        dialogBinding.imgMenu.setImageResource(R.drawable.ic_launcher_background) // Set placeholder awal

        fetchSuppliersForDialog { suppliers ->
            Log.d("MenuFragment", "Suppliers for dialog: ${suppliers.map { "${it.first} -> ${it.second}" }}")
            val supplierNames = suppliers.map { it.second }.toTypedArray()
            val supplierAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, supplierNames)
            supplierAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dialogBinding.spinnerSupplierName.adapter = supplierAdapter

            setupCategorySelection(dialogBinding, "food")

            dialogBinding.btnCameraMenu.setOnClickListener { checkStoragePermission() }

            val dialog = AlertDialog.Builder(requireContext()).setView(dialogBinding.root).create()

            dialogBinding.apply {
                btnCancel.setOnClickListener {
                    dialog.dismiss()
                    selectedImageUri = null
                    Log.d("MenuFragment", "showMenuDialog: Dialog cancelled")
                }
                btnSave.setOnClickListener {
                    val supplierIndex = spinnerSupplierName.selectedItemPosition
                    if (suppliers.isEmpty()) {
                        Toast.makeText(requireContext(), "Tidak ada pemasok tersedia", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val supplierId = suppliers[supplierIndex].first
                    val supplierName = suppliers[supplierIndex].second
                    val menuName = etMenuName.text.toString().trim()
                    val menuPrice = etMenuPrice.text.toString().toIntOrNull() ?: 0
                    val foodType = getSelectedFoodType(dialogBinding)

                    if (menuName.isNotEmpty() && menuPrice > 0 && foodType.isNotEmpty()) {
                        val namePart = menuName.toRequestBody("text/plain".toMediaTypeOrNull())
                        val pricePart = menuPrice.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                        val idSupplierPart = supplierId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                        val supplierNamePart = supplierName.toRequestBody("text/plain".toMediaTypeOrNull())
                        val foodTypePart = foodType.toRequestBody("text/plain".toMediaTypeOrNull())

                        var imagePart: MultipartBody.Part? = null
                        selectedImageUri?.let { uri ->
                            try {
                                val file = File(requireContext().cacheDir, "menu_image_${System.currentTimeMillis()}.jpg")
                                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                                    file.outputStream().use { output -> input.copyTo(output) }
                                }
                                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                imagePart = MultipartBody.Part.createFormData("menu_picture", file.name, requestFile)
                            } catch (e: Exception) {
                                Log.e("MenuFragment", "Error preparing image: ${e.message}", e)
                                Toast.makeText(requireContext(), "Gagal memproses gambar", Toast.LENGTH_SHORT).show()
                                return@setOnClickListener
                            }
                        }

                        apiService.createMenu(
                            name = namePart,
                            price = pricePart,
                            idSupplier = idSupplierPart,
                            supplierName = supplierNamePart,
                            foodType = foodTypePart,
                            menuPicture = imagePart
                        ).enqueue(object : Callback<SingleMenuResponse> {
                            override fun onResponse(call: Call<SingleMenuResponse>, response: Response<SingleMenuResponse>) {
                                if (!isAdded || _binding == null) return

                                if (response.isSuccessful && response.body()?.success == true) {
                                    response.body()?.data?.let { newMenu ->
                                        Log.d("MenuFragment", "New menu received: id=${newMenu.id}, stock=${newMenu.stock}")

                                        val mappedMenu = mapMenuItemsWithSuppliers(listOf(newMenu)).first()

                                        // Tambahkan item ke original list
                                        originalMenuList.add(mappedMenu)

                                        // Terapkan filter yang sedang aktif untuk memperbarui tampilan.
                                        applyCurrentFilter()
                                        menuAdapter.updateList(filteredMenuList)
                                        updateVisibility()

                                        // Temukan posisi item baru di list yang sudah difilter dan scroll ke sana.
                                        val newIndexInFilteredList = filteredMenuList.indexOfFirst { it.id == newMenu.id }
                                        if (newIndexInFilteredList != -1) {
                                            // Gunakan post untuk memastikan RecyclerView sudah selesai mengupdate layout.
                                            binding.rvAllMenu.post {
                                                if (isAdded) {
                                                    binding.rvAllMenu.smoothScrollToPosition(newIndexInFilteredList)
                                                    Log.d("MenuFragment", "Scrolled to new item at filtered index: $newIndexInFilteredList")
                                                }
                                            }
                                        }

                                        Toast.makeText(requireContext(), "Berhasil menambahkan menu baru", Toast.LENGTH_SHORT).show()
                                    } ?: run {
                                        Log.e("MenuFragment", "No data in createMenu response: ${response.body()}")
                                        Toast.makeText(requireContext(), "Menu tidak ditemukan di respons", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    handleErrorResponse(response, "Gagal menambahkan menu")
                                }
                                selectedImageUri = null
                            }

                            override fun onFailure(call: Call<SingleMenuResponse>, t: Throwable) {
                                if (!isAdded || _binding == null) return
                                handleNetworkError(t, "Error creating menu")
                                selectedImageUri = null
                            }
                        })
                        dialog.dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Mohon isi nama, harga, dan tipe makanan dengan benar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialog.show()
            dialogBinding.root.requestLayout()
            dialogBinding.root.invalidate()
            Log.d("MenuFragment", "showMenuDialog: Dialog shown")
        }
    }

    override fun onDestroyView() {
        _binding?.rvAllMenu?.viewTreeObserver?.removeOnGlobalLayoutListener(layoutListener)
        super.onDestroyView()
        _binding = null
    }
}