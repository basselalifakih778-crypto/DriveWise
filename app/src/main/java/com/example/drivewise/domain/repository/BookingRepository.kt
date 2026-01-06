/**
 * BookingRepository.kt
 * =====================
 * This file defines the BookingRepository interface - the contract for booking operations.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. BOOKING WORKFLOW:
 *    Client creates booking → Admin approves/rejects → Rental happens → Admin completes
 *
 * 2. OBSERVING vs FETCHING:
 *    - observeAllBookings() = Live updates (Flow), UI auto-updates
 *    - getBooking() = One-time fetch, returns current data only
 *
 * 3. STATUS UPDATES: Bookings change status often. We have specific methods
 *    for status updates to make the code clearer.
 */
package com.example.drivewise.domain.repository

import com.example.drivewise.domain.model.Booking
import com.example.drivewise.domain.model.BookingStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contract for car booking/rental operations.
 *
 * Implemented by FirebaseBookingRepository.
 * Used by BookingViewModel to manage the booking lifecycle.
 */
interface BookingRepository {

    /**
     * Observes ALL bookings in real-time.
     *
     * Used by admins to see all booking requests.
     * Sorted by createdAt (newest first).
     *
     * @return Flow emitting all bookings whenever any booking changes
     */
    fun observeAllBookings(): Flow<List<Booking>>

    /**
     * Observes bookings filtered by status.
     *
     * Useful for showing only pending, approved, etc.
     *
     * @param status The BookingStatus to filter by
     * @return Flow emitting only bookings with the specified status
     */
    fun observeBookingsByStatus(status: BookingStatus): Flow<List<Booking>>

    /**
     * Fetches a single booking by ID.
     *
     * Used to load booking details for a detail screen.
     *
     * @param bookingId The Firestore document ID
     * @return Result containing the Booking, or null if not found
     */
    suspend fun getBooking(bookingId: String): Result<Booking?>

    /**
     * Updates a booking's status (approve, reject, complete, etc.)
     *
     * This is the most common booking operation.
     *
     * @param bookingId The booking to update
     * @param status The new status (APPROVED, REJECTED, COMPLETED, etc.)
     * @param adminNotes Optional notes (e.g., rejection reason)
     * @return Result.success if updated successfully
     */
    suspend fun updateBookingStatus(
        bookingId: String,
        status: BookingStatus,
        adminNotes: String = ""
    ): Result<Unit>

    /**
     * Updates an entire booking document.
     *
     * Used when multiple fields need to change at once.
     *
     * @param booking The booking with updated fields
     * @return Result.success if updated successfully
     */
    suspend fun updateBooking(booking: Booking): Result<Unit>

    /**
     * Gets all bookings for a specific car.
     *
     * Useful to check if a car has any active bookings before deletion.
     *
     * @param carId The car's document ID
     * @return Result containing list of bookings for that car
     */
    suspend fun getBookingsForCar(carId: String): Result<List<Booking>>
}

