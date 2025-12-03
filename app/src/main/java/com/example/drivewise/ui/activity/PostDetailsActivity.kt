package com.example.drivewise.ui.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.drivewise.databinding.ActivityPostDetailsBinding
import com.example.drivewise.ui.state.PostsUiState
import com.example.drivewise.ui.viewmodel.PostsViewModel
import com.example.drivewise.Data.remote.FirebasePostsRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailsBinding
    private val viewModel: PostsViewModel by viewModels {
        PostsViewModelFactory(
            FirebasePostsRepository(
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            )
        )
    }
    private lateinit var postId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postId = intent.getStringExtra("POST_ID") ?: run {
            finish()
            return
        }

        setupViews()
        observeState()
    }

    private fun setupViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.postsState.collect { state ->
                    when (state) {
                        is PostsUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.tvError.visibility = View.GONE
                        }
                        is PostsUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvError.visibility = View.GONE

                            val post = state.posts.find { it.id == postId }
                            if (post == null) {
                                binding.tvError.text = "Post not found"
                                binding.tvError.visibility = View.VISIBLE
                            } else {
                                binding.tvTitle.text = post.title
                                binding.tvDate.text = formatDate(post.createdAt)
                                binding.tvDescription.text = post.description

                                if (!post.imageUrl.isNullOrEmpty()) {
                                    binding.ivPostImage.visibility = View.VISIBLE
                                    binding.ivPostImage.load(post.imageUrl) {
                                        crossfade(true)
                                        placeholder(android.R.drawable.ic_menu_gallery)
                                        error(android.R.drawable.ic_menu_gallery)
                                    }
                                } else {
                                    binding.ivPostImage.visibility = View.GONE
                                }
                            }
                        }
                        is PostsUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvError.visibility = View.VISIBLE
                            binding.tvError.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}

