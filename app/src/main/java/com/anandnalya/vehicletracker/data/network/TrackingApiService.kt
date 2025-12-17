package com.anandnalya.vehicletracker.data.network

import com.anandnalya.vehicletracker.data.model.VehicleStatusResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit API service for tracknovate.in vehicle tracking
 */
interface TrackingApiService {

    /**
     * Initialize session by calling the quickview.jsp page.
     * This returns JSESSIONID and other cookies needed for subsequent API calls.
     */
    @GET("jsp/quickview.jsp")
    suspend fun initializeSession(): Response<ResponseBody>

    @FormUrlEncoded
    @POST("GenerateJSON")
    suspend fun getVehicleStatus(
        @Query("method") method: String = "getVehicleStatus",
        @FieldMap fields: Map<String, String>
    ): Response<VehicleStatusResponse>

    companion object {
        const val BASE_URL = "https://tracknovate.in/"
    }
}
