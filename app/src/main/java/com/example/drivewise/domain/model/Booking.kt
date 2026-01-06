package com.example.drivewise.domain.model

enum class BookingStatus {
    PENDING, APPROVED, REJECTED, CANCELLED, COMPLETED
}

data class Booking(
    val id: String = "",
    val carId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val carName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val totalPrice: Double = 0.0,
    val status: String = BookingStatus.PENDING.name,
    val pickupPhotos: List<String> = emptyList(),
    val returnPhotos: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val adminNotes: String = ""
) {
    fun getStatusEnum(): BookingStatus =
        BookingStatus.values().firstOrNull { it.name == status } ?: BookingStatus.PENDING
}
