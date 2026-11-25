package app.earnings.tracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.YearMonth

@Composable
fun MonthScreen(store: Store, ym: YearMonth, onBack: () -> Unit) {
    val entries = store.entries
    val first = ym.atDay(1)
    val sp = elapsed(Span(first, ym.atEndOfMonth()))
    val daysList = entries.daily(sp.start, sp.end)
    val cum = daysList.cumulative()
    val total = cum.lastOrNull() ?: 0L
    val delta = entries.delta(Period.MONTH, first)
    val weeks = entries.weeksOf(ym)
    val separators = weeks.drop(1).map { it.start.dayOfMonth - 1 }
    val labels = weeks.map {
        val a = it.start.dayOfMonth
        val b = it.end.dayOfMonth
        ((a - 1 + b - 1) / 2) to (if (a == b) "$a" else "$a–$b")
    }
    val maxSum = weeks.maxOfOrNull { it.sum } ?: 0L
    val bestSum = weeks.filter { !it.isCurrent }.maxOfOrNull { it.sum } ?: 0L
    val best = daysList.maxOrNull() ?: 0L
    val bestDate = if (best > 0) sp.start.plusDays(daysList.indexOf(best).toLong()) else null
    val avg = if (daysList.isEmpty()) 0L else total / daysList.size
    var openWeek by rememberSaveable { mutableStateOf(-1L) }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp)
    ) {
        Appear(0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.pressable(onClick = onBack).size(44.dp).clip(RoundedCornerShape(12.dp)).background(C.surface),
                    contentAlignment = Alignment.Center
                ) { IconBack(C.text) }
                Column {
                    Text(monthTitle(ym), color = C.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("По неделям", color = C.muted, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))

        Appear(1) {
            Column {
                Label("Итого за месяц")
                Spacer(Modifier.height(8.dp))
                CountUpMoney(total, 44.sp, tracking = (-1.3).sp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DeltaChip(delta)
                    Text(if (delta == null) "пока не с чем сравнить" else "к прошлому месяцу", color = C.muted, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Appear(2) {
            AnimatedChart(
                cum = cum, totalDays = ym.lengthOfMonth(), xLabels = labels,
                modifier = Modifier.fillMaxWidth().height(230.dp),
                bars = daysList, separators = separators, animKey = ym
            )
        }
        Spacer(Modifier.height(20.dp))

        Appear(3) {
            Column {
                Row(Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Label("Неделя", Modifier.weight(1f))
                    Label("Сумма", Modifier.width(110.dp))
                    Label("К пред.", Modifier.width(78.dp))
                }
                Divider()
            }
        }

        weeks.indices.reversed().forEachIndexed { k, i ->
            val w = weeks[i]
            val prev = if (i > 0) weeks[i - 1].sum else 0L
            val wDelta = if (i > 0 && !w.isCurrent && prev > 0) (w.sum - prev) * 100.0 / prev else null
            val isBest = !w.isCurrent && w.sum > 0 && w.sum == bestSum
            val key = w.start.toEpochDay()
            Appear(4 + k) {
                WeekRow(
                    w = w, delta = wDelta, isBest = isBest, fraction = if (maxSum > 0) w.sum.toFloat() / maxSum else 0f,
                    expanded = openWeek == key, index = k,
                    entries = entries.filter { it.day in w.start.toEpochDay()..w.end.toEpochDay() }
                        .sortedWith(compareByDescending<Entry> { it.day }.thenByDescending { it.id }),
                    onToggle = { openWeek = if (openWeek == key) -1L else key },
                    onDelete = { store.remove(it) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Appear(4 + weeks.size) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile("Среднее в день", money(avg), "за ${daysList.size} дн.", Modifier.weight(1f))
                Tile("Лучший день", if (bestDate == null) "—" else money(best), bestDate?.let { dayMonth(it) } ?: "пока нет", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WeekRow(
    w: WeekPart, delta: Double?, isBest: Boolean, fraction: Float, expanded: Boolean, index: Int,
    entries: List<Entry>, onToggle: () -> Unit, onDelete: (Long) -> Unit,
) {
    val rot by animateFloatAsState(
        if (expanded) 90f else 0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "chev"
    )
    val days = java.time.temporal.ChronoUnit.DAYS.between(w.start, w.end).toInt() + 1
    Column {
        Row(
            Modifier.fillMaxWidth().pressable(onClick = onToggle).height(58.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(rangeLabel(w.start, w.end), color = C.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (w.isCurrent) "идёт · $days дн." else if (isBest) "лучшая неделя" else "$days дн.",
                    color = if (isBest) C.green else C.muted, fontSize = 12.sp
                )
            }
            Text(
                money(w.sum), color = C.text, fontFamily = Mono, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(110.dp), maxLines = 1, softWrap = false
            )
            DeltaText(delta, Modifier.width(60.dp))
            IconChevron(C.muted, rotation = rot)
        }
        GrowBar(fraction, if (isBest) C.green else C.bar, delayMs = 500 + index * 140, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                if (entries.isEmpty()) {
                    Text("На этой неделе записей нет", color = C.muted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                entries.forEach { e ->
                    EntryRow(e) { onDelete(e.id) }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        Divider()
    }
}

@Composable
private fun EntryRow(e: Entry, onDelete: () -> Unit) {
    var gone by remember { mutableStateOf(false) }
    LaunchedEffect(gone) {
        if (gone) {
            delay(320)
            onDelete()
        }
    }
    AnimatedVisibility(visible = !gone, exit = shrinkVertically() + fadeOut()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(C.surface)
                .padding(start = 14.dp).height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(dayMonth(e.date) + " · " + weekdayShort(e.date), color = C.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (e.note.isNotEmpty()) Text(e.note, color = C.muted, fontSize = 12.sp, maxLines = 1)
            }
            Text("+" + money(e.amount), color = C.text, fontFamily = Mono, fontSize = 14.sp, textAlign = TextAlign.End, maxLines = 1, softWrap = false)
            Box(
                Modifier.pressable { gone = true }.size(48.dp),
                contentAlignment = Alignment.Center
            ) { IconClose(C.muted) }
        }
    }
}
