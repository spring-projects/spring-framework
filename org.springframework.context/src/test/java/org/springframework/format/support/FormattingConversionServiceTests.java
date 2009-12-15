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

package org.springframework.format.support;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.format.datetime.joda.DateTimeParser;
import org.springframework.format.datetime.joda.JodaDateTimeFormatAnnotationFormatterFactory;
import org.springframework.format.datetime.joda.ReadablePartialPrinter;
import org.springframework.format.number.NumberFormatter;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class FormattingConversionServiceTests {

	private FormattingConversionService formattingService;

	@Before
	public void setUp() {
		formattingService = new FormattingConversionService();
		ConversionServiceFactory.addDefaultConverters(formattingService);
		LocaleContextHolder.setLocale(Locale.US);
	}

	@After
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
	}

	@Test
	public void testFormatFieldForTypeWithFormatter() throws ParseException {
		formattingService.addFormatterForFieldType(Number.class, new NumberFormatter());
		String formatted = formattingService.convert(3, String.class);
		assertEquals("3", formatted);
		Integer i = formattingService.convert("3", Integer.class);
		assertEquals(new Integer(3), i);
	}

	@Test
	public void testFormatFieldForTypeWithPrinterParserWithCoersion() throws ParseException {
		formattingService.addConverter(new Converter<DateTime, LocalDate>() {
			public LocalDate convert(DateTime source) {
				return source.toLocalDate();
			}
		});
		formattingService.addFormatterForFieldType(LocalDate.class, new ReadablePartialPrinter(DateTimeFormat
				.shortDate()), new DateTimeParser(DateTimeFormat.shortDate()));
		String formatted = formattingService.convert(new LocalDate(2009, 10, 31), String.class);
		assertEquals("10/31/09", formatted);
		LocalDate date = formattingService.convert("10/31/09", LocalDate.class);
		assertEquals(new LocalDate(2009, 10, 31), date);
	}

	@Test
	public void testFormatFieldForAnnotation() throws Exception {
		formattingService.addConverter(new Converter<Date, Long>() {
			public Long convert(Date source) {
				return source.getTime();
			}
		});
		formattingService.addConverter(new Converter<DateTime, Date>() {
			public Date convert(DateTime source) {
				return source.toDate();
			}
		});
		formattingService.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
		String formatted = (String) formattingService.convert(new LocalDate(2009, 10, 31).toDateTimeAtCurrentTime()
				.toDate(), new TypeDescriptor(Model.class.getField("date")), TypeDescriptor.valueOf(String.class));
		assertEquals("10/31/09", formatted);
		LocalDate date = new LocalDate(formattingService.convert("10/31/09", TypeDescriptor.valueOf(String.class),
				new TypeDescriptor(Model.class.getField("date"))));
		assertEquals(new LocalDate(2009, 10, 31), date);
	}
	
	@Test
	public void testPrintNull() throws ParseException {
		formattingService.addFormatterForFieldType(Number.class, new NumberFormatter());
		assertEquals("", formattingService.convert(null, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class)));
	}

	@Test
	public void testParseNull() throws ParseException {
		formattingService.addFormatterForFieldType(Number.class, new NumberFormatter());
		assertNull(formattingService.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
	}

	@Test
	public void testParseEmptyString() throws ParseException {
		formattingService.addFormatterForFieldType(Number.class, new NumberFormatter());
		assertNull(formattingService.convert("", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
	}

	@Test
	public void testPrintNullDefault() throws ParseException {
		assertEquals(null, formattingService.convert(null, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class)));
	}

	@Test
	public void testParseNullDefault() throws ParseException {
		assertNull(formattingService.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
	}

	@Test
	public void testParseEmptyStringDefault() throws ParseException {
		assertNull(formattingService.convert("", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
	}

	private static class Model {

		@SuppressWarnings("unused")
		@org.springframework.format.annotation.DateTimeFormat(style="S-")
		public Date date;

	}

}
