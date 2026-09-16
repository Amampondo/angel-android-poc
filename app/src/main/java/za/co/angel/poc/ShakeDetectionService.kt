package za.co.angel.poc

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ShakeDetectionService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var preferences: SharedPreferences
    private var motionSensor: Sensor? = null

    private val detector = ShakeDetector(onShake = ::handleShake)

    override fun onCreate() {
        super.onCreate()
        preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        motionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopDetection()
            return START_NOT_STICKY
        }

        startForeground(ONGOING_NOTIFICATION_ID, ongoingNotification())
        val registered = motionSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        } ?: false

        if (!registered) {
            preferences.edit().putBoolean(KEY_ARMED, false).apply()
            stopSelf()
            return START_NOT_STICKY
        }

        preferences.edit().putBoolean(KEY_ARMED, true).apply()
        sendBroadcast(Intent(ACTION_STATE_CHANGED).setPackage(packageName))
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        detector.addSample(
            event.values[0],
            event.values[1],
            event.values[2],
            SystemClock.elapsedRealtime()
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        preferences.edit().putBoolean(KEY_ARMED, false).apply()
        sendBroadcast(Intent(ACTION_STATE_CHANGED).setPackage(packageName))
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleShake() {
        val count = preferences.getInt(KEY_TRIGGER_COUNT, 0) + 1
        preferences.edit()
            .putInt(KEY_TRIGGER_COUNT, count)
            .putLong(KEY_LAST_TRIGGER_MS, System.currentTimeMillis())
            .apply()

        vibrateConfirmation()
        NotificationManagerCompat.from(this).notify(ALERT_NOTIFICATION_ID, alertNotification())
        sendBroadcast(Intent(ACTION_SHAKE_DETECTED).setPackage(packageName))
    }

    private fun stopDetection() {
        sensorManager.unregisterListener(this)
        detector.reset()
        preferences.edit().putBoolean(KEY_ARMED, false).apply()
        sendBroadcast(Intent(ACTION_STATE_CHANGED).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun vibrateConfirmation() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun ongoingNotification() = NotificationCompat.Builder(this, CHANNEL_MONITORING)
        .setSmallIcon(R.drawable.ic_angel)
        .setContentTitle(getString(R.string.monitoring_title))
        .setContentText(getString(R.string.monitoring_text))
        .setContentIntent(openAppPendingIntent())
        .setOngoing(true)
        .setSilent(true)
        .build()

    private fun alertNotification() = NotificationCompat.Builder(this, CHANNEL_ALERTS)
        .setSmallIcon(R.drawable.ic_angel)
        .setContentTitle(getString(R.string.test_alert_title))
        .setContentText(getString(R.string.test_alert_text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setContentIntent(openAppPendingIntent())
        .setAutoCancel(true)
        .build()

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MONITORING,
                getString(R.string.monitoring_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                getString(R.string.alert_channel),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
    }

    companion object {
        const val ACTION_STOP = "za.co.angel.poc.STOP_DETECTION"
        const val ACTION_SHAKE_DETECTED = "za.co.angel.poc.SHAKE_DETECTED"
        const val ACTION_STATE_CHANGED = "za.co.angel.poc.STATE_CHANGED"
        const val PREFERENCES = "angel_state"
        const val KEY_ARMED = "armed"
        const val KEY_TRIGGER_COUNT = "trigger_count"
        const val KEY_LAST_TRIGGER_MS = "last_trigger_ms"

        private const val CHANNEL_MONITORING = "shake_monitoring"
        private const val CHANNEL_ALERTS = "test_alerts"
        private const val ONGOING_NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 1002
    }
}

