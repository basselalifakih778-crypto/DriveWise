package com.example.drivewise.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.drivewise.R
import com.example.drivewise.databinding.ItemCustomerBinding
import com.example.drivewise.domain.model.User

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

    inner class CustomerViewHolder(
        private val binding: ItemCustomerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                tvName.text = user.fullName.ifEmpty { "No name provided" }
                tvEmail.text = user.email
                tvPhone.text = user.phone.ifEmpty { "No phone" }

                // Clear and explicit verification status
                if (user.isVerified) {
                    chipVerified.text = "✓ Verified"
                    chipVerified.setChipBackgroundColorResource(android.R.color.holo_green_light)
                    chipVerified.setTextColor(itemView.context.getColor(android.R.color.white))
                } else {
                    chipVerified.text = "⚠ Not Verified"
                    chipVerified.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                    chipVerified.setTextColor(itemView.context.getColor(android.R.color.black))
                }

                btnToggleVerify.text = if (user.isVerified) "Unverify" else "Verify"
                btnToggleVerify.setTextColor(
                    itemView.context.getColor(
                        if (user.isVerified) android.R.color.holo_red_dark
                        else android.R.color.holo_green_dark
                    )
                )

                if (user.profileImageUrl.isNotEmpty()) {
                    ivProfile.load(user.profileImageUrl) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                        placeholder(R.drawable.circle_background)
                        error(R.drawable.circle_background)
                    }
                } else {
                    ivProfile.setImageResource(R.drawable.circle_background)
                }

                btnViewProfile.setOnClickListener { onViewProfileClick(user) }
                btnViewDocuments.setOnClickListener { onViewDocumentsClick(user) }
                btnToggleVerify.setOnClickListener { onToggleVerifyClick(user) }
            }
        }
    }

    private class CustomerDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}
