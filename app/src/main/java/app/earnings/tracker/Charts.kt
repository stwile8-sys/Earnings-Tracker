package app.earnings.tracker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private fun niceCeil(v: Long): Long {
    if (v <= 0L) return 1L
    val mag = 10.0.pow(floor(log10(v.toDouble())))
    val n = v / mag
    val step = when {
        n <= 1.0 -> 1.0
        n <= 2.0 -> 2.0
        n <= 5.0 -> 5.0
        else -> 10.0
    }
    return ceil(step * mag).toLong()
}

private fun tagNum(v: Long): String {
    if (v < 1000) return v.toString()
    return ("%.1f".format(v / 1000.0)).replace('.', ',').removeSuffix(",0") + "к"
}

/**
 * Биржевой график накопленного заработка.
 * [cum] — накопленная сумма по дням, [totalDays] — сколько дней в периоде целиком (ось растянута на весь период),
 * [bars] — заработок по дням (столбики «объёма» внизу), [separators] — номера дней, где рисуем границу недели.
 */
@Composable
fun AnimatedChart(
    cum: List<Long>,
    totalDays: Int,
    xLabels: List<Pair<Int, String>>,
    modifier: Modifier = Modifier,
    bars: List<Long>? = null,
    separators: List<Int> = emptyList(),
    animKey: Any? = null,
) {
    val measurer = rememberTextMeasurer()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(animKey, cum) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
    }
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )
    val labelStyle = TextStyle(color = C.muted, fontSize = 11.sp, fontFamily = Mono)
    val tagStyle = TextStyle(color = C.onGreen, fontSize = 11.sp, fontFamily = Mono, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)

    Canvas(modifier) {
        if (cum.isEmpty()) return@Canvas
        val dp = 1.dp.toPx()
        val rightPad = 52 * dp
        val labelsH = 22 * dp
        val barsH = if (bars != null) 44 * dp else 0f
        val barsGap = if (bars != null) 10 * dp else 0f
        val plotW = size.width - rightPad
        val top = 10 * dp
        val lineBottom = size.height - labelsH - barsH - barsGap
        val niceMax = niceCeil(max(cum.last(), 1L))
        val n = cum.size
        val days = max(totalDays, n)

        fun xAt(i: Int) = if (days <= 1) 0f else plotW * i / (days - 1).toFloat()
        fun yAt(v: Long) = lineBottom - (v.toFloat() / niceMax) * (lineBottom - top)

        // сетка и подписи справа
        val dash = PathEffect.dashPathEffect(floatArrayOf(3 * dp, 4 * dp))
        listOf(0L, niceMax / 2, niceMax).forEachIndexed { k, v ->
            val y = yAt(v)
            drawLine(C.line, Offset(0f, y), Offset(plotW, y), strokeWidth = dp, pathEffect = if (k == 0) null else dash)
            val r = measurer.measure(shortNum(v), labelStyle)
            drawText(r, topLeft = Offset(plotW + 8 * dp, y - r.size.height / 2f))
        }

        // границы недель
        separators.forEach { idx ->
            val x = xAt(idx)
            drawLine(
                Color(0xFF2A323C), Offset(x, top), Offset(x, lineBottom + barsGap + barsH),
                strokeWidth = dp, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2 * dp, 4 * dp))
            )
        }

        // линия и заливка выезжают слева направо
        val xl = xAt(n - 1)
        val line = Path()
        cum.forEachIndexed { i, v -> if (i == 0) line.moveTo(xAt(i), yAt(v)) else line.lineTo(xAt(i), yAt(v)) }
        val fill = Path().apply {
            addPath(line)
            lineTo(xl, lineBottom); lineTo(xAt(0), lineBottom); close()
        }
        val clipRight = xl * progress.value + 1f
        clipRect(left = 0f, top = 0f, right = clipRight, bottom = size.height) {
            drawPath(fill, Brush.verticalGradient(listOf(C.green.copy(alpha = 0.22f), C.green.copy(alpha = 0f)), startY = top, endY = lineBottom))
            drawPath(line, C.green, style = Stroke(2 * dp, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // столбики по дням растут вслед за линией
        if (bars != null) {
            val maxBar = max(bars.maxOrNull() ?: 1L, 1L)
            val base = lineBottom + barsGap + barsH
            val bw = min(8 * dp, plotW / days * 0.6f).coerceAtLeast(2 * dp)
            drawLine(C.line, Offset(0f, base), Offset(plotW, base), strokeWidth = dp)
            bars.forEachIndexed { i, v ->
                if (v > 0) {
                    val p = (progress.value * (n + 3) - i).coerceIn(0f, 1f)
                    val h = v.toFloat() / maxBar * barsH * p
                    drawRoundRect(
                        C.green.copy(alpha = 0.55f), Offset(xAt(i) - bw / 2, base - h), Size(bw, h), CornerRadius(2 * dp)
                    )
                }
            }
        }

        // подписи по оси X
        xLabels.forEach { (idx, text) ->
            val r = measurer.measure(text, labelStyle)
            val x = (xAt(idx) - r.size.width / 2f).coerceIn(0f, plotW - r.size.width / 2f)
            drawText(r, topLeft = Offset(x, size.height - r.size.height - 2 * dp))
        }

        // точка на конце с пульсацией и меткой суммы
        val ma = ((progress.value - 0.88f) / 0.12f).coerceIn(0f, 1f)
        if (ma > 0f) {
            val c = Offset(xl, yAt(cum.last()))
            drawCircle(C.green.copy(alpha = 0.35f * (1f - pulse) * ma), (6 + 12 * pulse) * dp, c)
            drawCircle(C.green.copy(alpha = ma), 4 * dp * ma, c)
            val r = measurer.measure(tagNum(cum.last()), tagStyle)
            val w = r.size.width + 12 * dp
            val h = 18 * dp
            val tx = min(xl + 10 * dp, size.width - w)
            drawRoundRect(C.green.copy(alpha = ma), Offset(tx, c.y - h / 2), Size(w, h), CornerRadius(4 * dp))
            drawText(r, topLeft = Offset(tx + 6 * dp, c.y - r.size.height / 2f), alpha = ma)
        }
    }
}

/** Мини-график для строк списка: рисуется слева направо с задержкой. */
@Composable
fun Sparkline(values: List<Long>, color: Color, delayMs: Int = 0, modifier: Modifier = Modifier.size(64.dp, 28.dp)) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        kotlinx.coroutines.delay(delayMs.toLong())
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        val dp = 1.dp.toPx()
        val pad = 2 * dp
        val w = size.width
        val h = size.height - 2 * pad
        val path = Path()
        if (values.size < 2) {
            path.moveTo(0f, size.height / 2); path.lineTo(w, size.height / 2)
        } else {
            val lo = values.min().toFloat()
            val hi = values.max().toFloat()
            val span = if (hi - lo < 1f) 1f else hi - lo
            values.forEachIndexed { i, v ->
                val x = w * i / (values.size - 1)
                val y = pad + h - (v - lo) / span * h
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
        }
        clipRect(left = 0f, top = 0f, right = w * progress.value + 1f, bottom = size.height) {
            drawPath(path, color, style = Stroke(2 * dp, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
