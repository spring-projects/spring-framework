/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime;

import java.util.Calendar;
import java.util.Date;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.util.Assert;

/**
 * Configures basic date formatting for use with Spring, primarily for
 * {@link org.springframework.format.annotation.DateTimeFormat} declarations.
 * Applies to fields of type {@link Date}, {@link Calendar}, and {@code long}.
 *
 * <p>Designed for direct instantiation but also exposes the static
 * {@link #addDateConverters(ConverterRegistry)} utility method for
 * ad-hoc use against any {@code ConverterRegistry} instance.
 *
 * @author Phillip Webb
 * @since 3.2
 * @see org.springframework.format.datetime.standard.DateTimeFormatterRegistrar
 * @see FormatterRegistrar#registerFormatters
 */
public class DateFormatterRegistrar implements FormatterRegistrar {

	private @Nullable DateFormatter dateFormatter;


	/**
	 * Set a global date formatter to register.
	 * <p>If not specified, no general formatter for non-annotated
	 * {@link Date} and {@link Calendar} fields will be registered.
	 */
	public void setFormatter(DateFormatter dateFormatter) {
		Assert.notNull(dateFormatter, "DateFormatter must not be null");
		this.dateFormatter = dateFormatter;
	}


	@Override
	public void registerFormatters(FormatterRegistry registry) {
		addDateConverters(registry);
		// In order to retain back compatibility we only register Date/Calendar
		// types when a user defined formatter is specified (see SPR-10105)
		if (this.dateFormatter != null) {
			registry.addFormatter(this.dateFormatter);
			registry.addFormatterForFieldType(Calendar.class, this.dateFormatter);
		}
		registry.addFormatterForFieldAnnotation(new DateTimeFormatAnnotationFormatterFactory());
	}

	/**
	 * Add date converters to the specified registry.
	 * @param converterRegistry the registry of converters to add to
	 */
	public static void addDateConverters(ConverterRegistry converterRegistry) {
		converterRegistry.addConverter(new DateToLongConverter());
		converterRegistry.addConverter(new DateToCalendarConverter());
		converterRegistry.addConverter(new CalendarToDateConverter());
		converterRegistry.addConverter(new CalendarToLongConverter());
		converterRegistry.addConverter(new LongToDateConverter());
		converterRegistry.addConverter(new LongToCalendarConverter());
	}


	private static class DateToLongConverter implements Converter<Date, Long> {

		@Override
		public Long convert(Date source) {
			return source.getTime();
		}
	}


	private static class DateToCalendarConverter implements Converter<Date, Calendar> {

		@Override
		public Calendar convert(Date source) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(source);
			return calendar;
		}
	}


	private static class CalendarToDateConverter implements Converter<Calendar, Date> {

		@Override
		public Date convert(Calendar source) {
			return source.getTime();
		}
	}


	private static class CalendarToLongConverter implements Converter<Calendar, Long> {

		@Override
		public Long convert(Calendar source) {
			return source.getTimeInMillis();
		}
	}


	private static class LongToDateConverter implements Converter<Long, Date> {

		@Override
		public Date convert(Long source) {
			return new Date(source);
		}
	}


	private static class LongToCalendarConverter implements Converter<Long, Calendar> {

		@Override
		public Calendar convert(Long source) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(source);
			return calendar;
		}
	}

}
