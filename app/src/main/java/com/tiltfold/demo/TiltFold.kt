package com.tiltfold.demo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

@Composable
fun rememberTilt(): Pair<Float, Boolean> {
    val context = LocalContext.current
    var tilt by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorAvailable = sensor != null
        var filtered = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size < 2) return
                val target = (event.values[0] / SensorManager.GRAVITY_EARTH / 0.72f).coerceIn(-1f, 1f)
                filtered += (target - filtered) * 0.18f
                tilt = if (abs(filtered) < 0.012f) 0f else filtered
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (manager != null && sensor != null) {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            manager?.unregisterListener(listener)
            sensorAvailable = false
        }
    }
    return tilt to sensorAvailable
}

enum class FoldMode(val label: String) {
    THREE_D("3D fold"),
    SOFT("Soft curve"),
    OFF("Off")
}

object FoldTuning {
    const val MAX_ROTATION_DEGREES = 42f
    const val CAMERA_DISTANCE = 18f
}
