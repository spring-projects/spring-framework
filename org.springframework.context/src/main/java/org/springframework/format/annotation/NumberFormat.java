/*
 * Copyright 2002-2009 the original author or authors.
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
 * Declares that a field should be formatted as a number.
 * Supports formatting by style or custom pattern string.
 * Can be applied to any JDK <code>java.lang.Number</code> type.
 * <p>
 * For style-based formatting, set the {@link #style()} attribute to be the desired {@link Style}.  
 * For custom formatting, set the {@link #pattern()} attribute to be the number pattern, such as <code>#,###.##</code>.
 * <p>
 * Each attribute is mutually exclusive, so only set one attribute per annotation instance (the one most convenient one for your formatting needs).
 * When the pattern attribute is specified, it takes precedence over the style attribute.
 * When no annotation attributes are specified, the default format applied is style-based with a style of {@link Style#NUMBER}.
 * 
 * @author Keith Donald
 * @since 3.0
 * @see java.text.NumberFormat
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface NumberFormat {

	/**
	 * The style pattern to use to format the field.
	 * Defaults to {@link Style#NUMBER} for general-purpose number formatter.
	 * Set this attribute when you wish to format your field in accordance with a common style other than the default style.
	 */
	Style style() default Style.NUMBER;

	/**
	 * The custom pattern to use to format the field.
	 * Defaults to empty String, indicating no custom pattern String has been specified.
	 * Set this attribute when you wish to format your field in accordance with a custom number pattern not represented by a style.
	 */
	String pattern() default "";


	/**
	 * Common number format styles.
	 * @author Keith Donald
	 * @since 3.0
	 */
	public enum Style {

		/**
		 * The general-purpose number format for the current locale.
		 */
		NUMBER,
		
		/**
		 * The currency format for the current locale.
		 */
		CURRENCY,

		/**
		 * The percent format for the current locale.
		 */
		PERCENT
	}

}
