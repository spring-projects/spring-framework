/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.sockjs.server;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpMethod;

/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public enum TransportType {

	WEBSOCKET("websocket", HttpMethod.GET),

	XHR("xhr", HttpMethod.POST, "cors", "jsessionid", "no_cache"),

	XHR_SEND("xhr_send", HttpMethod.POST, "cors", "jsessionid", "no_cache"),

	JSONP("jsonp", HttpMethod.GET, "jsessionid", "no_cache"),

	JSONP_SEND("jsonp_send", HttpMethod.POST, "jsessionid", "no_cache"),

	XHR_STREAMING("xhr_streaming", HttpMethod.POST, "cors", "jsessionid", "no_cache"),

	EVENT_SOURCE("eventsource", HttpMethod.GET, "jsessionid", "no_cache"),

	HTML_FILE("htmlfile", HttpMethod.GET, "jsessionid", "no_cache");


	private final String value;

	private final HttpMethod httpMethod;

	private final List<String> headerHints;


	private TransportType(String value, HttpMethod httpMethod, String... headerHints) {
		this.value = value;
		this.httpMethod = httpMethod;
		this.headerHints = Arrays.asList(headerHints);
	}

	public String value() {
		return this.value;
	}

	/**
	 * The HTTP method for this transport.
	 */
	public HttpMethod getHttpMethod() {
		return this.httpMethod;
	}

	public boolean setsNoCache() {
		return this.headerHints.contains("no_cache");
	}

	public boolean supportsCors() {
		return this.headerHints.contains("cors");
	}

	public boolean setsJsessionId() {
		return this.headerHints.contains("jsessionid");
	}

	public static TransportType fromValue(String transportValue) {
		for (TransportType type : values()) {
			if (type.value().equals(transportValue)) {
				return type;
			}
		}
		throw new IllegalArgumentException("No matching constant for [" + transportValue + "]");
	}

	@Override
	public String toString() {
		return this.value;
	}

}
