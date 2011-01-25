/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.web.servlet.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerExceptionResolver;

/**
 * Test fixture for the configuration in mvc-config-annotation-driven.xml.
 * @author Rossen Stoyanchev
 */
public class AnnotationDrivenBeanDefinitionParserTests {

	private static GenericWebApplicationContext appContext;

	@BeforeClass
	public static void setup() {
		appContext = new GenericWebApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		reader.loadBeanDefinitions(new ClassPathResource("mvc-config-annotation-driven.xml",
				AnnotationDrivenBeanDefinitionParserTests.class));
		appContext.refresh();
	}

	@Test
	public void testMessageCodesResolver() {
		AnnotationMethodHandlerAdapter adapter = appContext.getBean(AnnotationMethodHandlerAdapter.class);
		assertNotNull(adapter);
		Object initializer = new DirectFieldAccessor(adapter).getPropertyValue("webBindingInitializer");
		assertNotNull(initializer);
		MessageCodesResolver resolver = ((ConfigurableWebBindingInitializer) initializer).getMessageCodesResolver();
		assertNotNull(resolver);
		assertEquals(TestMessageCodesResolver.class, resolver.getClass());
	}

	@Test
	public void testMessageConverters() {
		verifyMessageConverters(appContext.getBean(AnnotationMethodHandlerAdapter.class));
		verifyMessageConverters(appContext.getBean(AnnotationMethodHandlerExceptionResolver.class));
	}

	@Test
	public void testFormatters() throws Exception {
		FormattingConversionService conversionService = appContext.getBean(FormattingConversionService.class);
		assertNotNull(conversionService);

		TestBean testBean = conversionService.convert("5", TestBean.class);
		assertEquals(TestBeanFormatter.class.getSimpleName() + " should have been used.", 5, testBean.getField());
		assertEquals("5", conversionService.convert(testBean, String.class));

		TypeDescriptor intTypeDescriptor = new TypeDescriptor(TestBean.class.getDeclaredField("anotherField"));
		Object actual = conversionService.convert(">>5<<", TypeDescriptor.valueOf(String.class), intTypeDescriptor);
		assertEquals(TestBeanAnnotationFormatterFactory.class.getSimpleName() + " should have been used", 5, actual);
		actual = conversionService.convert(5, intTypeDescriptor, TypeDescriptor.valueOf(String.class));
		assertEquals(">>5<<", actual);
	}

	private void verifyMessageConverters(Object bean) {
		assertNotNull(bean);
		Object converters = new DirectFieldAccessor(bean).getPropertyValue("messageConverters");
		assertNotNull(converters);
		assertTrue(converters instanceof HttpMessageConverter<?>[]);
		assertEquals(2, ((HttpMessageConverter<?>[]) converters).length);
		assertTrue(((HttpMessageConverter<?>[]) converters)[0] instanceof StringHttpMessageConverter);
		assertTrue(((HttpMessageConverter<?>[]) converters)[1] instanceof ResourceHttpMessageConverter);
	}

	private static class TestMessageCodesResolver implements MessageCodesResolver {

		public String[] resolveMessageCodes(String errorCode, String objectName) {
			throw new IllegalStateException("Not expected to be invoked");
		}

		@SuppressWarnings("rawtypes")
		public String[] resolveMessageCodes(String errorCode, String objectName, String field, Class fieldType) {
			throw new IllegalStateException("Not expected to be invoked");
		}

	}

	@SuppressWarnings("unused")
	private static class TestWebArgumentResolver implements WebArgumentResolver {

		public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) throws Exception {
			throw new IllegalStateException("Not expected to be invoked");
		}

	}

	@SuppressWarnings("unused")
	private static class AnotherTestWebArgumentResolver implements WebArgumentResolver {

		public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) throws Exception {
			throw new IllegalStateException("Not expected to be invoked");
		}

	}

	private static class TestBeanFormatter implements Formatter<TestBean> {

		public String print(TestBean object, Locale locale) {
			return String.valueOf(object.getField());
		}

		public TestBean parse(String text, Locale locale) throws ParseException {
			TestBean object = new TestBean();
			object.setField(Integer.parseInt(text));
			return object;
		}

	}

	private static class TestBeanAnnotationFormatterFactory implements AnnotationFormatterFactory<SpecialIntFormat> {

		private final Set<Class<?>> fieldTypes = new HashSet<Class<?>>(1);

		@SuppressWarnings("unused")
		public TestBeanAnnotationFormatterFactory() {
			fieldTypes.add(Integer.class);
		}

		public Set<Class<?>> getFieldTypes() {
			return fieldTypes;
		}

		public Printer<?> getPrinter(SpecialIntFormat annotation, Class<?> fieldType) {
			return new Printer<Integer>() {
				public String print(Integer object, Locale locale) {
					return ">>" + object.toString() + "<<";
				}
			};
		}

		public Parser<?> getParser(SpecialIntFormat annotation, Class<?> fieldType) {
			return new Parser<Integer>() {
				public Integer parse(String text, Locale locale) throws ParseException {
					if (!text.startsWith(">>") || !text.endsWith("<<") || (text.length() < 5)) {
						throw new ParseException(text + " is not in the expected format '>>intValue<<'", 0);
					}
					return Integer.parseInt(text.substring(2,3));
				}
			};
		}

	}

	private static class TestBean {

		private int field;

		@SpecialIntFormat
		private int anotherField;

		public int getField() {
			return field;
		}

		public void setField(int field) {
			this.field = field;
		}

		@SuppressWarnings("unused")
		public int getAnotherField() {
			return anotherField;
		}

		@SuppressWarnings("unused")
		public void setAnotherField(int anotherField) {
			this.anotherField = anotherField;
		}

	}

	@Target({ ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
	@Retention(RetentionPolicy.RUNTIME)
	private @interface SpecialIntFormat {
	}

}
