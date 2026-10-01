package com.lifeops.core.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import androidx.work.*
import com.lifeops.MainActivity
import com.lifeops.R
import com.lifeops.core.database.*
import com.lifeops.core.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

object Reminders {
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 fun channel(c:Context) {c.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("next-actions","Next actions",NotificationManager.IMPORTANCE_DEFAULT).apply{description="Task and follow-up reminders"})}
 fun sync(c:Context,item:ItemEntity) {scope.launch {schedule(c,item);LifeOpsWidget.update(c)}}
 suspend fun schedule(c:Context,item:ItemEntity) {
  val dao=LifeDatabase.get(c).dao(); val wm=WorkManager.getInstance(c)
  val at=item.reminderAt
  if(at==null||item.deleted||item.finished||!Preferences(c).flow.first().notifications) {wm.cancelUniqueWork("reminder-${item.id}");dao.removeReminder(item.id);NotificationManagerCompat.from(c).cancel(item.id.hashCode());return}
  val prior=dao.reminderFor(item.id)
  if(prior?.at==at&&prior.delivered)return
  dao.reminder(ReminderEntity(item.id,at))
  val request=OneTimeWorkRequestBuilder<ReminderWorker>().setInputData(workDataOf("id" to item.id,"at" to at)).setInitialDelay((at-System.currentTimeMillis()).coerceAtLeast(0),TimeUnit.MILLISECONDS).addTag("lifeops-reminders").build()
  wm.enqueueUniqueWork("reminder-${item.id}",ExistingWorkPolicy.REPLACE,request)
 }
 suspend fun reconcile(c:Context) { LifeDatabase.get(c).dao().all().forEach {schedule(c,it)} }
}
class ReminderWorker(c:Context,p:WorkerParameters):CoroutineWorker(c,p) {
 override suspend fun doWork():Result {
  val dao=LifeDatabase.get(applicationContext).dao();val id=inputData.getString("id")?:return Result.success()
  val item=dao.get(id)?:return Result.success(); val at=inputData.getLong("at",0)
  if(item.deleted||item.finished||item.reminderAt!=at||!Preferences(applicationContext).flow.first().notifications)return Result.success()
  if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(applicationContext,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return Result.success()
  Reminders.channel(applicationContext)
  val open=PendingIntent.getActivity(applicationContext,id.hashCode(),Intent(applicationContext,MainActivity::class.java).putExtra("itemId",id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  fun action(type:String):PendingIntent=PendingIntent.getBroadcast(applicationContext,(id+type).hashCode(),Intent(applicationContext,ReminderReceiver::class.java).setAction(type).putExtra("id",id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val body=listOfNotNull(item.waitingSince?.let{waitingLabel(it)},item.nextAction.ifBlank{item.body}.takeIf{it.isNotBlank()}).joinToString(". ").ifBlank{"Your next action is ready."}
  val notification=NotificationCompat.Builder(applicationContext,"next-actions").setSmallIcon(R.drawable.ic_notification).setContentTitle(item.title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(open).setAutoCancel(true).addAction(0,"Mark done",action("done")).addAction(0,"Snooze 1h",action("snooze")).build()
  try {NotificationManagerCompat.from(applicationContext).notify(id.hashCode(),notification);dao.reminder(ReminderEntity(id,at,true))}catch(_:SecurityException){}
  return Result.success()
 }
}
class ReminderReceiver:BroadcastReceiver() {
 override fun onReceive(c:Context,intent:Intent) {
  val id=intent.getStringExtra("id")?:return
  WorkManager.getInstance(c).enqueueUniqueWork("action-$id",ExistingWorkPolicy.APPEND_OR_REPLACE,OneTimeWorkRequestBuilder<ReminderActionWorker>().setInputData(workDataOf("id" to id,"action" to intent.action)).build())
 }
}
class ReminderActionWorker(c:Context,p:WorkerParameters):CoroutineWorker(c,p) {
 override suspend fun doWork():Result {
  val db=LifeDatabase.get(applicationContext);val dao=db.dao();val id=inputData.getString("id")?:return Result.success()
  var changed:ItemEntity?=null;var recurring:ItemEntity?=null
  db.withTransaction {
   val item=dao.get(id)?:return@withTransaction
   if(item.deleted||item.finished)return@withTransaction
   changed=if(inputData.getString("action")=="snooze")item.copy(reminderAt=System.currentTimeMillis()+3600000) else item.copy(status=if(item.kind==Kind.FOLLOW)"Resolved" else "Completed")
   dao.put(changed!!.copy(updatedAt=System.currentTimeMillis()))
   if(inputData.getString("action")=="done"&&item.kind==Kind.TASK&&item.recurrence!="None") {
    val next=nextRecurrence(item.due?:System.currentTimeMillis(),item.recurrence)
    recurring=item.copy(id=java.util.UUID.randomUUID().toString(),status="Planned",due=next,reminderAt=item.reminderAt?.let{next-((item.due?:it)-it)},createdAt=System.currentTimeMillis(),updatedAt=System.currentTimeMillis());dao.put(recurring!!)
   }
  }
  NotificationManagerCompat.from(applicationContext).cancel(id.hashCode())
  changed?.let{Reminders.schedule(applicationContext,it)};recurring?.let{Reminders.schedule(applicationContext,it)}
  return Result.success()
 }
}
