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

import java.text.ParseException;
import java.time.Duration;
import java.util.Locale;

import org.springframework.format.Formatter;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.lang.Nullable;

/**
 * {@link Formatter} implementation for a JSR-310 {@link Duration},
 * following JSR-310's parsing rules for a Duration by default and
 * supporting additional {@code DurationFormat.Style} styles.
 *
 * @author Juergen Hoeller
 * @since 6.2
 * @see DurationFormatterUtils
 * @see DurationFormat.Style
 */
public class DurationFormatter implements Formatter<Duration> {

	private final DurationFormat.Style style;
	@Nullable
	private final DurationFormat.Unit defaultUnit;

	/**
	 * Create a {@code DurationFormatter} following JSR-310's parsing rules for a Duration
	 * (the {@link DurationFormat.Style#ISO8601 ISO-8601} style).
	 */
	DurationFormatter() {
		this(DurationFormat.Style.ISO8601);
	}

	/**
	 * Create a {@code DurationFormatter} in a specific {@link DurationFormat.Style}.
	 * <p>When a unit is needed but cannot be determined (e.g. printing a Duration in the
	 * {@code SIMPLE} style), {@code DurationFormat.Unit#MILLIS} is used.
	 */
	public DurationFormatter(DurationFormat.Style style) {
		this(style, null);
	}

	/**
	 * Create a {@code DurationFormatter} in a specific {@link DurationFormat.Style} with an
	 * optional {@code DurationFormat.Unit}.
	 * <p>If a {@code defaultUnit} is specified, it may be used in parsing cases when no
	 * unit is present in the string (provided the style allows for such a case). It will
	 * also be used as the representation's resolution when printing in the
	 * {@link DurationFormat.Style#SIMPLE} style. Otherwise, the style defines its default
	 * unit.
	 *
	 * @param style the {@code DurationStyle} to use
	 * @param defaultUnit the {@code DurationFormat.Unit} to fall back to when parsing and printing
	 */
	public DurationFormatter(DurationFormat.Style style, @Nullable DurationFormat.Unit defaultUnit) {
		this.style = style;
		this.defaultUnit = defaultUnit;
	}

	@Override
	public Duration parse(String text, Locale locale) throws ParseException {
		if (this.defaultUnit == null) {
			//delegate to the style
			return DurationFormatterUtils.parse(text, this.style);
		}
		return DurationFormatterUtils.parse(text, this.style, this.defaultUnit);
	}

	@Override
	public String print(Duration object, Locale locale) {
		if (this.defaultUnit == null) {
			//delegate the ultimate of the default unit to the style
			return DurationFormatterUtils.print(object, this.style);
		}
		return DurationFormatterUtils.print(object, this.style, this.defaultUnit);
	}

}
