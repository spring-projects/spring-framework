/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.transport;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

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

	XHR_STREAMING("xhr_streaming", HttpMethod.POST, "cors", "jsessionid", "no_cache"),

	EVENT_SOURCE("eventsource", HttpMethod.GET, "origin", "jsessionid", "no_cache"),

	HTML_FILE("htmlfile", HttpMethod.GET, "cors", "jsessionid", "no_cache");


	private static final Map<String, TransportType> TRANSPORT_TYPES =
			Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(type -> type.value, type -> type));


	public static @Nullable TransportType fromValue(String value) {
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
