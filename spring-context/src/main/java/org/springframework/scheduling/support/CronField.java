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

import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.ValueRange;
import java.util.BitSet;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A single field in a cron pattern. Created using the {@code parse*} methods,
 * main and only entry point is {@link #nextOrSame(Temporal)}.
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
final class CronField {

	private static final String[] MONTHS = new String[]{"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP",
			"OCT", "NOV", "DEC"};

	private static final String[] DAYS = new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

	private static final CronField ZERO_NANOS;


	static {
		ZERO_NANOS = new CronField(Type.NANO);
		ZERO_NANOS.bits.set(0);
	}


	private final Type type;

	private final BitSet bits;


	private CronField(Type type) {
		this.type = type;
		this.bits = new BitSet((int) type.range().getMaximum());
	}


	/**
	 * Return a {@code CronField} enabled for 0 nano seconds.
	 */
	public static CronField zeroNanos() {
		return ZERO_NANOS;
	}

	/**
	 * Parse the given value into a seconds {@code CronField}, the first entry of a cron expression.
	 */
	public static CronField parseSeconds(String value) {
		return parseField(value, Type.SECOND);
	}

	/**
	 * Parse the given value into a minutes {@code CronField}, the second entry of a cron expression.
	 */
	public static CronField parseMinutes(String value) {
		return parseField(value, Type.MINUTE);
	}

	/**
	 * Parse the given value into a hours {@code CronField}, the third entry of a cron expression.
	 */
	public static CronField parseHours(String value) {
		return parseField(value, Type.HOUR);
	}

	/**
	 * Parse the given value into a days of months {@code CronField}, the fourth entry of a cron expression.
	 */
	public static CronField parseDaysOfMonth(String value) {
		return parseDate(value, Type.DAY_OF_MONTH);
	}

	/**
	 * Parse the given value into a month {@code CronField}, the fifth entry of a cron expression.
	 */
	public static CronField parseMonth(String value) {
		value = replaceOrdinals(value, MONTHS);
		return parseField(value, Type.MONTH);
	}

	/**
	 * Parse the given value into a days of week {@code CronField}, the sixth entry of a cron expression.
	 */
	public static CronField parseDaysOfWeek(String value) {
		value = replaceOrdinals(value, DAYS);
		CronField result = parseDate(value, Type.DAY_OF_WEEK);
		if (result.bits.get(0)) {
			// cron supports 0 for Sunday; we use 7 like java.time
			result.bits.set(7);
			result.bits.clear(0);
		}
		return result;
	}


	private static CronField parseDate(String value, Type type) {
		if (value.indexOf('?') != -1) {
			value = "*";
		}
		return parseField(value, type);
	}

	private static CronField parseField(String value, Type type) {
		Assert.hasLength(value, "Value must not be empty");
		Assert.notNull(type, "Type must not be null");
		try {
			CronField result = new CronField(type);
			String[] fields = StringUtils.delimitedListToStringArray(value, ",");
			for (String field : fields) {
				int slashPos = field.indexOf('/');
				if (slashPos == -1) {
					ValueRange range = parseRange(field, type);
					result.setBits(range);
				}
				else {
					String rangeStr = value.substring(0, slashPos);
					String deltaStr = value.substring(slashPos + 1);
					ValueRange range = parseRange(rangeStr, type);
					if (rangeStr.indexOf('-') == -1) {
						range = ValueRange.of(range.getMinimum(), type.range().getMaximum());
					}
					int delta = Integer.parseInt(deltaStr);
					if (delta <= 0) {
						throw new IllegalArgumentException("Incrementer delta must be 1 or higher");
					}
					result.setBits(range, delta);
				}
			}
			return result;
		}
		catch (DateTimeException | IllegalArgumentException ex) {
			String msg = ex.getMessage() + " '" + value + "'";
			throw new IllegalArgumentException(msg, ex);
		}
	}

	private static ValueRange parseRange(String value, Type type) {
		if (value.indexOf('*') != -1) {
			return type.range();
		}
		else {
			int hyphenPos = value.indexOf('-');
			if (hyphenPos == -1) {
				int result = type.checkValidValue(Integer.parseInt(value));
				return ValueRange.of(result, result);
			}
			else {
				int min = Integer.parseInt(value.substring(0, hyphenPos));
				int max = Integer.parseInt(value.substring(hyphenPos + 1));
				min = type.checkValidValue(min);
				max = type.checkValidValue(max);
				return ValueRange.of(min, max);
			}
		}
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
	public <T extends Temporal> T nextOrSame(T temporal) {
		int current = this.type.get(temporal);
		int next = this.bits.nextSetBit(current);
		if (next == -1) {
			temporal = this.type.rollForward(temporal);
			next = this.bits.nextSetBit(0);
		}
		if (next == current) {
			return temporal;
		}
		else {
			int count = 0;
			current = this.type.get(temporal);
			while (current != next && count++ < CronExpression.MAX_ATTEMPTS) {
				temporal = this.type.elapseUntil(temporal, next);
				current = this.type.get(temporal);
			}
			if (count >= CronExpression.MAX_ATTEMPTS) {
				return null;
			}
			return this.type.reset(temporal);
		}
	}


	BitSet bits() {
		return this.bits;
	}

	private void setBits(ValueRange range) {
		this.bits.set((int) range.getMinimum(), (int) range.getMaximum() + 1);
	}

	private void setBits(ValueRange range, int delta) {
		for (int i = (int) range.getMinimum(); i <= range.getMaximum(); i += delta) {
			this.bits.set(i);
		}
	}


	@Override
	public int hashCode() {
		return this.bits.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CronField)) {
			return false;
		}
		CronField other = (CronField) o;
		return this.type == other.type &&
				this.bits.equals(other.bits);
	}

	@Override
	public String toString() {
		return this.type + " " + this.bits;
	}


	/**
	 * Represents the type of cron field, i.e. seconds, minutes, hours,
	 * day-of-month, month, day-of-week.
	 */
	private enum Type {
		NANO(ChronoField.NANO_OF_SECOND),
		SECOND(ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		MINUTE(ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		HOUR(ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		DAY_OF_MONTH(ChronoField.DAY_OF_MONTH, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		MONTH(ChronoField.MONTH_OF_YEAR, ChronoField.DAY_OF_MONTH, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND),
		DAY_OF_WEEK(ChronoField.DAY_OF_WEEK, ChronoField.HOUR_OF_DAY, ChronoField.MINUTE_OF_HOUR, ChronoField.SECOND_OF_MINUTE, ChronoField.NANO_OF_SECOND);


		private final ChronoField field;

		private final ChronoField[] lowerOrders;


		Type(ChronoField field, ChronoField... lowerOrders) {
			this.field = field;
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
		 * Return the general range of this type. For instance, this methods
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
		 * but this is not the case for {@link #DAY_OF_MONTH}. For instance,
		 * if {@code goal} is 31, and {@code temporal} is April 16th,
		 * this method returns May 1st, because April 31st does not exist.
		 * @param temporal the temporal to elapse
		 * @param goal the goal value
		 * @param <T> the type of temporal
		 * @return the elapsed temporal, typically with {@code goal} as value
		 * for this type.
		 */
		public <T extends Temporal> T elapseUntil(T temporal, int goal) {
			int current = get(temporal);
			if (current < goal) {
				return this.field.getBaseUnit().addTo(temporal, goal - current);
			}
			else {
				ValueRange range = temporal.range(this.field);
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
		public <T extends Temporal> T rollForward(T temporal) {
			int current = get(temporal);
			ValueRange range = temporal.range(this.field);
			long amount = range.getMaximum() - current + 1;
			return this.field.getBaseUnit().addTo(temporal, amount);
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
	}

}
