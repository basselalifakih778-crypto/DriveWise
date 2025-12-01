package com.example.drivewise.Data.remote

import com.example.drivewise.domain.model.Post
import com.example.drivewise.domain.repository.PostsRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePostsRepository(
    private val firestore: FirebaseFirestore
) : PostsRepository {

    private val postsCollection get() = firestore.collection("posts")

    override fun observePosts(): Flow<List<Post>> = callbackFlow {
        val registration = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { it.toObject(Post::class.java)?.copy(id = it.id) }
                    .orEmpty()
                trySend(posts)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun createPost(post: Post): Result<Unit> = runCatching {
        val doc = postsCollection.document()
        postsCollection.document(doc.id).set(post.copy(id = doc.id)).await()
    }

    override suspend fun getPost(postId: String): Result<Post?> = runCatching {
        val snapshot = postsCollection.document(postId).get().await()
        snapshot.toObject(Post::class.java)?.copy(id = snapshot.id)
    }
}

