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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;

/**
 * A base class for resolving method argument values by reading from the body of a request with 
 * {@link HttpMessageConverter}s and for handling method return values by writing to the response with 
 * {@link HttpMessageConverter}s.
 *   
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class AbstractMessageConverterMethodProcessor
		implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private final List<HttpMessageConverter<?>> messageConverters;

	protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> messageConverters) {
		Assert.notNull(messageConverters, "'messageConverters' must not be null");
		this.messageConverters = messageConverters;
	}

	@SuppressWarnings("unchecked")
	protected <T> Object readWithMessageConverters(NativeWebRequest webRequest,
												   MethodParameter methodParam,
												   Class<T> paramType) 
			throws IOException, HttpMediaTypeNotSupportedException {
		
		HttpInputMessage inputMessage = createInputMessage(webRequest);

		MediaType contentType = inputMessage.getHeaders().getContentType();
		if (contentType == null) {
			StringBuilder builder = new StringBuilder(ClassUtils.getShortName(methodParam.getParameterType()));
			String paramName = methodParam.getParameterName();
			if (paramName != null) {
				builder.append(' ');
				builder.append(paramName);
			}
			throw new HttpMediaTypeNotSupportedException("Cannot read parameter (" + builder.toString() +
					") using HttpMessageConverters: no Content-Type found in HTTP request");
		}

		List<MediaType> allSupportedMediaTypes = new ArrayList<MediaType>();
		if (this.messageConverters != null) {
			for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
				allSupportedMediaTypes.addAll(messageConverter.getSupportedMediaTypes());
				if (messageConverter.canRead(paramType, contentType)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Reading [" + paramType.getName() + "] as \"" + contentType + "\" using [" +
								messageConverter + "]");
					}
					return ((HttpMessageConverter<T>) messageConverter).read(paramType, inputMessage);
				}
			}
		}
		
		throw new HttpMediaTypeNotSupportedException(contentType, allSupportedMediaTypes);
	}

	protected abstract HttpInputMessage createInputMessage(NativeWebRequest webRequest);

	protected void writeWithMessageConverters(NativeWebRequest webRequest, Object returnValue) 
			throws IOException, HttpMediaTypeNotAcceptableException {
		writeWithMessageConverters(returnValue, createInputMessage(webRequest), createOutputMessage(webRequest));
	}

	protected abstract HttpOutputMessage createOutputMessage(NativeWebRequest webRequest);

	@SuppressWarnings("unchecked")
	protected <T> void writeWithMessageConverters(T returnValue, 
												  HttpInputMessage inputMessage, 
												  HttpOutputMessage outputMessage)
			throws IOException, HttpMediaTypeNotAcceptableException {
		
		List<MediaType> acceptedMediaTypes = getAcceptedMediaTypes(inputMessage);

		List<MediaType> allSupportedMediaTypes = new ArrayList<MediaType>();
		if (this.messageConverters != null) {
			for (MediaType acceptedMediaType : acceptedMediaTypes) {
				for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
					if (!messageConverter.canWrite(returnValue.getClass(), acceptedMediaType)) {
						continue;
					}
					((HttpMessageConverter<T>) messageConverter).write(returnValue, acceptedMediaType, outputMessage);
					if (logger.isDebugEnabled()) {
						MediaType contentType = outputMessage.getHeaders().getContentType();
						if (contentType == null) {
							contentType = acceptedMediaType;
						}
						logger.debug("Written [" + returnValue + "] as \"" + contentType + "\" using [" +
								messageConverter + "]");
					}
					return;
				}
			}
			for (HttpMessageConverter<?> messageConverter : messageConverters) {
				allSupportedMediaTypes.addAll(messageConverter.getSupportedMediaTypes());
			}
		}
		throw new HttpMediaTypeNotAcceptableException(allSupportedMediaTypes);
	}

	private List<MediaType> getAcceptedMediaTypes(HttpInputMessage inputMessage) {
		List<MediaType> acceptedMediaTypes = inputMessage.getHeaders().getAccept();
		if (acceptedMediaTypes.isEmpty()) {
			acceptedMediaTypes = Collections.singletonList(MediaType.ALL);
		}

		MediaType.sortByQualityValue(acceptedMediaTypes);
		return acceptedMediaTypes;
	}

}