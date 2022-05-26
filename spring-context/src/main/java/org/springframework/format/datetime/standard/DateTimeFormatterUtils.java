/*
 * Copyright 2002-2021 the original author or authors.
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

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

import org.springframework.util.StringUtils;

/**
 * Internal {@link DateTimeFormatter} utilities.
 *
 * @author Juergen Hoeller
 * @since 5.3.5
 */
abstract class DateTimeFormatterUtils {

	static DateTimeFormatter createStrictDateTimeFormatter(String pattern) {
		// Using strict parsing to align with Joda-Time and standard DateFormat behavior:
		// otherwise, an overflow like e.g. Feb 29 for a non-leap-year wouldn't get rejected.
		// However, with strict parsing, a year digit needs to be specified as 'u'...
		String patternToUse = StringUtils.replace(pattern, "yy", "uu");
		return DateTimeFormatter.ofPattern(patternToUse).withResolverStyle(ResolverStyle.STRICT);
	}

}
