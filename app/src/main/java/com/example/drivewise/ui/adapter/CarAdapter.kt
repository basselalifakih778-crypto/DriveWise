/**
 * CarAdapter.kt
 * ===============
 * RecyclerView adapter for displaying the list of cars (admin view).
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. RECYCLERVIEW ADAPTER: RecyclerView is Android's way to display lists efficiently.
 *    The adapter is the "bridge" between your data (List<Car>) and the views (XML items).
 *
 * 2. LISTADAPTER: A smart adapter that uses DiffUtil to calculate differences between
 *    old and new lists. This means only changed items are redrawn, not the whole list.
 *
 * 3. VIEWHOLDER PATTERN: Each list item has a ViewHolder that holds references to its views.
 *    This avoids expensive findViewById() calls when scrolling.
 *
 * 4. LAMBDA CALLBACKS: Instead of interfaces, we use Kotlin lambdas for click handling.
 *    The Activity passes in what to do when buttons are clicked.
 *
 * 5. COIL: An image loading library. Handles downloading, caching, and displaying images
 *    from URLs. Much simpler than doing it manually.
 *
 * FLOW:
 * Activity creates adapter with callbacks → Adapter renders each car →
 * User clicks button → Callback runs in Activity → Activity tells ViewModel
 */
package com.example.drivewise.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load  // Coil image loading extension
import com.example.drivewise.R
import com.example.drivewise.databinding.ItemCarBinding  // Auto-generated from item_car.xml
import com.example.drivewise.domain.model.Car

/**
 * RecyclerView adapter for cars in the admin management screen.
 *
 * Displays car info with edit, delete, and availability toggle buttons.
 *
 * @param onEditClick Called when Edit button is clicked, passes the Car
 * @param onDeleteClick Called when Delete button is clicked, passes the Car
 * @param onToggleAvailability Called when availability toggle is clicked, passes the Car
 */
class CarAdapter(
    private val onEditClick: (Car) -> Unit,           // Lambda for edit button
    private val onDeleteClick: (Car) -> Unit,         // Lambda for delete button
    private val onToggleAvailability: (Car) -> Unit   // Lambda for availability toggle
) : ListAdapter<Car, CarAdapter.CarViewHolder>(CarDiffCallback()) {
    // ListAdapter<DataType, ViewHolderType>(DiffCallback)

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE VIEWHOLDER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called when RecyclerView needs a new ViewHolder (when scrolling to new items).
     *
     * This inflates the XML layout and creates a ViewHolder to hold the views.
     * RecyclerView reuses ViewHolders, so this isn't called for every item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {
        // Inflate the item_car.xml layout using ViewBinding
        val binding = ItemCarBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false  // Don't attach to parent yet - RecyclerView will do this
        )
        return CarViewHolder(binding)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BIND VIEWHOLDER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called to display data at the specified position.
     *
     * @param holder The ViewHolder to update
     * @param position The position in the list
     */
    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {
        // getItem() is provided by ListAdapter - gets item at position
        holder.bind(getItem(position))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VIEWHOLDER CLASS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * ViewHolder that holds references to views in item_car.xml.
     *
     * INNER CLASS: Has access to outer class (CarAdapter) and its lambdas.
     */
    inner class CarViewHolder(
        private val binding: ItemCarBinding
    ) : RecyclerView.ViewHolder(binding.root) {  // root = the entire item view

        /**
         * Binds a Car object to the views.
         *
         * @param car The car data to display
         */
        fun bind(car: Car) {
            // APPLY BLOCK: Allows calling multiple methods on 'binding' without repeating it
            binding.apply {
                // Display car name (brand + model)
                tvCarName.text = "${car.brand} ${car.model}"

                // Display car details
                tvCarDetails.text = "${car.year} • ${car.transmission} • ${car.fuel}"

                // Display price
                tvCarPrice.text = "$${car.dayPrice}/day"

                // Set availability chip text and color
                chipAvailability.text = if (car.available) "Available" else "Unavailable"
                chipAvailability.setChipBackgroundColorResource(
                    if (car.available) android.R.color.holo_green_light
                    else android.R.color.holo_red_light
                )

                // Load car image using Coil
                if (car.imageUrl.isNotEmpty()) {
                    ivCarImage.load(car.imageUrl) {
                        crossfade(true)  // Smooth fade-in animation
                        placeholder(R.drawable.ic_launcher_background)  // Show while loading
                        error(R.drawable.ic_launcher_background)  // Show if load fails
                    }
                } else {
                    // No image URL - show placeholder
                    ivCarImage.setImageResource(R.drawable.ic_launcher_background)
                }

                // Set up button click listeners - call the lambdas from constructor
                btnEdit.setOnClickListener { onEditClick(car) }
                btnDelete.setOnClickListener { onDeleteClick(car) }

                // Toggle button shows opposite of current state
                btnToggleAvailability.text = if (car.available) "Set Unavailable" else "Set Available"
                btnToggleAvailability.setOnClickListener { onToggleAvailability(car) }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIFF CALLBACK
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * DiffUtil.ItemCallback tells ListAdapter how to compare items.
     *
     * This enables efficient list updates - only changed items are redrawn.
     */
    private class CarDiffCallback : DiffUtil.ItemCallback<Car>() {

        /**
         * Are these the same item? (Check by ID)
         * Used to detect if item was moved in the list.
         */
        override fun areItemsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Do these items have the same content?
         * If false, the item view will be rebound with new data.
         * Data classes automatically implement equals(), so this works.
         */
        override fun areContentsTheSame(oldItem: Car, newItem: Car): Boolean {
            return oldItem == newItem
        }
    }
}

