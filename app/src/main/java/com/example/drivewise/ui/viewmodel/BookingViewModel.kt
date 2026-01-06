package com.example.drivewise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drivewise.domain.model.Booking
import com.example.drivewise.domain.model.BookingStatus
import com.example.drivewise.domain.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingUiState(
    val bookings: List<Booking> = emptyList(),
    val filteredBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedBooking: Booking? = null,
    val currentFilter: BookingStatus? = null
)

sealed interface BookingEvent {
    data object BookingApproved : BookingEvent
    data object BookingRejected : BookingEvent
    data object BookingUpdated : BookingEvent
    data class Error(val message: String) : BookingEvent
}

class BookingViewModel(
    private val repository: BookingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookingUiState())
    val state: StateFlow<BookingUiState> = _state.asStateFlow()

    private val _event = MutableStateFlow<BookingEvent?>(null)
    val event: StateFlow<BookingEvent?> = _event.asStateFlow()

    init {
        observeAllBookings()
    }

    private fun observeAllBookings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                repository.observeAllBookings().collect { bookings ->
                    _state.update { state ->
                        val filtered = if (state.currentFilter != null) {
                            bookings.filter { it.getStatusEnum() == state.currentFilter }
                        } else {
                            bookings
                        }
                        state.copy(bookings = bookings, filteredBookings = filtered, isLoading = false, errorMessage = null)
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load bookings. Check Firestore permissions."
                    )
                }
            }
        }
    }

    fun filterByStatus(status: BookingStatus?) {
        _state.update { state ->
            val filtered = if (status != null) {
                state.bookings.filter { it.getStatusEnum() == status }
            } else {
                state.bookings
            }
            state.copy(currentFilter = status, filteredBookings = filtered)
        }
    }

    fun approveBooking(bookingId: String, notes: String = "") {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.updateBookingStatus(bookingId, BookingStatus.APPROVED, notes).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _event.value = BookingEvent.BookingApproved
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                    _event.value = BookingEvent.Error(throwable.message ?: "Failed to approve booking")
                }
            )
        }
    }

    fun rejectBooking(bookingId: String, reason: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.updateBookingStatus(bookingId, BookingStatus.REJECTED, reason).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _event.value = BookingEvent.BookingRejected
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                    _event.value = BookingEvent.Error(throwable.message ?: "Failed to reject booking")
                }
            )
        }
    }

    fun updateBookingStatus(bookingId: String, status: BookingStatus, notes: String = "") {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.updateBookingStatus(bookingId, status, notes).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _event.value = BookingEvent.BookingUpdated
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                    _event.value = BookingEvent.Error(throwable.message ?: "Failed to update booking status")
                }
            )
        }
    }

    fun updateBooking(booking: Booking) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.updateBooking(booking).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _event.value = BookingEvent.BookingUpdated
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                    _event.value = BookingEvent.Error(throwable.message ?: "Failed to update booking")
                }
            )
        }
    }

    fun loadBooking(bookingId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.getBooking(bookingId).fold(
                onSuccess = { booking ->
                    _state.update { it.copy(selectedBooking = booking, isLoading = false) }
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                }
            )
        }
    }

    fun clearEvent() {
        _event.value = null
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

