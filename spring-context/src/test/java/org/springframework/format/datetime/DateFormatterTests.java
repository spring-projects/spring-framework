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

package org.springframework.format.datetime;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;

import org.springframework.format.annotation.DateTimeFormat.ISO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;




/**
 * Tests for {@link DateFormatter}.
 *
 * @author Keith Donald
 * @author Phillip Webb
 */
public class DateFormatterTests {

	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");


	@Test
	public void shouldPrintAndParseDefault() throws Exception {
		DateFormatter formatter = new DateFormatter();
		formatter.setTimeZone(UTC);
		Date date = getDate(2009, Calendar.JUNE, 1);
		assertThat(formatter.print(date, Locale.US)).isEqualTo("Jun 1, 2009");
		assertThat(formatter.parse("Jun 1, 2009", Locale.US)).isEqualTo(date);
	}

	@Test
	public void shouldPrintAndParseFromPattern() throws ParseException {
		DateFormatter formatter = new DateFormatter("yyyy-MM-dd");
		formatter.setTimeZone(UTC);
		Date date = getDate(2009, Calendar.JUNE, 1);
		assertThat(formatter.print(date, Locale.US)).isEqualTo("2009-06-01");
		assertThat(formatter.parse("2009-06-01", Locale.US)).isEqualTo(date);
	}

	@Test
	public void shouldPrintAndParseShort() throws Exception {
		DateFormatter formatter = new DateFormatter();
		formatter.setTimeZone(UTC);
		formatter.setStyle(DateFormat.SHORT);
		Date date = getDate(2009, Calendar.JUNE, 1);
		assertThat(formatter.print(date, Locale.US)).isEqualTo("6/1/09");
		assertThat(formatter.parse("6/1/09", Locale.US)).isEqualTo(date);
	}

	@Test
	public void shouldPrintAndParseMedium() throws Exception {
		DateFormatter formatter = new DateFormatter();
		formatter.setTimeZone(UTC);
		formatter.setStyle(DateFormat.MEDIUM);
		Date date = getDate(2009, Calendar.JUNE, 1);
		assertThat(formatter.print(date, Locale.US)).isEqualTo("Jun 1, 2009");
		assertThat(formatter.parse("Jun 1, 2009", Locale.US)).isEqualTo(date);
	}

	@Test
	public void shouldPrintAndParseLong() throws Exception {
		DateFormatter formatter = new DateFormatter();
		formatter.setTimeZone(UTC);
		formatter.setStyle(DateFormat.LONG);
		Date date = getDate(2009, Calendar.JUNE, 1);
		assertThat(formatter.print(date, Locale.US)).isEqualTo("June 1, 2009");
		assertThat(formatter.parse("June 1, 2009", Locale.US)).isEqualTo(date);
	}

	@Test
	public void shouldPrintAndParseFull() throws Exception {
		DateFormatter formatter = new DateFormatter();
		formatter.setTimeZone(UTC);
		formatter.setStyle(DateFormat.FULL);
		Date date = getDate(2009, Calendar.JUNE, 1);
		assertThat(formatter.print(date, Locale.US)).isEqualTo("Monday, June 1, 2009");
		assertThat(formatter.parse("Monday, June 1, 2009", Locale.US)).isEqualTo(date);
	}

	@Test
	public void shouldPrintAndParseISODate() throws Exception {
		DateFormatter formatter = new DateFormatter();
		formatter.setTimeZone(UTC);
		formatter.setIso(ISO.DATE);
		Date date = getDate(2009, Calendar.JUNE, 1, 14, 23, 5, 3);
		assertThat(formatter.print(date, Locale.US)).isEqualTo("2009-06-01");
		assertThat(formatter.parse("2009-6-01", Locale.US))
				.isEqualTo(getDate(2009, Calendar.JUNE, 1));
	}

	@Test
	public void shouldPrintAndParseISOTime() throws Exception {
		DateFormatter formatter = new DateFormatter();
		formatter.setTimeZone(UTC);
		formatter.setIso(ISO.TIME);
		Date date = getDate(2009, Calendar.JANUARY, 1, 14, 23, 5, 3);
		assertThat(formatter.print(date, Locale.US)).isEqualTo("14:23:05.003Z");
		assertThat(formatter.parse("14:23:05.003Z", Locale.US))
				.isEqualTo(getDate(1970, Calendar.JANUARY, 1, 14, 23, 5, 3));
	}

	@Test
	public void shouldPrintAndParseISODateTime() throws Exception {
		DateFormatter formatter = new DateFormatter();
		formatter.setTimeZone(UTC);
		formatter.setIso(ISO.DATE_TIME);
		Date date = getDate(2009, Calendar.JUNE, 1, 14, 23, 5, 3);
		assertThat(formatter.print(date, Locale.US)).isEqualTo("2009-06-01T14:23:05.003Z");
		assertThat(formatter.parse("2009-06-01T14:23:05.003Z", Locale.US)).isEqualTo(date);
	}

	@Test
	public void shouldSupportJodaStylePatterns() throws Exception {
		String[] chars = { "S", "M", "-" };
		for (String d : chars) {
			for (String t : chars) {
				String style = d + t;
				if (!style.equals("--")) {
					Date date = getDate(2009, Calendar.JUNE, 10, 14, 23, 0, 0);
					if (t.equals("-")) {
						date = getDate(2009, Calendar.JUNE, 10);
					}
					else if (d.equals("-")) {
						date = getDate(1970, Calendar.JANUARY, 1, 14, 23, 0, 0);
					}
					testJodaStylePatterns(style, Locale.US, date);
				}
			}
		}
	}

	private void testJodaStylePatterns(String style, Locale locale, Date date) throws Exception {
		DateFormatter formatter = new DateFormatter();
		formatter.setTimeZone(UTC);
		formatter.setStylePattern(style);
		DateTimeFormatter jodaFormatter = DateTimeFormat.forStyle(style).withLocale(locale).withZone(DateTimeZone.UTC);
		String jodaPrinted = jodaFormatter.print(date.getTime());
		assertThat(formatter.print(date, locale))
				.as("Unable to print style pattern " + style)
				.isEqualTo(jodaPrinted);
		assertThat(formatter.parse(jodaPrinted, locale))
				.as("Unable to parse style pattern " + style)
				.isEqualTo(date);
	}

	@Test
	public void shouldThrowOnUnsupportedStylePattern() throws Exception {
		DateFormatter formatter = new DateFormatter();
		formatter.setStylePattern("OO");
		assertThatIllegalStateException().isThrownBy(() ->
				formatter.parse("2009", Locale.US))
			.withMessageContaining("Unsupported style pattern 'OO'");
	}

	@Test
	public void shouldUseCorrectOrder() throws Exception {
		DateFormatter formatter = new DateFormatter();
		formatter.setTimeZone(UTC);
		formatter.setStyle(DateFormat.SHORT);
		formatter.setStylePattern("L-");
		formatter.setIso(ISO.DATE_TIME);
		formatter.setPattern("yyyy");
		Date date = getDate(2009, Calendar.JUNE, 1, 14, 23, 5, 3);

		assertThat(formatter.print(date, Locale.US)).as("uses pattern").isEqualTo("2009");

		formatter.setPattern("");
		assertThat(formatter.print(date, Locale.US)).as("uses ISO").isEqualTo("2009-06-01T14:23:05.003Z");

		formatter.setIso(ISO.NONE);
		assertThat(formatter.print(date, Locale.US)).as("uses style pattern").isEqualTo("June 1, 2009");

		formatter.setStylePattern("");
		assertThat(formatter.print(date, Locale.US)).as("uses style").isEqualTo("6/1/09");
	}


	private Date getDate(int year, int month, int dayOfMonth) {
		return getDate(year, month, dayOfMonth, 0, 0, 0, 0);
	}

	private Date getDate(int year, int month, int dayOfMonth, int hour, int minute, int second, int millisecond) {
		Calendar cal = Calendar.getInstance(Locale.US);
		cal.setTimeZone(UTC);
		cal.clear();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		cal.set(Calendar.HOUR, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, second);
		cal.set(Calendar.MILLISECOND, millisecond);
		return cal.getTime();
	}

}
