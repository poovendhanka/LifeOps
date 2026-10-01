package com.lifeops.feature
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeops.core.design.*
import com.lifeops.core.util.*
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable fun ToolsScreen(){
 val tools=listOf("Percentage","EMI","Date difference","Age","Electricity usage","Fuel mileage","Expense split")
 var tool by rememberSaveable{mutableStateOf("Percentage")};var a by rememberSaveable(tool){mutableStateOf("")};var b by rememberSaveable(tool){mutableStateOf("")};var c by rememberSaveable(tool){mutableStateOf("")};var d1 by rememberSaveable(tool){mutableStateOf<Long?>(null)};var d2 by rememberSaveable(tool){mutableStateOf<Long?>(System.currentTimeMillis())}
 val x=a.toDoubleOrNull();val y=b.toDoubleOrNull();val z=c.toDoubleOrNull()
 fun fmt(n:Double)=if(n.isFinite())String.format(Locale.getDefault(),"%,.2f",n)else "Value too large"
 val result=try{when(tool){
 "Percentage"->if(x!=null&&y!=null)"${fmt(y)}% of ${fmt(x)}\n= ${fmt(x*y/100)}"else "Enter a value and percentage"
 "EMI"->if(x!=null&&y!=null&&z!=null&&x>0&&y>=0&&z>=1&&z<=1200&&z%1==0.0){val e=emi(x,y,z.toInt());"₹${fmt(e)} / month\nTotal interest  ₹${fmt(e*z-x)}\nTotal repayment  ₹${fmt(e*z)}"}else "Enter principal, annual rate and whole months"
 "Electricity usage"->if(x!=null&&y!=null&&x>=0&&y>=x){"${fmt(y-x)} kWh used"+(if(z!=null&&z>=0)"\nEstimated cost  ₹${fmt((y-x)*z)}"else "")}else "Current reading must be at least the previous reading"
 "Fuel mileage"->if(x!=null&&y!=null&&x>0&&y>0)"${fmt(x/y)} km / litre"else "Enter positive distance and litres"
 "Expense split"->if(x!=null&&y!=null&&x>=0&&y>=1&&y%1==0.0)"₹${fmt(x/y)} per person"else "Enter a total and whole number of people"
 "Date difference","Age"->if(d1!=null&&d2!=null){val start=Instant.ofEpochMilli(d1!!).atZone(ZoneId.systemDefault()).toLocalDate();val end=Instant.ofEpochMilli(d2!!).atZone(ZoneId.systemDefault()).toLocalDate();if(end<start)"End date must follow start date"else{val p=Period.between(start,end);"${p.years} years · ${p.months} months · ${p.days} days\n${ChronoUnit.DAYS.between(start,end)} total days"}}else "Choose both dates"
 else->""
 }}catch(_:Exception){"Check the entered values"}
 LazyColumn(contentPadding=PaddingValues(22.dp,8.dp,22.dp,100.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
 item{PageTitle("Everyday tools","Small calculations. Less mental clutter.")}
 item{SelectField("Calculator",tool,tools){tool=it;a="";b="";c="";d1=null;d2=System.currentTimeMillis()}}
 item{LifeOpsCard{Eyebrow("${tool.uppercase()} CALCULATOR")
 if(tool in listOf("Date difference","Age")){DateControl(if(tool=="Age")"Date of birth" else "From",d1,{d1=it},false);DateControl(if(tool=="Age")"Age on" else "To",d2,{d2=it},false)}else{
 EditText(when(tool){"EMI"->"Loan amount (₹)";"Electricity usage"->"Previous meter reading";"Fuel mileage"->"Distance (km)";"Expense split"->"Total expense (₹)";else->"Value"},a,{a=it},numeric=true)
 EditText(when(tool){"EMI"->"Annual interest (%)";"Electricity usage"->"Current meter reading";"Fuel mileage"->"Fuel used (litres)";"Expense split"->"People";else->"Percentage (%)"},b,{b=it},numeric=true)
 if(tool in listOf("EMI","Electricity usage"))EditText(if(tool=="EMI")"Tenure (months)" else "Flat rate / kWh (₹, optional)",c,{c=it},numeric=true)
 }}}
 item{LifeOpsCard{Eyebrow("RESULT");Text(result,fontSize=22.sp,lineHeight=34.sp)}}
 if(tool=="Electricity usage")item{Text("Flat-rate estimate only; excludes slab pricing, taxes and fixed charges.",color=Muted,fontSize=12.sp)}
 if(tool=="EMI")item{Text("Fixed-rate monthly estimate; excludes lender fees and insurance.",color=Muted,fontSize=12.sp)}
 }
}
