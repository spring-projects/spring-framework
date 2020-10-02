/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Locale;

import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;

import org.springframework.format.Printer;

/**
 * Prints Joda-Time {@link ReadableInstant} instances using a {@link DateTimeFormatter}.
 *
 * @author Keith Donald
 * @since 3.0
 * @deprecated as of 5.3, in favor of standard JSR-310 support
 */
@Deprecated
public final class ReadableInstantPrinter implements Printer<ReadableInstant> {

	private final DateTimeFormatter formatter;


	/**
	 * Create a new ReadableInstantPrinter.
	 * @param formatter the Joda DateTimeFormatter instance
	 */
	public ReadableInstantPrinter(DateTimeFormatter formatter) {
		this.formatter = formatter;
	}


	@Override
	public String print(ReadableInstant instant, Locale locale) {
		return JodaTimeContextHolder.getFormatter(this.formatter, locale).print(instant);
	}

}
