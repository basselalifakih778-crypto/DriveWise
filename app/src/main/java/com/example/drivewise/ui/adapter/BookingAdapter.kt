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

    inner class BookingViewHolder(
        private val binding: ItemBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.apply {
                tvBookingId.text = "Booking #${booking.id.takeLast(6)}"
                tvCarName.text = booking.carName.ifEmpty { "Car ID: ${booking.carId.takeLast(6)}" }
                tvCustomerName.text = "Customer: ${booking.userName.ifEmpty { "Unknown" }}"
                tvCustomerEmail.text = booking.userEmail
                tvStartDate.text = booking.startDate
                tvEndDate.text = booking.endDate
                tvTotalPrice.text = "$${booking.totalPrice}"

                val status = booking.getStatusEnum()
                chipStatus.text = status.name
                chipStatus.setChipBackgroundColorResource(
                    when (status) {
                        BookingStatus.PENDING -> android.R.color.holo_orange_light
                        BookingStatus.APPROVED -> android.R.color.holo_green_light
                        BookingStatus.REJECTED -> android.R.color.holo_red_light
                        BookingStatus.CANCELLED -> android.R.color.darker_gray
                        BookingStatus.COMPLETED -> android.R.color.holo_blue_light
                    }
                )

                // Show/hide action buttons based on status
                when (status) {
                    BookingStatus.PENDING -> {
                        layoutActions.visibility = View.VISIBLE
                        btnApprove.visibility = View.VISIBLE
                        btnReject.visibility = View.VISIBLE
                        btnComplete.visibility = View.GONE
                    }
                    BookingStatus.APPROVED -> {
                        layoutActions.visibility = View.GONE
                        btnApprove.visibility = View.GONE
                        btnReject.visibility = View.GONE
                        btnComplete.visibility = View.VISIBLE
                    }
                    else -> {
                        layoutActions.visibility = View.GONE
                        btnApprove.visibility = View.GONE
                        btnReject.visibility = View.GONE
                        btnComplete.visibility = View.GONE
                    }
                }

                // Photos button - always visible when there are photos
                val hasPhotos = booking.pickupPhotos.isNotEmpty() || booking.returnPhotos.isNotEmpty()
                
                if (hasPhotos) {
                    btnViewPhotos.visibility = View.VISIBLE
                    btnViewPhotos.text = "View Photos (${booking.pickupPhotos.size + booking.returnPhotos.size})"
                    btnViewPhotos.isEnabled = true
                } else {
                    btnViewPhotos.visibility = View.GONE
                }

                btnApprove.setOnClickListener { onApproveClick(booking) }
                btnReject.setOnClickListener { onRejectClick(booking) }
                btnViewPhotos.setOnClickListener { onViewPhotosClick(booking) }
                btnComplete.setOnClickListener { onCompleteClick(booking) }
            }
        }
    }

    private class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem == newItem
        }
    }
}

