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
 * Declares that a field should be formatted as a date time.
 * Supports formatting by {@link Style} or by pattern string.
 * <p>
 * For style-based formatting:
 * <ul>
 * <li>Set <code>dateStyle</code> attribute to specify the style of the <i>date</i> portion of the DateTime.
 * <li>Set <code>timeStyle</code> attribute to specify the style of the <i>time</i> portion of the DateTime.
 * <li>The default for both dateStyle and timeStyle if not specified is {@link Style#NONE}.
 * </ul>
 * For pattern-based formatting, set the <code>pattern</code> attribute to be the DateTime pattern, such as <code>yyyy/mm/dd h:mm:ss a</code>.
 * <p>
 * If no annotation attributes are specified, the default format applied is style-based with dateStyle={@link Style#SHORT} and timeStyle={@link Style#SHORT}.
 * 
 * @author Keith Donald
 * @since 3.0
 */
@Target( { ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface DateTimeFormat {

	/**
	 * The style to use for formatting the date portion of the property.
	 * Defaults to {@link Style#NONE}.
	 */
	Style dateStyle() default Style.NONE;

	/**
	 * The style to use for formatting the time portion of the property.
	 * Defaults to {@link Style#NONE}.
	 */
	Style timeStyle() default Style.NONE;

	/**
	 * A pattern String that defines a custom format for the property.
	 * Use this method or the <code>*Style</code> methods, not both.
	 */
	String pattern() default "";
	
	/**
	 * Supported DateTimeFormat styles.
	 */
	public enum Style {
		
		/**
		 * The short format style.
		 * <br>Example short dateStyle: Locale.US="M/d/yy" e.g. 10/31/2009
		 * <br>Example short timeStyle: Locale.US="h:mm a" e.g. 1:30 PM
		 */
		SHORT {
			public String toString() {
				return "S";
			}
		},
		
		/**
		 * The medium format style.
		 * <br>Example medium dateStyle: Locale.US="MMM d, yyyy" e.g Oct 31, 2009
		 * <br>Example medium timeStyle: Locale.US="h:mm:ss a" e.g. 1:30:00 PM
		 */
		MEDIUM {
			public String toString() {
				return "M";
			}
		},

		/**
		 * The long format style.
		 * <br>Example long dateStyle: Locale.US="MMMM d, yyyy" e.g October 31, 2009
		 * <br>Example long timeStyle: Locale.US="h:mm:ss a z" e.g. 1:30:00 PM Eastern Standard Time  
		 */
		LONG {
			public String toString() {
				return "L";
			}
		},

		/**
		 * The full format style.
		 * <br>Example full dateStyle: Locale.US="EEEE, MMMM d, yyyy" e.g. Saturday, October 31, 2009
		 * <br>Example full timeStyle: Locale.US="h:mm:ss a z" e.g 1:30:00 PM Eastern Standard Time
		 */
		FULL {
			public String toString() {
				return "F";
			}
		},

		/**
		 * The none format style.
		 * A dateStyle specified with this value results in date fields such as mm, dd, and yyyy being ignored.
		 * A timeStyle specified with this value results in time fields such as hh, mm being ignored.
		 * If both dateStyle and timeStyle are set to this value and the pattern attribute is also not specified,
		 * a default value for each is selected based on the type of field being annotated.
		 */
		NONE {
			public String toString() {
				return "-";
			}
		}		
	}
	
}
