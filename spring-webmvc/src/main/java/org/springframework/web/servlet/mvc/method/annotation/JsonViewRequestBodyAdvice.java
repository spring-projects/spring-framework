/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonView;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.SmartHttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonInputMessage;
import org.springframework.util.Assert;

/**
 * A {@link RequestBodyAdvice} implementation that adds support for Jackson's
 * {@code @JsonView} annotation declared on a Spring MVC {@code @HttpEntity}
 * or {@code @RequestBody} method parameter.
 *
 * <p>The deserialization view specified in the annotation will be passed in to the
 * Jackson converters which will then use it to deserialize the request body with.
 *
 * <p>Note that despite {@code @JsonView} allowing for more than one class to
 * be specified, the use for a request body advice is only supported with
 * exactly one class argument. Consider the use of a composite interface.
 *
 * @author Sebastien Deleuze
 * @since 4.2
 * @see com.fasterxml.jackson.annotation.JsonView
 * @see com.fasterxml.jackson.databind.ObjectMapper#readerWithView(Class)
 */
@SuppressWarnings("removal")
public class JsonViewRequestBodyAdvice extends RequestBodyAdviceAdapter {

	@Override
	public boolean supports(MethodParameter methodParameter, Type targetType,
			Class<? extends HttpMessageConverter<?>> converterType) {

		return methodParameter.getParameterAnnotation(JsonView.class) != null &&
				(AbstractJacksonHttpMessageConverter.class.isAssignableFrom(converterType) ||
						AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType));
	}

	@Override
	public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter methodParameter,
			Type targetType, Class<? extends HttpMessageConverter<?>> selectedConverterType) throws IOException {

		if (AbstractJacksonHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
			return inputMessage;
		}

		return new MappingJacksonInputMessage(inputMessage.getBody(), inputMessage.getHeaders(), getJsonView(methodParameter));
	}

	@Override
	public @Nullable Map<String, Object> determineReadHints(MethodParameter parameter, Type targetType, Class<? extends SmartHttpMessageConverter<?>> converterType) {
		return Collections.singletonMap(JsonView.class.getName(), getJsonView(parameter));
	}

	private static Class<?> getJsonView(MethodParameter methodParameter) {
		JsonView ann = methodParameter.getParameterAnnotation(JsonView.class);
		Assert.state(ann != null, "No JsonView annotation");

		Class<?>[] classes = ann.value();
		if (classes.length != 1) {
			throw new IllegalArgumentException(
					"@JsonView only supported for request body advice with exactly 1 class argument: " + methodParameter);
		}
		return classes[0];
	}

}
