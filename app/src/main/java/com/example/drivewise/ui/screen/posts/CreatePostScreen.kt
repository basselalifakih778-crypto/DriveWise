package com.example.drivewise.ui.screen.posts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivewise.ui.viewmodel.PostsViewModel

@Composable
fun CreatePostScreen(
    viewModel: PostsViewModel,
    onPostCreated: () -> Unit,
    adminId: String
) {
    val state by viewModel.formState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(24.dp)) {
        Text(text = "Create Post", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.padding(8.dp))

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::onTitleChanged,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(8.dp))

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChanged,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.padding(8.dp))

        OutlinedTextField(
            value = state.imageUrl,
            onValueChange = viewModel::onImageUrlChanged,
            label = { Text("Image URL (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        state.errorMessage?.let {
            Spacer(modifier = Modifier.padding(4.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.padding(8.dp))
        Button(onClick = {
            viewModel.createPost(adminId)
            if (viewModel.formState.value.errorMessage == null) onPostCreated()
        }, enabled = !state.isSubmitting) {
            Text(if (state.isSubmitting) "Publishing..." else "Publish")
        }
    }
}

