/*
 * Copyright 2002-2024 the original author or authors.
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
import java.time.temporal.Temporal;
import java.time.temporal.ValueRange;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Efficient bitwise-operator extension of {@link CronField}.
 * Created using the {@code parse*} methods.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.3
 */
final class BitsCronField extends CronField {

	public static final BitsCronField ZERO_NANOS = forZeroNanos();

	private static final long MASK = 0xFFFFFFFFFFFFFFFFL;

	// we store at most 60 bits, for seconds and minutes, so a 64-bit long suffices
	private long bits;


	private BitsCronField(Type type) {
		super(type);
	}


	/**
	 * Return a {@code BitsCronField} enabled for 0 nanoseconds.
	 */
	private static BitsCronField forZeroNanos() {
		BitsCronField field = new BitsCronField(Type.NANO);
		field.setBit(0);
		return field;
	}

	/**
	 * Parse the given value into a seconds {@code BitsCronField}, the first entry of a cron expression.
	 */
	public static BitsCronField parseSeconds(String value) {
		return parseField(value, Type.SECOND);
	}

	/**
	 * Parse the given value into a minutes {@code BitsCronField}, the second entry of a cron expression.
	 */
	public static BitsCronField parseMinutes(String value) {
		return BitsCronField.parseField(value, Type.MINUTE);
	}

	/**
	 * Parse the given value into an hours {@code BitsCronField}, the third entry of a cron expression.
	 */
	public static BitsCronField parseHours(String value) {
		return BitsCronField.parseField(value, Type.HOUR);
	}

	/**
	 * Parse the given value into a days of months {@code BitsCronField}, the fourth entry of a cron expression.
	 */
	public static BitsCronField parseDaysOfMonth(String value) {
		return parseDate(value, Type.DAY_OF_MONTH);
	}

	/**
	 * Parse the given value into a month {@code BitsCronField}, the fifth entry of a cron expression.
	 */
	public static BitsCronField parseMonth(String value) {
		return BitsCronField.parseField(value, Type.MONTH);
	}

	/**
	 * Parse the given value into a days of week {@code BitsCronField}, the sixth entry of a cron expression.
	 */
	public static BitsCronField parseDaysOfWeek(String value) {
		BitsCronField result = parseDate(value, Type.DAY_OF_WEEK);
		if (result.getBit(0)) {
			// cron supports 0 for Sunday; we use 7 like java.time
			result.setBit(7);
			result.clearBit(0);
		}
		return result;
	}

	private static BitsCronField parseDate(String value, BitsCronField.Type type) {
		if (value.equals("?")) {
			value = "*";
		}
		return BitsCronField.parseField(value, type);
	}

	private static BitsCronField parseField(String value, Type type) {
		Assert.hasLength(value, "Value must not be empty");
		Assert.notNull(type, "Type must not be null");
		try {
			BitsCronField result = new BitsCronField(type);
			String[] fields = StringUtils.delimitedListToStringArray(value, ",");
			for (String field : fields) {
				int slashPos = field.indexOf('/');
				if (slashPos == -1) {
					ValueRange range = parseRange(field, type);
					result.setBits(range);
				}
				else {
					String rangeStr = field.substring(0, slashPos);
					String deltaStr = field.substring(slashPos + 1);
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
		if (value.equals("*")) {
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
				if (type == Type.DAY_OF_WEEK && min == 7) {
					// If used as a minimum in a range, Sunday means 0 (not 7)
					min = 0;
				}
				return ValueRange.of(min, max);
			}
		}
	}


	@Nullable
	@Override
	public <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal) {
		int current = type().get(temporal);
		int next = nextSetBit(current);
		if (next == -1) {
			temporal = type().rollForward(temporal);
			next = nextSetBit(0);
		}
		if (next == current) {
			return temporal;
		}
		else {
			int count = 0;
			current = type().get(temporal);
			while (current != next && count++ < CronExpression.MAX_ATTEMPTS) {
				temporal = type().elapseUntil(temporal, next);
				current = type().get(temporal);
				next = nextSetBit(current);
				if (next == -1) {
					temporal = type().rollForward(temporal);
					next = nextSetBit(0);
				}
			}
			if (count >= CronExpression.MAX_ATTEMPTS) {
				return null;
			}
			return type().reset(temporal);
		}
	}

	boolean getBit(int index) {
		return (this.bits & (1L << index)) != 0;
	}

	private int nextSetBit(int fromIndex) {
		long result = this.bits & (MASK << fromIndex);
		if (result != 0) {
			return Long.numberOfTrailingZeros(result);
		}
		else {
			return -1;
		}
	}

	private void setBits(ValueRange range) {
		if (range.getMinimum() == range.getMaximum()) {
			setBit((int) range.getMinimum());
		}
		else {
			long minMask = MASK << range.getMinimum();
			long maxMask = MASK >>> - (range.getMaximum() + 1);
			this.bits |= (minMask & maxMask);
		}
	}

	private void setBits(ValueRange range, int delta) {
		if (delta == 1) {
			setBits(range);
		}
		else {
			for (int i = (int) range.getMinimum(); i <= range.getMaximum(); i += delta) {
				setBit(i);
			}
		}
	}

	private void setBit(int index) {
		this.bits |= (1L << index);
	}

	private void clearBit(int index) {
		this.bits &= ~(1L << index);
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BitsCronField)) {
			return false;
		}
		BitsCronField otherField = (BitsCronField) other;
		return (type() == otherField.type() && this.bits == otherField.bits);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.bits);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(type().toString());
		builder.append(" {");
		int i = nextSetBit(0);
		if (i != -1) {
			builder.append(i);
			i = nextSetBit(i+1);
			while (i != -1) {
				builder.append(", ");
				builder.append(i);
				i = nextSetBit(i+1);
			}
		}
		builder.append('}');
		return builder.toString();
	}

}
