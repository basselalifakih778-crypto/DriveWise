package com.example.drivewise.ui.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.drivewise.R
import com.example.drivewise.databinding.ActivityMyProfileBinding
import com.example.drivewise.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class MyProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyProfileBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var currentUser: User? = null
    private var uploadType: UploadType = UploadType.LICENSE
    private var licenseImageUrl: String? = null
    private var idDocumentUrl: String? = null
    private var profileImageUrl: String? = null
    private var userDocumentListener: com.google.firebase.firestore.ListenerRegistration? = null

    private enum class UploadType {
        LICENSE, ID, PROFILE
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                when (uploadType) {
                    UploadType.LICENSE -> uploadLicenseImage(uri)
                    UploadType.ID -> uploadIdImage(uri)
                    UploadType.PROFILE -> uploadProfileImage(uri)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        loadUserProfile()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Profile image click handler
        binding.ivProfileImage.setOnClickListener {
            uploadType = UploadType.PROFILE
            openImagePicker()
        }

        binding.cardLicenseImage.setOnClickListener {
            uploadType = UploadType.LICENSE
            openImagePicker()
        }

        binding.cardIdImage.setOnClickListener {
            uploadType = UploadType.ID
            openImagePicker()
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        // Remove any existing listener
        userDocumentListener?.remove()

        // Use real-time listener to keep verification status in sync
        userDocumentListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE

                if (error != null) {
                    Toast.makeText(this@MyProfileActivity, "Error loading profile: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val baseUser = snapshot.toObject(User::class.java)?.copy(uid = snapshot.id)
                    if (baseUser != null) {
                        val rawIsVerified = snapshot.get("isVerified")
                        val coercedIsVerified = when (rawIsVerified) {
                            is Boolean -> rawIsVerified
                            is Number -> rawIsVerified.toInt() != 0
                            is String -> rawIsVerified.equals("true", ignoreCase = true) || rawIsVerified == "1"
                            else -> baseUser.isVerified
                        }
                        currentUser = baseUser.copy(isVerified = coercedIsVerified)
                        displayProfile(currentUser!!)
                    }
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up the listener when activity is destroyed
        userDocumentListener?.remove()
    }

    private fun displayProfile(user: User) {
        binding.tvFullName.text = user.fullName.ifEmpty { "Not set" }
        binding.tvEmail.text = user.email

        // Verification status
        if (user.isVerified) {
            binding.tvVerificationStatus.text = "✅ Verified"
            binding.tvVerificationStatus.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            binding.tvVerificationStatus.text = "⚠️ Not Verified"
            binding.tvVerificationStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
        }

        // Form fields
        binding.etFullName.setText(user.fullName)
        binding.etPhone.setText(user.phone)
        binding.etAddress.setText(user.address)
        binding.etLicenseNumber.setText(user.licenseNumber)

        // Profile image - make it clear it's clickable
        profileImageUrl = user.profileImageUrl
        if (user.profileImageUrl.isNotEmpty()) {
            binding.ivProfileImage.load(user.profileImageUrl) {
                crossfade(true)
            }
        } else {
            binding.ivProfileImage.setImageResource(R.drawable.ic_person)
        }

        // License image
        licenseImageUrl = user.licenseImageUrl
        if (user.licenseImageUrl.isNotEmpty()) {
            binding.ivLicenseImage.load(user.licenseImageUrl)
            binding.layoutLicensePlaceholder.visibility = View.GONE
        }

        // ID document
        idDocumentUrl = user.idDocumentUrl
        if (user.idDocumentUrl.isNotEmpty()) {
            binding.ivIdImage.load(user.idDocumentUrl)
            binding.layoutIdPlaceholder.visibility = View.GONE
        }
    }

    private fun uploadLicenseImage(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        binding.progressLicense.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val filename = "documents/$userId/license_${UUID.randomUUID()}.jpg"
                val storageRef = storage.reference.child(filename)

                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                licenseImageUrl = downloadUrl
                binding.ivLicenseImage.load(downloadUrl)
                binding.layoutLicensePlaceholder.visibility = View.GONE

                Toast.makeText(this@MyProfileActivity, "License uploaded", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@MyProfileActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressLicense.visibility = View.GONE
            }
        }
    }

    private fun uploadIdImage(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        binding.progressId.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val filename = "documents/$userId/id_${UUID.randomUUID()}.jpg"
                val storageRef = storage.reference.child(filename)

                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                idDocumentUrl = downloadUrl
                binding.ivIdImage.load(downloadUrl)
                binding.layoutIdPlaceholder.visibility = View.GONE

                Toast.makeText(this@MyProfileActivity, "ID document uploaded", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@MyProfileActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressId.visibility = View.GONE
            }
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val filename = "profile/$userId/profile_${UUID.randomUUID()}.jpg"
                val storageRef = storage.reference.child(filename)

                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                profileImageUrl = downloadUrl
                binding.ivProfileImage.load(downloadUrl) {
                    crossfade(true)
                }

                // Update the profile image URL in Firestore immediately
                firestore.collection("users").document(userId)
                    .update("profileImageUrl", downloadUrl).await()

                Toast.makeText(this@MyProfileActivity, "Profile photo uploaded", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@MyProfileActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun saveProfile() {
        val userId = auth.currentUser?.uid ?: return

        val fullName = binding.etFullName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val licenseNumber = binding.etLicenseNumber.text.toString().trim()

        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Full name is required"
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "fullName" to fullName,
                    "phone" to phone,
                    "address" to address,
                    "licenseNumber" to licenseNumber
                )

                licenseImageUrl?.let { updates["licenseImageUrl"] = it }
                idDocumentUrl?.let { updates["idDocumentUrl"] = it }
                profileImageUrl?.let { updates["profileImageUrl"] = it }

                firestore.collection("users").document(userId).update(updates).await()

                Toast.makeText(this@MyProfileActivity, "Profile saved successfully", Toast.LENGTH_SHORT).show()

                // Reload to show updated data
                loadUserProfile()

            } catch (e: Exception) {
                Toast.makeText(this@MyProfileActivity, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }
}
