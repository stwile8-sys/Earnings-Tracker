package app.earnings.tracker

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Composable
fun TabsBar(selected: Period, onSelect: (Period) -> Unit) {
    val items = Period.values()
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(C.surface).padding(4.dp)
    ) {
        val tabW = maxWidth / items.size
        val x by animateDpAsState(
            targetValue = tabW * items.indexOf(selected),
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
            label = "tab"
        )
        Box(Modifier.offset(x = x).width(tabW).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(C.surface2))
        Row(Modifier.fillMaxSize()) {
            items.forEach { p ->
                val col by animateColorAsState(if (p == selected) C.text else C.muted, label = "tabText")
                Box(
                    Modifier.weight(1f).fillMaxHeight().pressable { onSelect(p) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(p.title, color = col, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun DeltaText(delta: Double?, modifier: Modifier = Modifier) {
    if (delta == null) {
        Text("—", color = C.muted, fontFamily = Mono, fontSize = 13.sp, modifier = modifier)
        return
    }
    val col = if (delta >= 0) C.green else C.red
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Triangle(delta >= 0, col)
        Text(percent(delta), color = col, fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private fun periodTitle(p: Period, span: Span): String = when (p) {
    Period.WEEK -> rangeLabel(span.start, span.end)
    Period.MONTH -> monthTitle(YearMonth.from(span.start))
    Period.YEAR -> span.start.year.toString()
}

private fun vsText(p: Period) = when (p) {
    Period.WEEK -> "к прошлой неделе"
    Period.MONTH -> "к прошлому месяцу"
    Period.YEAR -> "к прошлому году"
}

@Composable
fun OverviewScreen(store: Store, onOpenMonth: (YearMonth) -> Unit, onAdd: () -> Unit) {
    val entries = store.entries
    var period by rememberSaveable { mutableStateOf(Period.MONTH) }
    val t = today()
    val span = spanOf(period, t)
    val el = elapsed(span)
    val daysList = entries.daily(el.start, el.end)
    val cum = daysList.cumulative()
    val total = cum.lastOrNull() ?: 0L
    val totalDays = ChronoUnit.DAYS.between(span.start, span.end).toInt() + 1
    val delta = entries.delta(period, t)

    val labels: List<Pair<Int, String>> = when (period) {
        Period.WEEK -> (0 until 7).map { it to weekdayShort(span.start.plusDays(it.toLong())) }
        Period.MONTH -> listOf(0, 7, 14, 21, totalDays - 1).distinct().filter { it < totalDays }
            .map { it to (it + 1).toString() }
        Period.YEAR -> (0 until 12 step 2).map { m ->
            (LocalDate.of(span.start.year, m + 1, 1).dayOfYear - 1) to monthShort(m + 1)
        }
    }

    val weekSpan = elapsed(spanOf(Period.WEEK, t))
    val weekTotal = entries.total(weekSpan.start, weekSpan.end)
    val best = daysList.maxOrNull() ?: 0L
    val bestDate = if (best > 0) el.start.plusDays(daysList.indexOf(best).toLong()) else null
    val avg = if (daysList.isEmpty()) 0L else total / daysList.size

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 110.dp)
        ) {
            Appear(0) { TabsBar(period) { period = it } }
            Spacer(Modifier.height(24.dp))

            Appear(1) {
                Column {
                    Label("Итого · " + periodTitle(period, span))
                    Spacer(Modifier.height(8.dp))
                    CountUpMoney(total, 44.sp, tracking = (-1.3).sp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DeltaChip(delta, key = period)
                        Text(if (delta == null) "пока не с чем сравнить" else vsText(period), color = C.muted, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            Appear(2) {
                AnimatedChart(
                    cum = cum, totalDays = totalDays, xLabels = labels,
                    modifier = Modifier.fillMaxWidth().height(210.dp),
                    bars = if (period == Period.YEAR) null else daysList,
                    animKey = period
                )
            }
            Spacer(Modifier.height(16.dp))

            Appear(3, period) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (period == Period.WEEK) {
                        Tile("Среднее в день", money(avg), "за ${daysList.size} дн.", Modifier.weight(1f))
                    } else {
                        Tile("Эта неделя", money(weekTotal), rangeLabel(weekSpan.start, weekSpan.end), Modifier.weight(1f))
                    }
                    Tile("Лучший день", if (bestDate == null) "—" else money(best), bestDate?.let { dayMonth(it) } ?: "пока нет", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(24.dp))

            if (entries.isEmpty()) {
                Appear(4) { EmptyCard() }
            } else if (period == Period.WEEK) {
                Text("По дням", color = C.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                daysList.indices.reversed().forEachIndexed { k, i ->
                    val d = el.start.plusDays(i.toLong())
                    Appear(4 + k, period) { DayRow(d, daysList[i]) }
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("По месяцам", color = C.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Нажмите — откроются недели", color = C.muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                val months = entries.monthsList().filter { period == Period.MONTH || it.year == t.year }
                months.forEachIndexed { k, ym ->
                    Appear(4 + k, period) { MonthRow(ym, entries, k) { onOpenMonth(ym) } }
                }
            }
            Spacer(Modifier.height(24.dp))
            BackupLinks(store)
        }

        val s = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            delay(700)
            s.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        }
        Row(
            Modifier.align(Alignment.BottomEnd).padding(20.dp)
                .graphicsLayer { scaleX = s.value; scaleY = s.value; alpha = s.value.coerceIn(0f, 1f) }
                .pressable(onClick = onAdd)
                .height(56.dp).clip(RoundedCornerShape(16.dp)).background(C.green)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconPlus(C.onGreen)
            Text("Добавить", color = C.onGreen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Копия записей в файл и обратно. Формат общий с HTML-версией приложения. */
@Composable
private fun BackupLinks(store: Store) {
    val ctx = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = store.exportTo(ctx, uri)
            Toast.makeText(ctx, if (ok) "Копия сохранена" else "Не удалось сохранить копию", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val n = store.importFrom(ctx, uri)
            Toast.makeText(
                ctx, if (n < 0) "Не удалось прочитать файл копии" else "Добавлено записей: $n", Toast.LENGTH_LONG
            ).show()
        }
    }
    Column {
        Text(
            "Сохранить копию", color = C.muted, fontSize = 13.sp, textDecoration = TextDecoration.Underline,
            modifier = Modifier.pressable { exportLauncher.launch("zarabotok-" + today() + ".json") }.padding(vertical = 12.dp)
        )
        Text(
            "Загрузить копию", color = C.muted, fontSize = 13.sp, textDecoration = TextDecoration.Underline,
            modifier = Modifier.pressable { importLauncher.launch(arrayOf("*/*")) }.padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun EmptyCard() {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.surface).padding(20.dp)
    ) {
        Text("Пока нет записей", color = C.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Нажмите «Добавить», выберите дату и впишите, сколько заработали. График и месяцы появятся сами.",
            color = C.muted, fontSize = 14.sp, lineHeight = 20.sp
        )
    }
}

@Composable
private fun DayRow(d: LocalDate, sum: Long) {
    Column {
        Row(Modifier.fillMaxWidth().height(60.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(fullDate(d).substringBeforeLast(' '), color = C.text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(weekdayFull(d), color = C.muted, fontSize = 13.sp)
            }
            Text(
                if (sum > 0) "+" + money(sum) else "—",
                color = if (sum > 0) C.text else C.muted, fontFamily = Mono, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End
            )
        }
        Divider()
    }
}

@Composable
private fun MonthRow(ym: YearMonth, entries: List<Entry>, index: Int, onClick: () -> Unit) {
    val first = ym.atDay(1)
    val sp = elapsed(spanOf(Period.MONTH, first))
    val cum = entries.daily(sp.start, sp.end).cumulative()
    val sum = cum.lastOrNull() ?: 0L
    val delta = entries.delta(Period.MONTH, first)
    val current = ym == YearMonth.from(today())
    val col: Color = if (delta != null && delta < 0) C.red else if (sum > 0) C.green else C.muted
    Column {
        Row(
            Modifier.fillMaxWidth().height(72.dp).pressable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(monthName(ym.monthValue), color = C.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    rangeLabel(sp.start, sp.end) + if (current) " · идёт" else "",
                    color = C.muted, fontSize = 13.sp, maxLines = 1
                )
            }
            Sparkline(cum, col, delayMs = 300 + index * 120)
            Column(Modifier.width(100.dp), horizontalAlignment = Alignment.End) {
                Text(money(sum), color = C.text, fontFamily = Mono, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                DeltaText(delta, Modifier.padding(top = 2.dp))
            }
            IconChevron(C.muted)
        }
        Divider()
    }
}
