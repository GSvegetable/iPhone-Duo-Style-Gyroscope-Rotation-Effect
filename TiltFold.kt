文件里有个辅助函数 remember0f()，是我为了把 remember { mutableStateOf(0f) } 抽短而加的。如果你觉得它反而干扰阅读，可以把 var Tiltx 通过remember0f()改成var Tiltx通过androidx.构成。运行时。记住{androidx。构成。运行时.mutableStateOf(0f)}，然后把记住0f删掉。


包com.tiltfold.demo

进口android.content.Context
进口android.graphics。运行时着色器
进口安卓。硬件.传感器
进口android.hardware.SensorEvent
进口android.hardware.SensorEventListener
进口android.hardware.SensorManager
进口androidx.compose.runtime.composable
进口androidx.compose.runtime.DisposableEffect
进口androidx.compose.runtime.getValue
进口androidx.compose.runtime.mutableStateOf
进口androidx.compose.runtime.setValue
进口androidx.compose.ui.platform.LocalContext
进口kotlin.math.abs
进口kotlin.math.hypot

//=============================================================================
//FoldParams--所有可调参数的集中点
//=============================================================================
/**
 * 设计意图：
 *   效果相关的魔法数字全部收在这里，其它文件只引用、不硬编码。
 *   读者想调效果时只需要打开这一个对象。
 *
 * 想做成"用户可调"：
 *1.把'const val'改成'var...by mutableStateOf(...)'
 *2.加SharedPreferences读写
 *3.用合成滑杆绑定
 *自述文件里有完整示例。
 */
对象FoldParams{

    /** 三种效果模式 */
    enum 班级 模式 {
        /**3D旋转+凹侧模糊+非线性拉伸*/
blur_3D，
        /** 曲面重投影 + 凹侧模糊（无拉伸，侧面天然不露黑） */
curve_reproject，
        /** 完全关闭 */
关
    }

    /**当前模式。默认3D，视觉冲击最强。*/
    var 模式 通过mutableStateOf(Mode.BLUR_3D)

    //-------------------------------------------------------------------------
    // 陀螺仪
    //-------------------------------------------------------------------------

    /**
     * 灵敏度除数。传感器原始值除以这个数映射到 -1~1。
     *
     * 越大越迟钝——需要倾斜更多角度才能达到满值。
     *10f→灵敏，轻微倾斜就触发
     *16f→中等，日常推荐
     *25f→迟钝，需要明显倾斜
     */
ConstVal tilt_FULL_SCALE=16.0f

    /**
     * 一阶低通滤波系数。
     *sm=sm*平滑+原始*(1-平滑)
     *
     * 越接近 1 越平滑但越迟钝。0.90 是手感与稳定性的平衡点。
     */
常量VAL倾斜平滑=0.90f

    /**
     * 防手抖死区。
     *
     * 倾斜绝对值小于这个值时，效果完全关闭。
     * 0f = 无死区；上限建议不超过 0.35f（否则需要倾得很厉害才有反应）。
     */
    var deadZone by mutableStateOf(0f)

    // -------------------------------------------------------------------------
    // 3D 旋转模式
    // -------------------------------------------------------------------------

    /** 最大旋转角度（度）。35~50 之间效果明显，超过 60 会显得夸张。 */
    const val MAX_TILT_DEGREES = 48f

    /**
     * 相机距离倍率。
     *
     * 乘上 density 后作为 cameraDistance。
     *   越小 → 透视越强，凹陷感越明显
     *   越大 → 接近正交投影
     *
     * 8f 是近距强透视，凹陷感最明显。
     */
    const val CAMERA_DISTANCE_FACTOR = 8f

    /**
     * 非线性拉伸强度上限。
     *   0   → 不拉伸（侧面会露黑）
     *   1.6 → 实测能覆盖侧边黑边的经验值
     */
    const val STRETCH_MAX = 1.6f

    /**
     * 拉伸曲线的指数。
     *   c = pow(absTilt, EXPONENT) * STRETCH_MAX
     *
     *   1.0 → 线性，小倾斜就开始拉
     *   1.5 → 加速度曲线，小倾斜几乎不拉，大倾斜爆发（推荐）
     *   2.0 → 更陡
     */
    const val STRETCH_EXPONENT = 1.5

    // -------------------------------------------------------------------------
    // 曲面重投影模式
    // -------------------------------------------------------------------------

    /**
     * 曲面折叠的最大角度系数。
     *   实际 θ_max = absTilt * CURVED_MAX_ANGLE * π
     *
     * 0.5 表示满倾斜时折叠 90°。
     */
    const val CURVED_MAX_ANGLE = 0.5f

    /** 曲面模式折叠强度上限。满倾斜时使用 θ_max 的百分之多少。 */
    const val CURVED_FOLD_STRENGTH = 0.7f

    // -------------------------------------------------------------------------
    // 模糊 + 黑光（两种模式共用）
    // -------------------------------------------------------------------------

    /** 模糊强度倍率。1.4f 是"倾斜时看得出，不倾斜时干净"的实测值。 */
    const val BLUR_INTENSITY = 1.4f

    /** 模糊从凹侧蔓延的覆盖系数。3.2 是经过多轮调试的推荐值。 */
    const val BLUR_EXTENT = 3.2f

    /** 最大模糊半径（像素）。 */
    const val BLUR_MAX_RADIUS = 26f

    /** 黑光覆盖系数，比模糊略大让"光"看起来是从边缘渗进来的。 */
    const val DARK_EXTENT = 3.0f

    /** 黑光强度上限。1.0 = 满倾斜时边缘纯黑。 */
    const val DARK_MAX = 1.0f

    fun setMode(m: Mode) { mode = m }
    fun setDeadZone(v: Float) { deadZone = v.coerceIn(0f, 0.35f) }
}

// =============================================================================
// TiltSensor —— 陀螺仪封装
// =============================================================================
/**
 * 倾斜读取 Composable。返回 Triple:
 *   first  = 水平倾斜 -1~1
 *   second = 垂直倾斜 -1~1（本 demo 未使用）
 *   third  = 合矢量模长（未使用）
 *
 * 【为什么用 TYPE_GRAVITY 而不是 TYPE_ACCELEROMETER】
 *   加速度计包含用户运动产生的加速度。走路、坐车、手抖时读数剧烈波动。
 *   TYPE_GRAVITY 是系统通过传感器融合算出的重力方向，稳定得多。
 *
 * 【为什么要记录基准 baseX / baseY】
 *   用户拿手机的姿势各不相同（有人竖着，有人斜着）。
 *   首次采样时记录当前姿态作为"中立位"，之后所有读数都是相对它的偏移。
 */
@Composable
fun rememberTilt(): Triple<Float, Float, Float> {
    val context = LocalContext.current
    var tiltX by remember0f()
    var tiltY by remember0f()

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)

        var smoothedX = 0f
        var smoothedY = 0f
        var baseX: Float? = null
        var baseY: Float? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val curX = event.values[0]
                val curY = event.values[1]

                // 第一帧作为基准
                if (baseX == null) {
                    baseX = curX
                    baseY = curY
                }

                val dx = curX - baseX!!
                val dy = curY - baseY!!

                // 归一化到 -1~1
                val rawX = (dx / FoldParams.TILT_FULL_SCALE).coerceIn(-1f, 1f)
                val rawY = (dy / FoldParams.TILT_FULL_SCALE).coerceIn(-1f, 1f)

                // 一阶低通滤波
                val k = FoldParams.TILT_SMOOTHING
                smoothedX = smoothedX * k + rawX * (1f - k)
                smoothedY = smoothedY * k + rawY * (1f - k)

                tiltX = smoothedX
                tiltY = smoothedY
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensorManager != null && gravitySensor != null) {
            // SENSOR_DELAY_GAME 约 20ms 一次，够用且省电
            sensorManager.registerListener(listener, gravitySensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose { sensorManager?.unregisterListener(listener) }
    }

    val mag = hypot(tiltX, tiltY).coerceIn(0f, 1f)
    return Triple(tiltX, tiltY, mag)
}

/** 小工具：Compose 里初始化为 0f 的状态 */
@Composable
private fun remember0f() = androidx.compose.runtime.remember { mutableStateOf(0f) }

/**
 * 应用防手抖死区。
 *
 * 逻辑：
 *   |raw| <= deadZone  →  0（完全归零）
 *   |raw| >  deadZone  →  从 0 线性增长到 1（映射到剩余区间）
 *
 * 效果：阈值以下完全不触发，超过后从 0 平滑增长，没有突变。
 */
fun applyDeadZone(raw: Float, deadZone: Float): Float {
    if (deadZone <= 0.0001f) return raw

    val absRaw = abs(raw)
    if (absRaw <= deadZone) return 0f

    val sign = if (raw > 0f) 1f else -1f
    val mapped = (absRaw - deadZone) / (1f - deadZone)
    return sign * mapped.coerceIn(0f, 1f)
}

// =============================================================================
// HomeBlurShader —— 凹侧可变模糊 + 黑色光晕
// =============================================================================
/**
 * 输入 uniform:
 *   content      —— 当前图层内容（RenderEffect 自动注入）
 *   size         —— 图层尺寸
 *   concaveSide  —— 凹侧方向。-1 = 左凹，+1 = 右凹，0 = 无倾斜
 *   strength     —— 倾斜强度 0~1
 *
 * 输出效果:
 *   1. 从凹侧边缘向内的模糊，覆盖面积随 strength 增大
 *   2. 从凹侧边缘向内的黑色光晕，覆盖面积略大于模糊
 *
 * 【采样核为什么是 5×5 = 25 次】
 *   AGSL 对单次 shader 里的 content.eval() 调用次数有硬性上限
 *   （视设备而定，通常 32~64 次）。超过上限时 shader 会静默失败——
 *   不报错，但效果完全不生效。
 *   5×5 = 25 次是安全余量。
 *   需要更大力度的模糊时，应该用多层离屏模糊叠加，而不是加大核。
 */
const val HOME_BLUR_AGSL = """
uniform shader content;
uniform float2 size;
uniform float concaveSide;
uniform float strength;

half4 main(float2 xy) {
    half4 original = content.eval(xy);

    // 无倾斜或方向未定，原样输出
    if (abs(concaveSide) < 0.001 || strength < 0.001) {
        return original;
    }

    float2 uv = xy / size;

    // d 的定义：0 = 凹侧边缘，1 = 清晰侧边缘
    // 无论凹侧在左还是在右，都映射到同一套逻辑
    float d = (concaveSide < 0.0) ? uv.x : (1.0 - uv.x);

    // ---- 模糊 ----
    // 覆盖范围曲线：非线性。小倾斜时覆盖窄，大倾斜时快速扩展
    float extent = clamp(pow(strength, 0.8) * 3.2, 0.0, 1.0);
    if (extent < 0.005) {
        return original;
    }

    // 权重：凹侧边缘 = 1，向前沿平滑衰减到 0
    float t = clamp(d / extent, 0.0, 1.0);
    float mask = 1.0 - t * t * (3.0 - 2.0 * t);   // smoothstep

    half4 blurred = original;
    if (mask >= 0.005) {
        float radius = 26.0 * mask;

        half4 sum = half4(0.0);
        float total = 0.0;

        // 5×5 高斯采样
        // 权重 w = exp(-(i²+j²)/8)，i 和 j 是相对中心的偏移
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                float2 offset = float2(float(i), float(j)) * (radius / 2.0);
                float w = exp(-(float(i * i + j * j)) / 2.0);

                // 关键：采样坐标必须 clamp 到图层范围
                // 不 clamp 会取到未定义数据，边缘会出现异常色
                sum += content.eval(clamp(xy + offset, float2(0.0), size)) * w;
                total += w;
            }
        }

        blurred = mix(original, sum / total, mask);
    }

    // ---- 黑色光晕 ----
    // 覆盖范围比模糊略大，让"光"看起来是从边缘渗进来的
    float darkExtent = clamp(pow(strength, 0.85) * 3.0, 0.0, 1.0);
    float darkT = clamp(d / max(darkExtent, 0.001), 0.0, 1.0);
    float darkMask = 1.0 - darkT * darkT * (3.0 - 2.0 * darkT);

    // 最大黑度：满倾斜时达到纯黑
    float maxDark = clamp(strength, 0.0, 1.0);
    float darkAmount = darkMask * maxDark;

    if (darkAmount > 0.001) {
        // 直接乘 RGB，效果是往黑色靠拢
        // 不用 mix 到纯黑是因为 mix 在 alpha 处理上更复杂，乘 RGB 已经够用
        half3 dimmed = blurred.rgb * half(1.0 - darkAmount);
        blurred = half4(dimmed, blurred.a);
    }

    return blurred;
}
""".trimIndent()

fun createHomeBlurShader(): RuntimeShader = RuntimeShader(HOME_BLUR_AGSL)

// =============================================================================
// CurvedFoldShader —— 曲面重投影
// =============================================================================
/**
 * 核心思想（一句话）：
 *   屏幕内容铺在圆弧上，从正面看，看到的是弦长而不是弧长。
 *   因为映射是 [0, W] → [0, W] 的双射，侧面天然不会露黑。
 *
 * 【与 3D 旋转的本质区别】
 *   3D 旋转是"平面上每个点被投影到新位置"，
 *   边缘会跑到屏幕外，所以必须靠拉伸补偿。
 *
 *   曲面重投影是"内容按曲面重新分布"，
 *   凸侧的内容被"挤"向凸侧边缘，凹侧的内容被"压"向中间，
 *   永远铺满整个宽度。
 *
 * 【数学推导】
 *   折叠坐标 p ∈ [0, 1]（0 = 凸侧边缘，1 = 凹侧边缘）
 *   对应圆弧角度 θ ∈ [0, θ_max]
 *   屏幕投影位置 X = R · sin(θ)
 *
 *   反解：
 *     sin(θ) = p · sin(θ_max)
 *     θ = asin(p · sin(θ_max))
 *     源纹理坐标 q = θ / θ_max
 *
 *   q 关于 p 是非线性的：
 *     p 从 0 增长到 1，但 q 增长得更慢（因为 sin 在 0~π/2 是凹函数）
 *     结果：凹侧的内容被压向中间
 */
const val CURVED_FOLD_AGSL = """
uniform shader content;
uniform float2 size;
uniform float concaveSide;
uniform float foldStrength;
uniform float blurStrength;

const float PI = 3.141592653589793;

/**
 * 输出位置 p → 源纹理坐标 q 的映射。
 *
 * 因为 sin 曲线在 0~π/2 是凹的，这个映射有个有趣的性质：
 *   p = 0   → q = 0
 *   p = 0.5 → q > 0.5   （超过一半的源内容被压在前半段屏幕里）
 *   p = 1   → q = 1
 *
 * 视觉上就是：凹侧那一半的屏幕里，塞进了超过一半的源内容。
 */
float pToQ(float p, float thetaMax) {
    if (thetaMax < 0.001) return p;
    float sinTM = sin(thetaMax);
    float sinTheta = clamp(p * sinTM, 0.0, 1.0);
    return asin(sinTheta) / thetaMax;
}

/** 按曲面映射采样内容。凹侧在左还是在右，通过 concaveSide 镜像。 */
half4 sampleCurved(float2 xy, float2 size, float concaveSide, float thetaMax) {
    float W = size.x;
    float xNorm = xy.x / W;

    // 归一化折叠坐标 p：0 = 凸侧，1 = 凹侧
    float p = (concaveSide > 0.0) ? xNorm : (1.0 - xNorm);

    float q = clamp(pToQ(p, thetaMax), 0.0, 1.0);

    float srcNorm = (concaveSide > 0.0) ? q : (1.0 - q);
    float srcX = srcNorm * W;

    return content.eval(clamp(float2(srcX, xy.y), float2(0.0), size));
}

half4 main(float2 xy) {
    float W = size.x;
    float xNorm = xy.x / W;

    float p = (concaveSide > 0.0) ? xNorm : (1.0 - xNorm);

    // 最大折叠角度：foldStrength = 1 时 = π/2 = 90°
    float thetaMax = foldStrength * PI * 0.5;

    half4 original = sampleCurved(xy, size, concaveSide, thetaMax);

    // ---- 曲面法线光照 ----
    // 曲面上某一点的法线方向由角度 θ 决定：
    //   n(θ) = (sin θ, cos θ)
    //   θ = 0 时法线正对观察者，θ 增大时法线逐渐偏斜
    //
    // 光源方向 L 固定为 (0, 1)（正对屏幕）
    // 漫反射 diffuse = max(0, dot(n, L)) = max(0, cos θ)
    //
    // 视觉结果：凸侧亮，凹侧暗
    if (thetaMax > 0.001) {
        float sinTM = sin(thetaMax);
        float sinTheta = clamp(p * sinTM, 0.0, 1.0);
        float theta = asin(sinTheta);
        float cosTheta = cos(theta);

        float2 n = float2(sin(theta), cos(theta));
        float2 L = float2(0.0, 1.0);

        float diffuse = max(0.0, dot(n, L));

        // 环境光 + 漫反射
        // 0.62 是环境光，保证凹侧不会完全黑掉
        float lit = 0.62 + diffuse * 0.38;

        // 深度衰减：z = R · (1 - cos θ)，归一化后按比例压暗
        float zRatio = (1.0 - cosTheta) / max(1.0 - cos(thetaMax), 0.001);
        float depthShade = 1.0 - zRatio * 0.30;

        float totalShade = clamp(lit * depthShade, 0.0, 1.2);
        original.rgb *= half(totalShade);
        original.rgb = clamp(original.rgb, half3(0.0), half3(1.0));
    }

    // ---- 模糊 ----
    if (blurStrength < 0.005 || thetaMax < 0.001) {
        return original;
    }

    // 权重：p² 而非 p，让模糊更集中在凹侧
    float mask = p * p * blurStrength;

    if (mask < 0.005) {
        return original;
    }

    float radius = 26.0 * mask;
    half3 sumRgb = half3(0.0);
    half sumA = 0.0;
    float total = 0.0;

    // 5×5 采样，每个采样点也要走同一映射
    for (int i = -2; i <= 2; i++) {
        for (int j = -2; j <= 2; j++) {
            float2 offset = float2(float(i), float(j)) * (radius / 2.0);
            float2 sxy = clamp(xy + offset, float2(0.0), size);

            half4 s = sampleCurved(sxy, size, concaveSide, thetaMax);

            float w = exp(-(float(i * i + j * j)) / 2.0);
            sumRgb += s.rgb * w;
            sumA += s.a * w;
            total += w;
        }
    }

    half3 blurRgb = sumRgb / total;
    half blurA = sumA / total;

    // ---- 饱和度提升 ----
    // 模糊后画面容易发灰发脏。做一次轻微的饱和度补偿。
    // 只作用在模糊区域，清晰侧不动。
    half lum = dot(blurRgb, half3(0.299, 0.587, 0.114));
    half satBoost = half(1.0 + 0.12 * mask);
    blurRgb = mix(half3(lum), blurRgb, satBoost);
    blurRgb = clamp(blurRgb, half3(0.0), half3(1.0));

    half4 blurred = half4(blurRgb, blurA);

    return mix(original, blurred, half(mask));
}
""".trimIndent()

fun createCurvedFoldShader(): RuntimeShader = RuntimeShader(CURVED_FOLD_AGSL)

// =============================================================================
// StretchShader —— 非线性水平拉伸
// =============================================================================
/**
 * 用途：
 *   3D 旋转后，屏幕在视觉上变窄，凹侧会露出底下的黑底。
 *   用这个 shader 把内容按"凸侧锚定，凹侧拉长"的方式展开，
 *   补偿旋转造成的宽度损失。
 *
 * 【与均匀拉伸的区别】
 *   均匀拉伸（scaleX）：整块内容按同一比例变宽，凸侧也被拉。
 *   本 shader：凸侧边缘完全不动，越靠凹侧拉伸越强。
 *
 * 【数学】
 *   输出位置 x' = x + c · x³ / W²
 *
 *   归一化：令 u = x/W, u' = x'/W
 *     u' = u + c · u³
 *     即  u³ + (1/c)·u - u'/c = 0
 *
 *   反解 u（给定屏幕坐标 u'，求源坐标 u）：
 *     用 Cardano 公式求三次方程的唯一实根。
 *
 * 【为什么选三次而不是二次】
 *   二次曲线的拉伸集中在凹侧，但过渡不够陡。
 *   三次曲线让"凸侧那一大段几乎不动"更彻底。
 */
const val STRETCH_AGSL = """
uniform shader content;
uniform float2 size;
uniform float stretchAmount;
uniform float concaveSide;

/** 立方根。AGSL 的 pow 不支持负数底数，需要自己处理符号。 */
float cbrt(float x) {
    return sign(x) * pow(abs(x), 1.0 / 3.0);
}

half4 main(float2 xy) {
    float W = size.x;
    float xp = xy.x;

    // 镜像到"锚点在左"的坐标系
    // 无论凹侧在左还是在右，都按同一套公式算，最后再镜像回来
    float xm = (concaveSide < 0.0) ? (W - xp) : xp;
    float uPrime = xm / W;

    float c = stretchAmount;
    float uIn;

    if (c > 0.0001) {
        // 解三次方程 u³ + p·u + q = 0
        // 这里 p = 1/c，q = -u'/c
        float p = 1.0 / c;
        float q = -uPrime / c;

        // Cardano 公式：u = ∛(-q/2 + √Δ) + ∛(-q/2 - √Δ)
        // 其中 Δ = (q/2)² + (p/3)³
        float halfQ = q * 0.5;
        float pOver3 = p / 3.0;
        float disc = halfQ * halfQ + pOver3 * pOver3 * pOver3;
        float sq = sqrt(max(disc, 0.0));

        uIn = cbrt(-halfQ + sq) + cbrt(-halfQ - sq);
    } else {
        // 不拉伸，恒等映射
        uIn = uPrime;
    }

    uIn = clamp(uIn, 0.0, 1.0);

    // 镜像回原始坐标
    float xOrig = (concaveSide < 0.0) ? ((1.0 - uIn) * W) : (uIn * W);

    float2 sampleCoord = float2(clamp(xOrig, 0.0, W), xy.y);
    return content.eval(sampleCoord);
}
""".trimIndent()

fun createStretchShader(): RuntimeShader = RuntimeShader(STRETCH_AGSL)
