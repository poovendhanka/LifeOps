package com.lifeops.core.database
import org.json.JSONObject
object ItemCodec {
 fun encode(v:ItemEntity):JSONObject = JSONObject().apply {
  put("id",v.id)
  put("kind",v.kind)
  put("title",v.title)
  put("body",v.body)
  put("status",v.status)
  put("priority",v.priority)
  put("nextAction",v.nextAction)
  put("due",v.due ?: JSONObject.NULL)
  put("reminderAt",v.reminderAt ?: JSONObject.NULL)
  put("recurrence",v.recurrence)
  put("projectId",v.projectId ?: JSONObject.NULL)
  put("contact",v.contact)
  put("category",v.category)
  put("lastAction",v.lastAction)
  put("waitingSince",v.waitingSince ?: JSONObject.NULL)
  put("startAt",v.startAt ?: JSONObject.NULL)
  put("progress",v.progress)
  put("amountMinor",v.amountMinor)
  put("payment",v.payment)
  put("uri",v.uri)
  put("tags",v.tags)
  put("pinned",v.pinned)
  put("createdAt",v.createdAt)
  put("updatedAt",v.updatedAt)
  put("deleted",v.deleted)
  put("sample",v.sample)
 }
 fun decode(o:JSONObject):ItemEntity {
 val d=ItemEntity()
 return ItemEntity(
  id=o.optString("id",d.id),
  kind=o.optString("kind",d.kind),
  title=o.optString("title",d.title),
  body=o.optString("body",d.body),
  status=o.optString("status",d.status),
  priority=o.optInt("priority",d.priority),
  nextAction=o.optString("nextAction",d.nextAction),
  due=if(o.isNull("due"))null else o.getLong("due"),
  reminderAt=if(o.isNull("reminderAt"))null else o.getLong("reminderAt"),
  recurrence=o.optString("recurrence",d.recurrence),
  projectId=if(o.isNull("projectId"))null else o.getString("projectId"),
  contact=o.optString("contact",d.contact),
  category=o.optString("category",d.category),
  lastAction=o.optString("lastAction",d.lastAction),
  waitingSince=if(o.isNull("waitingSince"))null else o.getLong("waitingSince"),
  startAt=if(o.isNull("startAt"))null else o.getLong("startAt"),
  progress=o.optInt("progress",d.progress),
  amountMinor=o.optLong("amountMinor",d.amountMinor),
  payment=o.optString("payment",d.payment),
  uri=o.optString("uri",d.uri),
  tags=o.optString("tags",d.tags),
  pinned=o.optBoolean("pinned",d.pinned),
  createdAt=o.optLong("createdAt",d.createdAt),
  updatedAt=o.optLong("updatedAt",d.updatedAt),
  deleted=o.optBoolean("deleted",d.deleted),
  sample=o.optBoolean("sample",d.sample),
 )
 }
}
