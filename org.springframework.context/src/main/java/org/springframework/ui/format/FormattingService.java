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

import org.springframework.core.convert.TypeDescriptor;

/**
 * A service interface for formatting localized field values.
 * This is the entry point into the <code>ui.format</code> system.
 *
 * @author Keith Donald
 * @since 3.0
 */
public interface FormattingService {

	/**
	 * Print the field value for display in the locale.
	 * @param fieldValue the field value
	 * @param fieldType the field type
	 * @param locale the user's locale
	 * @return the printed string
	 */
	String print(Object fieldValue, TypeDescriptor fieldType, Locale locale);

	/**
	 * Parse the the value submitted by the user. 
	 * @param submittedValue the submitted field value
	 * @param fieldType the field type
	 * @param locale the user's locale
	 * @return the parsed field value
	 */
	Object parse(String submittedValue, TypeDescriptor fieldType, Locale locale) throws ParseException;

}
