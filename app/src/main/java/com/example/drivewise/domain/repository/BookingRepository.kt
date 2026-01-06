package com.example.drivewise.domain.repository

import com.example.drivewise.domain.model.Booking
import com.example.drivewise.domain.model.BookingStatus
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    fun observeAllBookings(): Flow<List<Booking>>
    fun observeBookingsByStatus(status: BookingStatus): Flow<List<Booking>>
    suspend fun getBooking(bookingId: String): Result<Booking?>
    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus, adminNotes: String = ""): Result<Unit>
    suspend fun updateBooking(booking: Booking): Result<Unit>
    suspend fun getBookingsForCar(carId: String): Result<List<Booking>>
}

