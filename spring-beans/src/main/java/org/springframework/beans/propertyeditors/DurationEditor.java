/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Editor for {@link java.time.Duration}, translating simple or ISO-8601 duration
 * representation into {@code Duration} objects. Uses ISO-8601 standard for a text
 * representation.
 * <p>
 * This class supports same extensions to the ISO-8601 duration representation as
 * those supported by {@link Duration#parse(CharSequence)} method.
 * <p>
 * Additional, simple representation implemented by this class is of the form
 * {@code OptionalSign Amount Space Unit} where {@code OptionalSign} is an optional
 * sign (+, - or nothing), {@code Amount} is a number that fits into a long,
 * {@code Space} is zero or one ASCII space ('\\u0020') and {@code Unit} is one of
 * 'ms', 's', 'm', 'h', 'd' for milliseconds, seconds, minutes, hours and days
 * respectively, where a day should be understood as exactly 24 hours.
 *
 * @author Piotr Findeisen
 * @since 5.0.0
 */
public class DurationEditor extends PropertyEditorSupport {

	private static final Pattern SIMPLE_REPRESENTATION = Pattern.compile(
			"(?<sign>[+-]?)(?<amount>\\d+) ?(?<unit>ms|s|m|h|d)");

	private static final Map<String, TemporalUnit> units = buildUnits();

	private static Map<String, TemporalUnit> buildUnits() {
		Map<String, TemporalUnit> units = new HashMap<>();
		units.put("ms", ChronoUnit.MILLIS);
		units.put("s", ChronoUnit.SECONDS);
		units.put("m", ChronoUnit.MINUTES);
		units.put("h", ChronoUnit.HOURS);
		units.put("d", ChronoUnit.DAYS);
		return Collections.unmodifiableMap(units);
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(parseDuration(text));
	}

	/**
	 * Converts {@code text} into {@link Duration} accepting both simple representation (number and unit) or
	 * ISO-8601 standard.
	 */
	private Duration parseDuration(String text) {
		Matcher matcher = SIMPLE_REPRESENTATION.matcher(text);
		if (matcher.matches()) {
			int sign = "-".equals(matcher.group("sign")) ? -1 : 1;
			long amount = Long.parseLong(matcher.group("amount"));
			TemporalUnit unit = units.get(matcher.group("unit"));

			return Duration.of(sign * amount, unit);
		}

		return Duration.parse(text);
	}

	@Override
	public String getAsText() {
		Duration value = (Duration) getValue();
		if (value == null) {
			return null;
		}
		return formatDuration(value);
	}

	private String formatDuration(Duration value) {
		return value.toString();
	}

}
