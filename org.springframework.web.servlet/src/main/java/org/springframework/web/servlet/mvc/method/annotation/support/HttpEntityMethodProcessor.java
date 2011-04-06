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

package org.springframework.web.servlet.mvc.method.annotation.support;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Implementation of {@link HandlerMethodArgumentResolver} and {@link HandlerMethodReturnValueHandler} 
 * that supports {@link HttpEntity} and {@link ResponseEntity}.
 * 
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class HttpEntityMethodProcessor extends AbstractMessageConverterMethodProcessor {

	public HttpEntityMethodProcessor(HttpMessageConverter<?>... messageConverters) {
		super(messageConverters);
	}

	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> parameterType = parameter.getParameterType();
		return HttpEntity.class.equals(parameterType);
	}

	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> parameterType = returnType.getParameterType();
		return HttpEntity.class.equals(parameterType) || ResponseEntity.class.equals(parameterType);
	}

	public boolean usesResponseArgument(MethodParameter parameterOrReturnType) {
		// only when HttpEntity or ResponseEntity is used as a return type
		return parameterOrReturnType.getParameterIndex() == -1;
	}

	public Object resolveArgument(MethodParameter parameter,
								  ModelMap model,
								  NativeWebRequest webRequest, 
								  WebDataBinderFactory binderFactory) 
			throws IOException, HttpMediaTypeNotSupportedException {
		Class<?> paramType = getHttpEntityType(parameter);
		Object body = readWithMessageConverters(webRequest, parameter, paramType);
		HttpInputMessage inputMessage = createInputMessage(webRequest);
		return new HttpEntity<Object>(body, inputMessage.getHeaders());
	}
	
	@Override
	protected HttpInputMessage createInputMessage(NativeWebRequest webRequest) {
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		return new ServletServerHttpRequest(servletRequest);
	}

	private Class<?> getHttpEntityType(MethodParameter methodParam) {
		Assert.isAssignable(HttpEntity.class, methodParam.getParameterType());
		ParameterizedType type = (ParameterizedType) methodParam.getGenericParameterType();
		if (type.getActualTypeArguments().length == 1) {
			Type typeArgument = type.getActualTypeArguments()[0];
			if (typeArgument instanceof Class) {
				return (Class<?>) typeArgument;
			}
			else if (typeArgument instanceof GenericArrayType) {
				Type componentType = ((GenericArrayType) typeArgument).getGenericComponentType();
				if (componentType instanceof Class) {
					// Surely, there should be a nicer way to do this
					Object array = Array.newInstance((Class<?>) componentType, 0);
					return array.getClass();
				}
			}
		}
		throw new IllegalArgumentException(
				"HttpEntity parameter (" + methodParam.getParameterName() + ") is not parameterized");

	}

	public void handleReturnValue(Object returnValue, 
								  MethodParameter returnType, 
								  ModelAndViewContainer mavContainer, 
								  NativeWebRequest webRequest) throws Exception {
		if (returnValue == null) {
			return;
		}
		Assert.isInstanceOf(HttpEntity.class, returnValue);
		HttpEntity<?> responseEntity = (HttpEntity<?>) returnValue;
		HttpOutputMessage outputMessage = createOutputMessage(webRequest);
		if (responseEntity instanceof ResponseEntity) {
			((ServerHttpResponse) outputMessage).setStatusCode(((ResponseEntity<?>) responseEntity).getStatusCode());
		}
		HttpHeaders entityHeaders = responseEntity.getHeaders();
		if (!entityHeaders.isEmpty()) {
			outputMessage.getHeaders().putAll(entityHeaders);
		}
		Object body = responseEntity.getBody();
		if (body != null) {
			writeWithMessageConverters(body, createInputMessage(webRequest), outputMessage);
		}
		else {
			// flush headers to the HttpServletResponse
			outputMessage.getBody();
		}
	}

	@Override
	protected HttpOutputMessage createOutputMessage(NativeWebRequest webRequest) {
		HttpServletResponse servletResponse = (HttpServletResponse) webRequest.getNativeResponse();
		return new ServletServerHttpResponse(servletResponse);
	}
}
