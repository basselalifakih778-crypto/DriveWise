package com.example.drivewise.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.drivewise.Data.remote.FirebaseCarRepository
import com.example.drivewise.databinding.ActivityBrowseCarsBinding
import com.example.drivewise.domain.model.Car
import com.example.drivewise.ui.adapter.ClientCarAdapter
import com.example.drivewise.ui.viewmodel.CarViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class BrowseCarsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowseCarsBinding
    private lateinit var carAdapter: ClientCarAdapter

    private val viewModel: CarViewModel by viewModels {
        CarViewModelFactory(
            FirebaseCarRepository(FirebaseFirestore.getInstance())
        )
    }

    // Filter values
    private var selectedTransmission: String? = null
    private var selectedFuel: String? = null
    private var minPrice: Double? = null
    private var maxPrice: Double? = null

    // All cars for filtering
    private var allCars: List<Car> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowseCarsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupFilters()
        observeState()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        carAdapter = ClientCarAdapter { car ->
            // Navigate to car details
            val intent = Intent(this, CarDetailsActivity::class.java)
            intent.putExtra("CAR_ID", car.id)
            startActivity(intent)
        }

        binding.rvCars.apply {
            layoutManager = LinearLayoutManager(this@BrowseCarsActivity)
            adapter = carAdapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            // Refresh is handled by the observer, just reset the indicator
            binding.swipeRefresh.isRefreshing = false
        }

        binding.btnApplyFilters.setOnClickListener {
            applyFilters()
        }

        binding.btnClearFilters.setOnClickListener {
            clearFilters()
        }
    }

    private fun setupFilters() {
        val transmissions = listOf("All", "Automatic", "Manual", "CVT")
        val fuels = listOf("All", "Petrol", "Diesel", "Electric", "Hybrid")

        binding.actvTransmission.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, transmissions)
        )
        binding.actvFuel.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fuels)
        )
    }

    private fun applyFilters() {
        val transmissionText = binding.actvTransmission.text.toString()
        val fuelText = binding.actvFuel.text.toString()
        val minPriceText = binding.etMinPrice.text.toString()
        val maxPriceText = binding.etMaxPrice.text.toString()

        selectedTransmission = if (transmissionText.isNotEmpty() && transmissionText != "All") transmissionText else null
        selectedFuel = if (fuelText.isNotEmpty() && fuelText != "All") fuelText else null
        minPrice = minPriceText.toDoubleOrNull()
        maxPrice = maxPriceText.toDoubleOrNull()

        filterAndDisplayCars()
    }

    private fun clearFilters() {
        binding.actvTransmission.setText("", false)
        binding.actvFuel.setText("", false)
        binding.etMinPrice.text?.clear()
        binding.etMaxPrice.text?.clear()

        selectedTransmission = null
        selectedFuel = null
        minPrice = null
        maxPrice = null

        filterAndDisplayCars()
    }

    private fun filterAndDisplayCars() {
        var filteredCars = allCars.filter { it.available }

        selectedTransmission?.let { transmission ->
            filteredCars = filteredCars.filter { it.transmission.equals(transmission, ignoreCase = true) }
        }

        selectedFuel?.let { fuel ->
            filteredCars = filteredCars.filter { it.fuel.equals(fuel, ignoreCase = true) }
        }

        minPrice?.let { min ->
            filteredCars = filteredCars.filter { it.dayPrice >= min }
        }

        maxPrice?.let { max ->
            filteredCars = filteredCars.filter { it.dayPrice <= max }
        }

        carAdapter.submitList(filteredCars)
        updateResultsCount(filteredCars.size)

        binding.tvEmpty.visibility = if (filteredCars.isEmpty()) View.VISIBLE else View.GONE
        binding.rvCars.visibility = if (filteredCars.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateResultsCount(count: Int) {
        binding.tvResultsCount.text = when {
            count == 0 -> "No cars found"
            count == 1 -> "Showing 1 car"
            else -> "Showing $count cars"
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    // Store all cars and filter
                    allCars = state.cars
                    filterAndDisplayCars()

                    state.errorMessage?.let { error ->
                        binding.tvEmpty.text = error
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}

