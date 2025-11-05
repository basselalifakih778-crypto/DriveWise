package com.example.drivewise.domain.model

enum class BookingStatus{
    PENDING, APPROVED, REJECTED, CANCLLED
}
data class Booking (
    val id:String,
    val carId:String,
    val userid:String,
    val startDate:String,
    val endDate:String,
    val status: BookingStatus,
    val  pickedPhotos: List<String> = emptyList(),
    val  returnedPhotos: List<String> = emptyList()

)


