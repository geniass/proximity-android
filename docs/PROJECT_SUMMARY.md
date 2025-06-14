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
- On app start or device boot, the app registers for activity and location updates using PendingIntent and system receivers (no persistent foreground service).
- When movement or significant location change is detected, the app processes the event and sends notifications as needed.
- When the device is stationary for 5+ minutes, GPS tracking is paused; it resumes immediately when movement is detected.
- All notifications are managed through a centralized NotificationService for consistency.
- On app start, the app gets the last known position from the system rather than storing it in a database, ensuring up to date location data.

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

## TODO Features
- Allow distance from POI to be configurable per trip.
- Allow importing or syncing trips with google maps saved places lists.

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
