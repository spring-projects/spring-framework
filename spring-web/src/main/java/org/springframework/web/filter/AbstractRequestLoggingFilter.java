/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * Base class for <code>Filter</code>s that perform logging operations before and after a request is processed.
 *
 * <p>Subclasses should override the <code>beforeRequest(HttpServletRequest, String)</code> and
 * <code>afterRequest(HttpServletRequest, String)</code> methods to perform the actual logging around the request.
 *
 * <p>Subclasses are passed the message to write to the log in the <code>beforeRequest</code> and
 * <code>afterRequest</code> methods. By default, only the URI of the request is logged. However, setting the
 * <code>includeQueryString</code> property to <code>true</code> will cause the query string of the request to be
 * included also. The payload (body) of the request can be logged via the <code>includePayload</code> flag. Note that
 * this will only log that which is read, which might not be the entire payload.
 *
 * <p>Prefixes and suffixes for the before and after messages can be configured using the
 * <code>beforeMessagePrefix</code>, <code>afterMessagePrefix</code>, <code>beforeMessageSuffix</code> and
 * <code>afterMessageSuffix</code> properties,
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see #beforeRequest
 * @see #afterRequest
 * @since 1.2.5
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
	 * Set whether or not the query string should be included in the log message. <p>Should be configured using an
	 * <code>&lt;init-param&gt;</code> for parameter name "includeQueryString" in the filter definition in
	 * <code>web.xml</code>.
	 */
	public void setIncludeQueryString(boolean includeQueryString) {
		this.includeQueryString = includeQueryString;
	}

	/**
	 * Return whether or not the query string should be included in the log message.
	 */
	protected boolean isIncludeQueryString() {
		return this.includeQueryString;
	}

	/**
	 * Set whether or not the client address and session id should be included in the log message. <p>Should be configured
	 * using an <code>&lt;init-param&gt;</code> for parameter name "includeClientInfo" in the filter definition in
	 * <code>web.xml</code>.
	 */
	public void setIncludeClientInfo(boolean includeClientInfo) {
		this.includeClientInfo = includeClientInfo;
	}

	/**
	 * Return whether or not the client address and session id should be included in the log message.
	 */
	protected boolean isIncludeClientInfo() {
		return this.includeClientInfo;
	}

	/**
	 * Set whether or not the request payload (body) should be included in the log message. <p>Should be configured using
	 * an <code>&lt;init-param&gt;</code> for parameter name "includePayload" in the filter definition in
	 * <code>web.xml</code>.
	 */

	public void setIncludePayload(boolean includePayload) {
		this.includePayload = includePayload;
	}

	/**
	 * Return whether or not the request payload (body) should be included in the log message.
	 */
	protected boolean isIncludePayload() {
		return includePayload;
	}

	/**
	 * Sets the maximum length of the payload body to be included in the log message. Default is 50 characters.
	 */
	public void setMaxPayloadLength(int maxPayloadLength) {
		Assert.isTrue(maxPayloadLength >= 0, "'maxPayloadLength' should be larger than or equal to 0");
		this.maxPayloadLength = maxPayloadLength;
	}

	/**
	 * Return the maximum length of the payload body to be included in the log message.
	 */
	protected int getMaxPayloadLength() {
		return maxPayloadLength;
	}

	/**
	 * Set the value that should be prepended to the log message written <i>before</i> a request is processed.
	 */
	public void setBeforeMessagePrefix(String beforeMessagePrefix) {
		this.beforeMessagePrefix = beforeMessagePrefix;
	}

	/**
	 * Set the value that should be apppended to the log message written <i>before</i> a request is processed.
	 */
	public void setBeforeMessageSuffix(String beforeMessageSuffix) {
		this.beforeMessageSuffix = beforeMessageSuffix;
	}

	/**
	 * Set the value that should be prepended to the log message written <i>after</i> a request is processed.
	 */
	public void setAfterMessagePrefix(String afterMessagePrefix) {
		this.afterMessagePrefix = afterMessagePrefix;
	}

	/**
	 * Set the value that should be appended to the log message written <i>after</i> a request is processed.
	 */
	public void setAfterMessageSuffix(String afterMessageSuffix) {
		this.afterMessageSuffix = afterMessageSuffix;
	}

	/**
	 * The default value is "true" so that the filter may log a "before" message
	 * at the start of request processing and an "after" message at the end from
	 * when the last asynchronously dispatched thread is exiting.
	 */
	@Override
	protected boolean shouldFilterAsyncDispatches() {
		return true;
	}

	/**
	 * Forwards the request to the next filter in the chain and delegates down to the subclasses to perform the actual
	 * request logging both before and after the request is processed.
	 *
	 * @see #beforeRequest
	 * @see #afterRequest
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		boolean isAsyncDispatch = isAsyncDispatch(request);

		if (isIncludePayload()) {
			if (isAsyncDispatch) {
				request = WebUtils.getNativeRequest(request, RequestCachingRequestWrapper.class);
				Assert.notNull(request, "Expected wrapped request");
			}
			else {
				request = new RequestCachingRequestWrapper(request);
			}
		}

		if (!isAsyncDispatch) {
			beforeRequest(request, getBeforeMessage(request));
		}
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			if (isLastRequestThread(request)) {
				afterRequest(request, getAfterMessage(request));
			}
		}
	}

	/**
	 * Get the message to write to the log before the request.
	 *
	 * @see #createMessage
	 */
	private String getBeforeMessage(HttpServletRequest request) {
		return createMessage(request, this.beforeMessagePrefix, this.beforeMessageSuffix);
	}

	/**
	 * Get the message to write to the log after the request.
	 *
	 * @see #createMessage
	 */
	private String getAfterMessage(HttpServletRequest request) {
		return createMessage(request, this.afterMessagePrefix, this.afterMessageSuffix);
	}

	/**
	 * Create a log message for the given request, prefix and suffix. <p>If <code>includeQueryString</code> is
	 * <code>true</code> then the inner part of the log message will take the form <code>request_uri?query_string</code>
	 * otherwise the message will simply be of the form <code>request_uri</code>. <p>The final message is composed of the
	 * inner part as described and the supplied prefix and suffix.
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
		if (isIncludePayload() && request instanceof RequestCachingRequestWrapper) {
			RequestCachingRequestWrapper wrapper = (RequestCachingRequestWrapper) request;
			byte[] buf = wrapper.toByteArray();
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
	 * Concrete subclasses should implement this method to write a log message <i>before</i> the request is processed.
	 *
	 * @param request current HTTP request
	 * @param message the message to log
	 */
	protected abstract void beforeRequest(HttpServletRequest request, String message);

	/**
	 * Concrete subclasses should implement this method to write a log message <i>after</i> the request is processed.
	 *
	 * @param request current HTTP request
	 * @param message the message to log
	 */
	protected abstract void afterRequest(HttpServletRequest request, String message);


	private static class RequestCachingRequestWrapper extends HttpServletRequestWrapper {

		private final ByteArrayOutputStream bos = new ByteArrayOutputStream();

		private final ServletInputStream inputStream;

		private BufferedReader reader;

		private RequestCachingRequestWrapper(HttpServletRequest request) throws IOException {
			super(request);
			this.inputStream = new RequestCachingInputStream(request.getInputStream());
		}

		@Override
		public ServletInputStream getInputStream() throws IOException {
			return inputStream;
		}

		@Override
		public String getCharacterEncoding() {
			return super.getCharacterEncoding() != null ? super.getCharacterEncoding() :
					WebUtils.DEFAULT_CHARACTER_ENCODING;
		}

		@Override
		public BufferedReader getReader() throws IOException {
			if (this.reader == null) {
				this.reader = new BufferedReader(new InputStreamReader(inputStream, getCharacterEncoding()));
			}
			return this.reader;
		}

		private byte[] toByteArray() {
			return this.bos.toByteArray();
		}

		private class RequestCachingInputStream extends ServletInputStream {

			private final ServletInputStream is;

			private RequestCachingInputStream(ServletInputStream is) {
				this.is = is;
			}

			@Override
			public int read() throws IOException {
				int ch = is.read();
				if (ch != -1) {
					bos.write(ch);
				}
				return ch;
			}

		}

	}

}
