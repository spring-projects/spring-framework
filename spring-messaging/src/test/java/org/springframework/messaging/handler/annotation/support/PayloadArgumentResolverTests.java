/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link PayloadArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
public class PayloadArgumentResolverTests {

	private PayloadArgumentResolver resolver;

	private MethodParameter param;
	private MethodParameter paramNotRequired;
	private MethodParameter paramWithSpelExpression;
	private MethodParameter paramValidated;


	@Before
	public void setup() throws Exception {

		this.resolver = new PayloadArgumentResolver(new StringMessageConverter(), testValidator());

		Method method = PayloadArgumentResolverTests.class.getDeclaredMethod("handleMessage",
				String.class, String.class, String.class, String.class);

		this.param = new MethodParameter(method , 0);
		this.paramNotRequired = new MethodParameter(method , 1);
		this.paramWithSpelExpression = new MethodParameter(method , 2);
		this.paramValidated = new MethodParameter(method , 3);
		this.paramValidated.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
	}


	@Test
	public void resolveRequired() throws Exception {
		Message<?> message = MessageBuilder.withPayload("ABC".getBytes()).build();
		Object actual = this.resolver.resolveArgument(this.param, message);

		assertEquals("ABC", actual);
	}

	@Test
	public void resolveNotRequired() throws Exception {

		Message<?> emptyByteArrayMessage = MessageBuilder.withPayload(new byte[0]).build();
		assertNull(this.resolver.resolveArgument(this.paramNotRequired, emptyByteArrayMessage));

		Message<?> notEmptyMessage = MessageBuilder.withPayload("ABC".getBytes()).build();
		assertEquals("ABC", this.resolver.resolveArgument(this.paramNotRequired, notEmptyMessage));
	}

	@Test(expected=IllegalStateException.class)
	public void resolveSpelExpressionNotSupported() throws Exception {
		Message<?> message = MessageBuilder.withPayload("ABC".getBytes()).build();
		this.resolver.resolveArgument(this.paramWithSpelExpression, message);
	}

	@Test
	public void resolveValidation() throws Exception {
		Message<?> message = MessageBuilder.withPayload("ABC".getBytes()).build();
		this.resolver.resolveArgument(this.paramValidated, message);
	}

	@Test(expected=MethodArgumentNotValidException.class)
	public void resolveFailValidation() throws Exception {
		Message<?> message = MessageBuilder.withPayload("".getBytes()).build();
		this.resolver.resolveArgument(this.paramValidated, message);
	}

	private Validator testValidator() {

		return new Validator() {
			@Override
			public boolean supports(Class<?> clazz) {
				return String.class.isAssignableFrom(clazz);
			}
			@Override
			public void validate(Object target, Errors errors) {
				String value = (String) target;
				if (StringUtils.isEmpty(value.toString())) {
					errors.reject("empty value");
				}
			}
		};
	}

	@SuppressWarnings("unused")
	private void handleMessage(
			@Payload String param,
			@Payload(required=false) String paramNotRequired,
			@Payload("foo.bar") String paramWithSpelExpression,
			@Validated @Payload String validParam) {
	}

}
