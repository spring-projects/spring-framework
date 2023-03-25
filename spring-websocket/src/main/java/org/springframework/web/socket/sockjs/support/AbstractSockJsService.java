/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.socket.sockjs.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.util.WebUtils;

/**
 * An abstract base class for {@link SockJsService} implementations that provides SockJS
 * path resolution and handling of static SockJS requests (e.g. "/info", "/iframe.html",
 * etc). Sub-classes must handle session URLs (i.e. transport-specific requests).
 *
 * <p>By default, only same origin requests are allowed. Use {@link #setAllowedOrigins}
 * to specify a list of allowed origins (a list containing "*" will allow all origins).
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
public abstract class AbstractSockJsService implements SockJsService, CorsConfigurationSource {

	private static final String XFRAME_OPTIONS_HEADER = "X-Frame-Options";

	private static final long ONE_YEAR = TimeUnit.DAYS.toSeconds(365);


	private static final Random random = new Random();

	protected final Log logger = LogFactory.getLog(getClass());

	private final TaskScheduler taskScheduler;

	private String name = "SockJSService@" + ObjectUtils.getIdentityHexString(this);

	private String clientLibraryUrl = "https://cdn.jsdelivr.net/sockjs/1.0.0/sockjs.min.js";

	private int streamBytesLimit = 128 * 1024;

	private boolean sessionCookieNeeded = true;

	private long heartbeatTime = TimeUnit.SECONDS.toMillis(25);

	private long disconnectDelay = TimeUnit.SECONDS.toMillis(5);

	private int httpMessageCacheSize = 100;

	private boolean webSocketEnabled = true;

	private boolean suppressCors = false;

	protected final CorsConfiguration corsConfiguration;

	private final SockJsRequestHandler infoHandler = new InfoHandler();

	private final SockJsRequestHandler iframeHandler = new IframeHandler();


	public AbstractSockJsService(TaskScheduler scheduler) {
		Assert.notNull(scheduler, "TaskScheduler must not be null");
		this.taskScheduler = scheduler;
		this.corsConfiguration = initCorsConfiguration();
	}

	private static CorsConfiguration initCorsConfiguration() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedMethod("*");
		config.setAllowedOrigins(Collections.emptyList());
		config.setAllowedOriginPatterns(Collections.emptyList());
		config.setAllowCredentials(true);
		config.setMaxAge(ONE_YEAR);
		config.addAllowedHeader("*");
		return config;
	}


	/**
	 * A scheduler instance to use for scheduling heart-beat messages.
	 */
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * Set a unique name for this service (mainly for logging purposes).
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the unique name associated with this service.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Transports with no native cross-domain communication (e.g. "eventsource",
	 * "htmlfile") must get a simple page from the "foreign" domain in an invisible
	 * iframe so that code in the iframe can run from  a domain local to the SockJS
	 * server. Since the iframe needs to load the SockJS javascript client library,
	 * this property allows specifying where to load it from.
	 * <p>By default this is set to point to
	 * "https://cdn.jsdelivr.net/sockjs/1.0.0/sockjs.min.js".
	 * However, it can also be set to point to a URL served by the application.
	 * <p>Note that it's possible to specify a relative URL in which case the URL
	 * must be relative to the iframe URL. For example assuming a SockJS endpoint
	 * mapped to "/sockjs", and resulting iframe URL "/sockjs/iframe.html", then
	 * the relative URL must start with "../../" to traverse up to the location
	 * above the SockJS mapping. In case of a prefix-based Servlet mapping one more
	 * traversal may be needed.
	 */
	public void setSockJsClientLibraryUrl(String clientLibraryUrl) {
		this.clientLibraryUrl = clientLibraryUrl;
	}

	/**
	 * Return he URL to the SockJS JavaScript client library.
	 */
	public String getSockJsClientLibraryUrl() {
		return this.clientLibraryUrl;
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
	public void setStreamBytesLimit(int streamBytesLimit) {
		this.streamBytesLimit = streamBytesLimit;
	}

	/**
	 * Return the minimum number of bytes that can be sent over a single HTTP
	 * streaming request before it will be closed.
	 */
	public int getStreamBytesLimit() {
		return this.streamBytesLimit;
	}

	/**
	 * The SockJS protocol requires a server to respond to an initial "/info" request from
	 * clients with a "cookie_needed" boolean property that indicates whether the use of a
	 * JSESSIONID cookie is required for the application to function correctly, e.g. for
	 * load balancing or in Java Servlet containers for the use of an HTTP session.
	 * <p>This is especially important for IE 8,9 that support XDomainRequest -- a modified
	 * AJAX/XHR -- that can do requests across domains but does not send any cookies. In
	 * those cases, the SockJS client prefers the "iframe-htmlfile" transport over
	 * "xdr-streaming" in order to be able to send cookies.
	 * <p>The SockJS protocol also expects a SockJS service to echo back the JSESSIONID
	 * cookie when this property is set to true. However, when running in a Servlet
	 * container this is not necessary since the container takes care of it.
	 * <p>The default value is "true" to maximize the chance for applications to work
	 * correctly in IE 8,9 with support for cookies (and the JSESSIONID cookie in
	 * particular). However, an application can choose to set this to "false" if
	 * the use of cookies (and HTTP session) is not required.
	 */
	public void setSessionCookieNeeded(boolean sessionCookieNeeded) {
		this.sessionCookieNeeded = sessionCookieNeeded;
	}

	/**
	 * Return whether the JSESSIONID cookie is required for the application to function.
	 */
	public boolean isSessionCookieNeeded() {
		return this.sessionCookieNeeded;
	}

	/**
	 * Specify the amount of time in milliseconds when the server has not sent
	 * any messages and after which the server should send a heartbeat frame
	 * to the client in order to keep the connection from breaking.
	 * <p>The default value is 25,000 (25 seconds).
	 */
	public void setHeartbeatTime(long heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
	}

	/**
	 * Return the amount of time in milliseconds when the server has not sent
	 * any messages.
	 */
	public long getHeartbeatTime() {
		return this.heartbeatTime;
	}

	/**
	 * The amount of time in milliseconds before a client is considered
	 * disconnected after not having a receiving connection, i.e. an active
	 * connection over which the server can send data to the client.
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
	 * The number of server-to-client messages that a session can cache while waiting
	 * for the next HTTP polling request from the client. All HTTP transports use this
	 * property since even streaming transports recycle HTTP requests periodically.
	 * <p>The amount of time between HTTP requests should be relatively brief and will
	 * not exceed the allows disconnect delay (see {@link #setDisconnectDelay(long)});
	 * 5 seconds by default.
	 * <p>The default size is 100.
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
	 * Some load balancers do not support WebSocket. This option can be used to
	 * disable the WebSocket transport on the server side.
	 * <p>The default value is "true".
	 */
	public void setWebSocketEnabled(boolean webSocketEnabled) {
		this.webSocketEnabled = webSocketEnabled;
	}

	/**
	 * Return whether WebSocket transport is enabled.
	 */
	public boolean isWebSocketEnabled() {
		return this.webSocketEnabled;
	}

	/**
	 * This option can be used to disable automatic addition of CORS headers for
	 * SockJS requests.
	 * <p>The default value is "false".
	 * @since 4.1.2
	 */
	public void setSuppressCors(boolean suppressCors) {
		this.suppressCors = suppressCors;
	}

	/**
	 * Return if automatic addition of CORS headers has been disabled.
	 * @since 4.1.2
	 * @see #setSuppressCors
	 */
	public boolean shouldSuppressCors() {
		return this.suppressCors;
	}

	/**
	 * Set the origins for which cross-origin requests are allowed from a browser.
	 * Please, refer to {@link CorsConfiguration#setAllowedOrigins(List)} for
	 * format details and considerations, and keep in mind that the CORS spec
	 * does not allow use of {@code "*"} with {@code allowCredentials=true}.
	 * For more flexible origin patterns use {@link #setAllowedOriginPatterns}
	 * instead.
	 *
	 * <p>By default, no origins are allowed. When
	 * {@link #setAllowedOriginPatterns(Collection) allowedOriginPatterns} is also
	 * set, then that takes precedence over this property.
	 *
	 * <p>Note when SockJS is enabled and origins are restricted, transport types
	 * that do not allow to check request origin (Iframe based transports) are
	 * disabled. As a consequence, IE 6 to 9 are not supported when origins are
	 * restricted.
	 * @since 4.1.2
	 * @see #setAllowedOriginPatterns(Collection)
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 * @see <a href="https://github.com/sockjs/sockjs-client#supported-transports-by-browser-html-served-from-http-or-https">SockJS supported transports by browser</a>
	 */
	public void setAllowedOrigins(Collection<String> allowedOrigins) {
		Assert.notNull(allowedOrigins, "Allowed origins Collection must not be null");
		this.corsConfiguration.setAllowedOrigins(new ArrayList<>(allowedOrigins));
	}

	/**
	 * Return the {@link #setAllowedOrigins(Collection) configured} allowed origins.
	 * @since 4.1.2
	 */
	@SuppressWarnings("ConstantConditions")
	public Collection<String> getAllowedOrigins() {
		return this.corsConfiguration.getAllowedOrigins();
	}
	/**
	 * Alternative to {@link #setAllowedOrigins(Collection)} that supports more
	 * flexible patterns for specifying the origins for which cross-origin
	 * requests are allowed from a browser. Please, refer to
	 * {@link CorsConfiguration#setAllowedOriginPatterns(List)} for format
	 * details and other considerations.
	 * <p>By default this is not set.
	 * @since 5.2.3
	 */
	public void setAllowedOriginPatterns(Collection<String> allowedOriginPatterns) {
		Assert.notNull(allowedOriginPatterns, "Allowed origin patterns Collection must not be null");
		this.corsConfiguration.setAllowedOriginPatterns(new ArrayList<>(allowedOriginPatterns));
	}

	/**
	 * Return {@link #setAllowedOriginPatterns(Collection) configured} origin patterns.
	 * @since 5.3.2
	 */
	@SuppressWarnings("ConstantConditions")
	public Collection<String> getAllowedOriginPatterns() {
		return this.corsConfiguration.getAllowedOriginPatterns();
	}


	/**
	 * This method determines the SockJS path and handles SockJS static URLs.
	 * Session URLs and raw WebSocket requests are delegated to abstract methods.
	 */
	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
			@Nullable String sockJsPath, WebSocketHandler wsHandler) throws SockJsException {

		if (sockJsPath == null) {
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue(
						"Expected SockJS path. Failing request: " + request.getURI(), -1, true));
			}
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		try {
			request.getHeaders();
		}
		catch (InvalidMediaTypeException ex) {
			// As per SockJS protocol content-type can be ignored (it's always json)
		}

		String requestInfo = (logger.isDebugEnabled() ? request.getMethod() + " " + request.getURI() : null);

		try {
			if (sockJsPath.isEmpty() || sockJsPath.equals("/")) {
				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				if ("websocket".equalsIgnoreCase(request.getHeaders().getUpgrade())) {
					response.setStatusCode(HttpStatus.BAD_REQUEST);
					return;
				}
				response.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
				response.getBody().write("Welcome to SockJS!\n".getBytes(StandardCharsets.UTF_8));
			}

			else if (sockJsPath.equals("/info")) {
				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				this.infoHandler.handle(request, response);
			}

			else if (sockJsPath.matches("/iframe[0-9-.a-z_]*.html")) {
				if (!getAllowedOrigins().isEmpty() && !getAllowedOrigins().contains("*") ||
						!getAllowedOriginPatterns().isEmpty()) {
					if (requestInfo != null) {
						logger.debug("Iframe support is disabled when an origin check is required. " +
								"Ignoring transport request: " + requestInfo);
					}
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}
				if (getAllowedOrigins().isEmpty()) {
					response.getHeaders().add(XFRAME_OPTIONS_HEADER, "SAMEORIGIN");
				}
				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				this.iframeHandler.handle(request, response);
			}

			else if (sockJsPath.equals("/websocket")) {
				if (isWebSocketEnabled()) {
					if (requestInfo != null) {
						logger.debug("Processing transport request: " + requestInfo);
					}
					handleRawWebSocketRequest(request, response, wsHandler);
				}
				else if (requestInfo != null) {
					logger.debug("WebSocket disabled. Ignoring transport request: " + requestInfo);
				}
			}

			else {
				String[] pathSegments = StringUtils.tokenizeToStringArray(sockJsPath.substring(1), "/");
				if (pathSegments.length != 3) {
					if (logger.isWarnEnabled()) {
						logger.warn(LogFormatUtils.formatValue("Invalid SockJS path '" + sockJsPath + "' - " +
								"required to have 3 path segments", -1, true));
					}
					if (requestInfo != null) {
						logger.debug("Ignoring transport request: " + requestInfo);
					}
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}

				String serverId = pathSegments[0];
				String sessionId = pathSegments[1];
				String transport = pathSegments[2];

				if (!isWebSocketEnabled() && transport.equals("websocket")) {
					if (requestInfo != null) {
						logger.debug("WebSocket disabled. Ignoring transport request: " + requestInfo);
					}
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}
				else if (!validateRequest(serverId, sessionId, transport) || !validatePath(request)) {
					if (requestInfo != null) {
						logger.debug("Ignoring transport request: " + requestInfo);
					}
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}

				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				handleTransportRequest(request, response, wsHandler, sessionId, transport);
			}
			response.close();
		}
		catch (IOException ex) {
			throw new SockJsException("Failed to write to the response", null, ex);
		}
	}

	protected boolean validateRequest(String serverId, String sessionId, String transport) {
		if (!StringUtils.hasText(serverId) || !StringUtils.hasText(sessionId) || !StringUtils.hasText(transport)) {
			logger.warn("No server, session, or transport path segment in SockJS request.");
			return false;
		}

		// Server and session id's must not contain "."
		if (serverId.contains(".") || sessionId.contains(".")) {
			logger.warn("Either server or session contains a \".\" which is not allowed by SockJS protocol.");
			return false;
		}

		return true;
	}

	/**
	 * Ensure the path does not contain a file extension, either in the filename
	 * (e.g. "/jsonp.bat") or possibly after path parameters ("/jsonp;Setup.bat")
	 * which could be used for RFD exploits.
	 * <p>Since the last part of the path is expected to be a transport type, the
	 * presence of an extension would not work. All we need to do is check if
	 * there are any path parameters, which would have been removed from the
	 * SockJS path during request mapping, and if found reject the request.
	 */
	private boolean validatePath(ServerHttpRequest request) {
		String path = request.getURI().getPath();
		int index = path.lastIndexOf('/') + 1;
		return (path.indexOf(';', index) == -1);
	}

	protected boolean checkOrigin(ServerHttpRequest request, ServerHttpResponse response, HttpMethod... httpMethods)
			throws IOException {

		if (WebUtils.isSameOrigin(request)) {
			return true;
		}

		if (this.corsConfiguration.checkOrigin(request.getHeaders().getOrigin()) == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("Origin header value '" + request.getHeaders().getOrigin() + "' not allowed.");
			}
			response.setStatusCode(HttpStatus.FORBIDDEN);
			return false;
		}

		return true;
	}

	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		if (!this.suppressCors && (request.getHeader(HttpHeaders.ORIGIN) != null)) {
			return this.corsConfiguration;
		}
		return null;
	}

	protected void addCacheHeaders(ServerHttpResponse response) {
		response.getHeaders().setCacheControl("public, max-age=" + ONE_YEAR);
		response.getHeaders().setExpires(System.currentTimeMillis() + ONE_YEAR * 1000);
	}

	protected void addNoCacheHeaders(ServerHttpResponse response) {
		response.getHeaders().setCacheControl("no-store, no-cache, must-revalidate, max-age=0");
	}

	protected void sendMethodNotAllowed(ServerHttpResponse response, HttpMethod... httpMethods) {
		logger.warn("Sending Method Not Allowed (405)");
		response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
		response.getHeaders().setAllow(new LinkedHashSet<>(Arrays.asList(httpMethods)));
	}


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


	private interface SockJsRequestHandler {

		void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException;
	}


	private class InfoHandler implements SockJsRequestHandler {

		private static final String INFO_CONTENT =
				"{\"entropy\":%s,\"origins\":[\"*:*\"],\"cookie_needed\":%s,\"websocket\":%s}";

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
			if (request.getMethod() == HttpMethod.GET) {
				addNoCacheHeaders(response);
				if (checkOrigin(request, response)) {
					response.getHeaders().setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
					String content = String.format(
							INFO_CONTENT, random.nextInt(), isSessionCookieNeeded(), isWebSocketEnabled());
					response.getBody().write(content.getBytes());
				}

			}
			else if (request.getMethod() == HttpMethod.OPTIONS) {
				if (checkOrigin(request, response)) {
					addCacheHeaders(response);
					response.setStatusCode(HttpStatus.NO_CONTENT);
				}
			}
			else {
				sendMethodNotAllowed(response, HttpMethod.GET, HttpMethod.OPTIONS);
			}
		}
	}


	private class IframeHandler implements SockJsRequestHandler {

		private static final String IFRAME_CONTENT =
				"<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n" +
				"  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
				"  <title>SockJS iframe</title>\n" +
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
			if (request.getMethod() != HttpMethod.GET) {
				sendMethodNotAllowed(response, HttpMethod.GET);
				return;
			}

			String content = String.format(IFRAME_CONTENT, getSockJsClientLibraryUrl());
			byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
			StringBuilder builder = new StringBuilder("\"0");
			DigestUtils.appendMd5DigestAsHex(contentBytes, builder);
			builder.append('"');
			String etagValue = builder.toString();

			List<String> ifNoneMatch = request.getHeaders().getIfNoneMatch();
			if (!CollectionUtils.isEmpty(ifNoneMatch) && ifNoneMatch.get(0).equals(etagValue)) {
				response.setStatusCode(HttpStatus.NOT_MODIFIED);
				return;
			}

			response.getHeaders().setContentType(new MediaType("text", "html", StandardCharsets.UTF_8));
			response.getHeaders().setContentLength(contentBytes.length);

			// No cache in order to check every time if IFrame are authorized
			addNoCacheHeaders(response);
			response.getHeaders().setETag(etagValue);
			response.getBody().write(contentBytes);
		}
	}

}
