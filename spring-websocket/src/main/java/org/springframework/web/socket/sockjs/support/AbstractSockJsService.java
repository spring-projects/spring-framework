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

package org.springframework.web.socket.sockjs.support;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsService;

/**
 * An abstract base class for {@link SockJsService} implementations that provides SockJS
 * path resolution and handling of static SockJS requests (e.g. "/info", "/iframe.html",
 * etc). Sub-classes must handle session URLs (i.e. transport-specific requests).
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractSockJsService implements SockJsService {

	private static final int MAX_KNOWN_SOCKJS_PREFIX_COUNT = 100;

	protected final Log logger = LogFactory.getLog(getClass());

	private static final long ONE_YEAR = TimeUnit.DAYS.toSeconds(365);


	private String name = "SockJSService@" + ObjectUtils.getIdentityHexString(this);

	private String clientLibraryUrl = "https://d1fxtkz8shb9d2.cloudfront.net/sockjs-0.3.4.min.js";

	private int streamBytesLimit = 128 * 1024;

	private boolean sessionCookieNeeded = true;

	private long heartbeatTime = 25 * 1000;

	private long disconnectDelay = 5 * 1000;

	private int httpMessageCacheSize = 100;

	private boolean webSocketsEnabled = true;

	private final TaskScheduler taskScheduler;


	public AbstractSockJsService(TaskScheduler scheduler) {
		Assert.notNull(scheduler, "scheduler must not be null");
		this.taskScheduler = scheduler;
	}


	/**
	 * A unique name for the service mainly for logging purposes.
	 */
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
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
	public void setSockJsClientLibraryUrl(String clientLibraryUrl) {
		this.clientLibraryUrl = clientLibraryUrl;
	}

	/**
	 * The URL to the SockJS JavaScript client library.
	 * @see #setSockJsClientLibraryUrl(String)
	 */
	public String getSockJsClientLibraryUrl() {
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
	public void setStreamBytesLimit(int streamBytesLimit) {
		this.streamBytesLimit = streamBytesLimit;
	}

	public int getStreamBytesLimit() {
		return this.streamBytesLimit;
	}

	/**
	 * The SockJS protocol requires a server to respond to an initial "/info" request from
	 * clients with a "cookie_needed" boolean property that indicates whether the use of a
	 * JSESSIONID cookie is required for the application to function correctly, e.g. for
	 * load balancing or in Java Servlet containers for the use of an HTTP session.
	 * <p>
	 * This is especially important for IE 8,9 that support XDomainRequest -- a modified
	 * AJAX/XHR -- that can do requests across domains but does not send any cookies. In
	 * those cases, the SockJS client prefers the "iframe-htmlfile" transport over
	 * "xdr-streaming" in order to be able to send cookies.
	 * <p>
	 * The SockJS protocol also expects a SockJS service to echo back the JSESSIONID
	 * cookie when this property is set to true. However, when running in a Servlet
	 * container this is not necessary since the container takes care of it.
	 * <p>
	 * The default value is "true" to maximize the chance for applications to work
	 * correctly in IE 8,9 with support for cookies (and the JSESSIONID cookie in
	 * particular). However, an application can choose to set this to "false" if
	 * the use of cookies (and HTTP session) is not required.
	 */
	public void setSessionCookieNeeded(boolean sessionCookieNeeded) {
		this.sessionCookieNeeded = sessionCookieNeeded;
	}

	/**
	 * Whether JSESSIONID cookie is required for the application to function. For
	 * more detail see {@link #setSessionCookieNeeded(boolean)}.
	 */
	public boolean isSessionCookieNeeded() {
		return this.sessionCookieNeeded;
	}

	/**
	 * The amount of time in milliseconds when the server has not sent any
	 * messages and after which the server should send a heartbeat frame to the
	 * client in order to keep the connection from breaking.
	 *
	 * <p>The default value is 25,000 (25 seconds).
	 */
	public void setHeartbeatTime(long heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
	}

	public long getHeartbeatTime() {
		return this.heartbeatTime;
	}

	/**
	 * A scheduler instance to use for scheduling heart-beat messages.
	 */
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * The amount of time in milliseconds before a client is considered
	 * disconnected after not having a receiving connection, i.e. an active
	 * connection over which the server can send data to the client.
	 *
	 * <p>The default value is 5000.
	 */
	public void setDisconnectDelay(long disconnectDelay) {
		this.disconnectDelay = disconnectDelay;
	}

	/**
	 * Return the amount of time in milliseconds before a client is considered disconnected.
	 */
	public long getDisconnectDelay() {
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
	public void setHttpMessageCacheSize(int httpMessageCacheSize) {
		this.httpMessageCacheSize = httpMessageCacheSize;
	}

	/**
	 * Return the size of the HTTP message cache.
	 */
	public int getHttpMessageCacheSize() {
		return this.httpMessageCacheSize;
	}

	/**
	 * Some load balancers don't support websockets. This option can be used to
	 * disable the WebSocket transport on the server side.
	 *
	 * <p>The default value is "true".
	 */
	public void setWebSocketsEnabled(boolean webSocketsEnabled) {
		this.webSocketsEnabled = webSocketsEnabled;
	}

	/**
	 * Whether WebSocket transport is enabled.
	 * @see #setWebSocketsEnabled(boolean)
	 */
	public boolean isWebSocketEnabled() {
		return this.webSocketsEnabled;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method determines the SockJS path and handles SockJS static URLs. Session URLs
	 * and raw WebSocket requests are delegated to abstract methods.
	 */
	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			String sockJsPath, WebSocketHandler wsHandler) throws SockJsException {

		if (sockJsPath == null) {
			logger.warn("No SockJS path provided, URI=\"" + request.getURI());
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		logger.debug(request.getMethod() + " with SockJS path [" + sockJsPath + "]");

		try {
			request.getHeaders();
		}
		catch (InvalidMediaTypeException ex) {
			logger.warn("Invalid media type ignored: " + ex.getMediaType());
		}

		try {
			if (sockJsPath.equals("") || sockJsPath.equals("/")) {
				response.getHeaders().setContentType(new MediaType("text", "plain", Charset.forName("UTF-8")));
				response.getBody().write("Welcome to SockJS!\n".getBytes("UTF-8"));
			}
			else if (sockJsPath.equals("/info")) {
				this.infoHandler.handle(request, response);
			}
			else if (sockJsPath.matches("/iframe[0-9-.a-z_]*.html")) {
				this.iframeHandler.handle(request, response);
			}
			else if (sockJsPath.equals("/websocket")) {
				handleRawWebSocketRequest(request, response, wsHandler);
			}
			else {
				String[] pathSegments = StringUtils.tokenizeToStringArray(sockJsPath.substring(1), "/");
				if (pathSegments.length != 3) {
					logger.warn("Expected \"/{server}/{session}/{transport}\" but got \"" + sockJsPath + "\"");
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
				handleTransportRequest(request, response, wsHandler, sessionId, transport);
			}
			response.close();
		}
		catch (IOException ex) {
			throw new SockJsException("Failed to write to the response", null, ex);
		}
	}

	/**
	 * Validate whether the given transport String extracted from the URL is a valid
	 * SockJS transport type (regardless of whether a transport handler is configured).
	 */
	protected abstract boolean isValidTransportType(String transportType);

	/**
	 * Handle request for raw WebSocket communication, i.e. without any SockJS message framing.
	 */
	protected abstract void handleRawWebSocketRequest(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler webSocketHandler) throws IOException;

	/**
	 * Handle a SockJS session URL (i.e. transport-specific request).
	 */
	protected abstract void handleTransportRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler webSocketHandler, String sessionId, String transport) throws SockJsException;


	protected boolean validateRequest(String serverId, String sessionId, String transport) {

		if (!StringUtils.hasText(serverId) || !StringUtils.hasText(sessionId) || !StringUtils.hasText(transport)) {
			logger.warn("Empty server, session, or transport value");
			return false;
		}

		// Server and session id's must not contain "."
		if (serverId.contains(".") || sessionId.contains(".")) {
			logger.warn("Server or session contain a \".\"");
			return false;
		}

		if (!isWebSocketEnabled() && transport.equals("websocket")) {
			logger.warn("Websocket transport is disabled");
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

	protected void sendMethodNotAllowed(ServerHttpResponse response, List<HttpMethod> httpMethods) {
		logger.debug("Sending Method Not Allowed (405)");
		response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
		response.getHeaders().setAllow(new HashSet<HttpMethod>(httpMethods));
	}


	private interface SockJsRequestHandler {

		void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException;
	}


	private static final Random random = new Random();

	private final SockJsRequestHandler infoHandler = new SockJsRequestHandler() {

		private static final String INFO_CONTENT =
				"{\"entropy\":%s,\"origins\":[\"*:*\"],\"cookie_needed\":%s,\"websocket\":%s}";

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {

			if (HttpMethod.GET.equals(request.getMethod())) {

				response.getHeaders().setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));

				addCorsHeaders(request, response);
				addNoCacheHeaders(response);

				String content = String.format(INFO_CONTENT, random.nextInt(), isSessionCookieNeeded(), isWebSocketEnabled());
				response.getBody().write(content.getBytes());
			}
			else if (HttpMethod.OPTIONS.equals(request.getMethod())) {

				response.setStatusCode(HttpStatus.NO_CONTENT);

				addCorsHeaders(request, response, HttpMethod.OPTIONS, HttpMethod.GET);
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

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {

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
