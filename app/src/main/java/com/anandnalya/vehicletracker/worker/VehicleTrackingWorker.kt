package com.anandnalya.vehicletracker.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.anandnalya.vehicletracker.MainActivity
import com.anandnalya.vehicletracker.R
import com.anandnalya.vehicletracker.data.model.VehicleConfig
import com.anandnalya.vehicletracker.data.repository.Result
import com.anandnalya.vehicletracker.data.repository.VehicleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class VehicleTrackingWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: VehicleRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "vehicle_tracking_work"
        const val CHANNEL_ID = "vehicle_tracking_channel"
        const val NOTIFICATION_ID = 1001

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<VehicleTrackingWorker>(
                15, TimeUnit.MINUTES // Minimum interval for periodic work
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val savedImei = repository.getSavedVehicleImei().first()
            val savedType = repository.getSavedVehicleType().first()
            val savedVehicleNo = repository.getSavedVehicleNo().first()

            if (savedImei.isNullOrEmpty() || savedVehicleNo.isNullOrEmpty()) {
                return Result.success()
            }

            val config = VehicleConfig(
                imeiNo = savedImei,
                vehicleType = savedType ?: "Bus",
                vehicleNo = savedVehicleNo
            )

            when (val result = repository.getVehicleStatus(config)) {
                is com.anandnalya.vehicletracker.data.repository.Result.Success -> {
                    val vehicle = result.data
                    showNotification(
                        title = "Vehicle: ${vehicle.vehicleNo}",
                        message = "Status: ${vehicle.status} | Speed: ${vehicle.speed} ${vehicle.speedUnit}"
                    )
                    Result.success()
                }
                is com.anandnalya.vehicletracker.data.repository.Result.Error -> {
                    Result.retry()
                }
                else -> Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vehicle Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Vehicle location updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
