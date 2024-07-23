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

package org.springframework.format.datetime.standard;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.format.annotation.DurationFormat;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Support {@code Duration} parsing and printing in several styles, as listed in
 * {@link DurationFormat.Style}.
 * <p>Some styles may not enforce any unit to be present, defaulting to {@code DurationFormat.Unit#MILLIS}
 * in that case. Methods in this class offer overloads that take a {@link DurationFormat.Unit} to
 * be used as a fall-back instead of the ultimate MILLIS default.
 *
 * @author Phillip Webb
 * @author Valentine Wu
 * @author Simon Baslé
 * @since 6.2
 */
public abstract class DurationFormatterUtils {

	private DurationFormatterUtils() {
		// singleton
	}

	/**
	 * Parse the given value to a duration.
	 * @param value the value to parse
	 * @param style the style in which to parse
	 * @return a duration
	 */
	public static Duration parse(String value, DurationFormat.Style style) {
		return parse(value, style, null);
	}

	/**
	 * Parse the given value to a duration.
	 * @param value the value to parse
	 * @param style the style in which to parse
	 * @param unit the duration unit to use if the value doesn't specify one ({@code null}
	 * will default to ms)
	 * @return a duration
	 */
	public static Duration parse(String value, DurationFormat.Style style, @Nullable DurationFormat.Unit unit) {
		return switch (style) {
			case ISO8601 -> parseIso8601(value);
			case SIMPLE -> parseSimple(value, unit);
		};
	}

	/**
	 * Print the specified duration in the specified style.
	 * @param value the value to print
	 * @param style the style to print in
	 * @return the printed result
	 */
	public static String print(Duration value, DurationFormat.Style style) {
		return print(value, style, null);
	}

	/**
	 * Print the specified duration in the specified style using the given unit.
	 * @param value the value to print
	 * @param style the style to print in
	 * @param unit the unit to use for printing, if relevant ({@code null} will default
	 * to ms)
	 * @return the printed result
	 */
	public static String print(Duration value, DurationFormat.Style style, @Nullable DurationFormat.Unit unit) {
		return switch (style) {
			case ISO8601 -> value.toString();
			case SIMPLE -> printSimple(value, unit);
		};
	}

	/**
	 * Detect the style then parse the value to return a duration.
	 * @param value the value to parse
	 * @return the parsed duration
	 * @throws IllegalArgumentException if the value is not a known style or cannot be
	 * parsed
	 */
	public static Duration detectAndParse(String value) {
		return detectAndParse(value, null);
	}

	/**
	 * Detect the style then parse the value to return a duration.
	 * @param value the value to parse
	 * @param unit the duration unit to use if the value doesn't specify one ({@code null}
	 * will default to ms)
	 * @return the parsed duration
	 * @throws IllegalArgumentException if the value is not a known style or cannot be
	 * parsed
	 */
	public static Duration detectAndParse(String value, @Nullable DurationFormat.Unit unit) {
		return parse(value, detect(value), unit);
	}

	/**
	 * Detect the style from the given source value.
	 * @param value the source value
	 * @return the duration style
	 * @throws IllegalArgumentException if the value is not a known style
	 */
	public static DurationFormat.Style detect(String value) {
		Assert.notNull(value, "Value must not be null");
		// warning: the order of parsing starts to matter if multiple patterns accept a plain integer (no unit suffix)
		if (ISO_8601_PATTERN.matcher(value).matches()) {
			return DurationFormat.Style.ISO8601;
		}
		if (SIMPLE_PATTERN.matcher(value).matches()) {
			return DurationFormat.Style.SIMPLE;
		}
		throw new IllegalArgumentException("'" + value + "' is not a valid duration, cannot detect any known style");
	}

	private static final Pattern ISO_8601_PATTERN = Pattern.compile("^[+-]?[pP].*$");
	private static final Pattern SIMPLE_PATTERN = Pattern.compile("^([+-]?\\d+)([a-zA-Z]{0,2})$");

	private static Duration parseIso8601(String value) {
		try {
			return Duration.parse(value);
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException("'" + value + "' is not a valid ISO-8601 duration", ex);
		}
	}

	private static Duration parseSimple(String text, @Nullable DurationFormat.Unit fallbackUnit) {
		try {
			Matcher matcher = SIMPLE_PATTERN.matcher(text);
			Assert.state(matcher.matches(), "Does not match simple duration pattern");
			String suffix = matcher.group(2);
			DurationFormat.Unit parsingUnit = (fallbackUnit == null ? DurationFormat.Unit.MILLIS : fallbackUnit);
			if (StringUtils.hasLength(suffix)) {
				parsingUnit = DurationFormat.Unit.fromSuffix(suffix);
			}
			return parsingUnit.parse(matcher.group(1));
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("'" + text + "' is not a valid simple duration", ex);
		}
	}

	private static String printSimple(Duration duration, @Nullable DurationFormat.Unit unit) {
		unit = (unit == null ? DurationFormat.Unit.MILLIS : unit);
		return unit.print(duration);
	}

}
