package com.lifeops.core.database
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
val Context.settingsStore by preferencesDataStore("settings")
data class Settings(val notifications:Boolean=true,val reminderHour:Int=9,val mondayFirst:Boolean=true)
class Preferences(private val context:Context) {
 private val notifications=booleanPreferencesKey("notifications")
 private val hour=intPreferencesKey("reminderHour")
 private val monday=booleanPreferencesKey("mondayFirst")
 val flow=context.settingsStore.data.map { Settings(it[notifications]?:true,it[hour]?:9,it[monday]?:true) }
 suspend fun save(s:Settings){context.settingsStore.edit{it[notifications]=s.notifications;it[hour]=s.reminderHour;it[monday]=s.mondayFirst}}
}
