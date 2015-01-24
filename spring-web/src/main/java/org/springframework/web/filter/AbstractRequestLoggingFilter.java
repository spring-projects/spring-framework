/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Base class for {@code Filter}s that perform logging operations before and after a request
 * is processed.
 *
 * <p>Subclasses should override the {@code beforeRequest(HttpServletRequest, String)} and
 * {@code afterRequest(HttpServletRequest, String)} methods to perform the actual logging
 * around the request.
 *
 * <p>Subclasses are passed the message to write to the log in the {@code beforeRequest} and
 * {@code afterRequest} methods. By default, only the URI of the request is logged. However,
 * setting the {@code includeQueryString} property to {@code true} will cause the query string
 * of the request to be included also. The payload (body) of the request can be logged via the
 * {@code includePayload} flag. Note that this will only log that which is read, which might
 * not be the entire payload.
 *
 * <p>Prefixes and suffixes for the before and after messages can be configured using the
 * {@code beforeMessagePrefix}, {@code afterMessagePrefix}, {@code beforeMessageSuffix} and
 * {@code afterMessageSuffix} properties,
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 1.2.5
 * @see #beforeRequest
 * @see #afterRequest
 */
public abstract class AbstractRequestLoggingFilter extends OncePerRequestFilter {

	public static final String DEFAULT_BEFORE_MESSAGE_PREFIX = "Before request [";

	public static final String DEFAULT_BEFORE_MESSAGE_SUFFIX = "]";

	public static final String DEFAULT_AFTER_MESSAGE_PREFIX = "After request [";

	public static final String DEFAULT_AFTER_MESSAGE_SUFFIX = "]";

	private static final int DEFAULT_MAX_PAYLOAD_LENGTH = 50;


	private boolean includeQueryString = false;

	private boolean includeClientInfo = false;

	private boolean includePayload = false;

	private int maxPayloadLength = DEFAULT_MAX_PAYLOAD_LENGTH;

	private String beforeMessagePrefix = DEFAULT_BEFORE_MESSAGE_PREFIX;

	private String beforeMessageSuffix = DEFAULT_BEFORE_MESSAGE_SUFFIX;

	private String afterMessagePrefix = DEFAULT_AFTER_MESSAGE_PREFIX;

	private String afterMessageSuffix = DEFAULT_AFTER_MESSAGE_SUFFIX;


	/**
	 * Set whether the query string should be included in the log message.
	 * <p>Should be configured using an {@code &lt;init-param&gt;} for parameter name
	 * "includeQueryString" in the filter definition in {@code web.xml}.
	 */
	public void setIncludeQueryString(boolean includeQueryString) {
		this.includeQueryString = includeQueryString;
	}

	/**
	 * Return whether the query string should be included in the log message.
	 */
	protected boolean isIncludeQueryString() {
		return this.includeQueryString;
	}

	/**
	 * Set whether the client address and session id should be included in the
	 * log message.
	 * <p>Should be configured using an {@code &lt;init-param&gt;} for parameter name
	 * "includeClientInfo" in the filter definition in {@code web.xml}.
	 */
	public void setIncludeClientInfo(boolean includeClientInfo) {
		this.includeClientInfo = includeClientInfo;
	}

	/**
	 * Return whether the client address and session id should be included in the
	 * log message.
	 */
	protected boolean isIncludeClientInfo() {
		return this.includeClientInfo;
	}

	/**
	 * Set whether the request payload (body) should be included in the log message.
	 * <p>Should be configured using an {@code &lt;init-param&gt;} for parameter name
	 * "includePayload" in the filter definition in {@code web.xml}.
	 */

	public void setIncludePayload(boolean includePayload) {
		this.includePayload = includePayload;
	}

	/**
	 * Return whether the request payload (body) should be included in the log message.
	 */
	protected boolean isIncludePayload() {
		return this.includePayload;
	}

	/**
	 * Sets the maximum length of the payload body to be included in the log message.
	 * Default is 50 characters.
	 */
	public void setMaxPayloadLength(int maxPayloadLength) {
		Assert.isTrue(maxPayloadLength >= 0, "'maxPayloadLength' should be larger than or equal to 0");
		this.maxPayloadLength = maxPayloadLength;
	}

	/**
	 * Return the maximum length of the payload body to be included in the log message.
	 */
	protected int getMaxPayloadLength() {
		return this.maxPayloadLength;
	}

	/**
	 * Set the value that should be prepended to the log message written
	 * <i>before</i> a request is processed.
	 */
	public void setBeforeMessagePrefix(String beforeMessagePrefix) {
		this.beforeMessagePrefix = beforeMessagePrefix;
	}

	/**
	 * Set the value that should be appended to the log message written
	 * <i>before</i> a request is processed.
	 */
	public void setBeforeMessageSuffix(String beforeMessageSuffix) {
		this.beforeMessageSuffix = beforeMessageSuffix;
	}

	/**
	 * Set the value that should be prepended to the log message written
	 * <i>after</i> a request is processed.
	 */
	public void setAfterMessagePrefix(String afterMessagePrefix) {
		this.afterMessagePrefix = afterMessagePrefix;
	}

	/**
	 * Set the value that should be appended to the log message written
	 * <i>after</i> a request is processed.
	 */
	public void setAfterMessageSuffix(String afterMessageSuffix) {
		this.afterMessageSuffix = afterMessageSuffix;
	}


	/**
	 * The default value is "false" so that the filter may log a "before" message
	 * at the start of request processing and an "after" message at the end from
	 * when the last asynchronously dispatched thread is exiting.
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * Forwards the request to the next filter in the chain and delegates down to the subclasses
	 * to perform the actual request logging both before and after the request is processed.
	 * @see #beforeRequest
	 * @see #afterRequest
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		boolean isFirstRequest = !isAsyncDispatch(request);
		HttpServletRequest requestToUse = request;

		if (isIncludePayload() && isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
			requestToUse = new ContentCachingRequestWrapper(request);
		}

		boolean shouldLog = shouldLog(requestToUse);
		if (shouldLog && isFirstRequest) {
			beforeRequest(requestToUse, getBeforeMessage(requestToUse));
		}
		try {
			filterChain.doFilter(requestToUse, response);
		}
		finally {
			if (shouldLog && !isAsyncStarted(requestToUse)) {
				afterRequest(requestToUse, getAfterMessage(requestToUse));
			}
		}
	}

	/**
	 * Get the message to write to the log before the request.
	 * @see #createMessage
	 */
	private String getBeforeMessage(HttpServletRequest request) {
		return createMessage(request, this.beforeMessagePrefix, this.beforeMessageSuffix);
	}

	/**
	 * Get the message to write to the log after the request.
	 * @see #createMessage
	 */
	private String getAfterMessage(HttpServletRequest request) {
		return createMessage(request, this.afterMessagePrefix, this.afterMessageSuffix);
	}

	/**
	 * Create a log message for the given request, prefix and suffix.
	 * <p>If {@code includeQueryString} is {@code true}, then the inner part
	 * of the log message will take the form {@code request_uri?query_string};
	 * otherwise the message will simply be of the form {@code request_uri}.
	 * <p>The final message is composed of the inner part as described and
	 * the supplied prefix and suffix.
	 */
	protected String createMessage(HttpServletRequest request, String prefix, String suffix) {
		StringBuilder msg = new StringBuilder();
		msg.append(prefix);
		msg.append("uri=").append(request.getRequestURI());
		if (isIncludeQueryString()) {
			msg.append('?').append(request.getQueryString());
		}
		if (isIncludeClientInfo()) {
			String client = request.getRemoteAddr();
			if (StringUtils.hasLength(client)) {
				msg.append(";client=").append(client);
			}
			HttpSession session = request.getSession(false);
			if (session != null) {
				msg.append(";session=").append(session.getId());
			}
			String user = request.getRemoteUser();
			if (user != null) {
				msg.append(";user=").append(user);
			}
		}
		if (isIncludePayload() && request instanceof ContentCachingRequestWrapper) {
			ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
			byte[] buf = wrapper.getContentAsByteArray();
			if (buf.length > 0) {
				int length = Math.min(buf.length, getMaxPayloadLength());
				String payload;
				try {
					payload = new String(buf, 0, length, wrapper.getCharacterEncoding());
				}
				catch (UnsupportedEncodingException e) {
					payload = "[unknown]";
				}
				msg.append(";payload=").append(payload);
			}

		}
		msg.append(suffix);
		return msg.toString();
	}


	/**
	 * Determine whether to call the {@link #beforeRequest}/{@link #afterRequest}
	 * methods for the current request, i.e. whether logging is currently active
	 * (and the log message is worth building).
	 * <p>The default implementation always returns {@code true}. Subclasses may
	 * override this with a log level check.
	 * @param request current HTTP request
	 * @return {@code true} if the before/after method should get called;
	 * {@code false} otherwise
	 * @since 4.1.5
	 */
	protected boolean shouldLog(HttpServletRequest request) {
		return true;
	}

	/**
	 * Concrete subclasses should implement this method to write a log message
	 * <i>before</i> the request is processed.
	 * @param request current HTTP request
	 * @param message the message to log
	 */
	protected abstract void beforeRequest(HttpServletRequest request, String message);

	/**
	 * Concrete subclasses should implement this method to write a log message
	 * <i>after</i> the request is processed.
	 * @param request current HTTP request
	 * @param message the message to log
	 */
	protected abstract void afterRequest(HttpServletRequest request, String message);

}
