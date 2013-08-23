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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
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
 * <p>
 * This service is unaware of the underlying HTTP request processing mechanism and URL
 * mappings but nevertheless needs to know the "SockJS path" for a given request, i.e. the
 * portion of the URL path that follows the SockJS prefix. In most cases, this can be
 * auto-detected since the <a href="https://github.com/sockjs/sockjs-client">SockJS
 * client</a> sends a "greeting URL" first. However it is recommended to configure
 * explicitly the expected SockJS prefixes via {@link #setValidSockJsPrefixes(String...)}
 * to eliminate any potential issues.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractSockJsService implements SockJsService {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final long ONE_YEAR = TimeUnit.DAYS.toSeconds(365);


	private String name = "SockJSService@" + ObjectUtils.getIdentityHexString(this);

	private String clientLibraryUrl = "https://d1fxtkz8shb9d2.cloudfront.net/sockjs-0.3.4.min.js";

	private int streamBytesLimit = 128 * 1024;

	private boolean sessionCookieEnabled = false;

	private long heartbeatTime = 25 * 1000;

	private long disconnectDelay = 5 * 1000;

	private int httpMessageCacheSize = 100;

	private boolean webSocketsEnabled = true;

	private final TaskScheduler taskScheduler;

	private final List<String> validSockJsPrefixes = new ArrayList<String>();

	private final Set<String> knownSockJsPrefixes = new CopyOnWriteArraySet<String>();


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
	 * Use this property to configure one or more prefixes that this SockJS service is
	 * allowed to serve. The prefix (e.g. "/echo") is needed to extract the SockJS
	 * specific portion of the URL (e.g. "${prefix}/info", "${prefix}/iframe.html", etc).
	 *
	 * <p>This property is not strictly required. In most cases, the SockJS path can be
	 * auto-detected since the initial request from the SockJS client is of the form
	 * "{prefix}/info". Assuming the SockJS service is mapped correctly (e.g. using
	 * Ant-style pattern "/echo/**") this should work fine. This property can be used
	 * to configure explicitly the prefixes this service is allowed to service.
	 *
	 * @param prefixes the prefixes to use; prefixes do not need to include the portions
	 *        of the path that represent Servlet container context or Servlet path.
	 */
	public void setValidSockJsPrefixes(String... prefixes) {

		this.validSockJsPrefixes.clear();
		for (String prefix : prefixes) {
			if (prefix.endsWith("/") && (prefix.length() > 1)) {
				prefix = prefix.substring(0, prefix.length() - 1);
			}
			this.validSockJsPrefixes.add(prefix);
		}

		// sort with longest prefix at the top
		Collections.sort(this.validSockJsPrefixes, Collections.reverseOrder(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return new Integer(o1.length()).compareTo(new Integer(o2.length()));
			}
		}));
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
	 * Some load balancers do sticky sessions, but only if there is a "JSESSIONID"
	 * cookie. Even if it is set to a dummy value, it doesn't matter since
	 * session information is added by the load balancer.
	 *
	 * <p>The default value is "false" since Java servers set the session cookie.
	 */
	public void setDummySessionCookieEnabled(boolean sessionCookieEnabled) {
		this.sessionCookieEnabled = sessionCookieEnabled;
	}

	/**
	 * Whether setting JSESSIONID cookie is necessary.
	 * @see #setDummySessionCookieEnabled(boolean)
	 */
	public boolean isDummySessionCookieEnabled() {
		return this.sessionCookieEnabled;
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
			WebSocketHandler wsHandler) throws SockJsException {

		String sockJsPath = getSockJsPath(request);
		if (sockJsPath == null) {
			logger.warn("Could not determine SockJS path for URL \"" + request.getURI().getPath() +
					". Consider setting validSockJsPrefixes.");
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
	 * Return the SockJS path or null if the path could not be determined.
	 */
	private String getSockJsPath(ServerHttpRequest request) {

		String path = request.getURI().getPath();

		// Try SockJS prefix hints
		if (!this.validSockJsPrefixes.isEmpty()) {
			for (String prefix : this.validSockJsPrefixes) {
				int index = path.indexOf(prefix);
				if (index != -1) {
					this.knownSockJsPrefixes.add(path.substring(0, index + prefix.length()));
					return path.substring(index + prefix.length());
				}
			}
			return null;
		}

		// Try SockJS info request
		if (path.endsWith("/info")) {
			this.knownSockJsPrefixes.add(path.substring(0, path.length() - "/info".length()));
			return "/info";
		}

		// Have we seen this prefix before (following the initial /info request)?
		String match = null;
		for (String sockJsPath : this.knownSockJsPrefixes) {
			if (path.startsWith(sockJsPath)) {
				if ((match == null) || (match.length() < sockJsPath.length())) {
					match = sockJsPath;
				}
			}
		}
		if (match != null) {
			String result = path.substring(match.length());
			Assert.isTrue(result.charAt(0)  == '/', "Invalid SockJS path extracted from incoming path \"" +
					path + "\". The extracted SockJS path is \"" + result +
					"\". It was extracted from these known SockJS prefixes " + this.knownSockJsPrefixes +
					". Consider setting 'validSockJsPrefixes' on DefaultSockJsService.");
			return result;
		}

		// Try SockJS greeting
		String pathNoSlash = path.endsWith("/")  ? path.substring(0, path.length() - 1) : path;
		String lastSegment = pathNoSlash.substring(pathNoSlash.lastIndexOf('/') + 1);

		if (!isValidTransportType(lastSegment) && !lastSegment.startsWith("iframe")) {
			this.knownSockJsPrefixes.add(path);
			return "";
		}

		return null;
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

				String content = String.format(INFO_CONTENT, random.nextInt(), isDummySessionCookieEnabled(), isWebSocketEnabled());
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
