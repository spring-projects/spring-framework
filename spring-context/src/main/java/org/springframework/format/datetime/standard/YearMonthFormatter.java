/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.format.datetime.standard;

import java.text.ParseException;
import java.time.YearMonth;
import java.util.Locale;

import org.springframework.format.Formatter;

/**
 * {@link Formatter} implementation for a JSR-310 {@link YearMonth},
 * following JSR-310's parsing rules for a YearMonth.
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
