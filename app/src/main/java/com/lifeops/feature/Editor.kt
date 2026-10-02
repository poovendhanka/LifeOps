@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.lifeops.feature

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeops.core.database.*
import com.lifeops.core.design.*
import com.lifeops.core.util.*
import java.time.*

fun statuses(kind:String)=when(kind){Kind.TASK->listOf("Inbox","Planned","In Progress","Waiting","Completed","Cancelled");Kind.FOLLOW->listOf("Action Required","Waiting","Resolved","Closed");Kind.PROJECT->listOf("Planning","Active","Paused","Waiting","Completed","Archived");else->listOf("Active")}
@Composable fun SelectField(label:String,value:String,options:List<String>,onChange:(String)->Unit){var menu by remember{mutableStateOf(false)};Column {Text(label,color=Muted,fontSize=12.sp);Box{OutlinedButton(onClick={menu=true},modifier=Modifier.testTag("select:$label").fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(17.dp),border=BorderStroke(1.dp,Edge)){Text(value,Modifier.weight(1f));Icon(Icons.Rounded.ExpandMore,null)};DropdownMenu(expanded=menu,onDismissRequest={menu=false}){options.forEach{DropdownMenuItem(text={Text(it)},onClick={onChange(it);menu=false})}}}}}
@Composable fun EditText(label:String,value:String,onChange:(String)->Unit,lines:Int=1,numeric:Boolean=false) {OutlinedTextField(value,onChange,label={Text(label)},minLines=lines,maxLines=if(lines==1)1 else 10,singleLine=lines==1,shape=RoundedCornerShape(18.dp),modifier=Modifier.testTag("field:$label").fillMaxWidth(),keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Decimal else KeyboardType.Text))}
@Composable fun DateControl(label:String,value:Long?,onChange:(Long?)->Unit,withTime:Boolean=true,defaultHour:Int=9) {
 val c=LocalContext.current
 fun select(){val z=value?.let{Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())}?:ZonedDateTime.now().withHour(defaultHour).withMinute(0);DatePickerDialog(c,{_,year,month,day->val date=LocalDate.of(year,month+1,day);if(withTime)TimePickerDialog(c,{_,hour,minute->onChange(date.atTime(hour,minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())},z.hour,z.minute,false).show()else onChange(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())},z.year,z.monthValue-1,z.dayOfMonth).show()}
 Column{Text(label,fontSize=12.sp,color=Muted);Row(verticalAlignment=Alignment.CenterVertically){OutlinedButton(onClick={select()},modifier=Modifier.weight(1f).heightIn(min=50.dp),shape=RoundedCornerShape(17.dp),border=BorderStroke(1.dp,Edge)){Icon(Icons.Rounded.CalendarToday,null,Modifier.size(18.dp));Spacer(Modifier.width(10.dp));Text(if(value==null)"Choose date${if(withTime)" & time" else ""}" else dateLabel(value,withTime),Modifier.weight(1f))};if(value!=null)IconButton(onClick={onChange(null)}){Icon(Icons.Rounded.Close,"Clear $label")}}}
}
@Composable fun ItemEditor(initial:ItemEntity,all:List<ItemEntity>,vm:LifeViewModel,onDismiss:()->Unit,onSave:(ItemEntity)->Unit) {
 val c=LocalContext.current
 var title by rememberSaveable(initial.id){mutableStateOf(initial.title)};var body by rememberSaveable(initial.id){mutableStateOf(initial.body)}
 var next by rememberSaveable(initial.id){mutableStateOf(initial.nextAction)};var status by rememberSaveable(initial.id){mutableStateOf(initial.status)}
 var priority by rememberSaveable(initial.id){mutableIntStateOf(initial.priority)};var due by rememberSaveable(initial.id){mutableStateOf(initial.due)}
 var reminder by rememberSaveable(initial.id){mutableStateOf(initial.reminderAt)};var recurrence by rememberSaveable(initial.id){mutableStateOf(initial.recurrence)}
 var project by rememberSaveable(initial.id){mutableStateOf(initial.projectId)};var contact by rememberSaveable(initial.id){mutableStateOf(initial.contact)}
 var category by rememberSaveable(initial.id){mutableStateOf(initial.category)};var lastAction by rememberSaveable(initial.id){mutableStateOf(initial.lastAction)}
 var since by rememberSaveable(initial.id){mutableStateOf(initial.waitingSince)};var start by rememberSaveable(initial.id){mutableStateOf(initial.startAt)}
 var progress by rememberSaveable(initial.id){mutableFloatStateOf(initial.progress.toFloat())};var tags by rememberSaveable(initial.id){mutableStateOf(initial.tags)}
 var amount by rememberSaveable(initial.id){mutableStateOf(if(initial.amountMinor>0)java.math.BigDecimal(initial.amountMinor).movePointLeft(2).toPlainString()else "")}
 var payment by rememberSaveable(initial.id){mutableStateOf(initial.payment)};var uri by rememberSaveable(initial.id){mutableStateOf(initial.uri)}
 var pinned by rememberSaveable(initial.id){mutableStateOf(initial.pinned)};var error by remember{mutableStateOf<String?>(null)}
 var expanded by rememberSaveable(initial.id){mutableStateOf(false)};var saving by remember{mutableStateOf(false)};var discard by remember{mutableStateOf(false)}
 val existing=all.any{it.id==initial.id};val kind=initial.kind
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(!it)vm.message("Notifications are disabled. Enable them in Settings to receive reminders.")}
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){picked->if(picked!=null){try{c.contentResolver.takePersistableUriPermission(picked,Intent.FLAG_GRANT_READ_URI_PERMISSION);uri=picked.toString();if(title.isBlank())c.contentResolver.query(picked,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())title=it.getString(0)}}catch(_:Exception){error="Could not keep access to this file. Choose another provider."}}}
 fun draft()=initial.copy(title=title.trim(),body=body.trim(),nextAction=next.trim(),status=status,priority=priority,due=due,reminderAt=reminder,recurrence=recurrence,projectId=project,contact=contact.trim(),category=category,lastAction=lastAction.trim(),waitingSince=since,startAt=start,progress=progress.toInt(),tags=tags.trim(),amountMinor=parseMoney(amount)?:0,payment=payment,uri=uri,pinned=pinned)
 fun dismiss(){if(draft()!=initial)discard=true else onDismiss()}
 ModalBottomSheet(onDismissRequest={dismiss()},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(topStart=30.dp,topEnd=30.dp)) {
  Column(Modifier.testTag("editor").fillMaxWidth().imePadding().padding(horizontal=22.dp).verticalScroll(rememberScrollState()).padding(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(if(existing)"Edit ${kind.lowercase()}" else "New ${kind.lowercase()}",fontSize=25.sp);IconButton(onClick={dismiss()}){Icon(Icons.Rounded.Close,"Close editor")}}
   EditText(if(kind==Kind.NOTE)"Title (optional)" else if(kind==Kind.EXPENSE)"Description" else "Title",title,{title=it;error=null})
   if(kind==Kind.EXPENSE){EditText("Amount (₹)",amount,{amount=it;error=null},numeric=true);SelectField("Category",category,listOf("Food","Travel","Bills","Shopping","Health","Family","Subscriptions","Other")){category=it};SelectField("Payment method",payment,listOf("UPI","Cash","Credit card","Debit card","Bank transfer","Other")){payment=it};DateControl("Expense date",due,{due=it},false)}
   if(kind==Kind.NOTE)EditText("Your note",body,{body=it;error=null},6)
   if(kind in listOf(Kind.TASK,Kind.PROJECT,Kind.FOLLOW)) {
    EditText("Next action",next,{next=it},2)
    if(kind==Kind.FOLLOW){EditText("Person / company",contact,{contact=it});SelectField("Category",category,listOf("Refund","Service","Work","Finance","Government","Delivery","Approval","Personal","Other")){category=it}}
    DateControl(if(kind==Kind.FOLLOW)"Next follow-up" else if(kind==Kind.PROJECT)"Target date" else "Due date",due,{due=it},defaultHour=vm.settings.value.reminderHour)
    SelectField("Status",status,statuses(kind)){status=it;if(it=="Waiting"&&since==null)since=System.currentTimeMillis()}
   }
   if(kind==Kind.DOCUMENT){SelectField("Category",category,listOf("Receipt","Invoice","Work","Tax","Warranty","Government","Personal","Other")){category=it};OutlinedButton(onClick={picker.launch(arrayOf("application/pdf","image/*","text/*","application/vnd.openxmlformats-officedocument.wordprocessingml.document"))},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Icon(Icons.Rounded.AttachFile,null);Text(if(uri.isBlank())"Choose file" else "Replace attached file")};if(uri.isNotBlank())Text("File access retained on this device",color=Muted,fontSize=12.sp)}
   TextButton(onClick={expanded=!expanded}){Icon(if(expanded)Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,null);Text(if(expanded)"Fewer details" else "More details")}
   androidx.compose.animation.AnimatedVisibility(expanded){Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
    if(kind!=Kind.NOTE)EditText(if(kind==Kind.EXPENSE||kind==Kind.DOCUMENT)"Notes" else "Description / notes",body,{body=it},3)
    if(kind in listOf(Kind.TASK,Kind.FOLLOW,Kind.PROJECT))SelectField("Priority",listOf("Low","Normal","High","Critical")[priority],listOf("Low","Normal","High","Critical")){priority=listOf("Low","Normal","High","Critical").indexOf(it)}
    if(kind==Kind.TASK||kind==Kind.FOLLOW){DateControl("Reminder",reminder,{reminder=it;if(it!=null&&Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS)},defaultHour=vm.settings.value.reminderHour);Text("Android may delay reminders in battery-saving mode.",color=Muted,fontSize=12.sp)}
    if(kind==Kind.TASK)SelectField("Repeat",recurrence,listOf("None","Daily","Weekly","Monthly")){recurrence=it}
    if(kind==Kind.FOLLOW){EditText("Last action",lastAction,{lastAction=it},2);DateControl("Waiting since",since,{since=it},false)}
    if(kind==Kind.PROJECT){DateControl("Start date",start,{start=it},false);Text("Manual progress · ${progress.toInt()}%",color=Muted);Slider(progress,{progress=it},valueRange=0f..100f,steps=19);Text("Linked tasks calculate progress automatically.",fontSize=12.sp,color=Muted)}
    if(kind!=Kind.PROJECT&&kind!=Kind.EXPENSE){val projects=all.filter{it.kind==Kind.PROJECT&&!it.finished};SelectField("${if(kind==Kind.DOCUMENT)"Project / follow-up" else "Project"}",all.find{it.id==project}?.title?:"None",listOf("None")+(if(kind==Kind.DOCUMENT)all.filter{it.kind in listOf(Kind.PROJECT,Kind.FOLLOW)&&!it.finished}else projects).map{it.title+" · "+it.id.take(4)}){value->project=all.firstOrNull{it.title+" · "+it.id.take(4)==value}?.id}}
    EditText("Tags (comma separated)",tags,{tags=it})
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Pin to top");Switch(pinned,{pinned=it})}
   }}
   error?.let{Text(it,color=MaterialTheme.colorScheme.error,fontSize=13.sp)}
   LiquidButton(if(saving)"Saving…" else if(existing)"Save changes" else "Add ${kind.lowercase()}",{
    val record=draft()
    error=when {kind!=Kind.NOTE&&title.isBlank()->"Please enter a title.";kind==Kind.NOTE&&title.isBlank()&&body.isBlank()->"Write a note or add a title.";kind==Kind.EXPENSE&&parseMoney(amount)==null->"Enter a valid positive amount.";kind==Kind.DOCUMENT&&uri.isBlank()->"Choose a file to attach.";reminder!=null&&reminder!=initial.reminderAt&&reminder!!<=System.currentTimeMillis()->"Choose a future reminder time.";since!=null&&since!!>System.currentTimeMillis()->"Waiting cannot start in the future.";start!=null&&due!=null&&start!!>due!!->"Target date must follow the start date.";else->null}
    if(error==null){onSave(record)}
   },Modifier.fillMaxWidth(),enabled=!saving)
  }
 }
 if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Discard changes?")},text={Text("Your unsaved changes will be lost.")},confirmButton={TextButton(onClick=onDismiss){Text("Discard")}},dismissButton={TextButton(onClick={discard=false}){Text("Keep editing")}})
}
