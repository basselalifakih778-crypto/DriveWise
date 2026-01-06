/**
 * ViewModelFactories.kt
 * ======================
 * This file contains factory classes for creating ViewModels with dependencies.
 *
 * KEY CONCEPTS FOR BEGINNERS:
 * ---------------------------
 * 1. WHY FACTORIES?
 *    By default, ViewModels can't have constructor parameters.
 *    Android creates them using reflection with a no-arg constructor.
 *    But our ViewModels NEED a repository parameter!
 *
 *    Solution: Create a Factory that knows how to build the ViewModel with its dependencies.
 *
 * 2. VIEWMODELPROVIDER.FACTORY:
 *    - Android calls our create() method when it needs a ViewModel
 *    - We check if the requested class matches our ViewModel
 *    - We create and return the ViewModel with the repository injected
 *
 * 3. MANUAL DEPENDENCY INJECTION:
 *    This is the "manual" way to do DI (without Hilt/Dagger).
 *    The Activity creates the repository and passes it to the factory.
 *    The factory creates the ViewModel with that repository.
 *
 * FLOW:
 * Activity creates Repository → Activity creates Factory(repository) →
 * Activity uses 'by viewModels { factory }' → Factory.create() called →
 * ViewModel returned with repository injected
 *
 * WITH HILT (the "automatic" way):
 * You would annotate the ViewModel with @HiltViewModel and repository with @Inject,
 * and Hilt would handle all of this automatically. But manual DI is simpler to understand.
 */
package com.example.drivewise.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.drivewise.domain.repository.AuthRepository
import com.example.drivewise.domain.repository.BookingRepository
import com.example.drivewise.domain.repository.CarRepository
import com.example.drivewise.domain.repository.PostsRepository
import com.example.drivewise.domain.repository.UserRepository
import com.example.drivewise.ui.viewmodel.AuthViewModel
import com.example.drivewise.ui.viewmodel.BookingViewModel
import com.example.drivewise.ui.viewmodel.CarViewModel
import com.example.drivewise.ui.viewmodel.PostsViewModel
import com.example.drivewise.ui.viewmodel.UserViewModel

// ═══════════════════════════════════════════════════════════════════════════════
// AUTH VIEWMODEL FACTORY
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Factory for creating AuthViewModel with AuthRepository dependency.
 *
 * USAGE IN ACTIVITY:
 *   private val viewModel: AuthViewModel by viewModels {
 *       AuthViewModelFactory(FirebaseAuthRepository(...))
 *   }
 *
 * @param repository The AuthRepository implementation to inject
 */
class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {

    /**
     * Creates a new ViewModel instance.
     *
     * @Suppress annotation tells Kotlin to ignore the unchecked cast warning.
     * We know the cast is safe because we check the class type first.
     *
     * @param modelClass The class of ViewModel to create
     * @return The ViewModel instance
     * @throws IllegalArgumentException if wrong ViewModel class is requested
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check if the requested class is AuthViewModel
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            // Create AuthViewModel with our repository
            return AuthViewModel(repository) as T
        }
        // If someone tries to use this factory for a different ViewModel, throw error
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// POSTS VIEWMODEL FACTORY
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Factory for creating PostsViewModel with PostsRepository dependency.
 *
 * @param repository The PostsRepository implementation to inject
 */
class PostsViewModelFactory(
    private val repository: PostsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostsViewModel::class.java)) {
            return PostsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CAR VIEWMODEL FACTORY
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Factory for creating CarViewModel with CarRepository dependency.
 *
 * @param repository The CarRepository implementation to inject
 */
class CarViewModelFactory(
    private val repository: CarRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CarViewModel::class.java)) {
            return CarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BOOKING VIEWMODEL FACTORY
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Factory for creating BookingViewModel with BookingRepository dependency.
 *
 * @param repository The BookingRepository implementation to inject
 */
class BookingViewModelFactory(
    private val repository: BookingRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookingViewModel::class.java)) {
            return BookingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// USER VIEWMODEL FACTORY
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Factory for creating UserViewModel with UserRepository dependency.
 *
 * @param repository The UserRepository implementation to inject
 */
class UserViewModelFactory(
    private val repository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            return UserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
