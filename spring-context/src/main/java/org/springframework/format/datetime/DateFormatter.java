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

package org.springframework.format.datetime;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.springframework.format.Formatter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A formatter for {@link java.util.Date} types.
 *
 * <p>Supports the configuration of an explicit date time pattern, timezone,
 * locale, and fallback date time patterns for lenient parsing.
 *
 * <p>Common ISO patterns for UTC instants are applied at millisecond precision.
 * Note that {@link org.springframework.format.datetime.standard.InstantFormatter}
 * is recommended for flexible UTC parsing into a {@link java.time.Instant} instead.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 3.0
 * @see SimpleDateFormat
 */
public class DateFormatter implements Formatter<Date> {

	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

	private static final Map<ISO, String> ISO_PATTERNS;

	private static final Map<ISO, String> ISO_FALLBACK_PATTERNS;

	static {
		// We use an EnumMap instead of Map.of(...) since the former provides better performance.
		Map<ISO, String> formats = new EnumMap<>(ISO.class);
		formats.put(ISO.DATE, "yyyy-MM-dd");
		formats.put(ISO.TIME, "HH:mm:ss.SSSXXX");
		formats.put(ISO.DATE_TIME, "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		ISO_PATTERNS = Collections.unmodifiableMap(formats);

		// Fallback format for the time part without milliseconds.
		Map<ISO, String> fallbackFormats = new EnumMap<>(ISO.class);
		fallbackFormats.put(ISO.TIME, "HH:mm:ssXXX");
		fallbackFormats.put(ISO.DATE_TIME, "yyyy-MM-dd'T'HH:mm:ssXXX");
		ISO_FALLBACK_PATTERNS = Collections.unmodifiableMap(fallbackFormats);
	}


	@Nullable
	private Object source;

	@Nullable
	private String pattern;

	@Nullable
	private String[] fallbackPatterns;

	private int style = DateFormat.DEFAULT;

	@Nullable
	private String stylePattern;

	@Nullable
	private ISO iso;

	@Nullable
	private TimeZone timeZone;

	private boolean lenient = false;


	/**
	 * Create a new default {@code DateFormatter}.
	 */
	public DateFormatter() {
	}

	/**
	 * Create a new {@code DateFormatter} for the given date time pattern.
	 */
	public DateFormatter(String pattern) {
		this.pattern = pattern;
	}


	/**
	 * Set the source of the configuration for this {@code DateFormatter} &mdash;
	 * for example, an instance of the {@link DateTimeFormat @DateTimeFormat}
	 * annotation if such an annotation was used to configure this {@code DateFormatter}.
	 * <p>The supplied source object will only be used for descriptive purposes
	 * by invoking its {@code toString()} method &mdash; for example, when
	 * generating an exception message to provide further context.
	 * @param source the source of the configuration
	 * @since 5.3.5
	 */
	public void setSource(Object source) {
		this.source = source;
	}

	/**
	 * Set the pattern to use to format date values.
	 * <p>If not specified, DateFormat's default style will be used.
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Set additional patterns to use as a fallback in case parsing fails for the
	 * configured {@linkplain #setPattern pattern}, {@linkplain #setIso ISO format},
	 * {@linkplain #setStyle style}, or {@linkplain #setStylePattern style pattern}.
	 * @param fallbackPatterns the fallback parsing patterns
	 * @since 5.3.5
	 * @see DateTimeFormat#fallbackPatterns()
	 */
	public void setFallbackPatterns(String... fallbackPatterns) {
		this.fallbackPatterns = fallbackPatterns;
	}

	/**
	 * Set the ISO format to use to format date values.
	 * @param iso the {@link ISO} format
	 * @since 3.2
	 */
	public void setIso(ISO iso) {
		this.iso = iso;
	}

	/**
	 * Set the {@link DateFormat} style to use to format date values.
	 * <p>If not specified, DateFormat's default style will be used.
	 * @see DateFormat#DEFAULT
	 * @see DateFormat#SHORT
	 * @see DateFormat#MEDIUM
	 * @see DateFormat#LONG
	 * @see DateFormat#FULL
	 */
	public void setStyle(int style) {
		this.style = style;
	}

	/**
	 * Set the two characters to use to format date values.
	 * <p>The first character is used for the date style; the second is used for
	 * the time style.
	 * <p>Supported characters:
	 * <ul>
	 * <li>'S' = Small</li>
	 * <li>'M' = Medium</li>
	 * <li>'L' = Long</li>
	 * <li>'F' = Full</li>
	 * <li>'-' = Omitted</li>
	 * </ul>
	 * This method mimics the styles supported by Joda-Time.
	 * @param stylePattern two characters from the set {"S", "M", "L", "F", "-"}
	 * @since 3.2
	 */
	public void setStylePattern(String stylePattern) {
		this.stylePattern = stylePattern;
	}

	/**
	 * Set the {@link TimeZone} to normalize the date values into, if any.
	 */
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Specify whether parsing is to be lenient. Default is {@code false}.
	 * <p>With lenient parsing, the parser may allow inputs that do not precisely match the format.
	 * With strict parsing, inputs must match the format exactly.
	 */
	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}


	@Override
	public String print(Date date, Locale locale) {
		return getDateFormat(locale).format(date);
	}

	@Override
	public Date parse(String text, Locale locale) throws ParseException {
		try {
			return getDateFormat(locale).parse(text);
		}
		catch (ParseException ex) {
			Set<String> fallbackPatterns = new LinkedHashSet<>();
			String isoPattern = ISO_FALLBACK_PATTERNS.get(this.iso);
			if (isoPattern != null) {
				fallbackPatterns.add(isoPattern);
			}
			if (!ObjectUtils.isEmpty(this.fallbackPatterns)) {
				Collections.addAll(fallbackPatterns, this.fallbackPatterns);
			}
			if (!fallbackPatterns.isEmpty()) {
				for (String pattern : fallbackPatterns) {
					try {
						DateFormat dateFormat = configureDateFormat(new SimpleDateFormat(pattern, locale));
						// Align timezone for parsing format with printing format if ISO is set.
						if (this.iso != null && this.iso != ISO.NONE) {
							dateFormat.setTimeZone(UTC);
						}
						return dateFormat.parse(text);
					}
					catch (ParseException ignoredException) {
						// Ignore fallback parsing exceptions since the exception thrown below
						// will include information from the "source" if available -- for example,
						// the toString() of a @DateTimeFormat annotation.
					}
				}
			}
			if (this.source != null) {
				ParseException parseException = new ParseException(
						String.format("Unable to parse date time value \"%s\" using configuration from %s", text, this.source),
						ex.getErrorOffset());
				parseException.initCause(ex);
				throw parseException;
			}
			// else rethrow original exception
			throw ex;
		}
	}


	protected DateFormat getDateFormat(Locale locale) {
		return configureDateFormat(createDateFormat(locale));
	}

	private DateFormat configureDateFormat(DateFormat dateFormat) {
		if (this.timeZone != null) {
			dateFormat.setTimeZone(this.timeZone);
		}
		dateFormat.setLenient(this.lenient);
		return dateFormat;
	}

	private DateFormat createDateFormat(Locale locale) {
		if (StringUtils.hasLength(this.pattern)) {
			return new SimpleDateFormat(this.pattern, locale);
		}
		if (this.iso != null && this.iso != ISO.NONE) {
			String pattern = ISO_PATTERNS.get(this.iso);
			if (pattern == null) {
				throw new IllegalStateException("Unsupported ISO format " + this.iso);
			}
			SimpleDateFormat format = new SimpleDateFormat(pattern);
			format.setTimeZone(UTC);
			return format;
		}
		if (StringUtils.hasLength(this.stylePattern)) {
			int dateStyle = getStylePatternForChar(0);
			int timeStyle = getStylePatternForChar(1);
			if (dateStyle != -1 && timeStyle != -1) {
				return DateFormat.getDateTimeInstance(dateStyle, timeStyle, locale);
			}
			if (dateStyle != -1) {
				return DateFormat.getDateInstance(dateStyle, locale);
			}
			if (timeStyle != -1) {
				return DateFormat.getTimeInstance(timeStyle, locale);
			}
			throw new IllegalStateException("Unsupported style pattern '" + this.stylePattern + "'");

		}
		return DateFormat.getDateInstance(this.style, locale);
	}

	private int getStylePatternForChar(int index) {
		if (this.stylePattern != null && this.stylePattern.length() > index) {
			switch (this.stylePattern.charAt(index)) {
				case 'S': return DateFormat.SHORT;
				case 'M': return DateFormat.MEDIUM;
				case 'L': return DateFormat.LONG;
				case 'F': return DateFormat.FULL;
				case '-': return -1;
			}
		}
		throw new IllegalStateException("Unsupported style pattern '" + this.stylePattern + "'");
	}

}
