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

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;

import java.lang.annotation.Annotation;

/**
 * A resolver to extract and convert the payload of a message using a
 * {@link MessageConverter}. It also validates the payload using a
 * {@link Validator} if the argument is annotated with a Validation annotation.
 *
 * <p>This {@link HandlerMethodArgumentResolver} should be ordered last as it supports all
 * types and does not require the {@link Payload} annotation.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 4.0
 */
public class PayloadArgumentResolver implements HandlerMethodArgumentResolver {

	private final MessageConverter converter;

	private final Validator validator;


	public PayloadArgumentResolver(MessageConverter messageConverter, Validator validator) {
		Assert.notNull(messageConverter, "converter must not be null");
		Assert.notNull(validator, "validator must not be null");
		this.converter = messageConverter;
		this.validator = validator;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return true;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		Payload annot = parameter.getParameterAnnotation(Payload.class);
		if ((annot != null) && StringUtils.hasText(annot.value())) {
			throw new IllegalStateException("@Payload SpEL expressions not supported by this resolver.");
		}

		Object target = getTargetPayload(parameter, message);
		if (annot != null && isEmptyPayload(target)) {
			if (annot.required()) {
				throw new MethodArgumentNotValidException(message, createPayloadRequiredExceptionMessage(parameter, target));
			}
			else {
				return null;
			}
		}

		if (annot != null) { // Only validate @Payload
			validate(message, parameter, target);
		}
		return target;
	}

	/**
	 * Return the target payload to handle for the specified message. Can either
	 * be the payload itself if the parameter type supports it or the converted
	 * one otherwise. While the payload of a {@link Message} cannot be null by
	 * design, this method may return a {@code null} payload if the conversion
	 * result is {@code null}.
	 */
	protected Object getTargetPayload(MethodParameter parameter, Message<?> message) {
		Class<?> sourceClass = message.getPayload().getClass();
		Class<?> targetClass = parameter.getParameterType();
		if (ClassUtils.isAssignable(targetClass,sourceClass)) {
			return message.getPayload();
		}
		return this.converter.fromMessage(message, targetClass);
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
			return ((String) payload).trim().equals("");
		}
		else {
			return false;
		}
	}

	protected void validate(Message<?> message, MethodParameter parameter, Object target) {
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation annot : annotations) {
			if (annot.annotationType().getSimpleName().startsWith("Valid")) {
				BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, parameter.getParameterName());
				Object hints = AnnotationUtils.getValue(annot);
				Object[] validationHints = hints instanceof Object[] ? (Object[]) hints : new Object[] {hints};

				if (!ObjectUtils.isEmpty(validationHints) && this.validator instanceof SmartValidator) {
					((SmartValidator) this.validator).validate(target, bindingResult, validationHints);
				}
				else if (this.validator != null) {
					this.validator.validate(target, bindingResult);
				}

				if (bindingResult.hasErrors()) {
					throw new MethodArgumentNotValidException(message, parameter, bindingResult);
				}

				break;
			}
		}
	}

	private String createPayloadRequiredExceptionMessage(MethodParameter parameter, Object payload) {
		String name = parameter.getParameterName() != null
				? parameter.getParameterName() : "arg" + parameter.getParameterIndex();
		StringBuilder sb = new StringBuilder("Payload parameter '").append(name)
				.append(" at index ").append(parameter.getParameterIndex()).append(" ");
		if (payload == null) {
			sb.append("could not be converted to '").append(parameter.getParameterType().getName())
					.append("' and is required");
		}
		else {
			sb.append("is required");
		}
		return sb.toString();
	}

}
