package app.earnings.tracker

import java.time.LocalDate
import java.time.YearMonth

private val monthsNom = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)
private val monthsShort = listOf(
    "янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сент", "окт", "нояб", "дек"
)
private val monthsGen = listOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря"
)
private val weekdaysFull = listOf("понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье")
private val weekdaysShort = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

private const val NB = " "

fun monthName(m: Int) = monthsNom[m - 1]
fun monthShort(m: Int) = monthsShort[m - 1]
fun weekdayShort(d: LocalDate) = weekdaysShort[d.dayOfWeek.value - 1]
fun weekdayFull(d: LocalDate) = weekdaysFull[d.dayOfWeek.value - 1]

fun fmtNumber(v: Long): String {
    val s = v.toString().reversed().chunked(3).joinToString(NB).reversed()
    return s
}

fun money(v: Long) = fmtNumber(v) + NB + "₽"

/** Короткая подпись для оси: 50к, 1,5к, 800. */
fun shortNum(v: Long): String {
    if (v < 1000) return v.toString()
    val k = v / 1000.0
    val t = if (k >= 10 || k == Math.floor(k)) k.toLong().toString() else "%.1f".format(k).replace('.', ',')
    return t + "к"
}

fun percent(p: Double): String {
    val sign = if (p >= 0) "+" else "−"
    return sign + "%.1f".format(Math.abs(p)).replace('.', ',') + "%"
}

fun fullDate(d: LocalDate) = "${d.dayOfMonth} ${monthsGen[d.monthValue - 1]} ${d.year}"
fun dayMonth(d: LocalDate) = "${d.dayOfMonth} ${monthsShort[d.monthValue - 1]}"
fun monthTitle(ym: YearMonth) = "${monthName(ym.monthValue)} ${ym.year}"

fun rangeLabel(a: LocalDate, b: LocalDate): String =
    if (a == b) dayMonth(a)
    else if (a.month == b.month) "${a.dayOfMonth}–${b.dayOfMonth} ${monthsShort[a.monthValue - 1]}"
    else "${dayMonth(a)} – ${dayMonth(b)}"
