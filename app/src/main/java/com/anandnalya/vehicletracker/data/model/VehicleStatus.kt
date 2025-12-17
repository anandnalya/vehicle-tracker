package com.anandnalya.vehicletracker.data.model

import com.google.gson.annotations.SerializedName

/**
 * Root response wrapper from the tracking API
 * Response format: { root: [[{...vehicle data...}]] }
 */
data class VehicleStatusResponse(
    @SerializedName("root")
    val root: List<List<VehicleStatus>>
) {
    fun getVehicle(): VehicleStatus? = root.firstOrNull()?.firstOrNull()
}

/**
 * Vehicle status data from tracknovate.in API
 */
data class VehicleStatus(
    @SerializedName("sts")
    val status: String,

    @SerializedName("vehicle_id")
    val vehicleId: String,

    @SerializedName("imeino")
    val imeiNo: String,

    @SerializedName("vehicle_no")
    val vehicleNo: String,

    @SerializedName("vehicle_name")
    val vehicleName: String,

    @SerializedName("vehicle_type")
    val vehicleType: String,

    @SerializedName("speed_unit")
    val speedUnit: String,

    @SerializedName("driver_json")
    val driverInfo: DriverInfo,

    @SerializedName("latitude")
    val latitude: String,

    @SerializedName("longitude")
    val longitude: String,

    @SerializedName("angle")
    val angle: String,

    @SerializedName("data_inserted_time")
    val dataInsertedTime: String,

    @SerializedName("data_inserted_time_mili")
    val dataInsertedTimeMili: String,

    @SerializedName("location")
    val location: String,

    @SerializedName("speed")
    val speed: String,

    @SerializedName("since")
    val since: String,

    @SerializedName("image_exist")
    val imageExist: String,

    @SerializedName("image_path")
    val imagePath: String
) {
    fun getLatitudeDouble(): Double = latitude.toDoubleOrNull() ?: 0.0
    fun getLongitudeDouble(): Double = longitude.toDoubleOrNull() ?: 0.0
    fun getSpeedInt(): Int = speed.toIntOrNull() ?: 0
    fun getAngleFloat(): Float = angle.toFloatOrNull() ?: 0f

    fun isRunning(): Boolean = status.equals("Running", ignoreCase = true)
    fun isStopped(): Boolean = status.equals("Stopped", ignoreCase = true)
    fun isIdle(): Boolean = status.equals("Idle", ignoreCase = true)
}

data class DriverInfo(
    @SerializedName("name")
    val name: String,

    @SerializedName("mobile_no")
    val mobileNo: String
)
