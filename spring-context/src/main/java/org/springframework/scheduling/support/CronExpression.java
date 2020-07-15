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

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Arrays;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Representation of a
 * <a href="https://www.manpagez.com/man/5/crontab/">crontab expression</a>
 * that can calculate the next time it matches.
 *
 * <p>{@code CronExpression} instances are created through
 * {@link #parse(String)}; the next match is determined with
 * {@link #next(Temporal)}.
 *
 * @author Arjen Poutsma
 * @since 5.3
 * @see CronTrigger
 */
public final class CronExpression {

	static final int MAX_ATTEMPTS = 366;


	private final CronField[] fields;

	private final String expression;


	private CronExpression(
			CronField seconds,
			CronField minutes,
			CronField hours,
			CronField daysOfMonth,
			CronField months,
			CronField daysOfWeek,
			String expression) {

		// to make sure we end up at 0 nanos, we add an extra field
		this.fields = new CronField[]{daysOfWeek, months, daysOfMonth, hours, minutes, seconds, CronField.zeroNanos()};
		this.expression = expression;
	}


	/**
	 * Parse the given
	 * <a href="https://www.manpagez.com/man/5/crontab/">crontab expression</a>
	 * string into a {@code CronExpression}.
	 * The string has six single space-separated time and date fields:
	 * <pre>
	 * &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; second (0-59)
	 * &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; minute (0 - 59)
	 * &#9474; &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; hour (0 - 23)
	 * &#9474; &#9474; &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; day of the month (1 - 31)
	 * &#9474; &#9474; &#9474; &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; month (1 - 12) (or JAN-DEC)
	 * &#9474; &#9474; &#9474; &#9474; &#9474; &#9484;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472;&#9472; day of the week (0 - 7)
	 * &#9474; &#9474; &#9474; &#9474; &#9474; &#9474;          (0 or 7 is Sunday, or MON-SUN)
	 * &#9474; &#9474; &#9474; &#9474; &#9474; &#9474;
	 * &#42; &#42; &#42; &#42; &#42; &#42;
	 * </pre>
	 *
	 * <p>The following rules apply:
	 * <ul>
	 * <li>
	 * A field may be an asterisk ({@code *}), which always stands for
	 * "first-last". For the "day of the month" or "day of the week" fields, a
	 * question mark ({@code ?}) may be used instead of an asterisk.
	 * </li>
	 * <li>
	 * Ranges of numbers are expressed by two numbers separated with a hyphen
	 * ({@code -}). The specified range is inclusive.
	 * </li>
	 * <li>Following a range (or {@code *}) with {@code "/n"} specifies
	 * skips of the number's value through the range.
	 * </li>
	 * <li>
	 * English names can also be used for the "month" and "day of week" fields.
	 * Use the first three letters of the particular day or month (case does not
	 * matter).
	 * </li>
	 * </ul>
	 *
	 * <p>Example expressions:
	 * <ul>
	 * <li>{@code "0 0 * * * *"} = the top of every hour of every day.</li>
	 * <li><code>"*&#47;10 * * * * *"</code> = every ten seconds.</li>
	 * <li>{@code "0 0 8-10 * * *"} = 8, 9 and 10 o'clock of every day.</li>
	 * <li>{@code "0 0 6,19 * * *"} = 6:00 AM and 7:00 PM every day.</li>
	 * <li>{@code "0 0/30 8-10 * * *"} = 8:00, 8:30, 9:00, 9:30, 10:00 and 10:30 every day.</li>
	 * <li>{@code "0 0 9-17 * * MON-FRI"} = on the hour nine-to-five weekdays</li>
	 * <li>{@code "0 0 0 25 12 ?"} = every Christmas Day at midnight</li>
	 * </ul>
	 *
	 * @param expression the expression string to parse
	 * @return the parsed {@code CronExpression} object
	 * @throws IllegalArgumentException in the expression does not conform to
	 * the cron format
	 */
	public static CronExpression parse(String expression) {
		Assert.hasLength(expression, "Expression string must not be empty");

		String[] fields = StringUtils.tokenizeToStringArray(expression, " ");
		if (fields.length != 6) {
			throw new IllegalArgumentException(String.format(
					"Cron expression must consist of 6 fields (found %d in \"%s\")", fields.length, expression));
		}
		try {
			CronField seconds = CronField.parseSeconds(fields[0]);
			CronField minutes = CronField.parseMinutes(fields[1]);
			CronField hours = CronField.parseHours(fields[2]);
			CronField daysOfMonth = CronField.parseDaysOfMonth(fields[3]);
			CronField months = CronField.parseMonth(fields[4]);
			CronField daysOfWeek = CronField.parseDaysOfWeek(fields[5]);

			return new CronExpression(seconds, minutes, hours, daysOfMonth, months, daysOfWeek, expression);
		}
		catch (IllegalArgumentException ex) {
			String msg = ex.getMessage() + " in cron expression \"" + expression + "\"";
			throw new IllegalArgumentException(msg, ex);
		}
	}


	/**
	 * Calculate the next {@link Temporal} that matches this expression.
	 * @param temporal the seed value
	 * @param <T> the type of temporal
	 * @return the next temporal that matches this expression, or {@code null}
	 * if no such temporal can be found
	 */
	@Nullable
	public <T extends Temporal> T next(T temporal) {
		return nextOrSame(ChronoUnit.NANOS.addTo(temporal, 1));
	}


	@Nullable
	private <T extends Temporal> T nextOrSame(T temporal) {
		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			T result = nextOrSameInternal(temporal);
			if (result == null || result.equals(temporal)) {
				return result;
			}
			temporal = result;
		}
		return null;
	}

	@Nullable
	private <T extends Temporal> T nextOrSameInternal(T temporal) {
		for (CronField field : this.fields) {
			temporal = field.nextOrSame(temporal);
			if (temporal == null) {
				return null;
			}
		}
		return temporal;
	}


	@Override
	public int hashCode() {
		return Arrays.hashCode(this.fields);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof CronExpression) {
			CronExpression other = (CronExpression) o;
			return Arrays.equals(this.fields, other.fields);
		}
		else {
			return false;
		}
	}

	/**
	 * Return the expression string used to create this {@code CronExpression}.
	 * @return the expression string
	 */
	@Override
	public String toString() {
		return this.expression;
	}

}
