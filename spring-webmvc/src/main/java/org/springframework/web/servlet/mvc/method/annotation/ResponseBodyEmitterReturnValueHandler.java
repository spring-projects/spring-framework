/*
 * Copyright 2002-2015 the original author or authors.
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
import java.io.OutputStream;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.support.AsyncHandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Supports return values of type {@link ResponseBodyEmitter} and also
 * {@code ResponseEntity<ResponseBodyEmitter>}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class ResponseBodyEmitterReturnValueHandler implements AsyncHandlerMethodReturnValueHandler {

	private static final Log logger = LogFactory.getLog(ResponseBodyEmitterReturnValueHandler.class);

	private final List<HttpMessageConverter<?>> messageConverters;


	public ResponseBodyEmitterReturnValueHandler(List<HttpMessageConverter<?>> messageConverters) {
		Assert.notEmpty(messageConverters, "'messageConverters' must not be empty");
		this.messageConverters = messageConverters;
	}


	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		if (ResponseBodyEmitter.class.isAssignableFrom(returnType.getParameterType())) {
			return true;
		}
		else if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
			Class<?> bodyType = ResolvableType.forMethodParameter(returnType).getGeneric(0).resolve();
			return (bodyType != null && ResponseBodyEmitter.class.isAssignableFrom(bodyType));
		}
		return false;
	}

	@Override
	public boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType) {
		if (returnValue != null) {
			if (returnValue instanceof ResponseBodyEmitter) {
				return true;
			}
			else if (returnValue instanceof ResponseEntity) {
				Object body = ((ResponseEntity) returnValue).getBody();
				return (body != null && body instanceof ResponseBodyEmitter);
			}
		}
		return false;
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue == null) {
			mavContainer.setRequestHandled(true);
			return;
		}

		HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
		ServerHttpResponse outputMessage = new ServletServerHttpResponse(response);

		if (ResponseEntity.class.isAssignableFrom(returnValue.getClass())) {
			ResponseEntity<?> responseEntity = (ResponseEntity<?>) returnValue;
			outputMessage.setStatusCode(responseEntity.getStatusCode());
			outputMessage.getHeaders().putAll(responseEntity.getHeaders());
			returnValue = responseEntity.getBody();
			if (returnValue == null) {
				mavContainer.setRequestHandled(true);
				return;
			}
		}

		ServletRequest request = webRequest.getNativeRequest(ServletRequest.class);
		ShallowEtagHeaderFilter.disableContentCaching(request);

		Assert.isInstanceOf(ResponseBodyEmitter.class, returnValue);
		ResponseBodyEmitter emitter = (ResponseBodyEmitter) returnValue;
		emitter.extendResponse(outputMessage);

		// Commit the response and wrap to ignore further header changes
		outputMessage.getBody();
		outputMessage.flush();
		outputMessage = new StreamingServletServerHttpResponse(outputMessage);

		DeferredResult<?> deferredResult = new DeferredResult<Object>(emitter.getTimeout());
		WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(deferredResult, mavContainer);

		HttpMessageConvertingHandler handler = new HttpMessageConvertingHandler(outputMessage, deferredResult);
		emitter.initialize(handler);
	}


	/**
	 * ResponseBodyEmitter.Handler that writes with HttpMessageConverter's.
	 */
	private class HttpMessageConvertingHandler implements ResponseBodyEmitter.Handler {

		private final ServerHttpResponse outputMessage;

		private final DeferredResult<?> deferredResult;

		public HttpMessageConvertingHandler(ServerHttpResponse outputMessage, DeferredResult<?> deferredResult) {
			this.outputMessage = outputMessage;
			this.deferredResult = deferredResult;
		}

		@Override
		public void send(Object data, MediaType mediaType) throws IOException {
			sendInternal(data, mediaType);
		}

		@SuppressWarnings("unchecked")
		private <T> void sendInternal(T data, MediaType mediaType) throws IOException {
			for (HttpMessageConverter<?> converter : ResponseBodyEmitterReturnValueHandler.this.messageConverters) {
				if (converter.canWrite(data.getClass(), mediaType)) {
					((HttpMessageConverter<T>) converter).write(data, mediaType, this.outputMessage);
					this.outputMessage.flush();
					if (logger.isDebugEnabled()) {
						logger.debug("Written [" + data + "] using [" + converter + "]");
					}
					return;
				}
			}
			throw new IllegalArgumentException("No suitable converter for " + data.getClass());
		}

		@Override
		public void complete() {
			this.deferredResult.setResult(null);
		}

		@Override
		public void completeWithError(Throwable failure) {
			this.deferredResult.setErrorResult(failure);
		}

		@Override
		public void onTimeout(Runnable callback) {
			this.deferredResult.onTimeout(callback);
		}

		@Override
		public void onCompletion(Runnable callback) {
			this.deferredResult.onCompletion(callback);
		}
	}


	/**
	 * Wrap to silently ignore header changes HttpMessageConverter's that would
	 * otherwise cause HttpHeaders to raise exceptions.
	 */
	private static class StreamingServletServerHttpResponse implements ServerHttpResponse {

		private final ServerHttpResponse delegate;

		private final HttpHeaders mutableHeaders = new HttpHeaders();

		public StreamingServletServerHttpResponse(ServerHttpResponse delegate) {
			this.delegate = delegate;
			this.mutableHeaders.putAll(delegate.getHeaders());
		}

		@Override
		public void setStatusCode(HttpStatus status) {
			this.delegate.setStatusCode(status);
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.mutableHeaders;
		}

		@Override
		public OutputStream getBody() throws IOException {
			return this.delegate.getBody();
		}

		@Override
		public void flush() throws IOException {
			this.delegate.flush();
		}

		@Override
		public void close() {
			this.delegate.close();
		}
	}

}
