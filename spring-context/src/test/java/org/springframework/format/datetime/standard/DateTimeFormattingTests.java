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

package org.springframework.format.datetime.standard;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.DataBinder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
public class DateTimeFormattingTests {

	private FormattingConversionService conversionService;

	private DataBinder binder;


	@BeforeEach
	public void setup() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		setup(registrar);
	}

	private void setup(DateTimeFormatterRegistrar registrar) {
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

	@AfterEach
	public void cleanup() {
		LocaleContextHolder.setLocale(null);
		DateTimeContextHolder.setDateTimeContext(null);
	}


	@Test
	public void testBindLocalDate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "10/31/09");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("localDate")).isEqualTo("10/31/09");
	}

	@Test
	public void testBindLocalDateWithSpecificStyle() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateStyle(FormatStyle.LONG);
		setup(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "October 31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("localDate")).isEqualTo("October 31, 2009");
	}

	@Test
	public void testBindLocalDateWithSpecificFormatter() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateFormatter(DateTimeFormatter.ofPattern("yyyyMMdd"));
		setup(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "20091031");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("localDate")).isEqualTo("20091031");
	}

	@Test
	public void testBindLocalDateArray() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", new String[] {"10/31/09"});
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
	}

	@Test
	public void testBindLocalDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateAnnotated", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("localDateAnnotated")).isEqualTo("Oct 31, 2009");
	}

	@Test
	public void testBindLocalDateAnnotatedWithError() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateAnnotated", "Oct -31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getFieldErrorCount("localDateAnnotated")).isEqualTo(1);
		assertThat(binder.getBindingResult().getFieldValue("localDateAnnotated")).isEqualTo("Oct -31, 2009");
	}

	@Test
	public void testBindNestedLocalDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("children[0].localDateAnnotated", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("children[0].localDateAnnotated")).isEqualTo("Oct 31, 2009");
	}

	@Test
	public void testBindLocalDateAnnotatedWithDirectFieldAccess() {
		binder.initDirectFieldAccess();
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateAnnotated", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("localDateAnnotated")).isEqualTo("Oct 31, 2009");
	}

	@Test
	public void testBindLocalDateAnnotatedWithDirectFieldAccessAndError() {
		binder.initDirectFieldAccess();
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateAnnotated", "Oct -31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getFieldErrorCount("localDateAnnotated")).isEqualTo(1);
		assertThat(binder.getBindingResult().getFieldValue("localDateAnnotated")).isEqualTo("Oct -31, 2009");
	}

	@Test
	public void testBindLocalDateFromJavaUtilCalendar() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", new GregorianCalendar(2009, 9, 31, 0, 0));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("localDate")).isEqualTo("10/31/09");
	}

	@Test
	public void testBindLocalTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "12:00 PM");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("localTime")).isEqualTo("12:00 PM");
	}

	@Test
	public void testBindLocalTimeWithSpecificStyle() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setTimeStyle(FormatStyle.MEDIUM);
		setup(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "12:00:00 PM");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("localTime")).isEqualTo("12:00:00 PM");
	}

	@Test
	public void testBindLocalTimeWithSpecificFormatter() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setTimeFormatter(DateTimeFormatter.ofPattern("HHmmss"));
		setup(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "130000");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("localTime")).isEqualTo("130000");
	}

	@Test
	public void testBindLocalTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTimeAnnotated", "12:00:00 PM");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("localTimeAnnotated")).isEqualTo("12:00:00 PM");
	}

	@Test
	public void testBindLocalTimeFromJavaUtilCalendar() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", new GregorianCalendar(1970, 0, 0, 12, 0));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("localTime")).isEqualTo("12:00 PM");
	}

	@Test
	public void testBindLocalDateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", LocalDateTime.of(2009, 10, 31, 12, 0));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		String value = binder.getBindingResult().getFieldValue("localDateTime").toString();
		assertThat(value.startsWith("10/31/09")).isTrue();
		assertThat(value.endsWith("12:00 PM")).isTrue();
	}

	@Test
	public void testBindLocalDateTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTimeAnnotated", LocalDateTime.of(2009, 10, 31, 12, 0));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		String value = binder.getBindingResult().getFieldValue("localDateTimeAnnotated").toString();
		assertThat(value.startsWith("Oct 31, 2009")).isTrue();
		assertThat(value.endsWith("12:00:00 PM")).isTrue();
	}

	@Test
	public void testBindLocalDateTimeFromJavaUtilCalendar() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", new GregorianCalendar(2009, 9, 31, 12, 0));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		String value = binder.getBindingResult().getFieldValue("localDateTime").toString();
		assertThat(value.startsWith("10/31/09")).isTrue();
		assertThat(value.endsWith("12:00 PM")).isTrue();
	}

	@Test
	public void testBindDateTimeWithSpecificStyle() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateTimeStyle(FormatStyle.MEDIUM);
		setup(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", LocalDateTime.of(2009, 10, 31, 12, 0));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		String value = binder.getBindingResult().getFieldValue("localDateTime").toString();
		assertThat(value.startsWith("Oct 31, 2009")).isTrue();
		assertThat(value.endsWith("12:00:00 PM")).isTrue();
	}

	@Test
	public void testBindDateTimeAnnotatedPattern() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateTimeAnnotatedPattern", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("dateTimeAnnotatedPattern")).isEqualTo("10/31/09 12:00 PM");
	}

	@Test
	public void testBindDateTimeOverflow() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateTimeAnnotatedPattern", "02/29/09 12:00 PM");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(1);
	}

	@Test
	public void testBindISODate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoDate", "2009-10-31");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("isoDate")).isEqualTo("2009-10-31");
	}

	@Test
	public void testBindISOTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoTime", "12:00:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("isoTime")).isEqualTo("12:00:00");
	}

	@Test
	public void testBindISOTimeWithZone() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoTime", "12:00:00.000-05:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("isoTime")).isEqualTo("12:00:00");
	}

	@Test
	public void testBindISODateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoDateTime", "2009-10-31T12:00:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("isoDateTime")).isEqualTo("2009-10-31T12:00:00");
	}

	@Test
	public void testBindISODateTimeWithZone() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoDateTime", "2009-10-31T12:00:00.000Z");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("isoDateTime")).isEqualTo("2009-10-31T12:00:00");
	}

	@Test
	public void testBindInstant() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("instant", "2009-10-31T12:00:00.000Z");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("instant").toString().startsWith("2009-10-31T12:00")).isTrue();
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testBindInstantFromJavaUtilDate() {
		TimeZone defaultZone = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		try {
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add("instant", new Date(109, 9, 31, 12, 0));
			binder.bind(propertyValues);
			assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
			assertThat(binder.getBindingResult().getFieldValue("instant").toString().startsWith("2009-10-31")).isTrue();
		}
		finally {
			TimeZone.setDefault(defaultZone);
		}
	}

	@Test
	public void testBindPeriod() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("period", "P6Y3M1D");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("period").toString().equals("P6Y3M1D")).isTrue();
	}

	@Test
	public void testBindDuration() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("duration", "PT8H6M12.345S");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("duration").toString().equals("PT8H6M12.345S")).isTrue();
	}

	@Test
	public void testBindYear() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("year", "2007");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("year").toString().equals("2007")).isTrue();
	}

	@Test
	public void testBindMonth() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("month", "JULY");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("month").toString().equals("JULY")).isTrue();
	}

	@Test
	public void testBindMonthInAnyCase() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("month", "July");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("month").toString().equals("JULY")).isTrue();
	}

	@Test
	public void testBindYearMonth() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("yearMonth", "2007-12");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("yearMonth").toString().equals("2007-12")).isTrue();
	}

	@Test
	public void testBindMonthDay() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("monthDay", "--12-03");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("monthDay").toString().equals("--12-03")).isTrue();
	}


	public static class DateTimeBean {

		private LocalDate localDate;

		@DateTimeFormat(style = "M-")
		private LocalDate localDateAnnotated;

		private LocalTime localTime;

		@DateTimeFormat(style = "-M")
		private LocalTime localTimeAnnotated;

		private LocalDateTime localDateTime;

		@DateTimeFormat(style = "MM")
		private LocalDateTime localDateTimeAnnotated;

		@DateTimeFormat(pattern = "M/d/yy h:mm a")
		private LocalDateTime dateTimeAnnotatedPattern;

		@DateTimeFormat(iso = ISO.DATE)
		private LocalDate isoDate;

		@DateTimeFormat(iso = ISO.TIME)
		private LocalTime isoTime;

		@DateTimeFormat(iso = ISO.DATE_TIME)
		private LocalDateTime isoDateTime;

		private Instant instant;

		private Period period;

		private Duration duration;

		private Year year;

		private Month month;

		private YearMonth yearMonth;

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

		public Year getYear() {
			return year;
		}

		public void setYear(Year year) {
			this.year = year;
		}

		public Month getMonth() {
			return month;
		}

		public void setMonth(Month month) {
			this.month = month;
		}

		public YearMonth getYearMonth() {
			return yearMonth;
		}

		public void setYearMonth(YearMonth yearMonth) {
			this.yearMonth = yearMonth;
		}

		public MonthDay getMonthDay() {
			return monthDay;
		}

		public void setMonthDay(MonthDay monthDay) {
			this.monthDay = monthDay;
		}

		public List<DateTimeBean> getChildren() {
			return children;
		}
	}

}
