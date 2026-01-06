package com.example.drivewise.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.drivewise.databinding.ActivityMyBookingsBinding
import com.example.drivewise.domain.model.Booking
import com.example.drivewise.domain.model.BookingStatus
import com.example.drivewise.ui.adapter.ClientBookingAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyBookingsBinding
    private lateinit var bookingAdapter: ClientBookingAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var allBookings: List<Booking> = emptyList()
    private var selectedStatus: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        loadBookings()
    }

    override fun onResume() {
        super.onResume()
        loadBookings()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        bookingAdapter = ClientBookingAdapter(
            onBookingClick = { booking ->
                // Navigate to booking details
                val intent = Intent(this, BookingDetailsActivity::class.java)
                intent.putExtra("BOOKING_ID", booking.id)
                startActivity(intent)
            },
            onUploadPhotosClick = { booking ->
                // Navigate to upload photos
                val intent = Intent(this, UploadPhotosActivity::class.java)
                intent.putExtra("BOOKING_ID", booking.id)
                intent.putExtra("CAR_NAME", booking.carName)
                startActivity(intent)
            }
        )

        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(this@MyBookingsActivity)
            adapter = bookingAdapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadBookings()
        }

        binding.btnBrowseCars.setOnClickListener {
            startActivity(Intent(this, BrowseCarsActivity::class.java))
        }

        // Setup status filter chips
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedStatus = when {
                checkedIds.contains(binding.chipPending.id) -> BookingStatus.PENDING.name
                checkedIds.contains(binding.chipApproved.id) -> BookingStatus.APPROVED.name
                checkedIds.contains(binding.chipCompleted.id) -> BookingStatus.COMPLETED.name
                checkedIds.contains(binding.chipCancelled.id) -> BookingStatus.CANCELLED.name
                else -> null
            }
            filterBookings()
        }
    }

    private fun loadBookings() {
        val userId = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val snapshot = firestore.collection("bookings")
                    .whereEqualTo("userId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                allBookings = snapshot.documents.mapNotNull {
                    it.toObject(Booking::class.java)?.copy(id = it.id)
                }

                filterBookings()

            } catch (e: Exception) {
                binding.tvEmpty.text = "Error loading bookings: ${e.message}"
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.rvBookings.visibility = View.GONE
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun filterBookings() {
        val filteredBookings = if (selectedStatus != null) {
            allBookings.filter { it.status == selectedStatus }
        } else {
            allBookings
        }

        bookingAdapter.submitList(filteredBookings)

        if (filteredBookings.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvBookings.visibility = View.GONE
            binding.tvEmpty.text = if (allBookings.isEmpty()) {
                "You haven't made any bookings yet"
            } else {
                "No bookings with this status"
            }
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvBookings.visibility = View.VISIBLE
        }
    }
}

