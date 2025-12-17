package com.anandnalya.vehicletracker.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anandnalya.vehicletracker.ui.components.ConfigDialog
import com.anandnalya.vehicletracker.ui.components.VehicleInfoCard
import com.anandnalya.vehicletracker.ui.components.VehicleMapView
import com.anandnalya.vehicletracker.ui.viewmodel.VehicleTrackerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleTrackerScreen(
    viewModel: VehicleTrackerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showIntervalMenu by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Show snackbar when proximity alert triggers
    LaunchedEffect(uiState.hasTriggeredProximityAlert) {
        if (uiState.hasTriggeredProximityAlert) {
            snackbarHostState.showSnackbar("Bus is near home!")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DPS Bus Tracker")
                        Text(
                            text = uiState.vehicleConfig.displayName.ifEmpty { uiState.vehicleConfig.vehicleNo },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Refresh interval dropdown
                    Box {
                        OutlinedButton(
                            onClick = { showIntervalMenu = true },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = when {
                                    uiState.refreshIntervalSeconds == 0 -> "Off"
                                    uiState.refreshIntervalSeconds < 60 -> "${uiState.refreshIntervalSeconds}s"
                                    else -> "${uiState.refreshIntervalSeconds / 60}m"
                                },
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        DropdownMenu(
                            expanded = showIntervalMenu,
                            onDismissRequest = { showIntervalMenu = false }
                        ) {
                            VehicleTrackerViewModel.REFRESH_INTERVALS.forEach { interval ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when {
                                                interval == 0 -> "Off"
                                                interval < 60 -> "${interval}s"
                                                else -> "${interval / 60}m"
                                            }
                                        )
                                    },
                                    onClick = {
                                        viewModel.setRefreshInterval(interval)
                                        showIntervalMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { showConfigDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.refreshVehicleStatus() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Map takes most of the screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                uiState.vehicleStatus?.let { vehicle ->
                    VehicleMapView(
                        latitude = vehicle.getLatitudeDouble(),
                        longitude = vehicle.getLongitudeDouble(),
                        vehicleNo = vehicle.vehicleNo,
                        heading = vehicle.getAngleFloat(),
                        homeLocation = uiState.homeLocation,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator()
                        } else {
                            Text(
                                text = "No vehicle data available",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Dismiss alert button when alert is playing
            if (uiState.isAlertPlaying) {
                Button(
                    onClick = { viewModel.stopAlertSound() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Dismiss Alert")
                }
            }

            // Vehicle info card at the bottom
            uiState.vehicleStatus?.let { vehicle ->
                VehicleInfoCard(
                    vehicleStatus = vehicle,
                    distanceToHome = uiState.distanceToHome,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    if (showConfigDialog) {
        ConfigDialog(
            vehicles = uiState.vehicles,
            selectedVehicle = uiState.vehicleConfig,
            homeLocation = uiState.homeLocation,
            proximityAlertEnabled = uiState.proximityAlertEnabled,
            proximityAlertDistance = uiState.proximityAlertDistance,
            alertSoundUri = uiState.alertSoundUri,
            alertDurationSeconds = uiState.alertDurationSeconds,
            alertVibrationEnabled = uiState.alertVibrationEnabled,
            isAlertPlaying = uiState.isAlertPlaying,
            onDismiss = { showConfigDialog = false },
            onSelectVehicle = { vehicleId ->
                viewModel.selectVehicle(vehicleId)
            },
            onAddVehicle = { imei, vehicleNo, displayName, vehicleType ->
                viewModel.addVehicle(imei, vehicleNo, displayName, vehicleType)
            },
            onUpdateVehicle = { vehicle ->
                viewModel.updateVehicle(vehicle)
            },
            onDeleteVehicle = { vehicleId ->
                viewModel.deleteVehicle(vehicleId)
            },
            onSetHomeLocation = { lat, lng, name ->
                viewModel.setHomeLocation(lat, lng, name)
            },
            onSetHomeToBusLocation = {
                viewModel.setHomeToCurrentBusLocation()
            },
            onSetProximityAlertEnabled = { enabled ->
                viewModel.setProximityAlertEnabled(enabled)
            },
            onSetProximityAlertDistance = { distance ->
                viewModel.setProximityAlertDistance(distance)
            },
            onSetAlertSoundUri = { uri ->
                viewModel.setAlertSoundUri(uri)
            },
            onSetAlertDuration = { seconds ->
                viewModel.setAlertDuration(seconds)
            },
            onSetAlertVibrationEnabled = { enabled ->
                viewModel.setAlertVibrationEnabled(enabled)
            },
            onTestAlertSound = {
                viewModel.testAlertSound()
            },
            onStopAlertSound = {
                viewModel.stopAlertSound()
            }
        )
    }
}
