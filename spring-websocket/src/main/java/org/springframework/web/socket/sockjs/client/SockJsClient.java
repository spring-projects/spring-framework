/*
 * Copyright 2002-2020 the original author or authors.
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
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A SockJS implementation of
 * {@link org.springframework.web.socket.client.WebSocketClient WebSocketClient}
 * with fallback alternatives that simulate a WebSocket interaction through plain
 * HTTP streaming and long polling techniques..
 *
 * <p>Implements {@link Lifecycle} in order to propagate lifecycle events to
 * the transports it is configured with.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 * @see <a href="https://github.com/sockjs/sockjs-client">https://github.com/sockjs/sockjs-client</a>
 * @see org.springframework.web.socket.sockjs.client.Transport
 */
public class SockJsClient implements WebSocketClient, Lifecycle {

	private static final boolean jackson2Present = ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", SockJsClient.class.getClassLoader());

	private static final Log logger = LogFactory.getLog(SockJsClient.class);

	private static final Set<String> supportedProtocols = new HashSet<>(4);

	static {
		supportedProtocols.add("ws");
		supportedProtocols.add("wss");
		supportedProtocols.add("http");
		supportedProtocols.add("https");
	}


	private final List<Transport> transports;

	@Nullable
	private String[] httpHeaderNames;

	private InfoReceiver infoReceiver;

	@Nullable
	private SockJsMessageCodec messageCodec;

	@Nullable
	private TaskScheduler connectTimeoutScheduler;

	private volatile boolean running;

	private final Map<URI, ServerInfo> serverInfoCache = new ConcurrentHashMap<>();


	/**
	 * Create a {@code SockJsClient} with the given transports.
	 * <p>If the list includes an {@link XhrTransport} (or more specifically an
	 * implementation of {@link InfoReceiver}) the instance is used to initialize
	 * the {@link #setInfoReceiver(InfoReceiver) infoReceiver} property, or
	 * otherwise is defaulted to {@link RestTemplateXhrTransport}.
	 * @param transports the (non-empty) list of transports to use
	 */
	public SockJsClient(List<Transport> transports) {
		Assert.notEmpty(transports, "No transports provided");
		this.transports = new ArrayList<>(transports);
		this.infoReceiver = initInfoReceiver(transports);
		if (jackson2Present) {
			this.messageCodec = new Jackson2SockJsMessageCodec();
		}
	}

	private static InfoReceiver initInfoReceiver(List<Transport> transports) {
		for (Transport transport : transports) {
			if (transport instanceof InfoReceiver) {
				return ((InfoReceiver) transport);
			}
		}
		return new RestTemplateXhrTransport();
	}


	/**
	 * The names of HTTP headers that should be copied from the handshake headers
	 * of each call to {@link SockJsClient#doHandshake(WebSocketHandler, WebSocketHttpHeaders, URI)}
	 * and also used with other HTTP requests issued as part of that SockJS
	 * connection, e.g. the initial info request, XHR send or receive requests.
	 * <p>By default if this property is not set, all handshake headers are also
	 * used for other HTTP requests. Set it if you want only a subset of handshake
	 * headers (e.g. auth headers) to be used for other HTTP requests.
	 * @param httpHeaderNames the HTTP header names
	 */
	public void setHttpHeaderNames(@Nullable String... httpHeaderNames) {
		this.httpHeaderNames = httpHeaderNames;
	}

	/**
	 * The configured HTTP header names to be copied from the handshake
	 * headers and also included in other HTTP requests.
	 */
	@Nullable
	public String[] getHttpHeaderNames() {
		return this.httpHeaderNames;
	}

	/**
	 * Configure the {@code InfoReceiver} to use to perform the SockJS "Info"
	 * request before the SockJS session starts.
	 * <p>If the list of transports provided to the constructor contained an
	 * {@link XhrTransport} or an implementation of {@link InfoReceiver} that
	 * instance would have been used to initialize this property, or otherwise
	 * it defaults to {@link RestTemplateXhrTransport}.
	 * @param infoReceiver the transport to use for the SockJS "Info" request
	 */
	public void setInfoReceiver(InfoReceiver infoReceiver) {
		Assert.notNull(infoReceiver, "InfoReceiver is required");
		this.infoReceiver = infoReceiver;
	}

	/**
	 * Return the configured {@code InfoReceiver} (never {@code null}).
	 */
	public InfoReceiver getInfoReceiver() {
		return this.infoReceiver;
	}

	/**
	 * Set the SockJsMessageCodec to use.
	 * <p>By default {@link org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec
	 * Jackson2SockJsMessageCodec} is used if Jackson is on the classpath.
	 */
	public void setMessageCodec(SockJsMessageCodec messageCodec) {
		Assert.notNull(messageCodec, "SockJsMessageCodec is required");
		this.messageCodec = messageCodec;
	}

	/**
	 * Return the SockJsMessageCodec to use.
	 */
	public SockJsMessageCodec getMessageCodec() {
		Assert.state(this.messageCodec != null, "No SockJsMessageCodec set");
		return this.messageCodec;
	}

	/**
	 * Configure a {@code TaskScheduler} for scheduling a connect timeout task
	 * where the timeout value is calculated based on the duration of the initial
	 * SockJS "Info" request. The connect timeout task ensures a more timely
	 * fallback but is otherwise entirely optional.
	 * <p>By default this is not configured in which case a fallback may take longer.
	 * @param connectTimeoutScheduler the task scheduler to use
	 */
	public void setConnectTimeoutScheduler(TaskScheduler connectTimeoutScheduler) {
		this.connectTimeoutScheduler = connectTimeoutScheduler;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			for (Transport transport : this.transports) {
				if (transport instanceof Lifecycle) {
					Lifecycle lifecycle = (Lifecycle) transport;
					if (!lifecycle.isRunning()) {
						lifecycle.start();
					}
				}
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			for (Transport transport : this.transports) {
				if (transport instanceof Lifecycle) {
					Lifecycle lifecycle = (Lifecycle) transport;
					if (lifecycle.isRunning()) {
						lifecycle.stop();
					}
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	public ListenableFuture<WebSocketSession> doHandshake(
			WebSocketHandler handler, String uriTemplate, Object... uriVars) {

		Assert.notNull(uriTemplate, "uriTemplate must not be null");
		URI uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVars).encode().toUri();
		return doHandshake(handler, null, uri);
	}

	@Override
	public final ListenableFuture<WebSocketSession> doHandshake(
			WebSocketHandler handler, @Nullable WebSocketHttpHeaders headers, URI url) {

		Assert.notNull(handler, "WebSocketHandler is required");
		Assert.notNull(url, "URL is required");

		String scheme = url.getScheme();
		if (!supportedProtocols.contains(scheme)) {
			throw new IllegalArgumentException("Invalid scheme: '" + scheme + "'");
		}

		SettableListenableFuture<WebSocketSession> connectFuture = new SettableListenableFuture<>();
		try {
			SockJsUrlInfo sockJsUrlInfo = new SockJsUrlInfo(url);
			ServerInfo serverInfo = getServerInfo(sockJsUrlInfo, getHttpRequestHeaders(headers));
			createRequest(sockJsUrlInfo, headers, serverInfo).connect(handler, connectFuture);
		}
		catch (Exception exception) {
			if (logger.isErrorEnabled()) {
				logger.error("Initial SockJS \"Info\" request to server failed, url=" + url, exception);
			}
			connectFuture.setException(exception);
		}
		return connectFuture;
	}

	@Nullable
	private HttpHeaders getHttpRequestHeaders(@Nullable HttpHeaders webSocketHttpHeaders) {
		if (getHttpHeaderNames() == null || webSocketHttpHeaders == null) {
			return webSocketHttpHeaders;
		}
		else {
			HttpHeaders httpHeaders = new HttpHeaders();
			for (String name : getHttpHeaderNames()) {
				List<String> values = webSocketHttpHeaders.get(name);
				if (values != null) {
					httpHeaders.put(name, values);
				}
			}
			return httpHeaders;
		}
	}

	private ServerInfo getServerInfo(SockJsUrlInfo sockJsUrlInfo, @Nullable HttpHeaders headers) {
		URI infoUrl = sockJsUrlInfo.getInfoUrl();
		ServerInfo info = this.serverInfoCache.get(infoUrl);
		if (info == null) {
			long start = System.currentTimeMillis();
			String response = this.infoReceiver.executeInfoRequest(infoUrl, headers);
			long infoRequestTime = System.currentTimeMillis() - start;
			info = new ServerInfo(response, infoRequestTime);
			this.serverInfoCache.put(infoUrl, info);
		}
		return info;
	}

	private DefaultTransportRequest createRequest(
			SockJsUrlInfo urlInfo, @Nullable HttpHeaders headers, ServerInfo serverInfo) {

		List<DefaultTransportRequest> requests = new ArrayList<>(this.transports.size());
		for (Transport transport : this.transports) {
			for (TransportType type : transport.getTransportTypes()) {
				if (serverInfo.isWebSocketEnabled() || !TransportType.WEBSOCKET.equals(type)) {
					requests.add(new DefaultTransportRequest(urlInfo, headers, getHttpRequestHeaders(headers),
							transport, type, getMessageCodec()));
				}
			}
		}
		if (CollectionUtils.isEmpty(requests)) {
			throw new IllegalStateException(
					"No transports: " + urlInfo + ", webSocketEnabled=" + serverInfo.isWebSocketEnabled());
		}
		for (int i = 0; i < requests.size() - 1; i++) {
			DefaultTransportRequest request = requests.get(i);
			Principal user = getUser();
			if (user != null) {
				request.setUser(user);
			}
			if (this.connectTimeoutScheduler != null) {
				request.setTimeoutValue(serverInfo.getRetransmissionTimeout());
				request.setTimeoutScheduler(this.connectTimeoutScheduler);
			}
			request.setFallbackRequest(requests.get(i + 1));
		}
		return requests.get(0);
	}

	/**
	 * Return the user to associate with the SockJS session and make available via
	 * {@link org.springframework.web.socket.WebSocketSession#getPrincipal()}.
	 * <p>By default this method returns {@code null}.
	 * @return the user to associate with the session (possibly {@code null})
	 */
	@Nullable
	protected Principal getUser() {
		return null;
	}

	/**
	 * By default the result of a SockJS "Info" request, including whether the
	 * server has WebSocket disabled and how long the request took (used for
	 * calculating transport timeout time) is cached. This method can be used to
	 * clear that cache hence causing it to re-populate.
	 */
	public void clearServerInfoCache() {
		this.serverInfoCache.clear();
	}


	/**
	 * A simple value object holding the result from a SockJS "Info" request.
	 */
	private static class ServerInfo {

		private final boolean webSocketEnabled;

		private final long responseTime;

		public ServerInfo(String response, long responseTime) {
			this.responseTime = responseTime;
			this.webSocketEnabled = !response.matches(".*[\"']websocket[\"']\\s*:\\s*false.*");
		}

		public boolean isWebSocketEnabled() {
			return this.webSocketEnabled;
		}

		public long getRetransmissionTimeout() {
			return (this.responseTime > 100 ? 4 * this.responseTime : this.responseTime + 300);
		}
	}

}
