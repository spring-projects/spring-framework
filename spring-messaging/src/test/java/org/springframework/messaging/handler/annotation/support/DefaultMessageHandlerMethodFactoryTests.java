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

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Stephane Nicoll
 */
class DefaultMessageHandlerMethodFactoryTests {

	private final SampleBean sample = new SampleBean();


	@Test
	void customConversion() throws Exception {
		DefaultMessageHandlerMethodFactory instance = createInstance();
		GenericConversionService conversionService = new GenericConversionService();
		conversionService.addConverter(SampleBean.class, String.class, source -> "foo bar");
		instance.setConversionService(conversionService);
		instance.afterPropertiesSet();

		InvocableHandlerMethod invocableHandlerMethod =
				createInvocableHandlerMethod(instance, "simpleString", String.class);

		invocableHandlerMethod.invoke(MessageBuilder.withPayload(sample).build());
		assertMethodInvocation(sample, "simpleString");
	}

	@Test
	void customConversionServiceFailure() {
		DefaultMessageHandlerMethodFactory instance = createInstance();
		GenericConversionService conversionService = new GenericConversionService();
		assertThat(conversionService.canConvert(Integer.class, String.class)).as("conversion service should fail to convert payload").isFalse();
		instance.setConversionService(conversionService);
		instance.afterPropertiesSet();

		InvocableHandlerMethod invocableHandlerMethod =
				createInvocableHandlerMethod(instance, "simpleString", String.class);

		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				invocableHandlerMethod.invoke(MessageBuilder.withPayload(123).build()));
	}

	@Test
	void customMessageConverterFailure() {
		DefaultMessageHandlerMethodFactory instance = createInstance();
		MessageConverter messageConverter = new ByteArrayMessageConverter();
		instance.setMessageConverter(messageConverter);
		instance.afterPropertiesSet();

		InvocableHandlerMethod invocableHandlerMethod =
				createInvocableHandlerMethod(instance, "simpleString", String.class);

		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				invocableHandlerMethod.invoke(MessageBuilder.withPayload(123).build()));
	}

	@Test
	void customArgumentResolver() throws Exception {
		DefaultMessageHandlerMethodFactory instance = createInstance();
		List<HandlerMethodArgumentResolver> customResolvers = new ArrayList<>();
		customResolvers.add(new CustomHandlerMethodArgumentResolver());
		instance.setCustomArgumentResolvers(customResolvers);
		instance.afterPropertiesSet();

		InvocableHandlerMethod invocableHandlerMethod =
				createInvocableHandlerMethod(instance, "customArgumentResolver", Locale.class);

		invocableHandlerMethod.invoke(MessageBuilder.withPayload(123).build());
		assertMethodInvocation(sample, "customArgumentResolver");
	}

	@Test
	void overrideArgumentResolvers() throws Exception {
		DefaultMessageHandlerMethodFactory instance = createInstance();
		List<HandlerMethodArgumentResolver> customResolvers = new ArrayList<>();
		customResolvers.add(new CustomHandlerMethodArgumentResolver());
		instance.setArgumentResolvers(customResolvers);
		instance.afterPropertiesSet();

		Message<String> message = MessageBuilder.withPayload("sample").build();

		// This will work as the local resolver is set
		InvocableHandlerMethod invocableHandlerMethod =
				createInvocableHandlerMethod(instance, "customArgumentResolver", Locale.class);
		invocableHandlerMethod.invoke(message);
		assertMethodInvocation(sample, "customArgumentResolver");

		// This won't work as no resolver is known for the payload
		InvocableHandlerMethod invocableHandlerMethod2 =
				createInvocableHandlerMethod(instance, "simpleString", String.class);

		assertThatExceptionOfType(MethodArgumentResolutionException.class).isThrownBy(() ->
				invocableHandlerMethod2.invoke(message)).withMessageContaining("No suitable resolver");
	}

	@Test
	void noValidationByDefault() throws Exception {
		DefaultMessageHandlerMethodFactory instance = createInstance();
		instance.afterPropertiesSet();

		InvocableHandlerMethod invocableHandlerMethod =
				createInvocableHandlerMethod(instance, "payloadValidation", String.class);
		invocableHandlerMethod.invoke(MessageBuilder.withPayload("failure").build());
		assertMethodInvocation(sample, "payloadValidation");
	}

	@Test
	void customValidation() {
		DefaultMessageHandlerMethodFactory instance = createInstance();
		instance.setValidator(new Validator() {
			@Override
			public boolean supports(Class<?> clazz) {
				return String.class.isAssignableFrom(clazz);
			}
			@Override
			public void validate(Object target, Errors errors) {
				String value = (String) target;
				if ("failure".equals(value)) {
					errors.reject("not a valid value");
				}
			}
		});
		instance.afterPropertiesSet();

		InvocableHandlerMethod invocableHandlerMethod =
				createInvocableHandlerMethod(instance, "payloadValidation", String.class);
		assertThatExceptionOfType(MethodArgumentNotValidException.class).isThrownBy(() ->
				invocableHandlerMethod.invoke(MessageBuilder.withPayload("failure").build()));
	}


	private void assertMethodInvocation(SampleBean bean, String methodName) {
		assertThat((boolean) bean.invocations.get(methodName)).as("Method " + methodName + " should have been invoked").isTrue();
	}

	private InvocableHandlerMethod createInvocableHandlerMethod(
			DefaultMessageHandlerMethodFactory factory, String methodName, Class<?>... parameterTypes) {
		return factory.createInvocableHandlerMethod(sample, getListenerMethod(methodName, parameterTypes));
	}

	private DefaultMessageHandlerMethodFactory createInstance() {
		DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
		factory.setBeanFactory(new StaticListableBeanFactory());
		return factory;
	}

	private Method getListenerMethod(String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(SampleBean.class, methodName, parameterTypes);
		assertThat(("no method found with name " + methodName + " and parameters " + Arrays.toString(parameterTypes))).isNotNull();
		return method;
	}


	static class SampleBean {

		private final Map<String, Boolean> invocations = new HashMap<>();

		public void simpleString(String value) {
			invocations.put("simpleString", true);
		}

		public void payloadValidation(@Payload @Validated String value) {
			invocations.put("payloadValidation", true);
		}

		public void customArgumentResolver(Locale locale) {
			invocations.put("customArgumentResolver", true);
			assertThat(locale).as("Wrong value for locale").isEqualTo(Locale.getDefault());
		}
	}


	static class CustomHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return parameter.getParameterType().isAssignableFrom(Locale.class);
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
			return Locale.getDefault();
		}
	}

}
