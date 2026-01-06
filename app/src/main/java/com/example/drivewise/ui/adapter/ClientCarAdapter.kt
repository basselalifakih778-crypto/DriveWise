package com.example.drivewise.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.drivewise.R
import com.example.drivewise.domain.model.Car
import com.google.android.material.card.MaterialCardView

class ClientCarAdapter(
    private val onCarClick: (Car) -> Unit
) : ListAdapter<Car, ClientCarAdapter.CarViewHolder>(CarDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client_car, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardCar)
        private val ivCar: ImageView = itemView.findViewById(R.id.ivCarImage)
        private val tvCarName: TextView = itemView.findViewById(R.id.tvCarName)
        private val tvCarSpecs: TextView = itemView.findViewById(R.id.tvCarSpecs)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvFeatures: TextView = itemView.findViewById(R.id.tvFeatures)
        private val btnViewDetails: com.google.android.material.button.MaterialButton =
            itemView.findViewById(R.id.btnViewDetails)

        fun bind(car: Car) {
            tvCarName.text = "${car.brand} ${car.model} (${car.year})"
            tvCarSpecs.text = "${car.transmission} • ${car.fuel}"
            tvPrice.text = "$${String.format("%.0f", car.dayPrice)}/day"

            val featuresText = car.features.take(3).joinToString(" • ")
            tvFeatures.text = if (featuresText.isNotEmpty()) featuresText else "View details"
            tvFeatures.visibility = View.VISIBLE

            if (car.imageUrl.isNotEmpty()) {
                ivCar.load(car.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_car_placeholder)
                    error(R.drawable.ic_car_placeholder)
                }
            } else {
                ivCar.setImageResource(R.drawable.ic_car_placeholder)
            }

            // Click listeners - both card and button should work
            val clickListener = View.OnClickListener { onCarClick(car) }
            cardView.setOnClickListener(clickListener)
            btnViewDetails.setOnClickListener(clickListener)
        }
    }

    class CarDiffCallback : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem == newItem
        }
    }
}

