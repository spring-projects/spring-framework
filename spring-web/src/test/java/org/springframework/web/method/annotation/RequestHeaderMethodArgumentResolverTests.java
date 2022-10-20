/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link RequestHeaderMethodArgumentResolver}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class RequestHeaderMethodArgumentResolverTests {

	private RequestHeaderMethodArgumentResolver resolver;

	private MethodParameter paramNamedDefaultValueStringHeader;
	private MethodParameter paramNamedValueStringArray;
	private MethodParameter paramSystemProperty;
	private MethodParameter paramContextPath;
	private MethodParameter paramResolvedNameWithExpression;
	private MethodParameter paramResolvedNameWithPlaceholder;
	private MethodParameter paramNamedValueMap;
	private MethodParameter paramDate;
	private MethodParameter paramInstant;
	private MethodParameter paramUuid;
	private MethodParameter paramUuidOptional;

	private MockHttpServletRequest servletRequest;

	private NativeWebRequest webRequest;


	@BeforeEach
	@SuppressWarnings("resource")
	void setup() throws Exception {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.refresh();
		resolver = new RequestHeaderMethodArgumentResolver(context.getBeanFactory());

		Method method = ReflectionUtils.findMethod(getClass(), "params", (Class<?>[]) null);
		paramNamedDefaultValueStringHeader = new SynthesizingMethodParameter(method, 0);
		paramNamedValueStringArray = new SynthesizingMethodParameter(method, 1);
		paramSystemProperty = new SynthesizingMethodParameter(method, 2);
		paramContextPath = new SynthesizingMethodParameter(method, 3);
		paramResolvedNameWithExpression = new SynthesizingMethodParameter(method, 4);
		paramResolvedNameWithPlaceholder = new SynthesizingMethodParameter(method, 5);
		paramNamedValueMap = new SynthesizingMethodParameter(method, 6);
		paramDate = new SynthesizingMethodParameter(method, 7);
		paramInstant = new SynthesizingMethodParameter(method, 8);
		paramUuid = new SynthesizingMethodParameter(method, 9);
		paramUuidOptional = new SynthesizingMethodParameter(method, 10);

		servletRequest = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(servletRequest, new MockHttpServletResponse());

		// Expose request to the current thread (for SpEL expressions)
		RequestContextHolder.setRequestAttributes(webRequest);
	}

	@AfterEach
	void reset() {
		RequestContextHolder.resetRequestAttributes();
	}


	@Test
	void supportsParameter() {
		assertThat(resolver.supportsParameter(paramNamedDefaultValueStringHeader)).as("String parameter not supported").isTrue();
		assertThat(resolver.supportsParameter(paramNamedValueStringArray)).as("String array parameter not supported").isTrue();
		assertThat(resolver.supportsParameter(paramNamedValueMap)).as("non-@RequestParam parameter supported").isFalse();
	}

	@Test
	void resolveStringArgument() throws Exception {
		String expected = "foo";
		servletRequest.addHeader("name", expected);

		Object result = resolver.resolveArgument(paramNamedDefaultValueStringHeader, null, webRequest, null);

		assertThat(result).isEqualTo(expected);
	}

	@Test
	void resolveStringArrayArgument() throws Exception {
		String[] expected = new String[] {"foo", "bar"};
		servletRequest.addHeader("name", expected);

		Object result = resolver.resolveArgument(paramNamedValueStringArray, null, webRequest, null);
		assertThat(result).isInstanceOf(String[].class);
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void resolveDefaultValue() throws Exception {
		Object result = resolver.resolveArgument(paramNamedDefaultValueStringHeader, null, webRequest, null);

		assertThat(result).isEqualTo("bar");
	}

	@Test
	void resolveDefaultValueFromSystemProperty() throws Exception {
		System.setProperty("systemProperty", "bar");
		try {
			Object result = resolver.resolveArgument(paramSystemProperty, null, webRequest, null);

			assertThat(result).isEqualTo("bar");
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	void resolveNameFromSystemPropertyThroughExpression() throws Exception {
		String expected = "foo";
		servletRequest.addHeader("bar", expected);

		System.setProperty("systemProperty", "bar");
		try {
			Object result = resolver.resolveArgument(paramResolvedNameWithExpression, null, webRequest, null);

			assertThat(result).isEqualTo(expected);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	void resolveNameFromSystemPropertyThroughPlaceholder() throws Exception {
		String expected = "foo";
		servletRequest.addHeader("bar", expected);

		System.setProperty("systemProperty", "bar");
		try {
			Object result = resolver.resolveArgument(paramResolvedNameWithPlaceholder, null, webRequest, null);

			assertThat(result).isEqualTo(expected);
		}
		finally {
			System.clearProperty("systemProperty");
		}
	}

	@Test
	void resolveDefaultValueFromRequest() throws Exception {
		servletRequest.setContextPath("/bar");

		Object result = resolver.resolveArgument(paramContextPath, null, webRequest, null);

		assertThat(result).isEqualTo("/bar");
	}

	@Test
	void notFound() throws Exception {
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				resolver.resolveArgument(paramNamedValueStringArray, null, webRequest, null));
	}

	@Test
	@SuppressWarnings("deprecation")
	void dateConversion() throws Exception {
		String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
		servletRequest.addHeader("name", rfc1123val);

		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setConversionService(new DefaultFormattingConversionService());
		Object result = resolver.resolveArgument(paramDate, null, webRequest,
				new DefaultDataBinderFactory(bindingInitializer));

		assertThat(result).isEqualTo(new Date(rfc1123val));
	}

	@Test
	void instantConversion() throws Exception {
		String rfc1123val = "Thu, 21 Apr 2016 17:11:08 +0100";
		servletRequest.addHeader("name", rfc1123val);

		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setConversionService(new DefaultFormattingConversionService());
		Object result = resolver.resolveArgument(paramInstant, null, webRequest,
				new DefaultDataBinderFactory(bindingInitializer));

		assertThat(result).isEqualTo(Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(rfc1123val)));
	}

	@Test
	void uuidConversionWithValidValue() throws Exception {
		UUID uuid = UUID.randomUUID();
		servletRequest.addHeader("name", uuid.toString());

		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setConversionService(new DefaultFormattingConversionService());
		Object result = resolver.resolveArgument(paramUuid, null, webRequest,
				new DefaultDataBinderFactory(bindingInitializer));

		assertThat(result).isEqualTo(uuid);
	}

	@Test
	void uuidConversionWithInvalidValue() throws Exception {
		servletRequest.addHeader("name", "bogus-uuid");

		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setConversionService(new DefaultFormattingConversionService());

		assertThatExceptionOfType(MethodArgumentTypeMismatchException.class).isThrownBy(
				() -> resolver.resolveArgument(paramUuid, null, webRequest,
						new DefaultDataBinderFactory(bindingInitializer)));
	}

	@Test
	void uuidConversionWithEmptyValue() throws Exception {
		uuidConversionWithEmptyOrBlankValue("");
	}

	@Test
	void uuidConversionWithBlankValue() throws Exception {
		uuidConversionWithEmptyOrBlankValue("     ");
	}

	private void uuidConversionWithEmptyOrBlankValue(String uuid) throws Exception {
		servletRequest.addHeader("name", uuid);

		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setConversionService(new DefaultFormattingConversionService());

		assertThatExceptionOfType(MissingRequestHeaderException.class).isThrownBy(() ->
				resolver.resolveArgument(paramUuid, null, webRequest,
						new DefaultDataBinderFactory(bindingInitializer)));
	}

	@Test
	void uuidConversionWithEmptyValueOptional() throws Exception {
		uuidConversionWithEmptyOrBlankValueOptional("");
	}

	@Test
	void uuidConversionWithBlankValueOptional() throws Exception {
		uuidConversionWithEmptyOrBlankValueOptional("     ");
	}

	private void uuidConversionWithEmptyOrBlankValueOptional(String uuid) throws Exception {
		servletRequest.addHeader("name", uuid);

		ConfigurableWebBindingInitializer bindingInitializer = new ConfigurableWebBindingInitializer();
		bindingInitializer.setConversionService(new DefaultFormattingConversionService());
		Object result = resolver.resolveArgument(paramUuidOptional, null, webRequest,
				new DefaultDataBinderFactory(bindingInitializer));

		assertThat(result).isNull();
	}


	void params(
			@RequestHeader(name = "name", defaultValue = "bar") String param1,
			@RequestHeader("name") String[] param2,
			@RequestHeader(name = "name", defaultValue="#{systemProperties.systemProperty}") String param3,
			@RequestHeader(name = "name", defaultValue="#{request.contextPath}") String param4,
			@RequestHeader("#{systemProperties.systemProperty}") String param5,
			@RequestHeader("${systemProperty}") String param6,
			@RequestHeader("name") Map<?, ?> unsupported,
			@RequestHeader("name") Date dateParam,
			@RequestHeader("name") Instant instantParam,
			@RequestHeader("name") UUID uuid,
			@RequestHeader(name = "name", required = false) UUID uuidOptional) {
	}

}
