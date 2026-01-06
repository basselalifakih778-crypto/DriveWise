/**
 * ManageCarsActivity.kt
 * =======================
 * This Activity allows admins to manage the rental car fleet.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. FULL CRUD: This screen supports all CRUD operations:
 *    - Create: FAB → AddEditCarActivity
 *    - Read: RecyclerView with CarAdapter
 *    - Update: Edit button → AddEditCarActivity with car ID
 *    - Delete: Delete button with confirmation dialog
 *
 * 2. ALERTDIALOG: Before deleting, we show a confirmation dialog.
 *    This prevents accidental deletions.
 *
 * 3. TOGGLE AVAILABILITY: Quick button to mark car available/unavailable
 *    without opening the full edit screen.
 *
 * 4. EMPTY STATE HANDLING: Shows helpful message when no cars exist,
 *    or shows error message if Firestore query fails.
 *
 * ADMIN ONLY: Only admins should access this screen.
 */
package com.example.drivewise.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.drivewise.Data.remote.FirebaseCarRepository
import com.example.drivewise.databinding.ActivityManageCarsBinding
import com.example.drivewise.domain.model.Car
import com.example.drivewise.ui.adapter.CarAdapter
import com.example.drivewise.ui.viewmodel.CarEvent
import com.example.drivewise.ui.viewmodel.CarViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * Admin screen for managing the car fleet.
 *
 * Features:
 * - View all cars in a list
 * - Add new cars via FAB
 * - Edit existing cars
 * - Delete cars with confirmation
 * - Toggle car availability
 */
class ManageCarsActivity : AppCompatActivity() {

    /** ViewBinding for activity_manage_cars.xml */
    private lateinit var binding: ActivityManageCarsBinding

    /** Adapter for cars RecyclerView */
    private lateinit var carAdapter: CarAdapter

    /** ViewModel for car operations */
    private val viewModel: CarViewModel by viewModels {
        CarViewModelFactory(
            FirebaseCarRepository(FirebaseFirestore.getInstance())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageCarsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeState()
    }

    /**
     * Sets up UI components.
     */
    private fun setupViews() {
        // Back navigation
        binding.toolbar.setNavigationOnClickListener { finish() }

        // ─────────────────────────────────────────────────────────────────────
        // CAR ADAPTER WITH CALLBACKS
        // Each callback handles a different action
        // ─────────────────────────────────────────────────────────────────────

        carAdapter = CarAdapter(
            // Edit button clicked - open AddEditCarActivity with car ID
            onEditClick = { car ->
                val intent = Intent(this, AddEditCarActivity::class.java)
                intent.putExtra("CAR_ID", car.id)  // Pass ID to load existing car
                startActivity(intent)
            },

            // Delete button clicked - show confirmation dialog
            onDeleteClick = { car ->
                showDeleteConfirmation(car)
            },

            // Toggle availability button clicked - flip the available status
            onToggleAvailability = { car ->
                viewModel.updateAvailability(car.id, !car.available)
            }
        )

        // Set up RecyclerView
        binding.rvCars.apply {
            layoutManager = LinearLayoutManager(this@ManageCarsActivity)
            adapter = carAdapter
        }

        // ─────────────────────────────────────────────────────────────────────
        // FAB FOR ADDING NEW CARS
        // ─────────────────────────────────────────────────────────────────────

        binding.fabAddCar.setOnClickListener {
            // No CAR_ID means "create new" mode
            startActivity(Intent(this, AddEditCarActivity::class.java))
        }

        // Pull-to-refresh (cars are already real-time, so just stop animation)
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }
    }

    /**
     * Observes ViewModel state and events.
     */
    private fun observeState() {
        // ─────────────────────────────────────────────────────────────────────
        // OBSERVE UI STATE
        // ─────────────────────────────────────────────────────────────────────

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    // Show/hide loading spinner
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Handle different states
                    if (state.errorMessage != null) {
                        // Error state - show error message
                        binding.tvEmpty.text = "Error: ${state.errorMessage}\n\nPlease check Firestore security rules."
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvCars.visibility = View.GONE
                    } else if (state.cars.isEmpty() && !state.isLoading) {
                        // Empty state - show helpful message
                        binding.tvEmpty.text = "No cars found.\nTap + to add a car."
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvCars.visibility = View.GONE
                    } else {
                        // Success state - show cars list
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvCars.visibility = View.VISIBLE
                        carAdapter.submitList(state.cars)
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // OBSERVE ONE-TIME EVENTS
        // ─────────────────────────────────────────────────────────────────────

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is CarEvent.CarDeleted -> {
                            Toast.makeText(
                                this@ManageCarsActivity,
                                "Car deleted successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.clearEvent()
                        }
                        is CarEvent.CarUpdated -> {
                            Toast.makeText(
                                this@ManageCarsActivity,
                                "Car updated",
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.clearEvent()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    /**
     * Shows a confirmation dialog before deleting a car.
     *
     * ALERTDIALOG: Android's built-in dialog component.
     * Builder pattern makes it easy to configure title, message, buttons.
     *
     * @param car The car to potentially delete
     */
    private fun showDeleteConfirmation(car: Car) {
        AlertDialog.Builder(this)
            .setTitle("Delete Car")
            .setMessage("Are you sure you want to delete ${car.brand} ${car.model}?")
            .setPositiveButton("Delete") { _, _ ->
                // User confirmed - delete the car
                viewModel.deleteCar(car.id)
            }
            .setNegativeButton("Cancel", null)  // null = just dismiss dialog
            .show()
    }
}

