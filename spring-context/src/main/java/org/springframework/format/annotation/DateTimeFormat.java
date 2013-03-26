/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.format.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a field should be formatted as a date time.
 * Supports formatting by style pattern, ISO date time pattern, or custom format pattern string.
 * Can be applied to {@code java.util.Date}, {@code java.util.Calendar}, {@code java.long.Long}, or Joda Time fields.
 * <p>
 * For style-based formatting, set the {@link #style()} attribute to be the style pattern code.
 * The first character of the code is the date style, and the second character is the time style.
 * Specify a character of 'S' for short style, 'M' for medium, 'L' for long, and 'F' for full.
 * A date or time may be omitted by specifying the style character '-'.
 * <p>
 * For ISO-based formatting, set the {@link #iso()} attribute to be the desired {@link ISO} format, such as {@link ISO#DATE}.
 <p>
 * For custom formatting, set the {@link #pattern()} attribute to be the DateTime pattern, such as {@code yyyy/MM/dd hh:mm:ss a}.
 * <p>
 * Each attribute is mutually exclusive, so only set one attribute per annotation instance (the one most convenient one for your formatting needs).
 * When the pattern attribute is specified, it takes precedence over both the style and ISO attribute.
 * When the iso attribute is specified, if takes precedence over the style attribute.
 * When no annotation attributes are specified, the default format applied is style-based with a style code of 'SS' (short date, short time).
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see org.joda.time.format.DateTimeFormat
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DateTimeFormat {

	/**
	 * The style pattern to use to format the field.
	 * Defaults to 'SS' for short date time.
	 * Set this attribute when you wish to format your field in accordance with a common style other than the default style.
	 */
	String style() default "SS";

	/**
	 * The ISO pattern to use to format the field.
	 * The possible ISO patterns are defined in the {@link ISO} enum.
	 * Defaults to ISO.NONE, indicating this attribute should be ignored.
	 * Set this attribute when you wish to format your field in accordance with an ISO date time format.
	 */
	ISO iso() default ISO.NONE;

	/**
	 * The custom pattern to use to format the field.
	 * Defaults to empty String, indicating no custom pattern String has been specified.
	 * Set this attribute when you wish to format your field in accordance with a custom date time pattern not represented by a style or ISO format.
	 */
	String pattern() default "";


	/**
	 * Common ISO date time format patterns.
	 */
	public enum ISO {

		/**
		 * The most common ISO Date Format {@code yyyy-MM-dd} e.g. 2000-10-31.
		 */
		DATE,

		/**
		 * The most common ISO Time Format {@code HH:mm:ss.SSSZ} e.g. 01:30:00.000-05:00.
		 */
		TIME,

		/**
		 * The most common ISO DateTime Format {@code yyyy-MM-dd'T'HH:mm:ss.SSSZ} e.g. 2000-10-31 01:30:00.000-05:00.
		 * The default if no annotation value is specified.
		 */
		DATE_TIME,

		/**
		 * Indicates that no ISO-based format pattern should be applied.
		 */
		NONE
	}

}
