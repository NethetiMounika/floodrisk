package com.example.floodrisk.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.load
import com.example.floodrisk.R
import com.example.floodrisk.adapter.ForecastAdapter
import com.example.floodrisk.databinding.ActivityMainBinding
import com.example.floodrisk.model.FloodForecastDay
import com.example.floodrisk.model.MonitoredPlace
import com.example.floodrisk.model.RiskLevel
import com.example.floodrisk.viewmodel.MainViewModel
import com.example.floodrisk.worker.FloodAlertWorker
import java.util.Locale
import java.util.concurrent.TimeUnit

open class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var forecastAdapter: ForecastAdapter

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.detectCurrentLocationAndFetch()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val placesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data != null && data.hasExtra("selected_place_id")) {
                val place = MonitoredPlace(
                    id = data.getLongExtra("selected_place_id", 0L),
                    name = data.getStringExtra("selected_place_name") ?: "Selected Place",
                    latitude = data.getDoubleExtra("selected_place_lat", 0.0),
                    longitude = data.getDoubleExtra("selected_place_lon", 0.0)
                )
                viewModel.selectPlace(place)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()

        checkAndRequestPermissions()
        scheduleBackgroundAlertWorker()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserSession()
    }

    private fun setupRecyclerView() {
        forecastAdapter = ForecastAdapter()
        binding.rvForecast.layoutManager = LinearLayoutManager(this)
        binding.rvForecast.adapter = forecastAdapter
    }

    private fun setupObservers() {
        viewModel.currentUser.observe(this) { user ->
            if (user == null) {
                navigateToLogin()
                return@observe
            }
            binding.tvUserName.text = user.displayName ?: "User"
            if (!user.photoUrl.isNullOrEmpty()) {
                binding.ivUserProfile.load(user.photoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_river)
                    error(R.drawable.ic_river)
                }
            } else {
                binding.ivUserProfile.setImageResource(R.drawable.ic_river)
            }
        }

        viewModel.selectedPlace.observe(this) { place ->
            if (place != null) {
                binding.tvCurrentPlaceName.text = place.name
                binding.tvLocationCoords.text = String.format(Locale.US, "Lat: %.4f, Lon: %.4f", place.latitude, place.longitude)
            }
        }

        viewModel.todayStatus.observe(this) { today ->
            bindTodayStatus(today)
        }

        viewModel.peakDay.observe(this) { peak ->
            bindPeakDay(peak)
        }

        viewModel.forecastDays.observe(this) { days ->
            forecastAdapter.submitList(days)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { error ->
            if (error != null) {
                binding.tvErrorMessage.text = error
                binding.layoutError.visibility = View.VISIBLE
            } else {
                binding.layoutError.visibility = View.GONE
            }
        }
    }

    private fun bindTodayStatus(today: FloodForecastDay?) {
        if (today == null) {
            binding.tvTodayDischarge.text = "N/A"
            binding.tvTodayRiskBadge.text = "UNAVAILABLE"
            binding.tvTodayMean.text = "Today's Mean: N/A"
            binding.tvTodayMax.text = "Today's Max: N/A"
            setRiskBadgeStyle(binding.tvTodayRiskBadge, RiskLevel.UNAVAILABLE)
            return
        }

        binding.tvTodayDischarge.text = today.riverDischarge?.let { String.format(Locale.US, "%.2f m³/s", it) } ?: "N/A"
        binding.tvTodayMean.text = today.riverDischargeMean?.let { String.format(Locale.US, "Today's Mean: %.2f m³/s", it) } ?: "Today's Mean: N/A"
        binding.tvTodayMax.text = today.riverDischargeMax?.let { String.format(Locale.US, "Today's Max: %.2f m³/s", it) } ?: "Today's Max: N/A"
        binding.tvTodayRiskBadge.text = today.riskLevel.displayName
        setRiskBadgeStyle(binding.tvTodayRiskBadge, today.riskLevel)
    }

    private fun bindPeakDay(peak: FloodForecastDay?) {
        if (peak == null) {
            binding.tvPeakDate.text = "Peak Day: N/A"
            binding.tvPeakValue.text = "N/A"
            binding.tvPeakRiskBadge.text = "UNAVAILABLE"
            setRiskBadgeStyle(binding.tvPeakRiskBadge, RiskLevel.UNAVAILABLE)
            return
        }

        binding.tvPeakDate.text = "Peak Day: ${peak.date}"
        val peakVal = peak.riverDischargeMax ?: peak.riverDischarge
        binding.tvPeakValue.text = peakVal?.let { String.format(Locale.US, "%.2f m³/s", it) } ?: "N/A"
        binding.tvPeakRiskBadge.text = peak.riskLevel.displayName
        setRiskBadgeStyle(binding.tvPeakRiskBadge, peak.riskLevel)
    }

    private fun setRiskBadgeStyle(badgeView: android.widget.TextView, risk: RiskLevel) {
        val (bgColor, textColor) = when (risk) {
            RiskLevel.NORMAL -> Pair(Color.parseColor("#E8F5E9"), Color.parseColor("#2E7D32"))
            RiskLevel.WATCH -> Pair(Color.parseColor("#FFFDE7"), Color.parseColor("#F57F17"))
            RiskLevel.ALERT -> Pair(Color.parseColor("#FFEBEE"), Color.parseColor("#C62828"))
            RiskLevel.UNAVAILABLE -> Pair(Color.parseColor("#EEEEEE"), Color.parseColor("#757575"))
        }
        badgeView.setTextColor(textColor)
        badgeView.setBackgroundColor(bgColor)
    }

    private fun setupClickListeners() {
        binding.btnUseGpsLocation.setOnClickListener {
            checkAndRequestLocationPermissions()
        }

        binding.btnManagePlaces.setOnClickListener {
            val intent = Intent(this, MonitoredPlacesActivity::class.java)
            placesLauncher.launch(intent)
        }

        binding.btnRefresh.setOnClickListener {
            viewModel.refresh()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.refresh()
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            navigateToLogin()
        }
    }

    private fun checkAndRequestPermissions() {
        checkAndRequestLocationPermissions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkAndRequestLocationPermissions() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            viewModel.detectCurrentLocationAndFetch()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun scheduleBackgroundAlertWorker() {
        val alertWorkRequest = PeriodicWorkRequestBuilder<FloodAlertWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "RiverFloodAlertWork",
            ExistingPeriodicWorkPolicy.KEEP,
            alertWorkRequest
        )
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
