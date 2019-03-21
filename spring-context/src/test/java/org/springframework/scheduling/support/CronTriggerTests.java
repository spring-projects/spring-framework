/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scheduling.support;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.scheduling.TriggerContext;

import static org.junit.Assert.*;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
@RunWith(Parameterized.class)
public class CronTriggerTests {

	private final Calendar calendar = new GregorianCalendar();

	private final Date date;

	private final TimeZone timeZone;


	public CronTriggerTests(Date date, TimeZone timeZone) {
		this.date = date;
		this.timeZone = timeZone;
	}

	@Parameters(name = "date [{0}], time zone [{1}]")
	public static List<Object[]> getParameters() {
		List<Object[]> list = new ArrayList<>();
		list.add(new Object[] { new Date(), TimeZone.getTimeZone("PST") });
		list.add(new Object[] { new Date(), TimeZone.getTimeZone("CET") });
		return list;
	}

	private void roundup(Calendar calendar) {
		calendar.add(Calendar.SECOND, 1);
		calendar.set(Calendar.MILLISECOND, 0);
	}


	@Before
	public void setUp() {
		calendar.setTimeZone(timeZone);
		calendar.setTime(date);
		roundup(calendar);
	}

	@Test
	public void testMatchAll() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * *", timeZone);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testMatchLastSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * *", timeZone);
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.SECOND, 58);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	public void testMatchSpecificSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("10 * * * * *", timeZone);
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.SECOND, 9);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	public void testIncrementSecondByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("11 * * * * *", timeZone);
		calendar.set(Calendar.SECOND, 10);
		Date date = calendar.getTime();
		calendar.add(Calendar.SECOND, 1);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementSecondWithPreviousExecutionTooEarly() throws Exception {
		CronTrigger trigger = new CronTrigger("11 * * * * *", timeZone);
		calendar.set(Calendar.SECOND, 11);
		SimpleTriggerContext context = new SimpleTriggerContext();
		context.update(calendar.getTime(), new Date(calendar.getTimeInMillis() - 100),
				new Date(calendar.getTimeInMillis() - 90));
		calendar.add(Calendar.MINUTE, 1);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementSecondAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("10 * * * * *", timeZone);
		calendar.set(Calendar.SECOND, 11);
		Date date = calendar.getTime();
		calendar.add(Calendar.SECOND, 59);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testSecondRange() throws Exception {
		CronTrigger trigger = new CronTrigger("10-15 * * * * *", timeZone);
		calendar.set(Calendar.SECOND, 9);
		assertMatchesNextSecond(trigger, calendar);
		calendar.set(Calendar.SECOND, 14);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	public void testIncrementMinute() throws Exception {
		CronTrigger trigger = new CronTrigger("0 * * * * *", timeZone);
		calendar.set(Calendar.MINUTE, 10);
		Date date = calendar.getTime();
		calendar.add(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		TriggerContext context1 = getTriggerContext(date);
		date = trigger.nextExecutionTime(context1);
		assertEquals(calendar.getTime(), date);
		calendar.add(Calendar.MINUTE, 1);
		TriggerContext context2 = getTriggerContext(date);
		date = trigger.nextExecutionTime(context2);
		assertEquals(calendar.getTime(), date);
	}

	@Test
	public void testIncrementMinuteByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("0 11 * * * *", timeZone);
		calendar.set(Calendar.MINUTE, 10);
		TriggerContext context = getTriggerContext(calendar.getTime());
		calendar.add(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementMinuteAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("0 10 * * * *", timeZone);
		calendar.set(Calendar.MINUTE, 11);
		calendar.set(Calendar.SECOND, 0);
		Date date = calendar.getTime();
		calendar.add(Calendar.MINUTE, 59);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementHour() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 * * * *", timeZone);
		calendar.set(Calendar.MONTH, 9);
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		calendar.set(Calendar.HOUR_OF_DAY, 11);
		calendar.set(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		Date date = calendar.getTime();
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.HOUR_OF_DAY, 12);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.set(Calendar.HOUR_OF_DAY, 13);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testIncrementHourAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 * * * *", timeZone);
		calendar.set(Calendar.MONTH, 9);
		calendar.set(Calendar.DAY_OF_MONTH, 10);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		Date date = calendar.getTime();
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 11);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.set(Calendar.HOUR_OF_DAY, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testIncrementDayOfMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *", timeZone);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		Date date = calendar.getTime();
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		assertEquals(2, calendar.get(Calendar.DAY_OF_MONTH));
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context2));
		assertEquals(3, calendar.get(Calendar.DAY_OF_MONTH));
	}

	@Test
	public void testIncrementDayOfMonthByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * 10 * *", timeZone);
		calendar.set(Calendar.DAY_OF_MONTH, 9);
		Date date = calendar.getTime();
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementDayOfMonthAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * 10 * *", timeZone);
		calendar.set(Calendar.DAY_OF_MONTH, 11);
		Date date = calendar.getTime();
		calendar.add(Calendar.MONTH, 1);
		calendar.set(Calendar.DAY_OF_MONTH, 10);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testDailyTriggerInShortMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *", timeZone);
		calendar.set(Calendar.MONTH, 8); // September: 30 days
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = calendar.getTime();
		calendar.set(Calendar.MONTH, 9); // October
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.set(Calendar.DAY_OF_MONTH, 2);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testDailyTriggerInLongMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *", timeZone);
		calendar.set(Calendar.MONTH, 7); // August: 31 days and not a daylight saving boundary
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = calendar.getTime();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 31);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.set(Calendar.MONTH, 8); // September
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testDailyTriggerOnDaylightSavingBoundary() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *", timeZone);
		calendar.set(Calendar.MONTH, 9); // October: 31 days and a daylight saving boundary in CET
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = calendar.getTime();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.DAY_OF_MONTH, 31);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.set(Calendar.MONTH, 10); // November
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testIncrementMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *", timeZone);
		calendar.set(Calendar.MONTH, 9);
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = calendar.getTime();
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MONTH, 10);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.set(Calendar.MONTH, 11);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testIncrementMonthAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *", timeZone);
		calendar.set(Calendar.MONTH, 11);
		calendar.set(Calendar.DAY_OF_MONTH, 31);
		calendar.set(Calendar.YEAR, 2010);
		Date date = calendar.getTime();
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MONTH, 0);
		calendar.set(Calendar.YEAR, 2011);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.set(Calendar.MONTH, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context2));
	}

	@Test
	public void testMonthlyTriggerInLongMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 31 * *", timeZone);
		calendar.set(Calendar.MONTH, 9);
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = calendar.getTime();
		calendar.set(Calendar.DAY_OF_MONTH, 31);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testMonthlyTriggerInShortMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *", timeZone);
		calendar.set(Calendar.MONTH, 9);
		calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date date = calendar.getTime();
		calendar.set(Calendar.MONTH, 10);
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementDayOfWeekByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * 2", timeZone);
		calendar.set(Calendar.DAY_OF_WEEK, 2);
		Date date = calendar.getTime();
		calendar.add(Calendar.DAY_OF_WEEK, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
		assertEquals(Calendar.TUESDAY, calendar.get(Calendar.DAY_OF_WEEK));
	}

	@Test
	public void testIncrementDayOfWeekAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * 2", timeZone);
		calendar.set(Calendar.DAY_OF_WEEK, 4);
		Date date = calendar.getTime();
		calendar.add(Calendar.DAY_OF_MONTH, 6);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
		assertEquals(Calendar.TUESDAY, calendar.get(Calendar.DAY_OF_WEEK));
	}

	@Test
	public void testSpecificMinuteSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("55 5 * * * *", timeZone);
		calendar.set(Calendar.MINUTE, 4);
		calendar.set(Calendar.SECOND, 54);
		Date date = calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		calendar.add(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 55);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.add(Calendar.HOUR, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test
	public void testSpecificHourSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("55 * 10 * * *", timeZone);
		calendar.set(Calendar.HOUR_OF_DAY, 9);
		calendar.set(Calendar.SECOND, 54);
		Date date = calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 55);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.add(Calendar.MINUTE, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test
	public void testSpecificMinuteHour() throws Exception {
		CronTrigger trigger = new CronTrigger("* 5 10 * * *", timeZone);
		calendar.set(Calendar.MINUTE, 4);
		calendar.set(Calendar.HOUR_OF_DAY, 9);
		Date date = calendar.getTime();
		calendar.add(Calendar.MINUTE, 1);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		calendar.set(Calendar.SECOND, 0);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		// next trigger is in one second because second is wildcard
		calendar.add(Calendar.SECOND, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test
	public void testSpecificDayOfMonthSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("55 * * 3 * *", timeZone);
		calendar.set(Calendar.DAY_OF_MONTH, 2);
		calendar.set(Calendar.SECOND, 54);
		Date date = calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 55);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.add(Calendar.MINUTE, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test
	public void testSpecificDate() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * 3 11 *", timeZone);
		calendar.set(Calendar.DAY_OF_MONTH, 2);
		calendar.set(Calendar.MONTH, 9);
		Date date = calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MONTH, 10); // 10=November
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.add(Calendar.SECOND, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonExistentSpecificDate() throws Exception {
		// TODO: maybe try and detect this as a special case in parser?
		CronTrigger trigger = new CronTrigger("0 0 0 31 6 *", timeZone);
		calendar.set(Calendar.DAY_OF_MONTH, 10);
		calendar.set(Calendar.MONTH, 2);
		Date date = calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		trigger.nextExecutionTime(context1);
		// new CronTrigger("0 0 0 30 1 ?", timeZone);
	}

	@Test
	public void testLeapYearSpecificDate() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 29 2 *", timeZone);
		calendar.set(Calendar.YEAR, 2007);
		calendar.set(Calendar.DAY_OF_MONTH, 10);
		calendar.set(Calendar.MONTH, 1); // 2=February
		Date date = calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		calendar.set(Calendar.YEAR, 2008);
		calendar.set(Calendar.DAY_OF_MONTH, 29);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.add(Calendar.YEAR, 4);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test
	public void testWeekDaySequence() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 7 ? * MON-FRI", timeZone);
		// This is a Saturday
		calendar.set(2009, 8, 26);
		Date date = calendar.getTime();
		// 7 am is the trigger time
		calendar.set(Calendar.HOUR_OF_DAY, 7);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		// Add two days because we start on Saturday
		calendar.add(Calendar.DAY_OF_MONTH, 2);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		// Next day is a week day so add one
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context2));
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context3 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context3));
	}

	@Test
	public void testDayOfWeekIndifferent() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * 2 * *", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * 2 * ?", timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSecondIncrementer() throws Exception {
		CronTrigger trigger1 = new CronTrigger("57,59 * * * * *", timeZone);
		CronTrigger trigger2 = new CronTrigger("57/2 * * * * *", timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSecondIncrementerWithRange() throws Exception {
		CronTrigger trigger1 = new CronTrigger("1,3,5 * * * * *", timeZone);
		CronTrigger trigger2 = new CronTrigger("1-6/2 * * * * *", timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testHourIncrementer() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * 4,8,12,16,20 * * *", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * 4/4 * * *", timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testDayNames() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0-6", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * * TUE,WED,THU,FRI,SAT,SUN,MON", timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSundayIsZero() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * * SUN", timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSundaySynonym() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * * 7", timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testMonthNames() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * 1-12 *", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * FEB,JAN,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC *", timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testMonthNamesMixedCase() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * 2 *", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * Feb *", timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSecondInvalid() throws Exception {
		new CronTrigger("77 * * * * *", timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSecondRangeInvalid() throws Exception {
		new CronTrigger("44-77 * * * * *", timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMinuteInvalid() throws Exception {
		new CronTrigger("* 77 * * * *", timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMinuteRangeInvalid() throws Exception {
		new CronTrigger("* 44-77 * * * *", timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHourInvalid() throws Exception {
		new CronTrigger("* * 27 * * *", timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHourRangeInvalid() throws Exception {
		new CronTrigger("* * 23-28 * * *", timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDayInvalid() throws Exception {
		new CronTrigger("* * * 45 * *", timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDayRangeInvalid() throws Exception {
		new CronTrigger("* * * 28-45 * *", timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMonthInvalid() throws Exception {
		new CronTrigger("0 0 0 25 13 ?", timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMonthInvalidTooSmall() throws Exception {
		new CronTrigger("0 0 0 25 0 ?", timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDayOfMonthInvalid() throws Exception {
		new CronTrigger("0 0 0 32 12 ?", timeZone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMonthRangeInvalid() throws Exception {
		new CronTrigger("* * * * 11-13 *", timeZone);
	}

	@Test
	public void testWhitespace() throws Exception {
		CronTrigger trigger1 = new CronTrigger("*  *  * *  1 *", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * 1 *", timeZone);
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testMonthSequence() throws Exception {
		CronTrigger trigger = new CronTrigger("0 30 23 30 1/3 ?", timeZone);
		calendar.set(2010, 11, 30);
		Date date = calendar.getTime();
		// set expected next trigger time
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 30);
		calendar.set(Calendar.SECOND, 0);
		calendar.add(Calendar.MONTH, 1);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		// Next trigger is 3 months latter
		calendar.add(Calendar.MONTH, 3);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context2));
		// Next trigger is 3 months latter
		calendar.add(Calendar.MONTH, 3);
		TriggerContext context3 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context3));
	}

	@Test
	public void testDaylightSavingMissingHour() throws Exception {
		// This trigger has to be somewhere in between 2am and 3am
		CronTrigger trigger = new CronTrigger("0 10 2 * * *", timeZone);
		calendar.set(Calendar.DAY_OF_MONTH, 31);
		calendar.set(Calendar.MONTH, Calendar.MARCH);
		calendar.set(Calendar.YEAR, 2013);
		calendar.set(Calendar.HOUR_OF_DAY, 1);
		calendar.set(Calendar.SECOND, 54);
		Date date = calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		if (timeZone.equals(TimeZone.getTimeZone("CET"))) {
			// Clocks go forward an hour so 2am doesn't exist in CET for this date
			calendar.add(Calendar.DAY_OF_MONTH, 1);
		}
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		calendar.set(Calendar.MINUTE, 10);
		calendar.set(Calendar.SECOND, 0);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
	}

	private void assertMatchesNextSecond(CronTrigger trigger, Calendar calendar) {
		Date date = calendar.getTime();
		roundup(calendar);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	private static TriggerContext getTriggerContext(Date lastCompletionTime) {
		SimpleTriggerContext context = new SimpleTriggerContext();
		context.update(null, null, lastCompletionTime);
		return context;
	}

}
