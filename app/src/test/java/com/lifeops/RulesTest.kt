package com.lifeops
import com.lifeops.core.util.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class RulesTest {
 @Test fun zeroInterestEmi(){assertEquals(1000.0,emi(12000.0,0.0,12),.001)}
 @Test fun standardEmi(){assertEquals(8884.8789,emi(100000.0,12.0,12),.01)}
 @Test(expected=IllegalArgumentException::class) fun invalidTenure(){emi(1000.0,10.0,0)}
 @Test fun moneyUsesMinorUnits(){assertEquals(166000L,parseMoney("1660"));assertEquals(124L,parseMoney("1.235"));assertNull(parseMoney("-5"));assertNull(parseMoney("NaN"));assertNull(parseMoney("0"));assertNull(parseMoney("999999999999999999999999"))}
 @Test fun waitingUsesCalendarDays(){val z=ZoneId.of("Asia/Kolkata");val a=ZonedDateTime.of(2026,9,30,23,50,0,0,z).toInstant().toEpochMilli();val b=ZonedDateTime.of(2026,10,1,0,10,0,0,z).toInstant().toEpochMilli();assertEquals(1L,waitingDays(a,b,z));assertEquals(0L,waitingDays(b,a,z))}
 @Test fun dstDayIsNotAlways24Hours(){val z=ZoneId.of("America/New_York");val now=ZonedDateTime.of(2026,3,8,12,0,0,0,z).toInstant().toEpochMilli();assertEquals(23*3600000L,dayEnd(now,z)-dayStart(now,z))}
 @Test fun monthlyRecurrenceHandlesMonthEnd(){val z=ZoneId.systemDefault();val a=LocalDate.of(2026,1,31).atTime(9,0).atZone(z).toInstant().toEpochMilli();val result=Instant.ofEpochMilli(nextRecurrence(a,"Monthly",a)).atZone(z);assertEquals(LocalDate.of(2026,2,28),result.toLocalDate())}
 @Test fun recurringTaskSkipsMissedOccurrences(){val now=System.currentTimeMillis();val result=nextRecurrence(now-20*86400000,"Weekly",now);assertTrue(result>now);assertTrue(result<=now+7*86400000)}
}
