package com.example.floodrisk.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.floodrisk.databinding.ActivityAddEditPlaceBinding
import com.example.floodrisk.location.LocationHelper
import com.example.floodrisk.viewmodel.MonitoredPlacesViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class AddEditPlaceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditPlaceBinding
    private val viewModel: MonitoredPlacesViewModel by viewModels()
    private lateinit var locationHelper: LocationHelper

    private var editingPlaceId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationHelper = LocationHelper(this)

        editingPlaceId = intent.getLongExtra("edit_place_id", 0L)
        if (editingPlaceId != 0L) {
            binding.toolbar.title = "Edit Monitored Place"
            binding.etPlaceName.setText(intent.getStringExtra("edit_place_name") ?: "")
            val lat = intent.getDoubleExtra("edit_place_lat", 0.0)
            val lon = intent.getDoubleExtra("edit_place_lon", 0.0)
            binding.etLatitude.setText(String.format(Locale.US, "%.6f", lat))
            binding.etLongitude.setText(String.format(Locale.US, "%.6f", lon))
            binding.cbSetDefault.isChecked = intent.getBooleanExtra("edit_place_default", false)
        } else {
            binding.toolbar.title = "Add Monitored Place"
        }

        setupToolbar()
        setupClickListeners()
        setupObservers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupClickListeners() {
        binding.btnUseCurrentLocation.setOnClickListener {
            lifecycleScope.launch {
                val loc = locationHelper.getCurrentLocation()
                if (loc != null) {
                    binding.etLatitude.setText(String.format(Locale.US, "%.6f", loc.latitude))
                    binding.etLongitude.setText(String.format(Locale.US, "%.6f", loc.longitude))
                    if (binding.etPlaceName.text.isNullOrEmpty()) {
                        binding.etPlaceName.setText("My Location")
                    }
                } else {
                    Toast.makeText(this@AddEditPlaceActivity, "Unable to detect current location.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etPlaceName.text.toString()
            val latStr = binding.etLatitude.text.toString()
            val lonStr = binding.etLongitude.text.toString()
            val isDefault = binding.cbSetDefault.isChecked

            val error = viewModel.validateInput(name, latStr, lonStr)
            if (error != null) {
                binding.tvErrorMessage.text = error
                binding.tvErrorMessage.visibility = View.VISIBLE
                return@setOnClickListener
            }

            binding.tvErrorMessage.visibility = View.GONE
            val lat = latStr.toDouble()
            val lon = lonStr.toDouble()

            if (editingPlaceId != 0L) {
                viewModel.updatePlace(editingPlaceId, name, lat, lon, isDefault)
            } else {
                viewModel.addPlace(name, lat, lon, isDefault)
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        viewModel.operationStatus.observe(this) { status ->
            if (status != null) {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
                viewModel.clearStatus()
                finish()
            }
        }
    }
}
