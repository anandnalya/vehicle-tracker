package com.anandnalya.vehicletracker.data.repository

import com.anandnalya.vehicletracker.data.model.HomeLocation
import com.anandnalya.vehicletracker.data.model.VehicleConfig
import com.anandnalya.vehicletracker.data.model.VehicleStatus
import com.anandnalya.vehicletracker.data.model.VehicleStatusRequest
import com.anandnalya.vehicletracker.data.network.SessionManager
import com.anandnalya.vehicletracker.data.network.TrackingApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

@Singleton
class VehicleRepository @Inject constructor(
    private val apiService: TrackingApiService,
    private val sessionManager: SessionManager
) {
    /**
     * Initialize session by making a GET request to VehicleTracking.jsp
     * The SessionCookieInterceptor will automatically capture and store the JSESSIONID
     */
    suspend fun initializeSession(): Result<Unit> {
        return try {
            val response = apiService.initializeSession()
            if (response.isSuccessful) {
                // Session cookie is automatically captured by SessionCookieInterceptor
                Result.Success(Unit)
            } else {
                Result.Error("Failed to initialize session: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error("Network error during session init: ${e.message}", e)
        }
    }

    /**
     * Check if we have a valid session
     */
    suspend fun hasValidSession(): Boolean {
        return sessionManager.getCookiesSync().isNotEmpty()
    }

    /**
     * Get vehicle status, initializing session if needed
     */
    suspend fun getVehicleStatus(config: VehicleConfig, retryCount: Int = 0): Result<VehicleStatus> {
        return try {
            // Always initialize session on first attempt or after clearing
            if (!hasValidSession()) {
                val sessionResult = initializeSession()
                if (sessionResult is Result.Error) {
                    return sessionResult
                }
            }

            val request = VehicleStatusRequest(
                imeiNo = config.imeiNo,
                vehicleType = config.vehicleType
            )

            val response = apiService.getVehicleStatus(
                fields = request.toFormData()
            )

            if (response.isSuccessful) {
                val body = response.body()
                val vehicle = body?.getVehicle()
                if (vehicle != null) {
                    Result.Success(vehicle)
                } else {
                    // Session might be stale or invalid, clear and retry once
                    if (retryCount < 1) {
                        sessionManager.clearSession()
                        return getVehicleStatus(config, retryCount + 1)
                    }
                    Result.Error("No vehicle data found. Please check vehicle configuration.")
                }
            } else if (response.code() == 401 || response.code() == 403) {
                // Session expired, clear and retry once
                if (retryCount < 1) {
                    sessionManager.clearSession()
                    return getVehicleStatus(config, retryCount + 1)
                }
                Result.Error("Session expired. Please refresh.")
            } else {
                Result.Error("API error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            // Parse errors might indicate stale session returning garbage, retry once
            if (retryCount < 1 && e.message?.contains("Expected") == true) {
                sessionManager.clearSession()
                return getVehicleStatus(config, retryCount + 1)
            }
            Result.Error("Network error: ${e.message}", e)
        }
    }

    fun getVehicleStatusFlow(config: VehicleConfig): Flow<Result<VehicleStatus>> = flow {
        emit(Result.Loading)
        emit(getVehicleStatus(config))
    }

    // Vehicle management
    fun getVehicles(): Flow<List<VehicleConfig>> = sessionManager.vehicles
    fun getSelectedVehicleId(): Flow<String> = sessionManager.selectedVehicleId

    suspend fun getSelectedVehicle(): VehicleConfig = sessionManager.getSelectedVehicle()

    suspend fun addVehicle(vehicle: VehicleConfig) {
        sessionManager.addVehicle(vehicle)
    }

    suspend fun updateVehicle(vehicle: VehicleConfig) {
        sessionManager.updateVehicle(vehicle)
    }

    suspend fun deleteVehicle(vehicleId: String) {
        sessionManager.deleteVehicle(vehicleId)
    }

    suspend fun selectVehicle(vehicleId: String) {
        sessionManager.clearSession() // Clear session when switching vehicles
        sessionManager.selectVehicle(vehicleId)
    }

    // Home location
    fun getHomeLocation(): Flow<HomeLocation> = sessionManager.homeLocation

    suspend fun saveHomeLocation(location: HomeLocation) {
        sessionManager.saveHomeLocation(location)
    }

    // Proximity alerts
    fun getProximityAlertEnabled(): Flow<Boolean> = sessionManager.proximityAlertEnabled
    fun getProximityAlertDistance(): Flow<Double> = sessionManager.proximityAlertDistance

    suspend fun setProximityAlertEnabled(enabled: Boolean) {
        sessionManager.setProximityAlertEnabled(enabled)
    }

    suspend fun setProximityAlertDistance(distanceKm: Double) {
        sessionManager.setProximityAlertDistance(distanceKm)
    }

    // Refresh interval
    fun getRefreshInterval(): Flow<Int> = sessionManager.refreshInterval

    suspend fun setRefreshInterval(seconds: Int) {
        sessionManager.setRefreshInterval(seconds)
    }

    // Alert sound settings
    fun getAlertSoundUri(): Flow<String?> = sessionManager.alertSoundUri
    fun getAlertDuration(): Flow<Int> = sessionManager.alertDuration
    fun getAlertVibrationEnabled(): Flow<Boolean> = sessionManager.alertVibrationEnabled

    suspend fun setAlertSoundUri(uri: String?) {
        sessionManager.setAlertSoundUri(uri)
    }

    suspend fun setAlertDuration(seconds: Int) {
        sessionManager.setAlertDuration(seconds)
    }

    suspend fun setAlertVibrationEnabled(enabled: Boolean) {
        sessionManager.setAlertVibrationEnabled(enabled)
    }

    // Legacy methods for compatibility
    suspend fun saveVehicleConfig(imei: String, vehicleType: String, vehicleNo: String) {
        sessionManager.clearSession()
        sessionManager.saveVehicleConfig(imei, vehicleType, vehicleNo)
    }

    fun getSavedVehicleImei(): Flow<String?> = sessionManager.vehicleImei
    fun getSavedVehicleType(): Flow<String?> = sessionManager.vehicleType
    fun getSavedVehicleNo(): Flow<String?> = sessionManager.vehicleNo
}
