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

import org.springframework.http.HttpMethod;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public enum TransportType {

	WEBSOCKET("websocket", HttpMethod.GET, false),

	XHR("xhr", HttpMethod.POST, true),
	XHR_SEND("xhr_send", HttpMethod.POST, true),

	JSONP("jsonp", HttpMethod.GET, false),
	JSONP_SEND("jsonp_send", HttpMethod.POST, false),

	XHR_STREAMING("xhr_streaming", HttpMethod.POST, true),
	EVENT_SOURCE("eventsource", HttpMethod.GET, false),
	HTML_FILE("htmlfile", HttpMethod.GET, false);


	private final String value;

	private final HttpMethod httpMethod;

	private final boolean corsSupported;


	private TransportType(String value, HttpMethod httpMethod, boolean corsSupported) {
		this.value = value;
		this.httpMethod = httpMethod;
		this.corsSupported = corsSupported;
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

	/**
	 * Are cross-domain requests (CORS) supported?
	 */
	public boolean isCorsSupported() {
		return this.corsSupported;
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
