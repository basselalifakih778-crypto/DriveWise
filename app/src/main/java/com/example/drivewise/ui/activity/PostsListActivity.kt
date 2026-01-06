/**
 * PostsListActivity.kt
 * ======================
 * This Activity displays a list of promotional/news posts.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. SHARED SCREEN: Both admins and clients can view posts.
 *    The difference is admins see the "Create Post" FAB (floating action button).
 *
 * 2. SWIPEREFRESHLAYOUT: Pull-down-to-refresh UI pattern.
 *    Although posts are already real-time, this gives users a familiar refresh gesture.
 *
 * 3. SEALED STATE HANDLING: PostsUiState can be Loading, Success, or Error.
 *    We handle all three cases in the 'when' statement.
 *
 * 4. EXTRA INTENT: SHOW_CREATE_BUTTON extra controls FAB visibility.
 *    AdminHomeActivity passes true, ClientHomeActivity passes false.
 *
 * FLOW:
 * 1. Check intent for SHOW_CREATE_BUTTON flag
 * 2. Set up RecyclerView with PostsAdapter
 * 3. Observe postsState from ViewModel
 * 4. Handle Loading/Success/Error states
 * 5. Click on post → PostDetailsActivity
 * 6. Click FAB (admin only) → CreatePostActivity
 */
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

/**
 * Displays a list of posts for both admins and clients.
 *
 * Admins can create new posts via FAB.
 * Clicking any post opens PostDetailsActivity.
 */
class PostsListActivity : AppCompatActivity() {

    /** ViewBinding for activity_posts_list.xml */
    private lateinit var binding: ActivityPostsListBinding

    /** ViewModel for posts - created with factory */
    private val viewModel: PostsViewModel by viewModels {
        PostsViewModelFactory(
            FirebasePostsRepository(
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            )
        )
    }

    /** Adapter for the posts RecyclerView */
    private lateinit var adapter: PostsAdapter

    /** Whether to show the create post FAB (true for admin, false for client) */
    private var showCreateButton = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the flag from intent - controls whether FAB is shown
        showCreateButton = intent.getBooleanExtra("SHOW_CREATE_BUTTON", false)

        setupViews()
        observeState()
    }

    /**
     * Sets up the UI components.
     */
    private fun setupViews() {
        // Set up toolbar
        setSupportActionBar(binding.toolbar)

        // Create adapter with click handler
        adapter = PostsAdapter { post ->
            // When post is clicked, open details screen
            val intent = Intent(this, PostDetailsActivity::class.java)
            intent.putExtra("POST_ID", post.id)  // Pass post ID to detail screen
            startActivity(intent)
        }

        // Set up RecyclerView
        binding.rvPosts.layoutManager = LinearLayoutManager(this)
        binding.rvPosts.adapter = adapter

        // Set up pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener {
            // Posts are observed automatically via Flow, so just stop the animation
            // In a real app, you might want to force a refresh here
            binding.swipeRefresh.isRefreshing = false
        }

        // Show/hide FAB based on intent flag (admin vs client)
        if (showCreateButton) {
            binding.fabCreatePost.visibility = View.VISIBLE
            binding.fabCreatePost.setOnClickListener {
                startActivity(Intent(this, CreatePostActivity::class.java))
            }
        }
    }

    /**
     * Observes the posts state from ViewModel.
     */
    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.postsState.collect { state ->
                    // Handle all three possible states using 'when'
                    when (state) {
                        // ─────────────────────────────────────────────────────
                        // LOADING STATE
                        // Show spinner, hide everything else
                        // ─────────────────────────────────────────────────────
                        is PostsUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvPosts.visibility = View.GONE
                            binding.tvEmpty.visibility = View.GONE
                            binding.tvError.visibility = View.GONE
                        }

                        // ─────────────────────────────────────────────────────
                        // SUCCESS STATE
                        // Show posts list, hide others
                        // ─────────────────────────────────────────────────────
                        is PostsUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvPosts.visibility = View.VISIBLE
                            binding.tvEmpty.visibility = View.GONE
                            binding.tvError.visibility = View.GONE
                            // Submit the list to adapter for display
                            adapter.submitList(state.posts)
                        }

                        // ─────────────────────────────────────────────────────
                        // ERROR STATE
                        // Show error message, hide others
                        // ─────────────────────────────────────────────────────
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

