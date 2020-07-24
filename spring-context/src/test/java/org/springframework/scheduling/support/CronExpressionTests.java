/*
 * Copyright 2002-2020 the original author or authors.
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

import org.junit.jupiter.api.Test;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.temporal.TemporalAdjusters.next;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class CronExpressionTests {

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
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(TUESDAY);
	}

	@Test
	void incrementDayOfWeekAndRollover() {
		CronExpression expression = CronExpression.parse("* * * * * 2");

		LocalDateTime last = LocalDateTime.now().with(next(WEDNESDAY));
		LocalDateTime expected = last.plusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
		LocalDateTime actual = expression.next(last);
		assertThat(actual).isEqualTo(expected);
		assertThat(actual.getDayOfWeek()).isEqualTo(TUESDAY);
	}

	@Test
	void specificMinuteSecond() {
		CronExpression expression = CronExpression.parse("55 5 * * * *");

		LocalDateTime last = LocalDateTime.now().withMinute(4).withSecond(54);
		LocalDateTime expected = last.plusMinutes(1).withSecond(55).withNano(0);
		LocalDateTime actual = expression.next(last);
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
		assertThat(actual).isEqualTo(expected);

		// Next day is a week day so add one
		last = actual;
		expected = expected.plusDays(1);
		actual = expression.next(last);
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
		assertThat(actual).isEqualTo(expected);

		// Next trigger is 3 months later
		last = actual;
		expected = expected.plusMonths(3);
		actual = expression.next(last);
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
	void yearly() {
		CronExpression expression = CronExpression.parse("@yearly");
		assertThat(expression).isEqualTo(CronExpression.parse("0 0 0 1 1 *"));

		LocalDateTime last = LocalDateTime.now().withMonth(10).withDayOfMonth(10);
		LocalDateTime expected = LocalDateTime.of(last.getYear() + 1, 1, 1, 0, 0);

		LocalDateTime actual = expression.next(last);
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusYears(1);
		actual = expression.next(last);
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
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusMonths(1);
		actual = expression.next(last);
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
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusWeeks(1);
		actual = expression.next(last);
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
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusDays(1);
		actual = expression.next(last);
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
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusHours(1);
		actual = expression.next(last);
		assertThat(actual).isEqualTo(expected);

		last = actual;
		expected = expected.plusHours(1);
		assertThat(expression.next(last)).isEqualTo(expected);
	}


}
