/**
 * BookingAdapter.kt
 * ==================
 * RecyclerView adapter for displaying bookings (admin view).
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. CONDITIONAL UI: Buttons are shown/hidden based on booking status.
 *    - PENDING: Show approve/reject buttons
 *    - APPROVED: Show complete button
 *    - Other states: Hide action buttons
 *
 * 2. STATUS COLORS: The status chip changes color based on booking state.
 *    This provides visual feedback at a glance.
 *
 * 3. PHOTOS INDICATOR: Shows a "View Photos" button only when photos exist.
 *
 * USED BY: ManageBookingsActivity (admin view)
 */
package com.example.drivewise.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.drivewise.databinding.ItemBookingBinding
import com.example.drivewise.domain.model.Booking
import com.example.drivewise.domain.model.BookingStatus

/**
 * RecyclerView adapter for bookings in the admin management screen.
 *
 * @param onApproveClick Called when Approve button is clicked
 * @param onRejectClick Called when Reject button is clicked
 * @param onViewPhotosClick Called when View Photos button is clicked
 * @param onCompleteClick Called when Complete button is clicked
 */
class BookingAdapter(
    private val onApproveClick: (Booking) -> Unit,
    private val onRejectClick: (Booking) -> Unit,
    private val onViewPhotosClick: (Booking) -> Unit,
    private val onCompleteClick: (Booking) -> Unit
) : ListAdapter<Booking, BookingAdapter.BookingViewHolder>(BookingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for booking items.
     */
    inner class BookingViewHolder(
        private val binding: ItemBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds booking data to views.
         *
         * Key logic:
         * - Display booking info (ID, car, customer, dates, price)
         * - Color-code the status chip
         * - Show/hide buttons based on current status
         */
        fun bind(booking: Booking) {
            binding.apply {
                // ─────────────────────────────────────────────────────────────
                // DISPLAY BOOKING INFO
                // ─────────────────────────────────────────────────────────────

                // Show last 6 chars of ID for brevity
                tvBookingId.text = "Booking #${booking.id.takeLast(6)}"

                // Show car name, or fallback to car ID if name is empty
                tvCarName.text = booking.carName.ifEmpty { "Car ID: ${booking.carId.takeLast(6)}" }

                // Customer info
                tvCustomerName.text = "Customer: ${booking.userName.ifEmpty { "Unknown" }}"
                tvCustomerEmail.text = booking.userEmail

                // Dates and price
                tvStartDate.text = booking.startDate
                tvEndDate.text = booking.endDate
                tvTotalPrice.text = "$${booking.totalPrice}"

                // ─────────────────────────────────────────────────────────────
                // STATUS CHIP (color-coded)
                // ─────────────────────────────────────────────────────────────

                val status = booking.getStatusEnum()
                chipStatus.text = status.name

                // Set chip background color based on status
                chipStatus.setChipBackgroundColorResource(
                    when (status) {
                        BookingStatus.PENDING -> android.R.color.holo_orange_light   // Yellow/orange
                        BookingStatus.APPROVED -> android.R.color.holo_green_light   // Green
                        BookingStatus.REJECTED -> android.R.color.holo_red_light     // Red
                        BookingStatus.CANCELLED -> android.R.color.darker_gray       // Gray
                        BookingStatus.COMPLETED -> android.R.color.holo_blue_light   // Blue
                    }
                )

                // ─────────────────────────────────────────────────────────────
                // CONDITIONAL ACTION BUTTONS
                // ─────────────────────────────────────────────────────────────

                // Show different buttons based on booking status
                when (status) {
                    BookingStatus.PENDING -> {
                        // Pending bookings can be approved or rejected
                        layoutActions.visibility = View.VISIBLE
                        btnApprove.visibility = View.VISIBLE
                        btnReject.visibility = View.VISIBLE
                        btnComplete.visibility = View.GONE
                    }
                    BookingStatus.APPROVED -> {
                        // Approved bookings can be marked complete
                        layoutActions.visibility = View.GONE
                        btnApprove.visibility = View.GONE
                        btnReject.visibility = View.GONE
                        btnComplete.visibility = View.VISIBLE
                    }
                    else -> {
                        // Other statuses (rejected, cancelled, completed) - no actions
                        layoutActions.visibility = View.GONE
                        btnApprove.visibility = View.GONE
                        btnReject.visibility = View.GONE
                        btnComplete.visibility = View.GONE
                    }
                }

                // ─────────────────────────────────────────────────────────────
                // PHOTOS BUTTON
                // ─────────────────────────────────────────────────────────────

                // Only show photos button if there are photos
                val hasPhotos = booking.pickupPhotos.isNotEmpty() || booking.returnPhotos.isNotEmpty()
                
                if (hasPhotos) {
                    btnViewPhotos.visibility = View.VISIBLE
                    // Show count of photos
                    btnViewPhotos.text = "View Photos (${booking.pickupPhotos.size + booking.returnPhotos.size})"
                    btnViewPhotos.isEnabled = true
                } else {
                    btnViewPhotos.visibility = View.GONE
                }

                // ─────────────────────────────────────────────────────────────
                // CLICK LISTENERS
                // ─────────────────────────────────────────────────────────────

                btnApprove.setOnClickListener { onApproveClick(booking) }
                btnReject.setOnClickListener { onRejectClick(booking) }
                btnViewPhotos.setOnClickListener { onViewPhotosClick(booking) }
                btnComplete.setOnClickListener { onCompleteClick(booking) }
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    private class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem == newItem
        }
    }
}

