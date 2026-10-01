@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class,androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.lifeops.feature

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.lifeops.core.database.*
import com.lifeops.core.design.*
import com.lifeops.core.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.*
import java.time.format.DateTimeFormatter

fun kindIcon(kind:String):ImageVector=when(kind){Kind.FOLLOW->Icons.Rounded.HourglassTop;Kind.PROJECT->Icons.Rounded.Layers;Kind.NOTE->Icons.Rounded.EditNote;Kind.EXPENSE->Icons.Rounded.AccountBalanceWallet;Kind.DOCUMENT->Icons.Rounded.Description;else->Icons.Rounded.CheckCircleOutline}
fun initialItem(kind:String)=ItemEntity(kind=kind,status=when(kind){Kind.FOLLOW->"Waiting";Kind.PROJECT->"Planning";Kind.TASK->"Inbox";else->"Active"},due=if(kind==Kind.EXPENSE)System.currentTimeMillis() else null,waitingSince=if(kind==Kind.FOLLOW)System.currentTimeMillis() else null)

@Composable fun LifeOpsRoot(vm:LifeViewModel,openedId:String?,captureRequested:Boolean,onOpened:()->Unit) {
 val all by vm.items.collectAsStateWithLifecycle()
 val nav=rememberNavController();val back by nav.currentBackStackEntryAsState();val route=back?.destination?.route?:"Home"
 val snackbar=remember{SnackbarHostState()};val scope=rememberCoroutineScope()
 var capture by rememberSaveable {mutableStateOf(false)}
 var edit by rememberSaveable(stateSaver=androidx.compose.runtime.saveable.Saver<ItemEntity?,String>(save={it?.let{ItemCodec.encode(it).toString()}?:""},restore={if(it.isEmpty())null else ItemCodec.decode(org.json.JSONObject(it))})) {mutableStateOf<ItemEntity?>(null)}
 val tabs=listOf("Home" to Icons.Rounded.Home,"Tasks" to Icons.Rounded.CheckCircleOutline,"Projects" to Icons.Rounded.Layers,"Tools" to Icons.Rounded.Tune,"More" to Icons.Rounded.GridView)
 fun go(path:String){nav.navigate(path){launchSingleTop=true}}
 fun new(kind:String){capture=false;edit=initialItem(kind)}
 fun archive(item:ItemEntity){vm.archive(item);scope.launch {if(snackbar.showSnackbar("Moved to archive","Undo",duration=SnackbarDuration.Long)==SnackbarResult.ActionPerformed)vm.restore(item)}}
 fun complete(item:ItemEntity){vm.complete(item);scope.launch {val undo=if(item.recurrence=="None")"Undo" else null;if(snackbar.showSnackbar(if(undo!=null)"Completed" else "Completed · next occurrence created",undo)==SnackbarResult.ActionPerformed)vm.save(item)}}
 LaunchedEffect(Unit){vm.messages.collect{snackbar.showSnackbar(it)}}
 LaunchedEffect(captureRequested){if(captureRequested){capture=true;onOpened()}}
 LaunchedEffect(openedId){if(openedId!=null){go("detail/$openedId");onOpened()}}
 Scaffold(containerColor=Black,snackbarHost={SnackbarHost(snackbar)},
  bottomBar={Surface(color=Black){Row(Modifier.navigationBarsPadding().padding(horizontal=12.dp,vertical=8.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){tabs.forEach{(label,icon)->
   val selected=route==label
   val bg by animateColorAsState(if(selected)Color(0xFF25252B)else Color.Transparent,label="nav")
   Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(bg).clickable {nav.navigate(label){popUpTo(nav.graph.startDestinationId){saveState=true};launchSingleTop=true;restoreState=true}}.padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,label,tint=if(selected)Silver else Muted,modifier=Modifier.size(23.dp));Spacer(Modifier.height(5.dp));Text(label,fontSize=10.sp,color=if(selected)Silver else Muted)}
  }}}},
  floatingActionButton={if(!route.startsWith("detail")) FloatingActionButton(onClick={capture=true},shape=CircleShape,containerColor=Silver,contentColor=Black,modifier=Modifier.size(58.dp)){Icon(Icons.Rounded.Add,"Quick capture",Modifier.size(28.dp))}}
 ) {padding->
  NavHost(nav,startDestination="Home",modifier=Modifier.padding(padding),enterTransition={fadeIn()},exitTransition={fadeOut()}) {
   composable("Home"){HomeScreen(all,{go(it)},{go("detail/${it.id}")},::new,::complete)}
   composable("Tasks"){ItemListScreen("Tasks",Kind.TASK,all,{go("detail/${it.id}")},::complete,::archive,{edit=it},{new(Kind.TASK)})}
   composable("Projects"){ItemListScreen("Projects",Kind.PROJECT,all,{go("detail/${it.id}")},::complete,::archive,{edit=it},{new(Kind.PROJECT)})}
   composable("Follow-ups"){ItemListScreen("Follow-ups",Kind.FOLLOW,all,{go("detail/${it.id}")},::complete,::archive,{edit=it},{new(Kind.FOLLOW)})}
   composable("Notes"){ItemListScreen("Notes",Kind.NOTE,all,{go("detail/${it.id}")},::complete,::archive,{edit=it},{new(Kind.NOTE)})}
   composable("Expenses"){ItemListScreen("Expenses",Kind.EXPENSE,all,{go("detail/${it.id}")},::complete,::archive,{edit=it},{new(Kind.EXPENSE)})}
   composable("Documents"){ItemListScreen("Documents",Kind.DOCUMENT,all,{go("detail/${it.id}")},::complete,::archive,{edit=it},{new(Kind.DOCUMENT)})}
   composable("Search"){SearchScreen(all){go("detail/${it.id}")}}
   composable("Tools"){ToolsScreen()}
   composable("Calendar"){CalendarScreen(all,vm){go("detail/${it.id}")}}
   composable("Settings"){SettingsScreen(vm)}
   composable("More"){MoreScreen(::go)}
   composable("detail/{id}"){entry->DetailScreen(entry.arguments?.getString("id")?: "",all,vm,{nav.popBackStack()},{edit=it},::complete,{archive(it);nav.popBackStack()},{go("detail/${it.id}")},{edit=initialItem(it.first).copy(projectId=it.second)})}
  }
 }
 if(capture)ModalBottomSheet(onDismissRequest={capture=false},containerColor=Color(0xFF111114),shape=RoundedCornerShape(topStart=30.dp,topEnd=30.dp)) {
  Column(Modifier.padding(horizontal=24.dp).padding(bottom=28.dp)){Eyebrow("GET IT OUT OF YOUR HEAD");Text("What’s on your mind?",fontSize=25.sp,modifier=Modifier.padding(vertical=12.dp));Kind.all.forEach{kind->ListItem(headlineContent={Text(if(kind==Kind.DOCUMENT)"Add document" else "New ${kind.lowercase()}")},leadingContent={Icon(kindIcon(kind),null)},trailingContent={Icon(Icons.Rounded.ArrowOutward,null)},colors=ListItemDefaults.colors(containerColor=Color.Transparent),modifier=Modifier.clip(RoundedCornerShape(18.dp)).clickable{new(kind)})}}
 }
 edit?.let{item->ItemEditor(item,all,vm,onDismiss={edit=null}){vm.save(it){edit=null}}}
}

@Composable fun PageTitle(title:String,subtitle:String?=null,action:(@Composable ()->Unit)?=null){Row(Modifier.fillMaxWidth().padding(top=12.dp,bottom=10.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontSize=30.sp,fontWeight=FontWeight.Medium);subtitle?.let{Text(it,color=Muted,fontSize=13.sp,modifier=Modifier.padding(top=5.dp))}};action?.invoke()}}

@Composable fun HomeScreen(all:List<ItemEntity>,go:(String)->Unit,open:(ItemEntity)->Unit,create:(String)->Unit,complete:(ItemEntity)->Unit) {
 var now by remember {mutableLongStateOf(System.currentTimeMillis())}
 LaunchedEffect(Unit){while(true){now=System.currentTimeMillis();delay(60000)}}
 val active=all.filter{!it.finished};val attention=active.filter{it.kind in listOf(Kind.TASK,Kind.FOLLOW)&&it.due!=null&&it.due<dayEnd(now)}
 val waiting=active.filter{it.status=="Waiting"}.sortedBy{it.waitingSince?:it.createdAt}
 val projects=active.filter{it.kind==Kind.PROJECT}
 val date=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
 val greeting=when(date.hour){in 5..11->"Good morning";in 12..16->"Good afternoon";else->"Good evening"}
 val monthStart=date.withDayOfMonth(1).toLocalDate().atStartOfDay(date.zone).toInstant().toEpochMilli()
 val spent=all.filter{it.kind==Kind.EXPENSE&&(it.due?:it.createdAt)>=monthStart&&(it.due?:it.createdAt)<=now}.sumOf{it.amountMinor}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(start=22.dp,end=22.dp,bottom=100.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item{Row(Modifier.fillMaxWidth().padding(top=18.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("LifeOps",fontSize=25.sp,fontWeight=FontWeight.SemiBold,letterSpacing=(-.7).sp);Row{IconButton(onClick={go("Search")}){Icon(Icons.Rounded.Search,"Search everything")};IconButton(onClick={go("Settings")}){Icon(Icons.Rounded.Settings,"Settings")}}}}
  item{Column(Modifier.padding(top=4.dp,bottom=6.dp)){Eyebrow(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")));Text(greeting+".",fontSize=30.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=8.dp));Text("A little clarity for what’s next.",color=Muted,fontSize=14.sp,modifier=Modifier.padding(top=6.dp))}}
  item{LifeOpsCard(onClick={go("Tasks")}){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Eyebrow("YOUR FOCUS");Icon(Icons.Rounded.NorthEast,null,tint=Muted,modifier=Modifier.size(20.dp))}
   Row(verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(15.dp)){AnimatedContent(attention.size,label="attention"){Text(it.toString().padStart(2,'0'),fontSize=64.sp,fontWeight=FontWeight.Light,letterSpacing=(-3).sp)};Text("things need\nyour attention",fontSize=17.sp,color=Silver,modifier=Modifier.padding(bottom=12.dp))}
   HorizontalDivider(color=Edge)
   Row(Modifier.fillMaxWidth().padding(top=6.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("${waiting.size} waiting",color=Muted,fontSize=12.sp);Text("${projects.size} active projects",color=Muted,fontSize=12.sp)}
  }}
  item{Section("Next up","View all"){go("Tasks")}}
  if(attention.isEmpty())item{EmptyState("A clear day ahead","Capture a next action, or make progress on a project.","Add a task"){create(Kind.TASK)}}
  else items(attention.sortedWith(compareBy<ItemEntity>{it.due}.thenByDescending{it.priority}).take(4),key={it.id}){ItemRow(it,all,open,complete)}
  item{Section("Waiting for","See all"){go("Follow-ups")}}
  if(waiting.isEmpty())item{LifeOpsCard {Text("Nothing waiting",fontSize=17.sp);Text("You’re currently caught up.",color=Muted,fontSize=13.sp)}}
  else items(waiting.take(3),key={"wait"+it.id}){item->LifeOpsCard(onClick={open(item)}){Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.HourglassTop,null,tint=Muted,modifier=Modifier.size(21.dp));Column(Modifier.weight(1f)){Text(item.title,fontSize=16.sp);Text(waitingLabel(item.waitingSince?:item.createdAt,now),color=Muted,fontSize=12.sp)};Icon(Icons.Rounded.ChevronRight,null,tint=Muted)};if(item.nextAction.isNotBlank())Text("Next · ${item.nextAction}",color=Silver,fontSize=13.sp)}}}
  item{Section("In motion","Projects"){go("Projects")}}
  if(projects.isEmpty())item{EmptyState("Start something meaningful","Keep the next step close, and the bigger picture in view.","New project"){create(Kind.PROJECT)}}
  else items(projects.take(3),key={"project"+it.id}){ItemRow(it,all,open,complete)}
  item{LifeOpsCard(onClick={go("Expenses")}){Eyebrow("SPENT THIS MONTH");Text(money(spent),fontSize=28.sp,fontWeight=FontWeight.Light);Text("Small details. A clearer picture.",fontSize=12.sp,color=Muted)}}
  item{Section("Quick capture")}
  item{Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Kind.all.chunked(3).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{kind->Surface(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).clickable{create(kind)},color=Color(0xFF111114),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,Edge)){Column(Modifier.padding(vertical=16.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(kindIcon(kind),null,Modifier.size(23.dp),tint=Silver);Text(kind,fontSize=11.sp,color=Muted,modifier=Modifier.padding(top=8.dp))}}}}}}
 }
}

@Composable fun ItemRow(item:ItemEntity,all:List<ItemEntity>,open:(ItemEntity)->Unit,complete:(ItemEntity)->Unit,selected:Boolean=false,onLong:()->Unit={}) {
 val haptic=LocalHapticFeedback.current
 val tasks=all.filter{it.projectId==item.id&&it.kind==Kind.TASK}
 val progress=if(tasks.isNotEmpty())tasks.count{it.status=="Completed"}.toFloat()/tasks.size else item.progress/100f
 LifeOpsCard(Modifier.combinedClickable(onClick={open(item)},onLongClick={haptic.performHapticFeedback(HapticFeedbackType.LongPress);onLong()})) {
  Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Top){
   if(item.kind==Kind.TASK||selected)IconButton(onClick={haptic.performHapticFeedback(HapticFeedbackType.LongPress);complete(item)},modifier=Modifier.size(48.dp).offset(x=(-8).dp,y=(-10).dp)){Icon(if(item.finished||selected)Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,if(selected)"Selected" else "Complete ${item.title}",tint=Silver)}
   else Icon(kindIcon(item.kind),null,tint=Muted,modifier=Modifier.padding(top=2.dp).size(21.dp))
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Text(item.title.ifBlank{item.body.take(70).ifBlank{"Untitled note"}},fontSize=16.sp,fontWeight=FontWeight.Medium,maxLines=2,overflow=TextOverflow.Ellipsis)
    val meta=when(item.kind){Kind.EXPENSE->"${money(item.amountMinor)} · ${item.category}";Kind.NOTE->item.body;Kind.FOLLOW->item.waitingSince?.let{waitingLabel(it)}?:item.status;Kind.DOCUMENT->item.category;else->if(item.due!=null)dateLabel(item.due)+(if(!item.finished&&item.due<dayStart())" · Overdue" else "") else item.status}
    Text(meta,color=Muted,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
   }
   if(item.priority>=2)Icon(if(item.priority==3)Icons.Rounded.PriorityHigh else Icons.Rounded.KeyboardDoubleArrowUp,if(item.priority==3)"Critical priority" else "High priority",modifier=Modifier.size(18.dp),tint=Silver)
   if(item.pinned)Icon(Icons.Rounded.PushPin,"Pinned",Modifier.size(16.dp),tint=Muted)
  }
  if(item.nextAction.isNotBlank()) {if(item.kind==Kind.PROJECT)Eyebrow("NEXT ACTION");Text(if(item.kind==Kind.PROJECT)item.nextAction else "Next · ${item.nextAction}",fontSize=13.sp,color=Silver,maxLines=2,overflow=TextOverflow.Ellipsis)}
  if(item.kind==Kind.PROJECT){LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),color=Silver,trackColor=Edge);Text("${(progress*100).toInt()}% · ${tasks.count{it.status=="Completed"}} / ${tasks.size} tasks · ${item.status}",fontSize=11.sp,color=Muted);if(System.currentTimeMillis()-item.updatedAt>10*86400000L)Text("No activity for 10+ days",fontSize=12.sp,color=Silver)}
 }
}

@Composable
fun MoreScreen(go: (String) -> Unit) {
    val entries = listOf(
        "Follow-ups" to Icons.Rounded.HourglassTop,
        "Notes" to Icons.Rounded.EditNote,
        "Expenses" to Icons.Rounded.AccountBalanceWallet,
        "Documents" to Icons.Rounded.Description,
        "Calendar" to Icons.Rounded.CalendarMonth,
        "Search" to Icons.Rounded.Search,
        "Settings" to Icons.Rounded.Settings
    )
    LazyColumn(
        contentPadding = PaddingValues(22.dp, 8.dp, 22.dp, 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { PageTitle("Your workspace", "Everything has its place.") }
        items(entries, key = { it.first }) { (label, icon) ->
            LifeOpsCard(onClick = { go(label) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = Silver)
                    Text(label, Modifier.weight(1f), fontSize = 17.sp)
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Muted)
                }
            }
        }
        item {
            Text(
                "LIFEOPS  /  PRIVATE BY DESIGN",
                fontSize = 10.sp,
                color = Muted,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}
