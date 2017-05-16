/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.OutputStream;
import java.util.concurrent.Callable;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Supports return values of type
 * {@link org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody}
 * and also {@code ResponseEntity<StreamingResponseBody>}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class StreamingResponseBodyReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		if (StreamingResponseBody.class.isAssignableFrom(returnType.getParameterType())) {
			return true;
		}
		else if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
			Class<?> bodyType = ResolvableType.forMethodParameter(returnType).getGeneric(0).resolve();
			return (bodyType != null && StreamingResponseBody.class.isAssignableFrom(bodyType));
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

		if (returnValue instanceof ResponseEntity) {
			ResponseEntity<?> responseEntity = (ResponseEntity<?>) returnValue;
			response.setStatus(responseEntity.getStatusCodeValue());
			outputMessage.getHeaders().putAll(responseEntity.getHeaders());
			returnValue = responseEntity.getBody();
			if (returnValue == null) {
				mavContainer.setRequestHandled(true);
				outputMessage.flush();
				return;
			}
		}

		ServletRequest request = webRequest.getNativeRequest(ServletRequest.class);
		ShallowEtagHeaderFilter.disableContentCaching(request);

		Assert.isInstanceOf(StreamingResponseBody.class, returnValue, "StreamingResponseBody expected");
		StreamingResponseBody streamingBody = (StreamingResponseBody) returnValue;

		Callable<Void> callable = new StreamingResponseBodyTask(outputMessage.getBody(), streamingBody);
		WebAsyncUtils.getAsyncManager(webRequest).startCallableProcessing(callable, mavContainer);
	}


	private static class StreamingResponseBodyTask implements Callable<Void> {

		private final OutputStream outputStream;

		private final StreamingResponseBody streamingBody;

		public StreamingResponseBodyTask(OutputStream outputStream, StreamingResponseBody streamingBody) {
			this.outputStream = outputStream;
			this.streamingBody = streamingBody;
		}

		@Override
		public Void call() throws Exception {
			this.streamingBody.writeTo(this.outputStream);
			return null;
		}
	}

}
