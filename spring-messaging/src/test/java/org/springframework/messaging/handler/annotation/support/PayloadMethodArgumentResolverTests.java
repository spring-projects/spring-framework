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

package org.springframework.messaging.handler.annotation.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture for {@link PayloadMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
public class PayloadMethodArgumentResolverTests {

	private PayloadMethodArgumentResolver resolver;

	private MethodParameter paramAnnotated;

	private MethodParameter paramAnnotatedNotRequired;

	private MethodParameter paramAnnotatedRequired;

	private MethodParameter paramWithSpelExpression;

	private MethodParameter paramNotAnnotated;

	private MethodParameter paramValidatedNotAnnotated;

	private MethodParameter paramValidated;


	@BeforeEach
	public void setup() throws Exception {
		this.resolver = new PayloadMethodArgumentResolver(new StringMessageConverter(), testValidator());

		Method payloadMethod = PayloadMethodArgumentResolverTests.class.getDeclaredMethod(
				"handleMessage", String.class, String.class, Locale.class,
				String.class, String.class, String.class, String.class);

		this.paramAnnotated = new SynthesizingMethodParameter(payloadMethod, 0);
		this.paramAnnotatedNotRequired = new SynthesizingMethodParameter(payloadMethod, 1);
		this.paramAnnotatedRequired = new SynthesizingMethodParameter(payloadMethod, 2);
		this.paramWithSpelExpression = new SynthesizingMethodParameter(payloadMethod, 3);
		this.paramValidated = new SynthesizingMethodParameter(payloadMethod, 4);
		this.paramValidated.initParameterNameDiscovery(new LocalVariableTableParameterNameDiscoverer());
		this.paramValidatedNotAnnotated = new SynthesizingMethodParameter(payloadMethod, 5);
		this.paramNotAnnotated = new SynthesizingMethodParameter(payloadMethod, 6);
	}

	@Test
	public void supportsParameter() {
		assertThat(this.resolver.supportsParameter(this.paramAnnotated)).isTrue();
		assertThat(this.resolver.supportsParameter(this.paramNotAnnotated)).isTrue();

		PayloadMethodArgumentResolver strictResolver = new PayloadMethodArgumentResolver(
				new StringMessageConverter(), testValidator(), false);

		assertThat(strictResolver.supportsParameter(this.paramAnnotated)).isTrue();
		assertThat(strictResolver.supportsParameter(this.paramNotAnnotated)).isFalse();
	}

	@Test
	public void resolveRequired() throws Exception {
		Message<?> message = MessageBuilder.withPayload("ABC".getBytes()).build();
		Object actual = this.resolver.resolveArgument(paramAnnotated, message);

		assertThat(actual).isEqualTo("ABC");
	}

	@Test
	public void resolveRequiredEmpty() throws Exception {
		Message<?> message = MessageBuilder.withPayload("").build();
		 // required but empty
		assertThatExceptionOfType(MethodArgumentNotValidException.class).isThrownBy(() ->
		this.resolver.resolveArgument(paramAnnotated, message));
	}

	@Test
	public void resolveRequiredEmptyNonAnnotatedParameter() throws Exception {
		Message<?> message = MessageBuilder.withPayload("").build();
		// required but empty
		assertThatExceptionOfType(MethodArgumentNotValidException.class).isThrownBy(() ->
				this.resolver.resolveArgument(this.paramNotAnnotated, message));
	}

	@Test
	public void resolveNotRequired() throws Exception {
		Message<?> emptyByteArrayMessage = MessageBuilder.withPayload(new byte[0]).build();
		assertThat(this.resolver.resolveArgument(this.paramAnnotatedNotRequired, emptyByteArrayMessage)).isNull();

		Message<?> emptyStringMessage = MessageBuilder.withPayload("").build();
		assertThat(this.resolver.resolveArgument(this.paramAnnotatedNotRequired, emptyStringMessage)).isNull();

		Message<?> notEmptyMessage = MessageBuilder.withPayload("ABC".getBytes()).build();
		assertThat(this.resolver.resolveArgument(this.paramAnnotatedNotRequired, notEmptyMessage)).isEqualTo("ABC");
	}

	@Test
	public void resolveNonConvertibleParam() throws Exception {
		Message<?> notEmptyMessage = MessageBuilder.withPayload(123).build();

		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				this.resolver.resolveArgument(this.paramAnnotatedRequired, notEmptyMessage))
			.withMessageContaining("Cannot convert");
	}

	@Test
	public void resolveSpelExpressionNotSupported() throws Exception {
		Message<?> message = MessageBuilder.withPayload("ABC".getBytes()).build();

		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.resolveArgument(paramWithSpelExpression, message));
	}

	@Test
	public void resolveValidation() throws Exception {
		Message<?> message = MessageBuilder.withPayload("ABC".getBytes()).build();
		this.resolver.resolveArgument(this.paramValidated, message);
	}

	@Test
	public void resolveFailValidation() throws Exception {
		// See testValidator()
		Message<?> message = MessageBuilder.withPayload("invalidValue".getBytes()).build();

		assertThatExceptionOfType(MethodArgumentNotValidException.class).isThrownBy(() ->
				this.resolver.resolveArgument(this.paramValidated, message));
	}

	@Test
	public void resolveFailValidationNoConversionNecessary() throws Exception {
		Message<?> message = MessageBuilder.withPayload("invalidValue").build();

		assertThatExceptionOfType(MethodArgumentNotValidException.class).isThrownBy(() ->
				this.resolver.resolveArgument(this.paramValidated, message));
	}

	@Test
	public void resolveNonAnnotatedParameter() throws Exception {
		Message<?> notEmptyMessage = MessageBuilder.withPayload("ABC".getBytes()).build();
		assertThat(this.resolver.resolveArgument(this.paramNotAnnotated, notEmptyMessage)).isEqualTo("ABC");

		Message<?> emptyStringMessage = MessageBuilder.withPayload("").build();
		assertThatExceptionOfType(MethodArgumentNotValidException.class).isThrownBy(() ->
				this.resolver.resolveArgument(this.paramValidated, emptyStringMessage));
	}

	@Test
	public void resolveNonAnnotatedParameterFailValidation() throws Exception {
		// See testValidator()
		Message<?> message = MessageBuilder.withPayload("invalidValue".getBytes()).build();

		assertThatExceptionOfType(MethodArgumentNotValidException.class).isThrownBy(() -> assertThat(this.resolver.resolveArgument(this.paramValidatedNotAnnotated, message)).isEqualTo("invalidValue"))
			.withMessageContaining("invalid value");
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
				if ("invalidValue".equals(value)) {
					errors.reject("invalid value");
				}
			}
		};
	}


	@SuppressWarnings("unused")
	private void handleMessage(
			@Payload String param,
			@Payload(required=false) String paramNotRequired,
			@Payload(required=true) Locale nonConvertibleRequiredParam,
			@Payload("foo.bar") String paramWithSpelExpression,
			@MyValid @Payload String validParam,
			@Validated String validParamNotAnnotated,
			String paramNotAnnotated) {
	}


	@Validated
	@Target({ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MyValid {
	}

}
