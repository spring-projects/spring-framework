/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.format.datetime.joda;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Locale;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;
import org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Tests for {@link DateTimeFormatterFactory}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
public class DateTimeFormatterFactoryTests {

	private DateTimeFormatterFactory factory = new DateTimeFormatterFactory();

	private DateTime dateTime = new DateTime(2009, 10, 21, 12, 10, 00, 00);


	@Test
	public void shouldDefaultToMediumFormat() throws Exception {
		assertThat(factory.getObject(), is(equalTo(DateTimeFormat.mediumDateTime())));
		assertThat(factory.getDateTimeFormatter(), is(equalTo(DateTimeFormat.mediumDateTime())));
	}

	@Test
	public void shouldCreateFromPattern() throws Exception {
		factory = new DateTimeFormatterFactory("yyyyMMddHHmmss");
		DateTimeFormatter formatter = factory.getObject();
		assertThat(formatter.print(dateTime), is("20091021121000"));
	}

	@Test
	public void shouldBeSingleton() throws Exception {
		assertThat(factory.isSingleton(), is(true));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void shouldCreateDateTimeFormatter() throws Exception {
		assertThat(factory.getObjectType(), is(equalTo((Class) DateTimeFormatter.class)));
	}

	@Test
	public void shouldGetDateTimeFormatterNullFallback() throws Exception {
		DateTimeFormatter formatter = factory.getDateTimeFormatter(null);
		assertThat(formatter, is(nullValue()));
	}

	@Test
	public void shouldGetDateTimeFormatterFallback() throws Exception {
		DateTimeFormatter fallback = DateTimeFormat.forStyle("LL");
		DateTimeFormatter formatter = factory.getDateTimeFormatter(fallback);
		assertThat(formatter, is(sameInstance(fallback)));
	}

	@Test
	public void shouldGetDateTimeFormatter() throws Exception {
		factory.setStyle("SS");
		assertThat(applyLocale(factory.getDateTimeFormatter()).print(dateTime), is("10/21/09 12:10 PM"));

		factory.setIso(ISO.DATE);
		assertThat(applyLocale(factory.getDateTimeFormatter()).print(dateTime), is("2009-10-21"));

		factory.setPattern("yyyyMMddHHmmss");
		assertThat(factory.getDateTimeFormatter().print(dateTime), is("20091021121000"));
	}

	@Test
	public void shouldGetWithTimeZone() throws Exception {

		TimeZone zurich = TimeZone.getTimeZone("Europe/Zurich");
		TimeZone newYork = TimeZone.getTimeZone("America/New_York");

		// Ensure that we are testing against a timezone other than the default.
		TimeZone testTimeZone;
		String offset;

		if (zurich.equals(TimeZone.getDefault())) {
			testTimeZone = newYork;
			offset = "-0400"; // Daylight savings on October 21st
		}
		else {
			testTimeZone = zurich;
			offset = "+0200"; // Daylight savings on October 21st
		}

		factory.setPattern("yyyyMMddHHmmss Z");
		factory.setTimeZone(testTimeZone);

		DateTimeZone dateTimeZone = DateTimeZone.forTimeZone(testTimeZone);
		DateTime dateTime = new DateTime(2009, 10, 21, 12, 10, 00, 00, dateTimeZone);

		assertThat(factory.getDateTimeFormatter().print(dateTime), is("20091021121000 " + offset));
	}

	private DateTimeFormatter applyLocale(DateTimeFormatter dateTimeFormatter) {
		return dateTimeFormatter.withLocale(Locale.US);
	}

}
