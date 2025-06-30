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

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Extension of {@link CronField} for
 * <a href="https://www.quartz-scheduler.org">Quartz</a>-specific fields.
 * Created using the {@code parse*} methods, uses a {@link TemporalAdjuster}
 * internally.
 *
 * <p>Supports a Quartz day-of-month/week field with an L/# expression. Follows
 * common cron conventions in every other respect, including 0-6 for SUN-SAT
 * (plus 7 for SUN as well). Note that Quartz deviates from the day-of-week
 * convention in cron through 1-7 for SUN-SAT whereas Spring strictly follows
 * cron even in combination with the optional Quartz-specific L/# expressions.
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
final class QuartzCronField extends CronField {

	private final Type rollForwardType;

	private final TemporalAdjuster adjuster;

	private final String value;


	private QuartzCronField(Type type, TemporalAdjuster adjuster, String value) {
		this(type, type, adjuster, value);
	}

	/**
	 * Constructor for fields that need to roll forward over a different type
	 * than the type this field represents. See {@link #parseDaysOfWeek(String)}.
	 */
	private QuartzCronField(Type type, Type rollForwardType, TemporalAdjuster adjuster, String value) {
		super(type);
		this.adjuster = adjuster;
		this.value = value;
		this.rollForwardType = rollForwardType;
	}


	/**
	 * Determine whether the given value is a Quartz day-of-month field.
	 */
	public static boolean isQuartzDaysOfMonthField(String value) {
		return value.contains("L") || value.contains("W");
	}

	/**
	 * Parse the given value into a days of months {@code QuartzCronField},
	 * the fourth entry of a cron expression.
	 * <p>Expects a "L" or "W" in the given value.
	 */
	public static QuartzCronField parseDaysOfMonth(String value) {
		int idx = value.lastIndexOf('L');
		if (idx != -1) {
			TemporalAdjuster adjuster;
			if (idx != 0) {
				throw new IllegalArgumentException("Unrecognized characters before 'L' in '" + value + "'");
			}
			else if (value.length() == 2 && value.charAt(1) == 'W') {  // "LW"
				adjuster = lastWeekdayOfMonth();
			}
			else {
				if (value.length() == 1) {  // "L"
					adjuster = lastDayOfMonth();
				}
				else {  // "L-[0-9]+"
					int offset = Integer.parseInt(value, idx + 1, value.length(), 10);
					if (offset >= 0) {
						throw new IllegalArgumentException("Offset '" + offset + " should be < 0 '" + value + "'");
					}
					adjuster = lastDayWithOffset(offset);
				}
			}
			return new QuartzCronField(Type.DAY_OF_MONTH, adjuster, value);
		}
		idx = value.lastIndexOf('W');
		if (idx != -1) {
			if (idx == 0) {
				throw new IllegalArgumentException("No day-of-month before 'W' in '" + value + "'");
			}
			else if (idx != value.length() - 1) {
				throw new IllegalArgumentException("Unrecognized characters after 'W' in '" + value + "'");
			}
			else {  // "[0-9]+W"
				int dayOfMonth = Integer.parseInt(value, 0, idx, 10);
				dayOfMonth = Type.DAY_OF_MONTH.checkValidValue(dayOfMonth);
				TemporalAdjuster adjuster = weekdayNearestTo(dayOfMonth);
				return new QuartzCronField(Type.DAY_OF_MONTH, adjuster, value);
			}
		}
		throw new IllegalArgumentException("No 'L' or 'W' found in '" + value + "'");
	}

	/**
	 * Determine whether the given value is a Quartz day-of-week field.
	 */
	public static boolean isQuartzDaysOfWeekField(String value) {
		return value.contains("L") || value.contains("#");
	}

	/**
	 * Parse the given value into a days of week {@code QuartzCronField},
	 * the sixth entry of a cron expression.
	 * <p>Expects a "L" or "#" in the given value.
	 */
	public static QuartzCronField parseDaysOfWeek(String value) {
		int idx = value.lastIndexOf('L');
		if (idx != -1) {
			if (idx != value.length() - 1) {
				throw new IllegalArgumentException("Unrecognized characters after 'L' in '" + value + "'");
			}
			else {
				TemporalAdjuster adjuster;
				if (idx == 0) {
					throw new IllegalArgumentException("No day-of-week before 'L' in '" + value + "'");
				}
				else {  // "[0-7]L"
					DayOfWeek dayOfWeek = parseDayOfWeek(value.substring(0, idx));
					adjuster = lastInMonth(dayOfWeek);
				}
				return new QuartzCronField(Type.DAY_OF_WEEK, Type.DAY_OF_MONTH, adjuster, value);
			}
		}
		idx = value.lastIndexOf('#');
		if (idx != -1) {
			if (idx == 0) {
				throw new IllegalArgumentException("No day-of-week before '#' in '" + value + "'");
			}
			else if (idx == value.length() - 1) {
				throw new IllegalArgumentException("No ordinal after '#' in '" + value + "'");
			}
			// "[0-7]#[0-9]+"
			DayOfWeek dayOfWeek = parseDayOfWeek(value.substring(0, idx));
			int ordinal = Integer.parseInt(value, idx + 1, value.length(), 10);
			if (ordinal <= 0) {
				throw new IllegalArgumentException("Ordinal '" + ordinal + "' in '" + value +
						"' must be positive number ");
			}
			TemporalAdjuster adjuster = dayOfWeekInMonth(ordinal, dayOfWeek);
			return new QuartzCronField(Type.DAY_OF_WEEK, Type.DAY_OF_MONTH, adjuster, value);
		}
		throw new IllegalArgumentException("No 'L' or '#' found in '" + value + "'");
	}

	private static DayOfWeek parseDayOfWeek(String value) {
		int dayOfWeek = Integer.parseInt(value);
		if (dayOfWeek == 0) {
			dayOfWeek = 7;  // cron is 0 based; java.time 1 based
		}
		try {
			return DayOfWeek.of(dayOfWeek);
		}
		catch (DateTimeException ex) {
			throw new IllegalArgumentException(ex.getMessage() + " '" + value + "'", ex);
		}
	}

	/**
	 * Returns an adjuster that resets to midnight.
	 */
	private static TemporalAdjuster atMidnight() {
		return temporal -> {
			if (temporal.isSupported(ChronoField.NANO_OF_DAY)) {
				return temporal.with(ChronoField.NANO_OF_DAY, 0);
			}
			else {
				return temporal;
			}
		};
	}

	/**
	 * Returns an adjuster that returns a new temporal set to the last
	 * day of the current month at midnight.
	 */
	private static TemporalAdjuster lastDayOfMonth() {
		TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();
		return temporal -> {
			Temporal result = adjuster.adjustInto(temporal);
			return rollbackToMidnight(temporal, result);
		};
	}

	/**
	 * Returns an adjuster that returns the last weekday of the month.
	 */
	private static TemporalAdjuster lastWeekdayOfMonth() {
		TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();
		return temporal -> {
			Temporal lastDom = adjuster.adjustInto(temporal);
			Temporal result;
			int dow = lastDom.get(ChronoField.DAY_OF_WEEK);
			if (dow == 6) {  // Saturday
				result = lastDom.minus(1, ChronoUnit.DAYS);
			}
			else if (dow == 7) {  // Sunday
				result = lastDom.minus(2, ChronoUnit.DAYS);
			}
			else {
				result = lastDom;
			}
			return rollbackToMidnight(temporal, result);
		};
	}

	/**
	 * Returns a temporal adjuster that finds the nth-to-last day of the month.
	 * @param offset the negative offset, i.e. -3 means third-to-last
	 * @return a nth-to-last day-of-month adjuster
	 */
	private static TemporalAdjuster lastDayWithOffset(int offset) {
		Assert.isTrue(offset < 0, "Offset should be < 0");
		TemporalAdjuster adjuster = TemporalAdjusters.lastDayOfMonth();
		return temporal -> {
			Temporal result = adjuster.adjustInto(temporal).plus(offset, ChronoUnit.DAYS);
			return rollbackToMidnight(temporal, result);
		};
	}

	/**
	 * Returns a temporal adjuster that finds the weekday nearest to the given
	 * day-of-month. If {@code dayOfMonth} falls on a Saturday, the date is
	 * moved back to Friday; if it falls on a Sunday (or if {@code dayOfMonth}
	 * is 1 and it falls on a Saturday), it is moved forward to Monday.
	 * @param dayOfMonth the goal day-of-month
	 * @return the weekday-nearest-to adjuster
	 */
	private static TemporalAdjuster weekdayNearestTo(int dayOfMonth) {
		return temporal -> {
			int current = Type.DAY_OF_MONTH.get(temporal);
			DayOfWeek dayOfWeek = DayOfWeek.from(temporal);

			if ((current == dayOfMonth && isWeekday(dayOfWeek)) ||  // dayOfMonth is a weekday
					(dayOfWeek == DayOfWeek.FRIDAY && current == dayOfMonth - 1) ||  // dayOfMonth is a Saturday, so Friday before
					(dayOfWeek == DayOfWeek.MONDAY && current == dayOfMonth + 1) ||  // dayOfMonth is a Sunday, so Monday after
					(dayOfWeek == DayOfWeek.MONDAY && dayOfMonth == 1 && current == 3)) {  // dayOfMonth is Saturday 1st, so Monday 3rd
				return temporal;
			}
			int count = 0;
			while (count++ < CronExpression.MAX_ATTEMPTS) {
				if (current == dayOfMonth) {
					dayOfWeek = DayOfWeek.from(temporal);

					if (dayOfWeek == DayOfWeek.SATURDAY) {
						if (dayOfMonth != 1) {
							temporal = temporal.minus(1, ChronoUnit.DAYS);
						}
						else {
							// exception for "1W" fields: execute on next Monday
							temporal = temporal.plus(2, ChronoUnit.DAYS);
						}
					}
					else if (dayOfWeek == DayOfWeek.SUNDAY) {
						temporal = temporal.plus(1, ChronoUnit.DAYS);
					}
					return atMidnight().adjustInto(temporal);
				}
				else {
					temporal = Type.DAY_OF_MONTH.elapseUntil(cast(temporal), dayOfMonth);
					current = Type.DAY_OF_MONTH.get(temporal);
				}
			}
			return null;
		};
	}

	private static boolean isWeekday(DayOfWeek dayOfWeek) {
		return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
	}

	/**
	 * Returns a temporal adjuster that finds the last of the given day-of-week
	 * in a month.
	 */
	private static TemporalAdjuster lastInMonth(DayOfWeek dayOfWeek) {
		TemporalAdjuster adjuster = TemporalAdjusters.lastInMonth(dayOfWeek);
		return temporal -> {
			Temporal result = adjuster.adjustInto(temporal);
			return rollbackToMidnight(temporal, result);
		};
	}

	/**
	 * Returns a temporal adjuster that finds {@code ordinal}-th occurrence of
	 * the given day-of-week in a month.
	 */
	private static TemporalAdjuster dayOfWeekInMonth(int ordinal, DayOfWeek dayOfWeek) {
		TemporalAdjuster adjuster = TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek);
		return temporal -> {
			// TemporalAdjusters can overflow to a different month
			// in this case, attempt the same adjustment with the next/previous month
			for (int i = 0; i < 12; i++) {
				Temporal result = adjuster.adjustInto(temporal);
				if (result.get(ChronoField.MONTH_OF_YEAR) == temporal.get(ChronoField.MONTH_OF_YEAR)) {
					return rollbackToMidnight(temporal, result);
				}
				temporal = result;
			}
			return null;
		};
	}

	/**
	 * Rolls back the given {@code result} to midnight. When
	 * {@code current} has the same day of month as {@code result}, the former
	 * is returned, to make sure that we don't end up before where we started.
	 */
	private static Temporal rollbackToMidnight(Temporal current, Temporal result) {
		if (result.get(ChronoField.DAY_OF_MONTH) == current.get(ChronoField.DAY_OF_MONTH)) {
			return current;
		}
		else {
			return atMidnight().adjustInto(result);
		}
	}


	@Override
	public <T extends Temporal & Comparable<? super T>> @Nullable T nextOrSame(T temporal) {
		T result = adjust(temporal);
		if (result != null) {
			if (result.compareTo(temporal) < 0) {
				// We ended up before the start, roll forward and try again
				temporal = this.rollForwardType.rollForward(temporal);
				result = adjust(temporal);
				if (result != null) {
					result = type().reset(result);
				}
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private <T extends Temporal & Comparable<? super T>> @Nullable T adjust(T temporal) {
		return (T) this.adjuster.adjustInto(temporal);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof QuartzCronField that &&
				type() == that.type() && this.value.equals(that.value)));
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}

	@Override
	public String toString() {
		return type() + " '" + this.value + "'";
	}

}
