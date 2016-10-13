/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link HttpEntity} and {@link RequestEntity} method argument values
 * and also handles {@link HttpEntity} and {@link ResponseEntity} return values.
 *
 * <p>An {@link HttpEntity} return type has a specific purpose. Therefore this
 * handler should be configured ahead of handlers that support any return
 * value type annotated with {@code @ModelAttribute} or {@code @ResponseBody}
 * to ensure they don't take over.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 3.1
 */
public class HttpEntityMethodProcessor extends AbstractMessageConverterMethodProcessor {

	/**
	 * Basic constructor with converters only. Suitable for resolving
	 * {@code HttpEntity}. For handling {@code ResponseEntity} consider also
	 * providing a {@code ContentNegotiationManager}.
	 */
	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters) {
		super(converters);
	}

	/**
	 * Basic constructor with converters and {@code ContentNegotiationManager}.
	 * Suitable for resolving {@code HttpEntity} and handling {@code ResponseEntity}
	 * without {@code Request~} or {@code ResponseBodyAdvice}.
	 */
	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
			ContentNegotiationManager manager) {

		super(converters, manager);
	}

	/**
	 * Complete constructor for resolving {@code HttpEntity} method arguments.
	 * For handling {@code ResponseEntity} consider also providing a
	 * {@code ContentNegotiationManager}.
	 * @since 4.2
	 */
	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
			List<Object> requestResponseBodyAdvice) {

		super(converters, null, requestResponseBodyAdvice);
	}

	/**
	 * Complete constructor for resolving {@code HttpEntity} and handling
	 * {@code ResponseEntity}.
	 */
	public HttpEntityMethodProcessor(List<HttpMessageConverter<?>> converters,
			ContentNegotiationManager manager, List<Object> requestResponseBodyAdvice) {

		super(converters, manager, requestResponseBodyAdvice);
	}


	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (HttpEntity.class == parameter.getParameterType() ||
				RequestEntity.class == parameter.getParameterType());
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return (HttpEntity.class.isAssignableFrom(returnType.getParameterType()) &&
				!RequestEntity.class.isAssignableFrom(returnType.getParameterType()));
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory)
			throws IOException, HttpMediaTypeNotSupportedException {

		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
		Type paramType = getHttpEntityType(parameter);
		if (paramType == null) {
			throw new IllegalArgumentException("HttpEntity parameter '" + parameter.getParameterName() +
					"' in method " + parameter.getMethod() + " is not parameterized");
		}

		Object body = readWithMessageConverters(webRequest, parameter, paramType);
		if (RequestEntity.class == parameter.getParameterType()) {
			return new RequestEntity<>(body, inputMessage.getHeaders(),
					inputMessage.getMethod(), inputMessage.getURI());
		}
		else {
			return new HttpEntity<>(body, inputMessage.getHeaders());
		}
	}

	private Type getHttpEntityType(MethodParameter parameter) {
		Assert.isAssignable(HttpEntity.class, parameter.getParameterType());
		Type parameterType = parameter.getGenericParameterType();
		if (parameterType instanceof ParameterizedType) {
			ParameterizedType type = (ParameterizedType) parameterType;
			if (type.getActualTypeArguments().length != 1) {
				throw new IllegalArgumentException("Expected single generic parameter on '" +
						parameter.getParameterName() + "' in method " + parameter.getMethod());
			}
			return type.getActualTypeArguments()[0];
		}
		else if (parameterType instanceof Class) {
			return Object.class;
		}
		else {
			return null;
		}
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		mavContainer.setRequestHandled(true);
		if (returnValue == null) {
			return;
		}

		ServletServerHttpRequest inputMessage = createInputMessage(webRequest);
		ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);

		Assert.isInstanceOf(HttpEntity.class, returnValue);
		HttpEntity<?> responseEntity = (HttpEntity<?>) returnValue;

		HttpHeaders outputHeaders = outputMessage.getHeaders();
		HttpHeaders entityHeaders = responseEntity.getHeaders();
		if (outputHeaders.containsKey(HttpHeaders.VARY) && entityHeaders.containsKey(HttpHeaders.VARY)) {
			List<String> values = getVaryRequestHeadersToAdd(outputHeaders, entityHeaders);
			if (!values.isEmpty()) {
				outputHeaders.setVary(values);
			}
		}
		if (!entityHeaders.isEmpty()) {
			for (Map.Entry<String, List<String>> entry : entityHeaders.entrySet()) {
				if (!outputHeaders.containsKey(entry.getKey())) {
					outputHeaders.put(entry.getKey(), entry.getValue());
				}
			}
		}

		if (responseEntity instanceof ResponseEntity) {
			int returnStatus = ((ResponseEntity<?>) responseEntity).getStatusCodeValue();
			outputMessage.getServletResponse().setStatus(returnStatus);
			if (returnStatus == 200) {
				if (isResourceNotModified(inputMessage, outputMessage)) {
					// Ensure headers are flushed, no body should be written.
					outputMessage.flush();
					// Skip call to converters, as they may update the body.
					return;
				}
			}
		}

		// Try even with null body. ResponseBodyAdvice could get involved.
		writeWithMessageConverters(responseEntity.getBody(), returnType, inputMessage, outputMessage);

		// Ensure headers are flushed even if no body was written.
		outputMessage.flush();
	}

	private List<String> getVaryRequestHeadersToAdd(HttpHeaders responseHeaders, HttpHeaders entityHeaders) {
		if (!responseHeaders.containsKey(HttpHeaders.VARY)) {
			return entityHeaders.getVary();
		}
		List<String> entityHeadersVary = entityHeaders.getVary();
		List<String> result = new ArrayList<>(entityHeadersVary);
		for (String header : responseHeaders.get(HttpHeaders.VARY)) {
			for (String existing : StringUtils.tokenizeToStringArray(header, ",")) {
				if ("*".equals(existing)) {
					return Collections.emptyList();
				}
				for (String value : entityHeadersVary) {
					if (value.equalsIgnoreCase(existing)) {
						result.remove(value);
					}
				}
			}
		}
		return result;
	}

	private boolean isResourceNotModified(ServletServerHttpRequest inputMessage, ServletServerHttpResponse outputMessage) {
		ServletWebRequest servletWebRequest =
				new ServletWebRequest(inputMessage.getServletRequest(), outputMessage.getServletResponse());
		HttpHeaders responseHeaders = outputMessage.getHeaders();
		String etag = responseHeaders.getETag();
		long lastModifiedTimestamp = responseHeaders.getLastModified();
		if (inputMessage.getMethod() == HttpMethod.GET || inputMessage.getMethod() == HttpMethod.HEAD) {
			responseHeaders.remove(HttpHeaders.ETAG);
			responseHeaders.remove(HttpHeaders.LAST_MODIFIED);
		}

		return servletWebRequest.checkNotModified(etag, lastModifiedTimestamp);
	}

	@Override
	protected Class<?> getReturnValueType(Object returnValue, MethodParameter returnType) {
		if (returnValue != null) {
			return returnValue.getClass();
		}
		else {
			Type type = getHttpEntityType(returnType);
			type = (type != null ? type : Object.class);
			return ResolvableType.forMethodParameter(returnType, type).resolve(Object.class);
		}
	}

}
