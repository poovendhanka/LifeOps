package com.lifeops.feature

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.lifeops.core.database.*
import com.lifeops.core.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*

class LifeViewModel(app:Application):AndroidViewModel(app) {
 val db=LifeDatabase.get(app);val dao=db.dao();val prefs=Preferences(app)
 val items=dao.observe().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val settings=prefs.flow.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),Settings())
 val messages=MutableSharedFlow<String>(extraBufferCapacity=4)
 fun message(text:String){messages.tryEmit(text)}
 fun save(item:ItemEntity,after:()->Unit={}) {viewModelScope.launch {
  try { val old=dao.get(item.id);val time=System.currentTimeMillis()
   val record=item.copy(updatedAt=time,waitingSince=if(item.status=="Waiting") item.waitingSince?:time else null)
   db.withTransaction {
    dao.put(record)
    if(old!=null&&old.status!=record.status)dao.event(FollowUpHistoryEntity(itemId=record.id,text="Status changed to ${record.status}"))
   }
   syncReminder(record)
   after()
  }catch(_:Exception){message("Could not save. Please try again.")}
 }}
 fun syncReminder(item:ItemEntity) { com.lifeops.core.notifications.Reminders.sync(getApplication(),item) }
 fun archive(item:ItemEntity){save(item.copy(deleted=true))}
 fun restore(item:ItemEntity){save(item.copy(deleted=false))}
 fun complete(item:ItemEntity){viewModelScope.launch {
  val latest=dao.get(item.id)?:return@launch
  if(latest.finished)return@launch
  val done=latest.copy(status=if(latest.kind==Kind.FOLLOW)"Resolved" else "Completed",updatedAt=System.currentTimeMillis())
  db.withTransaction {
   dao.put(done)
   dao.event(FollowUpHistoryEntity(itemId=latest.id,text="${done.status}: ${latest.title}"))
   if(latest.kind==Kind.TASK&&latest.recurrence!="None") {
    val next=nextRecurrence(latest.due?:System.currentTimeMillis(),latest.recurrence)
    val copy=latest.copy(id=java.util.UUID.randomUUID().toString(),status="Planned",due=next,reminderAt=latest.reminderAt?.let{next-((latest.due?:it)-it)},createdAt=System.currentTimeMillis(),updatedAt=System.currentTimeMillis())
    dao.put(copy);syncReminder(copy)
   }
  }
  syncReminder(done)
 }}
 fun addEvent(item:ItemEntity,text:String,at:Long=System.currentTimeMillis()) {if(text.isBlank())return;viewModelScope.launch {db.withTransaction {dao.event(FollowUpHistoryEntity(itemId=item.id,text=text.trim(),at=at));dao.get(item.id)?.let{dao.put(it.copy(lastAction=text.trim(),updatedAt=System.currentTimeMillis()))}}}}
 fun updateSettings(value:Settings){viewModelScope.launch {prefs.save(value);dao.all().forEach(::syncReminder)}}
 fun seed(){viewModelScope.launch {
  if(dao.all().any{it.sample&&!it.deleted}){message("Sample items are already loaded");return@launch}
  val now=System.currentTimeMillis(); val today=LocalDate.now().atTime(18,0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
  val project=ItemEntity(kind=Kind.PROJECT,title="LifeOps Android App",status="Active",nextAction="Review the dashboard experience",progress=35,sample=true,due=now+7*86400000)
  val course=ItemEntity(kind=Kind.PROJECT,title="AWS Learning",status="Active",nextAction="Explore networking services",progress=40,sample=true)
  dao.putAll(listOf(project,course,
   ItemEntity(title="Finish project documentation",status="In Progress",priority=2,nextAction="Write the installation guide",due=today,projectId=project.id,sample=true),
   ItemEntity(title="Complete AWS module",status="Planned",due=today,projectId=course.id,sample=true),
   ItemEntity(title="Review monthly expenses",status="Planned",due=now+86400000,sample=true),
   ItemEntity(kind=Kind.FOLLOW,title="Laptop service",contact="Service centre",category="Service",status="Waiting",nextAction="Call for the repair update",lastAction="Service request submitted",waitingSince=now-4*86400000,due=today,sample=true),
   ItemEntity(kind=Kind.FOLLOW,title="Refund request",contact="Customer care",category="Refund",status="Waiting",nextAction="Ask for the refund reference",waitingSince=now-6*86400000,due=now+86400000,sample=true),
   ItemEntity(kind=Kind.NOTE,title="A little space to think",body="Capture an idea before it disappears. Pin it, connect it to a project, or turn it into a next action.",pinned=true,sample=true),
   ItemEntity(kind=Kind.EXPENSE,title="Lunch",category="Food",amountMinor=24000,due=now,sample=true)))
  dao.event(FollowUpHistoryEntity(itemId=dao.all().first{it.title=="Laptop service"&&it.sample&&!it.deleted}.id,text="Service request submitted",at=now-4*86400000))
  message("Sample workspace loaded")
 }}
 fun clearSamples(){viewModelScope.launch {dao.all().filter{it.sample}.forEach{syncReminder(it.copy(deleted=true))};dao.removeSamples();message("Sample items removed")}}
}
