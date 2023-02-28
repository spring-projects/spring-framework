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

import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.ValueRange;
import java.util.function.BiFunction;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Single field in a cron pattern. Created using the {@code parse*} methods,
 * main and only entry point is {@link #nextOrSame(Temporal)}.
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
abstract class CronField {

	private static final String[] MONTHS = new String[]{"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP",
			"OCT", "NOV", "DEC"};

	private static final String[] DAYS = new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

	private final Type type;


	protected CronField(Type type) {
		this.type = type;
	}

	/**
	 * Return a {@code CronField} enabled for 0 nanoseconds.
	 */
	public static CronField zeroNanos() {
		return BitsCronField.zeroNanos();
	}

	/**
	 * Parse the given value into a seconds {@code CronField}, the first entry of a cron expression.
	 */
	public static CronField parseSeconds(String value) {
		return BitsCronField.parseSeconds(value);
	}

	/**
	 * Parse the given value into a minutes {@code CronField}, the second entry of a cron expression.
	 */
	public static CronField parseMinutes(String value) {
		return BitsCronField.parseMinutes(value);
	}

	/**
	 * Parse the given value into an hours {@code CronField}, the third entry of a cron expression.
	 */
	public static CronField parseHours(String value) {
		return BitsCronField.parseHours(value);
	}

	/**
	 * Parse the given value into a days of months {@code CronField}, the fourth entry of a cron expression.
	 */
	public static CronField parseDaysOfMonth(String value) {
		if (!QuartzCronField.isQuartzDaysOfMonthField(value)) {
			return BitsCronField.parseDaysOfMonth(value);
		}
		else {
			return parseList(value, Type.DAY_OF_MONTH, (field, type) -> {
				if (QuartzCronField.isQuartzDaysOfMonthField(field)) {
					return QuartzCronField.parseDaysOfMonth(field);
				}
				else {
					return BitsCronField.parseDaysOfMonth(field);
				}
			});
		}
	}

	/**
	 * Parse the given value into a month {@code CronField}, the fifth entry of a cron expression.
	 */
	public static CronField parseMonth(String value) {
		value = replaceOrdinals(value, MONTHS);
		return BitsCronField.parseMonth(value);
	}

	/**
	 * Parse the given value into a days of week {@code CronField}, the sixth entry of a cron expression.
	 */
	public static CronField parseDaysOfWeek(String value) {
		value = replaceOrdinals(value, DAYS);
		if (!QuartzCronField.isQuartzDaysOfWeekField(value)) {
			return BitsCronField.parseDaysOfWeek(value);
		}
		else {
			return parseList(value, Type.DAY_OF_WEEK, (field, type) -> {
				if (QuartzCronField.isQuartzDaysOfWeekField(field)) {
					return QuartzCronField.parseDaysOfWeek(field);
				}
				else {
					return BitsCronField.parseDaysOfWeek(field);
				}
			});
		}
	}


	private static CronField parseList(String value, Type type, BiFunction<String, Type, CronField> parseFieldFunction) {
		Assert.hasLength(value, "Value must not be empty");
		String[] fields = StringUtils.delimitedListToStringArray(value, ",");
		CronField[] cronFields = new CronField[fields.length];
		for (int i = 0; i < fields.length; i++) {
			cronFields[i] = parseFieldFunction.apply(fields[i], type);
		}
		return CompositeCronField.compose(cronFields, type, value);
	}

	private static String replaceOrdinals(String value, String[] list) {
		value = value.toUpperCase();
		for (int i = 0; i < list.length; i++) {
			String replacement = Integer.toString(i + 1);
			value = StringUtils.replace(value, list[i], replacement);
		}
		return value;
	}


	/**
	 * Get the next or same {@link Temporal} in the sequence matching this
	 * cron field.
	 * @param temporal the seed value
	 * @return the next or same temporal matching the pattern
	 */
	@Nullable
	public abstract <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal);


	protected Type type() {
		return this.type;
	}

	@SuppressWarnings("unchecked")
	protected static <T extends Temporal & Comparable<? super T>> T cast(Temporal temporal) {
		return (T) temporal;
	}


	/**
	 * Represents the type of cron field, i.e. seconds, minutes, hours,
	 * day-of-month, month, day-of-week.
	 */
	protected enum Type {
		NANO(ChronoField.NANO_OF_SECOND, ChronoUnit.SECONDS),
		SECOND(ChronoField.SECOND_OF_MINUTE, ChronoUnit.MINUTES, ChronoField.NANO_OF_SECOND),
		MINUTE(ChronoField.MINUTE_OF_HOUR, ChronoUnit.HOURS, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		HOUR(ChronoField.HOUR_OF_DAY, ChronoUnit.DAYS, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		DAY_OF_MONTH(ChronoField.DAY_OF_MONTH, ChronoUnit.MONTHS, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		MONTH(ChronoField.MONTH_OF_YEAR, ChronoUnit.YEARS, ChronoField.DAY_OF_MONTH, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		DAY_OF_WEEK(ChronoField.DAY_OF_WEEK, ChronoUnit.WEEKS, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND);


		private final ChronoField field;

		private final ChronoUnit higherOrder;

		private final ChronoField[] lowerOrders;


		Type(ChronoField field, ChronoUnit higherOrder, ChronoField... lowerOrders) {
			this.field = field;
			this.higherOrder = higherOrder;
			this.lowerOrders = lowerOrders;
		}


		/**
		 * Return the value of this type for the given temporal.
		 * @return the value of this type
		 */
		public int get(Temporal date) {
			return date.get(this.field);
		}

		/**
		 * Return the general range of this type. For instance, this method
		 * will return 0-31 for {@link #MONTH}.
		 * @return the range of this field
		 */
		public ValueRange range() {
			return this.field.range();
		}

		/**
		 * Check whether the given value is valid, i.e. whether it falls in
		 * {@linkplain #range() range}.
		 * @param value the value to check
		 * @return the value that was passed in
		 * @throws IllegalArgumentException if the given value is invalid
		 */
		public int checkValidValue(int value) {
			if (this == DAY_OF_WEEK && value == 0) {
				return value;
			}
			else {
				try {
					return this.field.checkValidIntValue(value);
				}
				catch (DateTimeException ex) {
					throw new IllegalArgumentException(ex.getMessage(), ex);
				}
			}
		}

		/**
		 * Elapse the given temporal for the difference between the current
		 * value of this field and the goal value. Typically, the returned
		 * temporal will have the given goal as the current value for this type,
		 * but this is not the case for {@link #DAY_OF_MONTH}.
		 * @param temporal the temporal to elapse
		 * @param goal the goal value
		 * @param <T> the type of temporal
		 * @return the elapsed temporal, typically with {@code goal} as value
		 * for this type.
		 */
		public <T extends Temporal & Comparable<? super T>> T elapseUntil(T temporal, int goal) {
			int current = get(temporal);
			ValueRange range = temporal.range(this.field);
			if (current < goal) {
				if (range.isValidIntValue(goal)) {
					return cast(temporal.with(this.field, goal));
				}
				else {
					// goal is invalid, eg. 29th Feb, so roll forward
					long amount = range.getMaximum() - current + 1;
					return this.field.getBaseUnit().addTo(temporal, amount);
				}
			}
			else {
				long amount = goal + range.getMaximum() - current + 1 - range.getMinimum();
				return this.field.getBaseUnit().addTo(temporal, amount);
			}
		}

		/**
		 * Roll forward the give temporal until it reaches the next higher
		 * order field. Calling this method is equivalent to calling
		 * {@link #elapseUntil(Temporal, int)} with goal set to the
		 * minimum value of this field's range.
		 * @param temporal the temporal to roll forward
		 * @param <T> the type of temporal
		 * @return the rolled forward temporal
		 */
		public <T extends Temporal & Comparable<? super T>> T rollForward(T temporal) {
			T result = this.higherOrder.addTo(temporal, 1);
			ValueRange range = result.range(this.field);
			return this.field.adjustInto(result, range.getMinimum());
		}

		/**
		 * Reset this and all lower order fields of the given temporal to their
		 * minimum value. For instance for {@link #MINUTE}, this method
		 * resets nanos, seconds, <strong>and</strong> minutes to 0.
		 * @param temporal the temporal to reset
		 * @param <T> the type of temporal
		 * @return the reset temporal
		 */
		public <T extends Temporal> T reset(T temporal) {
			for (ChronoField lowerOrder : this.lowerOrders) {
				if (temporal.isSupported(lowerOrder)) {
					temporal = lowerOrder.adjustInto(temporal, temporal.range(lowerOrder).getMinimum());
				}
			}
			return temporal;
		}

		@Override
		public String toString() {
			return this.field.toString();
		}
	}

}
