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

package org.springframework.web.reactive.sse;

import org.springframework.http.converter.reactive.SseHttpMessageConverter;
import org.springframework.util.MimeType;

/**
 * Represent a Server-Sent Event.
 *
 * <p>{@code Flux<SseEvent>} is Spring Web Reactive equivalent to Spring MVC
 * {@code SseEmitter} type. It allows to send Server-Sent Events in a reactive way.
 *
 * @author Sebastien Deleuze
 * @see SseHttpMessageConverter
 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommandation</a>
 */
public class SseEvent {

	private String id;

	private String name;

	private Object data;

	private MimeType mimeType;

	private Long reconnectTime;

	private String comment;

	/**
	 * Create an empty instance.
	 */
	public SseEvent() {
	}

	/**
	 * Create an instance with the provided {@code data}.
	 */
	public SseEvent(Object data) {
		this.data = data;
	}

	/**
	 * Create an instance with the provided {@code data} and {@code mediaType}.
	 */
	public SseEvent(Object data, MimeType mimeType) {
		this.data = data;
		this.mimeType = mimeType;
	}

	/**
	 * Set the {@code id} SSE field
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @see #setId(String)
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the {@code event} SSE field
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @see #setName(String)
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set {@code data} SSE field. If a multiline {@code String} is provided, it will be
	 * turned into multiple {@code data} field lines by  as
	 * defined in Server-Sent Events W3C recommandation.
	 *
	 * If no {@code mediaType} is defined, default {@link SseHttpMessageConverter} will:
	 *  - Turn single line {@code String} to a single {@code data} field
	 *  - Turn multiline line {@code String} to multiple {@code data} fields
	 *  - Serialize other {@code Object} as JSON
	 *
	 * @see #setMimeType(MimeType)
	 */
	public void setData(Object data) {
		this.data = data;
	}

	/**
	 * @see #setData(Object)
	 */
	public Object getData() {
		return data;
	}

	/**
	 * Set the {@link MimeType} used to serialize the {@code data}.
	 * {@link SseHttpMessageConverter} should be configured with the relevant encoder to be
	 * able to serialize it.
	 */
	public void setMimeType(MimeType mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * @see #setMimeType(MimeType)
	 */
	public MimeType getMimeType() {
		return mimeType;
	}

	/**
	 * Set the {@code retry} SSE field
	 */
	public void setReconnectTime(Long reconnectTime) {
		this.reconnectTime = reconnectTime;
	}

	/**
	 * @see #setReconnectTime(Long)
	 */
	public Long getReconnectTime() {
		return reconnectTime;
	}

	/**
	 * Set SSE comment. If a multiline comment is provided, it will be turned into multiple
	 * SSE comment lines by {@link SseHttpMessageConverter} as defined in Server-Sent Events W3C
	 * recommandation.
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @see #setComment(String)
	 */
	public String getComment() {
		return comment;
	}

}
