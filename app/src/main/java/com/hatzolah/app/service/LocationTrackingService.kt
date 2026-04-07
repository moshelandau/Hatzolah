package com.hatzolah.app.service

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.hatzolah.app.HatzolahApp
import com.hatzolah.app.data.repository.CallLogRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that tracks location via GPS during active calls
 * and calculates total mileage traveled.
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var callLogRepository: CallLogRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var lastLocation: Location? = null
    private var totalDistanceMeters: Double = 0.0
    private var activeCallLogId: Long = -1

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
        activeCallLogId = intent?.getLongExtra(EXTRA_CALL_LOG_ID, -1) ?: -1

        val notification = NotificationCompat.Builder(this, HatzolahApp.TRACKING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Hatzolah - Tracking Active")
            .setContentText("Recording mileage for current call")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(TRACKING_NOTIFICATION_ID, notification)
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateIntervalMillis(LOCATION_INTERVAL_MS / 2)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                request, locationCallback, Looper.getMainLooper()
            )
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun processLocation(location: Location) {
        lastLocation?.let { prev ->
            totalDistanceMeters += prev.distanceTo(location).toDouble()
            updateCallLogMileage()
        }
        lastLocation = location
    }

    private fun updateCallLogMileage() {
        if (activeCallLogId <= 0) return
        val miles = totalDistanceMeters / 1609.344
        serviceScope.launch {
            callLogRepository.getCallLogById(activeCallLogId)?.let { callLog ->
                callLogRepository.updateCallLog(callLog.copy(milesTraveled = miles))
            }
        }
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
