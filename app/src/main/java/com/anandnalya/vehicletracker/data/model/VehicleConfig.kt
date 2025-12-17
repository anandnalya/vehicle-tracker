package com.anandnalya.vehicletracker.data.model

import com.anandnalya.vehicletracker.BuildConfig
import java.util.UUID

/**
 * Configuration for a vehicle to track
 */
data class VehicleConfig(
    val id: String = UUID.randomUUID().toString(),
    val imeiNo: String,
    val vehicleType: String = "Bus",
    val vehicleNo: String = "",
    val displayName: String = ""
) {
    companion object {
        // Default vehicle from secrets.properties
        val DEFAULT = VehicleConfig(
            id = "default",
            imeiNo = BuildConfig.DEFAULT_VEHICLE_IMEI,
            vehicleType = BuildConfig.DEFAULT_VEHICLE_TYPE,
            vehicleNo = BuildConfig.DEFAULT_VEHICLE_NO,
            displayName = BuildConfig.DEFAULT_VEHICLE_NAME
        )
    }
}

/**
 * Home location for distance calculation and alerts
 */
data class HomeLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String = "Home"
) {
    companion object {
        // Default home location from secrets.properties
        val DEFAULT = HomeLocation(
            latitude = BuildConfig.HOME_LATITUDE.toDoubleOrNull() ?: 0.0,
            longitude = BuildConfig.HOME_LONGITUDE.toDoubleOrNull() ?: 0.0,
            name = BuildConfig.HOME_NAME
        )
    }
}

/**
 * API request parameters for vehicle status
 */
data class VehicleStatusRequest(
    val imeiNo: String,
    val vehicleType: String,
    val timezone: Int = -330,
    val inactiveTolerance: Long = 3600000L,
    val userDateTimeFormat: String = "dd-MM-yyyy HH:mm:ss"
) {
    fun toFormData(): Map<String, String> = mapOf(
        "javaclassmethodname" to "getVehicleStatus",
        "javaclassname" to "com.uffizio.tools.projectmanager.GenerateJSONAjax",
        "sImeiNo" to imeiNo,
        "vehicleType" to vehicleType,
        "timezone" to timezone.toString(),
        "lInActiveTolrance" to inactiveTolerance.toString(),
        "userDateTimeFormat" to userDateTimeFormat,
        "Flag" to "Callfromservice",
        "link_id" to "0",
        "user_id" to "0",
        "project_id" to "0"
    )
}
