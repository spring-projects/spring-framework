/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.handler.annotation.support;

import java.lang.annotation.Annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
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

/**
 * A resolver to extract and convert the payload of a message using a
 * {@link MessageConverter}. It also validates the payload using a
 * {@link Validator} if the argument is annotated with a Validation annotation.
 *
 * <p>This {@link HandlerMethodArgumentResolver} should be ordered last as it
 * supports all types and does not require the {@link Payload} annotation.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0
 */
public class PayloadArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;

	private final Validator validator;


	/**
	 * Create a new {@code PayloadArgumentResolver} with the given
	 * {@link MessageConverter}.
	 * @param messageConverter the MessageConverter to use (required)
	 * @since 4.0.9
	 */
	public PayloadArgumentResolver(MessageConverter messageConverter) {
		this(messageConverter, null);
	}

	/**
	 * Create a new {@code PayloadArgumentResolver} with the given
	 * {@link MessageConverter} and {@link Validator}.
	 * @param messageConverter the MessageConverter to use (required)
	 * @param validator the Validator to use (optional)
	 */
	public PayloadArgumentResolver(MessageConverter messageConverter, Validator validator) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.converter = messageConverter;
		this.validator = validator;
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return true;
	}

	@Override
	public Object resolveArgument(MethodParameter param, Message<?> message) throws Exception {
		Payload ann = param.getParameterAnnotation(Payload.class);
		if (ann != null && StringUtils.hasText(ann.value())) {
			throw new IllegalStateException("@Payload SpEL expressions not supported by this resolver");
		}

		Object payload = message.getPayload();
		if (isEmptyPayload(payload)) {
			if (ann == null || ann.required()) {
				String paramName = getParameterName(param);
				BindingResult bindingResult = new BeanPropertyBindingResult(payload, paramName);
				bindingResult.addError(new ObjectError(paramName, "@Payload param is required"));
				throw new MethodArgumentNotValidException(message, param, bindingResult);
			}
			else {
				return null;
			}
		}

		Class<?> targetClass = param.getParameterType();
		if (ClassUtils.isAssignable(targetClass, payload.getClass())) {
			validate(message, param, payload);
			return payload;
		}
		else {
			payload = this.converter.fromMessage(message, targetClass);
			if (payload == null) {
				throw new MessageConversionException(message,
						"No converter found to convert to " + targetClass + ", message=" + message, null);
			}
			validate(message, param, payload);
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
	protected boolean isEmptyPayload(Object payload) {
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

	protected void validate(Message<?> message, MethodParameter parameter, Object target) {
		if (this.validator == null) {
			return;
		}
		for (Annotation ann : parameter.getParameterAnnotations()) {
			if (ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = AnnotationUtils.getValue(ann);
				Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
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
