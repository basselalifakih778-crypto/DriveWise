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
import androidx.recyclerview.widget.GridLayoutManager
import com.example.drivewise.databinding.ActivityUploadPhotosBinding
import com.example.drivewise.domain.model.Booking
import com.example.drivewise.ui.adapter.UploadedPhotoAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UploadPhotosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadPhotosBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var bookingId: String = ""
    private var carName: String = ""
    private var isPickupPhotos: Boolean = true

    private val pickupPhotos = mutableListOf<String>()
    private val returnPhotos = mutableListOf<String>()
    private lateinit var photoAdapter: UploadedPhotoAdapter

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadPhoto(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadPhotosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookingId = intent.getStringExtra("BOOKING_ID") ?: ""
        carName = intent.getStringExtra("CAR_NAME") ?: ""

        if (bookingId.isEmpty()) {
            Toast.makeText(this, "Invalid booking", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        loadExistingPhotos()
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.tvCarName.text = carName

        photoAdapter = UploadedPhotoAdapter { photoUrl ->
            // Remove photo
            if (isPickupPhotos) {
                pickupPhotos.remove(photoUrl)
            } else {
                returnPhotos.remove(photoUrl)
            }
            updatePhotoList()
        }

        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(this@UploadPhotosActivity, 3)
            adapter = photoAdapter
        }

        binding.chipGroupPhotoType.setOnCheckedStateChangeListener { _, checkedIds ->
            isPickupPhotos = checkedIds.contains(binding.chipPickup.id)
            updatePhotoList()
        }

        binding.btnAddPhoto.setOnClickListener {
            openImagePicker()
        }

        binding.btnSave.setOnClickListener {
            savePhotos()
        }
    }

    private fun loadExistingPhotos() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val snapshot = firestore.collection("bookings").document(bookingId).get().await()
                val booking = snapshot.toObject(Booking::class.java)

                booking?.let {
                    pickupPhotos.clear()
                    pickupPhotos.addAll(it.pickupPhotos)
                    returnPhotos.clear()
                    returnPhotos.addAll(it.returnPhotos)
                    updatePhotoList()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UploadPhotosActivity, "Error loading photos: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updatePhotoList() {
        val currentPhotos = if (isPickupPhotos) pickupPhotos else returnPhotos
        photoAdapter.submitList(currentPhotos.toList())

        if (currentPhotos.isEmpty()) {
            binding.tvNoPhotos.visibility = View.VISIBLE
            binding.rvPhotos.visibility = View.GONE
        } else {
            binding.tvNoPhotos.visibility = View.GONE
            binding.rvPhotos.visibility = View.VISIBLE
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private fun uploadPhoto(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnAddPhoto.isEnabled = false

        lifecycleScope.launch {
            try {
                val photoType = if (isPickupPhotos) "pickup" else "return"
                val filename = "bookings/$bookingId/$photoType/${UUID.randomUUID()}.jpg"
                val storageRef = storage.reference.child(filename)

                storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                if (isPickupPhotos) {
                    pickupPhotos.add(downloadUrl)
                } else {
                    returnPhotos.add(downloadUrl)
                }

                updatePhotoList()
                Toast.makeText(this@UploadPhotosActivity, "Photo uploaded", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@UploadPhotosActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnAddPhoto.isEnabled = true
            }
        }
    }

    private fun savePhotos() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                val updates = mapOf(
                    "pickupPhotos" to pickupPhotos.toList(),
                    "returnPhotos" to returnPhotos.toList()
                )

                firestore.collection("bookings").document(bookingId).update(updates).await()

                Toast.makeText(this@UploadPhotosActivity, "Photos saved successfully", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                Toast.makeText(this@UploadPhotosActivity, "Error saving photos: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
        }
    }
}

