/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scheduling.support;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Before;
import org.junit.Test;
import org.springframework.scheduling.TriggerContext;

/**
 * @author Dave Syer
 * @author Mark Fisher
 */
public class CronTriggerTests {

	private Calendar calendar = new GregorianCalendar();

	private Date date = new Date();


	/**
	 * @param calendar
	 */
	private void roundup(Calendar calendar) {
		calendar.add(Calendar.SECOND, 1);
		calendar.set(Calendar.MILLISECOND, 0);
	}


	@Before
	public void setUp() {
		calendar.setTime(date);
		roundup(calendar);
	}

	@Test
	public void testMatchAll() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * *");
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testMatchLastSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("* * * * * *");
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.SECOND, 58);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	public void testMatchSpecificSecond() throws Exception {
		CronTrigger trigger = new CronTrigger("10 * * * * *");
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.SECOND, 9);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	public void testIncrementSecondByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("11 * * * * *");
		calendar.set(Calendar.SECOND, 10);
		Date date = calendar.getTime();
		calendar.add(Calendar.SECOND, 1);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementSecondAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("10 * * * * *");
		calendar.set(Calendar.SECOND, 11);
		Date date = calendar.getTime();
		calendar.add(Calendar.SECOND, 59);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testSecondRange() throws Exception {
		CronTrigger trigger = new CronTrigger("10-15 * * * * *");
		calendar.set(Calendar.SECOND, 9);
		assertMatchesNextSecond(trigger, calendar);
		calendar.set(Calendar.SECOND, 14);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	public void testIncrementMinuteByOne() throws Exception {
		CronTrigger trigger = new CronTrigger("0 11 * * * *");
		calendar.set(Calendar.MINUTE, 10);
		Date date = calendar.getTime();
		calendar.add(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementMinute() throws Exception {
		CronTrigger trigger = new CronTrigger("0 * * * * *");
		calendar.set(Calendar.MINUTE, 10);
		Date date = calendar.getTime();
		calendar.add(Calendar.MINUTE, 1);
		calendar.set(Calendar.SECOND, 0);
		TriggerContext context1 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.add(Calendar.MINUTE, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test
	public void testIncrementMinuteAndRollover() throws Exception {
		CronTrigger trigger = new CronTrigger("0 10 * * * *");
		calendar.set(Calendar.MINUTE, 11);
		calendar.set(Calendar.SECOND, 0);
		Date date = calendar.getTime();
		calendar.add(Calendar.MINUTE, 59);
		TriggerContext context = getTriggerContext(date);
		assertEquals(calendar.getTime(), trigger.nextExecutionTime(context));
	}

	@Test
	public void testIncrementHour() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 * * * *");
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
	public void testIncrementDayOfMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *");
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
		CronTrigger trigger = new CronTrigger("* * * 10 * *");
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
		CronTrigger trigger = new CronTrigger("* * * 10 * *");
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
		CronTrigger trigger = new CronTrigger("0 0 0 * * *");
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
		CronTrigger trigger = new CronTrigger("0 0 0 * * *");
		calendar.set(Calendar.MONTH, 9); // October: 31 days
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
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *");
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
	public void testMonthlyTriggerInLongMonth() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 0 31 * *");
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
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *");
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
		CronTrigger trigger = new CronTrigger("* * * * * 2");
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
		CronTrigger trigger = new CronTrigger("* * * * * 2");
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
		CronTrigger trigger = new CronTrigger("55 5 * * * *");
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
		CronTrigger trigger = new CronTrigger("55 * 2 * * *");
		calendar.set(Calendar.HOUR_OF_DAY, 1);
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
		CronTrigger trigger = new CronTrigger("* 5 10 * * *");
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
		CronTrigger trigger = new CronTrigger("55 * * 3 * *");
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
		CronTrigger trigger = new CronTrigger("* * * 3 10 *");
		calendar.set(Calendar.DAY_OF_MONTH, 2);
		calendar.set(Calendar.MONTH, 10);
		Date date = calendar.getTime();
		TriggerContext context1 = getTriggerContext(date);
		calendar.add(Calendar.DAY_OF_MONTH, 1);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context1));
		calendar.add(Calendar.SECOND, 1);
		TriggerContext context2 = getTriggerContext(date);
		assertEquals(calendar.getTime(), date = trigger.nextExecutionTime(context2));
	}

	@Test
	public void testWeekDaySequence() throws Exception {
		CronTrigger trigger = new CronTrigger("0 0 7 ? * MON-FRI");
		// This is a Saturday
		calendar.set(2009, 8, 26);
		date = calendar.getTime();
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
		CronTrigger trigger1 = new CronTrigger("* * * 2 * *");
		CronTrigger trigger2 = new CronTrigger("* * * 2 * ?");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSecondIncrementer() throws Exception {
		CronTrigger trigger1 = new CronTrigger("57,59 * * * * *");
		CronTrigger trigger2 = new CronTrigger("57/2 * * * * *");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSecondIncrementerWithRange() throws Exception {
		CronTrigger trigger1 = new CronTrigger("1,3,5 * * * * *");
		CronTrigger trigger2 = new CronTrigger("1-6/2 * * * * *");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testHourIncrementer() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * 4,8,12,16,20 * * *");
		CronTrigger trigger2 = new CronTrigger("* * 4/4 * * *");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testDayNames() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0-6");
		CronTrigger trigger2 = new CronTrigger("* * * * * TUE,WED,THU,FRI,SAT,SUN,MON");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSundayIsZero() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0");
		CronTrigger trigger2 = new CronTrigger("* * * * * SUN");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testSundaySynonym() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0");
		CronTrigger trigger2 = new CronTrigger("* * * * * 7");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testMonthNames() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * 0-11 *");
		CronTrigger trigger2 = new CronTrigger("* * * * FEB,JAN,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC *");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testMonthNamesMixedCase() throws Exception {
		CronTrigger trigger1 = new CronTrigger("* * * * 1 *");
		CronTrigger trigger2 = new CronTrigger("* * * * Feb *");
		assertEquals(trigger1, trigger2);
	}

	@Test
	public void testWhitespace() throws Exception {
		CronTrigger trigger1 = new CronTrigger("*  *  * *  1 *");
		CronTrigger trigger2 = new CronTrigger("* * * * 1 *");
		assertEquals(trigger1, trigger2);
	}

	/**
	 * @param trigger
	 * @param calendar
	 */
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
