/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.DelegatingServerHttpResponse;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;

/**
 * Implementation of {@link ServerResponse} for sending streaming response bodies.
 *
 * @author Brian Clozel
 */
final class StreamingServerResponse extends AbstractServerResponse {

	private final Consumer<StreamBuilder> streamConsumer;

	@Nullable
	private final Duration timeout;

	private StreamingServerResponse(HttpStatusCode statusCode, HttpHeaders headers, MultiValueMap<String, Cookie> cookies,
									Consumer<StreamBuilder> streamConsumer, @Nullable Duration timeout) {
		super(statusCode, headers, cookies);
		this.streamConsumer = streamConsumer;
		this.timeout = timeout;
	}

	static ServerResponse create(HttpStatusCode statusCode, HttpHeaders headers, MultiValueMap<String, Cookie> cookies,
										Consumer<StreamBuilder> streamConsumer, @Nullable Duration timeout) {
		Assert.notNull(statusCode, "statusCode must not be null");
		Assert.notNull(headers, "headers must not be null");
		Assert.notNull(cookies, "cookies must not be null");
		Assert.notNull(streamConsumer, "streamConsumer must not be null");
		return new StreamingServerResponse(statusCode, headers, cookies, streamConsumer, timeout);
	}

	@Nullable
	@Override
	protected ModelAndView writeToInternal(HttpServletRequest request, HttpServletResponse response, Context context) throws Exception {
		DeferredResult<?> result;
		if (this.timeout != null) {
			result = new DeferredResult<>(this.timeout.toMillis());
		}
		else {
			result = new DeferredResult<>();
		}
		DefaultAsyncServerResponse.writeAsync(request, response, result);
		this.streamConsumer.accept(new DefaultStreamBuilder(response, context, result, this.headers()));
		return null;
	}

	private static class DefaultStreamBuilder implements StreamBuilder {

		private final ServerHttpResponse outputMessage;

		private final DeferredResult<?> deferredResult;

		private final List<HttpMessageConverter<?>> messageConverters;

		private final HttpHeaders httpHeaders;

		private boolean sendFailed;


		public DefaultStreamBuilder(HttpServletResponse response, Context context, DeferredResult<?> deferredResult,
									HttpHeaders httpHeaders) {
			this.outputMessage = new ServletServerHttpResponse(response);
			this.deferredResult = deferredResult;
			this.messageConverters = context.messageConverters();
			this.httpHeaders = httpHeaders;
		}

		@Override
		public StreamBuilder write(Object object) throws IOException {
			write(object, null);
			return this;
		}

		@Override
		public StreamBuilder write(Object object, @Nullable MediaType mediaType) throws IOException {
			Assert.notNull(object, "data must not be null");
			try {
				if (object instanceof byte[] bytes) {
					this.outputMessage.getBody().write(bytes);
				}
				else if (object instanceof String str) {
					this.outputMessage.getBody().write(str.getBytes(StandardCharsets.UTF_8));
				}
				else {
					writeObject(object, mediaType);
				}
			}
			catch (IOException ex) {
				this.sendFailed = true;
				throw ex;
			}
			return this;
		}

		@SuppressWarnings("unchecked")
		private void writeObject(Object data, @Nullable MediaType mediaType) throws IOException {
			Class<?> elementClass = data.getClass();
			for (HttpMessageConverter<?> converter : this.messageConverters) {
				if (converter.canWrite(elementClass, mediaType)) {
					HttpMessageConverter<Object> objectConverter = (HttpMessageConverter<Object>) converter;
					ServerHttpResponse response = new MutableHeadersServerHttpResponse(this.outputMessage, this.httpHeaders);
					objectConverter.write(data, mediaType, response);
					return;
				}
			}
		}

		@Override
		public void flush() throws IOException {
			if (this.sendFailed) {
				return;
			}
			try {
				this.outputMessage.flush();
			}
			catch (IOException ex) {
				this.sendFailed = true;
				throw ex;
			}
		}

		@Override
		public void error(Throwable t) {
			if (this.sendFailed) {
				return;
			}
			this.deferredResult.setErrorResult(t);
		}

		@Override
		public void complete() {
			if (this.sendFailed) {
				return;
			}
			try {
				this.outputMessage.flush();
				this.deferredResult.setResult(null);
			}
			catch (IOException ex) {
				this.deferredResult.setErrorResult(ex);
			}
		}

		@Override
		public StreamBuilder onTimeout(Runnable onTimeout) {
			this.deferredResult.onTimeout(onTimeout);
			return this;
		}

		@Override
		public StreamBuilder onError(Consumer<Throwable> onError) {
			this.deferredResult.onError(onError);
			return this;
		}

		@Override
		public StreamBuilder onComplete(Runnable onCompletion) {
			this.deferredResult.onCompletion(onCompletion);
			return this;
		}

		/**
		 * Wrap to silently ignore header changes HttpMessageConverter's that would
		 * otherwise cause HttpHeaders to raise exceptions.
		 */
		private static final class MutableHeadersServerHttpResponse extends DelegatingServerHttpResponse {

			private final HttpHeaders mutableHeaders = new HttpHeaders();

			public MutableHeadersServerHttpResponse(ServerHttpResponse delegate, HttpHeaders headers) {
				super(delegate);
				this.mutableHeaders.putAll(delegate.getHeaders());
				this.mutableHeaders.putAll(headers);
			}

			@Override
			public HttpHeaders getHeaders() {
				return this.mutableHeaders;
			}

		}

	}

}
