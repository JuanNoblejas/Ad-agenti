package com.wanda.app.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.wanda.app.WandaApplication
import com.wanda.app.databinding.FragmentHomeBinding
import com.wanda.app.geofencing.LocationTrackingService
import com.wanda.app.utils.PermissionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            getCurrentLocationAndSetHome()
        } else {
            Toast.makeText(
                requireContext(),
                "Location permission is required to set your home",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startTracking()
        } else {
            Toast.makeText(
                requireContext(),
                "Background location permission is required for tracking",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()
        setupButtons()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun observeViewModel() {
        viewModel.latestRecord.observe(viewLifecycleOwner) { record ->
            if (record == null) {
                binding.tvStatus.text = "No location data"
                binding.tvLastEvent.text = "No events recorded yet"
            } else {
                when (record.type) {
                    "ENTRY" -> {
                        binding.tvStatus.text = "🏠 You are at home"
                    }
                    "EXIT" -> {
                        binding.tvStatus.text = "🚶 You have left"
                    }
                    else -> {
                        binding.tvStatus.text = "Unknown status"
                    }
                }
                val formattedDate = dateFormat.format(Date(record.timestamp))
                binding.tvLastEvent.text = "Last event: $formattedDate"
            }
        }
    }

    private fun setupButtons() {
        binding.btnSetHome.setOnClickListener {
            if (PermissionHelper.hasLocationPermission(requireContext())) {
                getCurrentLocationAndSetHome()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }

        binding.btnEditHome.setOnClickListener {
            showEditHomeDialog()
        }

        binding.btnDeleteHome.setOnClickListener {
            showDeleteHomeDialog()
        }

        binding.btnToggleTracking.setOnClickListener {
            val prefs = viewModel.getPreferencesManager()
            if (prefs.isTrackingEnabled) {
                stopTracking()
            } else {
                requestTrackingStart()
            }
        }
    }

    private fun updateUI() {
        val prefs = viewModel.getPreferencesManager()

        // Update tracking status
        if (prefs.isTrackingEnabled) {
            binding.tvTrackingStatus.text = "📡 Tracking: ACTIVE"
            binding.btnToggleTracking.text = "Stop tracking"
        } else {
            binding.tvTrackingStatus.text = "📡 Tracking: INACTIVE"
            binding.btnToggleTracking.text = "Start tracking"
        }

        // Update home location status
        if (prefs.isHomeSet) {
            val lat = String.format(Locale.US, "%.5f", prefs.homeLatitude)
            val lng = String.format(Locale.US, "%.5f", prefs.homeLongitude)
            val radius = prefs.geofenceRadius.toInt()
            binding.tvHomeLocation.text = "🏠 Home: $lat, $lng (radius: ${radius}m)"
            binding.btnEditHome.visibility = View.VISIBLE
            binding.btnDeleteHome.visibility = View.VISIBLE
        } else {
            binding.tvHomeLocation.text = "🏠 Home: Not set"
            binding.btnEditHome.visibility = View.GONE
            binding.btnDeleteHome.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndSetHome() {
        if (!PermissionHelper.hasLocationPermission(requireContext())) {
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        val cancellationToken = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                val prefs = viewModel.getPreferencesManager()
                prefs.setHomeLocation(location.latitude, location.longitude)
                Toast.makeText(
                    requireContext(),
                    "Home location set successfully!",
                    Toast.LENGTH_LONG
                ).show()
                updateUI()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Could not get location. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(
                requireContext(),
                "Error getting location: ${exception.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showEditHomeDialog() {
        val prefs = viewModel.getPreferencesManager()

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val etLatitude = EditText(requireContext()).apply {
            hint = "Latitude"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(if (prefs.isHomeSet) prefs.homeLatitude.toString() else "")
        }

        val etLongitude = EditText(requireContext()).apply {
            hint = "Longitude"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(if (prefs.isHomeSet) prefs.homeLongitude.toString() else "")
        }

        val etRadius = EditText(requireContext()).apply {
            hint = "Radius (meters)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(prefs.geofenceRadius.toInt().toString())
        }

        layout.addView(etLatitude)
        layout.addView(etLongitude)
        layout.addView(etRadius)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit home location")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                try {
                    val lat = etLatitude.text.toString().toDouble()
                    val lng = etLongitude.text.toString().toDouble()
                    val radius = etRadius.text.toString().toFloat()

                    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                        Toast.makeText(requireContext(), "Invalid coordinates", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    if (radius < 50 || radius > 5000) {
                        Toast.makeText(requireContext(), "Radius must be between 50 and 5000 meters", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    prefs.setHomeLocation(lat, lng)
                    prefs.geofenceRadius = radius
                    Toast.makeText(requireContext(), "Location updated!", Toast.LENGTH_SHORT).show()
                    updateUI()

                    // If tracking is active, restart service to apply new geofence
                    if (prefs.isTrackingEnabled) {
                        LocationTrackingService.stop(requireContext())
                        LocationTrackingService.start(requireContext())
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(requireContext(), "Please enter valid numeric values", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Shows a confirmation dialog before deleting the home location.
     * On confirmation: stops tracking, removes the system geofence, and
     * clears the coordinates from SharedPreferences to return to the initial state.
     */
    private fun showDeleteHomeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete home location?")
            .setMessage("The saved location will be removed and tracking will be stopped. You can set a new location afterwards.")
            .setPositiveButton("Yes, delete") { _, _ ->
                val prefs = viewModel.getPreferencesManager()

                // 1. Stop the tracking service if it is active
                if (prefs.isTrackingEnabled) {
                    LocationTrackingService.stop(requireContext())
                }

                // 2. Remove the geofence from Google Play Services
                val geofenceHelper = com.wanda.app.geofencing.GeofenceHelper(requireContext())
                geofenceHelper.removeGeofence(
                    onSuccess = {
                        // Runs on network/background thread, do not touch UI here
                    },
                    onFailure = { _ ->
                        // Even if deletion fails in Google, we continue cleaning locally
                    }
                )

                // 3. Clear coordinates and flags in SharedPreferences
                prefs.clearHomeLocation()

                // 4. Update the interface
                Toast.makeText(
                    requireContext(),
                    "Home location deleted. You can now set a new one.",
                    Toast.LENGTH_LONG
                ).show()
                updateUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestTrackingStart() {
        val prefs = viewModel.getPreferencesManager()
        if (!prefs.isHomeSet) {
            Toast.makeText(
                requireContext(),
                "You must set your home location first",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!PermissionHelper.hasLocationPermission(requireContext())) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !PermissionHelper.hasBackgroundLocationPermission(requireContext())
        ) {
            AlertDialog.Builder(requireContext())
                .setTitle("Permission required")
                .setMessage("To track your location in the background, we need the \"Allow all the time\" location permission. On the next screen, select \"Allow all the time\".")
                .setPositiveButton("Continue") { _, _ ->
                    backgroundLocationPermissionLauncher.launch(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionHelper.hasNotificationPermission(requireContext())
        ) {
            requestNotificationPermissionAndStartTracking()
            return
        }

        startTracking()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Start tracking regardless - notifications are nice-to-have
        startTracking()
    }

    private fun requestNotificationPermissionAndStartTracking() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startTracking()
        }
    }

    private fun startTracking() {
        val prefs = viewModel.getPreferencesManager()
        prefs.isTrackingEnabled = true
        LocationTrackingService.start(requireContext())
        Toast.makeText(requireContext(), "Tracking started", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun stopTracking() {
        val prefs = viewModel.getPreferencesManager()
        prefs.isTrackingEnabled = false
        LocationTrackingService.stop(requireContext())
        Toast.makeText(requireContext(), "Tracking stopped", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
