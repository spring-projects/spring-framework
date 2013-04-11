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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;


/**
 * Provides support for SockJS configuration options and serves the static SockJS URLs.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractSockJsService implements SockJsConfiguration {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final int ONE_YEAR = 365 * 24 * 60 * 60;


	private final String prefix;

	private String clientLibraryUrl = "https://d1fxtkz8shb9d2.cloudfront.net/sockjs-0.3.4.min.js";

	private int streamBytesLimit = 128 * 1024;

	private boolean jsessionIdCookieNeeded = true;

	private long heartbeatTime = 25 * 1000;

	private TaskScheduler heartbeatScheduler;

	private long disconnectDelay = 5 * 1000;

	private boolean webSocketsEnabled = true;


	/**
	 * Class constructor...
	 *
	 * @param prefix the path prefix for the SockJS service. All requests with a path
	 * that begins with the specified prefix will be handled by this service. In a
	 * Servlet container this is the path within the current servlet mapping.
	 */
	public AbstractSockJsService(String prefix) {
		Assert.hasText(prefix, "prefix is required");
		this.prefix = prefix;
		this.heartbeatScheduler = createScheduler("SockJs-heartbeat-");
	}

	protected TaskScheduler createScheduler(String threadNamePrefix) {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setThreadNamePrefix(threadNamePrefix);
		scheduler.afterPropertiesSet();
		return scheduler;
	}

	/**
	 * The path prefix to which the SockJS service is mapped.
	 */
	public String getPrefix() {
		return this.prefix;
	}

	/**
	 * Transports which don't support cross-domain communication natively (e.g.
	 * "eventsource", "htmlfile") rely on serving a simple page (using the
	 * "foreign" domain) from an invisible iframe. Code run from this iframe
	 * doesn't need to worry about cross-domain issues since it is running from
	 * a domain local to the SockJS server. The iframe does need to load the
	 * SockJS javascript client library and this option allows configuring its
	 * url.
	 * <p>
	 * By default this is set to point to
	 * "https://d1fxtkz8shb9d2.cloudfront.net/sockjs-0.3.4.min.js".
	 */
	public AbstractSockJsService setSockJsClientLibraryUrl(String clientLibraryUrl) {
		this.clientLibraryUrl = clientLibraryUrl;
		return this;
	}

	/**
	 * The URL to the SockJS JavaScript client library.
	 * @see #setSockJsClientLibraryUrl(String)
	 */
	public String getSockJsClientLibraryUrl() {
		return this.clientLibraryUrl;
	}

	public AbstractSockJsService setStreamBytesLimit(int streamBytesLimit) {
		this.streamBytesLimit = streamBytesLimit;
		return this;
	}

	public int getStreamBytesLimit() {
		return streamBytesLimit;
	}

	/**
	 * Some load balancers do sticky sessions, but only if there is a JSESSIONID
	 * cookie. Even if it is set to a dummy value, it doesn't matter since
	 * session information is added by the load balancer.
	 * <p>
	 * Set this option to indicate if a JSESSIONID cookie should be created. The
	 * default value is "true".
	 */
	public AbstractSockJsService setJsessionIdCookieNeeded(boolean jsessionIdCookieNeeded) {
		this.jsessionIdCookieNeeded = jsessionIdCookieNeeded;
		return this;
	}

	/**
	 * Whether setting JSESSIONID cookie is necessary.
	 * @see #setJsessionIdCookieNeeded(boolean)
	 */
	public boolean isJsessionIdCookieNeeded() {
		return this.jsessionIdCookieNeeded;
	}

	public AbstractSockJsService setHeartbeatTime(long heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
		return this;
	}

	public long getHeartbeatTime() {
		return this.heartbeatTime;
	}

	public TaskScheduler getHeartbeatScheduler() {
		return this.heartbeatScheduler;
	}

	public void setHeartbeatScheduler(TaskScheduler heartbeatScheduler) {
		Assert.notNull(heartbeatScheduler, "heartbeatScheduler is required");
		this.heartbeatScheduler = heartbeatScheduler;
	}

	public void setDisconnectDelay(long disconnectDelay) {
		this.disconnectDelay = disconnectDelay;
	}

	public long getDisconnectDelay() {
		return this.disconnectDelay;
	}

	/**
	 * Some load balancers don't support websockets. This option can be used to
	 * disable the WebSocket transport on the server side.
	 * <p>
	 * The default value is "true".
	 */
	public void setWebSocketsEnabled(boolean webSocketsEnabled) {
		this.webSocketsEnabled = webSocketsEnabled;
	}

	/**
	 * Whether WebSocket transport is enabled.
	 * @see #setWebSocketsEnabled(boolean)
	 */
	public boolean isWebSocketsEnabled() {
		return this.webSocketsEnabled;
	}


	/**
	 * TODO
	 *
	 * @param request
	 * @param response
	 * @param sockJsPath
	 *
	 * @throws Exception
	 */
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response, String sockJsPath)
			throws Exception {

		logger.debug(request.getMethod() + " [" + sockJsPath + "]");

		try {
			request.getHeaders();
		}
		catch (IllegalArgumentException ex) {
			// Ignore invalid Content-Type (TODO)
		}

		try {
			if (sockJsPath.equals("") || sockJsPath.equals("/")) {
				response.getHeaders().setContentType(new MediaType("text", "plain", Charset.forName("UTF-8")));
				response.getBody().write("Welcome to SockJS!\n".getBytes("UTF-8"));
				return;
			}
			else if (sockJsPath.equals("/info")) {
				this.infoHandler.handle(request, response);
				return;
			}
			else if (sockJsPath.matches("/iframe[0-9-.a-z_]*.html")) {
				this.iframeHandler.handle(request, response);
				return;
			}
			else if (sockJsPath.equals("/websocket")) {
				handleRawWebSocket(request, response);
				return;
			}

			String[] pathSegments = StringUtils.tokenizeToStringArray(sockJsPath.substring(1), "/");
			if (pathSegments.length != 3) {
				logger.debug("Expected /{server}/{session}/{transport} but got " + sockJsPath);
				response.setStatusCode(HttpStatus.NOT_FOUND);
				return;
			}

			String serverId = pathSegments[0];
			String sessionId = pathSegments[1];
			String transport = pathSegments[2];

			if (!validateRequest(serverId, sessionId, transport)) {
				response.setStatusCode(HttpStatus.NOT_FOUND);
				return;
			}

			handleTransportRequest(request, response, sessionId, TransportType.fromValue(transport));
		}
		finally {
			response.flush();
		}
	}

	protected abstract void handleRawWebSocket(ServerHttpRequest request, ServerHttpResponse response)
			throws Exception;

	protected abstract void handleTransportRequest(ServerHttpRequest request, ServerHttpResponse response,
			String sessionId, TransportType transportType) throws Exception;

	protected boolean validateRequest(String serverId, String sessionId, String transport) {

		if (!StringUtils.hasText(serverId) || !StringUtils.hasText(sessionId) || !StringUtils.hasText(transport)) {
			logger.debug("Empty server, session, or transport value");
			return false;
		}

		// Server and session id's must not contain "."
		if (serverId.contains(".") || sessionId.contains(".")) {
			logger.debug("Server or session contain a \".\"");
			return false;
		}

		if (!isWebSocketsEnabled() && transport.equals(TransportType.WEBSOCKET.value())) {
			logger.debug("Websocket transport is disabled");
			return false;
		}

		return true;
	}

	protected void addCorsHeaders(ServerHttpRequest request, ServerHttpResponse response, HttpMethod... httpMethods) {

		String origin = request.getHeaders().getFirst("origin");
		origin = ((origin == null) || origin.equals("null")) ? "*" : origin;

		response.getHeaders().add("Access-Control-Allow-Origin", origin);
		response.getHeaders().add("Access-Control-Allow-Credentials", "true");

		List<String> accessControllerHeaders = request.getHeaders().get("Access-Control-Request-Headers");
		if (accessControllerHeaders != null) {
			for (String header : accessControllerHeaders) {
				response.getHeaders().add("Access-Control-Allow-Headers", header);
			}
		}

		if (!ObjectUtils.isEmpty(httpMethods)) {
			response.getHeaders().add("Access-Control-Allow-Methods", StringUtils.arrayToDelimitedString(httpMethods, ", "));
			response.getHeaders().add("Access-Control-Max-Age", String.valueOf(ONE_YEAR));
		}
	}

	protected void addCacheHeaders(ServerHttpResponse response) {
		response.getHeaders().setCacheControl("public, max-age=" + ONE_YEAR);
		response.getHeaders().setExpires(new Date().getTime() + ONE_YEAR * 1000);
	}

	protected void addNoCacheHeaders(ServerHttpResponse response) {
		response.getHeaders().setCacheControl("no-store, no-cache, must-revalidate, max-age=0");
	}

	protected void sendMethodNotAllowed(ServerHttpResponse response, List<HttpMethod> httpMethods) throws IOException {
		logger.debug("Sending Method Not Allowed (405)");
		response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
		response.getHeaders().setAllow(new HashSet<HttpMethod>(httpMethods));
	}


	private interface SockJsRequestHandler {

		void handle(ServerHttpRequest request, ServerHttpResponse response) throws Exception;
	}

	private static final Random random = new Random();

	private final SockJsRequestHandler infoHandler = new SockJsRequestHandler() {

		private static final String INFO_CONTENT =
				"{\"entropy\":%s,\"origins\":[\"*:*\"],\"cookie_needed\":%s,\"websocket\":%s}";

		public void handle(ServerHttpRequest request, ServerHttpResponse response) throws Exception {

			if (HttpMethod.GET.equals(request.getMethod())) {

				response.getHeaders().setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

				addCorsHeaders(request, response);
				addNoCacheHeaders(response);

				String content = String.format(INFO_CONTENT, random.nextInt(), isJsessionIdCookieNeeded(), isWebSocketsEnabled());
				response.getBody().write(content.getBytes());
			}
			else if (HttpMethod.OPTIONS.equals(request.getMethod())) {

				response.setStatusCode(HttpStatus.NO_CONTENT);

				addCorsHeaders(request, response, HttpMethod.GET, HttpMethod.OPTIONS);
				addCacheHeaders(response);
			}
			else {
				sendMethodNotAllowed(response, Arrays.asList(HttpMethod.OPTIONS, HttpMethod.GET));
			}
		}
	};

	private final SockJsRequestHandler iframeHandler = new SockJsRequestHandler() {

		private static final String IFRAME_CONTENT =
				"<!DOCTYPE html>\n" +
		        "<html>\n" +
		        "<head>\n" +
		        "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n" +
		        "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
		        "  <script>\n" +
		        "    document.domain = document.domain;\n" +
		        "    _sockjs_onload = function(){SockJS.bootstrap_iframe();};\n" +
		        "  </script>\n" +
		        "  <script src=\"%s\"></script>\n" +
		        "</head>\n" +
		        "<body>\n" +
		        "  <h2>Don't panic!</h2>\n" +
		        "  <p>This is a SockJS hidden iframe. It's used for cross domain magic.</p>\n" +
		        "</body>\n" +
		        "</html>";

		public void handle(ServerHttpRequest request, ServerHttpResponse response) throws Exception {

			if (!HttpMethod.GET.equals(request.getMethod())) {
				sendMethodNotAllowed(response, Arrays.asList(HttpMethod.GET));
				return;
			}

			String content = String.format(IFRAME_CONTENT, getSockJsClientLibraryUrl());
			byte[] contentBytes = content.getBytes(Charset.forName("UTF-8"));
			StringBuilder builder = new StringBuilder("\"0");
			DigestUtils.appendMd5DigestAsHex(contentBytes, builder);
			builder.append('"');
			String etagValue = builder.toString();

			List<String> ifNoneMatch = request.getHeaders().getIfNoneMatch();
			if (!CollectionUtils.isEmpty(ifNoneMatch) && ifNoneMatch.get(0).equals(etagValue)) {
				response.setStatusCode(HttpStatus.NOT_MODIFIED);
				return;
			}

			response.getHeaders().setContentType(new MediaType("text", "html", Charset.forName("UTF-8")));
			response.getHeaders().setContentLength(contentBytes.length);

			addCacheHeaders(response);
			response.getHeaders().setETag(etagValue);
			response.getBody().write(contentBytes);
		}
	};

}
