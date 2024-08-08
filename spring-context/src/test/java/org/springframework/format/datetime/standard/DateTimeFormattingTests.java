/*
 * Copyright 2002-2024 the original author or authors.
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

import java.time.DateTimeException;
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
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.annotation.DurationFormat.Style;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.JRE.JAVA_19;
import static org.junit.jupiter.api.condition.JRE.JAVA_20;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Kazuki Shimizu
 */
class DateTimeFormattingTests {

	// JDK <= 19 requires a standard space before "AM/PM".
	// JDK >= 20 requires a NNBSP before "AM/PM".
	// \u202F is a narrow non-breaking space (NNBSP).
	private static final String TIME_SEPARATOR = (Runtime.version().feature() < 20 ? " " : "\u202F");


	private final FormattingConversionService conversionService = new FormattingConversionService();

	private DataBinder binder;


	@BeforeEach
	void setup() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		setup(registrar);
	}

	private void setup(DateTimeFormatterRegistrar registrar) {
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
	void cleanup() {
		LocaleContextHolder.setLocale(null);
		DateTimeContextHolder.setDateTimeContext(null);
	}


	@Test
	void testBindLocalDate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "10/31/09");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("localDate")).isEqualTo("10/31/09");
	}

	@Test
	void testBindLocalDateWithISO() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "2009-10-31");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("localDate")).isEqualTo("10/31/09");
	}

	@Test
	void testBindLocalDateWithSpecificStyle() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateStyle(FormatStyle.LONG);
		setup(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "October 31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("localDate")).isEqualTo("October 31, 2009");
	}

	@Test
	void testBindLocalDateWithSpecificFormatter() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateFormatter(DateTimeFormatter.ofPattern("yyyyMMdd"));
		setup(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", "20091031");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("localDate")).isEqualTo("20091031");
	}

	@Test
	void testBindLocalDateArray() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", new String[] {"10/31/09"});
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
	}

	@Test
	void testBindLocalDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleLocalDate", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("styleLocalDate")).isEqualTo("Oct 31, 2009");
	}

	@Test
	void testBindLocalDateAnnotatedWithError() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleLocalDate", "Oct -31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getFieldErrorCount("styleLocalDate")).isEqualTo(1);
		assertThat(binder.getBindingResult().getFieldValue("styleLocalDate")).isEqualTo("Oct -31, 2009");
	}

	@Test
	void testBindNestedLocalDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("children[0].styleLocalDate", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("children[0].styleLocalDate")).isEqualTo("Oct 31, 2009");
	}

	@Test
	void testBindLocalDateAnnotatedWithDirectFieldAccess() {
		binder.initDirectFieldAccess();
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleLocalDate", "Oct 31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("styleLocalDate")).isEqualTo("Oct 31, 2009");
	}

	@Test
	void testBindLocalDateAnnotatedWithDirectFieldAccessAndError() {
		binder.initDirectFieldAccess();
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleLocalDate", "Oct -31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getFieldErrorCount("styleLocalDate")).isEqualTo(1);
		assertThat(binder.getBindingResult().getFieldValue("styleLocalDate")).isEqualTo("Oct -31, 2009");
	}

	@Test
	void testBindLocalDateFromJavaUtilCalendar() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDate", new GregorianCalendar(2009, 9, 31, 0, 0));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("localDate")).isEqualTo("10/31/09");
	}

	@Test
	void testBindLocalTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "12:00%sPM".formatted(TIME_SEPARATOR));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		// \p{Zs} matches any Unicode space character
		assertThat(binder.getBindingResult().getFieldValue("localTime")).asString().matches("12:00\\p{Zs}PM");
	}

	@Test
	void testBindLocalTimeWithISO() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "12:00:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		// \p{Zs} matches any Unicode space character
		assertThat(binder.getBindingResult().getFieldValue("localTime")).asString().matches("12:00\\p{Zs}PM");
	}

	@Test
	void testBindLocalTimeWithSpecificStyle() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setTimeStyle(FormatStyle.MEDIUM);
		setup(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "12:00:00%sPM".formatted(TIME_SEPARATOR));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		// \p{Zs} matches any Unicode space character
		assertThat(binder.getBindingResult().getFieldValue("localTime")).asString().matches("12:00:00\\p{Zs}PM");
	}

	@Test
	void testBindLocalTimeWithSpecificFormatter() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setTimeFormatter(DateTimeFormatter.ofPattern("HHmmss"));
		setup(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", "130000");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("localTime")).isEqualTo("130000");
	}

	@Test
	void testBindLocalTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleLocalTime", "12:00:00%sPM".formatted(TIME_SEPARATOR));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		// \p{Zs} matches any Unicode space character
		assertThat(binder.getBindingResult().getFieldValue("styleLocalTime")).asString().matches("12:00:00\\p{Zs}PM");
	}

	@Test
	void testBindLocalTimeFromJavaUtilCalendar() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localTime", new GregorianCalendar(1970, 0, 0, 12, 0));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		// \p{Zs} matches any Unicode space character
		assertThat(binder.getBindingResult().getFieldValue("localTime")).asString().matches("12:00\\p{Zs}PM");
	}

	@Test
	void testBindLocalDateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", LocalDateTime.of(2009, 10, 31, 12, 0));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		String value = binder.getBindingResult().getFieldValue("localDateTime").toString();
		// \p{Zs} matches any Unicode space character
		assertThat(value).startsWith("10/31/09").matches(".+?12:00\\p{Zs}PM");
	}

	@Test
	void testBindLocalDateTimeWithISO() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", "2009-10-31T12:00:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		String value = binder.getBindingResult().getFieldValue("localDateTime").toString();
		// \p{Zs} matches any Unicode space character
		assertThat(value).startsWith("10/31/09").matches(".+?12:00\\p{Zs}PM");
	}

	@Test
	void testBindLocalDateTimeAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleLocalDateTime", LocalDateTime.of(2009, 10, 31, 12, 0));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		String value = binder.getBindingResult().getFieldValue("styleLocalDateTime").toString();
		// \p{Zs} matches any Unicode space character
		assertThat(value).startsWith("Oct 31, 2009").matches(".+?12:00:00\\p{Zs}PM");
	}

	@Test
	void testBindLocalDateTimeFromJavaUtilCalendar() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", new GregorianCalendar(2009, 9, 31, 12, 0));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		String value = binder.getBindingResult().getFieldValue("localDateTime").toString();
		// \p{Zs} matches any Unicode space character
		assertThat(value).startsWith("10/31/09").matches(".+?12:00\\p{Zs}PM");
	}

	@Test
	void testBindDateTimeWithSpecificStyle() {
		DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
		registrar.setDateTimeStyle(FormatStyle.MEDIUM);
		setup(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("localDateTime", LocalDateTime.of(2009, 10, 31, 12, 0));
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		String value = binder.getBindingResult().getFieldValue("localDateTime").toString();
		// \p{Zs} matches any Unicode space character
		assertThat(value).startsWith("Oct 31, 2009").matches(".+?12:00:00\\p{Zs}PM");
	}

	@Test
	void testBindPatternLocalDateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("patternLocalDateTime", "10/31/09 12:00 PM");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("patternLocalDateTime")).isEqualTo("10/31/09 12:00 PM");
	}

	@Test
	void testBindDateTimeOverflow() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("patternLocalDateTime", "02/29/09 12:00 PM");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(1);
	}

	@Test
	void testBindISODate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoLocalDate", "2009-10-31");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("isoLocalDate")).isEqualTo("2009-10-31");
	}

	@Test
	void isoLocalDateWithInvalidFormat() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		String propertyName = "isoLocalDate";
		propertyValues.add(propertyName, "2009-31-10");
		binder.bind(propertyValues);
		BindingResult bindingResult = binder.getBindingResult();
		assertThat(bindingResult.getErrorCount()).isEqualTo(1);
		FieldError fieldError = bindingResult.getFieldError(propertyName);
		assertThat(fieldError.unwrap(TypeMismatchException.class))
			.hasMessageContaining("for property 'isoLocalDate'")
			.hasCauseInstanceOf(ConversionFailedException.class).cause()
				.hasMessageContaining("for value [2009-31-10]")
				.hasCauseInstanceOf(IllegalArgumentException.class).cause()
					.hasMessageContaining("Parse attempt failed for value [2009-31-10]")
					.hasCauseInstanceOf(DateTimeParseException.class).cause()
						// Unable to parse date time value "2009-31-10" using configuration from
						// @org.springframework.format.annotation.DateTimeFormat(pattern=, style=SS, iso=DATE, fallbackPatterns=[])
						// We do not check "fallbackPatterns=[]", since the array representation in the toString()
						// implementation for annotations changed from [] to {} in Java 9.
						.hasMessageContainingAll(
							"Unable to parse date time value \"2009-31-10\" using configuration from",
							"@org.springframework.format.annotation.DateTimeFormat", "iso=DATE")
						.hasCauseInstanceOf(DateTimeParseException.class).cause()
							.hasMessageStartingWith("Text '2009-31-10'")
							.hasCauseInstanceOf(DateTimeException.class).cause()
								.hasMessageContaining("Invalid value for MonthOfYear (valid values 1 - 12): 31")
								.hasNoCause();
	}

	@Test
	void testBindISOTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoLocalTime", "12:00:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("isoLocalTime")).isEqualTo("12:00:00");
	}

	@Test
	void testBindISOTimeWithZone() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoLocalTime", "12:00:00.000-05:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("isoLocalTime")).isEqualTo("12:00:00");
	}

	@Test
	void testBindISODateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoLocalDateTime", "2009-10-31T12:00:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("isoLocalDateTime")).isEqualTo("2009-10-31T12:00:00");
	}

	@Test
	void testBindISODateTimeWithZone() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoLocalDateTime", "2009-10-31T12:00:00.000Z");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("isoLocalDateTime")).isEqualTo("2009-10-31T12:00:00");
	}

	@Test
	void testBindInstant() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("instant", "2009-10-31T12:00:00.000Z");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("instant").toString()).startsWith("2009-10-31T12:00");
	}

	@Test
	void testBindInstantAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleInstant", "2017-02-21T13:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("styleInstant")).isEqualTo("2017-02-21T13:00");
	}

	@Test
	@SuppressWarnings("deprecation")
	void testBindInstantFromJavaUtilDate() {
		TimeZone defaultZone = TimeZone.getDefault();
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		try {
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add("instant", new Date(109, 9, 31, 12, 0));
			binder.bind(propertyValues);
			assertThat(binder.getBindingResult().getErrorCount()).isZero();
			assertThat(binder.getBindingResult().getFieldValue("instant").toString()).startsWith("2009-10-31");
		}
		finally {
			TimeZone.setDefault(defaultZone);
		}
	}

	@Test
	void testBindPeriod() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("period", "P6Y3M1D");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("period").toString()).isEqualTo("P6Y3M1D");
	}

	@Test
	void testBindDuration() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("duration", "PT8H6M12.345S");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("duration").toString()).isEqualTo("PT8H6M12.345S");
	}

	@Test
	void testBindDurationAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleDuration", "2ms");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getFieldValue("styleDuration"))
				.isNotNull()
				.isEqualTo("2000us");
		assertThat(binder.getBindingResult().getAllErrors()).isEmpty();
	}

	@Test
	void testBindYear() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("year", "2007");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("year").toString()).isEqualTo("2007");
	}

	@Test
	void testBindMonth() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("month", "JULY");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("month").toString()).isEqualTo("JULY");
	}

	@Test
	void testBindMonthInAnyCase() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("month", "July");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("month").toString()).isEqualTo("JULY");
	}

	@Test
	void testBindYearMonth() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("yearMonth", "2007-12");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("yearMonth").toString()).isEqualTo("2007-12");
	}

	@Test
	void testBindYearMonthAnnotatedPattern() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("yearMonthAnnotatedPattern", "12/2007");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("yearMonthAnnotatedPattern")).isEqualTo("12/2007");
		assertThat(binder.getBindingResult().getRawFieldValue("yearMonthAnnotatedPattern")).isEqualTo(YearMonth.parse("2007-12"));
	}

	@Test
	void testBindMonthDay() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("monthDay", "--12-03");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("monthDay").toString()).isEqualTo("--12-03");
	}

	@Test
	void testBindMonthDayAnnotatedPattern() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("monthDayAnnotatedPattern", "1/3");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isZero();
		assertThat(binder.getBindingResult().getFieldValue("monthDayAnnotatedPattern")).isEqualTo("1/3");
		assertThat(binder.getBindingResult().getRawFieldValue("monthDayAnnotatedPattern")).isEqualTo(MonthDay.parse("--01-03"));
	}


	@Nested
	class FallbackPatternTests {

		@ParameterizedTest(name = "input date: {0}")
		@ValueSource(strings = {"2021-03-02", "2021.03.02", "20210302", "3/2/21"})
		void styleLocalDate(String propertyValue) {
			String propertyName = "styleLocalDateWithFallbackPatterns";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isZero();
			assertThat(bindingResult.getFieldValue(propertyName)).isEqualTo("3/2/21");
		}

		@ParameterizedTest(name = "input date: {0}")
		@ValueSource(strings = {"2021-03-02", "2021.03.02", "20210302", "3/2/21"})
		void patternLocalDate(String propertyValue) {
			String propertyName = "patternLocalDateWithFallbackPatterns";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isZero();
			assertThat(bindingResult.getFieldValue(propertyName)).isEqualTo("2021-03-02");
		}

		@EnabledForJreRange(max = JAVA_19)
		@ParameterizedTest(name = "input date: {0}")
		// JDK <= 19 requires a standard space before the "PM".
		@ValueSource(strings = {"12:00:00 PM", "12:00:00", "12:00"})
		void styleLocalTime_PreJDK20(String propertyValue) {
			styleLocalTime(propertyValue);
		}

		@EnabledForJreRange(min = JAVA_20)
		@ParameterizedTest(name = "input date: {0}")
		// JDK >= 20 requires a NNBSP before the "PM".
		// \u202F is a narrow non-breaking space (NNBSP).
		@ValueSource(strings = {"12:00:00\u202FPM", "12:00:00", "12:00"})
		void styleLocalTime_PostJDK20(String propertyValue) {
			styleLocalTime(propertyValue);
		}

		private void styleLocalTime(String propertyValue) {
			String propertyName = "styleLocalTimeWithFallbackPatterns";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isZero();
			// \p{Zs} matches any Unicode space character
			assertThat(bindingResult.getFieldValue(propertyName)).asString().matches("12:00:00\\p{Zs}PM");
		}

		@ParameterizedTest(name = "input date: {0}")
		@ValueSource(strings = {"2021-03-02T12:00:00", "2021-03-02 12:00:00", "3/2/21 12:00"})
		void isoLocalDateTime(String propertyValue) {
			String propertyName = "isoLocalDateTimeWithFallbackPatterns";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isZero();
			assertThat(bindingResult.getFieldValue(propertyName)).isEqualTo("2021-03-02T12:00:00");
		}

		@Test
		void patternLocalDateWithUnsupportedPattern() {
			String propertyValue = "210302";
			String propertyName = "patternLocalDateWithFallbackPatterns";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isEqualTo(1);
			FieldError fieldError = bindingResult.getFieldError(propertyName);
			assertThat(fieldError.unwrap(TypeMismatchException.class))
				.hasMessageContaining("for property 'patternLocalDateWithFallbackPatterns'")
				.hasCauseInstanceOf(ConversionFailedException.class).cause()
					.hasMessageContaining("for value [210302]")
					.hasCauseInstanceOf(IllegalArgumentException.class).cause()
						.hasMessageContaining("Parse attempt failed for value [210302]")
						.hasCauseInstanceOf(DateTimeParseException.class).cause()
							// Unable to parse date time value "210302" using configuration from
							// @org.springframework.format.annotation.DateTimeFormat(
							// pattern=yyyy-MM-dd, style=SS, iso=NONE, fallbackPatterns=[M/d/yy, yyyyMMdd, yyyy.MM.dd])
							.hasMessageContainingAll(
								"Unable to parse date time value \"210302\" using configuration from",
								"@org.springframework.format.annotation.DateTimeFormat",
								"yyyy-MM-dd", "M/d/yy", "yyyyMMdd", "yyyy.MM.dd")
							.hasCauseInstanceOf(DateTimeParseException.class).cause()
								.hasMessageStartingWith("Text '210302'")
								.hasNoCause();
		}

		@Test
		void testBindInstantAsLongEpochMillis() {
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add("instant", 1234L);
			binder.bind(propertyValues);
			assertThat(binder.getBindingResult().getErrorCount()).isZero();
			assertThat(binder.getBindingResult().getRawFieldValue("instant"))
					.isInstanceOf(Instant.class)
					.isEqualTo(Instant.ofEpochMilli(1234L));
			assertThat(binder.getBindingResult().getFieldValue("instant"))
					.hasToString("1970-01-01T00:00:01.234Z");
		}
	}


	public static class DateTimeBean {

		private LocalDate localDate;

		@DateTimeFormat(style = "M-")
		private LocalDate styleLocalDate;

		@DateTimeFormat(style = "S-", fallbackPatterns = {"yyyy-MM-dd", "yyyyMMdd", "yyyy.MM.dd"})
		private LocalDate styleLocalDateWithFallbackPatterns;

		@DateTimeFormat(pattern = "yyyy-MM-dd", fallbackPatterns = {"M/d/yy", "yyyyMMdd", "yyyy.MM.dd"})
		private LocalDate patternLocalDateWithFallbackPatterns;

		private LocalTime localTime;

		@DateTimeFormat(style = "-M")
		private LocalTime styleLocalTime;

		@DateTimeFormat(style = "-M", fallbackPatterns = {"HH:mm:ss", "HH:mm"})
		private LocalTime styleLocalTimeWithFallbackPatterns;

		private LocalDateTime localDateTime;

		@DateTimeFormat(style = "MM")
		private LocalDateTime styleLocalDateTime;

		@DateTimeFormat(pattern = "M/d/yy h:mm a")
		private LocalDateTime patternLocalDateTime;

		@DateTimeFormat(iso = ISO.DATE)
		private LocalDate isoLocalDate;

		@DateTimeFormat(iso = ISO.TIME)
		private LocalTime isoLocalTime;

		@DateTimeFormat(iso = ISO.DATE_TIME)
		private LocalDateTime isoLocalDateTime;

		@DateTimeFormat(iso = ISO.DATE_TIME, fallbackPatterns = {"yyyy-MM-dd HH:mm:ss", "M/d/yy HH:mm"})
		private LocalDateTime isoLocalDateTimeWithFallbackPatterns;

		private Instant instant;

		@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
		private Instant styleInstant;

		private Period period;

		private Duration duration;

		@DurationFormat(style = Style.SIMPLE, defaultUnit = DurationFormat.Unit.MICROS)
		private Duration styleDuration;

		private Year year;

		private Month month;

		private YearMonth yearMonth;

		@DateTimeFormat(pattern="MM/uuuu")
		private YearMonth yearMonthAnnotatedPattern;

		private MonthDay monthDay;

		@DateTimeFormat(pattern="M/d")
		private MonthDay monthDayAnnotatedPattern;

		private final List<DateTimeBean> children = new ArrayList<>();

		public LocalDate getLocalDate() {
			return this.localDate;
		}

		public void setLocalDate(LocalDate localDate) {
			this.localDate = localDate;
		}

		public LocalDate getStyleLocalDate() {
			return this.styleLocalDate;
		}

		public void setStyleLocalDate(LocalDate styleLocalDate) {
			this.styleLocalDate = styleLocalDate;
		}

		public LocalDate getStyleLocalDateWithFallbackPatterns() {
			return this.styleLocalDateWithFallbackPatterns;
		}

		public void setStyleLocalDateWithFallbackPatterns(LocalDate styleLocalDateWithFallbackPatterns) {
			this.styleLocalDateWithFallbackPatterns = styleLocalDateWithFallbackPatterns;
		}
		public LocalDate getPatternLocalDateWithFallbackPatterns() {
			return this.patternLocalDateWithFallbackPatterns;
		}

		public void setPatternLocalDateWithFallbackPatterns(LocalDate patternLocalDateWithFallbackPatterns) {
			this.patternLocalDateWithFallbackPatterns = patternLocalDateWithFallbackPatterns;
		}

		public LocalTime getLocalTime() {
			return this.localTime;
		}

		public void setLocalTime(LocalTime localTime) {
			this.localTime = localTime;
		}

		public LocalTime getStyleLocalTime() {
			return this.styleLocalTime;
		}

		public void setStyleLocalTime(LocalTime styleLocalTime) {
			this.styleLocalTime = styleLocalTime;
		}

		public LocalTime getStyleLocalTimeWithFallbackPatterns() {
			return this.styleLocalTimeWithFallbackPatterns;
		}

		public void setStyleLocalTimeWithFallbackPatterns(LocalTime styleLocalTimeWithFallbackPatterns) {
			this.styleLocalTimeWithFallbackPatterns = styleLocalTimeWithFallbackPatterns;
		}

		public LocalDateTime getLocalDateTime() {
			return this.localDateTime;
		}

		public void setLocalDateTime(LocalDateTime localDateTime) {
			this.localDateTime = localDateTime;
		}

		public LocalDateTime getStyleLocalDateTime() {
			return this.styleLocalDateTime;
		}

		public void setStyleLocalDateTime(LocalDateTime styleLocalDateTime) {
			this.styleLocalDateTime = styleLocalDateTime;
		}

		public LocalDateTime getPatternLocalDateTime() {
			return this.patternLocalDateTime;
		}

		public void setPatternLocalDateTime(LocalDateTime patternLocalDateTime) {
			this.patternLocalDateTime = patternLocalDateTime;
		}

		public LocalDate getIsoLocalDate() {
			return this.isoLocalDate;
		}

		public void setIsoLocalDate(LocalDate isoLocalDate) {
			this.isoLocalDate = isoLocalDate;
		}

		public LocalTime getIsoLocalTime() {
			return this.isoLocalTime;
		}

		public void setIsoLocalTime(LocalTime isoLocalTime) {
			this.isoLocalTime = isoLocalTime;
		}

		public LocalDateTime getIsoLocalDateTime() {
			return this.isoLocalDateTime;
		}

		public void setIsoLocalDateTime(LocalDateTime isoLocalDateTime) {
			this.isoLocalDateTime = isoLocalDateTime;
		}

		public LocalDateTime getIsoLocalDateTimeWithFallbackPatterns() {
			return this.isoLocalDateTimeWithFallbackPatterns;
		}

		public void setIsoLocalDateTimeWithFallbackPatterns(LocalDateTime isoLocalDateTimeWithFallbackPatterns) {
			this.isoLocalDateTimeWithFallbackPatterns = isoLocalDateTimeWithFallbackPatterns;
		}

		public Instant getInstant() {
			return this.instant;
		}

		public void setInstant(Instant instant) {
			this.instant = instant;
		}

		public Instant getStyleInstant() {
			return this.styleInstant;
		}

		public void setStyleInstant(Instant styleInstant) {
			this.styleInstant = styleInstant;
		}

		public Period getPeriod() {
			return this.period;
		}

		public void setPeriod(Period period) {
			this.period = period;
		}

		public Duration getDuration() {
			return this.duration;
		}

		public void setDuration(Duration duration) {
			this.duration = duration;
		}

		public Duration getStyleDuration() {
			return this.styleDuration;
		}

		public void setStyleDuration(Duration styleDuration) {
			this.styleDuration = styleDuration;
		}

		public Year getYear() {
			return this.year;
		}

		public void setYear(Year year) {
			this.year = year;
		}

		public Month getMonth() {
			return this.month;
		}

		public void setMonth(Month month) {
			this.month = month;
		}

		public YearMonth getYearMonth() {
			return this.yearMonth;
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
			return this.monthDay;
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
			return this.children;
		}
	}

}
