/*
 * Copyright 2002-present the original author or authors.
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.FieldError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Phillip Webb
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class DateFormattingTests {

	private final FormattingConversionService conversionService = new FormattingConversionService();

	private DataBinder binder;


	@BeforeEach
	void setup() {
		DefaultConversionService.addDefaultConverters(conversionService);
		setup(new DateFormatterRegistrar());
	}

	private void setup(DateFormatterRegistrar registrar) {
		registrar.registerFormatters(conversionService);

		SimpleDateBean bean = new SimpleDateBean();
		bean.getChildren().add(new SimpleDateBean());
		binder = new DataBinder(bean);
		binder.setConversionService(conversionService);

		LocaleContextHolder.setLocale(Locale.US);
	}

	@AfterEach
	void tearDown() {
		LocaleContextHolder.setLocale(null);
	}


	@Test
	void testBindLong() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("millis", "1256961600");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("millis")).isEqualTo("1256961600");
	}

	@Test
	void testBindLongAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleMillis", "10/31/09");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("styleMillis")).isEqualTo("10/31/09");
	}

	@Test
	void testBindCalendarAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleCalendar", "10/31/09");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("styleCalendar")).isEqualTo("10/31/09");
	}

	@Test
	void testBindDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleDate", "10/31/09");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("styleDate")).isEqualTo("10/31/09");
	}

	@Test
	void styleDateWithInvalidFormat() {
		String propertyName = "styleDate";
		String propertyValue = "99/01/01";
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add(propertyName, propertyValue);
		binder.bind(propertyValues);
		BindingResult bindingResult = binder.getBindingResult();
		assertThat(bindingResult.getErrorCount()).isEqualTo(1);
		FieldError fieldError = bindingResult.getFieldError(propertyName);
		TypeMismatchException exception = fieldError.unwrap(TypeMismatchException.class);
		assertThat(exception)
			.hasMessageContaining("for property 'styleDate'")
			.hasCauseInstanceOf(ConversionFailedException.class).cause()
				.hasMessageContaining("for value [99/01/01]")
				.hasCauseInstanceOf(IllegalArgumentException.class).cause()
					.hasMessageContaining("Parse attempt failed for value [99/01/01]")
					.hasCauseInstanceOf(ParseException.class).cause()
						// Unable to parse date time value "99/01/01" using configuration from
						// @org.springframework.format.annotation.DateTimeFormat(pattern=, style=S-, iso=NONE, fallbackPatterns=[])
						// We do not check "fallbackPatterns=[]", since the array representation in the toString()
						// implementation for annotations changed from [] to {} in Java 9. In addition, strings
						// are enclosed in double quotes beginning with Java 9. Thus, we cannot check directly
						// for the presence of "style=S-".
						.hasMessageContainingAll(
							"Unable to parse date time value \"99/01/01\" using configuration from",
							"@org.springframework.format.annotation.DateTimeFormat",
							"style=", "S-", "iso=NONE")
						.hasCauseInstanceOf(ParseException.class).cause()
							.hasMessageStartingWith("Unparseable date: \"99/01/01\"")
							.hasNoCause();
	}

	@Test
	void testBindDateArray() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleDate", new String[]{"10/31/09 12:00 PM"});
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
	}

	@Test
	void testBindDateAnnotatedWithError() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleDate", "Oct X31, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getFieldErrorCount("styleDate")).isEqualTo(1);
		assertThat(binder.getBindingResult().getFieldValue("styleDate")).isEqualTo("Oct X31, 2009");
	}

	@Test
	@Disabled
	void testBindDateAnnotatedWithFallbackError() {
		// TODO This currently passes because the Date(String) constructor fallback is used
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("styleDate", "Oct 031, 2009");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getFieldErrorCount("styleDate")).isEqualTo(1);
		assertThat(binder.getBindingResult().getFieldValue("styleDate")).isEqualTo("Oct 031, 2009");
	}

	@Test
	void testBindDateTimePatternAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("patternDate", "10/31/09 1:05");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("patternDate")).isEqualTo("10/31/09 1:05");
	}

	@Test
	void testBindDateTimePatternAnnotatedWithGlobalFormat() {
		DateFormatterRegistrar registrar = new DateFormatterRegistrar();
		DateFormatter dateFormatter = new DateFormatter();
		dateFormatter.setIso(ISO.DATE_TIME);
		registrar.setFormatter(dateFormatter);
		setup(registrar);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("patternDate", "10/31/09 1:05");
		binder.bind(propertyValues);
		BindingResult bindingResult = binder.getBindingResult();
		assertThat(bindingResult.getErrorCount()).isEqualTo(0);
		assertThat(bindingResult.getFieldValue("patternDate")).isEqualTo("10/31/09 1:05");
	}

	@Test
	void testBindDateTimePatternAnnotatedWithOverflow() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("patternDate", "02/29/09 12:00 PM");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(1);
	}

	@Test
	void testBindISODate() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoDate", "2009-10-31");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("isoDate")).isEqualTo("2009-10-31");
	}

	@Test
	void testBindISOTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoTime", "12:00:00.000-05:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("isoTime")).isEqualTo("17:00:00.000Z");
	}

	@Test
	void testBindISODateTime() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("isoDateTime", "2009-10-31T12:00:00.000-08:00");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("isoDateTime")).isEqualTo("2009-10-31T20:00:00.000Z");
	}

	@Test
	void testBindNestedDateAnnotated() {
		MutablePropertyValues propertyValues = new MutablePropertyValues();
		propertyValues.add("children[0].styleDate", "10/31/09");
		binder.bind(propertyValues);
		assertThat(binder.getBindingResult().getErrorCount()).isEqualTo(0);
		assertThat(binder.getBindingResult().getFieldValue("children[0].styleDate")).isEqualTo("10/31/09");
	}

	@Test
	void dateToStringWithoutGlobalFormat() {
		Date date = new Date();
		Object actual = this.conversionService.convert(date, TypeDescriptor.valueOf(Date.class), TypeDescriptor.valueOf(String.class));
		String expected = date.toString();
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void dateToStringWithGlobalFormat() {
		DateFormatterRegistrar registrar = new DateFormatterRegistrar();
		registrar.setFormatter(new DateFormatter());
		setup(registrar);
		Date date = new Date();
		Object actual = this.conversionService.convert(date, TypeDescriptor.valueOf(Date.class), TypeDescriptor.valueOf(String.class));
		String expected = new DateFormatter().print(date, Locale.US);
		assertThat(actual).isEqualTo(expected);
	}

	@Test  // SPR-10105
	@SuppressWarnings("deprecation")
	void stringToDateWithoutGlobalFormat() {
		String string = "Sat, 12 Aug 1995 13:30:00 GM";
		Date date = this.conversionService.convert(string, Date.class);
		assertThat(date).isEqualTo(new Date(string));
	}

	@Test  // SPR-10105
	void stringToDateWithGlobalFormat() {
		DateFormatterRegistrar registrar = new DateFormatterRegistrar();
		DateFormatter dateFormatter = new DateFormatter();
		dateFormatter.setIso(ISO.DATE_TIME);
		registrar.setFormatter(dateFormatter);
		setup(registrar);
		// This is a format that cannot be parsed by new Date(String)
		String string = "2009-06-01T14:23:05.003+00:00";
		Date date = this.conversionService.convert(string, Date.class);
		assertThat(date).isNotNull();
	}


	@Nested
	class FallbackPatternTests {

		@ParameterizedTest(name = "input date: {0}")
		@ValueSource(strings = {"2021-03-02", "2021.03.02", "20210302", "3/2/21"})
		void styleCalendar(String propertyValue) {
			String propertyName = "styleCalendarWithFallbackPatterns";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isEqualTo(0);
			assertThat(bindingResult.getFieldValue(propertyName)).isEqualTo("3/2/21");
		}

		@ParameterizedTest(name = "input date: {0}")
		@ValueSource(strings = {"2021-03-02", "2021.03.02", "20210302", "3/2/21"})
		void styleDate(String propertyValue) {
			String propertyName = "styleDateWithFallbackPatterns";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isEqualTo(0);
			assertThat(bindingResult.getFieldValue(propertyName)).isEqualTo("3/2/21");
		}

		@ParameterizedTest(name = "input date: {0}")
		@ValueSource(strings = {"2021-03-02", "2021.03.02", "20210302", "3/2/21"})
		void patternDate(String propertyValue) {
			String propertyName = "patternDateWithFallbackPatterns";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isEqualTo(0);
			assertThat(bindingResult.getFieldValue(propertyName)).isEqualTo("2021-03-02");
		}

		@ParameterizedTest(name = "input date: {0}")
		@ValueSource(strings = {"2021-03-02", "2021.03.02", "20210302", "3/2/21"})
		void isoDate(String propertyValue) {
			String propertyName = "isoDateWithFallbackPatterns";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isEqualTo(0);
			assertThat(bindingResult.getFieldValue(propertyName)).isEqualTo("2021-03-02");
		}

		/**
		 * {@link SimpleDateBean#styleDateTimeWithFallbackPatternsForPreAndPostJdk20}
		 * configures "SS" as the date/time style to use. Thus, we have to be aware
		 * of the following if we do not configure fallback patterns for parsing.
		 *
		 * <ul>
		 * <li>JDK &le; 19 requires a standard space before the "PM".
		 * <li>JDK &ge; 20 requires a narrow non-breaking space (NNBSP) before the "PM".
		 * </ul>
		 *
		 * <p>To avoid compatibility issues between JDK versions, we have configured
		 * two fallback patterns which emulate the "SS" style: <code>"MM/dd/yy h:mm a"</code>
		 * matches against a standard space before the "PM", and <code>"MM/dd/yy h:mm&#92;u202Fa"</code>
		 * matches against a narrow non-breaking space (NNBSP) before the "PM".
		 *
		 * <p>Thus, the following should theoretically be supported on any JDK (or at least
		 * JDK 17 - 23, where we have tested it).
		 *
		 * @see #patternDateTime(String)
		 */
		@ParameterizedTest(name = "input date: {0}")  // gh-33151
		@ValueSource(strings = {"10/31/09, 12:00 PM", "10/31/09, 12:00\u202FPM"})
		void styleDateTime_PreAndPostJdk20(String propertyValue) {
			String propertyName = "styleDateTimeWithFallbackPatternsForPreAndPostJdk20";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isEqualTo(0);
			String value = binder.getBindingResult().getFieldValue(propertyName).toString();
			// Since the "SS" style is always used for printing and the underlying format
			// changes depending on the JDK version, we cannot be certain that a normal
			// space is used before the "PM". Consequently we have to use a regular
			// expression to match against any Unicode space character (\p{Zs}).
			assertThat(value).startsWith("10/31/09").matches(".+?12:00\\p{Zs}PM");
		}

		/**
		 * To avoid the use of Locale-based styles (such as "MM") for
		 * {@link SimpleDateBean#patternDateTimeWithFallbackPatternForPreAndPostJdk20}, we have configured a
		 * primary pattern (<code>"MM/dd/yy h:mm a"</code>) that matches against a standard space
		 * before the "PM" and a fallback pattern (<code>"MM/dd/yy h:mm&#92;u202Fa"</code> that matches
		 * against a narrow non-breaking space (NNBSP) before the "PM".
		 *
		 * <p>Thus, the following should theoretically be supported on any JDK (or at least
		 * JDK 17 - 23, where we have tested it).
		 *
		 * @see #styleDateTime(String)
		 */
		@ParameterizedTest(name = "input date: {0}")  // gh-33151
		@ValueSource(strings = {"10/31/09 3:45 PM", "10/31/09 3:45\u202FPM"})
		void patternDateTime_PreAndPostJdk20(String propertyValue) {
			String propertyName = "patternDateTimeWithFallbackPatternForPreAndPostJdk20";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isEqualTo(0);
			String value = binder.getBindingResult().getFieldValue(propertyName).toString();
			// Since the "MM/dd/yy h:mm a" primary pattern is always used for printing, we
			// can be certain that a normal space is used before the "PM".
			assertThat(value).matches("10/31/09 3:45 PM");
		}

		@Test
		void patternDateWithUnsupportedPattern() {
			String propertyValue = "210302";
			String propertyName = "patternDateWithFallbackPatterns";
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.add(propertyName, propertyValue);
			binder.bind(propertyValues);
			BindingResult bindingResult = binder.getBindingResult();
			assertThat(bindingResult.getErrorCount()).isEqualTo(1);
			FieldError fieldError = bindingResult.getFieldError(propertyName);
			assertThat(fieldError.unwrap(TypeMismatchException.class))
				.hasMessageContaining("for property 'patternDateWithFallbackPatterns'")
				.hasCauseInstanceOf(ConversionFailedException.class).cause()
					.hasMessageContaining("for value [210302]")
					.hasCauseInstanceOf(IllegalArgumentException.class).cause()
						.hasMessageContaining("Parse attempt failed for value [210302]")
						.hasCauseInstanceOf(ParseException.class).cause()
							// Unable to parse date time value "210302" using configuration from
							// @org.springframework.format.annotation.DateTimeFormat(
							// pattern=yyyy-MM-dd, style=SS, iso=NONE, fallbackPatterns=[M/d/yy, yyyyMMdd, yyyy.MM.dd])
							.hasMessageContainingAll(
								"Unable to parse date time value \"210302\" using configuration from",
								"@org.springframework.format.annotation.DateTimeFormat",
								"yyyy-MM-dd", "M/d/yy", "yyyyMMdd", "yyyy.MM.dd")
							.hasCauseInstanceOf(ParseException.class).cause()
								.hasMessageStartingWith("Unparseable date: \"210302\"")
								.hasNoCause();
		}
	}


	@SuppressWarnings("unused")
	private static class SimpleDateBean {

		private Long millis;

		private Long styleMillis;

		@DateTimeFormat(style = "S-")
		private Calendar styleCalendar;

		@DateTimeFormat(style = "S-", fallbackPatterns = { "yyyy-MM-dd", "yyyyMMdd", "yyyy.MM.dd" })
		private Calendar styleCalendarWithFallbackPatterns;

		@DateTimeFormat(style = "S-")
		private Date styleDate;

		@DateTimeFormat(style = "S-", fallbackPatterns = { "yyyy-MM-dd", "yyyyMMdd", "yyyy.MM.dd" })
		private Date styleDateWithFallbackPatterns;

		// "SS" style matches either a standard space or a narrow non-breaking space (NNBSP) before AM/PM,
		// depending on the version of the JDK.
		// Fallback patterns match a standard space OR a narrow non-breaking space (NNBSP) before AM/PM.
		@DateTimeFormat(style = "SS", fallbackPatterns = { "M/d/yy, h:mm a", "M/d/yy, h:mm\u202Fa" })
		private Date styleDateTimeWithFallbackPatternsForPreAndPostJdk20;

		@DateTimeFormat(pattern = "M/d/yy h:mm")
		private Date patternDate;

		@DateTimeFormat(pattern = "yyyy-MM-dd", fallbackPatterns = { "M/d/yy", "yyyyMMdd", "yyyy.MM.dd" })
		private Date patternDateWithFallbackPatterns;

		// Primary pattern matches a standard space before AM/PM.
		// Fallback pattern matches a narrow non-breaking space (NNBSP) before AM/PM.
		@DateTimeFormat(pattern = "MM/dd/yy h:mm a", fallbackPatterns = "MM/dd/yy h:mm\u202Fa")
		private Date patternDateTimeWithFallbackPatternForPreAndPostJdk20;

		@DateTimeFormat(iso = ISO.DATE)
		private Date isoDate;

		@DateTimeFormat(iso = ISO.DATE, fallbackPatterns = { "M/d/yy", "yyyyMMdd", "yyyy.MM.dd" })
		private Date isoDateWithFallbackPatterns;

		@DateTimeFormat(iso = ISO.TIME)
		private Date isoTime;

		@DateTimeFormat(iso = ISO.DATE_TIME)
		private Date isoDateTime;

		private final List<SimpleDateBean> children = new ArrayList<>();


		public Long getMillis() {
			return this.millis;
		}

		public void setMillis(Long millis) {
			this.millis = millis;
		}

		@DateTimeFormat(style="S-")
		public Long getStyleMillis() {
			return this.styleMillis;
		}

		public void setStyleMillis(@DateTimeFormat(style="S-") Long styleMillis) {
			this.styleMillis = styleMillis;
		}

		public Calendar getStyleCalendar() {
			return this.styleCalendar;
		}

		public void setStyleCalendar(Calendar styleCalendar) {
			this.styleCalendar = styleCalendar;
		}

		public Calendar getStyleCalendarWithFallbackPatterns() {
			return this.styleCalendarWithFallbackPatterns;
		}

		public void setStyleCalendarWithFallbackPatterns(Calendar styleCalendarWithFallbackPatterns) {
			this.styleCalendarWithFallbackPatterns = styleCalendarWithFallbackPatterns;
		}

		public Date getStyleDate() {
			return this.styleDate;
		}

		public void setStyleDate(Date styleDate) {
			this.styleDate = styleDate;
		}

		public Date getStyleDateWithFallbackPatterns() {
			return this.styleDateWithFallbackPatterns;
		}

		public void setStyleDateWithFallbackPatterns(Date styleDateWithFallbackPatterns) {
			this.styleDateWithFallbackPatterns = styleDateWithFallbackPatterns;
		}

		public Date getStyleDateTimeWithFallbackPatternsForPreAndPostJdk20() {
			return this.styleDateTimeWithFallbackPatternsForPreAndPostJdk20;
		}

		public void setStyleDateTimeWithFallbackPatternsForPreAndPostJdk20(Date styleDateTimeWithFallbackPatternsForPreAndPostJdk20) {
			this.styleDateTimeWithFallbackPatternsForPreAndPostJdk20 = styleDateTimeWithFallbackPatternsForPreAndPostJdk20;
		}

		public Date getPatternDate() {
			return this.patternDate;
		}

		public void setPatternDate(Date patternDate) {
			this.patternDate = patternDate;
		}

		public Date getPatternDateWithFallbackPatterns() {
			return this.patternDateWithFallbackPatterns;
		}

		public void setPatternDateWithFallbackPatterns(Date patternDateWithFallbackPatterns) {
			this.patternDateWithFallbackPatterns = patternDateWithFallbackPatterns;
		}

		public Date getPatternDateTimeWithFallbackPatternForPreAndPostJdk20() {
			return this.patternDateTimeWithFallbackPatternForPreAndPostJdk20;
		}

		public void setPatternDateTimeWithFallbackPatternForPreAndPostJdk20(Date patternDateTimeWithFallbackPatternForPreAndPostJdk20) {
			this.patternDateTimeWithFallbackPatternForPreAndPostJdk20 = patternDateTimeWithFallbackPatternForPreAndPostJdk20;
		}

		public Date getIsoDate() {
			return this.isoDate;
		}

		public void setIsoDate(Date isoDate) {
			this.isoDate = isoDate;
		}

		public Date getIsoDateWithFallbackPatterns() {
			return this.isoDateWithFallbackPatterns;
		}

		public void setIsoDateWithFallbackPatterns(Date isoDateWithFallbackPatterns) {
			this.isoDateWithFallbackPatterns = isoDateWithFallbackPatterns;
		}

		public Date getIsoTime() {
			return this.isoTime;
		}

		public void setIsoTime(Date isoTime) {
			this.isoTime = isoTime;
		}

		public Date getIsoDateTime() {
			return this.isoDateTime;
		}

		public void setIsoDateTime(Date isoDateTime) {
			this.isoDateTime = isoDateTime;
		}

		public List<SimpleDateBean> getChildren() {
			return this.children;
		}
	}

}
