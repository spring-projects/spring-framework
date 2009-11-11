package org.springframework.format.datetime.joda;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.DataBinder;

public class JodaTimeFormattingTests {

	private FormattingConversionService conversionService = new FormattingConversionService();

	private DataBinder binder;

	@Before
	public void setUp() {
		JodaTimeFormattingConfigurer configurer = new JodaTimeFormattingConfigurer();
		configurer.installJodaTimeFormatting(conversionService);

		binder = new DataBinder(new JodaTimeBean());
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
		propertyValues.addPropertyValue("localDate", "10/31/09");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09", binder.getBindingResult().getFieldValue("localDate"));
	}

	@Test
	public void testBindLocalDateArray() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("localDate", new String[] { "10/31/09" });
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
	}

	@Test
	public void testBindLocalDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("localDateAnnotated", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Oct 31, 2009", binder.getBindingResult().getFieldValue("localDateAnnotated"));
	}

	@Test
	public void testBindLocalTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("localTime", "12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00 PM", binder.getBindingResult().getFieldValue("localTime"));
	}

	@Test
	public void testBindLocalTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("localTimeAnnotated", "12:00:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00:00 PM", binder.getBindingResult().getFieldValue("localTimeAnnotated"));
	}

	@Test
	public void testBindLocalDateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("localDateTime", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("localDateTime"));
	}

	@Test
	public void testBindLocalDateTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("localDateTimeAnnotated", "Saturday, October 31, 2009 12:00:00 PM ");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Saturday, October 31, 2009 12:00:00 PM ", binder.getBindingResult().getFieldValue(
				"localDateTimeAnnotated"));
	}

	@Test
	public void testBindDateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("dateTime", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("dateTime"));
	}

	@Test
	public void testBindDateTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("dateTimeAnnotated", "Oct 31, 2009 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Oct 31, 2009 12:00 PM", binder.getBindingResult().getFieldValue("dateTimeAnnotated"));
	}

	@Test
	public void testBindDateTimeAnnotatedPattern() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("dateTimeAnnotatedPattern", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("dateTimeAnnotatedPattern"));
	}

	@Test
	public void testBindDateTimeAnnotatedDefault() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("dateTimeAnnotatedDefault", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("dateTimeAnnotatedDefault"));
	}

	@Test
	public void testBindDate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("date", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("date"));
	}

	@Test
	public void testBindDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("dateAnnotated", "10/31/09");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09", binder.getBindingResult().getFieldValue("dateAnnotated"));
	}

	@Test
	public void testBindCalendar() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("calendar", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09 12:00 PM", binder.getBindingResult().getFieldValue("calendar"));
	}

	@Test
	public void testBindCalendarAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("calendarAnnotated", "10/31/09");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09", binder.getBindingResult().getFieldValue("calendarAnnotated"));
	}

	@Test
	public void testBindLong() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("millis", "1256961600");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("1256961600", binder.getBindingResult().getFieldValue("millis"));
	}

	@Test
	public void testBindLongAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("millisAnnotated", "10/31/09");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("10/31/09", binder.getBindingResult().getFieldValue("millisAnnotated"));
	}

	@Test
	public void testBindISODate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("isoDate", "2009-10-31");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("2009-10-31", binder.getBindingResult().getFieldValue("isoDate"));
	}

	@Test
	public void testBindISOTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("isoTime", "12:00:00.000-05:00");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("12:00:00.000", binder.getBindingResult().getFieldValue("isoTime"));
	}

	@Test
	public void testBindISODateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("isoDateTime", "2009-10-31T12:00:00.000Z");
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

		private DateTime dateTime;

		@DateTimeFormat(style="MS")
		private DateTime dateTimeAnnotated;

		private Date date;

		@DateTimeFormat(style="S-")
		private Date dateAnnotated;

		private Calendar calendar;

		@DateTimeFormat(style="S-")
		private Calendar calendarAnnotated;

		private Long millis;

		@DateTimeFormat
		private DateTime dateTimeAnnotatedDefault;

		@DateTimeFormat(style="S-")
		private Long millisAnnotated;

		@DateTimeFormat(pattern="M/d/yy h:mm a")
		private DateTime dateTimeAnnotatedPattern;

		@DateTimeFormat(iso=ISO.DATE)
		private LocalDate isoDate;

		@DateTimeFormat(iso=ISO.TIME)
		private LocalTime isoTime;

		@DateTimeFormat(iso=ISO.DATE_TIME)
		private DateTime isoDateTime;

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

		public Long getMillisAnnotated() {
			return millisAnnotated;
		}

		public void setMillisAnnotated(Long millisAnnotated) {
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
		
	}
}