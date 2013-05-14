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
package org.springframework.web.socket.sockjs;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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

/**
 * An abstract class for {@link SockJsService} implementations. Provides configuration
 * support, SockJS path resolution, and processing for static SockJS requests (e.g.
 * "/info", "/iframe.html", etc). Sub-classes are responsible for handling transport
 * requests.
 *
 * <p>
 * It is expected that this service is mapped correctly to one or more prefixes such as
 * "/echo" including all sub-URLs (e.g. "/echo/**"). A SockJS service itself is generally
 * unaware of request mapping details but nevertheless must be able to extract the SockJS
 * path, which is the portion of the request path following the prefix. In most cases,
 * this class can auto-detect the SockJS path but you can also explicitly configure the
 * list of valid prefixes with {@link #setValidSockJsPrefixes(String...)}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractSockJsService implements SockJsService, SockJsConfiguration {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final int ONE_YEAR = 365 * 24 * 60 * 60;


	private String name = "SockJSService@" + ObjectUtils.getIdentityHexString(this);

	private String clientLibraryUrl = "https://d1fxtkz8shb9d2.cloudfront.net/sockjs-0.3.4.min.js";

	private int streamBytesLimit = 128 * 1024;

	private boolean jsessionIdCookieRequired = true;

	private long heartbeatTime = 25 * 1000;

	private long disconnectDelay = 5 * 1000;

	private boolean webSocketsEnabled = true;

	private final TaskScheduler taskScheduler;

	private final List<String> sockJsPrefixes = new ArrayList<String>();

	private final Set<String> sockJsPathCache = new CopyOnWriteArraySet<String>();


	public AbstractSockJsService(TaskScheduler scheduler) {
		Assert.notNull(scheduler, "scheduler is required");
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
	 * <p>
	 * This property is not strictly required. In most cases, the SockJS path can be
	 * auto-detected since the initial request from the SockJS client is of the form
	 * "{prefix}/info". Assuming the SockJS service is mapped correctly (e.g. using
	 * Ant-style pattern "/echo/**") this should work fine. This property can be used
	 * to configure explicitly the prefixes this service is allowed to service.
	 *
	 * @param prefixes the prefixes to use; prefixes do not need to include the portions
	 *        of the path that represent Servlet container context or Servlet path.
	 */
	public void setValidSockJsPrefixes(String... prefixes) {

		this.sockJsPrefixes.clear();
		for (String prefix : prefixes) {
			if (prefix.endsWith("/") && (prefix.length() > 1)) {
				prefix = prefix.substring(0, prefix.length() - 1);
			}
			this.sockJsPrefixes.add(prefix);
		}

		// sort with longest prefix at the top
		Collections.sort(this.sockJsPrefixes, Collections.reverseOrder(new Comparator<String>() {
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

	@Override
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
	public AbstractSockJsService setJsessionIdCookieRequired(boolean jsessionIdCookieRequired) {
		this.jsessionIdCookieRequired = jsessionIdCookieRequired;
		return this;
	}

	/**
	 * Whether setting JSESSIONID cookie is necessary.
	 * @see #setJsessionIdCookieRequired(boolean)
	 */
	public boolean isJsessionIdCookieRequired() {
		return this.jsessionIdCookieRequired;
	}

	public AbstractSockJsService setHeartbeatTime(long heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
		return this;
	}

	@Override
	public long getHeartbeatTime() {
		return this.heartbeatTime;
	}

	@Override
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * The amount of time in milliseconds before a client is considered
	 * disconnected after not having a receiving connection, i.e. an active
	 * connection over which the server can send data to the client.
	 * <p>
	 * The default value is 5000.
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
	public boolean isWebSocketEnabled() {
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
	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler)
			throws IOException, TransportErrorException {

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
				handleRawWebSocketRequest(request, response, handler);
				return;
			}

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

			handleTransportRequest(request, response, sessionId, TransportType.fromValue(transport), handler);
		}
		finally {
			response.flush();
		}
	}

	/**
	 * Return the SockJS path or null if the path could not be determined.
	 */
	private String getSockJsPath(ServerHttpRequest request) {

		String path = request.getURI().getPath();

		// SockJS prefix hints?
		if (!this.sockJsPrefixes.isEmpty()) {
			for (String prefix : this.sockJsPrefixes) {
				int index = path.indexOf(prefix);
				if (index != -1) {
					this.sockJsPathCache.add(path.substring(0, index + prefix.length()));
					return path.substring(index + prefix.length());
				}
			}
			return null;
		}

		// SockJS info request?
		if (path.endsWith("/info")) {
			this.sockJsPathCache.add(path.substring(0, path.length() - 6));
			return "/info";
		}

		// Have we seen this prefix before (following the initial /info request)?
		String match = null;
		for (String sockJsPath : this.sockJsPathCache) {
			if (path.startsWith(sockJsPath)) {
				if ((match == null) || (match.length() < sockJsPath.length())) {
					match = sockJsPath;
				}
			}
		}
		if (match != null) {
			return path.substring(match.length());
		}

		// SockJS greeting?
		String pathNoSlash = path.endsWith("/")  ? path.substring(0, path.length() - 1) : path;
		String lastSegment = pathNoSlash.substring(pathNoSlash.lastIndexOf('/') + 1);

		if ((TransportType.fromValue(lastSegment) == null) && !lastSegment.startsWith("iframe")) {
			this.sockJsPathCache.add(path);
			return "";
		}

		return null;
	}

	protected abstract void handleRawWebSocketRequest(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler webSocketHandler) throws IOException;

	protected abstract void handleTransportRequest(ServerHttpRequest request, ServerHttpResponse response,
			String sessionId, TransportType transportType, WebSocketHandler webSocketHandler)
					throws IOException, TransportErrorException;


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

		if (!isWebSocketEnabled() && transport.equals(TransportType.WEBSOCKET.value())) {
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

	protected void sendMethodNotAllowed(ServerHttpResponse response, List<HttpMethod> httpMethods) throws IOException {
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

				String content = String.format(INFO_CONTENT, random.nextInt(), isJsessionIdCookieRequired(), isWebSocketEnabled());
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
