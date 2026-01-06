package com.example.drivewise.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.drivewise.Data.remote.FirebaseCarRepository
import com.example.drivewise.databinding.ActivityAddEditCarBinding
import com.example.drivewise.domain.model.Car
import com.example.drivewise.ui.viewmodel.CarEvent
import com.example.drivewise.ui.viewmodel.CarViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AddEditCarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditCarBinding
    private var carId: String? = null
    private var isEditMode = false
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private val storage = FirebaseStorage.getInstance()

    private val viewModel: CarViewModel by viewModels {
        CarViewModelFactory(
            FirebaseCarRepository(FirebaseFirestore.getInstance())
        )
    }

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                displaySelectedImage(uri)
            }
        }
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Permission denied. Cannot select image.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditCarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        carId = intent.getStringExtra("CAR_ID")
        isEditMode = carId != null

        setupViews()
        observeState()

        if (isEditMode) {
            binding.toolbar.title = "Edit Car"
            binding.btnSave.text = "Update Car"
            viewModel.loadCar(carId!!)
        }
    }

    private fun setupViews() {
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Setup dropdowns
        val transmissions = listOf("Automatic", "Manual", "CVT")
        val fuels = listOf("Petrol", "Diesel", "Electric", "Hybrid")

        binding.actvTransmission.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, transmissions)
        )
        binding.actvFuel.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, fuels)
        )

        binding.btnSelectImage.setOnClickListener {
            checkPermissionAndPickImage()
        }

        binding.etImageUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                loadImagePreview()
            }
        }

        binding.btnSave.setOnClickListener {
            saveCar()
        }
    }

    private fun checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openImagePicker()
            } else {
                permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // For older versions, no permission needed for ACTION_PICK
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private fun displaySelectedImage(uri: Uri) {
        binding.ivCarImage.load(uri) {
            crossfade(true)
        }
        binding.tvAddImage.visibility = View.GONE
        // Clear the URL field when a local image is selected
        binding.etImageUrl.setText("")
    }

    private fun loadImagePreview() {
        val url = binding.etImageUrl.text.toString().trim()
        if (url.isNotEmpty()) {
            binding.ivCarImage.load(url) {
                crossfade(true)
                listener(
                    onSuccess = { _, _ ->
                        binding.tvAddImage.visibility = View.GONE
                    },
                    onError = { _, _ ->
                        binding.tvAddImage.visibility = View.VISIBLE
                        binding.tvAddImage.text = "Failed to load image"
                    }
                )
            }
        }
    }

    private fun saveCar() {
        val brand = binding.etBrand.text.toString().trim()
        val model = binding.etModel.text.toString().trim()
        val yearText = binding.etYear.text.toString().trim()
        val transmission = binding.actvTransmission.text.toString().trim()
        val fuel = binding.actvFuel.text.toString().trim()
        val priceText = binding.etPrice.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val featuresText = binding.etFeatures.text.toString().trim()
        val imageUrl = binding.etImageUrl.text.toString().trim()
        val available = binding.switchAvailable.isChecked

        // Validation
        if (brand.isEmpty()) {
            binding.tilBrand.error = "Brand is required"
            return
        }
        if (model.isEmpty()) {
            binding.tilModel.error = "Model is required"
            return
        }
        if (yearText.isEmpty()) {
            binding.tilYear.error = "Year is required"
            return
        }
        if (transmission.isEmpty()) {
            binding.tilTransmission.error = "Transmission is required"
            return
        }
        if (fuel.isEmpty()) {
            binding.tilFuel.error = "Fuel type is required"
            return
        }
        if (priceText.isEmpty()) {
            binding.tilPrice.error = "Price is required"
            return
        }

        val year = yearText.toIntOrNull() ?: run {
            binding.tilYear.error = "Invalid year"
            return
        }

        val price = priceText.toDoubleOrNull() ?: run {
            binding.tilPrice.error = "Invalid price"
            return
        }

        val features = if (featuresText.isNotEmpty()) {
            featuresText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        // If a new image is selected, upload it first
        if (selectedImageUri != null) {
            uploadImageAndSaveCar(brand, model, year, transmission, fuel, price, description, features, available)
        } else {
            // Use existing URL or uploaded URL
            val finalImageUrl = uploadedImageUrl ?: imageUrl
            saveCarToFirestore(brand, model, year, transmission, fuel, price, description, features, available, finalImageUrl)
        }
    }

    private fun uploadImageAndSaveCar(
        brand: String,
        model: String,
        year: Int,
        transmission: String,
        fuel: String,
        price: Double,
        description: String,
        features: List<String>,
        available: Boolean
    ) {
        val uri = selectedImageUri ?: return

        // Show upload progress
        binding.progressBarUpload.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false
        binding.btnSelectImage.isEnabled = false

        lifecycleScope.launch {
            try {
                // Create a unique filename
                val filename = "cars/${UUID.randomUUID()}.jpg"
                val storageRef = storage.reference.child(filename)

                // Upload the file
                storageRef.putFile(uri).await()

                // Get the download URL
                val downloadUrl = storageRef.downloadUrl.await().toString()
                uploadedImageUrl = downloadUrl

                // Hide upload progress
                binding.progressBarUpload.visibility = View.GONE
                binding.btnSave.isEnabled = true
                binding.btnSelectImage.isEnabled = true

                // Save car with the uploaded image URL
                saveCarToFirestore(brand, model, year, transmission, fuel, price, description, features, available, downloadUrl)

            } catch (e: Exception) {
                binding.progressBarUpload.visibility = View.GONE
                binding.btnSave.isEnabled = true
                binding.btnSelectImage.isEnabled = true
                Toast.makeText(this@AddEditCarActivity, "Failed to upload image: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveCarToFirestore(
        brand: String,
        model: String,
        year: Int,
        transmission: String,
        fuel: String,
        price: Double,
        description: String,
        features: List<String>,
        available: Boolean,
        imageUrl: String
    ) {
        val car = Car(
            id = carId ?: "",
            brand = brand,
            model = model,
            year = year,
            transmission = transmission,
            fuel = fuel,
            dayPrice = price,
            imageUrl = imageUrl,
            available = available,
            features = features,
            description = description
        )

        if (isEditMode) {
            viewModel.updateCar(car)
        } else {
            viewModel.addCar(car)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.btnSave.isEnabled = !state.isLoading

                    state.selectedCar?.let { car ->
                        if (isEditMode) {
                            populateForm(car)
                        }
                    }

                    state.errorMessage?.let { error ->
                        binding.tvError.text = error
                        binding.tvError.visibility = View.VISIBLE
                        viewModel.clearError()
                    } ?: run {
                        binding.tvError.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    when (event) {
                        is CarEvent.CarAdded -> {
                            Toast.makeText(this@AddEditCarActivity, "Car added successfully", Toast.LENGTH_SHORT).show()
                            viewModel.clearEvent()
                            finish()
                        }
                        is CarEvent.CarUpdated -> {
                            Toast.makeText(this@AddEditCarActivity, "Car updated successfully", Toast.LENGTH_SHORT).show()
                            viewModel.clearEvent()
                            finish()
                        }
                        is CarEvent.Error -> {
                            Toast.makeText(this@AddEditCarActivity, event.message, Toast.LENGTH_SHORT).show()
                            viewModel.clearEvent()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun populateForm(car: Car) {
        binding.etBrand.setText(car.brand)
        binding.etModel.setText(car.model)
        binding.etYear.setText(car.year.toString())
        binding.actvTransmission.setText(car.transmission, false)
        binding.actvFuel.setText(car.fuel, false)
        binding.etPrice.setText(car.dayPrice.toString())
        binding.etDescription.setText(car.description)
        binding.etFeatures.setText(car.features.joinToString(", "))
        binding.etImageUrl.setText(car.imageUrl)
        binding.switchAvailable.isChecked = car.available

        if (car.imageUrl.isNotEmpty()) {
            binding.ivCarImage.load(car.imageUrl)
            binding.tvAddImage.visibility = View.GONE
            uploadedImageUrl = car.imageUrl  // Store the existing image URL
        }
    }
}

