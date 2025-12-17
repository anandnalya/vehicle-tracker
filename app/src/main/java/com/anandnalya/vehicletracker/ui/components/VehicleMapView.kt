package com.anandnalya.vehicletracker.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.anandnalya.vehicletracker.data.model.HomeLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun VehicleMapView(
    latitude: Double,
    longitude: Double,
    vehicleNo: String,
    heading: Float,
    homeLocation: HomeLocation?,
    modifier: Modifier = Modifier
) {
    val vehiclePosition = LatLng(latitude, longitude)
    val homePosition = homeLocation?.let { LatLng(it.latitude, it.longitude) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(vehiclePosition, 15f)
    }

    // Animate camera to show both vehicle and home if home is set
    LaunchedEffect(latitude, longitude, homeLocation) {
        if (homePosition != null) {
            // Calculate bounds to show both markers
            val boundsBuilder = LatLngBounds.builder()
                .include(vehiclePosition)
                .include(homePosition)
            val bounds = boundsBuilder.build()

            // Add padding and animate
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 100),
                durationMs = 1000
            )
        } else {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(vehiclePosition, 15f),
                durationMs = 1000
            )
        }
    }

    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            mapType = MapType.TERRAIN,
            isTrafficEnabled = true
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            myLocationButtonEnabled = false
        )
    ) {
        // Vehicle marker (blue)
        Marker(
            state = MarkerState(position = vehiclePosition),
            title = vehicleNo,
            snippet = "Heading: ${heading.toInt()}°",
            rotation = heading,
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        )

        // Home marker (green)
        if (homePosition != null) {
            Marker(
                state = MarkerState(position = homePosition),
                title = homeLocation.name,
                snippet = "Home location",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )
        }
    }
}
