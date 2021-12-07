/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.format.support;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.Formatter;
import org.springframework.format.number.NumberStyleFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Kazuki Shimizu
 * @author Sam Brannen
 */
public class FormattingConversionServiceTests {

	private FormattingConversionService formattingService;


	@BeforeEach
	public void setUp() {
		formattingService = new FormattingConversionService();
		DefaultConversionService.addDefaultConverters(formattingService);
		LocaleContextHolder.setLocale(Locale.US);
	}

	@AfterEach
	public void tearDown() {
		LocaleContextHolder.setLocale(null);
	}


	@Test
	public void formatFieldForTypeWithFormatter() {
		formattingService.addFormatterForFieldType(Number.class, new NumberStyleFormatter());
		String formatted = formattingService.convert(3, String.class);
		assertThat(formatted).isEqualTo("3");
		Integer i = formattingService.convert("3", Integer.class);
		assertThat(i).isEqualTo(3);
	}

	@Test
	public void printNull() {
		formattingService.addFormatterForFieldType(Number.class, new NumberStyleFormatter());
		assertThat(formattingService.convert(null, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class))).isEqualTo("");
	}

	@Test
	public void parseNull() {
		formattingService.addFormatterForFieldType(Number.class, new NumberStyleFormatter());
		assertThat(formattingService
				.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class))).isNull();
	}

	@Test
	public void parseEmptyString() {
		formattingService.addFormatterForFieldType(Number.class, new NumberStyleFormatter());
		assertThat(formattingService.convert("", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class))).isNull();
	}

	@Test
	public void parseBlankString() {
		formattingService.addFormatterForFieldType(Number.class, new NumberStyleFormatter());
		assertThat(formattingService.convert("     ", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class))).isNull();
	}

	@Test
	public void parseParserReturnsNull() {
		formattingService.addFormatterForFieldType(Integer.class, new NullReturningFormatter());
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				formattingService.convert("1", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class)));
	}

	@Test
	public void parseNullPrimitiveProperty() {
		formattingService.addFormatterForFieldType(Integer.class, new NumberStyleFormatter());
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				formattingService.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(int.class)));
	}

	@Test
	public void printNullDefault() {
		assertThat(formattingService
				.convert(null, TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class))).isEqualTo(null);
	}

	@Test
	public void parseNullDefault() {
		assertThat(formattingService
				.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class))).isNull();
	}

	@Test
	public void parseEmptyStringDefault() {
		assertThat(formattingService.convert("", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class))).isNull();
	}

	@Test
	public void introspectedFormatter() {
		formattingService.addFormatter(new NumberStyleFormatter("#,#00.0#"));
		assertThat(formattingService.convert(123, String.class)).isEqualTo("123.0");
		assertThat(formattingService.convert("123.0", Integer.class)).isEqualTo(123);
	}

	@Test
	public void introspectedPrinter() {
		formattingService.addPrinter(new NumberStyleFormatter("#,#00.0#"));
		assertThat(formattingService.convert(123, String.class)).isEqualTo("123.0");
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				formattingService.convert("123.0", Integer.class))
			.withCauseInstanceOf(NumberFormatException.class);
	}

	@Test
	public void introspectedParser() {
		formattingService.addParser(new NumberStyleFormatter("#,#00.0#"));
		assertThat(formattingService.convert("123.0", Integer.class)).isEqualTo(123);
		assertThat(formattingService.convert(123, String.class)).isEqualTo("123");
	}

	@Test
	public void proxiedFormatter() {
		Formatter<?> formatter = new NumberStyleFormatter();
		formattingService.addFormatter((Formatter<?>) new ProxyFactory(formatter).getProxy());
		assertThat(formattingService.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Integer.class))).isNull();
	}

	@Test
	public void introspectedConverter() {
		formattingService.addConverter(new IntegerConverter());
		assertThat(formattingService.convert("1", Integer.class)).isEqualTo(Integer.valueOf(1));
	}

	@Test
	public void proxiedConverter() {
		Converter<?, ?> converter = new IntegerConverter();
		formattingService.addConverter((Converter<?, ?>) new ProxyFactory(converter).getProxy());
		assertThat(formattingService.convert("1", Integer.class)).isEqualTo(Integer.valueOf(1));
	}

	@Test
	public void introspectedConverterFactory() {
		formattingService.addConverterFactory(new IntegerConverterFactory());
		assertThat(formattingService.convert("1", Integer.class)).isEqualTo(Integer.valueOf(1));
	}

	@Test
	public void proxiedConverterFactory() {
		ConverterFactory<?, ?> converterFactory = new IntegerConverterFactory();
		formattingService.addConverterFactory((ConverterFactory<?, ?>) new ProxyFactory(converterFactory).getProxy());
		assertThat(formattingService.convert("1", Integer.class)).isEqualTo(Integer.valueOf(1));
	}


	public static class NullReturningFormatter implements Formatter<Integer> {

		@Override
		public String print(Integer object, Locale locale) {
			return null;
		}

		@Override
		public Integer parse(String text, Locale locale) {
			return null;
		}
	}


	private static class IntegerConverter implements Converter<String, Integer> {

		@Override
		public Integer convert(String source) {
			return Integer.parseInt(source);
		}
	}


	private static class IntegerConverterFactory implements ConverterFactory<String, Number> {

		@Override
		@SuppressWarnings("unchecked")
		public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
			if (Integer.class == targetType) {
				return (Converter<String, T>) new IntegerConverter();
			}
			else {
				throw new IllegalStateException();
			}
		}
	}

}
