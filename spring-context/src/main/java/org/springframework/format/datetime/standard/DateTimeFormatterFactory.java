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

package org.springframework.format.datetime.standard;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.TimeZone;

import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory that creates a JSR-310 {@link java.time.format.DateTimeFormatter}.
 *
 * <p>Formatters will be created using the defined {@link #setPattern pattern},
 * {@link #setIso ISO}, and <code>xxxStyle</code> methods (considered in that order).
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.0
 * @see #createDateTimeFormatter()
 * @see #createDateTimeFormatter(DateTimeFormatter)
 * @see #setPattern
 * @see #setIso
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see #setDateTimeStyle
 * @see DateTimeFormatterFactoryBean
 */
public class DateTimeFormatterFactory {

	@Nullable
	private String pattern;

	@Nullable
	private ISO iso;

	@Nullable
	private FormatStyle dateStyle;

	@Nullable
	private FormatStyle timeStyle;

	@Nullable
	private TimeZone timeZone;


	/**
	 * Create a new {@code DateTimeFormatterFactory} instance.
	 */
	public DateTimeFormatterFactory() {
	}

	/**
	 * Create a new {@code DateTimeFormatterFactory} instance.
	 * @param pattern the pattern to use to format date values
	 */
	public DateTimeFormatterFactory(String pattern) {
		this.pattern = pattern;
	}


	/**
	 * Set the pattern to use to format date values.
	 * @param pattern the format pattern
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Set the ISO format used to format date values.
	 * @param iso the ISO format
	 */
	public void setIso(ISO iso) {
		this.iso = iso;
	}

	/**
	 * Set the style to use for date types.
	 */
	public void setDateStyle(FormatStyle dateStyle) {
		this.dateStyle = dateStyle;
	}

	/**
	 * Set the style to use for time types.
	 */
	public void setTimeStyle(FormatStyle timeStyle) {
		this.timeStyle = timeStyle;
	}

	/**
	 * Set the style to use for date and time types.
	 */
	public void setDateTimeStyle(FormatStyle dateTimeStyle) {
		this.dateStyle = dateTimeStyle;
		this.timeStyle = dateTimeStyle;
	}

	/**
	 * Set the two characters to use to format date values, in Joda-Time style.
	 * <p>The first character is used for the date style; the second is for
	 * the time style. Supported characters are:
	 * <ul>
	 * <li>'S' = Small</li>
	 * <li>'M' = Medium</li>
	 * <li>'L' = Long</li>
	 * <li>'F' = Full</li>
	 * <li>'-' = Omitted</li>
	 * </ul>
	 * <p>This method mimics the styles supported by Joda-Time. Note that
	 * JSR-310 natively favors {@link java.time.format.FormatStyle} as used for
	 * {@link #setDateStyle}, {@link #setTimeStyle} and {@link #setDateTimeStyle}.
	 * @param style two characters from the set {"S", "M", "L", "F", "-"}
	 */
	public void setStylePattern(String style) {
		Assert.isTrue(style.length() == 2, "Style pattern must consist of two characters");
		this.dateStyle = convertStyleCharacter(style.charAt(0));
		this.timeStyle = convertStyleCharacter(style.charAt(1));
	}

	@Nullable
	private FormatStyle convertStyleCharacter(char c) {
		switch (c) {
			case 'S': return FormatStyle.SHORT;
			case 'M': return FormatStyle.MEDIUM;
			case 'L': return FormatStyle.LONG;
			case 'F': return FormatStyle.FULL;
			case '-': return null;
			default: throw new IllegalArgumentException("Invalid style character '" + c + "'");
		}
	}

	/**
	 * Set the {@code TimeZone} to normalize the date values into, if any.
	 * @param timeZone the time zone
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}


	/**
	 * Create a new {@code DateTimeFormatter} using this factory.
	 * <p>If no specific pattern or style has been defined,
	 * {@link FormatStyle#MEDIUM medium date time format} will be used.
	 * @return a new date time formatter
	 * @see #createDateTimeFormatter(DateTimeFormatter)
	 */
	public DateTimeFormatter createDateTimeFormatter() {
		return createDateTimeFormatter(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
	}

	/**
	 * Create a new {@code DateTimeFormatter} using this factory.
	 * <p>If no specific pattern or style has been defined,
	 * the supplied {@code fallbackFormatter} will be used.
	 * @param fallbackFormatter the fall-back formatter to use
	 * when no specific factory properties have been set
	 * @return a new date time formatter
	 */
	public DateTimeFormatter createDateTimeFormatter(DateTimeFormatter fallbackFormatter) {
		DateTimeFormatter dateTimeFormatter = null;
		if (StringUtils.hasLength(this.pattern)) {
			dateTimeFormatter = DateTimeFormatterUtils.createStrictDateTimeFormatter(this.pattern);
		}
		else if (this.iso != null && this.iso != ISO.NONE) {
			switch (this.iso) {
				case DATE:
					dateTimeFormatter = DateTimeFormatter.ISO_DATE;
					break;
				case TIME:
					dateTimeFormatter = DateTimeFormatter.ISO_TIME;
					break;
				case DATE_TIME:
					dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
					break;
				default:
					throw new IllegalStateException("Unsupported ISO format: " + this.iso);
			}
		}
		else if (this.dateStyle != null && this.timeStyle != null) {
			dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(this.dateStyle, this.timeStyle);
		}
		else if (this.dateStyle != null) {
			dateTimeFormatter = DateTimeFormatter.ofLocalizedDate(this.dateStyle);
		}
		else if (this.timeStyle != null) {
			dateTimeFormatter = DateTimeFormatter.ofLocalizedTime(this.timeStyle);
		}

		if (dateTimeFormatter != null && this.timeZone != null) {
			dateTimeFormatter = dateTimeFormatter.withZone(this.timeZone.toZoneId());
		}
		return (dateTimeFormatter != null ? dateTimeFormatter : fallbackFormatter);
	}

}
