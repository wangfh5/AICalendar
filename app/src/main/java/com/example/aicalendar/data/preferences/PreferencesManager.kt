package com.example.aicalendar.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val apiKeyKey = stringPreferencesKey("api_key")
    private val baseUrlKey = stringPreferencesKey("base_url")
    private val modelNameKey = stringPreferencesKey("model_name")

    val apiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[apiKeyKey]
    }

    val baseUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[baseUrlKey] ?: "https://api.openai.com/v1"
    }

    val modelName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[modelNameKey] ?: "gpt-3.5-turbo"
    }

    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[apiKeyKey] = apiKey
        }
    }

    suspend fun saveBaseUrl(baseUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[baseUrlKey] = baseUrl
        }
    }

    suspend fun saveModelName(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[modelNameKey] = modelName
        }
    }
} 