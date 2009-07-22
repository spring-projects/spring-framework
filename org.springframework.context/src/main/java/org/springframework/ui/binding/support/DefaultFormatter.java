/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.ui.binding.support;

import java.text.ParseException;
import java.util.Locale;

import org.springframework.ui.format.Formatter;

@SuppressWarnings("unchecked")
// TODO should this delegate to type converter...
class DefaultFormatter implements Formatter {

	public static final Formatter INSTANCE = new DefaultFormatter();

	public String format(Object object, Locale locale) {
		if (object == null) {
			return "";
		} else {
			return object.toString();
		}
	}

	public Object parse(String formatted, Locale locale) throws ParseException {
		if (formatted == "") {
			return null;
		} else {
			return formatted;
		}
	}
}