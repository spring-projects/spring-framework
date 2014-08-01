/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.support;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for {@link RequestReplyHeaderMapper} implementations.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Stephane Nicoll
 * @since 4.1
 */
public abstract class AbstractHeaderMapper<T> implements RequestReplyHeaderMapper<T> {

	/**
	 * A special pattern that only matches standard request headers.
	 */
	public static final String STANDARD_REQUEST_HEADER_NAME_PATTERN = "STANDARD_REQUEST_HEADERS";

	/**
	 * A special pattern that only matches standard reply headers.
	 */
	public static final String STANDARD_REPLY_HEADER_NAME_PATTERN = "STANDARD_REPLY_HEADERS";

	/**
	 * A special pattern that matches any header that is not a standard header (i.e. any
	 * header that does not start with the configured standard header prefix)
	 */
	public static final String NON_STANDARD_HEADER_NAME_PATTERN = "NON_STANDARD_HEADERS";

	private static final Collection<String> TRANSIENT_HEADER_NAMES = Arrays.asList(
			MessageHeaders.ID, MessageHeaders.TIMESTAMP);

	protected final Log logger = LogFactory.getLog(getClass());

	private final String standardHeaderPrefix;

	private final Collection<String> requestHeaderNames;

	private final Collection<String> replyHeaderNames;

	private volatile HeaderMatcher requestHeaderMatcher;

	private volatile HeaderMatcher replyHeaderMatcher;

	/**
	 * Create a new instance.
	 * @param standardHeaderPrefix the header prefix that identifies standard header. Such prefix helps to
	 * differentiate user-defined headers from standard headers. If set, user-defined headers are also
	 * mapped by default
	 * @param requestHeaderNames the header names that should be mapped from a request to {@link MessageHeaders}
	 * @param replyHeaderNames the header names that should be mapped to a response from {@link MessageHeaders}
	 */
	protected AbstractHeaderMapper(String standardHeaderPrefix,
			Collection<String> requestHeaderNames, Collection<String> replyHeaderNames) {

		this.standardHeaderPrefix = standardHeaderPrefix;
		this.requestHeaderNames = requestHeaderNames;
		this.replyHeaderNames = replyHeaderNames;
		this.requestHeaderMatcher = createDefaultHeaderMatcher(this.standardHeaderPrefix, this.requestHeaderNames);
		this.replyHeaderMatcher = createDefaultHeaderMatcher(this.standardHeaderPrefix, this.replyHeaderNames);
	}

	/**
	 * Provide the header names that should be mapped from a request
	 * to a {@link MessageHeaders}
	 * The values can also contain simple wildcard patterns (e.g. "foo*" or "*foo") to be matched.
	 *
	 * @param requestHeaderNames The request header names.
	 */
	public void setRequestHeaderNames(String... requestHeaderNames) {
		Assert.notNull(requestHeaderNames, "'requestHeaderNames' must not be null");
		this.requestHeaderMatcher = createHeaderMatcher(Arrays.asList(requestHeaderNames));
	}

	/**
	 * Provide the header names that should be mapped to a response
	 * from a {@link MessageHeaders}
	 * The values can also contain simple wildcard patterns (e.g. "foo*" or "*foo") to be matched.
	 *
	 * @param replyHeaderNames The reply header names.
	 */
	public void setReplyHeaderNames(String... replyHeaderNames) {
		Assert.notNull(replyHeaderNames, "'replyHeaderNames' must not be null");
		this.replyHeaderMatcher = createHeaderMatcher(Arrays.asList(replyHeaderNames));
	}

	/**
	 * Create the initial {@link HeaderMatcher} based on the specified headers. If a
	 * {@code standardHeaderPrefix} is set, any non standard headers are mapped as
	 * well.
	 */
	protected HeaderMatcher createDefaultHeaderMatcher(String standardHeaderPrefix, Collection<String> headerNames) {
		HeaderMatcher headerMatcher = new ContentBasedHeaderMatcher(true, headerNames);
		if (StringUtils.hasText(standardHeaderPrefix)) {
			HeaderMatcher nonStandardHeaderMatcher = new PrefixBasedMatcher(false, standardHeaderPrefix);
			return new CompositeHeaderMatcher(headerMatcher, nonStandardHeaderMatcher);
		}
		else {
			return headerMatcher;
		}
	}

	/**
	 * Create a {@link HeaderMatcher} that match if any of the specified {@code patterns}
	 * match. The pattern can be a header name, a wildcard pattern such as
	 * {@code foo*}, {@code *foo}, or {@code within*foo}.
	 * <p>Special patterns are also recognized: {@link #STANDARD_REQUEST_HEADER_NAME_PATTERN},
	 * {@link #STANDARD_REQUEST_HEADER_NAME_PATTERN} and {@link #NON_STANDARD_HEADER_NAME_PATTERN}.
	 * @param patterns the patterns to apply
	 * @return a header mapper that match if any of the specified patters match
	 */
	protected CompositeHeaderMatcher createHeaderMatcher(Collection<String> patterns) {
		Collection<HeaderMatcher> matchers = new ArrayList<HeaderMatcher>();
		for (String pattern : patterns) {
			if (STANDARD_REQUEST_HEADER_NAME_PATTERN.equals(pattern)) {
				matchers.add(new ContentBasedHeaderMatcher(true, this.requestHeaderNames));
			}
			else if (STANDARD_REPLY_HEADER_NAME_PATTERN.equals(pattern)) {
				matchers.add(new ContentBasedHeaderMatcher(true, this.replyHeaderNames));
			}
			else if (NON_STANDARD_HEADER_NAME_PATTERN.equals(pattern)) {
				matchers.add(new PrefixBasedMatcher(false, this.standardHeaderPrefix));
			}
			else {
				matchers.add(new PatternBasedHeaderMatcher(Collections.singleton(pattern)));
			}
		}
		return new CompositeHeaderMatcher(matchers);
	}

	@Override
	public void fromHeadersToRequest(MessageHeaders headers, T target) {
		fromHeaders(headers, target, this.requestHeaderMatcher);
	}

	@Override
	public void fromHeadersToReply(MessageHeaders headers, T target) {
		fromHeaders(headers, target, this.replyHeaderMatcher);
	}

	@Override
	public Map<String, Object> toHeadersFromRequest(T source) {
		return toHeaders(source, this.requestHeaderMatcher);
	}

	@Override
	public Map<String, Object> toHeadersFromReply(T source) {
		return toHeaders(source, this.replyHeaderMatcher);
	}

	private void fromHeaders(MessageHeaders headers, T target, HeaderMatcher headerMatcher) {
		try {
			Map<String, Object> subset = new HashMap<String, Object>();
			for (Map.Entry<String, Object> entry : headers.entrySet()) {
				String headerName = entry.getKey();
				if (shouldMapHeader(headerName, headerMatcher)) {
					subset.put(headerName, entry.getValue());
				}
			}
			populateStandardHeaders(subset, target);
			populateUserDefinedHeaders(subset, target);
		}
		catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("error occurred while mapping from MessageHeaders", e);
			}
		}
	}

	private void populateUserDefinedHeaders(Map<String, Object> headers, T target) {
		for (String headerName : headers.keySet()) {
			Object value = headers.get(headerName);
			if (value != null) {
				try {
					if (!headerName.startsWith(this.standardHeaderPrefix)) {
						String key = createTargetPropertyName(headerName, true);
						populateUserDefinedHeader(key, value, target);
					}
				}
				catch (Exception e) {
					if (logger.isWarnEnabled()) {
						logger.warn("failed to map from Message header '" + headerName + "' to target", e);
					}
				}
			}
		}
	}

	/**
	 * Map headers from a source instance to the {@link MessageHeaders} of
	 * a {@link org.springframework.messaging.Message}.
	 */
	private Map<String, Object> toHeaders(T source, HeaderMatcher headerMatcher) {
		Map<String, Object> headers = new HashMap<String, Object>();
		Map<String, Object> standardHeaders = extractStandardHeaders(source);
		copyHeaders(standardHeaders, headers, headerMatcher);
		Map<String, Object> userDefinedHeaders = extractUserDefinedHeaders(source);
		copyHeaders(userDefinedHeaders, headers, headerMatcher);
		return headers;
	}

	private <V> void copyHeaders(Map<String, Object> source, Map<String, Object> target, HeaderMatcher headerMatcher) {
		if (!CollectionUtils.isEmpty(source)) {
			for (Map.Entry<String, Object> entry : source.entrySet()) {
				try {
					String headerName = createTargetPropertyName(entry.getKey(), false);
					if (shouldMapHeader(headerName, headerMatcher)) {
						target.put(headerName, entry.getValue());
					}
				}
				catch (Exception e) {
					if (logger.isWarnEnabled()) {
						logger.warn("error occurred while mapping header '"
								+ entry.getKey() + "' to Message header", e);
					}
				}
			}
		}
	}

	private boolean shouldMapHeader(String headerName, HeaderMatcher headerMatcher) {
		if (!StringUtils.hasText(headerName)
				|| getTransientHeaderNames().contains(headerName)) {
			return false;
		}
		return headerMatcher.matchHeader(headerName);
	}

	@SuppressWarnings("unchecked")
	protected <V> V getHeaderIfAvailable(Map<String, Object> headers, String name, Class<V> type) {
		Object value = headers.get(name);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			if (logger.isWarnEnabled()) {
				logger.warn("skipping header '" + name + "' since it is not of expected type [" + type + "], it is [" +
						value.getClass() + "]");
			}
			return null;
		}
		else {
			return (V) value;
		}
	}

	/**
	 * Alter the specified {@code propertyName} if necessary. By default, the original
	 * {@code propertyName} is returned
	 * @param propertyName the original name of the property
	 * @param fromMessageHeaders specify if the property originates from a {@link MessageHeaders}
	 * instance (true) or from the type managed by this mapper (false)
	 */
	protected String createTargetPropertyName(String propertyName, boolean fromMessageHeaders) {
		return propertyName;
	}

	/**
	 * Return the transient header names. Transient headers are never mapped.
	 */
	protected Collection<String> getTransientHeaderNames() {
		return TRANSIENT_HEADER_NAMES;
	}

	/**
	 * Extract the standard headers from the specified source.
	 */
	protected abstract Map<String, Object> extractStandardHeaders(T source);

	/**
	 * Extract the user-defined headers from the specified source.
	 */
	protected abstract Map<String, Object> extractUserDefinedHeaders(T source);

	/**
	 * Populate the specified standard headers to the specified source.
	 */
	protected abstract void populateStandardHeaders(Map<String, Object> headers, T target);

	/**
	 * Populate the specified user-defined headers to the specified source.
	 */
	protected abstract void populateUserDefinedHeader(String headerName, Object headerValue, T target);

	/**
	 * Strategy interface to determine if a given header name match.
	 */
	protected interface HeaderMatcher {

		/**
		 * Specify if the given {@code headerName} match.
		 */
		boolean matchHeader(String headerName);

	}

	/**
	 * A content-based {@link HeaderMatcher} that match if the specified
	 * header is contained within a list of candidates. The case of the
	 * header does not matter.
	 */
	protected static class ContentBasedHeaderMatcher implements HeaderMatcher {
		private static final Log logger = LogFactory.getLog(HeaderMatcher.class);

		private final boolean match;

		private final Collection<String> content;

		public ContentBasedHeaderMatcher(boolean match, Collection<String> content) {
			this.match = match;
			Assert.notNull(content, "Content must not be null");
			this.content = content;
		}

		@Override
		public boolean matchHeader(String headerName) {
			boolean result = (this.match == containsIgnoreCase(headerName));
			if (result && logger.isDebugEnabled()) {
				StringBuilder message = new StringBuilder("headerName=[{0}] WILL be mapped, ");
				if (!this.match) {
					message.append("not ");
				}
				message.append("found in {1}");
				logger.debug(MessageFormat.format(message.toString(), headerName, this.content));
			}
			return result;
		}

		private boolean containsIgnoreCase(String name) {
			for (String headerName : this.content) {
				if (headerName.equalsIgnoreCase(name)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * A pattern-based {@link HeaderMatcher} that match if the specified
	 * header match one of the specified simple patterns.
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected static class PatternBasedHeaderMatcher implements HeaderMatcher {

		private static final Log logger = LogFactory.getLog(HeaderMatcher.class);

		private final Collection<String> patterns;

		public PatternBasedHeaderMatcher(Collection<String> patterns) {
			Assert.notNull(patterns, "Patters must no be null");
			Assert.notEmpty(patterns, "At least one pattern must be specified");
			this.patterns = patterns;
		}

		@Override
		public boolean matchHeader(String headerName) {
			String header = headerName.toLowerCase();
			for (String pattern : this.patterns) {
				if (PatternMatchUtils.simpleMatch(pattern.toLowerCase(), header)) {
					if (logger.isDebugEnabled()) {
						logger.debug(MessageFormat.format(
								"headerName=[{0}] WILL be mapped, matched pattern={1}", headerName, pattern));
					}
					return true;
				}
			}
			return false;
		}

	}

	/**
	 * A prefix-based {@link HeaderMatcher} that match if the specified
	 * header starts with a configurable prefix.
	 */
	protected static class PrefixBasedMatcher implements HeaderMatcher {
		private static final Log logger = LogFactory.getLog(HeaderMatcher.class);


		private final boolean match;

		private final String prefix;

		public PrefixBasedMatcher(boolean match, String prefix) {
			this.match = match;
			this.prefix = prefix;
		}

		@Override
		public boolean matchHeader(String headerName) {
			boolean result = (this.match == headerName.startsWith(this.prefix));
			if (result && logger.isDebugEnabled()) {
				StringBuilder message = new StringBuilder("headerName=[{0}] WILL be mapped, ");
				if (!this.match) {
					message.append("does not ");
				}
				message.append("start with [{1}]");
				logger.debug(MessageFormat.format(message.toString(), headerName, this.prefix));
			}
			return result;
		}
	}

	protected static class CompositeHeaderMatcher implements HeaderMatcher {
		private final Collection<HeaderMatcher> strategies;

		private static final Log logger = LogFactory.getLog(HeaderMatcher.class);

		CompositeHeaderMatcher(Collection<HeaderMatcher> strategies) {
			this.strategies = strategies;
		}

		CompositeHeaderMatcher(HeaderMatcher... strategies) {
			this(Arrays.asList(strategies));
		}

		@Override
		public boolean matchHeader(String headerName) {
			for (HeaderMatcher strategy : this.strategies) {
				if (strategy.matchHeader(headerName)) {
					return true;
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug(MessageFormat.format("headerName=[{0}] WILL NOT be mapped", headerName));
			}
			return false;
		}
	}

}
