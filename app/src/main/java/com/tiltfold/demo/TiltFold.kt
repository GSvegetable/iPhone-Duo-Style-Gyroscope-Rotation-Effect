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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Reads device roll only while the host Activity is resumed. */
@Composable
fun rememberTilt(): Pair<Float, Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tilt by remember { mutableFloatStateOf(0f) }
    var sensorAvailable by remember { mutableStateOf(false) }

    DisposableEffect(context, lifecycleOwner) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val smoother = TiltSmoother()
        var listening = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.values.size < 3) return
                tilt = smoother.update(
                    TiltMath.normalize(event.values[0], event.values[1], event.values[2])
                )
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun stopListening() {
            if (listening) manager?.unregisterListener(listener)
            listening = false
            smoother.reset()
            tilt = 0f
        }

        fun startListening() {
            if (listening || manager == null || sensor == null) return
            listening = manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            sensorAvailable = listening
            if (!listening) tilt = 0f
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> startListening()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> stopListening()
                else -> Unit
            }
        }

        sensorAvailable = sensor != null
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            startListening()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopListening()
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
