/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.format.datetime.standard;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.DataBinder;

import static org.junit.Assert.*;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Kazuki Shimizu
 */
public class DateTimeFormattingTests {

	private FormattingConversionService conversionService;

	private DataBinder binder;


	@Before
	public void setUp() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		setUp(registrar);
	}

	private void setUp(DateTimeFormatterRegistrar registrar) {
		conversionService = new FormattingConversionService();
		DefaultConversionService.addDefaultConverters(conversionService);
		registrar.registerFormatters(conversionService);

		DateTimeBean bean = new DateTimeBean();
		bean.getChildren().add(new DateTimeBean());
		binder = new DataBinder(bean);
		binder.setConversionService(conversionService);

		LocaleContextHolder.setLocale(Locale.US);
		DateTimeContext context = new DateTimeContext();
		context.setTimeZone(ZoneId.of("-05:00"));
		DateTimeContextHolder.setDateTimeContext(context);
	}

	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
		DateTimeContextHolder.setDateTimeContext(null);
	}


	@Test
	public void testBindLocalDate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "10/31/09");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09", binder.getBindingResult().getFieldValue("localDate"));
	}

	@Test
	public void testBindLocalDateWithSpecificStyle() throws Exception {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateStyle(FormatStyle.LONG);
		setUp(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "October 31, 2009");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("October 31, 2009", binder.getBindingResult().getFieldValue("localDate"));
	}

	@Test
	public void testBindLocalDateWithSpecificFormatter() throws Exception {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateFormatter(DateTimeFormatter.ofPattern("yyyyMMdd"));
		setUp(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "20091031");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("20091031", binder.getBindingResult().getFieldValue("localDate"));
	}

	@Test
	public void testBindLocalDateArray() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", new String[] {"10/31/09"});
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
	}

	@Test
	public void testBindLocalDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateAnnotated", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Oct 31, 2009", binder.getBindingResult().getFieldValue("localDateAnnotated"));
	}

	@Test
	public void testBindLocalDateAnnotatedWithError() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateAnnotated", "Oct -31, 2009");
		binder.bind(propertyValues);
		assertEquals(1, binder.getBindingResult().getFieldErrorCount("localDateAnnotated"));
		assertEquals("Oct -31, 2009", binder.getBindingResult().getFieldValue("localDateAnnotated"));
	}

	@Test
	public void testBindNestedLocalDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("children[0].localDateAnnotated", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Oct 31, 2009", binder.getBindingResult().getFieldValue("children[0].localDateAnnotated"));
	}

	@Test
	public void testBindLocalDateAnnotatedWithDirectFieldAccess() {
		binder.initDirectFieldAccess();
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateAnnotated", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Oct 31, 2009", binder.getBindingResult().getFieldValue("localDateAnnotated"));
	}

	@Test
	public void testBindLocalDateAnnotatedWithDirectFieldAccessAndError() {
		binder.initDirectFieldAccess();
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateAnnotated", "Oct -31, 2009");
		binder.bind(propertyValues);
		assertEquals(1, binder.getBindingResult().getFieldErrorCount("localDateAnnotated"));
		assertEquals("Oct -31, 2009", binder.getBindingResult().getFieldValue("localDateAnnotated"));
	}

	@Test
	public void testBindLocalDateFromJavaUtilCalendar() throws Exception {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", new GregorianCalendar(2009, 9, 31, 0, 0));
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09", binder.getBindingResult().getFieldValue("localDate"));
	}

	@Test
	public void testBindLocalTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00 PM", binder.getBindingResult().getFieldValue("localTime"));
	}

	@Test
	public void testBindLocalTimeWithSpecificStyle() throws Exception {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setTimeStyle(FormatStyle.MEDIUM);
		setUp(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "12:00:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00:00 PM", binder.getBindingResult().getFieldValue("localTime"));
	}

	@Test
	public void testBindLocalTimeWithSpecificFormatter() throws Exception {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setTimeFormatter(DateTimeFormatter.ofPattern("HHmmss"));
		setUp(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "130000");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("130000", binder.getBindingResult().getFieldValue("localTime"));
	}

	@Test
	public void testBindLocalTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTimeAnnotated", "12:00:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00:00 PM", binder.getBindingResult().getFieldValue("localTimeAnnotated"));
	}

	@Test
	public void testBindLocalTimeFromJavaUtilCalendar() throws Exception {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", new GregorianCalendar(1970, 0, 0, 12, 0));
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00 PM", binder.getBindingResult().getFieldValue("localTime"));
	}

	@Test
	public void testBindLocalDateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", LocalDateTime.of(2009, 10, 31, 12, 0));
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		String value = binder.getBindingResult().getFieldValue("localDateTime").toString();
		assertTrue(value.startsWith("10/31/09"));
		assertTrue(value.endsWith("12:00 PM"));
	}

	@Test
	public void testBindLocalDateTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTimeAnnotated", LocalDateTime.of(2009, 10, 31, 12, 0));
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		String value = binder.getBindingResult().getFieldValue("localDateTimeAnnotated").toString();
		assertTrue(value.startsWith("Oct 31, 2009"));
		assertTrue(value.endsWith("12:00:00 PM"));
	}

	@Test
	public void testBindLocalDateTimeFromJavaUtilCalendar() throws Exception {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", new GregorianCalendar(2009, 9, 31, 12, 0));
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		String value = binder.getBindingResult().getFieldValue("localDateTime").toString();
		assertTrue(value.startsWith("10/31/09"));
		assertTrue(value.endsWith("12:00 PM"));
	}

	@Test
	public void testBindDateTimeWithSpecificStyle() throws Exception {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateTimeStyle(FormatStyle.MEDIUM);
		setUp(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", LocalDateTime.of(2009, 10, 31, 12, 0));
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		String value = binder.getBindingResult().getFieldValue("localDateTime").toString();
		assertTrue(value.startsWith("Oct 31, 2009"));
		assertTrue(value.endsWith("12:00:00 PM"));
	}

	@Test
	public void testBindDateTimeAnnotatedPattern() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateTimeAnnotatedPattern", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("dateTimeAnnotatedPattern"));
	}

	@Test
	public void testBindDateTimeOverflow() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateTimeAnnotatedPattern", "02/29/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(1, binder.getBindingResult().getErrorCount());
	}

	@Test
	public void testBindISODate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoDate", "2009-10-31");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("2009-10-31", binder.getBindingResult().getFieldValue("isoDate"));
	}

	@Test
	public void testBindISOTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoTime", "12:00:00.000-05:00");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00:00", binder.getBindingResult().getFieldValue("isoTime"));
	}

	@Test
	public void testBindISODateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoDateTime", "2009-10-31T12:00:00.000Z");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("2009-10-31T12:00:00", binder.getBindingResult().getFieldValue("isoDateTime"));
	}

	@Test
	public void testBindInstant() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("instant", "2009-10-31T12:00:00.000Z");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertTrue(binder.getBindingResult().getFieldValue("instant").toString().startsWith("2009-10-31T12:00"));
	}

	@Test
	public void testBindInstantFromJavaUtilDate() throws Exception {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("instant", new Date(109, 9, 31, 12, 0));
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertTrue(binder.getBindingResult().getFieldValue("instant").toString().startsWith("2009-10-31"));
	}

	@Test
	public void testBindPeriod() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("period", "P6Y3M1D");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertTrue(binder.getBindingResult().getFieldValue("period").toString().equals("P6Y3M1D"));
	}

	@Test
	public void testBindDuration() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("duration", "PT8H6M12.345S");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertTrue(binder.getBindingResult().getFieldValue("duration").toString().equals("PT8H6M12.345S"));
	}

	@Test
	public void testBindYearMonth() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("yearMonth", "2007-12");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertTrue(binder.getBindingResult().getFieldValue("yearMonth").toString().equals("2007-12"));
	}

	@Test
	public void testBindYearMonthAnnotatedPattern() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("yearMonthAnnotatedPattern", "12/2007");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertTrue(binder.getBindingResult().getFieldValue("yearMonthAnnotatedPattern").toString().equals("12/2007"));
		assertEquals(YearMonth.parse("2007-12"), binder.getBindingResult().getRawFieldValue("yearMonthAnnotatedPattern"));
	}

	@Test
	public void testBindMonthDay() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("monthDay", "--12-03");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertTrue(binder.getBindingResult().getFieldValue("monthDay").toString().equals("--12-03"));
	}

	@Test
	public void testBindMonthDayAnnotatedPattern() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("monthDayAnnotatedPattern", "1/3");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertTrue(binder.getBindingResult().getFieldValue("monthDayAnnotatedPattern").toString().equals("1/3"));
		assertEquals(MonthDay.parse("--01-03"), binder.getBindingResult().getRawFieldValue("monthDayAnnotatedPattern"));
	}


	public static class DateTimeBean {

		private LocalDate localDate;

		@DateTimeFormat(style="M-")
		private LocalDate localDateAnnotated;

		private LocalTime localTime;

		@DateTimeFormat(style="-M")
		private LocalTime localTimeAnnotated;

		private LocalDateTime localDateTime;

		@DateTimeFormat(style="MM")
		private LocalDateTime localDateTimeAnnotated;

		@DateTimeFormat(pattern="M/d/yy h:mm a")
		private LocalDateTime dateTimeAnnotatedPattern;

		@DateTimeFormat(iso=ISO.DATE)
		private LocalDate isoDate;

		@DateTimeFormat(iso=ISO.TIME)
		private LocalTime isoTime;

		@DateTimeFormat(iso=ISO.DATE_TIME)
		private LocalDateTime isoDateTime;

		private Instant instant;

		private Period period;

		private Duration duration;

		private YearMonth yearMonth;

		@DateTimeFormat(pattern="MM/uuuu")
		private YearMonth yearMonthAnnotatedPattern;

		@DateTimeFormat(pattern="M/d")
		private MonthDay monthDayAnnotatedPattern;

		private MonthDay monthDay;

		private final List<DateTimeBean> children = new ArrayList<>();

		public LocalDate getLocalDate() {
			return localDate;
		}

		public void setLocalDate(LocalDate localDate) {
			this.localDate = localDate;
		}

		public LocalDate getLocalDateAnnotated() {
			return localDateAnnotated;
		}

		public void setLocalDateAnnotated(LocalDate localDateAnnotated) {
			this.localDateAnnotated = localDateAnnotated;
		}

		public LocalTime getLocalTime() {
			return localTime;
		}

		public void setLocalTime(LocalTime localTime) {
			this.localTime = localTime;
		}

		public LocalTime getLocalTimeAnnotated() {
			return localTimeAnnotated;
		}

		public void setLocalTimeAnnotated(LocalTime localTimeAnnotated) {
			this.localTimeAnnotated = localTimeAnnotated;
		}

		public LocalDateTime getLocalDateTime() {
			return localDateTime;
		}

		public void setLocalDateTime(LocalDateTime localDateTime) {
			this.localDateTime = localDateTime;
		}

		public LocalDateTime getLocalDateTimeAnnotated() {
			return localDateTimeAnnotated;
		}

		public void setLocalDateTimeAnnotated(LocalDateTime localDateTimeAnnotated) {
			this.localDateTimeAnnotated = localDateTimeAnnotated;
		}

		public LocalDateTime getDateTimeAnnotatedPattern() {
			return dateTimeAnnotatedPattern;
		}

		public void setDateTimeAnnotatedPattern(LocalDateTime dateTimeAnnotatedPattern) {
			this.dateTimeAnnotatedPattern = dateTimeAnnotatedPattern;
		}

		public LocalDate getIsoDate() {
			return isoDate;
		}

		public void setIsoDate(LocalDate isoDate) {
			this.isoDate = isoDate;
		}

		public LocalTime getIsoTime() {
			return isoTime;
		}

		public void setIsoTime(LocalTime isoTime) {
			this.isoTime = isoTime;
		}

		public LocalDateTime getIsoDateTime() {
			return isoDateTime;
		}

		public void setIsoDateTime(LocalDateTime isoDateTime) {
			this.isoDateTime = isoDateTime;
		}

		public Instant getInstant() {
			return instant;
		}

		public void setInstant(Instant instant) {
			this.instant = instant;
		}

		public Period getPeriod() {
			return period;
		}

		public void setPeriod(Period period) {
			this.period = period;
		}

		public Duration getDuration() {
			return duration;
		}

		public void setDuration(Duration duration) {
			this.duration = duration;
		}

		public YearMonth getYearMonth() {
			return yearMonth;
		}

		public void setYearMonth(YearMonth yearMonth) {
			this.yearMonth = yearMonth;
		}

		public YearMonth getYearMonthAnnotatedPattern() {
			return yearMonthAnnotatedPattern;
		}

		public void setYearMonthAnnotatedPattern(YearMonth yearMonthAnnotatedPattern) {
			this.yearMonthAnnotatedPattern = yearMonthAnnotatedPattern;
		}

		public MonthDay getMonthDay() {
			return monthDay;
		}

		public void setMonthDay(MonthDay monthDay) {
			this.monthDay = monthDay;
		}

		public MonthDay getMonthDayAnnotatedPattern() {
			return monthDayAnnotatedPattern;
		}

		public void setMonthDayAnnotatedPattern(MonthDay monthDayAnnotatedPattern) {
			this.monthDayAnnotatedPattern = monthDayAnnotatedPattern;
		}

		public List<DateTimeBean> getChildren() {
			return children;
		}
	}

}
