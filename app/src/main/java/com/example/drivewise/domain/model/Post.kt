/**
 * Post.kt
 * ========
 * This file defines the Post data model for promotional/news posts.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. NULLABLE PROPERTY: 'imageUrl' is String? (nullable) because posts
 *    don't always have an image.
 *
 * 2. PURPOSE: Admins create posts to share news, promotions, or updates
 *    with clients. Both roles can view posts.
 *
 * FIRESTORE COLLECTION: "posts"
 */
package com.example.drivewise.domain.model

/**
 * Represents a promotional or news post in DriveWise.
 *
 * Admins create these to communicate with clients about:
 * - Special offers and discounts
 * - New cars added to the fleet
 * - Company news and updates
 * - Rental tips and guidelines
 *
 * @property id Unique post identifier (Firestore document ID)
 * @property title Post headline/title
 * @property description Full post content/body
 * @property imageUrl Optional URL to an image for the post (nullable)
 * @property createdAt Timestamp when post was created (for sorting)
 * @property createdBy UID of the admin who created the post
 */
data class Post(
    val id: String = "",              // Firestore document ID
    val title: String = "",           // Post title/headline
    val description: String = "",     // Post body/content
    val imageUrl: String? = null,     // Optional image URL (nullable!)
    val createdAt: Long = System.currentTimeMillis(),  // Creation timestamp
    val createdBy: String = ""        // Admin's user ID who created this
)

