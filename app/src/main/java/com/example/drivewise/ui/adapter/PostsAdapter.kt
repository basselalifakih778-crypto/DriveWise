/**
 * PostsAdapter.kt
 * =================
 * RecyclerView adapter for displaying promotional/news posts.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. SIMPLE ADAPTER: Only displays posts, no edit/delete buttons.
 *    Clicking anywhere on the item opens the post detail screen.
 *
 * 2. TEXT TRUNCATION: Description is truncated to 120 characters
 *    with take(120) for preview. Full text shown on detail screen.
 *
 * 3. DATE FORMATTING: Timestamps (Long) are converted to readable dates
 *    using SimpleDateFormat.
 *
 * USED BY: PostsListActivity
 */
package com.example.drivewise.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.drivewise.databinding.ItemPostBinding
import com.example.drivewise.domain.model.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * RecyclerView adapter for posts list.
 *
 * @param onPostClick Called when a post item is clicked, passes the Post
 */
class PostsAdapter(
    private val onPostClick: (Post) -> Unit
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding, onPostClick)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for post items.
     *
     * NOTE: This is a regular class, not inner class.
     * We pass the click lambda in the constructor instead.
     */
    class PostViewHolder(
        private val binding: ItemPostBinding,
        private val onPostClick: (Post) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds post data to views.
         */
        fun bind(post: Post) {
            // Display title
            binding.tvTitle.text = post.title

            // Display truncated description (first 120 chars)
            // take() returns first N characters
            binding.tvDescription.text = post.description.take(120)

            // Format and display date
            binding.tvDate.text = formatDate(post.createdAt)

            // Whole item is clickable - opens post detail
            binding.root.setOnClickListener {
                onPostClick(post)
            }
        }

        /**
         * Converts a timestamp to a human-readable date string.
         *
         * @param timestamp Milliseconds since epoch (like System.currentTimeMillis())
         * @return Formatted date like "Jan 15, 2024"
         */
        private fun formatDate(timestamp: Long): String {
            // SimpleDateFormat converts Date objects to formatted strings
            // "MMM dd, yyyy" = "Jan 15, 2024"
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return formatter.format(Date(timestamp))
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}

