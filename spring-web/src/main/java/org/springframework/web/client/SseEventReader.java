/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.SmartHttpMessageConverter;
import org.springframework.util.Assert;

/**
 * Blocking counterpart to {@link org.springframework.http.codec.ServerSentEventHttpMessageReader}:
 * reads a {@code text/event-stream} response body off a blocking {@link ClientHttpResponse},
 * parses the byte stream into individual {@link ServerSentEvent events}, and invokes a
 * consumer for each decoded event.
 *
 * <p>The grammar implemented here follows the
 * <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html#parsing-an-event-stream">
 * HTML Living Standard: parsing an event stream</a>, and intentionally mirrors the logic in
 * {@code ServerSentEventHttpMessageReader} so that blocking and reactive clients observe
 * the same events.
 *
 * @author RestClient team
 * @since 7.1
 * @see RestClient.ResponseSpec#bodyToServerSentEvents(Class, Consumer)
 */
final class SseEventReader {

	private final List<HttpMessageConverter<?>> messageConverters;

	private final @Nullable Map<String, Object> hints;


	/**
	 * Create a new reader that uses the given converters to decode the
	 * {@code data} payload of each event.
	 */
	SseEventReader(List<HttpMessageConverter<?>> messageConverters, @Nullable Map<String, Object> hints) {
		Assert.notNull(messageConverters, "MessageConverters must not be null");
		this.messageConverters = messageConverters;
		this.hints = hints;
	}


	/**
	 * Read all events from the given response, invoking {@code consumer} for each.
	 * Blocks until the response body is exhausted (the server closes the stream)
	 * or an I/O error occurs. The response is <em>not</em> closed by this method.
	 */
	<T> void read(ClientHttpResponse response, Class<T> dataType, Consumer<ServerSentEvent<T>> consumer)
			throws IOException {

		Assert.notNull(dataType, "DataType must not be null");
		Assert.notNull(consumer, "Consumer must not be null");

		MediaType contentType = response.getHeaders().getContentType();
		Charset charset = (contentType != null && contentType.getCharset() != null ?
				contentType.getCharset() : StandardCharsets.UTF_8);

		List<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), charset))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.isEmpty()) {
					dispatch(lines, dataType, consumer);
					lines.clear();
				}
				else {
					lines.add(line);
				}
			}
			// A final event may not be followed by a blank line.
			dispatch(lines, dataType, consumer);
		}
	}

	private <T> void dispatch(List<String> lines, Class<T> dataType, Consumer<ServerSentEvent<T>> consumer)
			throws IOException {

		if (lines.isEmpty()) {
			return;
		}
		ServerSentEvent<T> event = buildEvent(lines, dataType);
		if (event != null) {
			consumer.accept(event);
		}
	}

	/**
	 * Parse a block of non-empty lines (a single event) into a
	 * {@link ServerSentEvent}. Mirrors {@code ServerSentEventHttpMessageReader#buildEvent}.
	 */
	private <T> @Nullable ServerSentEvent<T> buildEvent(List<String> lines, Class<T> dataType) throws IOException {
		ServerSentEvent.Builder<T> sseBuilder = ServerSentEvent.builder();
		StringBuilder data = null;
		StringBuilder comment = null;

		for (String line : lines) {
			if (line.startsWith("data:")) {
				data = (data != null ? data : new StringBuilder());
				int length = line.length();
				if (length > 5) {
					int index = (line.charAt(5) != ' ' ? 5 : 6);
					if (length > index) {
						data.append(line, index, length);
					}
				}
				data.append('\n');
			}
			else if (line.startsWith("id:")) {
				sseBuilder.id(line.substring(3).trim());
			}
			else if (line.startsWith("event:")) {
				sseBuilder.event(line.substring(6).trim());
			}
			else if (line.startsWith("retry:")) {
				Long retry = parseRetry(line.substring(6).trim());
				if (retry != null) {
					sseBuilder.retry(Duration.ofMillis(retry));
				}
			}
			else if (line.startsWith(":")) {
				comment = (comment != null ? comment : new StringBuilder());
				comment.append(line.substring(1).trim()).append('\n');
			}
		}

		if (comment != null) {
			sseBuilder.comment(comment.substring(0, comment.length() - 1));
		}

		T decodedData = (data != null ? decodeData(data, dataType) : null);
		if (decodedData != null) {
			sseBuilder.data(decodedData);
		}
		return sseBuilder.build();
	}

	/**
	 * Parse the value of a {@code retry} field. Ignored unless it consists solely
	 * of ASCII digits and fits into a {@code long}.
	 */
	private static @Nullable Long parseRetry(String value) {
		if (value.isEmpty()) {
			return null;
		}
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (ch < '0' || ch > '9') {
				return null;
			}
		}
		try {
			return Long.parseLong(value);
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	/**
	 * Decode the raw {@code data} text into the target type. {@code String} data
	 * is returned verbatim (stripping the trailing newline added per data line);
	 * any other type is decoded via the configured {@link HttpMessageConverter}s,
	 * trying the response content type first and falling back to
	 * {@code application/json}, the typical encoding of SSE payloads.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private <T> T decodeData(StringBuilder data, Class<T> dataType) throws IOException {
		if (dataType == String.class) {
			return (T) data.substring(0, data.length() - 1);
		}

		byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
		HttpInputMessage message = new DataHttpInputMessage(bytes);

		for (MediaType mediaType : CANDIDATE_MEDIA_TYPES) {
			for (HttpMessageConverter<?> converter : this.messageConverters) {
				if (converter instanceof GenericHttpMessageConverter genericConverter) {
					if (genericConverter.canRead(dataType, null, mediaType)) {
						return (T) genericConverter.read(dataType, null, message);
					}
				}
				else if (converter instanceof SmartHttpMessageConverter smartConverter) {
					ResolvableType resolvableType = ResolvableType.forType(dataType);
					if (smartConverter.canRead(resolvableType, mediaType)) {
						return (T) smartConverter.read(resolvableType, message, this.hints);
					}
				}
				else if (converter.canRead(dataType, mediaType)) {
					@SuppressWarnings({"rawtypes", "unchecked"})
					HttpMessageConverter rawConverter = converter;
					return (T) rawConverter.read(dataType, message);
				}
			}
		}

		throw new RestClientException("No HttpMessageConverter can read SSE data of type [" +
				dataType.getName() + "] with content type [" + CANDIDATE_MEDIA_TYPES + "]");
	}


	/** Candidate content types used when decoding an event's data payload. */
	private static final List<MediaType> CANDIDATE_MEDIA_TYPES =
			List.of(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON);


	/**
	 * Trivial {@link HttpInputMessage} backed by an in-memory byte array, used to
	 * feed a single event's {@code data} bytes to a message converter.
	 */
	private static final class DataHttpInputMessage implements HttpInputMessage {

		private final byte[] bytes;

		private final HttpHeaders headers = new HttpHeaders();

		DataHttpInputMessage(byte[] bytes) {
			this.bytes = bytes;
		}

		@Override
		public InputStream getBody() {
			return new ByteArrayInputStream(this.bytes);
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}
	}

}
