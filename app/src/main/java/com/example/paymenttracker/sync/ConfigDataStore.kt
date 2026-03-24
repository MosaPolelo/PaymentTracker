package com.example.paymenttracker.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "config_store")

object ConfigDataStore {
    private val WEB_URL = stringPreferencesKey("web_url")
    private val API_KEY = stringPreferencesKey("api_key")

    suspend fun save(context: Context, url: String, key: String) {
        context.dataStore.edit { prefs ->
            prefs[WEB_URL] = url
            prefs[API_KEY] = key
        }
    }

    suspend fun getUrl(context: Context): String? {
        return context.dataStore.data.first()[WEB_URL]
    }

    suspend fun getKey(context: Context): String? {
        return context.dataStore.data.first()[API_KEY]
    }
}