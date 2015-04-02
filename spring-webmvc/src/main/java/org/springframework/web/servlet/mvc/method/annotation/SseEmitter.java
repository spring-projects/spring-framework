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
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;

/**
 * A specialization of {@link ResponseBodyEmitter} for sending
 * <a href="http://www.w3.org/TR/eventsource/">Server-Sent Events</a>.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class SseEmitter extends ResponseBodyEmitter {

	static final MediaType TEXT_PLAIN = new MediaType("text", "plain", Charset.forName("UTF-8"));


	@Override
	protected void extendResponse(ServerHttpResponse outputMessage) {
		super.extendResponse(outputMessage);
		HttpHeaders headers = outputMessage.getHeaders();
		if (headers.getContentType() == null) {
			headers.setContentType(new MediaType("text", "event-stream"));
		}
	}

	/**
	 * Send the object formatted as a single SSE "data" line. It's equivalent to:
	 * <pre>
	 * // static import of SseEmitter.*
	 *
	 * SseEmitter emitter = new SseEmitter();
	 * emitter.send(event().data(myObject));
	 * </pre>
	 * @param object the object to write
	 * @throws IOException raised when an I/O error occurs
	 * @throws java.lang.IllegalStateException wraps any other errors
	 */
	@Override
	public void send(Object object) throws IOException {
		send(object, null);
	}

	/**
	 * Send the object formatted as a single SSE "data" line. It's equivalent to:
	 * <pre>
	 * // static import of SseEmitter.*
	 *
	 * SseEmitter emitter = new SseEmitter();
	 * emitter.send(event().data(myObject, MediaType.APPLICATION_JSON));
	 * </pre>
	 * @param object the object to write
	 * @param mediaType a MediaType hint for selecting an HttpMessageConverter
	 * @throws IOException raised when an I/O error occurs
	 * @throws java.lang.IllegalStateException wraps any other errors
	 */
	@Override
	public void send(Object object, MediaType mediaType) throws IOException {
		if (object == null) {
			return;
		}
		send(event().data(object, mediaType));
	}

	/**
	 * Send an SSE event prepared with the given builder. For example:
	 * <pre>
	 * // static import of SseEmitter
	 *
	 * SseEmitter emitter = new SseEmitter();
	 * emitter.send(event().name("update").id("1").data(myObject));
	 * </pre>
	 * @param builder a builder for an SSE formatted event.
	 * @throws IOException raised when an I/O error occurs
	 * @throws java.lang.IllegalStateException wraps any other errors
	 */
	public void send(SseEventBuilder builder) throws IOException {
		Map<Object, MediaType> map = builder.build();
		for (Map.Entry<Object, MediaType> entry : map.entrySet()) {
			super.send(entry.getKey(), entry.getValue());
		}
	}


	public static SseEventBuilder event() {
		return new DefaultSseEventBuilder();
	}


	/**
	 * A builder for an SSE event.
	 */
	public interface SseEventBuilder {

		/**
		 * Add an SSE "comment" line.
		 */
		SseEventBuilder comment(String comment);

		/**
		 * Add an SSE "event" line.
		 */
		SseEventBuilder name(String eventName);

		/**
		 * Add an SSE "id" line.
		 */
		SseEventBuilder id(String id);

		/**
		 * Add an SSE "event" line.
		 */
		SseEventBuilder reconnectTime(long reconnectTimeMillis);

		/**
		 * Add an SSE "data" line.
		 */
		SseEventBuilder data(Object object);

		/**
		 * Add an SSE "data" line.
		 */
		SseEventBuilder data(Object object, MediaType mediaType);

		/**
		 * Return a map with objects that represent the data to be written to
		 * the response as well as the required SSE text formatting that
		 * surrounds it.
		 */
		Map<Object, MediaType> build();
	}


	/**
	 * Default implementation of SseEventBuilder.
	 */
	private static class DefaultSseEventBuilder implements SseEventBuilder {

		private final Map<Object, MediaType> map = new LinkedHashMap<Object, MediaType>(4);

		private StringBuilder sb;

		@Override
		public SseEventBuilder comment(String comment) {
			append(":").append(comment != null ? comment : "").append("\n");
			return this;
		}

		@Override
		public SseEventBuilder name(String name) {
			append("name:").append(name != null ? name : "").append("\n");
			return this;
		}

		@Override
		public SseEventBuilder id(String id) {
			append("id:").append(id != null ? id : "").append("\n");
			return this;
		}

		@Override
		public SseEventBuilder reconnectTime(long reconnectTimeMillis) {
			append("retry:").append(String.valueOf(reconnectTimeMillis)).append("\n");
			return this;
		}

		@Override
		public SseEventBuilder data(Object object) {
			return data(object, null);
		}

		@Override
		public SseEventBuilder data(Object object, MediaType mediaType) {
			append("data:");
			saveAppendedText();
			this.map.put(object, mediaType);
			append("\n");
			return this;
		}

		DefaultSseEventBuilder append(String text) {
			if (this.sb == null) {
				this.sb = new StringBuilder();
			}
			this.sb.append(text);
			return this;
		}

		private void saveAppendedText() {
			if (this.sb != null) {
				this.map.put(this.sb.toString(), TEXT_PLAIN);
				this.sb = null;
			}
		}

		@Override
		public Map<Object, MediaType> build() {
			if (this.sb == null || this.sb.length() == 0 && this.map.isEmpty()) {
				return Collections.<Object, MediaType>emptyMap();
			}
			append("\n");
			saveAppendedText();
			return this.map;
		}
	}

}
