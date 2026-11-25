package app.earnings.tracker

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class Period(val title: String) { WEEK("Неделя"), MONTH("Месяц"), YEAR("Год") }

data class Span(val start: LocalDate, val end: LocalDate)

fun today(): LocalDate = LocalDate.now()

fun spanOf(p: Period, anchor: LocalDate): Span = when (p) {
    Period.WEEK -> Span(
        anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
        anchor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    )
    Period.MONTH -> Span(anchor.withDayOfMonth(1), anchor.withDayOfMonth(anchor.lengthOfMonth()))
    Period.YEAR -> Span(anchor.withDayOfYear(1), anchor.withDayOfYear(anchor.lengthOfYear()))
}

fun shiftBack(p: Period, anchor: LocalDate): LocalDate = when (p) {
    Period.WEEK -> anchor.minusWeeks(1)
    Period.MONTH -> anchor.withDayOfMonth(1).minusMonths(1)
    Period.YEAR -> anchor.withDayOfYear(1).minusYears(1)
}

fun List<Entry>.total(a: LocalDate, b: LocalDate): Long {
    val x = a.toEpochDay()
    val y = b.toEpochDay()
    return filter { it.day in x..y }.sumOf { it.amount }
}

/** Заработок по каждому дню от a до b включительно. */
fun List<Entry>.daily(a: LocalDate, b: LocalDate): List<Long> {
    val days = ChronoUnit.DAYS.between(a, b).toInt() + 1
    if (days <= 0) return emptyList()
    val arr = LongArray(days)
    val x = a.toEpochDay()
    forEach { if (it.day in x..(x + days - 1)) arr[(it.day - x).toInt()] += it.amount }
    return arr.toList()
}

fun List<Long>.cumulative(): List<Long> {
    var s = 0L
    return map { s += it; s }
}

/** Период, обрезанный по сегодняшний день (будущее не считаем). */
fun elapsed(span: Span): Span {
    val t = today()
    val end = if (t.isBefore(span.end)) t else span.end
    return Span(span.start, if (end.isBefore(span.start)) span.start else end)
}

/** Изменение к прошлому периоду за то же число прошедших дней. Null, если сравнивать не с чем. */
fun List<Entry>.delta(p: Period, anchor: LocalDate): Double? {
    val cur = elapsed(spanOf(p, anchor))
    val curSum = total(cur.start, cur.end)
    val prevAnchor = shiftBack(p, anchor)
    val prevFull = spanOf(p, prevAnchor)
    val len = ChronoUnit.DAYS.between(cur.start, cur.end)
    val prevEndRaw = prevFull.start.plusDays(len)
    val prevEnd = if (prevEndRaw.isAfter(prevFull.end)) prevFull.end else prevEndRaw
    val prevSum = total(prevFull.start, prevEnd)
    if (prevSum <= 0L) return null
    return (curSum - prevSum) * 100.0 / prevSum
}

data class WeekPart(val start: LocalDate, val end: LocalDate, val sum: Long, val isCurrent: Boolean)

/** Недели месяца (пн–вс), обрезанные по границам месяца. */
fun List<Entry>.weeksOf(ym: YearMonth): List<WeekPart> {
    val first = ym.atDay(1)
    val last = ym.atEndOfMonth()
    val t = today()
    val res = mutableListOf<WeekPart>()
    var s = first
    while (!s.isAfter(last)) {
        val weekEnd = s.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val e = if (weekEnd.isAfter(last)) last else weekEnd
        if (!s.isAfter(t)) res.add(WeekPart(s, e, total(s, e), !t.isBefore(s) && !t.isAfter(e)))
        s = e.plusDays(1)
    }
    return res
}

/** Месяцы, в которых есть записи, плюс текущий; новые сверху. */
fun List<Entry>.monthsList(): List<YearMonth> {
    val set = map { YearMonth.from(it.date) }.toMutableSet()
    set.add(YearMonth.from(today()))
    return set.sortedDescending()
}
