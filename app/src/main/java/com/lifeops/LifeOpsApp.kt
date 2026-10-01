package com.lifeops
import android.app.Application
import com.lifeops.core.notifications.Reminders
import kotlinx.coroutines.*
class LifeOpsApp: Application() {
 override fun onCreate(){super.onCreate();Reminders.channel(this);CoroutineScope(SupervisorJob()+Dispatchers.IO).launch{Reminders.reconcile(this@LifeOpsApp)}}
}
