package com.anandnalya.vehicletracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anandnalya.vehicletracker.data.model.HomeLocation
import com.anandnalya.vehicletracker.data.model.VehicleConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDialog(
    vehicles: List<VehicleConfig>,
    selectedVehicle: VehicleConfig,
    homeLocation: HomeLocation,
    proximityAlertEnabled: Boolean,
    proximityAlertDistance: Double,
    alertSoundUri: String?,
    alertDurationSeconds: Int,
    alertVibrationEnabled: Boolean,
    isAlertPlaying: Boolean,
    onDismiss: () -> Unit,
    onSelectVehicle: (String) -> Unit,
    onAddVehicle: (imei: String, vehicleNo: String, displayName: String, vehicleType: String) -> Unit,
    onUpdateVehicle: (VehicleConfig) -> Unit,
    onDeleteVehicle: (String) -> Unit,
    onSetHomeLocation: (Double, Double, String) -> Unit,
    onSetHomeToBusLocation: () -> Unit,
    onSetProximityAlertEnabled: (Boolean) -> Unit,
    onSetProximityAlertDistance: (Double) -> Unit,
    onSetAlertSoundUri: (String?) -> Unit,
    onSetAlertDuration: (Int) -> Unit,
    onSetAlertVibrationEnabled: (Boolean) -> Unit,
    onTestAlertSound: () -> Unit,
    onStopAlertSound: () -> Unit
) {
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var editingVehicle by remember { mutableStateOf<VehicleConfig?>(null) }
    var showHomeLocationDialog by remember { mutableStateOf(false) }
    var vehicleExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Vehicle Selection Section
                Text(
                    text = "Vehicle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = vehicleExpanded,
                    onExpandedChange = { vehicleExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedVehicle.displayName.ifEmpty { selectedVehicle.vehicleNo },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selected Vehicle") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = vehicleExpanded,
                        onDismissRequest = { vehicleExpanded = false }
                    ) {
                        vehicles.forEach { vehicle ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = vehicle.displayName.ifEmpty { vehicle.vehicleNo },
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = vehicle.vehicleNo,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    onSelectVehicle(vehicle.id)
                                    vehicleExpanded = false
                                },
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { editingVehicle = vehicle }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (vehicles.size > 1) {
                                            IconButton(onClick = { onDeleteVehicle(vehicle.id) }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showAddVehicleDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Vehicle")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Home Location Section
                Text(
                    text = "Home Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = homeLocation.name,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format("%.4f, %.4f", homeLocation.latitude, homeLocation.longitude),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSetHomeToBusLocation,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Use Bus")
                    }
                    OutlinedButton(
                        onClick = { showHomeLocationDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Proximity Alert Section
                Text(
                    text = "Proximity Alert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Alert Sound")
                    Switch(
                        checked = proximityAlertEnabled,
                        onCheckedChange = onSetProximityAlertEnabled
                    )
                }

                if (proximityAlertEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Alert when within: ${String.format("%.1f", proximityAlertDistance)} km",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = proximityAlertDistance.toFloat(),
                        onValueChange = { onSetProximityAlertDistance(it.toDouble()) },
                        valueRange = 0.1f..10f,
                        steps = 98
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Alert Duration
                    Text(
                        text = "Alert duration: $alertDurationSeconds seconds",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = alertDurationSeconds.toFloat(),
                        onValueChange = { onSetAlertDuration(it.toInt()) },
                        valueRange = 1f..60f,
                        steps = 58
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Vibration toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Vibration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vibration")
                        }
                        Switch(
                            checked = alertVibrationEnabled,
                            onCheckedChange = onSetAlertVibrationEnabled
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sound Selection
                    Text(
                        text = "Alert Sound",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    var soundExpanded by remember { mutableStateOf(false) }
                    val soundOptions = listOf(
                        "None (Silent)" to "none",
                        "Default Notification" to null,
                        "Alarm" to "content://settings/system/alarm_alert",
                        "Ringtone" to "content://settings/system/ringtone",
                        "Beep" to "android.resource://com.anandnalya.vehicletracker/raw/beep",
                        "Chime" to "content://settings/system/notification_sound"
                    )
                    val currentSoundName = when (alertSoundUri) {
                        "none" -> "None (Silent)"
                        null -> "Default Notification"
                        else -> soundOptions.find { it.second == alertSoundUri }?.first ?: "Custom"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = soundExpanded,
                            onExpandedChange = { soundExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = currentSoundName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = soundExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                singleLine = true
                            )

                            ExposedDropdownMenu(
                                expanded = soundExpanded,
                                onDismissRequest = { soundExpanded = false }
                            ) {
                                soundOptions.forEach { (name, uri) ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            onSetAlertSoundUri(uri)
                                            soundExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.MusicNote, contentDescription = null)
                                        }
                                    )
                                }
                            }
                        }

                        // Test/Stop button
                        IconButton(
                            onClick = {
                                if (isAlertPlaying) {
                                    onStopAlertSound()
                                } else {
                                    onTestAlertSound()
                                }
                            }
                        ) {
                            Icon(
                                if (isAlertPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isAlertPlaying) "Stop" else "Test",
                                tint = if (isAlertPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        AddEditVehicleDialog(
            vehicle = null,
            onDismiss = { showAddVehicleDialog = false },
            onSave = { imei, vehicleNo, displayName, vehicleType ->
                onAddVehicle(imei, vehicleNo, displayName, vehicleType)
                showAddVehicleDialog = false
            }
        )
    }

    // Edit Vehicle Dialog
    editingVehicle?.let { vehicle ->
        AddEditVehicleDialog(
            vehicle = vehicle,
            onDismiss = { editingVehicle = null },
            onSave = { imei, vehicleNo, displayName, vehicleType ->
                onUpdateVehicle(vehicle.copy(
                    imeiNo = imei,
                    vehicleNo = vehicleNo,
                    displayName = displayName,
                    vehicleType = vehicleType
                ))
                editingVehicle = null
            }
        )
    }

    // Home Location Dialog
    if (showHomeLocationDialog) {
        HomeLocationDialog(
            homeLocation = homeLocation,
            onDismiss = { showHomeLocationDialog = false },
            onSave = { lat, lng, name ->
                onSetHomeLocation(lat, lng, name)
                showHomeLocationDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleDialog(
    vehicle: VehicleConfig?,
    onDismiss: () -> Unit,
    onSave: (imei: String, vehicleNo: String, displayName: String, vehicleType: String) -> Unit
) {
    var imei by remember { mutableStateOf(vehicle?.imeiNo ?: "") }
    var vehicleNo by remember { mutableStateOf(vehicle?.vehicleNo ?: "") }
    var displayName by remember { mutableStateOf(vehicle?.displayName ?: "") }
    var vehicleType by remember { mutableStateOf(vehicle?.vehicleType ?: "Bus") }
    var typeExpanded by remember { mutableStateOf(false) }

    val vehicleTypes = listOf("Bus", "Car", "Truck", "Bike", "Van")
    val isEditing = vehicle != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Vehicle" else "Add Vehicle") },
        text = {
            Column {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g., School Bus") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = vehicleNo,
                    onValueChange = { vehicleNo = it },
                    label = { Text("Vehicle Number") },
                    placeholder = { Text("e.g., MP09FA6814") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = imei,
                    onValueChange = { imei = it },
                    label = { Text("IMEI Number") },
                    placeholder = { Text("e.g., 123456789012345") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = vehicleType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vehicle Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        vehicleTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    vehicleType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(imei, vehicleNo, displayName, vehicleType) },
                enabled = imei.isNotBlank() && vehicleNo.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HomeLocationDialog(
    homeLocation: HomeLocation,
    onDismiss: () -> Unit,
    onSave: (lat: Double, lng: Double, name: String) -> Unit
) {
    var name by remember { mutableStateOf(homeLocation.name) }
    var latitude by remember { mutableStateOf(homeLocation.latitude.toString()) }
    var longitude by remember { mutableStateOf(homeLocation.longitude.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Home Location") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name") },
                    placeholder = { Text("e.g., Home") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    placeholder = { Text("e.g., 22.7196") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    placeholder = { Text("e.g., 75.8577") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()
                    if (lat != null && lng != null) {
                        onSave(lat, lng, name)
                    }
                },
                enabled = latitude.toDoubleOrNull() != null && longitude.toDoubleOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
