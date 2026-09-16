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

private val Context.settingsDataStore by preferencesDataStore(name = "auto_reply_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val enabled = booleanPreferencesKey("enabled")
        val aiProvider = stringPreferencesKey("ai_provider")
        val systemPrompt = stringPreferencesKey("system_prompt")
        val whitelist = stringSetPreferencesKey("whitelist")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        val provider = preferences[Keys.aiProvider]?.let { value ->
            runCatching { AiProvider.valueOf(value) }.getOrDefault(AiProvider.CHATGPT)
        } ?: AiProvider.CHATGPT

        AppSettings(
            enabled = preferences[Keys.enabled] ?: false,
            aiProvider = provider,
            systemPrompt = preferences[Keys.systemPrompt] ?: AppSettings().systemPrompt,
            whitelist = normalizeWhitelist(preferences[Keys.whitelist] ?: emptySet()),
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.enabled] = enabled }
    }

    suspend fun setAiProvider(provider: AiProvider) {
        context.settingsDataStore.edit { it[Keys.aiProvider] = provider.name }
    }

    suspend fun addWhitelistSender(sender: String) {
        val normalized = normalize(sender)
        if (normalized.isEmpty()) return
        context.settingsDataStore.edit { preferences ->
            val current = preferences[Keys.whitelist] ?: emptySet()
            if (current.none { normalize(it) == normalized }) {
                preferences[Keys.whitelist] = current + normalized
            }
        }
    }

    suspend fun removeWhitelistSender(sender: String) {
        val normalized = normalize(sender)
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.whitelist] = (preferences[Keys.whitelist] ?: emptySet())
                .filterNot { normalize(it) == normalized }
                .toSet()
        }
    }

    private fun normalizeWhitelist(values: Set<String>): Set<String> =
        values.map(::normalize).filter(String::isNotEmpty).toSet()

    private fun normalize(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase()
}
