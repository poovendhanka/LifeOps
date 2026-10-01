package com.lifeops.core.notifications
import android.app.PendingIntent
import android.appwidget.*
import android.content.*
import android.widget.RemoteViews
import com.lifeops.MainActivity
import com.lifeops.R
import com.lifeops.core.database.*
import com.lifeops.core.util.*
import kotlinx.coroutines.*
class LifeOpsWidget:AppWidgetProvider() {
 override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray){val pending=goAsync();CoroutineScope(Dispatchers.IO).launch{try{update(c)}finally{pending.finish()}}}
 companion object {
  suspend fun update(c:Context){val m=AppWidgetManager.getInstance(c);val ids=m.getAppWidgetIds(ComponentName(c,LifeOpsWidget::class.java));if(ids.isEmpty())return;val all=LifeDatabase.get(c).dao().all().filter{!it.deleted&&!it.finished};val today=all.filter{it.kind in listOf(Kind.TASK,Kind.FOLLOW)&&it.due!=null&&it.due<dayEnd()}.sortedBy{it.due};val next=today.firstOrNull()?:all.firstOrNull{it.nextAction.isNotBlank()}
   ids.forEach{id->val view=RemoteViews(c.packageName,R.layout.lifeops_widget);view.setTextViewText(R.id.widget_counts,"Today ${today.size}  ·  Waiting ${all.count{it.status=="Waiting"}}");view.setTextViewText(R.id.widget_next,next?.let{"Next: ${it.nextAction.ifBlank{it.title}}"}?:"A clear day ahead.");view.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,id,Intent(c,MainActivity::class.java).putExtra("itemId",next?.id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE));view.setOnClickPendingIntent(R.id.widget_add,PendingIntent.getActivity(c,id+100000,Intent(c,MainActivity::class.java).putExtra("capture",true),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE));m.updateAppWidget(id,view)}
  }
 }
}
