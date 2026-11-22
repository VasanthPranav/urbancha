package com.urbancha.inventory.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.urbancha.inventory.ui.theme.ThemeMode

/** Settings kept on this phone only (not synced): appearance and the Analytics controls. */
class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("urbancha_prefs", Context.MODE_PRIVATE)

    private var _theme by mutableStateOf(enumOr(sp.getString("theme", null), ThemeMode.Light))
    var theme: ThemeMode
        get() = _theme
        set(v) { _theme = v; sp.edit().putString("theme", v.name).apply() }

    private var _days by mutableStateOf(sp.getInt("analytics_days", 7))
    /** Analytics period: 1, 7, 30 or 90 days. */
    var analyticsDays: Int
        get() = _days
        set(v) { _days = v; sp.edit().putInt("analytics_days", v).apply() }

    private var _percent by mutableStateOf(sp.getBoolean("analytics_percent", false))
    /** Analytics "Show as": false = ₹ Amount, true = % Percent. */
    var analyticsPercent: Boolean
        get() = _percent
        set(v) { _percent = v; sp.edit().putBoolean("analytics_percent", v).apply() }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default
}

val LocalPrefs = androidx.compose.runtime.staticCompositionLocalOf<Prefs> { error("Prefs not provided") }
