/**
 * PhotoAdapter.kt
 * =================
 * RecyclerView adapter for displaying a grid of photos (view-only).
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. STRING LIST: Unlike other adapters that use data classes,
 *    this adapter just displays a list of URL strings.
 *
 * 2. VIEW-ONLY: No edit/delete buttons - just displays photos.
 *    Clicking a photo calls onPhotoClick (could open fullscreen view).
 *
 * 3. GRID LAYOUT: This adapter is typically used with GridLayoutManager
 *    to show photos in a grid (e.g., 3 columns).
 *
 * USED BY: CarPhotosActivity (admin view to see booking photos)
 */
package com.example.drivewise.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.drivewise.R
import com.example.drivewise.databinding.ItemPhotoBinding

/**
 * Adapter for displaying a grid of photos.
 *
 * @param onPhotoClick Called when a photo is clicked, passes the URL
 */
class PhotoAdapter(
    private val onPhotoClick: (String) -> Unit
) : ListAdapter<String, PhotoAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for photo items.
     */
    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds a photo URL to the ImageView.
         *
         * @param photoUrl The URL of the photo to display
         */
        fun bind(photoUrl: String) {
            // Load image using Coil
            binding.ivPhoto.load(photoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_launcher_background)
                error(R.drawable.ic_launcher_background)
            }
            // Click listener for fullscreen or other action
            binding.root.setOnClickListener { onPhotoClick(photoUrl) }
        }
    }

    /**
     * DiffUtil callback - compares URLs directly.
     * Since items are just strings, comparison is straightforward.
     */
    private class PhotoDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}

