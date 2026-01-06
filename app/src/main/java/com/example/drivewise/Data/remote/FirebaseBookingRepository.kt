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

class FirebaseBookingRepository(
    private val firestore: FirebaseFirestore
) : BookingRepository {

    private val bookingsCollection get() = firestore.collection("bookings")

    override fun observeAllBookings(): Flow<List<Booking>> = callbackFlow {
        val registration = bookingsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val bookings = snapshot?.documents?.mapNotNull {
                    it.toObject(Booking::class.java)?.copy(id = it.id)
                }.orEmpty()
                trySend(bookings)
            }
        awaitClose { registration.remove() }
    }

    override fun observeBookingsByStatus(status: BookingStatus): Flow<List<Booking>> = callbackFlow {
        val registration = bookingsCollection
            .whereEqualTo("status", status.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val bookings = snapshot?.documents?.mapNotNull {
                    it.toObject(Booking::class.java)?.copy(id = it.id)
                }.orEmpty()
                trySend(bookings)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getBooking(bookingId: String): Result<Booking?> = runCatching {
        val snapshot = bookingsCollection.document(bookingId).get().await()
        snapshot.toObject(Booking::class.java)?.copy(id = snapshot.id)
    }

    override suspend fun updateBookingStatus(
        bookingId: String,
        status: BookingStatus,
        adminNotes: String
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "status" to status.name
        )
        if (adminNotes.isNotBlank()) {
            updates["adminNotes"] = adminNotes
        }
        bookingsCollection.document(bookingId).update(updates).await()
    }

    override suspend fun updateBooking(booking: Booking): Result<Unit> = runCatching {
        bookingsCollection.document(booking.id).set(booking).await()
    }

    override suspend fun getBookingsForCar(carId: String): Result<List<Booking>> = runCatching {
        val snapshot = bookingsCollection
            .whereEqualTo("carId", carId)
            .get()
            .await()
        snapshot.documents.mapNotNull {
            it.toObject(Booking::class.java)?.copy(id = it.id)
        }
    }
}

