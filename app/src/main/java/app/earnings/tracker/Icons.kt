package app.earnings.tracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun IconPlus(color: Color, sz: Dp = 22.dp) {
    Canvas(Modifier.size(sz)) {
        val s = size.width
        val w = 2.2.dp.toPx()
        drawLine(color, Offset(s / 2, s * 0.2f), Offset(s / 2, s * 0.8f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s / 2), Offset(s * 0.8f, s / 2), w, StrokeCap.Round)
    }
}

@Composable
fun IconBack(color: Color, sz: Dp = 22.dp) {
    Canvas(Modifier.size(sz)) {
        val s = size.width
        val p = Path().apply {
            moveTo(s * 0.62f, s * 0.22f); lineTo(s * 0.32f, s * 0.5f); lineTo(s * 0.62f, s * 0.78f)
        }
        drawPath(p, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Шеврон «›»; rotation = 90 превращает в «⌄». */
@Composable
fun IconChevron(color: Color, rotation: Float = 0f, sz: Dp = 18.dp) {
    Canvas(Modifier.size(sz).graphicsLayer { rotationZ = rotation }) {
        val s = size.width
        val p = Path().apply {
            moveTo(s * 0.38f, s * 0.22f); lineTo(s * 0.66f, s * 0.5f); lineTo(s * 0.38f, s * 0.78f)
        }
        drawPath(p, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun IconClose(color: Color, sz: Dp = 18.dp) {
    Canvas(Modifier.size(sz)) {
        val s = size.width
        val w = 2.dp.toPx()
        drawLine(color, Offset(s * 0.25f, s * 0.25f), Offset(s * 0.75f, s * 0.75f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.75f, s * 0.25f), Offset(s * 0.25f, s * 0.75f), w, StrokeCap.Round)
    }
}

@Composable
fun IconCalendar(color: Color, sz: Dp = 22.dp) {
    Canvas(Modifier.size(sz)) {
        val s = size.width
        val w = 2.dp.toPx()
        drawRoundRect(
            color, Offset(s * 0.14f, s * 0.2f), Size(s * 0.72f, s * 0.68f),
            CornerRadius(s * 0.14f), style = Stroke(w)
        )
        drawLine(color, Offset(s * 0.14f, s * 0.42f), Offset(s * 0.86f, s * 0.42f), w)
        drawLine(color, Offset(s * 0.34f, s * 0.1f), Offset(s * 0.34f, s * 0.28f), w, StrokeCap.Round)
        drawLine(color, Offset(s * 0.66f, s * 0.1f), Offset(s * 0.66f, s * 0.28f), w, StrokeCap.Round)
    }
}
