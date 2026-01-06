/**
 * CarViewModel.kt
 * =================
 * This ViewModel handles all car fleet management operations.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. REAL-TIME OBSERVATION: The ViewModel automatically observes car changes
 *    from Firestore. When any car is added/updated/deleted anywhere,
 *    the UI updates automatically.
 *
 * 2. INIT BLOCK: The observeCars() call in init{} means we start loading
 *    cars as soon as the ViewModel is created. No manual "load" button needed.
 *
 * 3. DATA CLASS FOR UI STATE: CarUiState bundles all UI-related data together.
 *    This makes it easy to pass to composables or update views.
 *
 * USED BY: ManageCarsActivity (admin), BrowseCarsActivity (client), AddEditCarActivity
 */
package com.example.drivewise.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drivewise.domain.model.Car
import com.example.drivewise.domain.repository.CarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
// CAR UI STATE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Data class representing the UI state for car-related screens.
 *
 * WHY A DATA CLASS?
 * - Immutable: Once created, can't be modified (use copy() to create modified versions)
 * - All fields in one place: Easy to see what data the UI needs
 * - copy() method: Easy to update one field while keeping others
 *
 * @property cars The list of all cars
 * @property isLoading Whether we're loading data
 * @property errorMessage Error message to display, or null
 * @property selectedCar Currently selected car (for edit screen)
 * @property operationSuccess Whether the last CRUD operation succeeded
 */
data class CarUiState(
    val cars: List<Car> = emptyList(),      // List of cars to display
    val isLoading: Boolean = false,          // Show loading spinner?
    val errorMessage: String? = null,        // Error to show, null if none
    val selectedCar: Car? = null,            // Car being edited/viewed
    val operationSuccess: Boolean = false    // Did last add/update/delete succeed?
)

// ═══════════════════════════════════════════════════════════════════════════════
// CAR EVENTS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * One-time events from car operations.
 * UI should handle these once (show toast, navigate, etc.)
 */
sealed interface CarEvent {
    /** A new car was successfully added */
    data object CarAdded : CarEvent

    /** An existing car was successfully updated */
    data object CarUpdated : CarEvent

    /** A car was successfully deleted */
    data object CarDeleted : CarEvent

    /** An error occurred during a car operation */
    data class Error(val message: String) : CarEvent
}

// ═══════════════════════════════════════════════════════════════════════════════
// CAR VIEWMODEL
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * ViewModel for car fleet management.
 *
 * Provides:
 * - Real-time list of all cars
 * - CRUD operations (add, update, delete)
 * - Availability and pricing toggles
 *
 * @param repository The CarRepository implementation
 */
class CarViewModel(
    private val repository: CarRepository
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────────────
    // STATE
    // ─────────────────────────────────────────────────────────────────────────

    private val _state = MutableStateFlow(CarUiState())
    val state: StateFlow<CarUiState> = _state.asStateFlow()

    private val _event = MutableStateFlow<CarEvent?>(null)
    val event: StateFlow<CarEvent?> = _event.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // INITIALIZATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * init{} runs when the ViewModel is first created.
     * We immediately start observing cars so data is ready when UI appears.
     */
    init {
        observeCars()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OBSERVE CARS (REAL-TIME)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Starts observing the cars collection with real-time updates.
     *
     * HOW IT WORKS:
     * 1. Set loading state
     * 2. Collect from repository.observeCars() Flow
     * 3. Every time Firestore emits new data, update _state.cars
     * 4. If Flow throws error, catch and show error message
     *
     * WHY TRY-CATCH?
     * The Flow can throw if Firestore connection fails or permissions denied.
     */
    private fun observeCars() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // collect() suspends and receives every emission from the Flow
                repository.observeCars().collect { cars ->
                    _state.update { it.copy(cars = cars, isLoading = false, errorMessage = null) }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load cars. Check Firestore permissions."
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADD CAR
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Adds a new car to the fleet.
     *
     * @param car The car to add (id will be auto-generated)
     */
    fun addCar(car: Car) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            repository.addCar(car).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, operationSuccess = true) }
                    _event.value = CarEvent.CarAdded
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                    _event.value = CarEvent.Error(throwable.message ?: "Failed to add car")
                }
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE CAR
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updates an existing car's information.
     *
     * @param car The car with updated fields (must have valid id)
     */
    fun updateCar(car: Car) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            repository.updateCar(car).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, operationSuccess = true) }
                    _event.value = CarEvent.CarUpdated
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                    _event.value = CarEvent.Error(throwable.message ?: "Failed to update car")
                }
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE CAR
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deletes a car from the fleet.
     *
     * @param carId The ID of the car to delete
     */
    fun deleteCar(carId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            repository.deleteCar(carId).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false, operationSuccess = true) }
                    _event.value = CarEvent.CarDeleted
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                    _event.value = CarEvent.Error(throwable.message ?: "Failed to delete car")
                }
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUICK UPDATES (single field)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Toggles car availability (available/unavailable).
     * Convenience method for the "Set Available/Unavailable" button.
     *
     * @param carId The car's ID
     * @param available The new availability status
     */
    fun updateAvailability(carId: String, available: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.updateAvailability(carId, available).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _event.value = CarEvent.CarUpdated
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                }
            )
        }
    }

    /**
     * Updates only the daily price of a car.
     *
     * @param carId The car's ID
     * @param newPrice The new daily rental price
     */
    fun updatePricing(carId: String, newPrice: Double) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.updatePricing(carId, newPrice).fold(
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    _event.value = CarEvent.CarUpdated
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                }
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD SINGLE CAR
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads a single car by ID into selectedCar state.
     * Used by edit screen to populate the form.
     *
     * @param carId The car's ID to load
     */
    fun loadCar(carId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.getCar(carId).fold(
                onSuccess = { car ->
                    _state.update { it.copy(selectedCar = car, isLoading = false) }
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

    /** Clears the current event after it's been handled */
    fun clearEvent() {
        _event.value = null
    }

    /** Clears the error message */
    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

