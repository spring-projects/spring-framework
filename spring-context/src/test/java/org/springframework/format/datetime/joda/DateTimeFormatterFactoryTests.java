/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import org.springframework.format.annotation.DateTimeFormat.ISO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Phillip Webb
 * @author Sam Brannen
 */
public class DateTimeFormatterFactoryTests {

	// Potential test timezone, both have daylight savings on October 21st
	private static final TimeZone ZURICH = TimeZone.getTimeZone("Europe/Zurich");
	private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

	// Ensure that we are testing against a timezone other than the default.
	private static final TimeZone TEST_TIMEZONE = ZURICH.equals(TimeZone.getDefault()) ? NEW_YORK : ZURICH;


	private DateTimeFormatterFactory factory = new DateTimeFormatterFactory();

	private DateTime dateTime = new DateTime(2009, 10, 21, 12, 10, 00, 00);


	@Test
	public void createDateTimeFormatter() {
		assertThat(factory.createDateTimeFormatter()).isEqualTo(DateTimeFormat.mediumDateTime());
	}

	@Test
	public void createDateTimeFormatterWithPattern() {
		factory = new DateTimeFormatterFactory("yyyyMMddHHmmss");
		DateTimeFormatter formatter = factory.createDateTimeFormatter();
		assertThat(formatter.print(dateTime)).isEqualTo("20091021121000");
	}

	@Test
	public void createDateTimeFormatterWithNullFallback() {
		DateTimeFormatter formatter = factory.createDateTimeFormatter(null);
		assertThat(formatter).isNull();
	}

	@Test
	public void createDateTimeFormatterWithFallback() {
		DateTimeFormatter fallback = DateTimeFormat.forStyle("LL");
		DateTimeFormatter formatter = factory.createDateTimeFormatter(fallback);
		assertThat(formatter).isSameAs(fallback);
	}

	@Test
	public void createDateTimeFormatterInOrderOfPropertyPriority() {
		factory.setStyle("SS");
		String value = applyLocale(factory.createDateTimeFormatter()).print(dateTime);
		assertThat(value.startsWith("10/21/09")).isTrue();
		assertThat(value.endsWith("12:10 PM")).isTrue();

		factory.setIso(ISO.DATE);
		assertThat(applyLocale(factory.createDateTimeFormatter()).print(dateTime)).isEqualTo("2009-10-21");

		factory.setPattern("yyyyMMddHHmmss");
		assertThat(factory.createDateTimeFormatter().print(dateTime)).isEqualTo("20091021121000");
	}

	@Test
	public void createDateTimeFormatterWithTimeZone() {
		factory.setPattern("yyyyMMddHHmmss Z");
		factory.setTimeZone(TEST_TIMEZONE);
		DateTimeZone dateTimeZone = DateTimeZone.forTimeZone(TEST_TIMEZONE);
		DateTime dateTime = new DateTime(2009, 10, 21, 12, 10, 00, 00, dateTimeZone);
		String offset = (TEST_TIMEZONE.equals(NEW_YORK) ? "-0400" : "+0200");
		assertThat(factory.createDateTimeFormatter().print(dateTime)).isEqualTo("20091021121000 " + offset);
	}

	private DateTimeFormatter applyLocale(DateTimeFormatter dateTimeFormatter) {
		return dateTimeFormatter.withLocale(Locale.US);
	}

}
