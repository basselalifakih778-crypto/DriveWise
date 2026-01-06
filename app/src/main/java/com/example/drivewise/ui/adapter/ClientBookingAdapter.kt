/**
 * ClientBookingAdapter.kt
 * =========================
 * RecyclerView adapter for displaying bookings from the client's perspective.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. CLIENT VIEW: Shows bookings made BY the client (not all bookings like admin).
 *    Different actions available - upload photos instead of approve/reject.
 *
 * 2. STATUS WITH EMOJIS: Uses emoji icons for quick visual status recognition.
 *    ⏳ Pending, ✅ Approved, ❌ Rejected, 🚫 Cancelled, 🎉 Completed
 *
 * 3. CONDITIONAL UI: Upload Photos button only shows for approved bookings
 *    (client can only upload photos after booking is approved).
 *
 * USED BY: MyBookingsActivity (client view)
 */
package com.example.drivewise.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.drivewise.R
import com.example.drivewise.domain.model.Booking
import com.example.drivewise.domain.model.BookingStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Adapter for displaying client's own bookings.
 *
 * @param onBookingClick Called when a booking item is clicked
 * @param onUploadPhotosClick Called when Upload Photos button is clicked
 */
class ClientBookingAdapter(
    private val onBookingClick: (Booking) -> Unit,
    private val onUploadPhotosClick: (Booking) -> Unit
) : ListAdapter<Booking, ClientBookingAdapter.BookingViewHolder>(BookingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for client booking items.
     */
    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardBooking)
        private val tvCarName: TextView = itemView.findViewById(R.id.tvCarName)
        private val tvDates: TextView = itemView.findViewById(R.id.tvDates)
        private val tvTotalPrice: TextView = itemView.findViewById(R.id.tvTotalPrice)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvAdminNotes: TextView = itemView.findViewById(R.id.tvAdminNotes)
        private val btnUploadPhotos: MaterialButton = itemView.findViewById(R.id.btnUploadPhotos)

        /**
         * Binds booking data to views.
         */
        fun bind(booking: Booking) {
            tvCarName.text = booking.carName
            tvDates.text = "${booking.startDate} - ${booking.endDate}"
            tvTotalPrice.text = "$${String.format("%.2f", booking.totalPrice)}"

            // ─────────────────────────────────────────────────────────────────
            // STATUS WITH EMOJI
            // Makes status easy to recognize at a glance
            // ─────────────────────────────────────────────────────────────────

            // Pair of (displayText, colorResourceId)
            val (statusText, statusColor) = when (booking.getStatusEnum()) {
                BookingStatus.PENDING -> "⏳ Pending" to android.R.color.holo_orange_dark
                BookingStatus.APPROVED -> "✅ Approved" to android.R.color.holo_green_dark
                BookingStatus.REJECTED -> "❌ Rejected" to android.R.color.holo_red_dark
                BookingStatus.CANCELLED -> "🚫 Cancelled" to android.R.color.darker_gray
                BookingStatus.COMPLETED -> "🎉 Completed" to android.R.color.holo_blue_dark
            }
            tvStatus.text = statusText
            tvStatus.setTextColor(itemView.context.getColor(statusColor))

            // ─────────────────────────────────────────────────────────────────
            // ADMIN NOTES
            // Show rejection reason or other notes from admin
            // ─────────────────────────────────────────────────────────────────

            if (booking.adminNotes.isNotEmpty()) {
                tvAdminNotes.visibility = View.VISIBLE
                tvAdminNotes.text = "Note: ${booking.adminNotes}"
            } else {
                tvAdminNotes.visibility = View.GONE
            }

            // ─────────────────────────────────────────────────────────────────
            // UPLOAD PHOTOS BUTTON
            // Only show for approved bookings (client can document car condition)
            // ─────────────────────────────────────────────────────────────────

            if (booking.getStatusEnum() == BookingStatus.APPROVED) {
                btnUploadPhotos.visibility = View.VISIBLE
                btnUploadPhotos.setOnClickListener {
                    onUploadPhotosClick(booking)
                }
            } else {
                btnUploadPhotos.visibility = View.GONE
            }

            // Card click opens booking details
            cardView.setOnClickListener {
                onBookingClick(booking)
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem == newItem
        }
    }
}

