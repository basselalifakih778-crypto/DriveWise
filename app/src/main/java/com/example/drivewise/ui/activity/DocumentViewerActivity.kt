package com.example.drivewise.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.example.drivewise.databinding.ActivityDocumentViewerBinding

class DocumentViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val documentUrl = intent.getStringExtra("DOCUMENT_URL") ?: ""
        val documentTitle = intent.getStringExtra("DOCUMENT_TITLE") ?: "Document"

        binding.toolbar.title = documentTitle
        binding.toolbar.setNavigationOnClickListener { finish() }

        if (documentUrl.isNotEmpty()) {
            binding.ivDocument.load(documentUrl) {
                crossfade(true)
            }
        }
    }
}

