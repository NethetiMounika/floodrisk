package com.example.floodrisk.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.floodrisk.adapter.MonitoredPlacesAdapter
import com.example.floodrisk.databinding.ActivityMonitoredPlacesBinding
import com.example.floodrisk.model.MonitoredPlace
import com.example.floodrisk.viewmodel.MonitoredPlacesViewModel

class MonitoredPlacesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMonitoredPlacesBinding
    private val viewModel: MonitoredPlacesViewModel by viewModels()
    private lateinit var adapter: MonitoredPlacesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitoredPlacesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = MonitoredPlacesAdapter(
            onSelect = { place ->
                val resultIntent = Intent().apply {
                    putExtra("selected_place_id", place.id)
                    putExtra("selected_place_name", place.name)
                    putExtra("selected_place_lat", place.latitude)
                    putExtra("selected_place_lon", place.longitude)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            },
            onEdit = { place ->
                val intent = Intent(this, AddEditPlaceActivity::class.java).apply {
                    putExtra("edit_place_id", place.id)
                    putExtra("edit_place_name", place.name)
                    putExtra("edit_place_lat", place.latitude)
                    putExtra("edit_place_lon", place.longitude)
                    putExtra("edit_place_default", place.isDefault)
                }
                startActivity(intent)
            },
            onDelete = { place ->
                viewModel.deletePlace(place)
            }
        )

        binding.rvPlaces.layoutManager = LinearLayoutManager(this)
        binding.rvPlaces.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.places.observe(this) { places ->
            adapter.submitList(places)
            if (places.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvPlaces.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.rvPlaces.visibility = View.VISIBLE
            }
        }

        viewModel.operationStatus.observe(this) { status ->
            if (status != null) {
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
                viewModel.clearStatus()
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddPlace.setOnClickListener {
            val intent = Intent(this, AddEditPlaceActivity::class.java)
            startActivity(intent)
        }
    }
}
