package com.lifeops.core.util

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.pow

fun dayStart(now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()):Long=Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
fun dayEnd(now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()):Long=Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
fun dateLabel(value:Long?,withTime:Boolean=false):String=value?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(if(withTime) "d MMM yyyy · h:mm a" else "d MMM yyyy")) }?:"No date"
fun waitingDays(since:Long,now:Long=System.currentTimeMillis(),zone:ZoneId=ZoneId.systemDefault()):Long=ChronoUnit.DAYS.between(Instant.ofEpochMilli(since).atZone(zone).toLocalDate(),Instant.ofEpochMilli(now).atZone(zone).toLocalDate()).coerceAtLeast(0)
fun waitingLabel(since:Long,now:Long=System.currentTimeMillis()):String {val d=waitingDays(since,now); return when {d==0L->"Waiting since today";d==1L->"Waiting 1 day";d<14->"Waiting $d days";else->"Waiting ${d/7} weeks · $d days"}}
fun nextRecurrence(at:Long,rule:String,now:Long=System.currentTimeMillis()):Long {
 var next=Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
 do { next=when(rule){"Daily"->next.plusDays(1);"Weekly"->next.plusWeeks(1);"Monthly"->next.plusMonths(1);else->return at} } while(next.toInstant().toEpochMilli()<=now)
 return next.toInstant().toEpochMilli()
}
fun emi(principal:Double,annualRate:Double,months:Int):Double { require(principal>=0 && annualRate>=0 && months>0);val r=annualRate/1200;return if(r==0.0)principal/months else principal*r*(1+r).pow(months)/((1+r).pow(months)-1) }
fun money(minor:Long):String="₹"+java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag("en-IN")).apply { minimumFractionDigits=2;maximumFractionDigits=2 }.format(minor/100.0)
fun parseMoney(text:String):Long?=try { text.toBigDecimal().setScale(2,java.math.RoundingMode.HALF_UP).movePointRight(2).longValueExact().takeIf{it>0} }catch(_:Exception){null}
