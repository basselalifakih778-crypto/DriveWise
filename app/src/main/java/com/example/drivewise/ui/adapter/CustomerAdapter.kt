/**
 * CustomerAdapter.kt
 * ====================
 * RecyclerView adapter for displaying customer profiles (admin view).
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. ADMIN-ONLY: This adapter shows client users to admins for verification.
 *
 * 2. COIL TRANSFORMATIONS: CircleCropTransformation() makes profile images circular.
 *    This is a common UI pattern for avatar images.
 *
 * 3. VERIFICATION TOGGLE: The "Verify/Unverify" button changes text and color
 *    based on current verification status.
 *
 * 4. THREE ACTIONS: Each customer has three buttons:
 *    - View Profile: See full profile details
 *    - View Documents: See uploaded license/ID documents
 *    - Verify/Unverify: Toggle verification status
 *
 * USED BY: CustomerProfilesActivity (admin view)
 */
package com.example.drivewise.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation  // Makes images circular
import com.example.drivewise.R
import com.example.drivewise.databinding.ItemCustomerBinding
import com.example.drivewise.domain.model.User

/**
 * Adapter for displaying customer profiles to admins.
 *
 * @param onViewProfileClick Called when View Profile button is clicked
 * @param onViewDocumentsClick Called when View Documents button is clicked
 * @param onToggleVerifyClick Called when Verify/Unverify button is clicked
 */
class CustomerAdapter(
    private val onViewProfileClick: (User) -> Unit,
    private val onViewDocumentsClick: (User) -> Unit,
    private val onToggleVerifyClick: (User) -> Unit
) : ListAdapter<User, CustomerAdapter.CustomerViewHolder>(CustomerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for customer items.
     */
    inner class CustomerViewHolder(
        private val binding: ItemCustomerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds user data to views.
         */
        fun bind(user: User) {
            binding.apply {
                // ─────────────────────────────────────────────────────────────
                // BASIC INFO
                // ─────────────────────────────────────────────────────────────

                // Display name with fallback
                tvName.text = user.fullName.ifEmpty { "No name provided" }
                tvEmail.text = user.email
                tvPhone.text = user.phone.ifEmpty { "No phone" }

                // ─────────────────────────────────────────────────────────────
                // VERIFICATION STATUS CHIP
                // Clear visual indicator of verification status
                // ─────────────────────────────────────────────────────────────

                if (user.isVerified) {
                    chipVerified.text = "✓ Verified"
                    chipVerified.setChipBackgroundColorResource(android.R.color.holo_green_light)
                    chipVerified.setTextColor(itemView.context.getColor(android.R.color.white))
                } else {
                    chipVerified.text = "⚠ Not Verified"
                    chipVerified.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                    chipVerified.setTextColor(itemView.context.getColor(android.R.color.black))
                }

                // ─────────────────────────────────────────────────────────────
                // TOGGLE BUTTON
                // Text and color change based on current status
                // ─────────────────────────────────────────────────────────────

                btnToggleVerify.text = if (user.isVerified) "Unverify" else "Verify"
                btnToggleVerify.setTextColor(
                    itemView.context.getColor(
                        if (user.isVerified) android.R.color.holo_red_dark  // Red for "Unverify"
                        else android.R.color.holo_green_dark  // Green for "Verify"
                    )
                )

                // ─────────────────────────────────────────────────────────────
                // PROFILE IMAGE
                // Uses Coil with CircleCrop transformation for circular avatar
                // ─────────────────────────────────────────────────────────────

                if (user.profileImageUrl.isNotEmpty()) {
                    ivProfile.load(user.profileImageUrl) {
                        crossfade(true)
                        // CircleCropTransformation makes the image circular
                        transformations(CircleCropTransformation())
                        placeholder(R.drawable.circle_background)
                        error(R.drawable.circle_background)
                    }
                } else {
                    ivProfile.setImageResource(R.drawable.circle_background)
                }

                // ─────────────────────────────────────────────────────────────
                // BUTTON CLICK LISTENERS
                // ─────────────────────────────────────────────────────────────

                btnViewProfile.setOnClickListener { onViewProfileClick(user) }
                btnViewDocuments.setOnClickListener { onViewDocumentsClick(user) }
                btnToggleVerify.setOnClickListener { onToggleVerifyClick(user) }
            }
        }
    }

    /**
     * DiffUtil callback - compares users by UID.
     */
    private class CustomerDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
