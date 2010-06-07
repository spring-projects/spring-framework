/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.DataBinder;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class JodaTimeFormattingTests {

	private FormattingConversionService conversionService = new FormattingConversionService();

	private DataBinder binder;

	@Before
	public void setUp() {
		ConversionServiceFactory.addDefaultConverters(conversionService);

		JodaTimeFormattingConfigurer configurer = new JodaTimeFormattingConfigurer();
		configurer.installJodaTimeFormatting(conversionService);

		JodaTimeBean bean = new JodaTimeBean();
		bean.getChildren().add(new JodaTimeBean());
		binder = new DataBinder(bean);
		binder.setConversionService(conversionService);

		LocaleContextHolder.setLocale(Locale.US);
		JodaTimeContext context = new JodaTimeContext();
		context.setTimeZone(DateTimeZone.forID("-05:00"));
		JodaTimeContextHolder.setJodaTimeContext(context);
	}

	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
		JodaTimeContextHolder.setJodaTimeContext(null);
	}

	@Test
	public void testJodaTimePatternsForStyle() {
		System.out.println(org.joda.time.format.DateTimeFormat.patternForStyle("SS", LocaleContextHolder.getLocale()));
		System.out.println(org.joda.time.format.DateTimeFormat.patternForStyle("MM", LocaleContextHolder.getLocale()));
		System.out.println(org.joda.time.format.DateTimeFormat.patternForStyle("LL", LocaleContextHolder.getLocale()));
		System.out.println(org.joda.time.format.DateTimeFormat.patternForStyle("FF", LocaleContextHolder.getLocale()));
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
	public void testBindLocalTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00 PM", binder.getBindingResult().getFieldValue("localTime"));
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
	public void testBindLocalDateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("localDateTime"));
	}

	@Test
	public void testBindLocalDateTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTimeAnnotated", "Saturday, October 31, 2009 12:00:00 PM ");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Saturday, October 31, 2009 12:00:00 PM ", binder.getBindingResult().getFieldValue(
				"localDateTimeAnnotated"));
	}

	@Test
	public void testBindLocalDateTimeAnnotatedLong() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTimeAnnotatedLong", "October 31, 2009 12:00:00 PM ");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("October 31, 2009 12:00:00 PM ", binder.getBindingResult().getFieldValue(
				"localDateTimeAnnotatedLong"));
	}

	@Test
	public void testBindDateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateTime", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("dateTime"));
	}

	@Test
	public void testBindDateTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateTimeAnnotated", "Oct 31, 2009 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Oct 31, 2009 12:00 PM", binder.getBindingResult().getFieldValue("dateTimeAnnotated"));
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
	public void testBindDateTimeAnnotatedDefault() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateTimeAnnotatedDefault", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("dateTimeAnnotatedDefault"));
	}

	@Test
	public void testBindDate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("date", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("date"));
	}

	@Test
	public void testBindDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("dateAnnotated", "10/31/09");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09", binder.getBindingResult().getFieldValue("dateAnnotated"));
	}

	@Test
	public void testBindCalendar() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("calendar", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("calendar"));
	}

	@Test
	public void testBindCalendarAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("calendarAnnotated", "10/31/09");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09", binder.getBindingResult().getFieldValue("calendarAnnotated"));
	}

	@Test
	public void testBindLong() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("millis", "1256961600");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("1256961600", binder.getBindingResult().getFieldValue("millis"));
	}

	@Test
	public void testBindLongAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("millisAnnotated", "10/31/09");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09", binder.getBindingResult().getFieldValue("millisAnnotated"));
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
		assertEquals("12:00:00.000", binder.getBindingResult().getFieldValue("isoTime"));
	}

	@Test
	public void testBindISODateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoDateTime", "2009-10-31T12:00:00.000Z");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("2009-10-31T07:00:00.000-05:00", binder.getBindingResult().getFieldValue("isoDateTime"));
	}


	@SuppressWarnings("unused")
	private static class JodaTimeBean {

		private LocalDate localDate;

		@DateTimeFormat(style="M-")
		private LocalDate localDateAnnotated;

		private LocalTime localTime;

		@DateTimeFormat(style="-M")
		private LocalTime localTimeAnnotated;

		private LocalDateTime localDateTime;

		@DateTimeFormat(style="FF")
		private LocalDateTime localDateTimeAnnotated;

		@DateTimeFormat(style="LL")
		private LocalDateTime localDateTimeAnnotatedLong;

		private DateTime dateTime;

		@DateTimeFormat(style="MS")
		private DateTime dateTimeAnnotated;

		@DateTimeFormat
		private Date date;

		@DateTimeFormat(style="S-")
		private Date dateAnnotated;

		@DateTimeFormat
		private Calendar calendar;

		@DateTimeFormat(style="S-")
		private Calendar calendarAnnotated;

		private Long millis;

		@DateTimeFormat
		private DateTime dateTimeAnnotatedDefault;

		private Long millisAnnotated;

		@DateTimeFormat(pattern="M/d/yy h:mm a")
		private DateTime dateTimeAnnotatedPattern;

		@DateTimeFormat(iso=ISO.DATE)
		private LocalDate isoDate;

		@DateTimeFormat(iso=ISO.TIME)
		private LocalTime isoTime;

		@DateTimeFormat(iso=ISO.DATE_TIME)
		private DateTime isoDateTime;

		private final List<JodaTimeBean> children = new ArrayList<JodaTimeBean>();

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
		
		public LocalDateTime getLocalDateTimeAnnotatedLong() {
			return localDateTimeAnnotatedLong;
		}

		public void setLocalDateTimeAnnotatedLong(LocalDateTime localDateTimeAnnotatedLong) {
			this.localDateTimeAnnotatedLong = localDateTimeAnnotatedLong;
		}

		public DateTime getDateTime() {
			return dateTime;
		}

		public void setDateTime(DateTime dateTime) {
			this.dateTime = dateTime;
		}

		public DateTime getDateTimeAnnotated() {
			return dateTimeAnnotated;
		}

		public void setDateTimeAnnotated(DateTime dateTimeAnnotated) {
			this.dateTimeAnnotated = dateTimeAnnotated;
		}

		public DateTime getDateTimeAnnotatedPattern() {
			return dateTimeAnnotatedPattern;
		}

		public void setDateTimeAnnotatedPattern(DateTime dateTimeAnnotatedPattern) {
			this.dateTimeAnnotatedPattern = dateTimeAnnotatedPattern;
		}

		public DateTime getDateTimeAnnotatedDefault() {
			return dateTimeAnnotatedDefault;
		}

		public void setDateTimeAnnotatedDefault(DateTime dateTimeAnnotatedDefault) {
			this.dateTimeAnnotatedDefault = dateTimeAnnotatedDefault;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public Date getDateAnnotated() {
			return dateAnnotated;
		}

		public void setDateAnnotated(Date dateAnnotated) {
			this.dateAnnotated = dateAnnotated;
		}

		public Calendar getCalendar() {
			return calendar;
		}

		public void setCalendar(Calendar calendar) {
			this.calendar = calendar;
		}

		public Calendar getCalendarAnnotated() {
			return calendarAnnotated;
		}

		public void setCalendarAnnotated(Calendar calendarAnnotated) {
			this.calendarAnnotated = calendarAnnotated;
		}

		public Long getMillis() {
			return millis;
		}

		public void setMillis(Long millis) {
			this.millis = millis;
		}

		@DateTimeFormat(style="S-")
		public Long getMillisAnnotated() {
			return millisAnnotated;
		}

		public void setMillisAnnotated(@DateTimeFormat(style="S-") Long millisAnnotated) {
			this.millisAnnotated = millisAnnotated;
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

		public DateTime getIsoDateTime() {
			return isoDateTime;
		}

		public void setIsoDateTime(DateTime isoDateTime) {
			this.isoDateTime = isoDateTime;
		}

		public List<JodaTimeBean> getChildren() {
			return children;
		}
	}

}
