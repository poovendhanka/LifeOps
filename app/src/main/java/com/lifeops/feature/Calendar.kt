package com.lifeops.feature
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeops.core.database.*
import com.lifeops.core.design.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable fun CalendarScreen(all:List<ItemEntity>,vm:LifeViewModel,open:(ItemEntity)->Unit){
 var monthText by rememberSaveable{mutableStateOf(YearMonth.now().toString())};var selectedText by rememberSaveable{mutableStateOf(LocalDate.now().toString())}
 val month=YearMonth.parse(monthText);val selected=LocalDate.parse(selectedText);val settings by vm.settings.collectAsStateWithLifecycle()
 val dates=all.filter{it.due!=null&&it.kind in listOf(Kind.TASK,Kind.FOLLOW,Kind.PROJECT)}.groupBy{Instant.ofEpochMilli(it.due!!).atZone(ZoneId.systemDefault()).toLocalDate()}
 val offset=if(settings.mondayFirst)month.atDay(1).dayOfWeek.value-1 else month.atDay(1).dayOfWeek.value%7
 val weeks=List(((offset+month.lengthOfMonth()+6)/7)*7){index->(index-offset+1).takeIf{it in 1..month.lengthOfMonth()}}.chunked(7)
 LazyColumn(contentPadding=PaddingValues(22.dp,8.dp,22.dp,100.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
 item{PageTitle("Calendar","Your next actions, in time.")}
 item{LifeOpsCard{
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){IconButton(onClick={monthText=month.minusMonths(1).toString()}){Icon(Icons.Rounded.ChevronLeft,"Previous month")};Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),fontSize=20.sp);IconButton(onClick={monthText=month.plusMonths(1).toString()}){Icon(Icons.Rounded.ChevronRight,"Next month")}}
 Row{(if(settings.mondayFirst)listOf("M","T","W","T","F","S","S")else listOf("S","M","T","W","T","F","S")).forEach{Box(Modifier.weight(1f),contentAlignment=Alignment.Center){Text(it,color=Muted,fontSize=12.sp)}}}
 weeks.forEach{week->Row{week.forEach{day->Box(Modifier.weight(1f).heightIn(min=48.dp).clip(CircleShape).then(if(day!=null)Modifier.background(if(selected==month.atDay(day))Silver else Black).clickable{selectedText=month.atDay(day).toString()}else Modifier),contentAlignment=Alignment.Center){if(day!=null)Column(horizontalAlignment=Alignment.CenterHorizontally){Text(day.toString(),fontSize=14.sp,color=if(selected==month.atDay(day))Black else Silver);if(dates[month.atDay(day)]?.isNotEmpty()==true)Text("•",fontSize=10.sp,color=if(selected==month.atDay(day))Black else Silver)}}}}}
 }}
 item{Section(selected.format(DateTimeFormatter.ofPattern("EEEE, d MMM")))}
 if(dates[selected].isNullOrEmpty())item{Text("No scheduled items. Enjoy the space.",color=Muted)}
 items(dates[selected].orEmpty(),key={it.id}){ItemRow(it,all,open,{open(it)})}
 }
}
