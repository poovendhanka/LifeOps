@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.lifeops.feature

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeops.core.database.*
import com.lifeops.core.design.*
import com.lifeops.core.util.*

@Composable fun ItemListScreen(title:String,kind:String,all:List<ItemEntity>,open:(ItemEntity)->Unit,complete:(ItemEntity)->Unit,archive:(ItemEntity)->Unit,edit:(ItemEntity)->Unit,create:()->Unit) {
 var query by rememberSaveable {mutableStateOf("")};var filter by rememberSaveable{mutableStateOf(if(kind==Kind.TASK)"Today" else "Active")}
 var sort by rememberSaveable{mutableStateOf("Due date")};var showFilters by remember{mutableStateOf(false)};var priority by rememberSaveable{mutableIntStateOf(-1)}
 var selected by remember{mutableStateOf(setOf<String>())};var confirmArchive by remember{mutableStateOf(false)}
 val choices=if(kind==Kind.TASK)listOf("Today","Upcoming","Overdue","All","Completed") else if(kind==Kind.FOLLOW)listOf("Active","Waiting","Overdue","Resolved","All") else if(kind==Kind.PROJECT)listOf("Active","Completed","All") else listOf("All","Pinned")
 val actualFilter=if(filter in choices)filter else "All"
 val records=all.filter {it.kind==kind&&("${it.title} ${it.body} ${it.tags} ${it.nextAction} ${it.contact}").contains(query,true)&&(priority<0||it.priority==priority)&&when(actualFilter){"Today"->!it.finished&&it.due!=null&&it.due<dayEnd();"Upcoming"->!it.finished&&it.due!=null&&it.due>=dayEnd();"Overdue"->!it.finished&&it.due!=null&&it.due<dayStart();"Completed"->it.finished;"Resolved"->it.finished;"Waiting"->it.status=="Waiting";"Active"->!it.finished;"Pinned"->it.pinned;else->true}}.let{list->when(sort){"Priority"->list.sortedByDescending{it.priority};"Recently updated"->list.sortedByDescending{it.updatedAt};"Title"->list.sortedBy{it.title.lowercase()};else->list.sortedWith(compareByDescending<ItemEntity>{it.pinned}.thenBy{it.finished}.thenBy{it.due?:Long.MAX_VALUE})}}
 LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(22.dp,8.dp,22.dp,100.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item{PageTitle(title,if(kind==Kind.FOLLOW)"Keep the next conversation in sight." else "${records.size} ${if(records.size==1)"item" else "items"}"){IconButton(onClick={showFilters=true}){Icon(Icons.Rounded.Tune,"Filter and sort")}}}
  item{OutlinedTextField(query,{query=it},placeholder={Text("Search ${title.lowercase()}")},leadingIcon={Icon(Icons.Rounded.Search,null)},singleLine=true,shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth())}
  item{Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){choices.forEach{FilterChip(selected=actualFilter==it,onClick={filter=it},label={Text(it)})}}}
  if(selected.isNotEmpty())item{LifeOpsCard{Text("${selected.size} selected");Row {TextButton(onClick={all.filter{it.id in selected&&!it.finished}.forEach(complete);selected=emptySet()}){Text("Complete")};TextButton(onClick={confirmArchive=true}){Text("Archive")};TextButton(onClick={selected=emptySet()}){Text("Cancel")}}}}
  if(kind==Kind.EXPENSE)item{LifeOpsCard {Eyebrow("TOTAL IN THIS VIEW");Text(money(records.sumOf{it.amountMinor}),fontSize=30.sp)}}
  if(records.isEmpty())item{EmptyState("Nothing here yet",if(query.isNotBlank())"Try another search or change your filters." else "Make a little space in your mind. Capture it here.","Add ${kind.lowercase()}",create)}
  items(records,key={it.id}){item->
   val state=rememberSwipeToDismissBoxState(confirmValueChange={value->if(value==SwipeToDismissBoxValue.StartToEnd&&kind in listOf(Kind.TASK,Kind.FOLLOW)&&!item.finished)complete(item);if(value==SwipeToDismissBoxValue.EndToStart)edit(item);false})
   SwipeToDismissBox(state=state,enableDismissFromStartToEnd=kind in listOf(Kind.TASK,Kind.FOLLOW)&&!item.finished,backgroundContent={Row(Modifier.fillMaxSize().padding(18.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("Done",color=Silver);Text("Edit / reschedule",color=Silver)}}) {
    ItemRow(item,all,{if(selected.isEmpty())open(it)else selected=if(it.id in selected)selected-it.id else selected+it.id},{if(selected.isEmpty())complete(it)else selected=selected-it.id},item.id in selected,{selected=selected+item.id})
   }
  }
  if(records.isNotEmpty())item{Text("Swipe left to edit · Hold to select",fontSize=11.sp,color=Muted)}
 }
 if(showFilters)ModalBottomSheet(onDismissRequest={showFilters=false},containerColor=MaterialTheme.colorScheme.surface) {Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Make it your view",fontSize=24.sp);SelectField("Sort by",sort,listOf("Due date","Priority","Recently updated","Title")){sort=it};SelectField("Priority",if(priority<0)"Any" else listOf("Low","Normal","High","Critical")[priority],listOf("Any","Low","Normal","High","Critical")){priority=listOf("Low","Normal","High","Critical").indexOf(it)};LiquidButton("Apply",{showFilters=false},Modifier.fillMaxWidth());Spacer(Modifier.height(12.dp))}}
 if(confirmArchive)AlertDialog(onDismissRequest={confirmArchive=false},title={Text("Archive ${selected.size} items?")},text={Text("They will be hidden from your workspace and kept in the database.")},confirmButton={TextButton(onClick={all.filter{it.id in selected}.forEach(archive);selected=emptySet();confirmArchive=false}){Text("Archive")}},dismissButton={TextButton(onClick={confirmArchive=false}){Text("Cancel")}})
}

@Composable fun SearchScreen(all:List<ItemEntity>,open:(ItemEntity)->Unit) {
 var query by rememberSaveable{mutableStateOf("")}
 val results=remember(all,query){if(query.isBlank())emptyList()else all.filter {("${it.title} ${it.body} ${it.nextAction} ${it.contact} ${it.category} ${it.tags} ${it.lastAction}").contains(query.trim(),true)}}
 LazyColumn(contentPadding=PaddingValues(22.dp,8.dp,22.dp,100.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  item{PageTitle("Find your things","One search. Your whole workspace.")}
  item{OutlinedTextField(query,{query=it},singleLine=true,placeholder={Text("Search everything")},leadingIcon={Icon(Icons.Rounded.Search,null)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp))}
  if(query.isBlank())item{LifeOpsCard {Text("A thought, a project, a person…",fontSize=19.sp);Text("Search titles, notes, next actions and tags.",color=Muted)}}
  else if(results.isEmpty())item{Text("No results for “$query”",color=Muted)}
  results.groupBy{it.kind}.forEach{(kind,records)->item{Section("$kind · ${records.size}")};items(records,key={it.id}){ItemRow(it,all,open,{open(it)})}}
 }
}
