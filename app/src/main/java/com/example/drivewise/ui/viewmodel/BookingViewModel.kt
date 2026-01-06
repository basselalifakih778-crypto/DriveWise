/**
 * BookingViewModel.kt
 * ====================
 * This ViewModel handles all booking management operations.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. CLIENT-SIDE FILTERING: We load ALL bookings from Firestore, then filter
 *    in memory by status. This avoids needing complex Firestore indexes.
 *
 * 2. BOOKING WORKFLOW: The ViewModel supports the full lifecycle:
 *    PENDING → APPROVED/REJECTED → COMPLETED
 *
 * 3. TWO LISTS: We maintain both:
 *    - bookings: All bookings from database
 *    - filteredBookings: Subset matching current filter
 *
 * USED BY: ManageBookingsActivity (admin), MyBookingsActivity (client)
 */
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

// ═══════════════════════════════════════════════════════════════════════════════
// BOOKING UI STATE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * UI state for booking-related screens.
 *
 * @property bookings All bookings from the database
 * @property filteredBookings Bookings matching the current filter
 * @property isLoading Whether we're loading/updating data
 * @property errorMessage Error to display, or null
 * @property selectedBooking Currently selected booking for detail view
 * @property currentFilter The status filter being applied (null = show all)
 */
data class BookingUiState(
    val bookings: List<Booking> = emptyList(),
    val filteredBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedBooking: Booking? = null,
    val currentFilter: BookingStatus? = null  // null = no filter (show all)
)

// ═══════════════════════════════════════════════════════════════════════════════
// BOOKING EVENTS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * One-time events from booking operations.
 */
sealed interface BookingEvent {
    /** Booking was approved by admin */
    data object BookingApproved : BookingEvent

    /** Booking was rejected by admin */
    data object BookingRejected : BookingEvent

    /** Booking was updated (status change, photos added, etc.) */
    data object BookingUpdated : BookingEvent

    /** An error occurred */
    data class Error(val message: String) : BookingEvent
}

// ═══════════════════════════════════════════════════════════════════════════════
// BOOKING VIEWMODEL
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * ViewModel for booking management.
 *
 * Provides:
 * - Real-time list of all bookings
 * - Status filtering (pending, approved, etc.)
 * - Approve/reject/complete operations
 *
 * @param repository The BookingRepository implementation
 */
class BookingViewModel(
    private val repository: BookingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BookingUiState())
    val state: StateFlow<BookingUiState> = _state.asStateFlow()

    private val _event = MutableStateFlow<BookingEvent?>(null)
    val event: StateFlow<BookingEvent?> = _event.asStateFlow()

    /**
     * Start observing bookings when ViewModel is created.
     */
    init {
        observeAllBookings()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OBSERVE ALL BOOKINGS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Observes all bookings with real-time updates.
     *
     * When new data arrives, we also apply the current filter.
     * This ensures the filtered list stays in sync.
     */
    private fun observeAllBookings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                repository.observeAllBookings().collect { bookings ->
                    _state.update { state ->
                        // Apply current filter to new data
                        val filtered = if (state.currentFilter != null) {
                            bookings.filter { it.getStatusEnum() == state.currentFilter }
                        } else {
                            bookings  // No filter = show all
                        }
                        state.copy(
                            bookings = bookings,
                            filteredBookings = filtered,
                            isLoading = false,
                            errorMessage = null
                        )
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

    // ─────────────────────────────────────────────────────────────────────────
    // FILTER BY STATUS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Filters the displayed bookings by status.
     *
     * This is done in memory (no new Firestore query).
     * Pass null to clear the filter and show all bookings.
     *
     * @param status The status to filter by, or null for all
     */
    fun filterByStatus(status: BookingStatus?) {
        _state.update { state ->
            val filtered = if (status != null) {
                state.bookings.filter { it.getStatusEnum() == status }
            } else {
                state.bookings  // Show all
            }
            state.copy(currentFilter = status, filteredBookings = filtered)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPROVE BOOKING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Approves a pending booking.
     *
     * Changes status from PENDING to APPROVED.
     * The client can now pick up the car.
     *
     * @param bookingId The booking to approve
     * @param notes Optional admin notes
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // REJECT BOOKING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Rejects a pending booking.
     *
     * Changes status from PENDING to REJECTED.
     * The reason is stored in adminNotes.
     *
     * @param bookingId The booking to reject
     * @param reason Why the booking was rejected (shown to client)
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE BOOKING STATUS (GENERIC)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates a booking to any status.
     *
     * More flexible than approve/reject - can be used for COMPLETED, CANCELLED, etc.
     *
     * @param bookingId The booking to update
     * @param status The new status
     * @param notes Optional admin notes
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE FULL BOOKING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates a booking with all new field values.
     *
     * Used when multiple fields change (e.g., photos + notes).
     *
     * @param booking The updated booking object
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD SINGLE BOOKING
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads a single booking by ID.
     *
     * Used by detail screens to show booking information.
     *
     * @param bookingId The booking ID to load
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITY METHODS
    // ─────────────────────────────────────────────────────────────────────────

    fun clearEvent() {
        _event.value = null
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

