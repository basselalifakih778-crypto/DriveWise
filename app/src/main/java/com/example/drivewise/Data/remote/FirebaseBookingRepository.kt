/**
 * FirebaseBookingRepository.kt
 * =============================
 * This is the Firebase implementation of BookingRepository.
 * It handles all booking CRUD operations using Firestore.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. BOOKING WORKFLOW: This repository supports the full booking lifecycle:
 *    Create (client) → Approve/Reject (admin) → Complete (admin)
 *
 * 2. QUERIES: Firestore queries filter data:
 *    - whereEqualTo("status", "PENDING") - only pending bookings
 *    - orderBy("createdAt", DESCENDING) - newest first
 *
 * 3. MUTABLEMAP: Used when we want to update some fields but not others.
 *    We build a map of fields to update, then call update(map).
 *
 * FIRESTORE COLLECTION: "bookings"
 */
package com.example.drivewise.Data.remote

import com.example.drivewise.domain.model.Booking
import com.example.drivewise.domain.model.BookingStatus
import com.example.drivewise.domain.repository.BookingRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Firestore implementation of BookingRepository.
 *
 * Manages the "bookings" collection in Firestore.
 *
 * @param firestore The Firestore database instance
 */
class FirebaseBookingRepository(
    private val firestore: FirebaseFirestore
) : BookingRepository {

    /**
     * Reference to the "bookings" collection.
     */
    private val bookingsCollection get() = firestore.collection("bookings")

    // ═══════════════════════════════════════════════════════════════════════════
    // OBSERVE ALL BOOKINGS (REAL-TIME)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Observes all bookings with real-time updates.
     *
     * Used by admin to see all booking requests regardless of status.
     * Sorted by creation date (newest first).
     *
     * @return Flow emitting all bookings whenever any booking changes
     */
    override fun observeAllBookings(): Flow<List<Booking>> = callbackFlow {
        // Set up real-time listener on all bookings, sorted by date
        val registration = bookingsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                // Convert each document to a Booking object
                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }.orEmpty()

                // Emit the list to collectors
                trySend(bookings)
            }

        // Clean up listener when Flow is cancelled
        awaitClose { registration.remove() }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OBSERVE BOOKINGS BY STATUS (REAL-TIME)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Observes bookings filtered by status.
     *
     * Example: observeBookingsByStatus(BookingStatus.PENDING) returns only pending bookings.
     *
     * NOTE: This requires a Firestore composite index on (status, createdAt).
     * The index should be created automatically when the query first runs.
     *
     * @param status Only return bookings with this status
     * @return Flow emitting filtered bookings with real-time updates
     */
    override fun observeBookingsByStatus(status: BookingStatus): Flow<List<Booking>> = callbackFlow {
        // Filter by status field, then sort by date
        val registration = bookingsCollection
            .whereEqualTo("status", status.name)  // Filter: status == "PENDING" (or other)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val bookings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Booking::class.java)?.copy(id = doc.id)
                }.orEmpty()

                trySend(bookings)
            }

        awaitClose { registration.remove() }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET SINGLE BOOKING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Fetches a single booking by ID.
     *
     * @param bookingId The Firestore document ID
     * @return Result containing the Booking, or null if not found
     */
    override suspend fun getBooking(bookingId: String): Result<Booking?> = runCatching {
        val snapshot = bookingsCollection.document(bookingId).get().await()
        snapshot.toObject(Booking::class.java)?.copy(id = snapshot.id)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE BOOKING STATUS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Updates the status of a booking (approve, reject, complete, etc.)
     *
     * Uses update() to change only specific fields, not the whole document.
     * If adminNotes is provided, that field is also updated.
     *
     * @param bookingId The booking to update
     * @param status The new status
     * @param adminNotes Optional notes (e.g., rejection reason)
     * @return Result.success if updated successfully
     */
    override suspend fun updateBookingStatus(
        bookingId: String,
        status: BookingStatus,
        adminNotes: String
    ): Result<Unit> = runCatching {
        // Build a map of fields to update
        // mutableMapOf lets us add fields conditionally
        val updates = mutableMapOf<String, Any>(
            "status" to status.name  // Always update status
        )

        // Only update adminNotes if it's not blank
        if (adminNotes.isNotBlank()) {
            updates["adminNotes"] = adminNotes
        }

        // update() only changes the specified fields
        bookingsCollection.document(bookingId).update(updates).await()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UPDATE FULL BOOKING
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Replaces an entire booking document.
     *
     * Used when multiple fields need to change at once.
     *
     * @param booking The updated booking data
     * @return Result.success if updated successfully
     */
    override suspend fun updateBooking(booking: Booking): Result<Unit> = runCatching {
        // set() replaces the entire document
        bookingsCollection.document(booking.id).set(booking).await()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GET BOOKINGS FOR CAR
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Gets all bookings associated with a specific car.
     *
     * Useful for:
     * - Checking if a car has active bookings before deletion
     * - Showing booking history for a car
     *
     * @param carId The car's document ID
     * @return Result containing list of bookings for that car
     */
    override suspend fun getBookingsForCar(carId: String): Result<List<Booking>> = runCatching {
        // Query for all bookings where carId matches
        val snapshot = bookingsCollection
            .whereEqualTo("carId", carId)
            .get()
            .await()

        // Convert all matching documents to Booking objects
        snapshot.documents.mapNotNull { doc ->
            doc.toObject(Booking::class.java)?.copy(id = doc.id)
        }
    }
}

