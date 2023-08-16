/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.annotation.Annotation;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.ValidationAnnotationUtils;

/**
 * A resolver to extract and convert the payload of a message using a
 * {@link MessageConverter}.
 *
 * <p>Validation is applied if the method argument is annotated with
 * {@link org.springframework.validation.annotation.Validated} or
 * {@code @javax.validation.Valid}. Validation failure results in an
 * {@link MethodArgumentNotValidException}.
 *
 * <p>This {@link HandlerMethodArgumentResolver} should be ordered last as it
 * supports all types and does not require the {@link Payload} annotation.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 5.2
 */
public class PayloadMethodArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;

	@Nullable
	private final Validator validator;

	private final boolean useDefaultResolution;


	/**
	 * Create a new {@code PayloadArgumentResolver} with the given
	 * {@link MessageConverter}.
	 * @param messageConverter the MessageConverter to use (required)
	 */
	public PayloadMethodArgumentResolver(MessageConverter messageConverter) {
		this(messageConverter, null);
	}

	/**
	 * Create a new {@code PayloadArgumentResolver} with the given
	 * {@link MessageConverter} and {@link Validator}.
	 * @param messageConverter the MessageConverter to use (required)
	 * @param validator the Validator to use (optional)
	 */
	public PayloadMethodArgumentResolver(MessageConverter messageConverter, @Nullable Validator validator) {
		this(messageConverter, validator, true);
	}

	/**
	 * Create a new {@code PayloadArgumentResolver} with the given
	 * {@link MessageConverter} and {@link Validator}.
	 * @param messageConverter the MessageConverter to use (required)
	 * @param validator the Validator to use (optional)
	 * @param useDefaultResolution if "true" (the default) this resolver supports
	 * all parameters; if "false" then only arguments with the {@code @Payload}
	 * annotation are supported.
	 */
	public PayloadMethodArgumentResolver(MessageConverter messageConverter, @Nullable Validator validator,
			boolean useDefaultResolution) {

		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.converter = messageConverter;
		this.validator = validator;
		this.useDefaultResolution = useDefaultResolution;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(Payload.class) || this.useDefaultResolution);
	}

	@Override
	@Nullable
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		Payload ann = parameter.getParameterAnnotation(Payload.class);
		if (ann != null && StringUtils.hasText(ann.expression())) {
			throw new IllegalStateException("@Payload SpEL expressions not supported by this resolver");
		}

		Object payload = message.getPayload();
		if (isEmptyPayload(payload)) {
			if (ann == null || ann.required()) {
				String paramName = getParameterName(parameter);
				BindingResult bindingResult = new BeanPropertyBindingResult(payload, paramName);
				bindingResult.addError(new ObjectError(paramName, "Payload value must not be empty"));
				throw new MethodArgumentNotValidException(message, parameter, bindingResult);
			}
			else {
				return null;
			}
		}

		Class<?> targetClass = resolveTargetClass(parameter, message);
		Class<?> payloadClass = payload.getClass();
		if (ClassUtils.isAssignable(targetClass, payloadClass)) {
			validate(message, parameter, payload);
			return payload;
		}
		else {
			if (this.converter instanceof SmartMessageConverter) {
				SmartMessageConverter smartConverter = (SmartMessageConverter) this.converter;
				payload = smartConverter.fromMessage(message, targetClass, parameter);
			}
			else {
				payload = this.converter.fromMessage(message, targetClass);
			}
			if (payload == null) {
				throw new MessageConversionException(message, "Cannot convert from [" +
						payloadClass.getName() + "] to [" + targetClass.getName() + "] for " + message);
			}
			validate(message, parameter, payload);
			return payload;
		}
	}

	private String getParameterName(MethodParameter param) {
		String paramName = param.getParameterName();
		return (paramName != null ? paramName : "Arg " + param.getParameterIndex());
	}

	/**
	 * Specify if the given {@code payload} is empty.
	 * @param payload the payload to check (can be {@code null})
	 */
	protected boolean isEmptyPayload(@Nullable Object payload) {
		if (payload == null) {
			return true;
		}
		else if (payload instanceof byte[]) {
			return ((byte[]) payload).length == 0;
		}
		else if (payload instanceof String) {
			return !StringUtils.hasText((String) payload);
		}
		else {
			return false;
		}
	}

	/**
	 * Resolve the target class to convert the payload to.
	 * <p>By default this is simply {@link MethodParameter#getParameterType()}
	 * but that can be overridden to select a more specific target type after
	 * also taking into account the "Content-Type", e.g. return {@code String}
	 * if target type is {@code Object} and {@code "Content-Type:text/**"}.
	 * @param parameter the target method parameter
	 * @param message the message being processed
	 * @return the target type to use
	 * @since 5.2
	 */
	protected Class<?> resolveTargetClass(MethodParameter parameter, Message<?> message) {
		return parameter.getParameterType();
	}

	/**
	 * Validate the payload if applicable.
	 * @param message the currently processed message
	 * @param parameter the method parameter
	 * @param target the target payload object
	 * @throws MethodArgumentNotValidException in case of binding errors
	 */
	protected void validate(Message<?> message, MethodParameter parameter, Object target) {
		if (this.validator == null) {
			return;
		}
		for (Annotation ann : parameter.getParameterAnnotations()) {
			Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
			if (validationHints != null) {
				BeanPropertyBindingResult bindingResult =
						new BeanPropertyBindingResult(target, getParameterName(parameter));
				if (!ObjectUtils.isEmpty(validationHints) && this.validator instanceof SmartValidator) {
					((SmartValidator) this.validator).validate(target, bindingResult, validationHints);
				}
				else {
					this.validator.validate(target, bindingResult);
				}
				if (bindingResult.hasErrors()) {
					throw new MethodArgumentNotValidException(message, parameter, bindingResult);
				}
				break;
			}
		}
	}

}
