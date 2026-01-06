package com.example.drivewise.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.drivewise.Data.remote.FirebaseUserRepository
import com.example.drivewise.databinding.ActivityCustomerProfilesBinding
import com.example.drivewise.domain.model.Booking
import com.example.drivewise.domain.model.User
import com.example.drivewise.ui.adapter.CustomerAdapter
import com.example.drivewise.ui.viewmodel.UserEvent
import com.example.drivewise.ui.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CustomerProfilesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerProfilesBinding
    private lateinit var customerAdapter: CustomerAdapter

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val viewModel: UserViewModel by viewModels {
        UserViewModelFactory(
            FirebaseUserRepository(firestore)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerProfilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        observeState()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        customerAdapter = CustomerAdapter(
            onViewProfileClick = { user ->
                showCustomerProfile(user.uid)
            },
            onViewDocumentsClick = { user ->
                showDocumentsDialog(user)
            },
            onToggleVerifyClick = { user ->
                showVerifyConfirmation(user)
            }
        )

        binding.rvCustomers.apply {
            layoutManager = LinearLayoutManager(this@CustomerProfilesActivity)
            adapter = customerAdapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    if (state.users.isEmpty() && !state.isLoading) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvCustomers.visibility = View.GONE

                        // Show helpful message
                        if (state.errorMessage != null) {
                            binding.tvEmpty.text = "Error: ${state.errorMessage}\n\nMake sure:\n1. Firestore rules are published\n2. You're logged in as admin\n3. There are registered users"
                        } else {
                            binding.tvEmpty.text = "No customers found.\n\nUsers need to register first.\nMake sure users have role = 'client' in Firestore."
                        }
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvCustomers.visibility = View.VISIBLE
                        customerAdapter.submitList(state.users)
                    }

                    state.errorMessage?.let { error ->
                        Toast.makeText(this@CustomerProfilesActivity, "Error: $error", Toast.LENGTH_LONG).show()
                        // Don't clear error immediately so it shows in empty state
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is UserEvent.UserVerified -> {
                            Toast.makeText(this@CustomerProfilesActivity, "User verification updated", Toast.LENGTH_SHORT).show()
                            viewModel.clearEvent()
                        }
                        is UserEvent.Error -> {
                            Toast.makeText(this@CustomerProfilesActivity, "Error: ${event.message}", Toast.LENGTH_SHORT).show()
                            viewModel.clearEvent()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun showDocumentsDialog(user: User) {
        val documents = mutableListOf<String>()
        if (user.licenseImageUrl.isNotEmpty()) documents.add("Driver's License")
        if (user.idDocumentUrl.isNotEmpty()) documents.add("ID Document")

        if (documents.isEmpty()) {
            Toast.makeText(this, "No documents uploaded by this customer", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Customer Documents")
            .setItems(documents.toTypedArray()) { _, which ->
                val url = when (which) {
                    0 -> if (user.licenseImageUrl.isNotEmpty()) user.licenseImageUrl else user.idDocumentUrl
                    1 -> user.idDocumentUrl
                    else -> ""
                }
                if (url.isNotEmpty()) {
                    val intent = Intent(this, DocumentViewerActivity::class.java)
                    intent.putExtra("DOCUMENT_URL", url)
                    intent.putExtra("DOCUMENT_TITLE", documents[which])
                    startActivity(intent)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showVerifyConfirmation(user: User) {
        val action = if (user.isVerified) "unverify" else "verify"
        AlertDialog.Builder(this)
            .setTitle("${action.replaceFirstChar { it.uppercase() }} Customer")
            .setMessage("Are you sure you want to $action ${user.fullName.ifEmpty { user.email }}?")
            .setPositiveButton(action.replaceFirstChar { it.uppercase() }) { _, _ ->
                viewModel.updateVerification(user.uid, !user.isVerified)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomerProfile(userId: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                val userSnap = firestore.collection("users").document(userId).get().await()
                val baseUser = userSnap.toObject(User::class.java)?.copy(uid = userSnap.id)

                if (baseUser == null) {
                    Toast.makeText(this@CustomerProfilesActivity, "Customer profile not found", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Coerce isVerified defensively (Firestore may store it as Boolean/String/Number)
                val rawIsVerified = userSnap.get("isVerified")
                val coercedIsVerified = when (rawIsVerified) {
                    is Boolean -> rawIsVerified
                    is Number -> rawIsVerified.toInt() != 0
                    is String -> rawIsVerified.equals("true", ignoreCase = true) || rawIsVerified == "1"
                    else -> baseUser.isVerified
                }

                val user = baseUser.copy(isVerified = coercedIsVerified)

                val bookings = fetchBookingsForUser(userId)

                val verifiedText = if (user.isVerified) "Verified" else "Not Verified"
                val details = buildString {
                    appendLine("Name: ${user.fullName.ifEmpty { "(not set)" }}")
                    appendLine("Email: ${user.email.ifEmpty { "(not set)" }}")
                    appendLine("Phone: ${user.phone.ifEmpty { "(not set)" }}")
                    appendLine("Address: ${user.address.ifEmpty { "(not set)" }}")
                    appendLine()
                    appendLine("Verification: $verifiedText")
                    appendLine("Driver License Number: ${user.licenseNumber.ifEmpty { "(not set)" }}")
                    appendLine()
                    appendLine("Uploaded:")
                    appendLine("- Profile Photo: ${if (user.profileImageUrl.isNotEmpty()) "Yes" else "No"}")
                    appendLine("- Driver License Photo: ${if (user.licenseImageUrl.isNotEmpty()) "Yes" else "No"}")
                    appendLine("- ID Document Photo: ${if (user.idDocumentUrl.isNotEmpty()) "Yes" else "No"}")
                    appendLine()
                    appendLine("Bookings: ${bookings.size}")
                }

                val bookingItems = bookings
                    .sortedByDescending { it.createdAt }
                    .take(10)
                    .map { b ->
                        "${b.carName.ifEmpty { b.carId }} • ${b.startDate} → ${b.endDate} • ${b.status}"
                    }

                AlertDialog.Builder(this@CustomerProfilesActivity)
                    .setTitle("Customer Profile")
                    .setMessage(details)
                    .setPositiveButton("View Documents") { _, _ ->
                        showDocumentsDialog(user)
                    }
                    .setNeutralButton(if (bookingItems.isEmpty()) "Close" else "View Bookings") { _, _ ->
                        if (bookingItems.isNotEmpty()) {
                            AlertDialog.Builder(this@CustomerProfilesActivity)
                                .setTitle("Recent Bookings (${bookingItems.size})")
                                .setItems(bookingItems.toTypedArray(), null)
                                .setPositiveButton("Close", null)
                                .show()
                        }
                    }
                    .setNegativeButton("Close", null)
                    .show()

            } catch (e: Exception) {
                Toast.makeText(this@CustomerProfilesActivity, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun fetchBookingsForUser(userId: String): List<Booking> {
        return try {
            val snap = firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            snap.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.copy(id = doc.id)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
