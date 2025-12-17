package com.anandnalya.vehicletracker.ui.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anandnalya.vehicletracker.data.model.HomeLocation
import com.anandnalya.vehicletracker.data.model.VehicleConfig
import com.anandnalya.vehicletracker.data.model.VehicleStatus
import com.anandnalya.vehicletracker.data.repository.Result
import com.anandnalya.vehicletracker.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class VehicleTrackerUiState(
    val vehicleStatus: VehicleStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val vehicleConfig: VehicleConfig = VehicleConfig.DEFAULT,
    val vehicles: List<VehicleConfig> = listOf(VehicleConfig.DEFAULT),
    val isAutoRefreshEnabled: Boolean = true,
    val refreshIntervalSeconds: Int = 30, // 0 means disabled
    val lastRefreshTime: Long = 0L,
    val isSessionInitialized: Boolean = false,
    val homeLocation: HomeLocation = HomeLocation.DEFAULT,
    val distanceToHome: Double? = null, // in km
    val proximityAlertEnabled: Boolean = true,
    val proximityAlertDistance: Double = 1.0, // in km
    val hasTriggeredProximityAlert: Boolean = false,
    val alertSoundUri: String? = null, // null means default notification sound
    val alertDurationSeconds: Int = 5,
    val alertVibrationEnabled: Boolean = true,
    val isAlertPlaying: Boolean = false
)

@HiltViewModel
class VehicleTrackerViewModel @Inject constructor(
    private val repository: VehicleRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleTrackerUiState())
    val uiState: StateFlow<VehicleTrackerUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        val REFRESH_INTERVALS = listOf(0, 10, 15, 30, 60, 120) // 0 = disabled, others in seconds
    }

    init {
        loadSavedConfig()
        startAutoRefresh()
    }

    private fun loadSavedConfig() {
        viewModelScope.launch {
            // Load vehicles list
            repository.getVehicles().collect { vehicles ->
                _uiState.value = _uiState.value.copy(vehicles = vehicles)
            }
        }

        viewModelScope.launch {
            // Load selected vehicle
            val selectedVehicle = repository.getSelectedVehicle()
            _uiState.value = _uiState.value.copy(vehicleConfig = selectedVehicle)

            // Fetch initial data
            refreshVehicleStatus()
        }

        viewModelScope.launch {
            // Load home location
            repository.getHomeLocation().collect { home ->
                _uiState.value = _uiState.value.copy(homeLocation = home)
                updateDistanceToHome()
            }
        }

        viewModelScope.launch {
            // Load proximity alert settings
            repository.getProximityAlertEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(proximityAlertEnabled = enabled)
            }
        }

        viewModelScope.launch {
            repository.getProximityAlertDistance().collect { distance ->
                _uiState.value = _uiState.value.copy(proximityAlertDistance = distance)
            }
        }

        viewModelScope.launch {
            repository.getRefreshInterval().collect { interval ->
                _uiState.value = _uiState.value.copy(refreshIntervalSeconds = interval)
                // Restart auto refresh with new interval
                if (_uiState.value.isAutoRefreshEnabled) {
                    startAutoRefresh()
                }
            }
        }

        viewModelScope.launch {
            repository.getAlertSoundUri().collect { uri ->
                _uiState.value = _uiState.value.copy(alertSoundUri = uri)
            }
        }

        viewModelScope.launch {
            repository.getAlertDuration().collect { duration ->
                _uiState.value = _uiState.value.copy(alertDurationSeconds = duration)
            }
        }

        viewModelScope.launch {
            repository.getAlertVibrationEnabled().collect { enabled ->
                _uiState.value = _uiState.value.copy(alertVibrationEnabled = enabled)
            }
        }
    }

    fun refreshVehicleStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = repository.getVehicleStatus(_uiState.value.vehicleConfig)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        vehicleStatus = result.data,
                        isLoading = false,
                        error = null,
                        lastRefreshTime = System.currentTimeMillis(),
                        isSessionInitialized = true
                    )
                    updateDistanceToHome()
                    checkProximityAlert()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    private fun updateDistanceToHome() {
        val status = _uiState.value.vehicleStatus ?: return
        val home = _uiState.value.homeLocation

        val distance = calculateDistance(
            status.getLatitudeDouble(),
            status.getLongitudeDouble(),
            home.latitude,
            home.longitude
        )

        _uiState.value = _uiState.value.copy(distanceToHome = distance)
    }

    private fun checkProximityAlert() {
        val state = _uiState.value
        val distance = state.distanceToHome ?: return

        if (state.proximityAlertEnabled &&
            distance <= state.proximityAlertDistance &&
            !state.hasTriggeredProximityAlert) {
            // Trigger alert
            playAlertSound()
            _uiState.value = _uiState.value.copy(hasTriggeredProximityAlert = true)
        } else if (distance > state.proximityAlertDistance) {
            // Reset alert when bus moves away
            _uiState.value = _uiState.value.copy(hasTriggeredProximityAlert = false)
        }
    }

    private fun playAlertSound() {
        try {
            val state = _uiState.value

            // Stop any existing alert
            stopAlertSound()

            _uiState.value = _uiState.value.copy(isAlertPlaying = true)

            // Play sound if not set to "None"
            if (state.alertSoundUri != "none") {
                val soundUri = if (state.alertSoundUri != null) {
                    Uri.parse(state.alertSoundUri)
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }

                mediaPlayer = MediaPlayer.create(context, soundUri)
                mediaPlayer?.isLooping = true // Loop until duration expires
                mediaPlayer?.start()
            }

            // Vibrate if enabled
            if (state.alertVibrationEnabled) {
                startVibration(state.alertDurationSeconds * 1000L)
            }

            // Stop after configured duration
            handler.postDelayed({
                stopAlertSound()
            }, state.alertDurationSeconds * 1000L)
        } catch (e: Exception) {
            // Ignore sound errors
            _uiState.value = _uiState.value.copy(isAlertPlaying = false)
        }
    }

    private fun startVibration(durationMs: Long) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (vibrator.hasVibrator()) {
                // Vibration pattern: wait 0ms, vibrate 500ms, wait 500ms, repeat
                val pattern = longArrayOf(0, 500, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(pattern, 0), // 0 = repeat from index 0
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }

    private fun stopVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.cancel()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun stopAlertSound() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        stopVibration()
        _uiState.value = _uiState.value.copy(isAlertPlaying = false)
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     * @return distance in kilometers
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    // Vehicle management
    fun selectVehicle(vehicleId: String) {
        viewModelScope.launch {
            repository.selectVehicle(vehicleId)
            val vehicle = _uiState.value.vehicles.find { it.id == vehicleId } ?: VehicleConfig.DEFAULT
            _uiState.value = _uiState.value.copy(
                vehicleConfig = vehicle,
                vehicleStatus = null,
                hasTriggeredProximityAlert = false
            )
            refreshVehicleStatus()
        }
    }

    fun addVehicle(imeiNo: String, vehicleNo: String, displayName: String, vehicleType: String = "Bus") {
        viewModelScope.launch {
            val newVehicle = VehicleConfig(
                imeiNo = imeiNo,
                vehicleNo = vehicleNo,
                displayName = displayName.ifEmpty { vehicleNo },
                vehicleType = vehicleType
            )
            repository.addVehicle(newVehicle)
            // Refresh vehicles list
            val vehicles = repository.getVehicles().first()
            _uiState.value = _uiState.value.copy(vehicles = vehicles)
        }
    }

    fun updateVehicle(vehicle: VehicleConfig) {
        viewModelScope.launch {
            repository.updateVehicle(vehicle)
            // Refresh vehicles list
            val vehicles = repository.getVehicles().first()
            _uiState.value = _uiState.value.copy(vehicles = vehicles)
            // If this is the selected vehicle, update config
            if (vehicle.id == _uiState.value.vehicleConfig.id) {
                _uiState.value = _uiState.value.copy(vehicleConfig = vehicle)
            }
        }
    }

    fun deleteVehicle(vehicleId: String) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicleId)
            // Refresh vehicles list
            val vehicles = repository.getVehicles().first()
            _uiState.value = _uiState.value.copy(vehicles = vehicles)
            // If deleted the selected vehicle, select the first one
            if (vehicleId == _uiState.value.vehicleConfig.id) {
                selectVehicle(vehicles.first().id)
            }
        }
    }

    // Home location
    fun setHomeLocation(latitude: Double, longitude: Double, name: String = "Home") {
        viewModelScope.launch {
            val location = HomeLocation(latitude, longitude, name)
            repository.saveHomeLocation(location)
            _uiState.value = _uiState.value.copy(
                homeLocation = location,
                hasTriggeredProximityAlert = false
            )
            updateDistanceToHome()
        }
    }

    fun setHomeToCurrentBusLocation() {
        val status = _uiState.value.vehicleStatus ?: return
        setHomeLocation(status.getLatitudeDouble(), status.getLongitudeDouble())
    }

    // Proximity alert
    fun setProximityAlertEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setProximityAlertEnabled(enabled)
            _uiState.value = _uiState.value.copy(
                proximityAlertEnabled = enabled,
                hasTriggeredProximityAlert = false
            )
        }
    }

    fun setProximityAlertDistance(distanceKm: Double) {
        viewModelScope.launch {
            repository.setProximityAlertDistance(distanceKm)
            _uiState.value = _uiState.value.copy(
                proximityAlertDistance = distanceKm,
                hasTriggeredProximityAlert = false
            )
        }
    }

    fun resetProximityAlert() {
        _uiState.value = _uiState.value.copy(hasTriggeredProximityAlert = false)
    }

    fun setAutoRefresh(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAutoRefreshEnabled = enabled)
        if (enabled) {
            startAutoRefresh()
        } else {
            stopAutoRefresh()
        }
    }

    fun setRefreshInterval(seconds: Int) {
        viewModelScope.launch {
            repository.setRefreshInterval(seconds)
            _uiState.value = _uiState.value.copy(refreshIntervalSeconds = seconds)
            // Restart auto refresh with new interval
            if (_uiState.value.isAutoRefreshEnabled) {
                startAutoRefresh()
            }
        }
    }

    fun setAlertSoundUri(uri: String?) {
        viewModelScope.launch {
            repository.setAlertSoundUri(uri)
            _uiState.value = _uiState.value.copy(alertSoundUri = uri)
        }
    }

    fun setAlertDuration(seconds: Int) {
        viewModelScope.launch {
            repository.setAlertDuration(seconds)
            _uiState.value = _uiState.value.copy(alertDurationSeconds = seconds)
        }
    }

    fun setAlertVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAlertVibrationEnabled(enabled)
            _uiState.value = _uiState.value.copy(alertVibrationEnabled = enabled)
        }
    }

    fun testAlertSound() {
        playAlertSound()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        val interval = _uiState.value.refreshIntervalSeconds
        if (interval <= 0) {
            // Auto refresh is disabled
            return
        }
        autoRefreshJob = viewModelScope.launch {
            while (_uiState.value.isAutoRefreshEnabled && _uiState.value.refreshIntervalSeconds > 0) {
                delay(_uiState.value.refreshIntervalSeconds * 1000L)
                if (_uiState.value.isAutoRefreshEnabled && _uiState.value.refreshIntervalSeconds > 0) {
                    refreshVehicleStatus()
                }
            }
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
        stopAlertSound()
    }
}
