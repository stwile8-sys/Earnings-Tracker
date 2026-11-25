package app.earnings.tracker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/** Показывает цифры суммы с пробелами между разрядами: 84500 → 84 500. */
private val ThousandsTransform = VisualTransformation { text ->
    val raw = text.text
    val len = raw.length
    val sb = StringBuilder()
    raw.forEachIndexed { i, ch ->
        if (i > 0 && (len - i) % 3 == 0) sb.append(' ')
        sb.append(ch)
    }
    val mapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            var c = 0
            for (j in 1 until offset) if ((len - j) % 3 == 0) c++
            return offset + c
        }

        override fun transformedToOriginal(offset: Int): Int {
            var pos = 0
            var orig = 0
            while (orig < len && pos < offset) {
                if (orig > 0 && (len - orig) % 3 == 0) {
                    pos++
                    if (pos >= offset) break
                }
                pos++
                orig++
            }
            return orig
        }
    }
    TransformedText(AnnotatedString(sb.toString()), mapping)
}

@Composable
private fun DateChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) C.text else C.surface, label = "chipBg")
    val fg by animateColorAsState(if (selected) C.bg else C.text, label = "chipFg")
    Box(
        Modifier.pressable(onClick = onClick).height(44.dp).clip(RoundedCornerShape(12.dp)).background(bg)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val todayMs = today().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMs
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("Готово", color = C.green) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = C.muted) } }
    ) { DatePicker(state = state) }
}

@Composable
fun AddScreen(store: Store, onBack: () -> Unit) {
    val entries = store.entries
    val focus = LocalFocusManager.current
    var dayEpoch by rememberSaveable { mutableStateOf(today().toEpochDay()) }
    var sumText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf("work") }
    var showPicker by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    val date = LocalDate.ofEpochDay(dayEpoch)
    val t = today()
    val amount = sumText.toLongOrNull() ?: 0L
    val ym = YearMonth.from(date)
    val before = entries.total(ym.atDay(1), ym.atEndOfMonth())
    val after = before + amount
    val delta = (entries.toList() + Entry(-1L, dayEpoch, amount, "")).delta(Period.MONTH, date)
    val custom = dayEpoch != t.toEpochDay() && dayEpoch != t.minusDays(1).toEpochDay() && dayEpoch != t.minusDays(2).toEpochDay()

    LaunchedEffect(saved) {
        if (saved) {
            store.add(date, amount, note, cat)
            delay(500)
            onBack()
        }
    }

    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val borderCol by animateColorAsState(if (focused || amount > 0) C.green else C.line, label = "border")
    val btnAlpha by animateFloatAsState(if (amount > 0 && !saved) 1f else 0.4f, label = "btn")
    val btnColor by animateColorAsState(if (saved) C.text else C.green, label = "btnColor")

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp)
        ) {
            Appear(0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.pressable(onClick = onBack).size(44.dp).clip(RoundedCornerShape(12.dp)).background(C.surface),
                        contentAlignment = Alignment.Center
                    ) { IconBack(C.text) }
                    Column {
                        Text("Новая запись", color = C.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text(monthTitle(ym), color = C.muted, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            Appear(1) {
                Column {
                    Label("Дата")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateChip("Сегодня", dayEpoch == t.toEpochDay()) { dayEpoch = t.toEpochDay() }
                        DateChip("Вчера", dayEpoch == t.minusDays(1).toEpochDay()) { dayEpoch = t.minusDays(1).toEpochDay() }
                        DateChip("Позавчера", dayEpoch == t.minusDays(2).toEpochDay()) { dayEpoch = t.minusDays(2).toEpochDay() }
                        val calBg by animateColorAsState(if (custom) C.text else C.surface, label = "calBg")
                        Box(
                            Modifier.pressable { focus.clearFocus(); showPicker = true }.size(44.dp)
                                .clip(RoundedCornerShape(12.dp)).background(calBg),
                            contentAlignment = Alignment.Center
                        ) { IconCalendar(if (custom) C.bg else C.text, 20.dp) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        weekdayFull(date).replaceFirstChar { it.uppercase() } + ", " + fullDate(date),
                        color = C.muted, fontSize = 14.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            Appear(2) {
                Column {
                    Label("Сумма")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(16.dp)).background(C.surface)
                            .border(if (focused) 2.dp else 1.dp, borderCol, RoundedCornerShape(16.dp))
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BasicTextField(
                            value = sumText,
                            onValueChange = { v -> sumText = v.filter { it.isDigit() }.take(9).trimStart('0') },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(color = C.text, fontSize = 44.sp, fontWeight = FontWeight.SemiBold, fontFamily = Mono),
                            cursorBrush = SolidColor(C.green),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = ThousandsTransform,
                            interactionSource = interaction,
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (sumText.isEmpty()) {
                                        Text("0", color = C.muted, fontSize = 44.sp, fontWeight = FontWeight.SemiBold, fontFamily = Mono)
                                    }
                                    inner()
                                }
                            }
                        )
                        Text("₽", color = C.muted, fontSize = 30.sp, fontFamily = Mono)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(500L, 1000L, 5000L, 10000L).forEach { add ->
                            Box(
                                Modifier.weight(1f).pressable {
                                    sumText = (amount + add).toString().take(9)
                                }.height(44.dp).clip(RoundedCornerShape(12.dp)).background(C.surface)
                                    .border(1.dp, C.line, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) { Text("+" + fmtNumber(add), color = C.text, fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            Appear(3) {
                Column {
                    Label("Категория")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CATS.forEach { c -> CatChip(c, cat == c.id) { cat = c.id } }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            Appear(4) {
                Column {
                    Label("Заметка · необязательно")
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(C.surface)
                            .border(1.dp, C.line, RoundedCornerShape(14.dp)).padding(horizontal = 18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = note, onValueChange = { note = it.take(80) }, singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = C.text, fontSize = 16.sp),
                            cursorBrush = SolidColor(C.green),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (note.isEmpty()) Text("Например, имя клиента или место", color = C.muted, fontSize = 16.sp)
                                    inner()
                                }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            Appear(5) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.surface).padding(16.dp)) {
                    SumLine("Было за " + monthName(ym.monthValue).lowercase(), money(before), C.text)
                    SumLine("Эта запись", "+" + money(amount), C.green)
                    Spacer(Modifier.height(10.dp))
                    Divider()
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Станет", color = C.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            CountUpMoney(after, 20.sp, durationMs = 450)
                            if (amount > 0) DeltaText(delta, Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }

        Box(
            Modifier.align(Alignment.BottomCenter).padding(20.dp).fillMaxWidth()
                .graphicsLayer { alpha = btnAlpha }
                .pressable(enabled = amount > 0 && !saved) { focus.clearFocus(); saved = true }
                .height(56.dp).clip(RoundedCornerShape(16.dp)).background(btnColor),
            contentAlignment = Alignment.Center
        ) {
            Text(if (saved) "Сохранено" else "Сохранить запись", color = C.onGreen, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (showPicker) {
        PickerDialog(date, onPick = { dayEpoch = it.toEpochDay() }, onDismiss = { showPicker = false })
    }
}

@Composable
private fun CatChip(c: Cat, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) C.text else C.surface, label = "catBg")
    val fg by animateColorAsState(if (selected) C.bg else C.text, label = "catFg")
    Row(
        Modifier.pressable(onClick = onClick).height(44.dp).clip(RoundedCornerShape(12.dp)).background(bg)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(c.color))
        Text(c.name, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SumLine(title: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = C.muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false)
    }
}
