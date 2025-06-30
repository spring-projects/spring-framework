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

package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;

/**
 * A helper class for configuring SockJS fallback options for use with an
 * {@link org.springframework.web.socket.config.annotation.EnableWebSocket} and
 * {@link WebSocketConfigurer} setup.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsServiceRegistration {

	private @Nullable TaskScheduler scheduler;

	private @Nullable String clientLibraryUrl;

	private @Nullable Integer streamBytesLimit;

	private @Nullable Boolean sessionCookieNeeded;

	private @Nullable Long heartbeatTime;

	private @Nullable Long disconnectDelay;

	private @Nullable Integer httpMessageCacheSize;

	private @Nullable Boolean webSocketEnabled;

	private final List<TransportHandler> transportHandlers = new ArrayList<>();

	private final List<TransportHandler> transportHandlerOverrides = new ArrayList<>();

	private final List<HandshakeInterceptor> interceptors = new ArrayList<>();

	private final List<String> allowedOrigins = new ArrayList<>();

	private final List<String> allowedOriginPatterns = new ArrayList<>();

	private @Nullable Boolean suppressCors;

	private @Nullable SockJsMessageCodec messageCodec;


	public SockJsServiceRegistration() {
	}


	/**
	 * A scheduler instance to use for scheduling SockJS heart-beats.
	 */
	public SockJsServiceRegistration setTaskScheduler(TaskScheduler scheduler) {
		Assert.notNull(scheduler, "TaskScheduler is required");
		this.scheduler = scheduler;
		return this;
	}

	/**
	 * Transports with no native cross-domain communication (for example, "eventsource",
	 * "htmlfile") must get a simple page from the "foreign" domain in an invisible
	 * {@code iframe} so that code in the {@code iframe} can run from a domain
	 * local to the SockJS server. Since the {@code iframe} needs to load the
	 * SockJS JavaScript client library, this property allows specifying where to
	 * load it from.
	 * <p>By default this is set to point to
	 * <a href="https://cdn.jsdelivr.net/sockjs/1.0.0/sockjs.min.js">"https://cdn.jsdelivr.net/sockjs/1.0.0/sockjs.min.js"</a>.
	 * However, it can also be set to point to a URL served by the application.
	 * <p>Note that it's possible to specify a relative URL in which case the URL
	 * must be relative to the {@code iframe} URL. For example assuming a SockJS endpoint
	 * mapped to "/sockjs", and resulting {@code iframe} URL "/sockjs/iframe.html", then
	 * the relative URL must start with "../../" to traverse up to the location
	 * above the SockJS mapping. In case of a prefix-based Servlet mapping one more
	 * traversals may be needed.
	 */
	public SockJsServiceRegistration setClientLibraryUrl(String clientLibraryUrl) {
		this.clientLibraryUrl = clientLibraryUrl;
		return this;
	}

	/**
	 * Streaming transports save responses on the client side and don't free
	 * memory used by delivered messages. Such transports need to recycle the
	 * connection once in a while. This property sets a minimum number of bytes
	 * that can be sent over a single HTTP streaming request before it will be
	 * closed. After that client will open a new request. Setting this value to
	 * one effectively disables streaming and will make streaming transports to
	 * behave like polling transports.
	 * <p>The default value is 128K (i.e. 128 * 1024).
	 */
	public SockJsServiceRegistration setStreamBytesLimit(int streamBytesLimit) {
		this.streamBytesLimit = streamBytesLimit;
		return this;
	}

	/**
	 * The SockJS protocol requires a server to respond to the initial "/info" request
	 * from clients with a "cookie_needed" boolean property that indicates whether the use
	 * of a JSESSIONID cookie is required for the application to function correctly, for example,
	 * for load balancing or in Java Servlet containers for the use of an HTTP session.
	 *
	 * <p>This is especially important for IE 8,9 that support XDomainRequest -- a modified
	 * AJAX/XHR -- that can do requests across domains but does not send any cookies. In
	 * those cases, the SockJS client prefers the "iframe-htmlfile" transport over
	 * "xdr-streaming" in order to be able to send cookies.
	 *
	 * <p>The default value is "true" to maximize the chance for applications to work
	 * correctly in IE 8,9 with support for cookies (and the JSESSIONID cookie in
	 * particular). However, an application can choose to set this to "false" if the use
	 * of cookies (and HTTP session) is not required.
	 */
	public SockJsServiceRegistration setSessionCookieNeeded(boolean sessionCookieNeeded) {
		this.sessionCookieNeeded = sessionCookieNeeded;
		return this;
	}

	/**
	 * The amount of time in milliseconds when the server has not sent any
	 * messages and after which the server should send a heartbeat frame to the
	 * client in order to keep the connection from breaking.
	 * <p>The default value is 25,000 (25 seconds).
	 */
	public SockJsServiceRegistration setHeartbeatTime(long heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
		return this;
	}

	/**
	 * The amount of time in milliseconds before a client is considered
	 * disconnected after not having a receiving connection, i.e. an active
	 * connection over which the server can send data to the client.
	 * <p>The default value is 5000.
	 */
	public SockJsServiceRegistration setDisconnectDelay(long disconnectDelay) {
		this.disconnectDelay = disconnectDelay;
		return this;
	}

	/**
	 * The number of server-to-client messages that a session can cache while waiting for
	 * the next HTTP polling request from the client. All HTTP transports use this
	 * property since even streaming transports recycle HTTP requests periodically.
	 * <p>The amount of time between HTTP requests should be relatively brief and will not
	 * exceed the allows disconnect delay (see
	 * {@link #setDisconnectDelay(long)}), 5 seconds by default.
	 * <p>The default size is 100.
	 */
	public SockJsServiceRegistration setHttpMessageCacheSize(int httpMessageCacheSize) {
		this.httpMessageCacheSize = httpMessageCacheSize;
		return this;
	}

	/**
	 * Some load balancers don't support WebSocket. This option can be used to
	 * disable the WebSocket transport on the server side.
	 * <p>The default value is "true".
	 */
	public SockJsServiceRegistration setWebSocketEnabled(boolean webSocketEnabled) {
		this.webSocketEnabled = webSocketEnabled;
		return this;
	}

	public SockJsServiceRegistration setTransportHandlers(TransportHandler... handlers) {
		this.transportHandlers.clear();
		if (!ObjectUtils.isEmpty(handlers)) {
			this.transportHandlers.addAll(Arrays.asList(handlers));
		}
		return this;
	}

	public SockJsServiceRegistration setTransportHandlerOverrides(TransportHandler... handlers) {
		this.transportHandlerOverrides.clear();
		if (!ObjectUtils.isEmpty(handlers)) {
			this.transportHandlerOverrides.addAll(Arrays.asList(handlers));
		}
		return this;
	}

	public SockJsServiceRegistration setInterceptors(HandshakeInterceptor... interceptors) {
		this.interceptors.clear();
		if (!ObjectUtils.isEmpty(interceptors)) {
			this.interceptors.addAll(Arrays.asList(interceptors));
		}
		return this;
	}

	/**
	 * Configure allowed {@code Origin} header values.
	 * @since 4.1.2
	 */
	protected SockJsServiceRegistration setAllowedOrigins(String... allowedOrigins) {
		this.allowedOrigins.clear();
		if (!ObjectUtils.isEmpty(allowedOrigins)) {
			this.allowedOrigins.addAll(Arrays.asList(allowedOrigins));
		}
		return this;
	}

	/**
	 * Configure allowed {@code Origin} pattern header values.
	 * @since 5.3.2
	 */
	protected SockJsServiceRegistration setAllowedOriginPatterns(String... allowedOriginPatterns) {
		this.allowedOriginPatterns.clear();
		if (!ObjectUtils.isEmpty(allowedOriginPatterns)) {
			this.allowedOriginPatterns.addAll(Arrays.asList(allowedOriginPatterns));
		}
		return this;
	}

	/**
	 * This option can be used to disable automatic addition of CORS headers for
	 * SockJS requests.
	 * <p>The default value is "false".
	 * @since 5.3.23
	 */
	public SockJsServiceRegistration setSuppressCors(boolean suppressCors) {
		this.suppressCors = suppressCors;
		return this;
	}

	/**
	 * The codec to use for encoding and decoding SockJS messages.
	 * <p>By default {@code Jackson2SockJsMessageCodec} is used requiring the
	 * Jackson library to be present on the classpath.
	 * @param codec the codec to use.
	 * @since 4.1
	 */
	public SockJsServiceRegistration setMessageCodec(SockJsMessageCodec codec) {
		this.messageCodec = codec;
		return this;
	}

	protected SockJsService getSockJsService() {
		TransportHandlingSockJsService service = createSockJsService();
		service.setHandshakeInterceptors(this.interceptors);

		if (this.clientLibraryUrl != null) {
			service.setSockJsClientLibraryUrl(this.clientLibraryUrl);
		}
		if (this.streamBytesLimit != null) {
			service.setStreamBytesLimit(this.streamBytesLimit);
		}
		if (this.sessionCookieNeeded != null) {
			service.setSessionCookieNeeded(this.sessionCookieNeeded);
		}
		if (this.heartbeatTime != null) {
			service.setHeartbeatTime(this.heartbeatTime);
		}
		if (this.disconnectDelay != null) {
			service.setDisconnectDelay(this.disconnectDelay);
		}
		if (this.httpMessageCacheSize != null) {
			service.setHttpMessageCacheSize(this.httpMessageCacheSize);
		}
		if (this.webSocketEnabled != null) {
			service.setWebSocketEnabled(this.webSocketEnabled);
		}
		if (this.suppressCors != null) {
			service.setSuppressCors(this.suppressCors);
		}
		service.setAllowedOrigins(this.allowedOrigins);
		service.setAllowedOriginPatterns(this.allowedOriginPatterns);

		if (this.messageCodec != null) {
			service.setMessageCodec(this.messageCodec);
		}
		return service;
	}

	/**
	 * Return the TaskScheduler, if configured.
	 */
	protected @Nullable TaskScheduler getTaskScheduler() {
		return this.scheduler;
	}

	private TransportHandlingSockJsService createSockJsService() {
		Assert.state(this.scheduler != null, "No TaskScheduler available");
		Assert.state(this.transportHandlers.isEmpty() || this.transportHandlerOverrides.isEmpty(),
				"Specify either TransportHandlers or TransportHandler overrides, not both");
		return (!this.transportHandlers.isEmpty() ?
				new TransportHandlingSockJsService(this.scheduler, this.transportHandlers) :
				new DefaultSockJsService(this.scheduler, this.transportHandlerOverrides));
	}

}
