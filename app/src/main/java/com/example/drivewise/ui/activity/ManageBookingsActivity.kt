package com.example.drivewise.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.drivewise.Data.remote.FirebaseBookingRepository
import com.example.drivewise.databinding.ActivityManageBookingsBinding
import com.example.drivewise.domain.model.Booking
import com.example.drivewise.domain.model.BookingStatus
import com.example.drivewise.ui.adapter.BookingAdapter
import com.example.drivewise.ui.viewmodel.BookingEvent
import com.example.drivewise.ui.viewmodel.BookingViewModel
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ManageBookingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBookingsBinding
    private lateinit var bookingAdapter: BookingAdapter

    private val viewModel: BookingViewModel by viewModels {
        BookingViewModelFactory(
            FirebaseBookingRepository(FirebaseFirestore.getInstance())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBookingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeState()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Setup tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Pending"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Approved"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Rejected"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Completed"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val status = when (tab?.position) {
                    1 -> BookingStatus.PENDING
                    2 -> BookingStatus.APPROVED
                    3 -> BookingStatus.REJECTED
                    4 -> BookingStatus.COMPLETED
                    else -> null
                }
                viewModel.filterByStatus(status)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        bookingAdapter = BookingAdapter(
            onApproveClick = { booking ->
                showApproveDialog(booking)
            },
            onRejectClick = { booking ->
                showRejectDialog(booking)
            },
            onViewPhotosClick = { booking ->
                val intent = Intent(this, CarPhotosActivity::class.java)
                intent.putExtra("BOOKING_ID", booking.id)
                intent.putExtra("CAR_NAME", booking.carName)
                intent.putStringArrayListExtra("PICKUP_PHOTOS", ArrayList(booking.pickupPhotos))
                intent.putStringArrayListExtra("RETURN_PHOTOS", ArrayList(booking.returnPhotos))
                startActivity(intent)
            },
            onCompleteClick = { booking ->
                showCompleteDialog(booking)
            }
        )

        binding.rvBookings.apply {
            layoutManager = LinearLayoutManager(this@ManageBookingsActivity)
            adapter = bookingAdapter
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

                    if (state.filteredBookings.isEmpty() && !state.isLoading) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvBookings.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvBookings.visibility = View.VISIBLE
                        bookingAdapter.submitList(state.filteredBookings)
                    }

                    state.errorMessage?.let { error ->
                        Toast.makeText(this@ManageBookingsActivity, error, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is BookingEvent.BookingApproved -> {
                            Toast.makeText(this@ManageBookingsActivity, "Booking approved", Toast.LENGTH_SHORT).show()
                            viewModel.clearEvent()
                        }
                        is BookingEvent.BookingRejected -> {
                            Toast.makeText(this@ManageBookingsActivity, "Booking rejected", Toast.LENGTH_SHORT).show()
                            viewModel.clearEvent()
                        }
                        is BookingEvent.BookingUpdated -> {
                            Toast.makeText(this@ManageBookingsActivity, "Booking updated", Toast.LENGTH_SHORT).show()
                            viewModel.clearEvent()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun showApproveDialog(booking: Booking) {
        val editText = EditText(this).apply {
            hint = "Add notes (optional)"
        }

        AlertDialog.Builder(this)
            .setTitle("Approve Booking")
            .setMessage("Approve booking for ${booking.carName}?")
            .setView(editText)
            .setPositiveButton("Approve") { _, _ ->
                viewModel.approveBooking(booking.id, editText.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRejectDialog(booking: Booking) {
        val editText = EditText(this).apply {
            hint = "Reason for rejection"
        }

        AlertDialog.Builder(this)
            .setTitle("Reject Booking")
            .setMessage("Reject booking for ${booking.carName}?")
            .setView(editText)
            .setPositiveButton("Reject") { _, _ ->
                val reason = editText.text.toString()
                if (reason.isNotBlank()) {
                    viewModel.rejectBooking(booking.id, reason)
                } else {
                    Toast.makeText(this, "Please provide a reason", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCompleteDialog(booking: Booking) {
        AlertDialog.Builder(this)
            .setTitle("Complete Booking")
            .setMessage("Mark booking for ${booking.carName} as completed?\n\nThis indicates the car has been returned.")
            .setPositiveButton("Complete") { _, _ ->
                viewModel.updateBookingStatus(booking.id, BookingStatus.COMPLETED, "Booking completed - car returned")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

