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

package org.springframework.web.socket.sockjs.client;

import java.net.URI;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Container for the base URL of a SockJS endpoint with additional helper methods
 * to derive related SockJS URLs: specifically, the {@link #getInfoUrl() info}
 * and {@link #getTransportUrl(TransportType) transport} URLs.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.1
 */
public class SockJsUrlInfo {

	private static final IdGenerator idGenerator = new JdkIdGenerator();


	private final URI sockJsUrl;

	private @Nullable String serverId;

	private @Nullable String sessionId;

	private @Nullable UUID uuid;


	/**
	 * Construct a new {@code SockJsUrlInfo} instance,
	 * calculating a random server id and session id if necessary.
	 * @param sockJsUrl the target URL
	 */
	public SockJsUrlInfo(URI sockJsUrl) {
		this.sockJsUrl = sockJsUrl;
	}

	/**
	 * Construct a new {@code SockJsUrlInfo} instance.
	 * @param sockJsUrl the target URL
	 * @param serverId a pre-determined server id, if any
	 * @param sessionId a pre-determined session id, if any
	 * @since 6.1.3
	 */
	public SockJsUrlInfo(URI sockJsUrl, @Nullable String serverId, @Nullable String sessionId) {
		this.sockJsUrl = sockJsUrl;
		this.serverId = serverId;
		this.sessionId = sessionId;
	}


	public URI getSockJsUrl() {
		return this.sockJsUrl;
	}

	public String getServerId() {
		if (this.serverId == null) {
			this.serverId = String.valueOf(Math.abs(getUuid().getMostSignificantBits()) % 1000);
		}
		return this.serverId;
	}

	public String getSessionId() {
		if (this.sessionId == null) {
			this.sessionId = StringUtils.delete(getUuid().toString(), "-");
		}
		return this.sessionId;
	}

	protected UUID getUuid() {
		if (this.uuid == null) {
			this.uuid = idGenerator.generateId();
		}
		return this.uuid;
	}

	public URI getInfoUrl() {
		return UriComponentsBuilder.fromUri(this.sockJsUrl)
				.scheme(getScheme(TransportType.XHR))
				.pathSegment("info")
				.build(true).toUri();
	}

	public URI getTransportUrl(TransportType transportType) {
		return UriComponentsBuilder.fromUri(this.sockJsUrl)
				.scheme(getScheme(transportType))
				.pathSegment(getServerId())
				.pathSegment(getSessionId())
				.pathSegment(transportType.toString())
				.build(true).toUri();
	}

	private String getScheme(TransportType transportType) {
		String scheme = this.sockJsUrl.getScheme();
		if (TransportType.WEBSOCKET.equals(transportType)) {
			if (!scheme.startsWith("ws")) {
				scheme = ("https".equals(scheme) ? "wss" : "ws");
			}
		}
		else {
			if (!scheme.startsWith("http")) {
				scheme = ("wss".equals(scheme) ? "https" : "http");
			}
		}
		return scheme;
	}


	@Override
	public String toString() {
		return "SockJsUrlInfo[url=" + this.sockJsUrl + "]";
	}

}
