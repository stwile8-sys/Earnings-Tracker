package app.earnings.tracker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/** Разбивка дохода по категориям за период: цветная полоса, которая выезжает слева направо, и легенда. */
@Composable
fun SplitCard(entries: List<Entry>, start: LocalDate, end: LocalDate) {
    val a = start.toEpochDay()
    val b = end.toEpochDay()
    val sums = CATS.map { c -> entries.filter { catOf(it.cat).id == c.id && it.day in a..b }.sumOf { it.amount } }
    val total = sums.sum()
    val p = remember { Animatable(0f) }
    LaunchedEffect(sums) {
        p.snapTo(0f)
        p.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    if (total <= 0L) return

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.surface).padding(16.dp)) {
        Label("По категориям")
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(C.line)
                .drawWithContent {
                    clipRect(right = size.width * p.value) { this@drawWithContent.drawContent() }
                },
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            sums.forEachIndexed { i, s ->
                if (s > 0) Box(Modifier.weight(s.toFloat()).fillMaxHeight().background(CATS[i].color))
            }
        }
        Spacer(Modifier.height(8.dp))
        sums.forEachIndexed { i, s ->
            if (s > 0) {
                Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(CATS[i].color))
                    Text(CATS[i].name, color = C.text, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 10.dp))
                    Text(money(s), color = C.text, fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
                    Text(
                        Math.round(s * 100.0 / total).toString() + "%", color = C.muted, fontFamily = Mono, fontSize = 13.sp,
                        textAlign = TextAlign.End, modifier = Modifier.width(46.dp)
                    )
                }
            }
        }
    }
}
