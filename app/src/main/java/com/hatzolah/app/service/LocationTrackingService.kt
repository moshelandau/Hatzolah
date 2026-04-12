package com.hatzolah.app.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.hatzolah.app.HatzolahApp
import com.hatzolah.app.data.repository.CallLogRepository
import com.hatzolah.app.data.repository.FirebaseResponderRepository
import com.hatzolah.app.util.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that tracks location via GPS during active calls
 * and calculates total mileage traveled.
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var callLogRepository: CallLogRepository
    @Inject lateinit var firebaseResponderRepository: FirebaseResponderRepository
    @Inject lateinit var preferencesManager: PreferencesManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    private var lastLocation: Location? = null
    private var totalDistanceMeters: Double = 0.0
    private var activeCallLogId: Long = -1
    private var updatesStarted: Boolean = false
    private var hasEverBeenOnScene: Boolean = false

    companion object {
        const val EXTRA_CALL_LOG_ID = "call_log_id"
        private const val TRACKING_NOTIFICATION_ID = 2001
        private const val LOCATION_INTERVAL_MS = 5000L
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    processLocation(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground IMMEDIATELY to satisfy Android 8+ requirement.
        // If any crash happens before this, system will kill the service.
        val notification = NotificationCompat.Builder(this, HatzolahApp.TRACKING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Hatzolah - Tracking Active")
            .setContentText("Recording mileage for current call")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        try {
            startForeground(TRACKING_NOTIFICATION_ID, notification)
        } catch (_: Throwable) {
            stopSelf()
            return START_NOT_STICKY
        }

        activeCallLogId = intent?.getLongExtra(EXTRA_CALL_LOG_ID, -1) ?: -1

        if (activeCallLogId <= 0) {
            // No valid call log - don't waste battery on nothing
            stopSelf()
            return START_NOT_STICKY
        }

        // Check permissions before starting location updates
        if (!hasLocationPermission()) {
            stopSelf()
            return START_NOT_STICKY
        }

        startLocationUpdates()
        // START_NOT_STICKY - don't restart without the call log id (would be invalid)
        return START_NOT_STICKY
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun startLocationUpdates() {
        val client = fusedLocationClient ?: return
        val callback = locationCallback ?: return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateIntervalMillis(LOCATION_INTERVAL_MS / 2)
            .build()

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            updatesStarted = true
        } catch (_: SecurityException) {
            stopSelf()
        } catch (_: Throwable) {
            stopSelf()
        }
    }

    private fun processLocation(location: Location) {
        lastLocation?.let { prev ->
            val distance = prev.distanceTo(location).toDouble()
            // Filter out impossible jumps (>500m per update = 200+ mph)
            if (distance < 500.0) {
                totalDistanceMeters += distance
                updateCallLogMileage()
            }
        }
        lastLocation = location
        updateResponderStatus(location)
    }

    /**
     * Compares current location to the active dispatch scene and updates the
     * responder status: BLUE (en route) -> GREEN (on scene) -> RED (left after
     * having been on scene). Pushes the update to Firebase if configured.
     */
    private fun updateResponderStatus(location: Location) {
        val scene = preferencesManager.getDispatchSceneLocation() ?: return
        val cad = preferencesManager.getActiveDispatchCad()
        if (cad.isBlank()) return

        val results = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, scene.first, scene.second, results)
        val distanceMeters = results[0]
        val arrivalRadius = preferencesManager.getArrivalRadiusMeters()

        val current = preferencesManager.getResponderStatus()
        val newStatus = when {
            distanceMeters <= arrivalRadius -> {
                hasEverBeenOnScene = true
                "GREEN"
            }
            hasEverBeenOnScene && distanceMeters > arrivalRadius * 3 -> "RED"
            else -> if (current == "RED") "RED" else "BLUE"
        }

        if (newStatus != current) {
            preferencesManager.setResponderStatus(newStatus)
        }

        // Push to Firebase (fire-and-forget)
        val memberId = preferencesManager.getLoggedInMemberId()
        if (memberId > 0) {
            serviceScope.launch {
                try {
                    firebaseResponderRepository.updateStatus(
                        cad = cad,
                        memberId = memberId,
                        status = newStatus,
                        lat = location.latitude,
                        lng = location.longitude
                    )
                } catch (_: Throwable) { /* ignore */ }
            }
        }
    }

    private fun updateCallLogMileage() {
        if (activeCallLogId <= 0) return
        val miles = totalDistanceMeters / 1609.344
        serviceScope.launch {
            try {
                callLogRepository.getCallLogById(activeCallLogId)?.let { callLog ->
                    callLogRepository.updateCallLog(callLog.copy(milesTraveled = miles))
                }
            } catch (_: Throwable) { /* don't crash the service on DB errors */ }
        }
    }

    override fun onDestroy() {
        try {
            if (updatesStarted) {
                locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
            }
        } catch (_: Throwable) {}
        try {
            serviceScope.cancel()
        } catch (_: Throwable) {}
        locationCallback = null
        fusedLocationClient = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
