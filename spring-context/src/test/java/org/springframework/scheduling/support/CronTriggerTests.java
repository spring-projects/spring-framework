/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import org.springframework.scheduling.TriggerContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link CronTrigger}.
 *
 * @author Dave Syer
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
@ParameterizedClass
@FieldSource("parameters")
@SuppressWarnings("deprecation")
class CronTriggerTests {

	static List<Arguments> parameters = List.of(
			arguments(new Date(), TimeZone.getTimeZone("PST")),
			arguments(new Date(), TimeZone.getTimeZone("CET")));


	private final Calendar calendar = new GregorianCalendar();
	private final Date localDateTime;
	private final TimeZone timeZone;


	CronTriggerTests(Date localDateTime, TimeZone timeZone) {
		this.calendar.setTime(localDateTime);
		this.calendar.setTimeZone(timeZone);
		roundup(this.calendar);
		this.localDateTime = localDateTime;
		this.timeZone = timeZone;
	}


	@Test
	void matchAll() {
		CronTrigger trigger = new CronTrigger("* * * * * *", timeZone);
		TriggerContext context = getTriggerContext(localDateTime);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void matchLastSecond() {
		CronTrigger trigger = new CronTrigger("* * * * * *", timeZone);
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.SECOND, 58);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	void matchSpecificSecond() {
		CronTrigger trigger = new CronTrigger("10 * * * * *", timeZone);
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.SECOND, 9);
		assertMatchesNextSecond(trigger, calendar);
	}

	@Test
	void incrementSecondByOne() {
		CronTrigger trigger = new CronTrigger("11 * * * * *", timeZone);
		this.calendar.set(Calendar.SECOND, 10);
		Date localDate = this.calendar.getTime();
		this.calendar.add(Calendar.SECOND, 1);
		TriggerContext context = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void incrementSecondWithPreviousExecutionTooEarly() {
		CronTrigger trigger = new CronTrigger("11 * * * * *", timeZone);
		this.calendar.set(Calendar.SECOND, 11);
		SimpleTriggerContext context = new SimpleTriggerContext();
		context.update(this.calendar.getTime(), new Date(this.calendar.getTimeInMillis() - 100),
				new Date(this.calendar.getTimeInMillis() - 90));
		this.calendar.add(Calendar.MINUTE, 1);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void incrementSecondAndRollover() {
		CronTrigger trigger = new CronTrigger("10 * * * * *", timeZone);
		this.calendar.set(Calendar.SECOND, 11);
		Date localDate = this.calendar.getTime();
		this.calendar.add(Calendar.SECOND, 59);
		TriggerContext context = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void secondRange() {
		CronTrigger trigger = new CronTrigger("10-15 * * * * *", timeZone);
		this.calendar.set(Calendar.SECOND, 9);
		assertMatchesNextSecond(trigger, this.calendar);
		this.calendar.set(Calendar.SECOND, 14);
		assertMatchesNextSecond(trigger, this.calendar);
	}

	@Test
	void incrementMinute() {
		CronTrigger trigger = new CronTrigger("0 * * * * *", timeZone);
		this.calendar.set(Calendar.MINUTE, 10);
		Date localDate = this.calendar.getTime();
		this.calendar.add(Calendar.MINUTE, 1);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context1 = getTriggerContext(localDate);
		localDate = trigger.nextExecutionTime(context1);
		assertThat(localDate).isEqualTo(this.calendar.getTime());
		this.calendar.add(Calendar.MINUTE, 1);
		TriggerContext context2 = getTriggerContext(localDate);
		localDate = trigger.nextExecutionTime(context2);
		assertThat(localDate).isEqualTo(this.calendar.getTime());
	}

	@Test
	void incrementMinuteByOne() {
		CronTrigger trigger = new CronTrigger("0 11 * * * *", timeZone);
		this.calendar.set(Calendar.MINUTE, 10);
		TriggerContext context = getTriggerContext(this.calendar.getTime());
		this.calendar.add(Calendar.MINUTE, 1);
		this.calendar.set(Calendar.SECOND, 0);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void incrementMinuteAndRollover() {
		CronTrigger trigger = new CronTrigger("0 10 * * * *", timeZone);
		this.calendar.set(Calendar.MINUTE, 11);
		this.calendar.set(Calendar.SECOND, 0);
		Date localDate = this.calendar.getTime();
		this.calendar.add(Calendar.MINUTE, 59);
		TriggerContext context = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void incrementHour() {
		CronTrigger trigger = new CronTrigger("0 0 * * * *", timeZone);
		this.calendar.set(Calendar.MONTH, 9);
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		this.calendar.set(Calendar.HOUR_OF_DAY, 11);
		this.calendar.set(Calendar.MINUTE, 1);
		this.calendar.set(Calendar.SECOND, 0);
		Date localDate = this.calendar.getTime();
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.HOUR_OF_DAY, 12);
		TriggerContext context1 = getTriggerContext(localDate);
		Object actual = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual).isEqualTo(this.calendar.getTime());
		this.calendar.set(Calendar.HOUR_OF_DAY, 13);
		TriggerContext context2 = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context2)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void incrementHourAndRollover() {
		CronTrigger trigger = new CronTrigger("0 0 * * * *", timeZone);
		this.calendar.set(Calendar.MONTH, 9);
		this.calendar.set(Calendar.DAY_OF_MONTH, 10);
		this.calendar.set(Calendar.HOUR_OF_DAY, 23);
		this.calendar.set(Calendar.MINUTE, 1);
		this.calendar.set(Calendar.SECOND, 0);
		Date localDate = this.calendar.getTime();
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.DAY_OF_MONTH, 11);
		TriggerContext context1 = getTriggerContext(localDate);
		Object actual = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual).isEqualTo(this.calendar.getTime());
		this.calendar.set(Calendar.HOUR_OF_DAY, 1);
		TriggerContext context2 = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context2)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void incrementDayOfMonth() {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *", timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		Date localDate = this.calendar.getTime();
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context1 = getTriggerContext(localDate);
		Object actual1 = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual1).isEqualTo(this.calendar.getTime());
		assertThat(this.calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(2);
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context2 = getTriggerContext(localDate);
		Object actual = trigger.nextExecutionTime(context2);
		assertThat(actual).isEqualTo(this.calendar.getTime());
		assertThat(this.calendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(3);
	}

	@Test
	void incrementDayOfMonthByOne() {
		CronTrigger trigger = new CronTrigger("* * * 10 * *", timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 9);
		Date localDate = this.calendar.getTime();
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void incrementDayOfMonthAndRollover() {
		CronTrigger trigger = new CronTrigger("* * * 10 * *", timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 11);
		Date localDate = this.calendar.getTime();
		this.calendar.add(Calendar.MONTH, 1);
		this.calendar.set(Calendar.DAY_OF_MONTH, 10);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void dailyTriggerInShortMonth() {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *", timeZone);
		this.calendar.set(Calendar.MONTH, 8); // September: 30 days
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date localDate = this.calendar.getTime();
		this.calendar.set(Calendar.MONTH, 9); // October
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context1 = getTriggerContext(localDate);
		Object actual = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual).isEqualTo(this.calendar.getTime());
		this.calendar.set(Calendar.DAY_OF_MONTH, 2);
		TriggerContext context2 = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context2)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void dailyTriggerInLongMonth() {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *", timeZone);
		this.calendar.set(Calendar.MONTH, 7); // August: 31 days and not a daylight saving boundary
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date localDate = this.calendar.getTime();
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		this.calendar.set(Calendar.DAY_OF_MONTH, 31);
		TriggerContext context1 = getTriggerContext(localDate);
		Object actual = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual).isEqualTo(this.calendar.getTime());
		this.calendar.set(Calendar.MONTH, 8); // September
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context2 = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context2)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void dailyTriggerOnDaylightSavingBoundary() {
		CronTrigger trigger = new CronTrigger("0 0 0 * * *", timeZone);
		this.calendar.set(Calendar.MONTH, 9); // October: 31 days and a daylight saving boundary in CET
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date localDate = this.calendar.getTime();
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		this.calendar.set(Calendar.DAY_OF_MONTH, 31);
		TriggerContext context1 = getTriggerContext(localDate);
		Object actual = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual).isEqualTo(this.calendar.getTime());
		this.calendar.set(Calendar.MONTH, 10); // November
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context2 = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context2)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void incrementMonth() {
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *", timeZone);
		this.calendar.set(Calendar.MONTH, 9);
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date localDate = this.calendar.getTime();
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		this.calendar.set(Calendar.MONTH, 10);
		TriggerContext context1 = getTriggerContext(localDate);
		Object actual = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual).isEqualTo(this.calendar.getTime());
		this.calendar.set(Calendar.MONTH, 11);
		TriggerContext context2 = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context2)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void incrementMonthAndRollover() {
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *", timeZone);
		this.calendar.set(Calendar.MONTH, 11);
		this.calendar.set(Calendar.DAY_OF_MONTH, 31);
		this.calendar.set(Calendar.YEAR, 2010);
		Date localDate = this.calendar.getTime();
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		this.calendar.set(Calendar.MONTH, 0);
		this.calendar.set(Calendar.YEAR, 2011);
		TriggerContext context1 = getTriggerContext(localDate);
		Object actual = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual).isEqualTo(this.calendar.getTime());
		this.calendar.set(Calendar.MONTH, 1);
		TriggerContext context2 = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context2)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void monthlyTriggerInLongMonth() {
		CronTrigger trigger = new CronTrigger("0 0 0 31 * *", timeZone);
		this.calendar.set(Calendar.MONTH, 9);
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date localDate = this.calendar.getTime();
		this.calendar.set(Calendar.DAY_OF_MONTH, 31);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void monthlyTriggerInShortMonth() {
		CronTrigger trigger = new CronTrigger("0 0 0 1 * *", timeZone);
		this.calendar.set(Calendar.MONTH, 9);
		this.calendar.set(Calendar.DAY_OF_MONTH, 30);
		Date localDate = this.calendar.getTime();
		this.calendar.set(Calendar.MONTH, 10);
		this.calendar.set(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(this.calendar.getTime());
	}

	@Test
	void incrementDayOfWeekByOne() {
		CronTrigger trigger = new CronTrigger("* * * * * 2", timeZone);
		this.calendar.set(Calendar.DAY_OF_WEEK, 2);
		Date localDate = this.calendar.getTime();
		this.calendar.add(Calendar.DAY_OF_WEEK, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(this.calendar.getTime());
		assertThat(this.calendar.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.TUESDAY);
	}

	@Test
	void incrementDayOfWeekAndRollover() {
		CronTrigger trigger = new CronTrigger("* * * * * 2", timeZone);
		this.calendar.set(Calendar.DAY_OF_WEEK, 4);
		Date localDate = this.calendar.getTime();
		this.calendar.add(Calendar.DAY_OF_MONTH, 6);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(this.calendar.getTime());
		assertThat(this.calendar.get(Calendar.DAY_OF_WEEK)).isEqualTo(Calendar.TUESDAY);
	}

	@Test
	void specificMinuteSecond() {
		CronTrigger trigger = new CronTrigger("55 5 * * * *", timeZone);
		this.calendar.set(Calendar.MINUTE, 4);
		this.calendar.set(Calendar.SECOND, 54);
		Date localDate = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(localDate);
		this.calendar.add(Calendar.MINUTE, 1);
		this.calendar.set(Calendar.SECOND, 55);
		Object actual1 = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual1).isEqualTo(this.calendar.getTime());
		this.calendar.add(Calendar.HOUR, 1);
		TriggerContext context2 = getTriggerContext(localDate);
		Object actual = trigger.nextExecutionTime(context2);
		assertThat(actual).isEqualTo(this.calendar.getTime());
	}

	@Test
	void specificHourSecond() {
		CronTrigger trigger = new CronTrigger("55 * 10 * * *", timeZone);
		this.calendar.set(Calendar.HOUR_OF_DAY, 9);
		this.calendar.set(Calendar.SECOND, 54);
		Date localDate = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(localDate);
		this.calendar.add(Calendar.HOUR_OF_DAY, 1);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 55);
		Object actual1 = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual1).isEqualTo(this.calendar.getTime());
		this.calendar.add(Calendar.MINUTE, 1);
		TriggerContext context2 = getTriggerContext(localDate);
		Object actual = trigger.nextExecutionTime(context2);
		assertThat(actual).isEqualTo(this.calendar.getTime());
	}

	@Test
	void specificMinuteHour() {
		CronTrigger trigger = new CronTrigger("* 5 10 * * *", timeZone);
		this.calendar.set(Calendar.MINUTE, 4);
		this.calendar.set(Calendar.HOUR_OF_DAY, 9);
		Date localDate = this.calendar.getTime();
		this.calendar.add(Calendar.MINUTE, 1);
		this.calendar.add(Calendar.HOUR_OF_DAY, 1);
		this.calendar.set(Calendar.SECOND, 0);
		TriggerContext context1 = getTriggerContext(localDate);
		Object actual1 = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual1).isEqualTo(this.calendar.getTime());
		// next trigger is in one second because second is wildcard
		this.calendar.add(Calendar.SECOND, 1);
		TriggerContext context2 = getTriggerContext(localDate);
		Object actual = trigger.nextExecutionTime(context2);
		assertThat(actual).isEqualTo(this.calendar.getTime());
	}

	@Test
	void specificDayOfMonthSecond() {
		CronTrigger trigger = new CronTrigger("55 * * 3 * *", timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 2);
		this.calendar.set(Calendar.SECOND, 54);
		Date localDate = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(localDate);
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 55);
		Object actual1 = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual1).isEqualTo(this.calendar.getTime());
		this.calendar.add(Calendar.MINUTE, 1);
		TriggerContext context2 = getTriggerContext(localDate);
		Object actual = trigger.nextExecutionTime(context2);
		assertThat(actual).isEqualTo(this.calendar.getTime());
	}

	@Test
	void specificDate() {
		CronTrigger trigger = new CronTrigger("* * * 3 11 *", timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 2);
		this.calendar.set(Calendar.MONTH, 9);
		Date localDate = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(localDate);
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MONTH, 10); // 10=November
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		Object actual1 = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual1).isEqualTo(this.calendar.getTime());
		this.calendar.add(Calendar.SECOND, 1);
		TriggerContext context2 = getTriggerContext(localDate);
		Object actual = trigger.nextExecutionTime(context2);
		assertThat(actual).isEqualTo(this.calendar.getTime());
	}

	@Test
	void nonExistentSpecificDate() {
		// TODO: maybe try and detect this as a special case in parser?
		CronTrigger trigger = new CronTrigger("0 0 0 31 6 *", timeZone);
		this.calendar.set(Calendar.DAY_OF_MONTH, 10);
		this.calendar.set(Calendar.MONTH, 2);
		Date localDate = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(localDate);
		assertThat(trigger.nextExecutionTime(context1)).isNull();
	}

	@Test
	void leapYearSpecificDate() {
		CronTrigger trigger = new CronTrigger("0 0 0 29 2 *", timeZone);
		this.calendar.set(Calendar.YEAR, 2007);
		this.calendar.set(Calendar.DAY_OF_MONTH, 10);
		this.calendar.set(Calendar.MONTH, 1); // 2=February
		Date localDate = this.calendar.getTime();
		TriggerContext context1 = getTriggerContext(localDate);
		this.calendar.set(Calendar.YEAR, 2008);
		this.calendar.set(Calendar.DAY_OF_MONTH, 29);
		this.calendar.set(Calendar.HOUR_OF_DAY, 0);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		Object actual1 = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual1).isEqualTo(this.calendar.getTime());
		this.calendar.add(Calendar.YEAR, 4);
		TriggerContext context2 = getTriggerContext(localDate);
		Object actual = trigger.nextExecutionTime(context2);
		assertThat(actual).isEqualTo(this.calendar.getTime());
	}

	@Test
	void weekDaySequence() {
		CronTrigger trigger = new CronTrigger("0 0 7 ? * MON-FRI", timeZone);
		// This is a Saturday
		this.calendar.set(2009, 8, 26);
		Date localDate = this.calendar.getTime();
		// 7 am is the trigger time
		this.calendar.set(Calendar.HOUR_OF_DAY, 7);
		this.calendar.set(Calendar.MINUTE, 0);
		this.calendar.set(Calendar.SECOND, 0);
		// Add two days because we start on Saturday
		this.calendar.add(Calendar.DAY_OF_MONTH, 2);
		TriggerContext context1 = getTriggerContext(localDate);
		Object actual2 = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual2).isEqualTo(this.calendar.getTime());
		// Next day is a week day so add one
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context2 = getTriggerContext(localDate);
		Object actual1 = localDate = trigger.nextExecutionTime(context2);
		assertThat(actual1).isEqualTo(this.calendar.getTime());
		this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		TriggerContext context3 = getTriggerContext(localDate);
		Object actual = trigger.nextExecutionTime(context3);
		assertThat(actual).isEqualTo(this.calendar.getTime());
	}

	@Test
	void dayOfWeekIndifferent() {
		CronTrigger trigger1 = new CronTrigger("* * * 2 * *", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * 2 * ?", timeZone);
		assertThat(trigger2).isEqualTo(trigger1);
	}

	@Test
	void secondIncrementer() {
		CronTrigger trigger1 = new CronTrigger("57,59 * * * * *", timeZone);
		CronTrigger trigger2 = new CronTrigger("57/2 * * * * *", timeZone);
		assertThat(trigger2).isEqualTo(trigger1);
	}

	@Test
	void secondIncrementerWithRange() {
		CronTrigger trigger1 = new CronTrigger("1,3,5 * * * * *", timeZone);
		CronTrigger trigger2 = new CronTrigger("1-6/2 * * * * *", timeZone);
		assertThat(trigger2).isEqualTo(trigger1);
	}

	@Test
	void hourIncrementer() {
		CronTrigger trigger1 = new CronTrigger("* * 4,8,12,16,20 * * *", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * 4/4 * * *", timeZone);
		assertThat(trigger2).isEqualTo(trigger1);
	}

	@Test
	void dayNames() {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0-6", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * * TUE,WED,THU,FRI,SAT,SUN,MON", timeZone);
		assertThat(trigger2).isEqualTo(trigger1);
	}

	@Test
	void sundayIsZero() {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * * SUN", timeZone);
		assertThat(trigger2).isEqualTo(trigger1);
	}

	@Test
	void sundaySynonym() {
		CronTrigger trigger1 = new CronTrigger("* * * * * 0", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * * 7", timeZone);
		assertThat(trigger2).isEqualTo(trigger1);
	}

	@Test
	void monthNames() {
		CronTrigger trigger1 = new CronTrigger("* * * * 1-12 *", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * FEB,JAN,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC *", timeZone);
		assertThat(trigger2).isEqualTo(trigger1);
	}

	@Test
	void monthNamesMixedCase() {
		CronTrigger trigger1 = new CronTrigger("* * * * 2 *", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * Feb *", timeZone);
		assertThat(trigger2).isEqualTo(trigger1);
	}

	@Test
	void secondInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CronTrigger("77 * * * * *", timeZone));
	}

	@Test
	void secondRangeInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CronTrigger("44-77 * * * * *", timeZone));
	}

	@Test
	void minuteInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CronTrigger("* 77 * * * *", timeZone));
	}

	@Test
	void minuteRangeInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CronTrigger("* 44-77 * * * *", timeZone));
	}

	@Test
	void hourInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CronTrigger("* * 27 * * *", timeZone));
	}

	@Test
	void hourRangeInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CronTrigger("* * 23-28 * * *", timeZone));
	}

	@Test
	void dayInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CronTrigger("* * * 45 * *", timeZone));
	}

	@Test
	void dayRangeInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CronTrigger("* * * 28-45 * *", timeZone));
	}

	@Test
	void monthInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CronTrigger("0 0 0 25 13 ?", timeZone));
	}

	@Test
	void monthInvalidTooSmall() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CronTrigger("0 0 0 25 0 ?", timeZone));
	}

	@Test
	void dayOfMonthInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CronTrigger("0 0 0 32 12 ?", timeZone));
	}

	@Test
	void monthRangeInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CronTrigger("* * * * 11-13 *", timeZone));
	}

	@Test
	void whitespace() {
		CronTrigger trigger1 = new CronTrigger("*  *  * *  1 *", timeZone);
		CronTrigger trigger2 = new CronTrigger("* * * * 1 *", timeZone);
		assertThat(trigger2).isEqualTo(trigger1);
	}

	@Test
	void monthSequence() {
		CronTrigger trigger = new CronTrigger("0 30 23 30 1/3 ?", timeZone);
		this.calendar.set(2010, 11, 30);
		Date localDate = this.calendar.getTime();
		// set expected next trigger time
		this.calendar.set(Calendar.HOUR_OF_DAY, 23);
		this.calendar.set(Calendar.MINUTE, 30);
		this.calendar.set(Calendar.SECOND, 0);
		this.calendar.add(Calendar.MONTH, 1);
		TriggerContext context1 = getTriggerContext(localDate);
		Object actual2 = localDate = trigger.nextExecutionTime(context1);
		assertThat(actual2).isEqualTo(this.calendar.getTime());
		// Next trigger is 3 months latter
		this.calendar.add(Calendar.MONTH, 3);
		TriggerContext context2 = getTriggerContext(localDate);
		Object actual1 = localDate = trigger.nextExecutionTime(context2);
		assertThat(actual1).isEqualTo(this.calendar.getTime());
		// Next trigger is 3 months latter
		this.calendar.add(Calendar.MONTH, 3);
		TriggerContext context3 = getTriggerContext(localDate);
		Object actual = trigger.nextExecutionTime(context3);
		assertThat(actual).isEqualTo(this.calendar.getTime());
	}

	@Test
	void daylightSavingMissingHour() {
		// This trigger has to be somewhere between 2:00 AM and 3:00 AM, so we
		// use a cron expression for 2:10 AM every day.
		CronTrigger trigger = new CronTrigger("0 10 2 * * *", timeZone);

		// 2:00 AM on March 31, 2013: start of Daylight Saving Time for CET in 2013.
		// Setting up last completion:
		// - PST: Sun Mar 31 10:09:54 CEST 2013
		// - CET: Sun Mar 31 01:09:54 CET 2013
		this.calendar.set(Calendar.DAY_OF_MONTH, 31);
		this.calendar.set(Calendar.MONTH, Calendar.MARCH);
		this.calendar.set(Calendar.YEAR, 2013);
		this.calendar.set(Calendar.HOUR_OF_DAY, 1);
		this.calendar.set(Calendar.MINUTE, 9);
		this.calendar.set(Calendar.SECOND, 54);
		Date lastCompletionTime = this.calendar.getTime();

		// Setting up expected next execution time:
		// - PST: Sun Mar 31 11:10:00 CEST 2013
		// - CET: Mon Apr 01 02:10:00 CEST 2013
		if (timeZone.equals(TimeZone.getTimeZone("CET"))) {
			// Clocks go forward an hour so 2am doesn't exist in CET for this localDateTime
			this.calendar.add(Calendar.DAY_OF_MONTH, 1);
		}
		this.calendar.add(Calendar.HOUR_OF_DAY, 1);
		this.calendar.set(Calendar.MINUTE, 10);
		this.calendar.set(Calendar.SECOND, 0);

		TriggerContext context = getTriggerContext(lastCompletionTime);
		Object nextExecutionTime = trigger.nextExecutionTime(context);
		assertThat(nextExecutionTime).isEqualTo(this.calendar.getTime());
	}


	private static void roundup(Calendar calendar) {
		calendar.add(Calendar.SECOND, 1);
		calendar.set(Calendar.MILLISECOND, 0);
	}

	private static void assertMatchesNextSecond(CronTrigger trigger, Calendar calendar) {
		Date localDateTime = calendar.getTime();
		roundup(calendar);
		TriggerContext context = getTriggerContext(localDateTime);
		assertThat(trigger.nextExecutionTime(context)).isEqualTo(calendar.getTime());
	}

	private static TriggerContext getTriggerContext(Date lastCompletionTime) {
		return new SimpleTriggerContext(null, null, lastCompletionTime);
	}

}
