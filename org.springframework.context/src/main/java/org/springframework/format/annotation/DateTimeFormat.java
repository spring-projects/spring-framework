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
 * @author Keith Donald
 * @since 3.0
 */
@Target( { ElementType.METHOD, ElementType.FIELD })
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
		SHORT {
			public String toString() {
				return "S";
			}
		},
		MEDIUM {
			public String toString() {
				return "M";
			}
		},
		LONG {
			public String toString() {
				return "L";
			}
		},
		FULL {
			public String toString() {
				return "F";
			}
		},
		NONE {
			public String toString() {
				return "-";
			}
		}		
	}
	
}
