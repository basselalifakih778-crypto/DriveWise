package com.example.drivewise.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.drivewise.databinding.ActivityBookingDetailsBinding
import com.example.drivewise.domain.model.Booking
import com.example.drivewise.domain.model.BookingStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BookingDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookingDetailsBinding
    private val firestore = FirebaseFirestore.getInstance()

    private var bookingId: String = ""
    private var booking: Booking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookingDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookingId = intent.getStringExtra("BOOKING_ID") ?: ""

        if (bookingId.isEmpty()) {
            Toast.makeText(this, "Invalid booking", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadBooking()
    }

    override fun onResume() {
        super.onResume()
        loadBooking()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnUploadPhotos.setOnClickListener {
            booking?.let { b ->
                val intent = Intent(this, UploadPhotosActivity::class.java)
                intent.putExtra("BOOKING_ID", b.id)
                intent.putExtra("CAR_NAME", b.carName)
                startActivity(intent)
            }
        }

        binding.btnCancelBooking.setOnClickListener {
            showCancelConfirmation()
        }
    }

    private fun loadBooking() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val snapshot = firestore.collection("bookings").document(bookingId).get().await()
                booking = snapshot.toObject(Booking::class.java)?.copy(id = snapshot.id)

                booking?.let { displayBooking(it) }
                    ?: run {
                        Toast.makeText(this@BookingDetailsActivity, "Booking not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            } catch (e: Exception) {
                Toast.makeText(this@BookingDetailsActivity, "Error loading booking: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun displayBooking(booking: Booking) {
        binding.tvBookingId.text = "Booking #${booking.id.takeLast(8).uppercase()}"
        binding.tvCarName.text = booking.carName
        binding.tvStartDate.text = booking.startDate
        binding.tvEndDate.text = booking.endDate
        binding.tvTotalPrice.text = "$${String.format("%.2f", booking.totalPrice)}"

        // Status display
        val (statusText, statusColor) = when (booking.getStatusEnum()) {
            BookingStatus.PENDING -> "⏳ Pending Approval" to android.R.color.holo_orange_dark
            BookingStatus.APPROVED -> "✅ Approved" to android.R.color.holo_green_dark
            BookingStatus.REJECTED -> "❌ Rejected" to android.R.color.holo_red_dark
            BookingStatus.CANCELLED -> "🚫 Cancelled" to android.R.color.darker_gray
            BookingStatus.COMPLETED -> "🎉 Completed" to android.R.color.holo_blue_dark
        }
        binding.tvStatus.text = statusText
        binding.tvStatus.setTextColor(getColor(statusColor))

        // Admin notes
        if (booking.adminNotes.isNotEmpty()) {
            binding.tvAdminNotesLabel.visibility = View.VISIBLE
            binding.cardAdminNotes.visibility = View.VISIBLE
            binding.tvAdminNotes.text = booking.adminNotes
        } else {
            binding.tvAdminNotesLabel.visibility = View.GONE
            binding.cardAdminNotes.visibility = View.GONE
        }

        // Show/hide action buttons based on status
        when (booking.getStatusEnum()) {
            BookingStatus.PENDING -> {
                binding.btnCancelBooking.visibility = View.VISIBLE
                binding.btnUploadPhotos.visibility = View.GONE
            }
            BookingStatus.APPROVED -> {
                binding.btnCancelBooking.visibility = View.GONE
                binding.btnUploadPhotos.visibility = View.VISIBLE
            }
            else -> {
                binding.btnCancelBooking.visibility = View.GONE
                binding.btnUploadPhotos.visibility = View.GONE
            }
        }
    }

    private fun showCancelConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                cancelBooking()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelBooking() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCancelBooking.isEnabled = false

        lifecycleScope.launch {
            try {
                firestore.collection("bookings").document(bookingId)
                    .update("status", BookingStatus.CANCELLED.name)
                    .await()

                Toast.makeText(this@BookingDetailsActivity, "Booking cancelled", Toast.LENGTH_SHORT).show()
                loadBooking()

            } catch (e: Exception) {
                Toast.makeText(this@BookingDetailsActivity, "Error cancelling booking: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnCancelBooking.isEnabled = true
            }
        }
    }
}

