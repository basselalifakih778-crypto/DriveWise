/**
 * ClientCarAdapter.kt
 * =====================
 * RecyclerView adapter for displaying cars in the client browse view.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. CLIENT vs ADMIN VIEW: This adapter is simpler than CarAdapter (admin).
 *    Clients can only view and click on cars - no edit/delete buttons.
 *
 * 2. MANUAL VIEW INFLATION: Uses LayoutInflater.inflate() instead of ViewBinding.
 *    Both approaches work - this shows an alternative pattern.
 *
 * 3. FINDVIEWBYID: Traditional way to get view references.
 *    With ViewBinding, you'd use binding.viewName instead.
 *
 * USED BY: BrowseCarsActivity (client view)
 */
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

/**
 * Adapter for displaying available cars to clients.
 *
 * Shows car image, name, specs, price, and features.
 * Clicking opens the car details screen.
 *
 * @param onCarClick Called when a car item is clicked
 */
class ClientCarAdapter(
    private val onCarClick: (Car) -> Unit
) : ListAdapter<Car, ClientCarAdapter.CarViewHolder>(CarDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        // Manual inflation without ViewBinding
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_client_car, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder using manual findViewById instead of ViewBinding.
     *
     * This is the traditional approach. ViewBinding (used in other adapters)
     * is safer and more concise, but both work.
     */
    inner class CarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Get references to views using findViewById
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardCar)
        private val ivCar: ImageView = itemView.findViewById(R.id.ivCarImage)
        private val tvCarName: TextView = itemView.findViewById(R.id.tvCarName)
        private val tvCarSpecs: TextView = itemView.findViewById(R.id.tvCarSpecs)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvFeatures: TextView = itemView.findViewById(R.id.tvFeatures)
        private val btnViewDetails: com.google.android.material.button.MaterialButton =
            itemView.findViewById(R.id.btnViewDetails)

        /**
         * Binds car data to views.
         */
        fun bind(car: Car) {
            // Display car name with year
            tvCarName.text = "${car.brand} ${car.model} (${car.year})"

            // Display specs
            tvCarSpecs.text = "${car.transmission} • ${car.fuel}"

            // Display price formatted nicely
            tvPrice.text = "$${String.format("%.0f", car.dayPrice)}/day"

            // Display first 3 features as a preview
            // take(3) gets first 3 items, joinToString combines with separator
            val featuresText = car.features.take(3).joinToString(" • ")
            tvFeatures.text = if (featuresText.isNotEmpty()) featuresText else "View details"
            tvFeatures.visibility = View.VISIBLE

            // Load car image
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
            // This improves UX - user can click anywhere on the card
            val clickListener = View.OnClickListener { onCarClick(car) }
            cardView.setOnClickListener(clickListener)
            btnViewDetails.setOnClickListener(clickListener)
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    class CarDiffCallback : DiffUtil.ItemCallback<Car>() {
        override fun areItemsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem == newItem
        }
    }
}

