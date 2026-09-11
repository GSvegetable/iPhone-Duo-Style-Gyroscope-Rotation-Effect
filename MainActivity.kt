package com.tiltfold.demo

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.pow

/**
 * Demo 入口。
 *
 * 演示三种倾斜效果，底部按钮切换。
 *
 * 阅读顺序建议：
 *   1. TiltFold.kt   —— 参数 + 传感器 + 三个 AGSL shader
 *   2. 本文件         —— 了解如何把它们组合起来
 *
 * 图层结构是本文件的核心。三个模式各有不同的图层叠法，
 * 每种叠法后面都写了"为什么是这样而不是那样"。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    color = Color(0xFFF6F7F9),
                    modifier = Modifier.fillMaxSize()
                ) {
                    TiltFoldDemo()
                }
            }
        }
    }
}

@Composable
fun TiltFoldDemo() {

    // =========================================================================
    // 一、陀螺仪数据
    // =========================================================================
    val (rawTiltX, _, _) = rememberTilt()

    // 应用防手抖死区：阈值以下的抖动完全不触发效果
    val tiltX = applyDeadZone(rawTiltX, FoldParams.deadZone)

    val absTilt = abs(tiltX).coerceIn(0f, 1f)

    // 凹侧方向：
    //   向右倾（tiltX > 0）→ 屏幕左侧凹（-1）
    //   向左倾（tiltX < 0）→ 屏幕右侧凹（+1）
    //   0 → 无方向，shader 会直接返回原图
    val concaveSide = when {
        tiltX > 0.0001f -> -1f
        tiltX < -0.0001f -> 1f
        else -> 0f
    }

    // =========================================================================
    // 二、连续变化的旋转轴
    // =========================================================================
    // tiltX = 0  → originX = 0.5（轴在中间，不旋转）
    // tiltX = +1 → originX = 0（轴在左边缘，右侧凹）
    // tiltX = -1 → originX = 1（轴在右边缘，左侧凹）
    //
    // 【踩坑记录】
    // 早期版本用 when 判断分档：
    //   tiltX > 0.05f -> 0f
    //   tiltX < -0.05f -> 1f
    //   else -> 0.5f
    // 结果：回正过程中 tiltX 穿过 ±0.05 时轴突变，画面"震"一下。
    // 现在改成连续函数，全程无断点。
    val originX = (0.5f - tiltX * 0.5f).coerceIn(0f, 1f)

    // =========================================================================
    // 三、派生量
    // =========================================================================

    // 拉伸系数（仅 3D 模式用）。
    // 非线性曲线：小倾斜几乎不拉，大倾斜爆发，把侧面露出的黑补回来。
    val stretchC = if (FoldParams.mode == FoldParams.Mode.BLUR_3D && absTilt > 0.0001f) {
        val curve = absTilt.toDouble().pow(FoldParams.STRETCH_EXPONENT).toFloat()
        curve * FoldParams.STRETCH_MAX
    } else 0f

    // 曲面折叠强度（仅曲面模式用）。
    val curvedFoldStrength =
        if (FoldParams.mode == FoldParams.Mode.CURVED_REPROJECT && absTilt > 0.0001f) {
            absTilt * FoldParams.CURVED_FOLD_STRENGTH
        } else 0f

    // 模糊强度（两种模式共用）。
    val blurStrength = (absTilt * FoldParams.BLUR_INTENSITY).coerceIn(0f, 1f)

    // =========================================================================
    // 四、Shader 实例
    // =========================================================================
    // AGSL 从 API 33 起支持，低版本走 RenderEffect.createBlurEffect 降级。
    // remember 避免每次重组重建 RuntimeShader。
    val useShader = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val blurShader = remember { if (useShader) createHomeBlurShader() else null }
    val curvedShader = remember { if (useShader) createCurvedFoldShader() else null }
    val stretchShader = remember { if (useShader) createStretchShader() else null }

    // =========================================================================
    // 五、主界面
    // =========================================================================
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 底色。倾斜时露出的黑就是这个。
            .background(Color.Black)
    ) {
        when (FoldParams.mode) {

            // =================================================================
            // 模式一：3D 旋转 + 凹侧模糊 + 非线性拉伸
            // =================================================================
            //
            // 图层结构（从外到内）：
            //
            //   [模糊层]   renderEffect = HomeBlurShader
            //     [旋转层] rotationY = tiltX * 38°
            //       [拉伸层] renderEffect = StretchShader
            //         [内容] DemoContent
            //
            // 【踩坑记录 1】
            // 早期版本把 rotationY 和 renderEffect 放在同一图层。
            // 两者都作用于图层变换，系统只应用其中一个，
            // 实际表现是 renderEffect 被静默忽略。
            // 必须拆层。
            //
            // 【踩坑记录 2】
            // renderEffect 单独用不生效，必须配合
            // compositingStrategy = CompositingStrategy.Offscreen。
            // 原因是 renderEffect 需要图层先被渲染到独立离屏缓冲，
            // 系统默认不给普通图层分配。
            FoldParams.Mode.BLUR_3D -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                            if (useShader && blurShader != null) {
                                blurShader.setFloatUniform("size", size.width, size.height)
                                blurShader.setFloatUniform("concaveSide", concaveSide)
                                blurShader.setFloatUniform("strength", blurStrength)
                                renderEffect = RenderEffect
                                    .createRuntimeShaderEffect(blurShader, "content")
                                    .asComposeRenderEffect()
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                // API 31~32 降级：均匀模糊（没有凹侧概念，全屏糊）
                                val radius = 8f * blurStrength * this.density
                                renderEffect = RenderEffect
                                    .createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            } else {
                                renderEffect = null
                            }
                        }
                ) {
                    // 旋转时露出的黑底
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationY = tiltX * FoldParams.MAX_TILT_DEGREES
                                transformOrigin = TransformOrigin(originX, 0.5f)
                                cameraDistance = FoldParams.CAMERA_DISTANCE_FACTOR * this.density
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    compositingStrategy = CompositingStrategy.Offscreen
                                    if (useShader && stretchShader != null) {
                                        stretchShader.setFloatUniform("size", size.width, size.height)
                                        stretchShader.setFloatUniform("stretchAmount", stretchC)
                                        stretchShader.setFloatUniform("concaveSide", concaveSide)
                                        renderEffect = RenderEffect
                                            .createRuntimeShaderEffect(stretchShader, "content")
                                            .asComposeRenderEffect()
                                    } else {
                                        renderEffect = null
                                    }
                                }
                        ) {
                            DemoContent()
                        }
                    }
                }
            }

            // =================================================================
            // 模式二：曲面重投影 + 模糊
            // =================================================================
            //
            // 单层搞定。曲面重投影本身是 [0,W]→[0,W] 的双射，
            // 侧面天然不会露黑，不需要额外的拉伸补偿层。
            //
            // 图层结构：
            //   [曲面层]   renderEffect = CurvedFoldShader（含模糊和光照）
            //     [内容]  DemoContent
            FoldParams.Mode.CURVED_REPROJECT -> {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                            if (useShader && curvedShader != null) {
                                curvedShader.setFloatUniform("size", size.width, size.height)
                                curvedShader.setFloatUniform("concaveSide", concaveSide)
                                curvedShader.setFloatUniform("foldStrength", curvedFoldStrength)
                                curvedShader.setFloatUniform("blurStrength", blurStrength)
                                renderEffect = RenderEffect
                                    .createRuntimeShaderEffect(curvedShader, "content")
                                    .asComposeRenderEffect()
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val radius = 8f * blurStrength * this.density
                                renderEffect = RenderEffect
                                    .createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                                    .asComposeRenderEffect()
                            } else {
                                renderEffect = null
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                    DemoContent()
                }
            }

            // =================================================================
            // 模式三：关闭所有效果
            // =================================================================
            FoldParams.Mode.OFF -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    DemoContent()
                }
            }
        }

        // 底部模式切换按钮
        ModeSwitcher(
            currentMode = FoldParams.mode,
            onModeChange = { FoldParams.setMode(it) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/**
 * 模式切换按钮行。
 * 演示用，不涉及效果本身。
 */
@Composable
private fun ModeSwitcher(
    currentMode: FoldParams.Mode,
    onModeChange: (FoldParams.Mode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FoldParams.Mode.values().forEach { mode ->
            val selected = mode == currentMode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (selected) Color(0xFF00E5FF).copy(alpha = 0.85f)
                        else Color.White.copy(alpha = 0.15f)
                    )
                    .clickable { onModeChange(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (mode) {
                        FoldParams.Mode.BLUR_3D -> "3D 旋转"
                        FoldParams.Mode.CURVED_REPROJECT -> "曲面重投影"
                        FoldParams.Mode.OFF -> "关闭"
                    },
                    color = if (selected) Color.Black else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * 演示内容。
 *
 * 几张卡片和几行文字，让效果有东西可以形变。
 * 换成你自己的 UI 也不影响效果层。
 */
@Composable
private fun DemoContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF353A47),  // 顶部深灰蓝
                        Color(0xFFF6F7F9)   // 底部白
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(50.dp))

            Text(
                text = "TiltFold",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "倾斜手机查看效果",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(32.dp))

            // 几张卡片，用来观察拉伸和压缩的形变
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DemoCard("卡片 A", "内容", Modifier.weight(1f))
                DemoCard("卡片 B", "内容", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DemoCard("卡片 C", "内容", Modifier.weight(1f))
                DemoCard("卡片 D", "内容", Modifier.weight(1f))
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "观察要点",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "• 凹侧的模糊和黑光\n" +
                        "• 拉伸时凸侧是否锚定\n" +
                        "• 曲面模式的侧面是否露黑\n" +
                        "• 回正过程中是否平滑",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                lineHeight = 22.sp
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun DemoCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE0E0E0))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = Color(0xFF555555)
        )
    }
}
