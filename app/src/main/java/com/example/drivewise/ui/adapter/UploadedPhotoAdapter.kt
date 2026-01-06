/**
 * UploadedPhotoAdapter.kt
 * =========================
 * RecyclerView adapter for displaying uploaded photos with remove button.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. EDITABLE LIST: Unlike PhotoAdapter (view-only), this adapter
 *    has a remove button on each photo.
 *
 * 2. USE CASE: When client is uploading car condition photos,
 *    they can see their uploads and remove any they don't want.
 *
 * 3. CALLBACK PATTERN: The Activity handles the actual removal.
 *    The adapter just reports "user wants to remove this URL".
 *
 * USED BY: UploadPhotosActivity (client uploading pickup/return photos)
 */
package com.example.drivewise.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.drivewise.R

/**
 * Adapter for displaying uploaded photos with a remove option.
 *
 * @param onRemoveClick Called when the remove button is clicked, passes the photo URL
 */
class UploadedPhotoAdapter(
    private val onRemoveClick: (String) -> Unit
) : ListAdapter<String, UploadedPhotoAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_uploaded_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for uploaded photo items.
     */
    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Manual view references
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        /**
         * Binds a photo URL to views.
         *
         * @param photoUrl The URL of the uploaded photo
         */
        fun bind(photoUrl: String) {
            // Load the photo using Coil
            ivPhoto.load(photoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_car_placeholder)
                error(R.drawable.ic_car_placeholder)
            }

            // Remove button click - notify Activity to remove this photo
            btnRemove.setOnClickListener {
                onRemoveClick(photoUrl)
            }
        }
    }

    /**
     * DiffUtil callback for URL comparison.
     */
    class PhotoDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}

