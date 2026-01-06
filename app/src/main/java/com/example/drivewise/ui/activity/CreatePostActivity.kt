/**
 * CreatePostActivity.kt
 * =======================
 * This Activity allows admins to create new promotional/news posts.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. ADMIN-ONLY: This Activity should only be accessible to admins.
 *    The FAB that launches this is only visible on admin's posts screen.
 *
 * 2. FORM PATTERN: User fills form → clicks Submit → ViewModel creates post
 *    → On success, form resets and Activity closes.
 *
 * 3. TOOLBAR NAVIGATION: Uses supportActionBar with up navigation.
 *    The up button (←) finishes this Activity and returns to previous screen.
 *
 * 4. FIREBASE AUTH UID: We get the current admin's UID to store as createdBy.
 *    This tracks who created each post.
 *
 * FLOW:
 * 1. Admin fills in title, description, optional image URL
 * 2. Click Submit → viewModel.createPost(adminId)
 * 3. ViewModel validates → calls repository
 * 4. On success: form resets, Toast shown, Activity finishes
 * 5. On error: error message displayed
 */
package com.example.drivewise.ui.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.drivewise.databinding.ActivityCreatePostBinding
import com.example.drivewise.ui.viewmodel.PostsViewModel
import com.example.drivewise.Data.remote.FirebasePostsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * Admin screen for creating new posts.
 *
 * Collects title, description, and optional image URL.
 * On submit, creates post in Firestore and closes.
 */
class CreatePostActivity : AppCompatActivity() {

    /** ViewBinding for activity_create_post.xml */
    private lateinit var binding: ActivityCreatePostBinding

    /** ViewModel for post creation */
    private val viewModel: PostsViewModel by viewModels {
        PostsViewModelFactory(
            FirebasePostsRepository(
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeState()
    }

    /**
     * Sets up UI components and listeners.
     */
    private fun setupViews() {
        // Set up toolbar with back navigation
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)  // Show ← back arrow

        // ─────────────────────────────────────────────────────────────────────
        // TEXT INPUT LISTENERS
        // Update ViewModel state as admin types
        // ─────────────────────────────────────────────────────────────────────

        binding.etTitle.addTextChangedListener {
            viewModel.onTitleChanged(it?.toString() ?: "")
        }

        binding.etDescription.addTextChangedListener {
            viewModel.onDescriptionChanged(it?.toString() ?: "")
        }

        binding.etImageUrl.addTextChangedListener {
            viewModel.onImageUrlChanged(it?.toString() ?: "")
        }

        // ─────────────────────────────────────────────────────────────────────
        // SUBMIT BUTTON
        // ─────────────────────────────────────────────────────────────────────

        binding.btnSubmit.setOnClickListener {
            // Get current admin's UID from Firebase Auth
            val adminId = FirebaseAuth.getInstance().currentUser?.uid

            if (adminId != null) {
                // Create the post with admin ID
                viewModel.createPost(adminId)
            } else {
                // Shouldn't happen if user is logged in, but handle gracefully
                Toast.makeText(
                    this,
                    "You must be logged in to create a post",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Handles toolbar menu item clicks.
     * Specifically, the up/back button.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Up button pressed - close this Activity
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Observes form state from ViewModel.
     */
    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.formState.collect { state ->
                    // Show/hide loading spinner
                    binding.progressBar.visibility = if (state.isSubmitting) View.VISIBLE else View.GONE

                    // Disable submit button while submitting
                    binding.btnSubmit.isEnabled = !state.isSubmitting

                    // Show/hide error message
                    state.errorMessage?.let { error ->
                        binding.tvError.text = error
                        binding.tvError.visibility = View.VISIBLE
                    } ?: run {
                        binding.tvError.visibility = View.GONE
                    }

                    // ─────────────────────────────────────────────────────────
                    // DETECT SUCCESSFUL SUBMISSION
                    // Form resets after success (empty state), so we detect
                    // this by checking if form is empty after we had content.
                    // ─────────────────────────────────────────────────────────

                    if (!state.isSubmitting &&
                        state.title.isEmpty() &&
                        state.description.isEmpty() &&
                        state.errorMessage == null) {

                        // Check if we actually had content before (user just submitted)
                        if (binding.etTitle.text?.isNotEmpty() == true) {
                            Toast.makeText(
                                this@CreatePostActivity,
                                "Post created successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()  // Close Activity and go back
                        }
                    }
                }
            }
        }
    }
}

