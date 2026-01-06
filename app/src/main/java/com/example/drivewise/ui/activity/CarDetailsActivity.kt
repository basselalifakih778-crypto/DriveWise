package com.example.drivewise.ui.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.drivewise.R
import com.example.drivewise.databinding.ActivityCarDetailsBinding
import com.example.drivewise.domain.model.Booking
import com.example.drivewise.domain.model.BookingStatus
import com.example.drivewise.domain.model.Car
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CarDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarDetailsBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var car: Car? = null
    private var startDate: Calendar? = null
    private var endDate: Calendar? = null

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val carId = intent.getStringExtra("CAR_ID")
        if (carId == null) {
            Toast.makeText(this, "Car not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadCarDetails(carId)
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.etStartDate.setOnClickListener {
            showDatePicker(true)
        }

        binding.etEndDate.setOnClickListener {
            showDatePicker(false)
        }

        binding.btnBookNow.setOnClickListener {
            submitBookingRequest()
        }
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()

        // If selecting end date and start date is set, use start date as minimum
        if (!isStartDate && startDate != null) {
            calendar.timeInMillis = startDate!!.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val picker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }

                if (isStartDate) {
                    startDate = selectedDate
                    binding.etStartDate.setText(dateFormat.format(selectedDate.time))

                    // Clear end date if it's before the new start date
                    if (endDate != null && endDate!!.before(selectedDate)) {
                        endDate = null
                        binding.etEndDate.text?.clear()
                    }
                } else {
                    endDate = selectedDate
                    binding.etEndDate.setText(dateFormat.format(selectedDate.time))
                }

                calculateTotalPrice()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today for start date, or day after start for end date
        val minDate = Calendar.getInstance()
        if (!isStartDate && startDate != null) {
            minDate.timeInMillis = startDate!!.timeInMillis
            minDate.add(Calendar.DAY_OF_MONTH, 1)
        }
        picker.datePicker.minDate = minDate.timeInMillis

        picker.show()
    }

    private fun calculateTotalPrice() {
        val start = startDate
        val end = endDate
        val currentCar = car

        if (start != null && end != null && currentCar != null) {
            val diffInMillis = end.timeInMillis - start.timeInMillis
            val days = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

            if (days > 0) {
                val totalPrice = days * currentCar.dayPrice
                binding.tvTotalPrice.text = "$${String.format("%.2f", totalPrice)}"
            } else {
                binding.tvTotalPrice.text = "$0.00"
            }
        } else {
            binding.tvTotalPrice.text = "$0.00"
        }
    }

    private fun loadCarDetails(carId: String) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val snapshot = firestore.collection("cars").document(carId).get().await()
                car = snapshot.toObject(Car::class.java)?.copy(id = snapshot.id)

                car?.let { displayCarDetails(it) }
                    ?: run {
                        Toast.makeText(this@CarDetailsActivity, "Car not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            } catch (e: Exception) {
                Toast.makeText(this@CarDetailsActivity, "Error loading car: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayCarDetails(car: Car) {
        binding.tvCarName.text = "${car.brand} ${car.model}"
        binding.tvCarYear.text = car.year.toString()
        binding.tvPriceBadge.text = "$${String.format("%.0f", car.dayPrice)}/day"
        binding.tvTransmission.text = car.transmission
        binding.tvFuel.text = car.fuel
        binding.tvDescription.text = car.description.ifEmpty { "No description available" }

        // Availability
        if (car.available) {
            binding.tvAvailability.text = "✅ Available for Booking"
            binding.tvAvailability.setTextColor(getColor(android.R.color.holo_green_dark))
            binding.btnBookNow.isEnabled = true
        } else {
            binding.tvAvailability.text = "❌ Currently Unavailable"
            binding.tvAvailability.setTextColor(getColor(android.R.color.holo_red_dark))
            binding.btnBookNow.isEnabled = false
        }

        // Image
        if (car.imageUrl.isNotEmpty()) {
            binding.ivCarImage.load(car.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_car_placeholder)
                error(R.drawable.ic_car_placeholder)
            }
        }

        // Features
        binding.chipGroupFeatures.removeAllViews()
        if (car.features.isEmpty()) {
            val chip = Chip(this)
            chip.text = "No features listed"
            chip.isEnabled = false
            binding.chipGroupFeatures.addView(chip)
        } else {
            car.features.forEach { feature ->
                val chip = Chip(this)
                chip.text = feature
                chip.isCheckable = false
                binding.chipGroupFeatures.addView(chip)
            }
        }
    }

    private suspend fun isCurrentUserVerified(uid: String): Boolean {
        val snap = firestore.collection("users").document(uid).get().await()
        val raw = snap.get("isVerified")
        return when (raw) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> raw.equals("true", ignoreCase = true) || raw == "1"
            else -> false
        }
    }

    private fun showNotVerifiedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Verification required")
            .setMessage("You must verify your account (upload license/ID) before booking a car.")
            .setPositiveButton("Go to Profile") { _, _ ->
                startActivity(Intent(this, MyProfileActivity::class.java))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitBookingRequest() {
        val currentCar = car ?: return
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Please login to book a car", Toast.LENGTH_SHORT).show()
            return
        }

        if (startDate == null) {
            binding.tilStartDate.error = "Please select start date"
            return
        }

        if (endDate == null) {
            binding.tilEndDate.error = "Please select end date"
            return
        }

        binding.tilStartDate.error = null
        binding.tilEndDate.error = null

        val start = startDate!!
        val end = endDate!!

        val diffInMillis = end.timeInMillis - start.timeInMillis
        val days = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

        if (days <= 0) {
            Toast.makeText(this, "End date must be after start date", Toast.LENGTH_SHORT).show()
            return
        }

        val totalPrice = days * currentCar.dayPrice

        binding.progressBar.visibility = View.VISIBLE
        binding.btnBookNow.isEnabled = false

        lifecycleScope.launch {
            try {
                // Block booking if user isn't verified
                val verified = isCurrentUserVerified(currentUser.uid)
                if (!verified) {
                    showNotVerifiedDialog()
                    return@launch
                }

                // Get user details
                val userSnapshot = firestore.collection("users").document(currentUser.uid).get().await()
                val userName = userSnapshot.getString("fullName") ?: currentUser.email ?: "Unknown"
                val userEmail = currentUser.email ?: ""

                val booking = Booking(
                    carId = currentCar.id,
                    userId = currentUser.uid,
                    userName = userName,
                    userEmail = userEmail,
                    carName = "${currentCar.brand} ${currentCar.model}",
                    startDate = dateFormat.format(start.time),
                    endDate = dateFormat.format(end.time),
                    totalPrice = totalPrice,
                    status = BookingStatus.PENDING.name,
                    createdAt = System.currentTimeMillis()
                )

                firestore.collection("bookings").add(booking).await()

                Toast.makeText(
                    this@CarDetailsActivity,
                    "Booking request submitted! You'll be notified when it's approved.",
                    Toast.LENGTH_LONG
                ).show()

                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@CarDetailsActivity,
                    "Error submitting booking: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnBookNow.isEnabled = true
            }
        }
    }
}
