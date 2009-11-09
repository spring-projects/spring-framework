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
 * Declares that a field should be formatted as a ISO date time.
 * @author Keith Donald
 * @since 3.0
 */
@Target( { ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ISODateTimeFormat {

	/**
	 * The ISO style to use to format the date time.
	 * Defaults to {@link Style#DATE_TIME}.
	 */
	Style value() default Style.DATE_TIME;

	public enum Style {
		
		/** 
		 * The most common ISO Date Format <code>yyyy-MM-dd</code> e.g. 2000-10-31.
		 */
		DATE,

		/** 
		 * The most common ISO Time Format <code>hh:mm:ss.SSSZ</code> e.g. 01:30:00.000-05:00.
		 */
		TIME,

		/** 
		 * The most common ISO DateTime Format <code>yyyy-MM-dd'T'hh:mm:ss.SSSZ</code> e.g. 2000-10-31 01:30:00.000-05:00.
		 * The default if no annotation value is specified.
		 */
		DATE_TIME
	}

}
