package com.calmcorner.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One finished exercise. Kept only on this phone. */
data class SessionRecord(
    val time: Long,
    val tool: Tool,
    val before: Zone?,
    val after: Zone?,
)

/** Simple on-device history stored as a small JSON file (last 500 sessions). */
class HistoryStore(context: Context) {
    private val file = File(context.filesDir, "history.json")
    val records = mutableStateListOf<SessionRecord>().apply { addAll(load()) }

    fun add(record: SessionRecord) {
        records.add(record)
        while (records.size > 500) records.removeAt(0)
        save()
    }

    /** Fill in the "after" colour for the most recent session. */
    fun setAfter(zone: Zone?) {
        val last = records.lastOrNull() ?: return
        records[records.lastIndex] = last.copy(after = zone)
        save()
    }

    fun clear() {
        records.clear()
        save()
    }

    private fun load(): List<SessionRecord> = try {
        if (!file.exists()) emptyList() else {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val tool = Tool.entries.firstOrNull { it.name == o.optString("tool") } ?: return@mapNotNull null
                SessionRecord(
                    time = o.getLong("time"),
                    tool = tool,
                    before = Zone.entries.firstOrNull { it.name == o.optString("before") },
                    after = Zone.entries.firstOrNull { it.name == o.optString("after") },
                )
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun save() {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(JSONObject().apply {
                put("time", r.time)
                put("tool", r.tool.name)
                put("before", r.before?.name ?: "")
                put("after", r.after?.name ?: "")
            })
        }
        file.writeText(arr.toString())
    }
}

val LocalHistory = staticCompositionLocalOf<HistoryStore> { error("HistoryStore not provided") }
