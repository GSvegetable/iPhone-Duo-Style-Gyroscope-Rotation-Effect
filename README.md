```markdown
# TiltFold

> 用陀螺仪驱动 UI 折叠效果的最小可运行示例。
> 三种模式：3D 旋转、曲面重投影、凹侧模糊 + 黑光。
> 纯 AGSL 实现，无第三方依赖。

## 效果说明

三种模式，通过倾斜手机左右触发：

| 模式 | 视觉特征 |
|------|---------|
| `BLUR_3D` | 屏幕绕一侧边缘做 3D 旋转；凹侧模糊 + 黑光；内容非线性拉伸补偿侧面露黑 |
| `CURVED_REPROJECT` | 屏幕被"弯折"；凹侧压缩、凸侧拉宽；侧面永不露黑；元素始终正对观察者 |
| `OFF` | 关闭所有效果 |

## 目录结构

```

tiltfold/
├── app/src/main/java/com/tiltfold/demo/
│   ├── MainActivity.kt        入口 + 演示 UI + 模式调度
│   └── TiltFold.kt            参数 + 传感器 + 三个 AGSL shader
├── app/src/main/AndroidManifest.xml
├── app/build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md

```

## 核心原理速览

### 1. 陀螺仪读取

用 `Sensor.TYPE_GRAVITY` 而不是 `TYPE_ACCELEROMETER`：

- 加速度计包含用户运动产生的加速度，抖动大
- 重力传感器只反映重力方向，手上晃动不影响

首次采样时记录一个基准值 `baseX`，之后所有倾斜都是相对基准的偏移。这样用户以任何姿势拿起手机，都以"拿起那一刻"为中立位。

### 2. 平滑

一阶低通滤波：`sm = sm * k + raw * (1 - k)`，`k = 0.90`。

`k` 越大越平滑但越迟钝。0.90 是"手感跟手"和"不抖"之间的平衡点。

### 3. 防手抖死区

倾斜绝对值小于 `deadZone` 时直接归零。

作用：拿着手机打字、走路时，画面不会因为轻微晃动而产生可感知的形变。

### 4. 模糊 + 黑光（HomeBlurShader）

AGSL 单 pass，5×5 高斯采样（25 点）。

凹侧判定：`d = concaveSide < 0 ? uv.x : 1 - uv.x`，`d = 0` 是凹侧边缘。
模糊半径随 `d` 增长而下降，`d` 超过 `extent` 后不模糊。

黑光是同一套空间权重乘以一个暗化系数，直接乘在 RGB 上。

### 5. 曲面重投影（CurvedFoldShader）

**核心公式**：

```

屏幕内容铺在圆弧上，弧长 = 屏幕宽度
从正面看，看到的是弦长

凸侧固定，凹侧被压向凸侧
输出位置 p → 源位置 q 的映射：
θ = asin(p * sin(θ_max))
q = θ / θ_max

```

因为这是一个 `[0, W] → [0, W]` 的双射，**侧面天然不会露黑**。

对比 3D 旋转：3D 旋转是"平面上每个点投影后位置变了"，边缘会跑到屏幕外，所以要靠拉伸补偿。曲面重投影是"内容按曲面重新分布"，永远铺满。

### 6. 非线性拉伸（StretchShader）

3D 旋转时侧面会露出底下的黑。理论上要用拉伸把内容补回去，但不能均匀拉伸——凸侧不该被拉。

用三次曲线：

```

输出位置 x' = x + c · x³ / W²

```

- `x = 0`（凸侧）→ `x' = 0`，完全不动
- `x = W`（凹侧）→ `x' = W + c·W`
- 中间按三次曲线过渡

反解是三次方程，用 Cardano 公式求唯一实根。

---

## 踩坑记录

这一节是这篇 README 最重要的部分。按时间顺序记录踩过的坑。

### 坑 1：AGSL 里 `content.eval()` 采样次数有上限

第一次写模糊时，用了 9×9 共 81 次采样，结果**完全没效果**。

原因：AGSL 对单次 shader 里的 `content.eval()` 调用次数有硬性上限（视设备而定，通常 32~64 次）。超了不是报错，是**静默失败**——shader 直接不生效，画面原样输出。

解决：把有效采样数压到 5×5 = 25 次，用更大的半径补偿质量。

**结论**：写 AGSL 时，采样核控制在 5×5 以内。要大半径模糊就多 pass 叠加。

### 坑 2：`renderEffect` 需要 `CompositingStrategy.Offscreen`

`graphicsLayer { renderEffect = ... }` 单独用不生效。

原因：`renderEffect` 依赖图层被渲染到一个独立的离屏缓冲，然后在这个缓冲上应用效果。默认情况下 Compose 不为普通图层分配离屏缓冲。

解决：

```kotlin
graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
    renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
        .asComposeRenderEffect()
}
```

坑 3：rotationY 和 renderEffect 在同一图层不能共存

两者都作用于图层变换。系统只能应用其中一个，实际表现是 renderEffect 被忽略。

解决：拆成两层。

```kotlin
Box(Modifier.graphicsLayer { renderEffect = ... }) {   // 外层：模糊
    Box(Modifier.graphicsLayer { rotationY = ... }) {  // 内层：旋转
        内容
    }
}
```

坑 4：3D 旋转侧面会露黑

一开始用 rotationY 做倾斜，发现凹侧会露出底下的黑底。

原因：3D 旋转让平面在视觉上变窄，投影后覆盖不到原来的宽度。

解决：叠加一层拉伸。最初用均匀 scaleX，副作用是凸侧也被拉宽。

改进：改成三次曲线非线性拉伸，凸侧锚定，越靠凹侧拉得越多。

坑 5：非均匀拉伸的锚点必须是凸侧

拉伸曲线 x' = x + c·x³/W² 里，x = 0 处不动。所以如果凹侧在右边，x = 0 就是屏幕左边缘（凸侧）；凹侧在左边时，需要镜像坐标。

实现里用 concaveSide 判断并镜像，避免写两份代码。

坑 6：旋转轴跳变引起"震"

初版把旋转轴写成：

```kotlin
val originX = when {
    tiltX > 0.05f -> 0f
    tiltX < -0.05f -> 1f
    else -> 0.5f
}
```

回正过程中，tiltX 穿过 ±0.05 时轴从 0/1 突然跳到 0.5，画面会"震"一下。

解决：改成连续函数：

```kotlin
val originX = (0.5f - tiltX * 0.5f).coerceIn(0f, 1f)
```

· tiltX = 0 → originX = 0.5（轴在中间，不旋转）
· tiltX = +1 → originX = 0（轴在左边缘）
· tiltX = -1 → originX = 1（轴在右边缘）

全程连续，无断点。

坑 7：renderEffect 的挂载/卸载阈值

早期版本用 if (blurStrength > 0.01f) 决定是否挂载 shader。倾斜到阈值附近时，shader 反复挂载/卸载，画面闪烁。

解决：阈值压到极小（0.0001），让 shader 一直挂着，只靠 uniform 值控制效果强弱。

坑 8：content.eval() 的采样坐标要 clamp

模糊采样时坐标会超出图层范围。不 clamp 的话，边缘采样会取到未定义的数据，表现为边缘泛白或泛黑。

```glsl
float2 sc = clamp(xy + offset, float2(0.0), size);
```

---

参数调节

所有可调参数集中在 TiltFold.kt 顶部的 FoldParams 对象里。想做用户可调，把它改成 mutableStateOf + SharedPreferences 即可。

示例：把死区做成用户可调

```kotlin
object FoldParams {
    var deadZone by mutableStateOf(0f)
        private set

    fun setDeadZone(ctx: Context, v: Float) {
        deadZone = v.coerceIn(0f, 0.35f)
        ctx.getSharedPreferences("tiltfold", Context.MODE_PRIVATE)
            .edit().putFloat("dead_zone", deadZone).apply()
    }
}
```

然后用 Slider 绑定 deadZone 值即可。

---

集成到你的项目

1. 拷贝 TiltFold.kt
2. 在需要倾斜的顶层 Box 包一层结构（参考 MainActivity.kt 里的模式调度）
3. 保证 shader 图层有 compositingStrategy = CompositingStrategy.Offscreen

---

兼容性

· minSdk 24：AGSL 从 Android 13（API 33）开始支持，用 Build.VERSION.SDK_INT >= 33 判断
· 低版本降级：用 RenderEffect.createBlurEffect 做均匀模糊（已在代码里实现 fallback）

---

部署步骤

1. Android Studio 新建 Empty Compose Activity 项目，包名 com.tiltfold.demo
2. 用本仓库 6 个文件覆盖对应路径
3. Sync → Run

没有包含：

· gradle/wrapper/ 目录（Android Studio 新建项目自带）
· 图标资源（用默认的）
· strings.xml 里的 app_name（改成 "TiltFold" 即可）
