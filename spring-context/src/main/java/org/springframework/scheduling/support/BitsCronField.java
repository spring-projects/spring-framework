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
import java.time.temporal.Temporal;
import java.time.temporal.ValueRange;
import java.util.BitSet;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Efficient {@link BitSet}-based extension of {@link CronField}.
 * Created using the {@code parse*} methods.
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
final class BitsCronField extends CronField {

	private static final BitsCronField ZERO_NANOS;


	static {
		ZERO_NANOS = new BitsCronField(Type.NANO);
		ZERO_NANOS.bits.set(0);
	}

	private final BitSet bits;



	private BitsCronField(Type type) {
		super(type);
		this.bits = new BitSet((int) type.range().getMaximum());
	}

	/**
	 * Return a {@code BitsCronField} enabled for 0 nano seconds.
	 */
	public static BitsCronField zeroNanos() {
		return BitsCronField.ZERO_NANOS;
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
	 * Parse the given value into a hours {@code BitsCronField}, the third entry of a cron expression.
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
		BitSet bits = result.bits;
		if (bits.get(0)) {
			// cron supports 0 for Sunday; we use 7 like java.time
			bits.set(7);
			bits.clear(0);
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
				return ValueRange.of(min, max);
			}
		}
	}

	@Nullable
	@Override
	public <T extends Temporal & Comparable<? super T>> T nextOrSame(T temporal) {
		int current = type().get(temporal);
		int next = this.bits.nextSetBit(current);
		if (next == -1) {
			temporal = type().rollForward(temporal);
			next = this.bits.nextSetBit(0);
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
			}
			if (count >= CronExpression.MAX_ATTEMPTS) {
				return null;
			}
			return type().reset(temporal);
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
		if (!(o instanceof BitsCronField)) {
			return false;
		}
		BitsCronField other = (BitsCronField) o;
		return type() == other.type() &&
				this.bits.equals(other.bits);
	}

	@Override
	public String toString() {
		return type() + " " + this.bits;
	}

}
