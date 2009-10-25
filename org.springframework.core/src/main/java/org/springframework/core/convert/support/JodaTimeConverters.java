/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.core.convert.support;

import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.ClassUtils;

class JodaTimeConverters {

	public static void addConverters(GenericConversionService registry) {
		if (!isJodaTimePresent()) {
			return;
		}
		registry.addConverter(DateTime.class, LocalDate.class, new DateTimeToLocalDateConverter());
		registry.addConverter(LocalDate.class, DateTime.class, new LocalDateToDateTimeConverter());
		
		registry.addConverter(DateTime.class, LocalTime.class, new DateTimeToLocalTimeConverter());
		registry.addConverter(LocalTime.class, DateTime.class, new LocalTimeToDateTimeConverter());
		
		registry.addConverter(DateTime.class, LocalDateTime.class, new DateTimeToLocalDateTimeConverter());
		registry.addConverter(LocalDateTime.class, DateTime.class, new LocalDateTimeToDateTimeConverter());
		
		registry.addConverter(DateTime.class, DateMidnight.class, new DateTimeToDateMidnightConverter());
		registry.addConverter(DateMidnight.class, Date.class, new DateMidnightToDateTimeConverter());

		registry.addConverter(DateTime.class, Date.class, new DateTimeToDateConverter());
		registry.addConverter(Date.class, DateTime.class, new DateToDateTimeConverter());
		
		registry.addConverter(DateTime.class, Calendar.class, new DateTimeToCalendarConverter());
		registry.addConverter(Calendar.class, DateTime.class, new CalendarToDateTimeConverter());
	}

	private static boolean isJodaTimePresent() {
		return ClassUtils.isPresent("org.joda.time.DateTime", JodaTimeConverters.class.getClassLoader());
	}
	
	private static class DateTimeToLocalDateConverter implements Converter<DateTime, LocalDate> {
		public LocalDate convert(DateTime source) {
			return source.toLocalDate();
		}
	}
	
	private static class LocalDateToDateTimeConverter implements Converter<LocalDate, DateTime> {
		public DateTime convert(LocalDate source) {
			return source.toDateTimeAtStartOfDay();
		}		
	}

	private static class DateTimeToLocalTimeConverter implements Converter<DateTime, LocalTime> {
		public LocalTime convert(DateTime source) {
			return source.toLocalTime();
		}
	}

	private static class LocalTimeToDateTimeConverter implements Converter<LocalTime, DateTime> {
		public DateTime convert(LocalTime source) {
			return source.toDateTimeToday();
		}		
	}
	
	private static class DateTimeToLocalDateTimeConverter implements Converter<DateTime, LocalDateTime> {
		public LocalDateTime convert(DateTime source) {
			return source.toLocalDateTime();
		}
	}

	private static class LocalDateTimeToDateTimeConverter implements Converter<LocalDateTime, DateTime> {
		public DateTime convert(LocalDateTime source) {
			return source.toDateTime();
		}		
	}

	private static class DateTimeToDateMidnightConverter implements Converter<DateTime, DateMidnight> {
		public DateMidnight convert(DateTime source) {
			return source.toDateMidnight();
		}
	}

	private static class DateMidnightToDateTimeConverter implements Converter<DateMidnight, DateTime> {
		public DateTime convert(DateMidnight source) {
			return source.toDateTime();
		}		
	}

	private static class DateTimeToDateConverter implements Converter<DateTime, Date> {
		public Date convert(DateTime source) {
			return source.toDate();
		}
	}

	private static class DateToDateTimeConverter implements Converter<Date, DateTime> {
		public DateTime convert(Date source) {
			return new DateTime(source);
		}
	}

	private static class DateTimeToCalendarConverter implements Converter<DateTime, Calendar> {
		public Calendar convert(DateTime source) {
			return source.toGregorianCalendar();
		}
	}

	private static class CalendarToDateTimeConverter implements Converter<Calendar, DateTime> {
		public DateTime convert(Calendar source) {
			return new DateTime(source);
		}
	}

}
