@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.lifeops.feature

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeops.core.database.*
import com.lifeops.core.design.*
import com.lifeops.core.util.*

@Composable fun DetailScreen(id:String,all:List<ItemEntity>,vm:LifeViewModel,back:()->Unit,edit:(ItemEntity)->Unit,complete:(ItemEntity)->Unit,archive:(ItemEntity)->Unit,open:(ItemEntity)->Unit,create:(Pair<String,String>)->Unit) {
 val item=all.find{it.id==id}
 if(item==null){Column(Modifier.padding(22.dp)){Text("This item is no longer in your workspace.");TextButton(onClick=back){Text("Go back")}};return}
 val history by remember(id){vm.dao.history(id)}.collectAsStateWithLifecycle(emptyList())
 var confirm by remember{mutableStateOf(false)};var changeStatus by remember{mutableStateOf(false)};var event by rememberSaveable(id){mutableStateOf("")};var eventAt by rememberSaveable(id){mutableStateOf<Long?>(System.currentTimeMillis())}
 val context=LocalContext.current
 val linked=all.filter{it.projectId==id};val tasks=linked.filter{it.kind==Kind.TASK}
 val progress=if(tasks.isNotEmpty())100*tasks.count{it.status=="Completed"}/tasks.size else item.progress
 LazyColumn(contentPadding=PaddingValues(22.dp,8.dp,22.dp,40.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){IconButton(onClick=back){Icon(Icons.Rounded.ArrowBack,"Back")};Row {IconButton(onClick={vm.save(item.copy(pinned=!item.pinned))}){Icon(if(item.pinned)Icons.Rounded.PushPin else Icons.Rounded.PushPin,if(item.pinned)"Unpin" else "Pin",tint=if(item.pinned)Silver else Muted)};IconButton(onClick={edit(item)}){Icon(Icons.Rounded.Edit,"Edit item")};IconButton(onClick={confirm=true}){Icon(Icons.Rounded.Archive,"Archive item")}}}}
  item{Eyebrow(item.kind);Text(item.title.ifBlank{"Untitled note"},fontSize=30.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=10.dp));Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){AssistChip(onClick={changeStatus=true},label={Text(item.status)},trailingIcon={Icon(Icons.Rounded.ExpandMore,null,Modifier.size(16.dp))});Text(listOf("Low","Normal","High","Critical")[item.priority]+" priority",fontSize=12.sp,color=Muted)}}
  if(item.kind in listOf(Kind.TASK,Kind.FOLLOW,Kind.PROJECT))item{LifeOpsCard(onClick={edit(item)}){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("NEXT ACTION");Icon(Icons.Rounded.NorthEast,null,Modifier.size(18.dp),tint=Muted)};Text(item.nextAction.ifBlank{"Give this a clear next step."},fontSize=22.sp);if(item.due!=null)Text(dateLabel(item.due,true),color=Muted,fontSize=13.sp)}}
  if(item.kind==Kind.FOLLOW)item{LifeOpsCard{Eyebrow(item.category);item.waitingSince?.let{Text(waitingLabel(it),fontSize=23.sp)};if(item.contact.isNotBlank())Text(item.contact,color=Silver);if(item.lastAction.isNotBlank()){HorizontalDivider(color=Edge);Eyebrow("LAST ACTION");Text(item.lastAction,color=Muted)}}}
  if(item.kind==Kind.PROJECT)item{LifeOpsCard{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("PROGRESS");Text("$progress%")};LinearProgressIndicator(progress={progress/100f},modifier=Modifier.fillMaxWidth(),color=Silver,trackColor=Edge);Text("${tasks.count{it.status=="Completed"}} completed · ${tasks.size} tasks",color=Muted,fontSize=13.sp);item.startAt?.let{Text("Started ${dateLabel(it)}",color=Muted,fontSize=12.sp)}}}
  if(item.kind==Kind.EXPENSE)item{LifeOpsCard {Eyebrow(item.category);Text(money(item.amountMinor),fontSize=36.sp,fontWeight=FontWeight.Light);Text("${item.payment} · ${dateLabel(item.due)}",color=Muted)}}
  if(item.body.isNotBlank())item{Section(if(item.kind==Kind.NOTE)"Note" else "Details");LifeOpsCard {androidx.compose.foundation.text.selection.SelectionContainer{Text(item.body,fontSize=16.sp,lineHeight=25.sp)}}}
  if(item.kind==Kind.DOCUMENT)item{LiquidButton("Open document",{try{val uri=Uri.parse(item.uri);context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,context.contentResolver.getType(uri)?:"application/octet-stream").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}catch(_:Exception){vm.message("File unavailable. Edit this record to select it again, or install a compatible viewer.")}},Modifier.fillMaxWidth())}
  if(item.kind==Kind.NOTE)item{OutlinedButton(onClick={vm.save(initialItem(Kind.TASK).copy(title=item.title.ifBlank{item.body.take(80)},body=item.body,projectId=item.projectId,tags=item.tags)){vm.message("Task created. Your note is preserved.")}},modifier=Modifier.fillMaxWidth()){Icon(Icons.Rounded.CheckCircleOutline,null);Spacer(Modifier.width(8.dp));Text("Create task from note")}}
  if(item.kind==Kind.PROJECT){item{Section("Project tasks","Add task"){create(Kind.TASK to id)}};if(tasks.isEmpty())item{Text("No tasks linked yet.",color=Muted)};items(tasks,key={it.id}){ItemRow(it,all,open,complete)};item{Section("Notes & references","Add note"){create(Kind.NOTE to id)}}}
  if(item.kind in listOf(Kind.PROJECT,Kind.FOLLOW)){items(linked.filter{it.kind!=Kind.TASK},key={it.id}){ItemRow(it,all,open,complete)};item{OutlinedButton(onClick={create(Kind.DOCUMENT to id)},modifier=Modifier.fillMaxWidth()){Icon(Icons.Rounded.AttachFile,null);Text("Attach document")}}}
  item.projectId?.let {pid->all.find{it.id==pid}?.let{parent->item{LifeOpsCard(onClick={open(parent)}){Eyebrow("LINKED TO");Text(parent.title)}}}}
  if(item.kind in listOf(Kind.FOLLOW,Kind.PROJECT)) {
   item{Section("Activity")}
   item{LifeOpsCard{EditText("Add an update",event,{event=it},2);DateControl("Event date",eventAt,{eventAt=it});LiquidButton("Add to timeline",{if(eventAt!=null&&eventAt!!<=System.currentTimeMillis()){vm.addEvent(item,event,eventAt!!);event="";eventAt=System.currentTimeMillis()}else vm.message("Choose a past or current event date")},enabled=event.isNotBlank())}}
   items(history,key={it.id}){h->Row(horizontalArrangement=Arrangement.spacedBy(14.dp),modifier=Modifier.fillMaxWidth().padding(vertical=7.dp)){Icon(Icons.Rounded.FiberManualRecord,null,Modifier.padding(top=5.dp).size(9.dp),tint=Muted);Column{Text(dateLabel(h.at,true),fontSize=11.sp,color=Muted);Text(h.text,modifier=Modifier.padding(top=5.dp),fontSize=15.sp)}}}
  }
  if(item.tags.isNotBlank())item{Text("Tags · ${item.tags}",color=Muted,fontSize=13.sp)}
  item{Column(verticalArrangement=Arrangement.spacedBy(5.dp)){Text("Created ${dateLabel(item.createdAt,true)}",color=Muted,fontSize=11.sp);Text("Updated ${dateLabel(item.updatedAt,true)}",color=Muted,fontSize=11.sp);item.reminderAt?.let{Text("Reminder ${dateLabel(it,true)}",color=Silver,fontSize=12.sp)};if(item.recurrence!="None")Text("Repeats ${item.recurrence.lowercase()}",color=Muted,fontSize=12.sp)}}
  if(!item.finished&&item.kind in listOf(Kind.TASK,Kind.FOLLOW,Kind.PROJECT))item{LiquidButton(if(item.kind==Kind.FOLLOW)"Mark resolved" else "Mark completed",{complete(item)},Modifier.fillMaxWidth())}
 }
 if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Archive this ${item.kind.lowercase()}?")},text={Text("It will be hidden from your workspace. You can restore it in Settings → Archive.")},confirmButton={TextButton(onClick={confirm=false;archive(item)}){Text("Archive")}},dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}})
 if(changeStatus)ModalBottomSheet(onDismissRequest={changeStatus=false},containerColor=MaterialTheme.colorScheme.surface){Column(Modifier.padding(24.dp)){Text("Change status",fontSize=24.sp);statuses(item.kind).forEach{status->TextButton(onClick={if(status=="Completed"||status=="Resolved")complete(item)else vm.save(item.copy(status=status));changeStatus=false},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)){Text(status)}}}}
}
