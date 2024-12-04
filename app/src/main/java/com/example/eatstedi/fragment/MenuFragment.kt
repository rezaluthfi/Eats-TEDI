package com.example.eatstedi.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eatstedi.R
import com.example.eatstedi.adapter.MenuAdapter
import com.example.eatstedi.adapter.OrderAdapter
import com.example.eatstedi.databinding.FragmentMenuBinding
import com.example.eatstedi.databinding.ViewModalAddEditMenuBinding
import com.example.eatstedi.databinding.ViewModalInvoiceOrderBinding
import com.example.eatstedi.model.MenuItem

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!  // Use this to safely access binding
    private val selectedMenuItems = mutableListOf<MenuItem>()
    private lateinit var orderAdapter: OrderAdapter  // Adapter for the order RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Dummy data for menu
        val menuList = listOf(
            MenuItem("Nasi Goreng Spesial", "Warung Makan Bu Tuti", "Rp15.000", "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
            MenuItem("Ayam Bakar Madu", "Dapoer Ayu", "Rp20.000", "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
            MenuItem("Mie Ayam Bakso", "Bakso Pak Slamet", "Rp18.000", "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
            MenuItem("Sate Ayam", "Sate Cak Udin", "Rp22.000", "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
            MenuItem("Nasi Uduk Komplit", "Warung Nasi Uduk", "Rp17.000", "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
            MenuItem("Soto Ayam", "Soto Ayam Pak Slamet", "Rp16.000", "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
            MenuItem("Bakso Malang", "Bakso Malang Pak Slamet", "Rp19.000", "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
            MenuItem("Nasi Kuning Komplit", "Warung Nasi Kuning", "Rp18.000", "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
            MenuItem("Mie Goreng Spesial", "Warung Makan Bu Tuti", "Rp15.000", "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
            MenuItem("Ayam Geprek", "Ayam Geprek Bu Tuti", "Rp20.000", "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
            MenuItem("Nasi Padang", "Warung Nasi Padang", "Rp22.000", "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg"),
            MenuItem("Sate Padang", "Sate Padang Pak Slamet", "Rp25.000", "https://cdn1-production-images-kly.akamaized.net/1psFPKhBdQ4-qCoIwvI9QN7ouqA=/640x640/smart/filters:quality(75):strip_icc():format(jpeg)/kly-media-production/medias/4102226/original/065145300_1658889141-nasi_goreng_resepyummyanak.jpg")
        )

        // Initialize MenuAdapter with item click listener
        val menuAdapter = MenuAdapter(menuList) { menuItem ->
            addItemToOrder(menuItem)
        }
        binding.rvAllMenu.adapter = menuAdapter
       //jika nested scroll view tampil, maka grid layout manager berkolom 2, jika tidak maka berkolom 3
        binding.rvAllMenu.layoutManager = if (binding.nestedScrollView.visibility == View.VISIBLE) {
            GridLayoutManager(context, 2)
        } else {
            GridLayoutManager(context, 3)
        }

        // Initialize order RecyclerView adapter
        orderAdapter = OrderAdapter(selectedMenuItems)
        binding.rvOrderMenu.adapter = orderAdapter
        binding.rvOrderMenu.layoutManager = LinearLayoutManager(context)

        // Toggle visibility of the sidebar with arrow button
        binding.arrowToggle.setOnClickListener {
            toggleSidebar()
        }

        // Set the click listener for the Pay Now button
        binding.btnPayNow.setOnClickListener {
            showInvoiceDialog()
        }

        // Set the click listener for the Add New Menu button
        binding.btnAddNewMenu.setOnClickListener {
            showMenuDialog()
        }

        // Dummy data for names from the resource array (replace with your actual resource)
        val arrayNames = resources.getStringArray(R.array.supplier_name)

        // Create an ArrayAdapter for the Spinner using a simple layout
        val nameAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, arrayNames)
        nameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set the adapter to the Spinner using binding
        binding.spFilterName.adapter = nameAdapter

        // Set listener for when an item is selected
        binding.spFilterName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Get the selected name from the Spinner
                //val selectedName = arrayNames[position]
                // You can use this name to filter data or perform other actions
                //filterDataByName(selectedName)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Optional: actions when no item is selected
            }
        }

    }

    private fun addItemToOrder(menuItem: MenuItem) {
        selectedMenuItems.add(menuItem)
        orderAdapter.notifyDataSetChanged()  // Update the RecyclerView
        binding.nestedScrollView.visibility = View.VISIBLE  // Show sidebar
    }

    private fun toggleSidebar() {
        binding.nestedScrollView.visibility = if (binding.nestedScrollView.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun showInvoiceDialog() {
        // Inflate the invoice dialog layout using ViewBinding
        val dialogBinding = ViewModalInvoiceOrderBinding.inflate(layoutInflater)

        // Set values for the invoice dynamically if needed
        dialogBinding.tvTotalPaymentAmount.text = "Rp20.000,00"
        dialogBinding.tvMoneyPay.text = "Rp50.000,00"
        dialogBinding.tvMoneyChange.text = "Rp30.000,00"

        // Create the AlertDialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Set the Close button functionality
        dialogBinding.payNow.setOnClickListener {
            dialog.dismiss()  // Close the dialog when "Close" button is clicked
        }

        // Show the dialog
        dialog.show()

        // Adjust the dialog size to match the content width (wrap content) and prevent white space
        dialog.window?.apply {
            setLayout(
                WindowManager.LayoutParams.WRAP_CONTENT, // Adjust width to match content
                WindowManager.LayoutParams.WRAP_CONTENT  // Adjust height to match content
            )
        }
    }

    // Function to show the Add Menu dialog
    private fun showMenuDialog() {
        // Inflate the dialog layout using ViewBinding
        val dialogBinding = ViewModalAddEditMenuBinding.inflate(layoutInflater)

        // Set up the spinner with example data
        // Categories from strings.xml
        val categories = resources.getStringArray(R.array.menu_category)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerMenuCategory.adapter = spinnerAdapter

        // Create the dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Show the dialog
        dialog.show()

        // Set up listeners for buttons in the dialog
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()  // Close the dialog
        }

        dialogBinding.btnSave.setOnClickListener {
            // You can handle saving logic here, such as collecting values from the EditTexts and Spinner
            val supplierName = dialogBinding.etSupplierName.text.toString()
            val menuName = dialogBinding.etMenuName.text.toString()
            val menuPrice = dialogBinding.etMenuPrice.text.toString()
            val category = dialogBinding.spinnerMenuCategory.selectedItem.toString()

            // Optionally, do something with the data
            // For now, we just log the data to confirm it's working
            println("Supplier: $supplierName, Menu: $menuName, Price: $menuPrice, Category: $category")

            dialog.dismiss()  // Close the dialog
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Clean up binding to avoid memory leaks
    }
}

