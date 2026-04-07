# Hatzolah Members App

Android application for Hatzolah emergency medical services volunteers. Manages dispatch notifications, hospital directory, member contacts, call logging, mileage tracking, and RMA integration.

## Features

### Dispatch & Navigation
- **SMS Listener**: Monitors incoming dispatch messages and auto-extracts addresses
- **Lock Screen Notification**: High-priority notification with one-tap Google Maps navigation
- **Dashboard**: Shows most recent dispatch with navigate button

### Directories
- **Hospital Directory**: Searchable database with ER locations, access codes, kosher rooms, bed availability, department hotlines, and communication systems. Clickable addresses open Google Maps.
- **Member Directory**: Lists all verified volunteers with phone, WhatsApp, and email contacts

### Call Management
- **Call Logging**: Records date, time, address, mileage, duration, and RMA status
- **GPS Mileage Tracking**: Automatic distance calculation during active calls
- **Call Documentation**: HIPAA-conscious patient notes (name, DOB, medical notes) - private to each member
- **RMA Integration**: Quick-dial hotline with outcome logging (approved/denied/transferred)

### Analytics
- **Team Statistics**: Aggregate call volume, total miles, average response time (no individual breakdowns)
- **Personal Analytics**: Your own calls per month, total miles, and hours

### Security
- **SMS Verification**: One-time code sent to registered phone number
- **Admin-Controlled Registration**: Members must be added by an admin before they can log in
- **Data Privacy**: Patient data and personal call history are private to each user

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM with Hilt dependency injection
- **Database**: Room (local SQLite)
- **Location**: Google Play Services Location API
- **Navigation**: Jetpack Navigation Compose

## Project Structure

```
app/src/main/java/com/hatzolah/app/
├── HatzolahApp.kt              # Application class, notification channels
├── di/                          # Hilt dependency injection modules
├── data/
│   ├── database/
│   │   ├── entity/              # Member, Hospital, CallLog entities
│   │   ├── dao/                 # Data access objects
│   │   └── HatzolahDatabase.kt # Room database
│   └── repository/              # Repository layer
├── service/
│   ├── SmsReceiver.kt           # Dispatch SMS broadcast receiver
│   ├── SmsParser.kt             # Address extraction from dispatch messages
│   ├── SmsVerificationService.kt# Phone verification authentication
│   └── LocationTrackingService.kt# GPS mileage tracking foreground service
├── util/
│   ├── DispatchNotificationHelper.kt # Lock screen notifications
│   └── PreferencesManager.kt    # App preferences/session management
└── ui/
    ├── MainActivity.kt          # Entry point with auth gate
    ├── theme/                   # Material 3 theming
    ├── navigation/              # Bottom nav + routing
    ├── auth/                    # SMS verification login
    ├── dashboard/               # Home screen with dispatch + stats
    ├── hospital/                # Hospital directory
    ├── member/                  # Member directory
    ├── callhistory/             # Personal call history + documentation
    ├── analytics/               # Team & personal statistics
    ├── rma/                     # RMA hotline + outcome logging
    ├── protocols/               # Emergency medical protocols
    └── admin/                   # Member management + settings
```

## Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on a physical Android device (SMS and location features require real hardware)

## Publishing to Google Play

1. Test thoroughly on a real Android device
2. Review the [Privacy Policy](PRIVACY_POLICY.md)
3. Pay the $25 Google Play Developer registration fee
4. Generate a signed APK/AAB
5. Submit to Google Play (expect 24-48 hour review)

## License

Private - Hatzolah internal use only.
