package com.lifeops.core.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

object Kind {
    const val TASK="Task"; const val FOLLOW="Follow-up"; const val PROJECT="Project"
    const val NOTE="Note"; const val EXPENSE="Expense"; const val DOCUMENT="Document"
    val all=listOf(TASK,FOLLOW,NOTE,PROJECT,EXPENSE,DOCUMENT)
}
// Shared lifecycle fields live in one indexed table; type-specific values are optional.
// A single stable ID also allows search, tags, reminders and attachments to share relations.
@Entity(tableName="items", indices=[Index("kind"),Index("due"),Index("projectId"),Index("status")])
data class ItemEntity(
    @PrimaryKey val id:String=UUID.randomUUID().toString(),
    val kind:String=Kind.TASK,
    val title:String="",
    val body:String="",
    val status:String="Inbox",
    val priority:Int=1,
    val nextAction:String="",
    val due:Long?=null,
    val reminderAt:Long?=null,
    val recurrence:String="None",
    val projectId:String?=null,
    val contact:String="",
    val category:String="Other",
    val lastAction:String="",
    val waitingSince:Long?=null,
    val startAt:Long?=null,
    val progress:Int=0,
    val amountMinor:Long=0,
    val payment:String="UPI",
    val uri:String="",
    val tags:String="",
    val pinned:Boolean=false,
    val createdAt:Long=System.currentTimeMillis(),
    val updatedAt:Long=System.currentTimeMillis(),
    val deleted:Boolean=false,
    val sample:Boolean=false
) {
    val finished:Boolean get()=status in listOf("Completed","Cancelled","Resolved","Closed","Archived")
}
@Entity(tableName="history",foreignKeys=[ForeignKey(entity=ItemEntity::class,parentColumns=["id"],childColumns=["itemId"],onDelete=ForeignKey.CASCADE)],indices=[Index("itemId")])
data class FollowUpHistoryEntity(@PrimaryKey val id:String=UUID.randomUUID().toString(),val itemId:String,val text:String,val at:Long=System.currentTimeMillis())
@Entity(tableName="reminders",foreignKeys=[ForeignKey(entity=ItemEntity::class,parentColumns=["id"],childColumns=["itemId"],onDelete=ForeignKey.CASCADE)],indices=[Index("itemId")])
data class ReminderEntity(@PrimaryKey val itemId:String,val at:Long,val delivered:Boolean=false)

@Dao interface LifeDao {
    @Query("SELECT * FROM items WHERE deleted=0 ORDER BY pinned DESC,updatedAt DESC") fun observe():Flow<List<ItemEntity>>
    @Query("SELECT * FROM items WHERE deleted=1 ORDER BY updatedAt DESC") fun archived():Flow<List<ItemEntity>>
    @Query("SELECT * FROM items") suspend fun all():List<ItemEntity>
    @Query("SELECT * FROM items WHERE id=:id") suspend fun get(id:String):ItemEntity?
    @Upsert suspend fun put(item:ItemEntity)
    @Upsert suspend fun putAll(items:List<ItemEntity>)
    @Query("SELECT * FROM history WHERE itemId=:id ORDER BY at DESC") fun history(id:String):Flow<List<FollowUpHistoryEntity>>
    @Query("SELECT * FROM history ORDER BY at") suspend fun allHistory():List<FollowUpHistoryEntity>
    @Upsert suspend fun event(event:FollowUpHistoryEntity)
    @Upsert suspend fun events(events:List<FollowUpHistoryEntity>)
    @Query("UPDATE items SET deleted=1 WHERE sample=1") suspend fun removeSamples()
    @Upsert suspend fun reminder(reminder:ReminderEntity)
    @Query("DELETE FROM reminders WHERE itemId=:id") suspend fun removeReminder(id:String)
    @Query("SELECT * FROM reminders WHERE delivered=0") suspend fun pendingReminders():List<ReminderEntity>
    @Query("SELECT * FROM reminders WHERE itemId=:id") suspend fun reminderFor(id:String):ReminderEntity?
}
@Database(entities=[ItemEntity::class,FollowUpHistoryEntity::class,ReminderEntity::class],version=1,exportSchema=true)
abstract class LifeDatabase:RoomDatabase() {
 abstract fun dao():LifeDao
 companion object {
    @Volatile private var instance:LifeDatabase?=null
    fun get(context:Context):LifeDatabase=instance?:synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,LifeDatabase::class.java,"lifeops.db").build().also{instance=it}}
 }
}
