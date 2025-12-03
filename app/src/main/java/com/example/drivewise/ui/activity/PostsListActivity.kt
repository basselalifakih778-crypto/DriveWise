package com.example.drivewise.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.drivewise.databinding.ActivityPostsListBinding
import com.example.drivewise.ui.adapter.PostsAdapter
import com.example.drivewise.ui.state.PostsUiState
import com.example.drivewise.ui.viewmodel.PostsViewModel
import com.example.drivewise.Data.remote.FirebasePostsRepository
import kotlinx.coroutines.launch

class PostsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostsListBinding
    private val viewModel: PostsViewModel by viewModels {
        PostsViewModelFactory(
            FirebasePostsRepository(
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            )
        )
    }
    private lateinit var adapter: PostsAdapter
    private var showCreateButton = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showCreateButton = intent.getBooleanExtra("SHOW_CREATE_BUTTON", false)

        setupViews()
        observeState()
    }

    private fun setupViews() {
        setSupportActionBar(binding.toolbar)

        adapter = PostsAdapter { post ->
            val intent = Intent(this, PostDetailsActivity::class.java)
            intent.putExtra("POST_ID", post.id)
            startActivity(intent)
        }

        binding.rvPosts.layoutManager = LinearLayoutManager(this)
        binding.rvPosts.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            // Posts are observed automatically, so just stop refreshing
            binding.swipeRefresh.isRefreshing = false
        }

        if (showCreateButton) {
            binding.fabCreatePost.visibility = View.VISIBLE
            binding.fabCreatePost.setOnClickListener {
                startActivity(Intent(this, CreatePostActivity::class.java))
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.postsState.collect { state ->
                    when (state) {
                        is PostsUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvPosts.visibility = View.GONE
                            binding.tvEmpty.visibility = View.GONE
                            binding.tvError.visibility = View.GONE
                        }
                        is PostsUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvPosts.visibility = View.VISIBLE
                            binding.tvEmpty.visibility = View.GONE
                            binding.tvError.visibility = View.GONE
                            adapter.submitList(state.posts)
                        }
                        is PostsUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvPosts.visibility = View.GONE
                            binding.tvEmpty.visibility = View.GONE
                            binding.tvError.visibility = View.VISIBLE
                            binding.tvError.text = state.message
                        }
                    }
                }
            }
        }
    }
}

