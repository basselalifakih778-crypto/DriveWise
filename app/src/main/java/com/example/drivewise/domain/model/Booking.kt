/**
 * Booking.kt
 * ===========
 * This file defines the Booking model and BookingStatus enum.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. BOOKING LIFECYCLE: A booking goes through several states:
 *    PENDING → APPROVED → COMPLETED (happy path)
 *    PENDING → REJECTED (admin denies)
 *    Any state → CANCELLED (user cancels)
 *
 * 2. DENORMALIZATION: We store both IDs (carId, userId) AND display names
 *    (carName, userName). This is intentional - it's faster to display
 *    without needing extra database queries.
 *
 * 3. PHOTOS: Clients upload photos when picking up and returning cars
 *    to document the car's condition.
 *
 * FIRESTORE COLLECTION: "bookings"
 */
package com.example.drivewise.domain.model

/**
 * Enum representing the possible states of a booking.
 *
 * WORKFLOW:
 * 1. Client creates booking → PENDING
 * 2. Admin reviews:
 *    - Approves → APPROVED (client can pick up car)
 *    - Rejects → REJECTED (with reason in adminNotes)
 * 3. After rental period:
 *    - Admin marks complete → COMPLETED
 * 4. At any time:
 *    - Client can cancel → CANCELLED
 */
enum class BookingStatus {
    PENDING,    // Waiting for admin approval
    APPROVED,   // Admin approved, rental can proceed
    REJECTED,   // Admin rejected the booking
    CANCELLED,  // Client cancelled the booking
    COMPLETED   // Rental completed successfully
}

/**
 * Represents a car rental booking in DriveWise.
 *
 * @property id Unique booking identifier (Firestore document ID)
 * @property carId Reference to the car being booked
 * @property userId Reference to the user making the booking
 * @property userName Display name of the user (denormalized for quick display)
 * @property userEmail User's email (for contact purposes)
 * @property carName Display name of the car (denormalized - "Toyota Camry")
 * @property startDate Rental start date (stored as string, e.g., "2024-01-15")
 * @property endDate Rental end date (stored as string, e.g., "2024-01-20")
 * @property totalPrice Total cost for the rental period
 * @property status Current booking status as string (maps to BookingStatus enum)
 * @property pickupPhotos URLs of photos taken when client picked up the car
 * @property returnPhotos URLs of photos taken when client returned the car
 * @property createdAt Timestamp when booking was created
 * @property adminNotes Notes from admin (e.g., rejection reason)
 */
data class Booking(
    val id: String = "",                          // Firestore document ID
    val carId: String = "",                       // Reference to cars collection
    val userId: String = "",                      // Reference to users collection
    val userName: String = "",                    // Cached user name for display
    val userEmail: String = "",                   // User's contact email
    val carName: String = "",                     // Cached car name (e.g., "BMW X5")
    val startDate: String = "",                   // Rental start date
    val endDate: String = "",                     // Rental end date
    val totalPrice: Double = 0.0,                 // Total rental cost
    val status: String = BookingStatus.PENDING.name,  // Current status
    val pickupPhotos: List<String> = emptyList(), // Car condition photos at pickup
    val returnPhotos: List<String> = emptyList(), // Car condition photos at return
    val createdAt: Long = System.currentTimeMillis(),  // When booking was created
    val adminNotes: String = ""                   // Admin comments/rejection reason
) {
    /**
     * Converts the status string to a BookingStatus enum for type-safe comparisons.
     *
     * WHY STORE AS STRING?
     * Firestore handles strings better than enums. This method lets us use
     * type-safe enum comparisons in code while storing simple strings in the DB.
     *
     * @return The BookingStatus enum value, or PENDING if status string is invalid
     */
    fun getStatusEnum(): BookingStatus =
        BookingStatus.values().firstOrNull { it.name == status } ?: BookingStatus.PENDING
}
