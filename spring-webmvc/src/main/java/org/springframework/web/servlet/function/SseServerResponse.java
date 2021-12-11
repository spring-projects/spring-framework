/*
 * Copyright 2002-2020 the original author or authors.
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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.DelegatingServerHttpResponse;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.ModelAndView;

/**
 * Implementation of {@link ServerResponse} for sending
 * <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events</a>.
 *
 * @author Arjen Poutsma
 * @since 5.3.2
 */
final class SseServerResponse extends AbstractServerResponse {

	private final Consumer<SseBuilder> sseConsumer;

	@Nullable
	private final Duration timeout;


	private SseServerResponse(Consumer<SseBuilder> sseConsumer, @Nullable Duration timeout) {
		super(200, createHeaders(), emptyCookies());
		this.sseConsumer = sseConsumer;
		this.timeout = timeout;
	}

	private static HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.TEXT_EVENT_STREAM);
		headers.setCacheControl(CacheControl.noCache());
		return headers;
	}

	private static MultiValueMap<String, Cookie> emptyCookies() {
		return CollectionUtils.toMultiValueMap(Collections.emptyMap());
	}


	@Nullable
	@Override
	protected ModelAndView writeToInternal(HttpServletRequest request, HttpServletResponse response,
			Context context) throws ServletException, IOException {

		DeferredResult<?> result;
		if (this.timeout != null) {
			result = new DeferredResult<>(this.timeout.toMillis());
		}
		else {
			result = new DeferredResult<>();
		}

		DefaultAsyncServerResponse.writeAsync(request, response, result);
		this.sseConsumer.accept(new DefaultSseBuilder(response, context, result));
		return null;
	}


	public static ServerResponse create(Consumer<SseBuilder> sseConsumer, @Nullable Duration timeout) {
		Assert.notNull(sseConsumer, "SseConsumer must not be null");

		return new SseServerResponse(sseConsumer, timeout);
	}


	private static final class DefaultSseBuilder implements SseBuilder {

		private static final byte[] NL_NL = new byte[]{'\n', '\n'};


		private final ServerHttpResponse outputMessage;

		private final DeferredResult<?> deferredResult;

		private final List<HttpMessageConverter<?>> messageConverters;

		private final StringBuilder builder = new StringBuilder();

		private boolean sendFailed;


		public DefaultSseBuilder(HttpServletResponse response, Context context, DeferredResult<?> deferredResult) {
			this.outputMessage = new ServletServerHttpResponse(response);
			this.deferredResult = deferredResult;
			this.messageConverters = context.messageConverters();
		}

		@Override
		public void send(Object object) throws IOException {
			data(object);
		}

		@Override
		public SseBuilder id(String id) {
			Assert.hasLength(id, "Id must not be empty");
			return field("id", id);
		}

		@Override
		public SseBuilder event(String eventName) {
			Assert.hasLength(eventName, "Name must not be empty");
			return field("event", eventName);
		}

		@Override
		public SseBuilder retry(Duration duration) {
			Assert.notNull(duration, "Duration must not be null");
			String millis = Long.toString(duration.toMillis());
			return field("retry", millis);
		}

		@Override
		public SseBuilder comment(String comment) {
			Assert.hasLength(comment, "Comment must not be empty");
			String[] lines = comment.split("\n");
			for (String line : lines) {
				field("", line);
			}
			return this;
		}

		private SseBuilder field(String name, String value) {
			this.builder.append(name).append(':').append(value).append('\n');
			return this;
		}

		@Override
		public void data(Object object) throws IOException {
			Assert.notNull(object, "Object must not be null");

			if (object instanceof String) {
				writeString((String) object);
			}
			else {
				writeObject(object);
			}
		}

		private void writeString(String string) throws IOException {
			String[] lines = string.split("\n");
			for (String line : lines) {
				field("data", line);
			}
			this.builder.append('\n');

			try {
				OutputStream body = this.outputMessage.getBody();
				body.write(builderBytes());
				body.flush();
			}
			catch (IOException ex) {
				this.sendFailed = true;
				throw ex;
			}
			finally {
				this.builder.setLength(0);
			}
		}

		@SuppressWarnings("unchecked")
		private void writeObject(Object data) throws IOException {
			this.builder.append("data:");
			try {
				this.outputMessage.getBody().write(builderBytes());

				Class<?> dataClass = data.getClass();
				for (HttpMessageConverter<?> converter : this.messageConverters) {
					if (converter.canWrite(dataClass, MediaType.APPLICATION_JSON)) {
						HttpMessageConverter<Object> objectConverter = (HttpMessageConverter<Object>) converter;
						ServerHttpResponse response = new MutableHeadersServerHttpResponse(this.outputMessage);
						objectConverter.write(data, MediaType.APPLICATION_JSON, response);
						this.outputMessage.getBody().write(NL_NL);
						this.outputMessage.flush();
						return;
					}
				}
			}
			catch (IOException ex) {
				this.sendFailed = true;
				throw ex;
			}
			finally {
				this.builder.setLength(0);
			}
		}

		private byte[] builderBytes() {
			return this.builder.toString().getBytes(StandardCharsets.UTF_8);
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
		public SseBuilder onTimeout(Runnable onTimeout) {
			this.deferredResult.onTimeout(onTimeout);
			return this;
		}

		@Override
		public SseBuilder onError(Consumer<Throwable> onError) {
			this.deferredResult.onError(onError);
			return this;
		}

		@Override
		public SseBuilder onComplete(Runnable onCompletion) {
			this.deferredResult.onCompletion(onCompletion);
			return this;
		}


		/**
		 * Wrap to silently ignore header changes HttpMessageConverter's that would
		 * otherwise cause HttpHeaders to raise exceptions.
		 */
		private static final class MutableHeadersServerHttpResponse extends DelegatingServerHttpResponse {

			private final HttpHeaders mutableHeaders = new HttpHeaders();

			public MutableHeadersServerHttpResponse(ServerHttpResponse delegate) {
				super(delegate);
				this.mutableHeaders.putAll(delegate.getHeaders());
			}

			@Override
			public HttpHeaders getHeaders() {
				return this.mutableHeaders;
			}

		}

	}
}
