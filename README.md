# Vehicle Tracker

An Android application for real-time vehicle tracking with Google Maps integration.

## Features

- Real-time vehicle location tracking on Google Maps
- Background location updates using WorkManager
- Proximity alerts with customizable distance thresholds
- Multi-vehicle support with configuration management
- Session-based authentication with cookie persistence
- Configurable home location for distance calculations

## Requirements

- Android SDK 26+ (Android 8.0 Oreo)
- Java 17
- Google Maps API key

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose, Material 3
- **Maps**: Google Maps Compose
- **Networking**: Retrofit, OkHttp
- **Dependency Injection**: Hilt
- **Background Processing**: WorkManager
- **Local Storage**: DataStore Preferences
- **Async**: Kotlin Coroutines & Flow

## Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/anandnalya/androidapp.git
   cd androidapp
   ```

2. Create a `secrets.properties` file in the project root (see [Configuration](#configuration) below)

3. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```

4. Install on a connected device or emulator:
   ```bash
   ./gradlew installDebug
   ```

## Configuration

Create a `secrets.properties` file in the project root with the following settings:

```properties
# Google Maps API Key (required)
MAPS_API_KEY=your_google_maps_api_key_here

# Default home location for proximity alerts
HOME_LATITUDE=your_home_latitude
HOME_LONGITUDE=your_home_longitude
HOME_NAME=Home

# Default vehicle configuration
DEFAULT_VEHICLE_IMEI=your_vehicle_imei
DEFAULT_VEHICLE_TYPE=Bus
DEFAULT_VEHICLE_NO=your_vehicle_number
DEFAULT_VEHICLE_NAME=your_vehicle_name
```

| Property | Description |
|----------|-------------|
| `MAPS_API_KEY` | Google Maps API key for map display |
| `HOME_LATITUDE` | Latitude of home location for proximity alerts |
| `HOME_LONGITUDE` | Longitude of home location for proximity alerts |
| `HOME_NAME` | Display name for home location |
| `DEFAULT_VEHICLE_IMEI` | IMEI number of the default vehicle to track |
| `DEFAULT_VEHICLE_TYPE` | Vehicle type (e.g., Bus, Car) |
| `DEFAULT_VEHICLE_NO` | Vehicle registration number |
| `DEFAULT_VEHICLE_NAME` | Display name for the vehicle |

## Project Structure

```
app/src/main/java/com/anandnalya/vehicletracker/
├── data/
│   ├── model/          # Data classes (VehicleStatus, VehicleConfig)
│   ├── network/        # API service, session management, interceptors
│   └── repository/     # Vehicle repository
├── di/                 # Hilt dependency injection modules
├── ui/
│   ├── components/     # Reusable Compose components
│   ├── screens/        # App screens
│   ├── theme/          # Material theme configuration
│   └── viewmodel/      # ViewModels
└── worker/             # Background WorkManager tasks
```

## Permissions

The app requires the following permissions:

| Permission | Purpose |
|------------|---------|
| `INTERNET` | API communication |
| `ACCESS_FINE_LOCATION` | Precise location services |
| `ACCESS_COARSE_LOCATION` | Approximate location services |
| `FOREGROUND_SERVICE` | Background tracking |
| `POST_NOTIFICATIONS` | Proximity alerts |
| `VIBRATE` | Alert vibration |

## License

MIT License
