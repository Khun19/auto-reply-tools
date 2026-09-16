package com.autoreplytools.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.autoreplytools.core.model.AiProvider
import com.autoreplytools.core.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.settingsDataStore by preferencesDataStore(name = "auto_reply_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val enabled = booleanPreferencesKey("enabled")
        val aiProvider = stringPreferencesKey("ai_provider")
        val systemPrompt = stringPreferencesKey("system_prompt")
        val whitelist = stringSetPreferencesKey("whitelist")
        val recentViberSenders = stringPreferencesKey("recent_viber_senders")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        val provider = preferences[Keys.aiProvider]?.let { value ->
            runCatching { AiProvider.valueOf(value) }.getOrDefault(AiProvider.CHATGPT)
        } ?: AiProvider.CHATGPT

        AppSettings(
            enabled = preferences[Keys.enabled] ?: false,
            aiProvider = provider,
            systemPrompt = preferences[Keys.systemPrompt] ?: AppSettings().systemPrompt,
            whitelist = preferences[Keys.whitelist] ?: emptySet(),
        )
    }

    val recentViberSenders: Flow<List<String>> = context.settingsDataStore.data.map { preferences ->
        decodeRecentSenders(preferences[Keys.recentViberSenders])
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.enabled] = enabled }
    }

    suspend fun addWhitelistSender(sender: String) {
        val normalized = sender.trim()
        if (normalized.isEmpty()) return
        context.settingsDataStore.edit { preferences ->
            val current = preferences[Keys.whitelist] ?: emptySet()
            preferences[Keys.whitelist] = current + normalized
        }
    }

    suspend fun removeWhitelistSender(sender: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.whitelist] = (preferences[Keys.whitelist] ?: emptySet()) - sender
        }
    }

    suspend fun recordRecentViberSender(sender: String) {
        val normalized = sender.trim()
        if (normalized.isEmpty()) return
        context.settingsDataStore.edit { preferences ->
            val current = decodeRecentSenders(preferences[Keys.recentViberSenders])
            val updated = buildList {
                add(normalized)
                current.forEach { if (!it.equals(normalized, ignoreCase = true)) add(it) }
            }.take(MAX_RECENT_VIBER_SENDERS)
            preferences[Keys.recentViberSenders] = JSONArray(updated).toString()
        }
    }

    private fun decodeRecentSenders(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    array.optString(index).trim().takeIf { it.isNotEmpty() }?.let(::add)
                }
            }.take(MAX_RECENT_VIBER_SENDERS)
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val MAX_RECENT_VIBER_SENDERS = 20
    }
}
