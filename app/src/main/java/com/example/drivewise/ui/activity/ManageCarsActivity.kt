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

class ManageCarsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageCarsBinding
    private lateinit var carAdapter: CarAdapter

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

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        carAdapter = CarAdapter(
            onEditClick = { car ->
                val intent = Intent(this, AddEditCarActivity::class.java)
                intent.putExtra("CAR_ID", car.id)
                startActivity(intent)
            },
            onDeleteClick = { car ->
                showDeleteConfirmation(car)
            },
            onToggleAvailability = { car ->
                viewModel.updateAvailability(car.id, !car.available)
            }
        )

        binding.rvCars.apply {
            layoutManager = LinearLayoutManager(this@ManageCarsActivity)
            adapter = carAdapter
        }

        binding.fabAddCar.setOnClickListener {
            startActivity(Intent(this, AddEditCarActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    if (state.errorMessage != null) {
                        binding.tvEmpty.text = "Error: ${state.errorMessage}\n\nPlease check Firestore security rules."
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvCars.visibility = View.GONE
                    } else if (state.cars.isEmpty() && !state.isLoading) {
                        binding.tvEmpty.text = "No cars found.\nTap + to add a car."
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvCars.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvCars.visibility = View.VISIBLE
                        carAdapter.submitList(state.cars)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is CarEvent.CarDeleted -> {
                            Toast.makeText(this@ManageCarsActivity, "Car deleted successfully", Toast.LENGTH_SHORT).show()
                            viewModel.clearEvent()
                        }
                        is CarEvent.CarUpdated -> {
                            Toast.makeText(this@ManageCarsActivity, "Car updated", Toast.LENGTH_SHORT).show()
                            viewModel.clearEvent()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmation(car: Car) {
        AlertDialog.Builder(this)
            .setTitle("Delete Car")
            .setMessage("Are you sure you want to delete ${car.brand} ${car.model}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCar(car.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

