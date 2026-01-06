package com.example.drivewise.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.drivewise.R
import com.example.drivewise.databinding.ItemCarBinding
import com.example.drivewise.domain.model.Car

class CarAdapter(
    private val onEditClick: (Car) -> Unit,
    private val onDeleteClick: (Car) -> Unit,
    private val onToggleAvailability: (Car) -> Unit
) : ListAdapter<Car, CarAdapter.CarViewHolder>(CarDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val binding = ItemCarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CarViewHolder(
        private val binding: ItemCarBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(car: Car) {
            binding.apply {
                tvCarName.text = "${car.brand} ${car.model}"
                tvCarDetails.text = "${car.year} • ${car.transmission} • ${car.fuel}"
                tvCarPrice.text = "$${car.dayPrice}/day"

                chipAvailability.text = if (car.available) "Available" else "Unavailable"
                chipAvailability.setChipBackgroundColorResource(
                    if (car.available) android.R.color.holo_green_light
                    else android.R.color.holo_red_light
                )

                if (car.imageUrl.isNotEmpty()) {
                    ivCarImage.load(car.imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_launcher_background)
                        error(R.drawable.ic_launcher_background)
                    }
                } else {
                    ivCarImage.setImageResource(R.drawable.ic_launcher_background)
                }

                btnEdit.setOnClickListener { onEditClick(car) }
                btnDelete.setOnClickListener { onDeleteClick(car) }
                btnToggleAvailability.text = if (car.available) "Set Unavailable" else "Set Available"
                btnToggleAvailability.setOnClickListener { onToggleAvailability(car) }
            }
        }
    }

    private class CarDiffCallback : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem == newItem
        }
    }
}

