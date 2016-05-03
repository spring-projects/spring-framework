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

package org.springframework.web.socket.sockjs.transport;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpMethod;

/**
 * SockJS transport types.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
public enum TransportType {

	WEBSOCKET("websocket", HttpMethod.GET, "origin"),

	XHR("xhr", HttpMethod.POST, "cors", "jsessionid", "no_cache"),

	XHR_SEND("xhr_send", HttpMethod.POST, "cors", "jsessionid", "no_cache"),

	JSONP("jsonp", HttpMethod.GET, "jsessionid", "no_cache"),

	JSONP_SEND("jsonp_send", HttpMethod.POST, "jsessionid", "no_cache"),

	XHR_STREAMING("xhr_streaming", HttpMethod.POST, "cors", "jsessionid", "no_cache"),

	EVENT_SOURCE("eventsource", HttpMethod.GET, "origin", "jsessionid", "no_cache"),

	HTML_FILE("htmlfile", HttpMethod.GET, "cors", "jsessionid", "no_cache");


	private static final Map<String, TransportType> TRANSPORT_TYPES;

	static {
		Map<String, TransportType> transportTypes = new HashMap<String, TransportType>();
		for (TransportType type : values()) {
			transportTypes.put(type.value, type);
		}
		TRANSPORT_TYPES = Collections.unmodifiableMap(transportTypes);
	}

	public static TransportType fromValue(String value) {
		return TRANSPORT_TYPES.get(value);
	}


	private final String value;

	private final HttpMethod httpMethod;

	private final List<String> headerHints;


	TransportType(String value, HttpMethod httpMethod, String... headerHints) {
		this.value = value;
		this.httpMethod = httpMethod;
		this.headerHints = Arrays.asList(headerHints);
	}


	public String value() {
		return this.value;
	}

	public HttpMethod getHttpMethod() {
		return this.httpMethod;
	}

	public boolean sendsNoCacheInstruction() {
		return this.headerHints.contains("no_cache");
	}

	public boolean sendsSessionCookie() {
		return this.headerHints.contains("jsessionid");
	}

	public boolean supportsCors() {
		return this.headerHints.contains("cors");
	}

	public boolean supportsOrigin() {
		return this.headerHints.contains("cors") || this.headerHints.contains("origin");
	}


	@Override
	public String toString() {
		return this.value;
	}

}
