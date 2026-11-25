package app.earnings.tracker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

object C {
    val bg = Color(0xFF0B0D10)
    val surface = Color(0xFF12151A)
    val surface2 = Color(0xFF222932)
    val line = Color(0xFF1E232A)
    val text = Color(0xFFE6EAEE)
    val muted = Color(0xFF8D96A0)
    val green = Color(0xFF3DDC97)
    val red = Color(0xFFFF6B6B)
    val bar = Color(0xFF556070)
    val onGreen = Color(0xFF04210F)
}

val Mono = FontFamily.Monospace

/** Плавное появление снизу с задержкой: так элементы выезжают друг за другом. */
@Composable
fun Appear(index: Int, key: Any? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        delay(index * 60L)
        a.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    Box(modifier.graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 28.dp.toPx()
    }) { content() }
}

/** Нажатие с пружинкой: элемент слегка «проваливается» и возвращается. */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

/** Сумма, которая «набегает» до нужного значения; при смене плавно едет от старого. */
@Composable
fun CountUpMoney(
    target: Long,
    size: TextUnit,
    weight: FontWeight = FontWeight.SemiBold,
    color: Color = C.text,
    tracking: TextUnit = 0.sp,
    durationMs: Int = 1000,
) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(target) { a.animateTo(target.toFloat(), tween(durationMs, easing = FastOutSlowInEasing)) }
    Text(
        text = money(a.value.toLong()),
        fontFamily = Mono, fontSize = size, fontWeight = weight, color = color,
        letterSpacing = tracking, maxLines = 1, softWrap = false
    )
}

@Composable
fun Triangle(up: Boolean, color: Color) {
    Canvas(Modifier.size(9.dp)) {
        val p = Path()
        if (up) {
            p.moveTo(size.width / 2, 0f); p.lineTo(size.width, size.height); p.lineTo(0f, size.height)
        } else {
            p.moveTo(size.width / 2, size.height); p.lineTo(size.width, 0f); p.lineTo(0f, 0f)
        }
        p.close()
        drawPath(p, color)
    }
}

/** Плашка «+12,4%» с эффектом всплытия. */
@Composable
fun DeltaChip(delta: Double?, suffix: String = "", key: Any? = null) {
    if (delta == null) return
    val up = delta >= 0
    val col = if (up) C.green else C.red
    val s = remember(key) { Animatable(0.4f) }
    LaunchedEffect(key, delta) {
        delay(700)
        s.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
    }
    Row(
        Modifier
            .graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = ((s.value - 0.4f) / 0.6f).coerceIn(0f, 1f) }
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(col.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Triangle(up, col)
        Text(
            percent(delta) + suffix, color = col, fontFamily = Mono,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
        )
    }
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), modifier = modifier, color = C.muted, fontFamily = Mono,
        fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp
    )
}

@Composable
fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
}

/** Горизонтальная полоска, которая заполняется до доли [fraction]. */
@Composable
fun GrowBar(fraction: Float, color: Color, delayMs: Int = 0, modifier: Modifier = Modifier) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        delay(delayMs.toLong())
        a.animateTo(fraction.coerceIn(0f, 1f), tween(800, easing = FastOutSlowInEasing))
    }
    Box(modifier.height(5.dp).clip(RoundedCornerShape(3.dp)).background(C.line)) {
        Box(
            Modifier
                .fillMaxWidth(a.value)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
    }
}

@Composable
fun Tile(title: String, value: String, note: String, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(C.surface).padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        androidx.compose.foundation.layout.Column {
            Text(title, color = C.muted, fontSize = 12.sp)
            Text(
                value, color = C.text, fontFamily = Mono, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp), maxLines = 1, softWrap = false
            )
            Text(note, color = C.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
        }
    }
}
