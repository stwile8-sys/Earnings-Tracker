package app.earnings.tracker

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Запись о доходе. cat: work — заработок, tips — чаевые, other — прочее. */
data class Entry(val id: Long, val day: Long, val amount: Long, val note: String, val cat: String = "work") {
    val date: LocalDate get() = LocalDate.ofEpochDay(day)
}

private fun Entry.toJson(): JSONObject =
    JSONObject().put("id", id).put("day", day).put("amount", amount).put("note", note).put("cat", cat)

private fun JSONObject.toEntry(id: Long = getLong("id")): Entry {
    val c = optString("cat", "work")
    return Entry(id, getLong("day"), getLong("amount"), optString("note", ""), if (CATS.any { it.id == c }) c else "work")
}

/** Хранилище записей: JSON в SharedPreferences, список обновляет экран сам. */
class Store(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("earnings", Context.MODE_PRIVATE)

    val entries = mutableStateListOf<Entry>()

    init {
        val raw = prefs.getString("data", "[]") ?: "[]"
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) entries.add(arr.getJSONObject(i).toEntry())
    }

    private fun toJson(): String {
        val arr = JSONArray()
        entries.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    private fun save() {
        prefs.edit().putString("data", toJson()).apply()
    }

    /** Копия записей в файл. Тот же формат, что у HTML-версии. */
    fun exportTo(context: Context, uri: Uri): Boolean = try {
        context.contentResolver.openOutputStream(uri)?.use { it.write(toJson().toByteArray(Charsets.UTF_8)) }
        true
    } catch (e: Exception) {
        false
    }

    /** Добавляет записи из файла копии (уже существующие пропускает). Возвращает, сколько добавлено, или -1 при ошибке. */
    fun importFrom(context: Context, uri: Uri): Int = try {
        val text = context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
            ?: throw IllegalStateException()
        val arr = JSONArray(text)
        val have = entries.map { it.id }.toMutableSet()
        var added = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (have.add(o.getLong("id"))) {
                entries.add(o.toEntry())
                added++
            }
        }
        save()
        added
    } catch (e: Exception) {
        -1
    }

    fun add(date: LocalDate, amount: Long, note: String, cat: String) {
        entries.add(Entry(System.currentTimeMillis(), date.toEpochDay(), amount, note.trim(), cat))
        save()
    }

    fun remove(id: Long) {
        entries.removeAll { it.id == id }
        save()
    }
}
