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

class CreatePostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreatePostBinding
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

    private fun setupViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.etTitle.addTextChangedListener {
            viewModel.onTitleChanged(it?.toString() ?: "")
        }

        binding.etDescription.addTextChangedListener {
            viewModel.onDescriptionChanged(it?.toString() ?: "")
        }

        binding.etImageUrl.addTextChangedListener {
            viewModel.onImageUrlChanged(it?.toString() ?: "")
        }

        binding.btnSubmit.setOnClickListener {
            val adminId = FirebaseAuth.getInstance().currentUser?.uid
            if (adminId != null) {
                viewModel.createPost(adminId)
            } else {
                Toast.makeText(this, "You must be logged in to create a post", Toast.LENGTH_SHORT).show()
            }
        }
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
                viewModel.formState.collect { state ->
                    binding.progressBar.visibility = if (state.isSubmitting) View.VISIBLE else View.GONE
                    binding.btnSubmit.isEnabled = !state.isSubmitting

                    state.errorMessage?.let { error ->
                        binding.tvError.text = error
                        binding.tvError.visibility = View.VISIBLE
                    } ?: run {
                        binding.tvError.visibility = View.GONE
                    }

                    // If form was successfully submitted (empty form means reset after success)
                    if (!state.isSubmitting && state.title.isEmpty() && state.description.isEmpty() && state.errorMessage == null) {
                        // Check if we just submitted
                        if (binding.etTitle.text?.isNotEmpty() == true) {
                            Toast.makeText(this@CreatePostActivity, "Post created successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            }
        }
    }
}

