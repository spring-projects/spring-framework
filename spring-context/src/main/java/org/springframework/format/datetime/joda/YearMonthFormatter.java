/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.format.datetime.joda;

import java.text.ParseException;
import java.util.Locale;

import org.joda.time.YearMonth;

import org.springframework.format.Formatter;

/**
 * {@link Formatter} implementation for a Joda-Time {@link YearMonth},
 * following Joda-Time's parsing rules for a YearMonth.
 *
 * @author Juergen Hoeller
 * @since 4.2.4
 * @see YearMonth#parse
 */
class YearMonthFormatter implements Formatter<YearMonth> {

	@Override
	public YearMonth parse(String text, Locale locale) throws ParseException {
		return YearMonth.parse(text);
	}

	@Override
	public String print(YearMonth object, Locale locale) {
		return object.toString();
	}

}
