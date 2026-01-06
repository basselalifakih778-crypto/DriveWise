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

data class CarUiState(
    val cars: List<Car> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedCar: Car? = null,
    val operationSuccess: Boolean = false
)

sealed interface CarEvent {
    data object CarAdded : CarEvent
    data object CarUpdated : CarEvent
    data object CarDeleted : CarEvent
    data class Error(val message: String) : CarEvent
}

class CarViewModel(
    private val repository: CarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CarUiState())
    val state: StateFlow<CarUiState> = _state.asStateFlow()

    private val _event = MutableStateFlow<CarEvent?>(null)
    val event: StateFlow<CarEvent?> = _event.asStateFlow()

    init {
        observeCars()
    }

    private fun observeCars() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
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

    fun clearEvent() {
        _event.value = null
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}

