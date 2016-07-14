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

package org.springframework.http.codec;

import org.springframework.http.MediaType;
import org.springframework.http.codec.SseEventEncoder;

/**
 * Represent a Server-Sent Event.
 *
 * <p>{@code Flux<SseEvent>} is Spring Web Reactive equivalent to Spring MVC
 * {@code SseEmitter} type. It allows to send Server-Sent Events in a reactive way.
 *
 * @author Sebastien Deleuze
 * @see SseEventEncoder
 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
 */
public class SseEvent {

	private String id;

	private String name;

	private Object data;

	private MediaType mediaType;

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
	public SseEvent(Object data, MediaType mediaType) {
		this.data = data;
		this.mediaType = mediaType;
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
	 * defined in Server-Sent Events W3C recommendation.
	 *
	 * If no {@code mediaType} is defined, default {@link SseEventEncoder} will:
	 *  - Turn single line {@code String} to a single {@code data} field
	 *  - Turn multiline line {@code String} to multiple {@code data} fields
	 *  - Serialize other {@code Object} as JSON
	 *
	 * @see #setMediaType(MediaType)
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
	 * Set the {@link MediaType} used to serialize the {@code data}.
	 * {@link SseEventEncoder} should be configured with the relevant encoder to be
	 * able to serialize it.
	 */
	public void setMediaType(MediaType mediaType) {
		this.mediaType = mediaType;
	}

	/**
	 * @see #setMediaType(MediaType)
	 */
	public MediaType getMediaType() {
		return this.mediaType;
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
	 * SSE comment lines by {@link SseEventEncoder} as defined in Server-Sent Events W3C
	 * recommendation.
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
