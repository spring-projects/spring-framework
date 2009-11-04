package org.springframework.ui.format.jodatime;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.format.jodatime.DateTimeFormat.Style;
import org.springframework.ui.format.support.FormattingConversionService;
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
	}

	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
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
	@Ignore
	public void testBindLocalDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.addPropertyValue("localDateAnnotated", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertEquals(0, binder.getBindingResult().getErrorCount());
		assertEquals("Oct 31, 2009", binder.getBindingResult().getFieldValue("localDateAnnotated"));
	}

	public static class JodaTimeBean {

		private LocalDate localDate;

		@DateTimeFormat(dateStyle = Style.MEDIUM)
		private LocalDate localDateAnnotated;

		private LocalTime localTime;

		@DateTimeFormat(timeStyle = Style.LONG)
		private LocalDate localTimeAnnotated;

		private LocalDateTime localDateTime;

		@DateTimeFormat(dateStyle = Style.FULL, timeStyle = Style.FULL)
		private LocalDateTime localDateTimeAnnotated;

		private DateTime dateTime;

		@DateTimeFormat(dateStyle = Style.MEDIUM, timeStyle = Style.SHORT)
		private LocalDateTime dateTimeAnnotated;

		private Date date;

		@DateTimeFormat(dateStyle = Style.SHORT)
		private Date dateAnnotated;

		private Calendar calendar;

		@DateTimeFormat(dateStyle = Style.SHORT)
		private Calendar calendarAnnotated;

		private Long millis;

		@DateTimeFormat(dateStyle = Style.SHORT)
		private Long millisAnnotated;

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

		public LocalDate getLocalTimeAnnotated() {
			return localTimeAnnotated;
		}

		public void setLocalTimeAnnotated(LocalDate localTimeAnnotated) {
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

		public LocalDateTime getDateTimeAnnotated() {
			return dateTimeAnnotated;
		}

		public void setDateTimeAnnotated(LocalDateTime dateTimeAnnotated) {
			this.dateTimeAnnotated = dateTimeAnnotated;
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

	}
}
