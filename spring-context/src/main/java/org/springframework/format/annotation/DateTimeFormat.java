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

package org.springframework.format.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a field or method parameter should be formatted as a date or time.
 *
 * <p>Supports formatting by style pattern, ISO date time pattern, or custom format pattern string.
 * Can be applied to {@link java.util.Date}, {@link java.util.Calendar}, {@link Long} (for
 * millisecond timestamps) as well as JSR-310 {@code java.time} value types.
 *
 * <p>For style-based formatting, set the {@link #style} attribute to the desired style pattern code.
 * The first character of the code is the date style, and the second character is the time style.
 * Specify a character of 'S' for short style, 'M' for medium, 'L' for long, and 'F' for full.
 * The date or time may be omitted by specifying the style character '-' &mdash; for example,
 * 'M-' specifies a medium format for the date with no time.
 *
 * <p>For ISO-based formatting, set the {@link #iso} attribute to the desired {@link ISO} format,
 * such as {@link ISO#DATE}.
 *
 * <p>For custom formatting, set the {@link #pattern} attribute to a date time pattern, such as
 * {@code "yyyy/MM/dd hh:mm:ss a"}.
 *
 * <p>Each attribute is mutually exclusive, so only set one attribute per annotation instance
 * (the one most convenient for your formatting needs).
 *
 * <ul>
 * <li>When the pattern attribute is specified, it takes precedence over both the style and ISO attribute.</li>
 * <li>When the {@link #iso} attribute is specified, it takes precedence over the style attribute.</li>
 * <li>When no annotation attributes are specified, the default format applied is style-based
 * with a style code of 'SS' (short date, short time).</li>
 * </ul>
 *
 * <h3>Time Zones</h3>
 * <p>Whenever the {@link #style} or {@link #pattern} attribute is used, the
 * {@linkplain java.util.TimeZone#getDefault() default time zone} of the JVM will
 * be used when formatting {@link java.util.Date} values. Whenever the {@link #iso}
 * attribute is used when formatting {@link java.util.Date} values, {@code UTC}
 * will be used as the time zone. The same time zone will be applied to any
 * {@linkplain #fallbackPatterns fallback patterns} as well. In order to enforce
 * consistent use of {@code UTC} as the time zone, you can bootstrap the JVM with
 * {@code -Duser.timezone=UTC}.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 * @see java.time.format.DateTimeFormatter
 * @see org.joda.time.format.DateTimeFormat
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface DateTimeFormat {

	/**
	 * The style pattern to use to format the field or method parameter.
	 * <p>Defaults to 'SS' for short date, short time. Set this attribute when you
	 * wish to format your field or method parameter in accordance with a common
	 * style other than the default style.
	 * @see #fallbackPatterns
	 */
	String style() default "SS";

	/**
	 * The ISO pattern to use to format the field or method parameter.
	 * <p>Supported ISO patterns are defined in the {@link ISO} enum.
	 * <p>Defaults to {@link ISO#NONE}, indicating this attribute should be ignored.
	 * Set this attribute when you wish to format your field or method parameter
	 * in accordance with an ISO format.
	 * @see #fallbackPatterns
	 */
	ISO iso() default ISO.NONE;

	/**
	 * The custom pattern to use to format the field or method parameter.
	 * <p>Defaults to empty String, indicating no custom pattern String has been
	 * specified. Set this attribute when you wish to format your field or method
	 * parameter in accordance with a custom date time pattern not represented by
	 * a style or ISO format.
	 * <p>Note: This pattern follows the original {@link java.text.SimpleDateFormat} style,
	 * as also supported by Joda-Time, with strict parsing semantics towards overflows
	 * (e.g. rejecting a Feb 29 value for a non-leap-year). As a consequence, 'yy'
	 * characters indicate a year in the traditional style, not a "year-of-era" as in the
	 * {@link java.time.format.DateTimeFormatter} specification (i.e. 'yy' turns into 'uu'
	 * when going through a {@code DateTimeFormatter} with strict resolution mode).
	 * @see #fallbackPatterns
	 */
	String pattern() default "";

	/**
	 * The set of custom patterns to use as a fallback in case parsing fails for
	 * the primary {@link #pattern}, {@link #iso}, or {@link #style} attribute.
	 * <p>For example, if you wish to use the ISO date format for parsing and
	 * printing but allow for lenient parsing of user input for various date
	 * formats, you could configure something similar to the following.
	 * <pre style="code">
	 * {@literal @}DateTimeFormat(iso = ISO.DATE, fallbackPatterns = { "M/d/yy", "dd.MM.yyyy" })
	 * </pre>
	 * <p>Fallback patterns are only used for parsing. They are not used for
	 * printing the value as a String. The primary {@link #pattern}, {@link #iso},
	 * or {@link #style} attribute is always used for printing. For details on
	 * which time zone is used for fallback patterns, see the
	 * {@linkplain DateTimeFormat class-level documentation}.
	 * <p>Fallback patterns are not supported for Joda-Time value types.
	 * @since 5.3.5
	 */
	String[] fallbackPatterns() default {};


	/**
	 * Common ISO date time format patterns.
	 */
	enum ISO {

		/**
		 * The most common ISO Date Format {@code yyyy-MM-dd} &mdash; for example,
		 * "2000-10-31".
		 */
		DATE,

		/**
		 * The most common ISO Time Format {@code HH:mm:ss.SSSXXX} &mdash; for example,
		 * "01:30:00.000-05:00".
		 */
		TIME,

		/**
		 * The most common ISO Date Time Format {@code yyyy-MM-dd'T'HH:mm:ss.SSSXXX}
		 * &mdash; for example, "2000-10-31T01:30:00.000-05:00".
		 */
		DATE_TIME,

		/**
		 * Indicates that no ISO-based format pattern should be applied.
		 */
		NONE
	}

}
