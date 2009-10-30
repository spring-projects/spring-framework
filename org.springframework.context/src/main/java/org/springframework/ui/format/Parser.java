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
 * Parses objects of type T from their printed representations.
 *
 * @author Keith Donald
 * @since 3.0 
 * @param <T> the type of object this Parser parses
 */
public interface Parser<T> {

	/**
	 * Parse an object from its printed representation.
	 * @param printed a printed representation
	 * @param locale the current user locale
	 * @return the parsed object
	 * @throws ParseException when a parse exception occurs in a java.text parsing library
	 * @throws RuntimeException when a parse exception occurs
	 */
	T parse(String printed, Locale locale) throws ParseException;

}
