package com.lifeops.core.database
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.*
import java.util.zip.*

object Backup {
 const val MAX_BYTES=20*1024*1024
 suspend fun export(c:Context,uri:Uri)=withContext(Dispatchers.IO){
  val db=LifeDatabase.get(c);val root=db.withTransaction{JSONObject().put("format","LifeOps").put("version",1).put("exportedAt",System.currentTimeMillis()).put("items",JSONArray().apply{db.dao().all().forEach{put(ItemCodec.encode(it))}}).put("history",JSONArray().apply{db.dao().allHistory().forEach{put(JSONObject().put("id",it.id).put("itemId",it.itemId).put("text",it.text).put("at",it.at))}})}
  val bytes=root.toString(2).toByteArray(Charsets.UTF_8);require(bytes.size<=MAX_BYTES){"Backup exceeds the 20 MB data limit"}
  ZipOutputStream(requireNotNull(c.contentResolver.openOutputStream(uri,"wt"))).use{it.putNextEntry(ZipEntry("lifeops.json"));it.write(bytes);it.closeEntry()}
 }
 suspend fun import(c:Context,uri:Uri):Int=withContext(Dispatchers.IO){
  val text=ZipInputStream(requireNotNull(c.contentResolver.openInputStream(uri))).use{zip->val entry=zip.nextEntry;require(entry?.name=="lifeops.json"){"Not a LifeOps backup"};val bytes=zip.readNBytes(MAX_BYTES+1);require(bytes.size<=MAX_BYTES){"Backup exceeds 20 MB"};bytes.toString(Charsets.UTF_8)}
  val root=JSONObject(text);require(root.optString("format")=="LifeOps"&&root.getInt("version")==1){"Unsupported backup format"}
  val array=root.getJSONArray("items");require(array.length()<=50000){"Too many items"}
  val records=(0 until array.length()).map{ItemCodec.decode(array.getJSONObject(it))}
  require(records.map{it.id}.toSet().size==records.size){"Duplicate IDs in backup"}
  records.forEach{require(it.id.isNotBlank()&&it.id.length<=100&&it.kind in Kind.all&&it.priority in 0..3&&it.progress in 0..100&&it.amountMinor>=0&&it.updatedAt>0&&it.title.length<=10000&&it.body.length<=1000000){"Invalid record in backup"}}
  val hist=root.getJSONArray("history");require(hist.length()<=100000){"Too many history entries"}
  val events=(0 until hist.length()).map{val o=hist.getJSONObject(it);FollowUpHistoryEntity(o.getString("id"),o.getString("itemId"),o.getString("text"),o.getLong("at"))}
  val db=LifeDatabase.get(c);var count=0
  db.withTransaction{
   val current=db.dao().all().associateBy{it.id}
   records.forEach{incoming->val old=current[incoming.id];if(old==null||incoming.updatedAt>old.updatedAt){
    // File grants cannot travel with a backup. Only retain an already-granted local URI.
    val canRead=c.contentResolver.persistedUriPermissions.any{it.uri.toString()==incoming.uri&&it.isReadPermission}
    db.dao().put(incoming.copy(uri=if(canRead)incoming.uri else ""));count++
   }}
   val ids=db.dao().all().map{it.id}.toSet()
   require(events.all{it.itemId in ids&&it.text.length<=100000&&it.id.isNotBlank()}){"Invalid timeline link"}
   db.dao().events(events)
  }
  com.lifeops.core.notifications.Reminders.reconcile(c)
  count
 }
}
