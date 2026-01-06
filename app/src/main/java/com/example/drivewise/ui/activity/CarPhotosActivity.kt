package com.example.drivewise.ui.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.drivewise.databinding.ActivityCarPhotosBinding
import com.example.drivewise.ui.adapter.PhotoAdapter
import com.google.android.material.tabs.TabLayout

class CarPhotosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCarPhotosBinding
    private lateinit var photoAdapter: PhotoAdapter

    private var pickupPhotos: List<String> = emptyList()
    private var returnPhotos: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarPhotosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bookingId = intent.getStringExtra("BOOKING_ID") ?: ""
        val carName = intent.getStringExtra("CAR_NAME") ?: ""
        pickupPhotos = intent.getStringArrayListExtra("PICKUP_PHOTOS") ?: emptyList()
        returnPhotos = intent.getStringArrayListExtra("RETURN_PHOTOS") ?: emptyList()

        setupViews(bookingId, carName)
    }

    private fun setupViews(bookingId: String, carName: String) {
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tvBookingInfo.text = "Booking #${bookingId.takeLast(6)}"
        binding.tvCarInfo.text = carName

        photoAdapter = PhotoAdapter { photoUrl ->
            // Could open full-screen photo viewer
        }

        binding.rvPhotos.apply {
            layoutManager = GridLayoutManager(this@CarPhotosActivity, 2)
            adapter = photoAdapter
        }

        // Setup tabs
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Pickup Photos (${pickupPhotos.size})"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Return Photos (${returnPhotos.size})"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showPhotos(pickupPhotos)
                    1 -> showPhotos(returnPhotos)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Show pickup photos by default
        showPhotos(pickupPhotos)
    }

    private fun showPhotos(photos: List<String>) {
        if (photos.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvPhotos.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvPhotos.visibility = View.VISIBLE
            photoAdapter.submitList(photos)
        }
    }
}

