/*
 * Copyright 2002-2021 the original author or authors.
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.temporal.TemporalAdjusters.next;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class CronExpressionTests {

	private static final Condition<Temporal> weekday = new Condition<Temporal>("weekday") {

		@Override
		public boolean matches(Temporal value) {
			int dayOfWeek = value.get(ChronoField.DAY_OF_WEEK);
			return dayOfWeek != 6 && dayOfWeek != 7;
		}
	};

	@Test
	public void isValidExpression() {
		assertThat(CronExpression.isValidExpression(null)).isFalse();
		assertThat(CronExpression.isValidExpression("")).isFalse();
		assertThat(CronExpression.isValidExpression("*")).isFalse();
		assertThat(CronExpression.isValidExpression("* * * * *")).isFalse();
		assertThat(CronExpression.isValidExpression("* * * * * * *")).isFalse();

		assertThat(CronExpression.isValidExpression("* * * * * *")).isTrue();
	}

	@Test
	void matchAll() {
		CronExpression expression = CronExpression.parse("* * * * * *");

		LocalDateTime last = LocalDateTime.now();
		LocalDateTime expected = last.plusSeconds(1).withNano(0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void matchLastSecond() {
		CronExpression expression = CronExpression.parse("* * * * * *");

		LocalDateTime last = LocalDateTime.now().withSecond(58);
		LocalDateTime expected = last.plusSeconds(1).withNano(0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void matchSpecificSecond() {
		CronExpression expression = CronExpression.parse("10 * * * * *");

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime last = now.withSecond(9);
		LocalDateTime expected = last.withSecond(10).withNano(0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void incrementSecondByOne() {
		CronExpression expression = CronExpression.parse("11 * * * * *");

		LocalDateTime last = LocalDateTime.now().withSecond(10);
		LocalDateTime expected = last.plusSeconds(1).withNano(0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void incrementSecondAndRollover() {
		CronExpression expression = CronExpression.parse("10 * * * * *");

		LocalDateTime last = LocalDateTime.now().withSecond(11);
		LocalDateTime expected = last.plusMinutes(1).withSecond(10).withNano(0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void secondRange() {
		CronExpression expression = CronExpression.parse("10-15 * * * * *");
		LocalDateTime now = LocalDateTime.now();

		for (int i = 9; i < 15; i++) {
			LocalDateTime last = now.withSecond(i);
			LocalDateTime expected = last.plusSeconds(1).withNano(0);
			assertThat(expression.next(last)).isEqualTo(expected);
		}
	}

	@Test
	void incrementMinute() {
		CronExpression expression = CronExpression.parse("0 * * * * *");

		LocalDateTime last = LocalDateTime.now().withMinute(10);
		LocalDateTime expected = last.plusMinutes(1).withSecond(0).withNano(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusMinutes(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void incrementMinuteByOne() {
		CronExpression expression = CronExpression.parse("0 11 * * * *");

		LocalDateTime last = LocalDateTime.now().withMinute(10);
		LocalDateTime expected = last.plusMinutes(1).withSecond(0).withNano(0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void incrementMinuteAndRollover() {
		CronExpression expression = CronExpression.parse("0 10 * * * *");

		LocalDateTime last = LocalDateTime.now().withMinute(11).withSecond(0);
		LocalDateTime expected = last.plusMinutes(59).withNano(0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void incrementHour() {
		CronExpression expression = CronExpression.parse("0 0 * * * *");

		int year = Year.now().getValue();
		LocalDateTime last = LocalDateTime.of(year, 10, 30, 11, 1);
		LocalDateTime expected = last.withHour(12).withMinute(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.withHour(13);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void incrementHourAndRollover() {
		CronExpression expression = CronExpression.parse("0 0 * * * *");

		int year = Year.now().getValue();
		LocalDateTime last = LocalDateTime.of(year, 9, 10, 23, 1);
		LocalDateTime expected = last.withDayOfMonth(11).withHour(0).withMinute(0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void incrementDayOfMonth() {
		CronExpression expression = CronExpression.parse("0 0 0 * * *");

		LocalDateTime last = LocalDateTime.now().withDayOfMonth(1);
		LocalDateTime expected = last.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusDays(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void incrementDayOfMonthByOne() {
		CronExpression expression = CronExpression.parse("* * * 10 * *");

		LocalDateTime last = LocalDateTime.now().withDayOfMonth(9);
		LocalDateTime expected = last.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void incrementDayOfMonthAndRollover() {
		CronExpression expression = CronExpression.parse("* * * 10 * *");

		LocalDateTime last = LocalDateTime.now().withDayOfMonth(11);
		LocalDateTime expected =
				last.plusMonths(1).withDayOfMonth(10).withHour(0).withMinute(0).withSecond(0).withNano(0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void dailyTriggerInShortMonth() {
		CronExpression expression = CronExpression.parse("0 0 0 * * *");

		// September: 30 days
		LocalDateTime last = LocalDateTime.now().withMonth(9).withDayOfMonth(30);
		LocalDateTime expected = LocalDateTime.of(last.getYear(), 10, 1, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.withDayOfMonth(2);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void dailyTriggerInLongMonth() {
		CronExpression expression = CronExpression.parse("0 0 0 * * *");

		// August: 31 days and not a daylight saving boundary
		LocalDateTime last = LocalDateTime.now().withMonth(8).withDayOfMonth(30);
		LocalDateTime expected = last.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusDays(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void dailyTriggerOnDaylightSavingBoundary() {
		CronExpression expression = CronExpression.parse("0 0 0 * * *");

		// October: 31 days and a daylight saving boundary in CET
		ZonedDateTime last = ZonedDateTime.now(ZoneId.of("CET")).withMonth(10).withDayOfMonth(30);
		ZonedDateTime expected = last.withDayOfMonth(31).withHour(0).withMinute(0).withSecond(0).withNano(0);
		ZonedDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.withMonth(11).withDayOfMonth(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void incrementMonth() {
		CronExpression expression = CronExpression.parse("0 0 0 1 * *");

		LocalDateTime last = LocalDateTime.now().withMonth(10).withDayOfMonth(30);
		LocalDateTime expected = LocalDateTime.of(last.getYear(), 11, 1, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.withMonth(12);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void incrementMonthAndRollover() {
		CronExpression expression = CronExpression.parse("0 0 0 1 * *");

		LocalDateTime last = LocalDateTime.now().withYear(2010).withMonth(12).withDayOfMonth(31);
		LocalDateTime expected = LocalDateTime.of(2011, 1, 1, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusMonths(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void monthlyTriggerInLongMonth() {
		CronExpression expression = CronExpression.parse("0 0 0 31 * *");

		LocalDateTime last = LocalDateTime.now().withMonth(10).withDayOfMonth(30);
		LocalDateTime expected = last.withDayOfMonth(31).withHour(0).withMinute(0).withSecond(0).withNano(0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void monthlyTriggerInShortMonth() {
		CronExpression expression = CronExpression.parse("0 0 0 1 * *");

		LocalDateTime last = LocalDateTime.now().withMonth(10).withDayOfMonth(30);
		LocalDateTime expected = LocalDateTime.of(last.getYear(), 11, 1, 0, 0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void incrementDayOfWeekByOne() {
		CronExpression expression = CronExpression.parse("* * * * * 2");

		LocalDateTime last = LocalDateTime.now().with(next(MONDAY));
		LocalDateTime expected = last.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(TUESDAY);
	}

	@Test
	void incrementDayOfWeekAndRollover() {
		CronExpression expression = CronExpression.parse("* * * * * 2");

		LocalDateTime last = LocalDateTime.now().with(next(WEDNESDAY));
		LocalDateTime expected = last.plusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(TUESDAY);
	}

	@Test
	void specificMinuteSecond() {
		CronExpression expression = CronExpression.parse("55 5 * * * *");

		LocalDateTime last = LocalDateTime.now().withMinute(4).withSecond(54);
		LocalDateTime expected = last.plusMinutes(1).withSecond(55).withNano(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusHours(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void specificHourSecond() {
		CronExpression expression = CronExpression.parse("55 * 10 * * *");

		LocalDateTime last = LocalDateTime.now().withHour(9).withSecond(54);
		LocalDateTime expected = last.plusHours(1).withMinute(0).withSecond(55).withNano(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusMinutes(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void specificMinuteHour() {
		CronExpression expression = CronExpression.parse("* 5 10 * * *");

		LocalDateTime last = LocalDateTime.now().withHour(9).withMinute(4);
		LocalDateTime expected = last.plusHours(1).plusMinutes(1).withSecond(0).withNano(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		// next trigger is in one second because second is wildcard
		expected = expected.plusSeconds(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void specificDayOfMonthSecond() {
		CronExpression expression = CronExpression.parse("55 * * 3 * *");

		LocalDateTime last = LocalDateTime.now().withDayOfMonth(2).withSecond(54);
		LocalDateTime expected = last.plusDays(1).withHour(0).withMinute(0).withSecond(55).withNano(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusMinutes(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void specificDate() {
		CronExpression expression = CronExpression.parse("* * * 3 11 *");

		LocalDateTime last = LocalDateTime.now().withMonth(10).withDayOfMonth(2);
		LocalDateTime expected = LocalDateTime.of(last.getYear(), 11, 3, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusSeconds(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void nonExistentSpecificDate() {
		CronExpression expression = CronExpression.parse("0 0 0 31 6 *");

		LocalDateTime last = LocalDateTime.now().withMonth(3).withDayOfMonth(10);
		assertThat(expression.next(last)).isNull();
	}

	@Test
	void leapYearSpecificDate() {
		CronExpression expression = CronExpression.parse("0 0 0 29 2 *");

		LocalDateTime last = LocalDateTime.now().withYear(2007).withMonth(2).withDayOfMonth(10);
		LocalDateTime expected = LocalDateTime.of(2008, 2, 29, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusYears(4);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void weekDaySequence() {
		CronExpression expression = CronExpression.parse("0 0 7 ? * MON-FRI");

		// This is a Saturday
		LocalDateTime last = LocalDateTime.of(LocalDate.of(2009, 9, 26), LocalTime.now());
		LocalDateTime expected = last.plusDays(2).withHour(7).withMinute(0).withSecond(0).withNano(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		// Next day is a week day so add one
		last = actual;
		expected = expected.plusDays(1);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusDays(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void monthSequence() {
		CronExpression expression = CronExpression.parse("0 30 23 30 1/3 ?");

		LocalDateTime last = LocalDateTime.of(LocalDate.of(2010, 12, 30), LocalTime.now());
		LocalDateTime expected = last.plusMonths(1).withHour(23).withMinute(30).withSecond(0).withSecond(0).withNano(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		// Next trigger is 3 months later
		last = actual;
		expected = expected.plusMonths(3);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusMonths(3);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	public void fixedDays() {
		CronExpression expression = CronExpression.parse("0 0 0 29 2 WED");

		LocalDateTime last = LocalDateTime.of(2012, 2, 29, 1, 0);
		assertThat(last.getDayOfWeek()).isEqualTo(WEDNESDAY);

		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual.getDayOfMonth()).isEqualTo(29);
		assertThat(actual.getDayOfWeek()).isEqualTo(WEDNESDAY);
	}

	@Test
	void friday13th() {
		CronExpression expression = CronExpression.parse("0 0 0 13 * FRI");

		LocalDateTime last = LocalDateTime.of(2018, 7, 31, 11, 47, 14);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);
		assertThat(actual.getDayOfMonth()).isEqualTo(13);

		last = actual;
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);
		assertThat(actual.getDayOfMonth()).isEqualTo(13);
	}

	@Test
	public void everyTenDays() {
		CronExpression cronExpression = CronExpression.parse("0 15 12 */10 1-8 5");

		LocalDateTime last = LocalDateTime.parse("2021-04-30T12:14:59");
		LocalDateTime expected = LocalDateTime.parse("2021-05-21T12:15");
		LocalDateTime actual = cronExpression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = LocalDateTime.parse("2021-06-11T12:15");
		actual = cronExpression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = LocalDateTime.parse("2022-01-21T12:15");
		actual = cronExpression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void yearly() {
		CronExpression expression = CronExpression.parse("@yearly");
		assertThat(expression).isEqualTo(CronExpression.parse("0 0 0 1 1 *"));

		LocalDateTime last = LocalDateTime.now().withMonth(10).withDayOfMonth(10);
		LocalDateTime expected = LocalDateTime.of(last.getYear() + 1, 1, 1, 0, 0);

		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusYears(1);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusYears(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void annually() {
		CronExpression expression = CronExpression.parse("@annually");
		assertThat(expression).isEqualTo(CronExpression.parse("0 0 0 1 1 *"));
		assertThat(expression).isEqualTo(CronExpression.parse("@yearly"));
	}

	@Test
	void monthly() {
		CronExpression expression = CronExpression.parse("@monthly");
		assertThat(expression).isEqualTo(CronExpression.parse("0 0 0 1 * *"));

		LocalDateTime last = LocalDateTime.now().withMonth(10).withDayOfMonth(10);
		LocalDateTime expected = LocalDateTime.of(last.getYear(), 11, 1, 0, 0);

		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusMonths(1);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusMonths(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void weekly() {
		CronExpression expression = CronExpression.parse("@weekly");
		assertThat(expression).isEqualTo(CronExpression.parse("0 0 0 * * 0"));

		LocalDateTime last = LocalDateTime.now();
		LocalDateTime expected = last.with(next(SUNDAY)).withHour(0).withMinute(0).withSecond(0).withNano(0);

		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusWeeks(1);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusWeeks(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void daily() {
		CronExpression expression = CronExpression.parse("@daily");
		assertThat(expression).isEqualTo(CronExpression.parse("0 0 0 * * *"));

		LocalDateTime last = LocalDateTime.now();
		LocalDateTime expected = last.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusDays(1);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusDays(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void midnight() {
		CronExpression expression = CronExpression.parse("@midnight");
		assertThat(expression).isEqualTo(CronExpression.parse("0 0 0 * * *"));
		assertThat(expression).isEqualTo(CronExpression.parse("@daily"));
	}

	@Test
	void hourly() {
		CronExpression expression = CronExpression.parse("@hourly");
		assertThat(expression).isEqualTo(CronExpression.parse("0 0 * * * *"));

		LocalDateTime last = LocalDateTime.now();
		LocalDateTime expected = last.plusHours(1).withMinute(0).withSecond(0).withNano(0);

		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusHours(1);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusHours(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}


	@Test
	void quartzLastDayOfMonth() {
		CronExpression expression = CronExpression.parse("0 0 0 L * *");

		LocalDateTime last = LocalDateTime.of(LocalDate.of(2008, 1, 4), LocalTime.now());
		LocalDateTime expected = LocalDateTime.of(2008, 1, 31, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = LocalDateTime.of(2008, 2, 29, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = LocalDateTime.of(2008, 3, 31, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = LocalDateTime.of(2008, 4, 30, 0, 0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void quartzLastDayOfMonthOffset() {
		// L-3 =  third-to-last day of the month
		CronExpression expression = CronExpression.parse("0 0 0 L-3 * *");

		LocalDateTime last = LocalDateTime.of(LocalDate.of(2008, 1, 4), LocalTime.now());
		LocalDateTime expected = LocalDateTime.of(2008, 1, 28, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = LocalDateTime.of(2008, 2, 26, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = LocalDateTime.of(2008, 3, 28, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = LocalDateTime.of(2008, 4, 27, 0, 0);
		assertThat(expression.next(last)).isEqualTo(expected);
	}

	@Test
	void quartzLastWeekdayOfMonth() {
		CronExpression expression = CronExpression.parse("0 0 0 LW * *");

		LocalDateTime last = LocalDateTime.of(LocalDate.of(2008, 1, 4), LocalTime.now());
		LocalDateTime expected = LocalDateTime.of(2008, 1, 31, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2008, 2, 29, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2008, 3, 31, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2008, 4, 30, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2008, 5, 30, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2008, 6, 30, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2008, 7, 31, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2008, 8, 29, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2008, 9, 30, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2008, 10, 31, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2008, 11, 28, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2008, 12, 31, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);
	}

	@Test
	public void quartzLastDayOfWeekOffset() {
		// last Friday (5) of the month
		CronExpression expression = CronExpression.parse("0 0 0 * * 5L");

		LocalDateTime last = LocalDateTime.of(LocalDate.of(2008, 1, 4), LocalTime.now());
		LocalDateTime expected = LocalDateTime.of(2008, 1, 25, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2008, 2, 29, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2008, 3, 28, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2008, 4, 25, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2008, 5, 30, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2008, 6, 27, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2008, 7, 25, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2008, 8, 29, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2008, 9, 26, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2008, 10, 31, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2008, 11, 28, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2008, 12, 26, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);
	}

	@Test
	void quartzWeekdayNearestTo15() {
		CronExpression expression = CronExpression.parse("0 0 0 15W * ?");

		LocalDateTime last = LocalDateTime.of(2020, 1, 1, 0, 0);
		LocalDateTime expected = LocalDateTime.of(2020, 1, 15, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 2, 14, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 3, 16, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 4, 15, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);
	}

	@Test
	void quartzWeekdayNearestTo1() {
		CronExpression expression = CronExpression.parse("0 0 0 1W * ?");

		LocalDateTime last = LocalDateTime.of(2019, 12, 31, 0, 0);
		LocalDateTime expected = LocalDateTime.of(2020, 1, 1, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 2, 3, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 3, 2, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 4, 1, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);
	}

	@Test
	void quartzWeekdayNearestTo31() {
		CronExpression expression = CronExpression.parse("0 0 0 31W * ?");

		LocalDateTime last = LocalDateTime.of(2020, 1, 1, 0, 0);
		LocalDateTime expected = LocalDateTime.of(2020, 1, 31, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 3, 31, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 7, 31, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 8, 31, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 10, 30, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 12, 31, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);
	}

	@Test
	void quartz2ndFridayOfTheMonth() {
		CronExpression expression = CronExpression.parse("0 0 0 ? * 5#2");

		LocalDateTime last = LocalDateTime.of(2020, 1, 1, 0, 0);
		LocalDateTime expected = LocalDateTime.of(2020, 1, 10, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2020, 2, 14, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2020, 3, 13, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2020, 4, 10, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);
	}

	@Test
	void quartz2ndFridayOfTheMonthDayName() {
		CronExpression expression = CronExpression.parse("0 0 0 ? * FRI#2");

		LocalDateTime last = LocalDateTime.of(2020, 1, 1, 0, 0);
		LocalDateTime expected = LocalDateTime.of(2020, 1, 10, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2020, 2, 14, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2020, 3, 13, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2020, 4, 10, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);
	}

	@Test
	void quartzFifthWednesdayOfTheMonth() {
		CronExpression expression = CronExpression.parse("0 0 0 ? * 3#5");

		LocalDateTime last = LocalDateTime.of(2020, 1, 1, 0, 0);
		LocalDateTime expected = LocalDateTime.of(2020, 1, 29, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(WEDNESDAY);

		last = actual;
		expected = LocalDateTime.of(2020, 4, 29, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(WEDNESDAY);

		last = actual;
		expected = LocalDateTime.of(2020, 7, 29, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(WEDNESDAY);

		last = actual;
		expected = LocalDateTime.of(2020, 9, 30, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(WEDNESDAY);

		last = actual;
		expected = LocalDateTime.of(2020, 12, 30, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(WEDNESDAY);
	}

	@Test
	void dayOfMonthListWithQuartz() {
		CronExpression expression = CronExpression.parse("0 0 0 1W,15,LW * ?");

		LocalDateTime last = LocalDateTime.of(2019, 12, 30, 0, 0);
		LocalDateTime expected = LocalDateTime.of(2019, 12, 31, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 1, 1, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);

		last = actual;
		expected = LocalDateTime.of(2020, 1, 15, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = LocalDateTime.of(2020, 1, 31, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual).is(weekday);
	}

	@Test
	void dayOfWeekListWithQuartz() {
		CronExpression expression = CronExpression.parse("0 0 0 ? * THU#1,THU#3,THU#5");

		LocalDateTime last = LocalDateTime.of(2019, 12, 31, 0, 0);
		LocalDateTime expected = LocalDateTime.of(2020, 1, 2, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(THURSDAY);

		last = actual;
		expected = LocalDateTime.of(2020, 1, 16, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(THURSDAY);

		last = actual;
		expected = LocalDateTime.of(2020, 1, 30, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(THURSDAY);
	}

	@Test
	void quartzLastDayOfMonthEveryHour() {
		CronExpression expression = CronExpression.parse("0 0 * L * *");

		LocalDateTime last = LocalDateTime.of(2021, 1, 30, 0, 1);
		LocalDateTime expected = LocalDateTime.of(2021, 1, 31, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = LocalDateTime.of(2021, 1, 31, 1, 0);
		expected = LocalDateTime.of(2021, 1, 31, 2, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void quartzLastDayOfMonthOffsetEveryHour() {
		CronExpression expression = CronExpression.parse("0 0 * L-1 * *");

		LocalDateTime last = LocalDateTime.of(2021, 1, 29, 0, 1);
		LocalDateTime expected = LocalDateTime.of(2021, 1, 30, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = LocalDateTime.of(2021, 1, 30, 1, 0);
		expected = LocalDateTime.of(2021, 1, 30, 2, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void quartzFirstWeekdayOfMonthEveryHour() {
		CronExpression expression = CronExpression.parse("0 0 * 1W * *");

		LocalDateTime last = LocalDateTime.of(2021, 1, 31, 0, 1);
		LocalDateTime expected = LocalDateTime.of(2021, 2, 1, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = LocalDateTime.of(2021, 2, 1, 1, 0);
		expected = LocalDateTime.of(2021, 2, 1, 2, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void quartzLastWeekdayOfMonthEveryHour() {
		CronExpression expression = CronExpression.parse("0 0 * LW * *");

		LocalDateTime last = LocalDateTime.of(2021, 1, 28, 0, 1);
		LocalDateTime expected = LocalDateTime.of(2021, 1, 29, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = LocalDateTime.of(2021, 1, 29, 1, 0);
		expected = LocalDateTime.of(2021, 1, 29, 2, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void quartz5thFridayOfTheMonthEveryHour() {
		CronExpression expression = CronExpression.parse("0 0 * ? * FRI#5");

		LocalDateTime last = LocalDateTime.of(2021, 1, 28, 0, 1);
		LocalDateTime expected = LocalDateTime.of(2021, 1, 29, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = LocalDateTime.of(2021, 1, 29, 1, 0);
		expected = LocalDateTime.of(2021, 1, 29, 2, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void quartzLastFridayOfTheMonthEveryHour() {
		CronExpression expression = CronExpression.parse("0 0 * ? * FRIL");

		LocalDateTime last = LocalDateTime.of(2021, 1, 28, 0, 1);
		LocalDateTime expected = LocalDateTime.of(2021, 1, 29, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = LocalDateTime.of(2021, 1, 29, 1, 0);
		expected = LocalDateTime.of(2021, 1, 29, 2, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void sundayToFriday() {
		CronExpression expression = CronExpression.parse("0 0 0 ? * SUN-FRI");

		LocalDateTime last = LocalDateTime.of(2021, 2, 25, 15, 0);
		LocalDateTime expected = LocalDateTime.of(2021, 2, 26, 0, 0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(FRIDAY);

		last = actual;
		expected = LocalDateTime.of(2021, 2, 28, 0, 0);
		actual = expression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(SUNDAY);
	}

	@Test
	public void daylightSaving() {
		CronExpression cronExpression = CronExpression.parse("0 0 9 * * *");

		ZonedDateTime last = ZonedDateTime.parse("2021-03-27T09:00:00+01:00[Europe/Amsterdam]");
		ZonedDateTime expected = ZonedDateTime.parse("2021-03-28T09:00:00+02:00[Europe/Amsterdam]");
		ZonedDateTime actual = cronExpression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		last = ZonedDateTime.parse("2021-10-30T09:00:00+02:00[Europe/Amsterdam]");
		expected = ZonedDateTime.parse("2021-10-31T09:00:00+01:00[Europe/Amsterdam]");
		actual = cronExpression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);

		cronExpression = CronExpression.parse("0 10 2 * * *");

		last = ZonedDateTime.parse("2013-03-31T01:09:00+01:00[Europe/Amsterdam]");
		expected = ZonedDateTime.parse("2013-04-01T02:10:00+02:00[Europe/Amsterdam]");
		actual = cronExpression.next(last);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(expected);
	}



}
