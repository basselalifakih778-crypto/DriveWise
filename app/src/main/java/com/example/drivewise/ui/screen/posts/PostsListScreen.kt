package com.example.drivewise.ui.screen.posts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.drivewise.domain.model.Post
import com.example.drivewise.ui.state.PostsUiState
import com.example.drivewise.ui.viewmodel.PostsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PostsListScreen(
    viewModel: PostsViewModel,
    onPostClick: (Post) -> Unit,
    showCreateButton: Boolean,
    onCreatePost: () -> Unit
) {
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (postsState) {
            PostsUiState.Loading -> Text("Loading posts...", modifier = Modifier.padding(16.dp))
            is PostsUiState.Error -> Text(
                text = (postsState as PostsUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
            is PostsUiState.Success -> PostsLazyList(
                posts = (postsState as PostsUiState.Success).posts,
                onPostClick = onPostClick
            )
        }

        if (showCreateButton) {
            ExtendedFloatingActionButton(
                text = { Text("Create Post") },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                onClick = onCreatePost,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun PostsLazyList(posts: List<Post>, onPostClick: (Post) -> Unit) {
    LazyColumn {
        items(posts, key = { it.id }) { post ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable { onPostClick(post) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = post.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = post.description.take(120),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    val date = formatDate(post.createdAt)
                    Text(text = date, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

