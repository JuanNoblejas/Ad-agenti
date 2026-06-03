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

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES"))

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
                "Se necesita permiso de ubicación para establecer tu casa",
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
                "Se necesita permiso de ubicación en segundo plano para el rastreo",
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
                binding.tvStatus.text = "Sin datos de ubicación"
                binding.tvLastEvent.text = "Aún no se han registrado eventos"
            } else {
                when (record.type) {
                    "ENTRADA" -> {
                        binding.tvStatus.text = "🏠 Estás en casa"
                    }
                    "SALIDA" -> {
                        binding.tvStatus.text = "🚶 Has salido"
                    }
                    else -> {
                        binding.tvStatus.text = "Estado desconocido"
                    }
                }
                val formattedDate = dateFormat.format(Date(record.timestamp))
                binding.tvLastEvent.text = "Último evento: $formattedDate"
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
            binding.tvTrackingStatus.text = "📡 Rastreo: ACTIVO"
            binding.btnToggleTracking.text = "Detener rastreo"
        } else {
            binding.tvTrackingStatus.text = "📡 Rastreo: INACTIVO"
            binding.btnToggleTracking.text = "Iniciar rastreo"
        }

        // Update home location status
        if (prefs.isHomeSet) {
            val lat = String.format(Locale.US, "%.5f", prefs.homeLatitude)
            val lng = String.format(Locale.US, "%.5f", prefs.homeLongitude)
            val radius = prefs.geofenceRadius.toInt()
            binding.tvHomeLocation.text = "🏠 Casa: $lat, $lng (radio: ${radius}m)"
            binding.btnEditHome.visibility = View.VISIBLE
            binding.btnDeleteHome.visibility = View.VISIBLE
        } else {
            binding.tvHomeLocation.text = "🏠 Casa: No establecida"
            binding.btnEditHome.visibility = View.GONE
            binding.btnDeleteHome.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndSetHome() {
        if (!PermissionHelper.hasLocationPermission(requireContext())) {
            Toast.makeText(requireContext(), "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show()
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
                    "¡Ubicación de casa establecida correctamente!",
                    Toast.LENGTH_LONG
                ).show()
                updateUI()
            } else {
                Toast.makeText(
                    requireContext(),
                    "No se pudo obtener la ubicación. Inténtalo de nuevo.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(
                requireContext(),
                "Error al obtener ubicación: ${exception.message}",
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
            hint = "Latitud"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(if (prefs.isHomeSet) prefs.homeLatitude.toString() else "")
        }

        val etLongitude = EditText(requireContext()).apply {
            hint = "Longitud"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(if (prefs.isHomeSet) prefs.homeLongitude.toString() else "")
        }

        val etRadius = EditText(requireContext()).apply {
            hint = "Radio (metros)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(prefs.geofenceRadius.toInt().toString())
        }

        layout.addView(etLatitude)
        layout.addView(etLongitude)
        layout.addView(etRadius)

        AlertDialog.Builder(requireContext())
            .setTitle("Editar ubicación de casa")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                try {
                    val lat = etLatitude.text.toString().toDouble()
                    val lng = etLongitude.text.toString().toDouble()
                    val radius = etRadius.text.toString().toFloat()

                    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
                        Toast.makeText(requireContext(), "Coordenadas inválidas", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    if (radius < 50 || radius > 5000) {
                        Toast.makeText(requireContext(), "El radio debe estar entre 50 y 5000 metros", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    prefs.setHomeLocation(lat, lng)
                    prefs.geofenceRadius = radius
                    Toast.makeText(requireContext(), "¡Ubicación actualizada!", Toast.LENGTH_SHORT).show()
                    updateUI()

                    // If tracking is active, restart service to apply new geofence
                    if (prefs.isTrackingEnabled) {
                        LocationTrackingService.stop(requireContext())
                        LocationTrackingService.start(requireContext())
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(requireContext(), "Por favor, introduce valores numéricos válidos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Muestra un diálogo de confirmación antes de borrar la ubicación de casa.
     * Al confirmar: detiene el rastreo, elimina la geovalla del sistema y
     * limpia las coordenadas de SharedPreferences para volver al estado inicial.
     */
    private fun showDeleteHomeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("¿Borrar ubicación de casa?")
            .setMessage("Se eliminará la ubicación guardada y se detendrá el rastreo. Podrás registrar una nueva ubicación después.")
            .setPositiveButton("Sí, borrar") { _, _ ->
                val prefs = viewModel.getPreferencesManager()

                // 1. Detener el servicio de rastreo si está activo
                if (prefs.isTrackingEnabled) {
                    LocationTrackingService.stop(requireContext())
                }

                // 2. Eliminar la geovalla del sistema de Google Play Services
                val geofenceHelper = com.wanda.app.geofencing.GeofenceHelper(requireContext())
                geofenceHelper.removeGeofence(
                    onSuccess = {
                        // Se ejecuta en hilo de red/background, no tocar UI aquí
                    },
                    onFailure = { _ ->
                        // Aunque falle el borrado en Google, continuamos limpiando localmente
                    }
                )

                // 3. Limpiar las coordenadas y flags en SharedPreferences
                prefs.clearHomeLocation()

                // 4. Actualizar la interfaz
                Toast.makeText(
                    requireContext(),
                    "Ubicación de casa eliminada. Ya puedes registrar una nueva.",
                    Toast.LENGTH_LONG
                ).show()
                updateUI()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun requestTrackingStart() {
        val prefs = viewModel.getPreferencesManager()
        if (!prefs.isHomeSet) {
            Toast.makeText(
                requireContext(),
                "Primero debes establecer la ubicación de tu casa",
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
                .setTitle("Permiso necesario")
                .setMessage("Para rastrear tu ubicación en segundo plano, necesitamos el permiso de ubicación \"Permitir siempre\". En la siguiente pantalla, selecciona \"Permitir todo el tiempo\".")
                .setPositiveButton("Continuar") { _, _ ->
                    backgroundLocationPermissionLauncher.launch(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                }
                .setNegativeButton("Cancelar", null)
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
        Toast.makeText(requireContext(), "Rastreo iniciado", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun stopTracking() {
        val prefs = viewModel.getPreferencesManager()
        prefs.isTrackingEnabled = false
        LocationTrackingService.stop(requireContext())
        Toast.makeText(requireContext(), "Rastreo detenido", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
