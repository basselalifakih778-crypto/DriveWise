/**
 * BrowseCarsActivity.kt
 * =======================
 * This Activity allows clients to browse available rental cars.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. CLIENT-FACING: This is where clients look for cars to rent.
 *    Only available cars (available=true) are shown by default.
 *
 * 2. CLIENT-SIDE FILTERING: We load all cars from Firestore once,
 *    then filter in Kotlin code. This is simpler than complex Firestore queries.
 *    Good for small datasets (<1000 cars).
 *
 * 3. FILTER STATE: We track filter values (transmission, fuel, price range)
 *    in instance variables. When "Apply Filters" is clicked, we re-filter.
 *
 * 4. AUTOCOMPLETE DROPDOWN: Uses AutoCompleteTextView with ArrayAdapter
 *    for transmission and fuel selection.
 *
 * FLOW:
 * 1. Load all cars → filter to available only
 * 2. User sets filters → clicks "Apply"
 * 3. filterAndDisplayCars() applies all active filters
 * 4. Click car → CarDetailsActivity to book
 */
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

/**
 * Client screen for browsing and filtering available cars.
 */
class BrowseCarsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowseCarsBinding
    private lateinit var carAdapter: ClientCarAdapter

    private val viewModel: CarViewModel by viewModels {
        CarViewModelFactory(
            FirebaseCarRepository(FirebaseFirestore.getInstance())
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FILTER STATE
    // These hold the current filter values
    // ─────────────────────────────────────────────────────────────────────────

    /** Selected transmission filter (null = no filter) */
    private var selectedTransmission: String? = null

    /** Selected fuel type filter (null = no filter) */
    private var selectedFuel: String? = null

    /** Minimum price filter (null = no minimum) */
    private var minPrice: Double? = null

    /** Maximum price filter (null = no maximum) */
    private var maxPrice: Double? = null

    /** All cars from database (before filtering) */
    private var allCars: List<Car> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowseCarsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupFilters()
        observeState()
    }

    /**
     * Sets up UI components.
     */
    private fun setupViews() {
        // Back navigation
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Create adapter - clicking a car opens details
        carAdapter = ClientCarAdapter { car ->
            val intent = Intent(this, CarDetailsActivity::class.java)
            intent.putExtra("CAR_ID", car.id)
            startActivity(intent)
        }

        // Set up RecyclerView
        binding.rvCars.apply {
            layoutManager = LinearLayoutManager(this@BrowseCarsActivity)
            adapter = carAdapter
        }

        // Pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }

        // Filter buttons
        binding.btnApplyFilters.setOnClickListener {
            applyFilters()
        }

        binding.btnClearFilters.setOnClickListener {
            clearFilters()
        }
    }

    /**
     * Sets up the filter dropdown options.
     *
     * ARRAYADAPTER: Bridges a list of strings to the dropdown view.
     * "All" option means "no filter for this field".
     */
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

    /**
     * Reads filter values from UI and applies them.
     */
    private fun applyFilters() {
        // Read values from UI
        val transmissionText = binding.actvTransmission.text.toString()
        val fuelText = binding.actvFuel.text.toString()
        val minPriceText = binding.etMinPrice.text.toString()
        val maxPriceText = binding.etMaxPrice.text.toString()

        // Convert to filter values (null means "no filter")
        selectedTransmission = if (transmissionText.isNotEmpty() && transmissionText != "All") transmissionText else null
        selectedFuel = if (fuelText.isNotEmpty() && fuelText != "All") fuelText else null
        minPrice = minPriceText.toDoubleOrNull()
        maxPrice = maxPriceText.toDoubleOrNull()

        // Re-filter and display
        filterAndDisplayCars()
    }

    /**
     * Clears all filters and resets UI.
     */
    private fun clearFilters() {
        // Clear UI
        binding.actvTransmission.setText("", false)
        binding.actvFuel.setText("", false)
        binding.etMinPrice.text?.clear()
        binding.etMaxPrice.text?.clear()

        // Clear filter state
        selectedTransmission = null
        selectedFuel = null
        minPrice = null
        maxPrice = null

        // Re-filter (will show all available cars)
        filterAndDisplayCars()
    }

    /**
     * Applies all active filters to allCars and updates the RecyclerView.
     *
     * FILTER CHAIN: Each let{} block adds another filter condition.
     * If selectedTransmission is not null, we filter by it. And so on.
     */
    private fun filterAndDisplayCars() {
        // Start with only available cars
        var filteredCars = allCars.filter { it.available }

        // Apply transmission filter if set
        selectedTransmission?.let { transmission ->
            filteredCars = filteredCars.filter { it.transmission.equals(transmission, ignoreCase = true) }
        }

        // Apply fuel filter if set
        selectedFuel?.let { fuel ->
            filteredCars = filteredCars.filter { it.fuel.equals(fuel, ignoreCase = true) }
        }

        // Apply min price filter if set
        minPrice?.let { min ->
            filteredCars = filteredCars.filter { it.dayPrice >= min }
        }

        // Apply max price filter if set
        maxPrice?.let { max ->
            filteredCars = filteredCars.filter { it.dayPrice <= max }
        }

        // Update UI
        carAdapter.submitList(filteredCars)
        updateResultsCount(filteredCars.size)

        // Show/hide empty state
        binding.tvEmpty.visibility = if (filteredCars.isEmpty()) View.VISIBLE else View.GONE
        binding.rvCars.visibility = if (filteredCars.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * Updates the "Showing X cars" text.
     */
    private fun updateResultsCount(count: Int) {
        binding.tvResultsCount.text = when {
            count == 0 -> "No cars found"
            count == 1 -> "Showing 1 car"
            else -> "Showing $count cars"
        }
    }

    /**
     * Observes ViewModel state for car data.
     */
    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    // Store all cars and apply filters
                    allCars = state.cars
                    filterAndDisplayCars()

                    // Show error if any
                    state.errorMessage?.let { error ->
                        binding.tvEmpty.text = error
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
}

