# Project Summary: Proximity Native Android App

## Overview
Proximity is a native Android application designed to help users discover and be reminded of places of interest nearby, especially while traveling. The app allows users to manage trips and places of interest, and leverages intelligent background location tracking to notify users when they are close to saved locations. The architecture is optimized for battery efficiency and reliable background operation, following modern Android best practices.

## Core Functionality
- **Trip Management:** Users can create, edit, and manage multiple trips, each with its own set of places of interest.
- **Places of Interest:** For each trip, users can add, view, and remove places they want to visit. Each place includes details such as name, coordinates, and Google Place ID.
- **Proximity Notifications:** The app tracks the user's location in the background and notifies them when they are near a saved place of interest (e.g., within 500 meters).
- **Distance Calculation:** The app calculates and displays the distance from the user's current location to each place of interest.
- **Intelligent Activity Recognition:** Uses device sensors to detect activity (WALKING, RUNNING, IN_VEHICLE, STILL) and pauses/resumes GPS tracking to save battery.
- **Significant Movement Detection:** Only triggers notifications for movements of 500+ meters, reducing unnecessary alerts and battery usage.

## How It Works

### Geofencing Strategy
The app uses a single dynamic geofence centered on the user's current location rather than creating individual geofences for each POI. This approach is designed to handle the reality that users may have hundreds or thousands of POIs across multiple trips, while Google limits apps to 100 active geofences maximum.

**Key aspects of the geofencing method:**
- **Single 200m radius geofence** - Created at the user's current location when the app starts or location changes significantly
- **EXIT-only trigger** - The app only wakes up when the user exits the geofence (i.e., moves more than 200m from their last known position)
- **Dynamic repositioning** - When triggered, the app gets the new location, checks all POIs for proximity (within 500m), sends notifications if any are found, then creates a new geofence at the new location
- **Avoids Google's 100 geofence limit** - By using only one geofence instead of one per POI, the app can handle unlimited POIs without hitting platform restrictions
- **Reduces complexity** - No need for geofence clustering algorithms, POI prioritization, or complex geofence management logic

This method ensures the app only performs location checks when the user has moved a significant distance, optimizing battery life while maintaining reliable proximity detection for any number of saved places.

### Background Processing
- On app start or device boot, the app registers for geofence events using PendingIntent and BroadcastReceiver (no persistent foreground service)
- When the user exits the geofence, the GeofenceTransitionReceiver handles the event and processes proximity checks
- All notifications are managed through a centralized NotificationUtils for consistency
- The app gets fresh location data from the system rather than storing stale location data in the database

## User Interface
- **Trips List Screen:** Displays all trips with progress indicators, trip details, and options to add or edit trips.
- **Trip Form Sheet:** Allows users to create or edit a trip, set the name, start/end dates, and manage associated places.
- **Places of Interest Screen:** Shows all places for a selected trip. Users can toggle between a list and a map view. Includes a search/add button and a tracking status bar.
- **Map View:** Integrates Google Maps to display all places as markers. Users can tap markers for details and actions.
                User can open places in Google Maps for navigation.
- **Tracking Status Bar:** Indicates whether background location tracking is active.
- **Debug Features:** Includes debug toggles and info for development and troubleshooting.

## Features
- Add, edit, and delete trips and places of interest.
- View places in both list and interactive map formats.
- Real-time distance updates based on user location.
- Open places directly in Google Maps for navigation.
- Responsive UI for various device sizes.
- Background location tracking for proximity notifications.
- Smart pause/resume of tracking based on activity recognition.
- Asks for permissions when needed, and guides users to the correct settings page for background location permissions.
- Persistent state across app restarts and device reboots.
- Offline-first design: The app can function without an internet connection, relying on locally stored data for trips and places of interest.
- Show distance and direction in notifications.
- Add action buttons to notifications.
- Group notifications for nearby places.

## TODO
- **Code TODOs:**
    - `app/src/main/java/dev/croock/proximity/MainActivity.kt`: Add trip functionality.
    - `app/src/main/java/dev/croock/proximity/PlacesOfInterestScreen.kt`: Implement search for places of interest.
    - `app/src/main/res/xml/data_extraction_rules.xml`: Configure backup rules.
- **Feature TODOs:**
    - Allow distance from POI to be configurable per trip.
    - Allow importing or syncing trips with Google Maps saved places lists.
    - Trip-aware filtering: Only activate geofences for current/active trips.
    - Cooldown periods for notifications for the same POI.
    - Context-aware filtering (e.g., for restaurants).
    - Adapt to Android's background execution limits.
    - Add "Ignore" action button to notifications
    - Implement smart timing for notifications to avoid spam.

## Future Battery Optimization Ideas
If further battery optimization is needed, these strategies could be implemented:

### Smart Geofence Management
- **Activity-based activation** - Only create geofences when user is walking/moving; disable during STILL periods
- **Time-based scheduling** - Pause geofencing during sleep hours (e.g., 11 PM - 7 AM) or user-defined quiet periods
- **Battery level adaptation** - Increase geofence radius or reduce check frequency when battery is low (<20%)

### Location Strategy Optimization
- **Adaptive radius** - Increase geofence radius in rural areas (less POI density), decrease in urban areas
- **Distance-based POI filtering** - Only check POIs within reasonable range (e.g., 10km) instead of all POIs
- **Movement pattern learning** - Use historical data to predict likely routes and pre-filter relevant POIs
- **Significant location change** - Use Android's PASSIVE_PROVIDER or significant location changes instead of precise GPS when possible

### Notification Intelligence
- **Batch processing** - Group multiple nearby POI checks into single location request
- **Smart wake patterns** - Align processing with other app activities to reduce independent wake-ups

### System Integration
- **Doze mode compliance** - Ensure proper behavior during Android's deep sleep states
- **Network usage optimization** - Cache POI data locally to minimize API calls during background processing
- **Sensor fusion** - Use accelerometer/gyroscope to detect movement before GPS activation

These optimizations would maintain the app's core functionality while further reducing battery impact for users with extensive POI collections or those prioritizing battery life.

## Stretch Goals
- **Landmark-based directions** - Instead of using cardinal directions (north, south, etc.), use nearby landmarks as directional references (e.g., "350m toward Central Station" or "200m past the McDonald's"). This would require querying nearby places from the Google Places API based on current location and calculating which landmark is in the same direction as the target POI.

## Architecture Overview
- **No Foreground Service:** All background work is handled via system-managed PendingIntent and BroadcastReceiver, not a persistent service.
- **NotificationService:** Centralized notification logic for all app components.
- **BootReceiver:** Ensures activity/location tracking resumes after device reboot.
- **ActivityLocationReceiver:** Handles activity/location updates and triggers notifications.
- **Modern Android Practices:** Uses AndroidX, Notification Channels, and runtime permissions.

## Technologies Used
- **Kotlin** for native Android development.
- **Google Maps SDK** for map integration.
- **Fused Location Provider** for efficient location tracking.
- **Activity Recognition API** for detecting user activity.
- **AndroidX** for modern Android components and best practices.
- **Room Database / SQLite** for persistent storage of trips and places of interest.

This architecture provides reliable, battery-efficient background tracking and notification delivery, following current Android best practices for background work, while offering a rich trip and place management experience inspired by the original Flutter app.
