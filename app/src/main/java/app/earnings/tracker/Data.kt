package app.earnings.tracker

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class Entry(val id: Long, val day: Long, val amount: Long, val note: String) {
    val date: LocalDate get() = LocalDate.ofEpochDay(day)
}

/** Хранилище записей: JSON в SharedPreferences, список обновляет экран сам. */
class Store(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("earnings", Context.MODE_PRIVATE)

    val entries = mutableStateListOf<Entry>()

    init {
        val raw = prefs.getString("data", "[]") ?: "[]"
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            entries.add(Entry(o.getLong("id"), o.getLong("day"), o.getLong("amount"), o.optString("note", "")))
        }
    }

    private fun save() {
        val arr = JSONArray()
        entries.forEach {
            arr.put(JSONObject().put("id", it.id).put("day", it.day).put("amount", it.amount).put("note", it.note))
        }
        prefs.edit().putString("data", arr.toString()).apply()
    }

    fun add(date: LocalDate, amount: Long, note: String) {
        entries.add(Entry(System.currentTimeMillis(), date.toEpochDay(), amount, note.trim()))
        save()
    }

    fun remove(id: Long) {
        entries.removeAll { it.id == id }
        save()
    }
}
