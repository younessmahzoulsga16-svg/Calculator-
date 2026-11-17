package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.engine.AngleUnit
import com.example.engine.NumberDisplayMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.calcDataStore: DataStore<Preferences> by preferencesDataStore(name = "novacalc_prefs")

data class CalculationRecord(
    val id: Long,
    val expression: String,
    val result: String,
    val alternateResult: String?,
    val angleUnit: AngleUnit,
    val timestampMs: Long
)

data class PersistedCalcPrefs(
    val angleUnit: AngleUnit = AngleUnit.DEG,
    val displayMode: NumberDisplayMode = NumberDisplayMode.AUTO,
    val history: List<CalculationRecord> = emptyList()
)

class CalcPreferencesRepository(private val context: Context) {

    private object Keys {
        val ANGLE_UNIT = stringPreferencesKey("angle_unit")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val HISTORY_JSON = stringPreferencesKey("history_json")
    }

    val preferencesFlow: Flow<PersistedCalcPrefs> = context.calcDataStore.data.map { prefs ->
        val angle = prefs[Keys.ANGLE_UNIT]?.let { name ->
            AngleUnit.entries.firstOrNull { it.name == name }
        } ?: AngleUnit.DEG

        val mode = prefs[Keys.DISPLAY_MODE]?.let { name ->
            NumberDisplayMode.entries.firstOrNull { it.name == name }
        } ?: NumberDisplayMode.AUTO

        val history = decodeHistory(prefs[Keys.HISTORY_JSON])
        PersistedCalcPrefs(angleUnit = angle, displayMode = mode, history = history)
    }

    suspend fun saveAngleUnit(unit: AngleUnit) {
        context.calcDataStore.edit { it[Keys.ANGLE_UNIT] = unit.name }
    }

    suspend fun saveDisplayMode(mode: NumberDisplayMode) {
        context.calcDataStore.edit { it[Keys.DISPLAY_MODE] = mode.name }
    }

    suspend fun saveHistory(records: List<CalculationRecord>) {
        val capped = records.take(60)
        context.calcDataStore.edit { it[Keys.HISTORY_JSON] = encodeHistory(capped) }
    }

    private fun encodeHistory(records: List<CalculationRecord>): String {
        val arr = JSONArray()
        for (rec in records) {
            val obj = JSONObject().apply {
                put("id", rec.id)
                put("expr", rec.expression)
                put("res", rec.result)
                put("alt", rec.alternateResult ?: "")
                put("ang", rec.angleUnit.name)
                put("ts", rec.timestampMs)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeHistory(raw: String?): List<CalculationRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = ArrayList<CalculationRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val angle = AngleUnit.entries.firstOrNull { it.name == obj.optString("ang") } ?: AngleUnit.DEG
                val alt = obj.optString("alt").takeIf { it.isNotEmpty() }
                list.add(
                    CalculationRecord(
                        id = obj.optLong("id", i.toLong()),
                        expression = obj.optString("expr"),
                        result = obj.optString("res"),
                        alternateResult = alt,
                        angleUnit = angle,
                        timestampMs = obj.optLong("ts", 0L)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
