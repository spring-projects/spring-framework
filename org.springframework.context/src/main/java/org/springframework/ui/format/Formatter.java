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

package org.springframework.ui.format;

import java.text.ParseException;
import java.util.Locale;

/**
 * Formats objects of type T for display.
 *
 * @author Keith Donald
 * @since 3.0 
 * @param <T> the type of object this formatter can format
 */
public interface Formatter<T> {

	/**
	 * Format the object of type T for display.
	 * @param object the object to format
	 * @param locale the user's locale
	 * @return the formatted display string
	 */
	String format(T object, Locale locale);

	/**
	 * Parse an object from its formatted representation.
	 * @param formatted a formatted representation
	 * @param locale the user's locale
	 * @return the parsed object
	 * @throws ParseException when a parse exception occurs
	 * @throws RuntimeException when thrown by coercion methods that are
	 *
	 */
	T parse(String formatted, Locale locale) throws ParseException;

}
