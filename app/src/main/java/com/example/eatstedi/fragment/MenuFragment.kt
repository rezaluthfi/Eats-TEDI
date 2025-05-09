package com.example.eatstedi.fragment

import android.Manifest
import android.app.Activity
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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.eatstedi.R
import com.example.eatstedi.adapter.MenuAdapter
import com.example.eatstedi.adapter.OrderAdapter
import com.example.eatstedi.api.retrofit.RetrofitClient
import com.example.eatstedi.api.service.ApiService
import com.example.eatstedi.api.service.GenericResponse
import com.example.eatstedi.api.service.MenuResponse
import com.example.eatstedi.api.service.SingleMenuResponse
import com.example.eatstedi.api.service.SearchRequest
import com.example.eatstedi.databinding.FragmentMenuBinding
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
        uri?.let {
            selectedImageUri = it
            Glide.with(this).load(it).into(dialogBinding.imgMenu)
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

        // Initialize with tvNoData and rvAllMenu GONE until data is fetched
        binding.tvNoData.visibility = View.GONE
        binding.rvAllMenu.visibility = View.GONE
        Log.d("MenuFragment", "onViewCreated: tv_no_data set to GONE, rvAllMenu set to GONE")

        apiService = RetrofitClient.getInstance(requireContext())
        setupMenuRecyclerView()
        setupOrderRecyclerView()
        setupInputPayment()
        setupChipFilter()
        setupButtons()
        setupSpinner()
        setupSearchView()
        fetchMenuFromServer()
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoData.visibility = View.GONE
        binding.rvAllMenu.visibility = View.GONE
        Log.d("MenuFragment", "showProgressBar: Showing progress bar, tv_no_data GONE, rvAllMenu GONE")
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
        Log.d("MenuFragment", "hideProgressBar: Hiding progress bar")
    }

    private fun fetchMenuFromServer() {
        showProgressBar()
        apiService.getMenu().enqueue(object : Callback<MenuResponse> {
            override fun onResponse(call: Call<MenuResponse>, response: Response<MenuResponse>) {
                hideProgressBar()
                if (response.isSuccessful && response.body()?.success == true) {
                    originalMenuList.clear()
                    originalMenuList.addAll(response.body()?.data ?: emptyList())
                    filteredMenuList = originalMenuList
                    menuAdapter.updateList(filteredMenuList)
                    isInitialFetchComplete = true
                    updateVisibility()
                    Log.d("MenuFragment", "Menu fetched: ${originalMenuList.size} items, tvNoData=${if (filteredMenuList.isEmpty()) "VISIBLE" else "GONE"}")
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
        binding.rvAllMenu.visibility = if (filteredMenuList.isEmpty()) View.GONE else View.VISIBLE
        binding.tvNoData.visibility = if (filteredMenuList.isEmpty()) View.VISIBLE else View.GONE
        Log.d("MenuFragment", "updateVisibility: rvAllMenu=${if (filteredMenuList.isEmpty()) "GONE" else "VISIBLE"}, tvNoData=${if (filteredMenuList.isEmpty()) "VISIBLE" else "GONE"}")
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
            // Disable listeners to prevent filter reapplication
            binding.chipGroup.setOnCheckedChangeListener(null)
            binding.spFilterName.onItemSelectedListener = null

            filteredMenuList = originalMenuList
            menuAdapter.updateList(filteredMenuList)
            // Reset UI state
            binding.chipAllMenu.isChecked = true
            binding.spFilterName.setSelection(0)
            updateVisibility()
            Log.d("MenuFragment", "filterMenus: Reset to original list, originalSize=${originalMenuList.size}, filteredSize=${filteredMenuList.size}")

            // Re-enable listeners
            setupChipFilterListener()
            setupSpinnerListener()
            return
        }
        showProgressBar()
        apiService.searchMenu(SearchRequest(query)).enqueue(object : Callback<MenuResponse> {
            override fun onResponse(call: Call<MenuResponse>, response: Response<MenuResponse>) {
                hideProgressBar()
                if (response.isSuccessful && response.body()?.success == true) {
                    filteredMenuList = response.body()?.data ?: emptyList()
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
                hideProgressBar()
                if (response.isSuccessful && response.body()?.success == true) {
                    filteredMenuList = response.body()?.data ?: emptyList()
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
                hideProgressBar()
                if (response.isSuccessful && response.body()?.success == true) {
                    var result = response.body()?.data ?: emptyList()
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
                hideProgressBar()
                filteredMenuList = emptyList()
                menuAdapter.updateList(filteredMenuList)
                updateVisibility()
                handleNetworkError(t, "Error filtering by supplier")
            }
        })
    }

    private fun showEditMenuDialog(menuItem: MenuItem) {
        dialogBinding = ViewModalAddEditMenuBinding.inflate(layoutInflater)
        val categories = resources.getStringArray(R.array.menu_category)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerMenuCategory.adapter = spinnerAdapter
        val suppliers = listOf(Pair(1, "Warung Bu Tuti"))
        val supplierNames = suppliers.map { it.second }.toTypedArray()
        val supplierAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, supplierNames)
        supplierAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerSupplierName.adapter = supplierAdapter
        val supplierIndex = suppliers.indexOfFirst { it.first == menuItem.idSupplier }
        if (supplierIndex != -1) {
            dialogBinding.spinnerSupplierName.setSelection(supplierIndex)
        } else {
            dialogBinding.spinnerSupplierName.setSelection(0)
        }
        dialogBinding.etMenuName.setText(menuItem.menuName)
        dialogBinding.etMenuPrice.setText(menuItem.price.toString())
        dialogBinding.etMenuStock.setText(menuItem.stock.toString())
        dialogBinding.spinnerMenuCategory.setSelection(categories.indexOf(menuItem.category))
        Glide.with(this).load(menuItem.imageUrl).into(dialogBinding.imgMenu)
        dialogBinding.btnCameraMenu.setOnClickListener { checkStoragePermission() }
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogBinding.root).create()
        dialogBinding.apply {
            btnCancel.setOnClickListener { dialog.dismiss() }
            btnSave.setOnClickListener {
                val supplierIndex = spinnerSupplierName.selectedItemPosition
                val supplierId = suppliers[supplierIndex].first
                val supplierName = suppliers[supplierIndex].second
                val menuName = etMenuName.text.toString()
                val menuPrice = etMenuPrice.text.toString().toIntOrNull() ?: 0
                val menuStock = etMenuStock.text.toString().toIntOrNull() ?: 0
                val category = spinnerMenuCategory.selectedItem.toString()
                if (menuName.isNotEmpty() && menuPrice > 0 && menuStock >= 0) {
                    updateMenu(menuItem.id, menuName, menuPrice, menuStock, supplierId, supplierName, category)
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Mohon isi nama, harga, dan stok valid", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun updateMenu(menuId: Int, newMenuName: String, newMenuPrice: Int, newMenuStock: Int, newSupplierId: Int, newSupplierName: String, newCategory: String) {
        val foodType = when (newCategory) {
            "Makanan" -> "food"
            "Camilan" -> "snack"
            "Minuman" -> "drink"
            else -> "other"
        }
        val namePart = newMenuName.toRequestBody("text/plain".toMediaTypeOrNull())
        val pricePart = newMenuPrice.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val stockPart = newMenuStock.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val idSupplierPart = newSupplierId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val supplierNamePart = newSupplierName.toRequestBody("text/plain".toMediaTypeOrNull())
        val foodTypePart = foodType.toRequestBody("text/plain".toMediaTypeOrNull())
        var imagePart: MultipartBody.Part? = null
        selectedImageUri?.let { uri ->
            val file = File(requireContext().cacheDir, "menu_image_${System.currentTimeMillis()}.jpg")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            imagePart = MultipartBody.Part.createFormData("menu_picture", file.name, requestFile)
        }
        apiService.updateMenu(menuId, namePart, pricePart, stockPart, idSupplierPart, supplierNamePart, foodTypePart, imagePart)
            .enqueue(object : Callback<SingleMenuResponse> {
                override fun onResponse(call: Call<SingleMenuResponse>, response: Response<SingleMenuResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { updatedMenu ->
                            Log.d("MenuFragment", "Updated menu received: $updatedMenu")
                            val index = originalMenuList.indexOfFirst { menu -> menu.id == menuId }
                            if (index != -1) {
                                originalMenuList[index] = updatedMenu
                                filteredMenuList = originalMenuList
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
                    handleNetworkError(t, "Error updating menu")
                    selectedImageUri = null
                }
            })
    }

    private fun deleteMenu(menuItem: MenuItem) {
        apiService.deleteMenu(menuItem.id).enqueue(object : Callback<GenericResponse> {
            override fun onResponse(call: Call<GenericResponse>, response: Response<GenericResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    originalMenuList.remove(menuItem)
                    filteredMenuList = originalMenuList
                    menuAdapter.updateList(filteredMenuList)
                    selectedMenuItems.remove(menuItem)
                    orderAdapter.notifyDataSetChanged()
                    updateVisibility()
                    Toast.makeText(requireContext(), "Berhasil menghapus menu", Toast.LENGTH_SHORT).show()
                } else {
                    handleErrorResponse(response, "Gagal menghapus menu")
                }
            }

            override fun onFailure(call: Call<GenericResponse>, t: Throwable) {
                handleNetworkError(t, "Error deleting menu")
            }
        })
    }

    private fun setupMenuRecyclerView() {
        menuAdapter = MenuAdapter(
            filteredMenuList,
            onItemClick = { menuItem -> addItemToOrder(menuItem) },
            onEditMenu = { menuItem -> showEditMenuDialog(menuItem) },
            onDeleteMenu = { menuItem -> deleteMenu(menuItem) }
        )
        binding.rvAllMenu.apply {
            adapter = menuAdapter
            layoutManager = if (binding.nestedScrollView.visibility == View.VISIBLE) {
                GridLayoutManager(context, 2)
            } else {
                GridLayoutManager(context, 3)
            }
            this.layoutParams.width = if (binding.nestedScrollView.visibility == View.VISIBLE) {
                ViewGroup.LayoutParams.MATCH_PARENT
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
        Log.d("MenuFragment", "setupMenuRecyclerView: MenuAdapter initialized")
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
            if (binding.rbCash.isChecked) {
                val paymentInput = binding.etInputPayment.text.toString().replace("Rp", "").replace(".", "").trim()
                if (paymentInput.isEmpty()) {
                    Toast.makeText(requireContext(), "Mohon masukkan jumlah pembayaran", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val totalPayment = selectedMenuItems.sumOf { it.price * it.quantity }
                if (paymentInput.toInt() < totalPayment) {
                    Toast.makeText(requireContext(), "Jumlah pembayaran hasil", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                Toast.makeText(requireContext(), "Pembayaran cash berhasil!", Toast.LENGTH_SHORT).show()
            } else if (binding.rbQris.isChecked) {
                Toast.makeText(requireContext(), "Pembayaran QRIS berhasil!", Toast.LENGTH_SHORT).show()
            }
            showInvoiceDialog()
            showNoOrderMessage(selectedMenuItems.isEmpty())
            binding.etInputPayment.setText("")
        }
        updateTotalPayment()
        showNoOrderMessage(selectedMenuItems.isEmpty())
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
        binding.tvEmptyOrder.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun setupSpinner() {
        val supplierList = listOf(Pair(0, "Semua Pemasok"), Pair(1, "Warung Bu Tuti"))
        val arrayNames = supplierList.map { it.second }.toTypedArray()
        val nameAdapter = ArrayAdapter(requireContext(), R.layout.custom_item_spinner, arrayNames)
        nameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spFilterName.apply {
            adapter = nameAdapter
            setSelection(0)
        }
        setupSpinnerListener()
    }

    private fun setupSpinnerListener() {
        binding.spFilterName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isInitialFetchComplete) return
                val supplierList = listOf(Pair(0, "Semua Pemasok"), Pair(1, "Warung Bu Tuti"))
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
        val existingItem = selectedMenuItems.find { it.id == menuItem.id }
        if (existingItem != null) {
            existingItem.quantity += 1
            orderAdapter.notifyItemChanged(selectedMenuItems.indexOf(existingItem))
            Toast.makeText(requireContext(), "${menuItem.menuName} sudah ditambahkan, kuantitas bertambah", Toast.LENGTH_SHORT).show()
        } else {
            selectedMenuItems.add(menuItem.copy(quantity = 1))
            orderAdapter.notifyItemInserted(selectedMenuItems.size - 1)
            Toast.makeText(requireContext(), "${menuItem.menuName} ditambahkan ke pesanan", Toast.LENGTH_SHORT).show()
        }
        orderAdapter.notifyDataSetChanged()
        binding.nestedScrollView.visibility = View.VISIBLE
        binding.rvAllMenu.layoutManager = GridLayoutManager(context, 2)
        updateTotalPayment()
        showNoOrderMessage(selectedMenuItems.isEmpty())
    }

    fun formatPrice(price: Int): String {
        val numberFormat = NumberFormat.getInstance(Locale("id", "ID"))
        return numberFormat.format(price)
    }

    private fun updateTotalPayment() {
        val totalPayment = selectedMenuItems.sumOf { it.price * it.quantity }
        binding.tvTotalPaymentAmount.text = "Rp${formatPrice(totalPayment)}"
    }

    private fun toggleSidebar() {
        binding.nestedScrollView.visibility = if (binding.nestedScrollView.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
        binding.rvAllMenu.layoutManager = if (binding.nestedScrollView.visibility == View.VISIBLE) {
            GridLayoutManager(context, 2)
        } else {
            GridLayoutManager(context, 3)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showInvoiceDialog() {
        val dialogBinding = ViewModalInvoiceOrderBinding.inflate(layoutInflater)
        val totalPayment = selectedMenuItems.sumOf { it.price * it.quantity }
        dialogBinding.apply {
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
            tvNameEmployee.text = "Kasir"
            val paymentMethod = if (binding.rbCash.isChecked) "Cash" else "QRIS"
            tvPaymentMethodLabel.text = "Bayar ($paymentMethod)"
            tvMoneyPay.text = "Rp${formatPrice(totalPayment)}"
            if (binding.rbCash.isChecked) {
                val paymentInput = binding.etInputPayment.text.toString().replace("Rp", "").replace(".", "").trim()
                val moneyPay = if (paymentInput.isNotEmpty()) paymentInput.toInt() else 0
                val change = moneyPay - totalPayment
                tvMoneyChange.text = "Rp${formatPrice(change)}"
            } else {
                tvMoneyChange.text = "Rp0"
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
        dialogBinding.root.setPadding(24, 48, 24, 24)
    }

    private fun resetOrderData() {
        selectedMenuItems.clear()
        orderAdapter.notifyDataSetChanged()
        binding.tvTotalPaymentAmount.text = "Rp0"
        showNoOrderMessage(selectedMenuItems.isEmpty())
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

    private fun showMenuDialog() {
        dialogBinding = ViewModalAddEditMenuBinding.inflate(layoutInflater)
        val categories = resources.getStringArray(R.array.menu_category)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerMenuCategory.adapter = spinnerAdapter
        val suppliers = listOf(Pair(1, "Warung Bu Tuti"))
        val supplierNames = suppliers.map { it.second }.toTypedArray()
        val supplierAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, supplierNames)
        supplierAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerSupplierName.adapter = supplierAdapter
        dialogBinding.btnCameraMenu.setOnClickListener { checkStoragePermission() }
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogBinding.root).create()
        dialogBinding.apply {
            btnCancel.setOnClickListener { dialog.dismiss() }
            btnSave.setOnClickListener {
                val supplierIndex = spinnerSupplierName.selectedItemPosition
                val supplierId = suppliers[supplierIndex].first
                val supplierName = suppliers[supplierIndex].second
                val menuName = etMenuName.text.toString()
                val menuPrice = etMenuPrice.text.toString().toIntOrNull() ?: 0
                val menuStock = etMenuStock.text.toString().toIntOrNull() ?: 0
                val category = spinnerMenuCategory.selectedItem.toString()
                if (menuName.isNotEmpty() && menuPrice > 0 && menuStock >= 0) {
                    val foodType = when (category) {
                        "Makanan" -> "food"
                        "Camilan" -> "snack"
                        "Minuman" -> "drink"
                        else -> "other"
                    }
                    val namePart = menuName.toRequestBody("text/plain".toMediaTypeOrNull())
                    val pricePart = menuPrice.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val stockPart = menuStock.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val idSupplierPart = supplierId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                    val supplierNamePart = supplierName.toRequestBody("text/plain".toMediaTypeOrNull())
                    val foodTypePart = foodType.toRequestBody("text/plain".toMediaTypeOrNull())
                    var imagePart: MultipartBody.Part? = null
                    selectedImageUri?.let { uri ->
                        val file = File(requireContext().cacheDir, "menu_image_${System.currentTimeMillis()}.jpg")
                        requireContext().contentResolver.openInputStream(uri)?.use { input ->
                            file.outputStream().use { output -> input.copyTo(output) }
                        }
                        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        imagePart = MultipartBody.Part.createFormData("menu_picture", file.name, requestFile)
                    }
                    apiService.createMenu(namePart, pricePart, stockPart, idSupplierPart, supplierNamePart, foodTypePart, imagePart)
                        .enqueue(object : Callback<SingleMenuResponse> {
                            override fun onResponse(call: Call<SingleMenuResponse>, response: Response<SingleMenuResponse>) {
                                if (response.isSuccessful && response.body()?.success == true) {
                                    response.body()?.data?.let { newMenu ->
                                        Log.d("MenuFragment", "New menu created: $newMenu")
                                        originalMenuList.add(newMenu)
                                        filteredMenuList = originalMenuList
                                        menuAdapter.updateList(filteredMenuList)
                                        updateVisibility()
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
                                handleNetworkError(t, "Error creating menu")
                                selectedImageUri = null
                            }
                        })
                    dialog.dismiss()
                } else {
                    Toast.makeText(requireContext(), "Mohon isi semua data dengan benar", Toast.LENGTH_SHORT).show()
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