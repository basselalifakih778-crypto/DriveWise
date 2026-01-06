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

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardBooking)
        private val tvCarName: TextView = itemView.findViewById(R.id.tvCarName)
        private val tvDates: TextView = itemView.findViewById(R.id.tvDates)
        private val tvTotalPrice: TextView = itemView.findViewById(R.id.tvTotalPrice)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvAdminNotes: TextView = itemView.findViewById(R.id.tvAdminNotes)
        private val btnUploadPhotos: MaterialButton = itemView.findViewById(R.id.btnUploadPhotos)

        fun bind(booking: Booking) {
            tvCarName.text = booking.carName
            tvDates.text = "${booking.startDate} - ${booking.endDate}"
            tvTotalPrice.text = "$${String.format("%.2f", booking.totalPrice)}"

            // Status with emoji
            val (statusText, statusColor) = when (booking.getStatusEnum()) {
                BookingStatus.PENDING -> "⏳ Pending" to android.R.color.holo_orange_dark
                BookingStatus.APPROVED -> "✅ Approved" to android.R.color.holo_green_dark
                BookingStatus.REJECTED -> "❌ Rejected" to android.R.color.holo_red_dark
                BookingStatus.CANCELLED -> "🚫 Cancelled" to android.R.color.darker_gray
                BookingStatus.COMPLETED -> "🎉 Completed" to android.R.color.holo_blue_dark
            }
            tvStatus.text = statusText
            tvStatus.setTextColor(itemView.context.getColor(statusColor))

            // Admin notes
            if (booking.adminNotes.isNotEmpty()) {
                tvAdminNotes.visibility = View.VISIBLE
                tvAdminNotes.text = "Note: ${booking.adminNotes}"
            } else {
                tvAdminNotes.visibility = View.GONE
            }

            // Show upload photos button only for approved bookings
            if (booking.getStatusEnum() == BookingStatus.APPROVED) {
                btnUploadPhotos.visibility = View.VISIBLE
                btnUploadPhotos.setOnClickListener {
                    onUploadPhotosClick(booking)
                }
            } else {
                btnUploadPhotos.visibility = View.GONE
            }

            cardView.setOnClickListener {
                onBookingClick(booking)
            }
        }
    }

    class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem == newItem
        }
    }
}

