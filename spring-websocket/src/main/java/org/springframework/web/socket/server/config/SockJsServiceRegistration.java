/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.server.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;


/**
 * A helper class for configuring SockJS fallback options, typically used indirectly, in
 * conjunction with {@link EnableWebSocket @EnableWebSocket} and
 * {@link WebSocketConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsServiceRegistration {

	private TaskScheduler taskScheduler;

	private String clientLibraryUrl;

	private Integer streamBytesLimit;

	private Boolean sessionCookieEnabled;

	private Long heartbeatTime;

	private Long disconnectDelay;

	private Integer httpMessageCacheSize;

	private Boolean webSocketEnabled;

	private final List<HandshakeInterceptor> handshakeInterceptors = new ArrayList<HandshakeInterceptor>();


	public SockJsServiceRegistration(TaskScheduler defaultTaskScheduler) {
		this.taskScheduler = defaultTaskScheduler;
	}


	public SockJsServiceRegistration setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
		return this;
	}

	protected TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * Transports which don't support cross-domain communication natively (e.g.
	 * "eventsource", "htmlfile") rely on serving a simple page (using the
	 * "foreign" domain) from an invisible iframe. Code run from this iframe
	 * doesn't need to worry about cross-domain issues since it is running from
	 * a domain local to the SockJS server. The iframe does need to load the
	 * SockJS javascript client library and this option allows configuring its
	 * url.
	 *
	 * <p>By default this is set to point to
	 * "https://d1fxtkz8shb9d2.cloudfront.net/sockjs-0.3.4.min.js".
	 */
	public SockJsServiceRegistration setClientLibraryUrl(String clientLibraryUrl) {
		this.clientLibraryUrl = clientLibraryUrl;
		return this;
	}

	/**
	 * The URL to the SockJS JavaScript client library.
	 * @see #setSockJsClientLibraryUrl(String)
	 */
	protected String getClientLibraryUrl() {
		return this.clientLibraryUrl;
	}

	/**
	 * Streaming transports save responses on the client side and don't free
	 * memory used by delivered messages. Such transports need to recycle the
	 * connection once in a while. This property sets a minimum number of bytes
	 * that can be send over a single HTTP streaming request before it will be
	 * closed. After that client will open a new request. Setting this value to
	 * one effectively disables streaming and will make streaming transports to
	 * behave like polling transports.
	 *
	 * <p>The default value is 128K (i.e. 128 * 1024).
	 */
	public SockJsServiceRegistration setStreamBytesLimit(int streamBytesLimit) {
		this.streamBytesLimit = streamBytesLimit;
		return this;
	}

	protected Integer getStreamBytesLimit() {
		return this.streamBytesLimit;
	}

	/**
	 * Some load balancers do sticky sessions, but only if there is a "JSESSIONID"
	 * cookie. Even if it is set to a dummy value, it doesn't matter since
	 * session information is added by the load balancer.
	 *
	 * <p>The default value is "false" since Java servers set the session cookie.
	 */
	public SockJsServiceRegistration setDummySessionCookieEnabled(boolean sessionCookieEnabled) {
		this.sessionCookieEnabled = sessionCookieEnabled;
		return this;
	}

	/**
	 * Whether setting JSESSIONID cookie is necessary.
	 * @see #setDummySessionCookieEnabled(boolean)
	 */
	protected Boolean getDummySessionCookieEnabled() {
		return this.sessionCookieEnabled;
	}

	/**
	 * The amount of time in milliseconds when the server has not sent any
	 * messages and after which the server should send a heartbeat frame to the
	 * client in order to keep the connection from breaking.
	 *
	 * <p>The default value is 25,000 (25 seconds).
	 */
	public SockJsServiceRegistration setHeartbeatTime(long heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
		return this;
	}

	protected Long getHeartbeatTime() {
		return this.heartbeatTime;
	}

	/**
	 * The amount of time in milliseconds before a client is considered
	 * disconnected after not having a receiving connection, i.e. an active
	 * connection over which the server can send data to the client.
	 *
	 * <p>The default value is 5000.
	 */
	public SockJsServiceRegistration setDisconnectDelay(long disconnectDelay) {
		this.disconnectDelay = disconnectDelay;
		return this;
	}

	/**
	 * Return the amount of time in milliseconds before a client is considered disconnected.
	 */
	protected Long getDisconnectDelay() {
		return this.disconnectDelay;
	}

	/**
	 * The number of server-to-client messages that a session can cache while waiting for
	 * the next HTTP polling request from the client. All HTTP transports use this
	 * property since even streaming transports recycle HTTP requests periodically.
	 * <p>
	 * The amount of time between HTTP requests should be relatively brief and will not
	 * exceed the allows disconnect delay (see
	 * {@link #setDisconnectDelay(long)}), 5 seconds by default.
	 * <p>
	 * The default size is 100.
	 */
	public SockJsServiceRegistration setHttpMessageCacheSize(int httpMessageCacheSize) {
		this.httpMessageCacheSize = httpMessageCacheSize;
		return this;
	}

	/**
	 * Return the size of the HTTP message cache.
	 */
	protected Integer getHttpMessageCacheSize() {
		return this.httpMessageCacheSize;
	}

	/**
	 * Some load balancers don't support WebSocket. This option can be used to
	 * disable the WebSocket transport on the server side.
	 *
	 * <p>The default value is "true".
	 */
	public SockJsServiceRegistration setWebSocketEnabled(boolean webSocketEnabled) {
		this.webSocketEnabled = webSocketEnabled;
		return this;
	}

	/**
	 * Whether WebSocket transport is enabled.
	 * @see #setWebSocketsEnabled(boolean)
	 */
	protected Boolean getWebSocketEnabled() {
		return this.webSocketEnabled;
	}

	public SockJsServiceRegistration setInterceptors(HandshakeInterceptor... interceptors) {
		if (!ObjectUtils.isEmpty(interceptors)) {
			this.handshakeInterceptors.addAll(Arrays.asList(interceptors));
		}
		return this;
	}

	protected List<HandshakeInterceptor> getInterceptors() {
		return this.handshakeInterceptors;
	}

	protected SockJsService getSockJsService(String[] sockJsPrefixes) {
		DefaultSockJsService service = createSockJsService();
		if (sockJsPrefixes != null) {
			service.setValidSockJsPrefixes(sockJsPrefixes);
		}
		if (getClientLibraryUrl() != null) {
			service.setSockJsClientLibraryUrl(getClientLibraryUrl());
		}
		if (getStreamBytesLimit() != null) {
			service.setStreamBytesLimit(getStreamBytesLimit());
		}
		if (getDummySessionCookieEnabled() != null) {
			service.setDummySessionCookieEnabled(getDummySessionCookieEnabled());
		}
		if (getHeartbeatTime() != null) {
			service.setHeartbeatTime(getHeartbeatTime());
		}
		if (getDisconnectDelay() != null) {
			service.setDisconnectDelay(getDisconnectDelay());
		}
		if (getHttpMessageCacheSize() != null) {
			service.setHttpMessageCacheSize(getHttpMessageCacheSize());
		}
		if (getWebSocketEnabled() != null) {
			service.setWebSocketsEnabled(getWebSocketEnabled());
		}
		service.setHandshakeInterceptors(getInterceptors());
		return service;
	}

	protected DefaultSockJsService createSockJsService() {
		return new DefaultSockJsService(getTaskScheduler());
	}

}
