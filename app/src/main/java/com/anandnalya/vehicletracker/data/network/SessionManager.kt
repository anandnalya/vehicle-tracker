package com.anandnalya.vehicletracker.data.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anandnalya.vehicletracker.data.model.HomeLocation
import com.anandnalya.vehicletracker.data.model.VehicleConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * Manages session cookies, vehicle configuration, home location, and alert settings
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    companion object {
        private val COOKIES_KEY = stringPreferencesKey("cookies")
        private val VEHICLES_KEY = stringPreferencesKey("vehicles_json")
        private val SELECTED_VEHICLE_ID_KEY = stringPreferencesKey("selected_vehicle_id")
        private val HOME_LAT_KEY = doublePreferencesKey("home_latitude")
        private val HOME_LNG_KEY = doublePreferencesKey("home_longitude")
        private val HOME_NAME_KEY = stringPreferencesKey("home_name")
        private val PROXIMITY_ALERT_ENABLED_KEY = booleanPreferencesKey("proximity_alert_enabled")
        private val PROXIMITY_ALERT_DISTANCE_KEY = doublePreferencesKey("proximity_alert_distance")
        private val REFRESH_INTERVAL_KEY = intPreferencesKey("refresh_interval")
        private val ALERT_SOUND_URI_KEY = stringPreferencesKey("alert_sound_uri")
        private val ALERT_DURATION_KEY = intPreferencesKey("alert_duration")
        private val ALERT_VIBRATION_ENABLED_KEY = booleanPreferencesKey("alert_vibration_enabled")
    }

    // Vehicle list
    val vehicles: Flow<List<VehicleConfig>> = context.dataStore.data.map { preferences ->
        val json = preferences[VEHICLES_KEY]
        if (json.isNullOrEmpty()) {
            listOf(VehicleConfig.DEFAULT)
        } else {
            try {
                val type = object : TypeToken<List<VehicleConfig>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                listOf(VehicleConfig.DEFAULT)
            }
        }
    }

    val selectedVehicleId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_VEHICLE_ID_KEY] ?: VehicleConfig.DEFAULT.id
    }

    // Home location
    val homeLocation: Flow<HomeLocation> = context.dataStore.data.map { preferences ->
        HomeLocation(
            latitude = preferences[HOME_LAT_KEY] ?: HomeLocation.DEFAULT.latitude,
            longitude = preferences[HOME_LNG_KEY] ?: HomeLocation.DEFAULT.longitude,
            name = preferences[HOME_NAME_KEY] ?: HomeLocation.DEFAULT.name
        )
    }

    // Proximity alert settings
    val proximityAlertEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PROXIMITY_ALERT_ENABLED_KEY] ?: true
    }

    val proximityAlertDistance: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[PROXIMITY_ALERT_DISTANCE_KEY] ?: 1.0 // 1 km default
    }

    val refreshInterval: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_INTERVAL_KEY] ?: 30 // 30 seconds default
    }

    val alertSoundUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ALERT_SOUND_URI_KEY] // null means use default notification sound
    }

    val alertDuration: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ALERT_DURATION_KEY] ?: 5 // 5 seconds default
    }

    val alertVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ALERT_VIBRATION_ENABLED_KEY] ?: true // enabled by default
    }

    // Legacy support for single vehicle
    val vehicleImei: Flow<String?> = context.dataStore.data.map { preferences ->
        val selectedId = preferences[SELECTED_VEHICLE_ID_KEY] ?: VehicleConfig.DEFAULT.id
        val vehiclesJson = preferences[VEHICLES_KEY]
        if (vehiclesJson.isNullOrEmpty()) {
            VehicleConfig.DEFAULT.imeiNo
        } else {
            try {
                val type = object : TypeToken<List<VehicleConfig>>() {}.type
                val list: List<VehicleConfig> = gson.fromJson(vehiclesJson, type)
                list.find { it.id == selectedId }?.imeiNo ?: VehicleConfig.DEFAULT.imeiNo
            } catch (e: Exception) {
                VehicleConfig.DEFAULT.imeiNo
            }
        }
    }

    val vehicleType: Flow<String?> = context.dataStore.data.map { preferences ->
        val selectedId = preferences[SELECTED_VEHICLE_ID_KEY] ?: VehicleConfig.DEFAULT.id
        val vehiclesJson = preferences[VEHICLES_KEY]
        if (vehiclesJson.isNullOrEmpty()) {
            VehicleConfig.DEFAULT.vehicleType
        } else {
            try {
                val type = object : TypeToken<List<VehicleConfig>>() {}.type
                val list: List<VehicleConfig> = gson.fromJson(vehiclesJson, type)
                list.find { it.id == selectedId }?.vehicleType ?: VehicleConfig.DEFAULT.vehicleType
            } catch (e: Exception) {
                VehicleConfig.DEFAULT.vehicleType
            }
        }
    }

    val vehicleNo: Flow<String?> = context.dataStore.data.map { preferences ->
        val selectedId = preferences[SELECTED_VEHICLE_ID_KEY] ?: VehicleConfig.DEFAULT.id
        val vehiclesJson = preferences[VEHICLES_KEY]
        if (vehiclesJson.isNullOrEmpty()) {
            VehicleConfig.DEFAULT.vehicleNo
        } else {
            try {
                val type = object : TypeToken<List<VehicleConfig>>() {}.type
                val list: List<VehicleConfig> = gson.fromJson(vehiclesJson, type)
                list.find { it.id == selectedId }?.vehicleNo ?: VehicleConfig.DEFAULT.vehicleNo
            } catch (e: Exception) {
                VehicleConfig.DEFAULT.vehicleNo
            }
        }
    }

    suspend fun getCookiesSync(): String {
        return context.dataStore.data.first()[COOKIES_KEY] ?: ""
    }

    suspend fun saveCookies(cookies: String) {
        context.dataStore.edit { preferences ->
            preferences[COOKIES_KEY] = cookies
        }
    }

    suspend fun saveVehicles(vehicles: List<VehicleConfig>) {
        context.dataStore.edit { preferences ->
            preferences[VEHICLES_KEY] = gson.toJson(vehicles)
        }
    }

    suspend fun addVehicle(vehicle: VehicleConfig) {
        val currentVehicles = vehicles.first().toMutableList()
        currentVehicles.add(vehicle)
        saveVehicles(currentVehicles)
    }

    suspend fun updateVehicle(vehicle: VehicleConfig) {
        val currentVehicles = vehicles.first().toMutableList()
        val index = currentVehicles.indexOfFirst { it.id == vehicle.id }
        if (index >= 0) {
            currentVehicles[index] = vehicle
            saveVehicles(currentVehicles)
        }
    }

    suspend fun deleteVehicle(vehicleId: String) {
        val currentVehicles = vehicles.first().toMutableList()
        currentVehicles.removeAll { it.id == vehicleId }
        if (currentVehicles.isEmpty()) {
            currentVehicles.add(VehicleConfig.DEFAULT)
        }
        saveVehicles(currentVehicles)
        // If deleted vehicle was selected, select first available
        val selectedId = selectedVehicleId.first()
        if (selectedId == vehicleId) {
            selectVehicle(currentVehicles.first().id)
        }
    }

    suspend fun selectVehicle(vehicleId: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_VEHICLE_ID_KEY] = vehicleId
        }
    }

    suspend fun getSelectedVehicle(): VehicleConfig {
        val selectedId = selectedVehicleId.first()
        return vehicles.first().find { it.id == selectedId } ?: VehicleConfig.DEFAULT
    }

    // Legacy method for compatibility
    suspend fun saveVehicleConfig(imei: String, vehicleType: String, vehicleNo: String) {
        val selectedId = selectedVehicleId.first()
        val currentVehicles = vehicles.first().toMutableList()
        val index = currentVehicles.indexOfFirst { it.id == selectedId }
        if (index >= 0) {
            currentVehicles[index] = currentVehicles[index].copy(
                imeiNo = imei,
                vehicleType = vehicleType,
                vehicleNo = vehicleNo
            )
            saveVehicles(currentVehicles)
        }
    }

    suspend fun saveHomeLocation(location: HomeLocation) {
        context.dataStore.edit { preferences ->
            preferences[HOME_LAT_KEY] = location.latitude
            preferences[HOME_LNG_KEY] = location.longitude
            preferences[HOME_NAME_KEY] = location.name
        }
    }

    suspend fun setProximityAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PROXIMITY_ALERT_ENABLED_KEY] = enabled
        }
    }

    suspend fun setProximityAlertDistance(distanceKm: Double) {
        context.dataStore.edit { preferences ->
            preferences[PROXIMITY_ALERT_DISTANCE_KEY] = distanceKm
        }
    }

    suspend fun setRefreshInterval(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[REFRESH_INTERVAL_KEY] = seconds
        }
    }

    suspend fun setAlertSoundUri(uri: String?) {
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[ALERT_SOUND_URI_KEY] = uri
            } else {
                preferences.remove(ALERT_SOUND_URI_KEY)
            }
        }
    }

    suspend fun setAlertDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[ALERT_DURATION_KEY] = seconds
        }
    }

    suspend fun setAlertVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALERT_VIBRATION_ENABLED_KEY] = enabled
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(COOKIES_KEY)
        }
    }
}
