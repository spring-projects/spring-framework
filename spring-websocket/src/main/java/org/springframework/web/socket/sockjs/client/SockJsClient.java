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

package org.springframework.web.socket.sockjs.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.AbstractWebSocketClient;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.transport.TransportType;

import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A SockJS implementation of
 * {@link org.springframework.web.socket.client.WebSocketClient WebSocketClient}
 * with HTTP-based fallback alternative simulating a WebSocket interaction.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 *
 * @see <a href="http://sockjs.org">http://sockjs.org</a>
 * @see org.springframework.web.socket.sockjs.client.Transport
 */
public class SockJsClient extends AbstractWebSocketClient {

	private static final boolean jackson2Present = ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", SockJsClient.class.getClassLoader());

	private static final Log logger = LogFactory.getLog(SockJsClient.class);


	private final List<Transport> transports;

	private InfoReceiver infoReceiver;

	private SockJsMessageCodec messageCodec;

	private TaskScheduler taskScheduler;

	private final Map<URI, ServerInfo> infoCache = new ConcurrentHashMap<URI, ServerInfo>();


	/**
	 * Create a {@code SockJsClient} with the given transports.
	 * @param transports the transports to use
	 */
	public SockJsClient(List<Transport> transports) {
		Assert.notEmpty(transports, "No transports provided");
		this.transports = new ArrayList<Transport>(transports);
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
	 * Configure the {@code InfoReceiver} to use to perform the SockJS "Info"
	 * request before the SockJS session starts.
	 *
	 * <p>By default this is initialized either by looking through the configured
	 * transports to find the first {@code XhrTransport} or by creating an
	 * instance of {@code RestTemplateXhrTransport}.
	 *
	 * @param infoReceiver the transport to use for the SockJS "Info" request
	 */
	public void setInfoReceiver(InfoReceiver infoReceiver) {
		this.infoReceiver = infoReceiver;
	}

	public InfoReceiver getInfoReceiver() {
		return this.infoReceiver;
	}

	/**
	 * Set the SockJsMessageCodec to use.
	 *
	 * <p>By default {@link org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec
	 * Jackson2SockJsMessageCodec} is used if Jackson is on the classpath.
	 *
	 * @param messageCodec the message messageCodec to use
	 */
	public void setMessageCodec(SockJsMessageCodec messageCodec) {
		Assert.notNull(messageCodec, "'messageCodec' is required");
		this.messageCodec = messageCodec;
	}

	public SockJsMessageCodec getMessageCodec() {
		return this.messageCodec;
	}

	/**
	 * Configure a {@code TaskScheduler} for scheduling a connect timeout task
	 * where the timeout value is calculated based on the duration of the initial
	 * SockJS info request. Having a connect timeout task is optional but can
	 * improve the speed with which the client falls back to alternative
	 * transport options.
	 *
	 * <p>By default no task scheduler is configured in which case it may take
	 * longer before a fallback transport can be used.
	 *
	 * @param taskScheduler the scheduler to use
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	public void clearServerInfoCache() {
		this.infoCache.clear();
	}

	@Override
	protected void assertUri(URI uri) {
		Assert.notNull(uri, "uri must not be null");
		String scheme = uri.getScheme();
		Assert.isTrue(scheme != null && ("ws".equals(scheme) || "wss".equals(scheme)
				|| "http".equals(scheme) || "https".equals(scheme)), "Invalid scheme: " + scheme);
	}

	@Override
	protected ListenableFuture<WebSocketSession> doHandshakeInternal(WebSocketHandler handler,
			HttpHeaders handshakeHeaders, URI url, List<String> protocols,
			List<WebSocketExtension> extensions, Map<String, Object> attributes) {

		SettableListenableFuture<WebSocketSession> connectFuture = new SettableListenableFuture<WebSocketSession>();
		try {
			SockJsUrlInfo sockJsUrlInfo = new SockJsUrlInfo(url);
			ServerInfo serverInfo = getServerInfo(sockJsUrlInfo);
			createFallbackChain(sockJsUrlInfo, handshakeHeaders, serverInfo).connect(handler, connectFuture);
		}
		catch (Throwable exception) {
			if (logger.isErrorEnabled()) {
				logger.error("Initial SockJS \"Info\" request to server failed, url=" + url, exception);
			}
			connectFuture.setException(exception);
		}
		return connectFuture;
	}

	private ServerInfo getServerInfo(SockJsUrlInfo sockJsUrlInfo) {
		URI infoUrl = sockJsUrlInfo.getInfoUrl();
		ServerInfo info = this.infoCache.get(infoUrl);
		if (info == null) {
			long start = System.currentTimeMillis();
			String response = this.infoReceiver.executeInfoRequest(infoUrl);
			long infoRequestTime = System.currentTimeMillis() - start;
			info = new ServerInfo(response, infoRequestTime);
			this.infoCache.put(infoUrl, info);
		}
		return info;
	}

	private DefaultTransportRequest createFallbackChain(SockJsUrlInfo urlInfo, HttpHeaders headers, ServerInfo serverInfo) {
		List<DefaultTransportRequest> requests = new ArrayList<DefaultTransportRequest>(this.transports.size());
		for (Transport transport : this.transports) {
			if (transport instanceof XhrTransport) {
				XhrTransport xhrTransport = (XhrTransport) transport;
				if (!xhrTransport.isXhrStreamingDisabled()) {
					addRequest(requests, urlInfo, headers, serverInfo, transport, TransportType.XHR_STREAMING);
				}
				addRequest(requests, urlInfo, headers, serverInfo, transport, TransportType.XHR);
			}
			else if (serverInfo.isWebSocketEnabled()) {
				addRequest(requests, urlInfo, headers, serverInfo, transport, TransportType.WEBSOCKET);
			}
		}
		Assert.notEmpty(requests,
				"0 transports for request to " + urlInfo + " . Configured transports: " +
						this.transports + ". SockJS server webSocketEnabled=" + serverInfo.isWebSocketEnabled());
		for (int i = 0; i < requests.size() - 1; i++) {
			requests.get(i).setFallbackRequest(requests.get(i + 1));
		}
		return requests.get(0);
	}

	private void addRequest(List<DefaultTransportRequest> requests, SockJsUrlInfo info, HttpHeaders headers,
			ServerInfo serverInfo, Transport transport, TransportType type) {

		DefaultTransportRequest request = new DefaultTransportRequest(info, headers, transport, type, getMessageCodec());
		request.setUser(getUser());
		if (this.taskScheduler != null) {
			request.setTimeoutValue(serverInfo.getRetransmissionTimeout());
			request.setTimeoutScheduler(this.taskScheduler);
		}
		requests.add(request);
	}

	/**
	 * Return the user to associate with the SockJS session and make available via
	 * {@link org.springframework.web.socket.WebSocketSession#getPrincipal()
	 * WebSocketSession#getPrincipal()}.
	 * <p>By default this method returns {@code null}.
	 * @return the user to associate with the session, possibly {@code null}
	 */
	protected Principal getUser() {
		return null;
	}


	private static class ServerInfo {

		private final boolean webSocketEnabled;

		private final long responseTime;


		private ServerInfo(String response, long responseTime) {
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