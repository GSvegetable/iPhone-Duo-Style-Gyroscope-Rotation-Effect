package com.tiltfold.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private val Ink = Color(0xFF10131B)
private val Muted = Color(0xFF8D96A8)
private val Accent = Color(0xFFB7F36B)
private val Panel = Color(0xFF191E29)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 19, 27)
        window.navigationBarColor = android.graphics.Color.rgb(16, 19, 27)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
                    TiltFoldScreen()
                }
            }
        }
    }
}

@Composable
private fun TiltFoldScreen() {
    val (sensorTilt, sensorAvailable) = rememberTilt()
    var manualTilt by remember { mutableFloatStateOf(0f) }
    var intensity by remember { mutableFloatStateOf(0.82f) }
    var mode by remember { mutableStateOf(FoldMode.THREE_D) }
    val tilt = (if (sensorAvailable) sensorTilt else manualTilt) * intensity

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF10131B), Color(0xFF171E2A), Color(0xFF10131B))))
            .padding(horizontal = 22.dp, vertical = 18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Accent), contentAlignment = Alignment.Center) {
                Text("T", color = Ink, fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
            Column(Modifier.padding(start = 11.dp)) {
                Text("TILTFOLD", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("MOTION STUDY  /  ANDROID", color = Muted, fontSize = 9.sp, letterSpacing = 1.4.sp)
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.clip(CircleShape).background(Color(0xFF242D3A)).padding(horizontal = 11.dp, vertical = 7.dp)) {
                Text(if (sensorAvailable) "SENSOR LIVE" else "MANUAL MODE", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("One screen.", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.4).sp)
            Text("A different dimension.", color = Accent, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.4).sp)
            Text("Tilt your phone and watch the surface respond.", color = Muted, fontSize = 13.sp)
        }

        FoldPreview(tilt, mode, Modifier.fillMaxWidth().height(340.dp))

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(Panel).border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("RENDER MODE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Spacer(Modifier.weight(1f))
                Text("${(abs(tilt) * 100).toInt()}% TILT", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FoldMode.values().forEach { item ->
                    val selected = mode == item
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Accent else Color(0xFF252C39))
                            .border(1.dp, if (selected) Accent else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { mode = item }.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(item.label, color = if (selected) Ink else Color(0xFFD5D9E1), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("INTENSITY", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text("${(intensity * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
            }
            Slider(value = intensity, onValueChange = { intensity = it }, valueRange = 0.2f..1f)
            if (!sensorAvailable) {
                Text("No motion sensor detected. Use the slider to preview the fold.", color = Muted, fontSize = 11.sp)
                Slider(value = manualTilt, onValueChange = { manualTilt = it }, valueRange = -1f..1f)
            } else {
                Text("Live motion input · move slowly for the smoothest response", color = Muted, fontSize = 11.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TILTFOLD  /  EXPERIMENTAL", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
            Text("v2.0.0", color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun FoldPreview(tilt: Float, mode: FoldMode, modifier: Modifier = Modifier) {
    val rotation = if (mode == FoldMode.THREE_D) tilt * FoldTuning.MAX_ROTATION_DEGREES else 0f
    val fold = abs(tilt)
    Box(
        modifier = modifier.clip(RoundedCornerShape(28.dp))
            .background(Brush.radialGradient(listOf(Color(0xFF344256), Color(0xFF202A39), Color(0xFF151B26)))),
        contentAlignment = Alignment.Center
    ) {
        val foldAngle = when (mode) {
            FoldMode.THREE_D -> rotation
            FoldMode.SOFT -> tilt * 9f
            FoldMode.OFF -> 0f
        }
        Row(
            Modifier.fillMaxWidth(0.82f).height(310.dp)
                .clip(RoundedCornerShape(25.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.weight(1f).fillMaxSize()
                    .graphicsLayer {
                        rotationY = -foldAngle * 0.62f
                        transformOrigin = TransformOrigin(1f, 0.5f)
                        cameraDistance = FoldTuning.CAMERA_DISTANCE * density
                    }
                    .background(Brush.linearGradient(listOf(Color(0xFFB8D5F5), Color(0xFF8AA7D4))))
                    .border(1.dp, Color.White.copy(alpha = 0.28f))
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Text("DUO DISPLAY", color = Color(0xFF17243A), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Column {
                        Text("Beyond", color = Color(0xFF142039), fontSize = 27.sp, fontWeight = FontWeight.Light, letterSpacing = (-1.2).sp)
                        Text("the", color = Color(0xFF142039), fontSize = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.2).sp)
                    }
                    Text("TILTFOLD  /  01", color = Color(0xFF263D5B), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
            Box(
                Modifier.weight(1f).fillMaxSize()
                    .graphicsLayer {
                        rotationY = foldAngle * 0.62f
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        cameraDistance = FoldTuning.CAMERA_DISTANCE * density
                    }
                    .background(Brush.linearGradient(listOf(Color(0xFF8AA7D4), Color(0xFF536C9B))))
                    .border(1.dp, Color.White.copy(alpha = 0.28f))
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
                    Text("02 / 02", color = Color(0xFF263D5B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("flat.", color = Color(0xFF142039), fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.2).sp)
                        Text("A surface shaped by motion.", color = Color(0xFF243650), fontSize = 9.sp)
                    }
                    Text("LIVE SURFACE", color = Color(0xFF263D5B), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
        Box(
            Modifier.align(Alignment.Center).size(width = 2.dp, height = 278.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF263D5B).copy(alpha = 0.75f), Color.Transparent)))
        )
        if (mode != FoldMode.OFF && fold > 0.04f) {
            Box(
                Modifier.align(if (tilt >= 0f) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 26.dp).size(width = 34.dp, height = 250.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = fold * 0.52f), Color.Transparent)))
            )
        }
        Text(
            text = when (mode) {
                FoldMode.THREE_D -> "PERSPECTIVE FOLD"
                FoldMode.SOFT -> "SOFT SURFACE"
                FoldMode.OFF -> "FLAT DISPLAY"
            },
            color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp, letterSpacing = 1.6.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
        )
    }
}
