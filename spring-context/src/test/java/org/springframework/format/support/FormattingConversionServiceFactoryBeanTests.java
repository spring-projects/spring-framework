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

package org.springframework.format.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.NumberFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class FormattingConversionServiceFactoryBeanTests {

	@Test
	public void testDefaultFormattersOn() throws Exception {
		FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
		factory.afterPropertiesSet();
		FormattingConversionService fcs = factory.getObject();
		TypeDescriptor descriptor = new TypeDescriptor(TestBean.class.getDeclaredField("pattern"));

		LocaleContextHolder.setLocale(Locale.GERMAN);
		try {
			Object value = fcs.convert("15,00", TypeDescriptor.valueOf(String.class), descriptor);
			assertThat(value).isEqualTo(15.0);
			value = fcs.convert(15.0, descriptor, TypeDescriptor.valueOf(String.class));
			assertThat(value).isEqualTo("15");
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	@Test
	public void testDefaultFormattersOff() throws Exception {
		FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
		factory.setRegisterDefaultFormatters(false);
		factory.afterPropertiesSet();
		FormattingConversionService fcs = factory.getObject();
		TypeDescriptor descriptor = new TypeDescriptor(TestBean.class.getDeclaredField("pattern"));

		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				fcs.convert("15,00", TypeDescriptor.valueOf(String.class), descriptor))
			.withCauseInstanceOf(NumberFormatException.class);
	}

	@Test
	public void testCustomFormatter() throws Exception {
		FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
		Set<Object> formatters = new HashSet<>();
		formatters.add(new TestBeanFormatter());
		formatters.add(new SpecialIntAnnotationFormatterFactory());
		factory.setFormatters(formatters);
		factory.afterPropertiesSet();
		FormattingConversionService fcs = factory.getObject();

		TestBean testBean = fcs.convert("5", TestBean.class);
		assertThat(testBean.getSpecialInt()).isEqualTo(5);
		assertThat(fcs.convert(testBean, String.class)).isEqualTo("5");

		TypeDescriptor descriptor = new TypeDescriptor(TestBean.class.getDeclaredField("specialInt"));
		Object value = fcs.convert(":5", TypeDescriptor.valueOf(String.class), descriptor);
		assertThat(value).isEqualTo(5);
		value = fcs.convert(5, descriptor, TypeDescriptor.valueOf(String.class));
		assertThat(value).isEqualTo(":5");
	}

	@Test
	public void testFormatterRegistrar() throws Exception {
		FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
		Set<FormatterRegistrar> registrars = new HashSet<>();
		registrars.add(new TestFormatterRegistrar());
		factory.setFormatterRegistrars(registrars);
		factory.afterPropertiesSet();
		FormattingConversionService fcs = factory.getObject();

		TestBean testBean = fcs.convert("5", TestBean.class);
		assertThat(testBean.getSpecialInt()).isEqualTo(5);
		assertThat(fcs.convert(testBean, String.class)).isEqualTo("5");
	}

	@Test
	public void testInvalidFormatter() throws Exception {
		FormattingConversionServiceFactoryBean factory = new FormattingConversionServiceFactoryBean();
		Set<Object> formatters = new HashSet<>();
		formatters.add(new Object());
		factory.setFormatters(formatters);
		assertThatIllegalArgumentException().isThrownBy(factory::afterPropertiesSet);
	}


	@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	private @interface SpecialInt {

		@AliasFor("alias")
		String value() default "";

		@AliasFor("value")
		String alias() default "";
	}


	private static class TestBean {

		@NumberFormat(pattern = "##,00")
		private double pattern;

		@SpecialInt("aliased")
		private int specialInt;

		public int getSpecialInt() {
			return specialInt;
		}

		public void setSpecialInt(int field) {
			this.specialInt = field;
		}
	}


	private static class TestBeanFormatter implements Formatter<TestBean> {

		@Override
		public String print(TestBean object, Locale locale) {
			return String.valueOf(object.getSpecialInt());
		}

		@Override
		public TestBean parse(String text, Locale locale) throws ParseException {
			TestBean object = new TestBean();
			object.setSpecialInt(Integer.parseInt(text));
			return object;
		}
	}


	private static class SpecialIntAnnotationFormatterFactory implements AnnotationFormatterFactory<SpecialInt> {

		private final Set<Class<?>> fieldTypes = new HashSet<>(1);

		public SpecialIntAnnotationFormatterFactory() {
			fieldTypes.add(Integer.class);
		}

		@Override
		public Set<Class<?>> getFieldTypes() {
			return fieldTypes;
		}

		@Override
		public Printer<?> getPrinter(SpecialInt annotation, Class<?> fieldType) {
			assertThat(annotation.value()).isEqualTo("aliased");
			assertThat(annotation.alias()).isEqualTo("aliased");
			return new Printer<Integer>() {
				@Override
				public String print(Integer object, Locale locale) {
					return ":" + object.toString();
				}
			};
		}

		@Override
		public Parser<?> getParser(SpecialInt annotation, Class<?> fieldType) {
			assertThat(annotation.value()).isEqualTo("aliased");
			assertThat(annotation.alias()).isEqualTo("aliased");
			return new Parser<Integer>() {
				@Override
				public Integer parse(String text, Locale locale) throws ParseException {
					return Integer.parseInt(text.substring(1));
				}
			};
		}
	}


	private static class TestFormatterRegistrar implements FormatterRegistrar {

		@Override
		public void registerFormatters(FormatterRegistry registry) {
			registry.addFormatter(new TestBeanFormatter());
		}
	}

}
