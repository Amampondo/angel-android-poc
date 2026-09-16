package za.co.angel.poc

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var eventText: TextView
    private lateinit var armButton: Button

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) armDetection()
            updateUi()
        }

    private val serviceEvents = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = updateUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        eventText = findViewById(R.id.eventText)
        armButton = findViewById(R.id.armButton)

        armButton.setOnClickListener {
            if (isArmed()) stopDetection() else requestPermissionAndArm()
        }
        findViewById<Button>(R.id.testButton).setOnClickListener { runTestAlert() }
        updateUi()
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(ShakeDetectionService.ACTION_SHAKE_DETECTED)
            addAction(ShakeDetectionService.ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(
            this,
            serviceEvents,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        unregisterReceiver(serviceEvents)
        super.onStop()
    }

    private fun requestPermissionAndArm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        armDetection()
    }

    private fun armDetection() {
        ContextCompat.startForegroundService(this, Intent(this, ShakeDetectionService::class.java))
    }

    private fun stopDetection() {
        startService(
            Intent(this, ShakeDetectionService::class.java)
                .setAction(ShakeDetectionService.ACTION_STOP)
        )
    }

    private fun runTestAlert() {
        val preferences = getSharedPreferences(ShakeDetectionService.PREFERENCES, MODE_PRIVATE)
        val count = preferences.getInt(ShakeDetectionService.KEY_TRIGGER_COUNT, 0) + 1
        preferences.edit()
            .putInt(ShakeDetectionService.KEY_TRIGGER_COUNT, count)
            .putLong(ShakeDetectionService.KEY_LAST_TRIGGER_MS, System.currentTimeMillis())
            .apply()
        updateUi()
    }

    private fun isArmed(): Boolean =
        getSharedPreferences(ShakeDetectionService.PREFERENCES, MODE_PRIVATE)
            .getBoolean(ShakeDetectionService.KEY_ARMED, false)

    private fun updateUi() {
        val preferences = getSharedPreferences(ShakeDetectionService.PREFERENCES, MODE_PRIVATE)
        val armed = preferences.getBoolean(ShakeDetectionService.KEY_ARMED, false)
        val count = preferences.getInt(ShakeDetectionService.KEY_TRIGGER_COUNT, 0)
        val lastTrigger = preferences.getLong(ShakeDetectionService.KEY_LAST_TRIGGER_MS, 0)

        statusText.text = getString(if (armed) R.string.status_armed else R.string.status_disarmed)
        statusText.setTextColor(
            ContextCompat.getColor(this, if (armed) R.color.angel_green else R.color.angel_muted)
        )
        armButton.text = getString(if (armed) R.string.disarm else R.string.arm)
        eventText.text = if (lastTrigger == 0L) {
            getString(R.string.no_events)
        } else {
            getString(
                R.string.event_summary,
                count,
                DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(lastTrigger))
            )
        }
    }
}
