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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

import static org.junit.Assert.*;
import static org.springframework.messaging.support.AbstractHeaderMapper.*;

/**
 * @author Stephane Nicoll
 */
public class HeaderMapperTests {

	private final GenericTestHeaderMapper mapper = new GenericTestHeaderMapper();

	@Test
	public void toHeadersFromRequest() {
		GenericTestProperties properties = createSimpleGenericTestProperties();

		Map<String, Object> attributes = this.mapper.toHeadersFromRequest(properties);
		assertEquals("appId", attributes.get(GenericTestHeaders.APP_ID));
		assertEquals("request-123", attributes.get(GenericTestHeaders.REQUEST_ONLY));
		assertFalse(attributes.containsKey(GenericTestHeaders.REPLY_ONLY));
		assertEquals("bar", attributes.get("foo"));
		assertEquals("Wrong number of mapped header(s)", 3, attributes.size());
	}

	@Test
	public void toHeadersFromRequestWithStar() {
		this.mapper.setRequestHeaderNames("*");
		GenericTestProperties properties = createSimpleGenericTestProperties();

		Map<String, Object> attributes = this.mapper.toHeadersFromRequest(properties);
		assertEquals("appId", attributes.get(GenericTestHeaders.APP_ID));
		assertEquals("request-123", attributes.get(GenericTestHeaders.REQUEST_ONLY));
		assertEquals("reply-123", attributes.get(GenericTestHeaders.REPLY_ONLY));
		assertEquals("bar", attributes.get("foo"));
		assertEquals("Wrong number of mapped header(s)", 4, attributes.size());
	}

	@Test
	public void toHeadersFromRequestWithCustomPatterns() {
		this.mapper.setRequestHeaderNames("foo*", "generic_reply*");
		GenericTestProperties properties = createSimpleGenericTestProperties();

		Map<String, Object> attributes = this.mapper.toHeadersFromRequest(properties);
		assertEquals(null, attributes.get(GenericTestHeaders.APP_ID));
		assertEquals(null, attributes.get(GenericTestHeaders.REQUEST_ONLY));
		assertEquals("reply-123", attributes.get(GenericTestHeaders.REPLY_ONLY));
		assertEquals("bar", attributes.get("foo"));
		assertEquals("Wrong number of mapped header(s)", 2, attributes.size());
	}

	@Test
	public void toHeadersFromRequestWithStandardRequestPattern() {
		this.mapper.setRequestHeaderNames("foo*", GenericTestHeaderMapper.STANDARD_REQUEST_HEADER_NAME_PATTERN);
		GenericTestProperties properties = createSimpleGenericTestProperties();
		properties.setUserDefinedHeader("foo2", "bar");
		properties.setUserDefinedHeader("something-else", "bar");

		Map<String, Object> attributes = this.mapper.toHeadersFromRequest(properties);
		assertEquals("appId", attributes.get(GenericTestHeaders.APP_ID));
		assertEquals("request-123", attributes.get(GenericTestHeaders.REQUEST_ONLY));
		assertFalse(attributes.containsKey(GenericTestHeaders.REPLY_ONLY));
		assertEquals("bar", attributes.get("foo"));
		assertEquals("bar", attributes.get("foo2"));
		assertEquals("Wrong number of mapped header(s)", 4, attributes.size());
	}

	@Test
	public void toHeadersFromRequestWithOnlyStandardHeaders() {
		this.mapper.setRequestHeaderNames(GenericTestHeaderMapper.STANDARD_REQUEST_HEADER_NAME_PATTERN);
		GenericTestProperties properties = createSimpleGenericTestProperties();
		properties.setUserDefinedHeader("foo2", "bar");
		properties.setUserDefinedHeader("something-else", "bar");

		Map<String, Object> attributes = this.mapper.toHeadersFromRequest(properties);
		assertEquals("appId", attributes.get(GenericTestHeaders.APP_ID));
		assertEquals("request-123", attributes.get(GenericTestHeaders.REQUEST_ONLY));
		assertFalse(attributes.containsKey(GenericTestHeaders.REPLY_ONLY));
		assertEquals("Wrong number of mapped header(s)", 2, attributes.size());
	}

	@Test
	public void toHeadersFromReply() {
		GenericTestProperties properties = createSimpleGenericTestProperties();

		Map<String, Object> attributes = this.mapper.toHeadersFromReply(properties);
		assertEquals("appId", attributes.get(GenericTestHeaders.APP_ID));
		assertFalse(attributes.containsKey(GenericTestHeaders.REQUEST_ONLY));
		assertEquals("reply-123", attributes.get(GenericTestHeaders.REPLY_ONLY));
		assertEquals("bar", attributes.get("foo"));
		assertEquals("Wrong number of mapped header(s)", 3, attributes.size());
	}

	@Test
	public void toHeadersFromReplyWithStar() {
		this.mapper.setReplyHeaderNames("*");
		GenericTestProperties properties = createSimpleGenericTestProperties();

		Map<String, Object> attributes = this.mapper.toHeadersFromReply(properties);
		assertEquals("appId", attributes.get(GenericTestHeaders.APP_ID));
		assertEquals("request-123", attributes.get(GenericTestHeaders.REQUEST_ONLY));
		assertEquals("reply-123", attributes.get(GenericTestHeaders.REPLY_ONLY));
		assertEquals("bar", attributes.get("foo"));
		assertEquals("Wrong number of mapped header(s)", 4, attributes.size());
	}

	@Test
	public void toHeadersFromReplyWithStandardReplyPattern() {
		this.mapper.setReplyHeaderNames("foo*", GenericTestHeaderMapper.STANDARD_REPLY_HEADER_NAME_PATTERN);
		GenericTestProperties properties = createSimpleGenericTestProperties();

		properties.setUserDefinedHeader("foo2", "bar");
		properties.setUserDefinedHeader("something-else", "bar");

		Map<String, Object> attributes = this.mapper.toHeadersFromReply(properties);
		assertEquals("appId", attributes.get(GenericTestHeaders.APP_ID));
		assertFalse(attributes.containsKey(GenericTestHeaders.REQUEST_ONLY));
		assertEquals("reply-123", attributes.get(GenericTestHeaders.REPLY_ONLY));
		assertEquals("bar", attributes.get("foo"));
		assertEquals("bar", attributes.get("foo2"));
		assertEquals("Wrong number of mapped header(s)", 4, attributes.size());
	}

	@Test
	public void toHeadersFromReplyWithOnlyStandardReplyHeaders() {
		this.mapper.setReplyHeaderNames(GenericTestHeaderMapper.STANDARD_REPLY_HEADER_NAME_PATTERN);
		GenericTestProperties properties = createSimpleGenericTestProperties();

		properties.setUserDefinedHeader("foo2", "bar");
		properties.setUserDefinedHeader("something-else", "bar");

		Map<String, Object> attributes = this.mapper.toHeadersFromReply(properties);
		assertEquals("appId", attributes.get(GenericTestHeaders.APP_ID));
		assertFalse(attributes.containsKey(GenericTestHeaders.REQUEST_ONLY));
		assertEquals("reply-123", attributes.get(GenericTestHeaders.REPLY_ONLY));
		assertEquals("Wrong number of mapped header(s)", 2, attributes.size());
	}

	@Test
	public void customTransientHeaderNames() {
		GenericTestHeaderMapper customMapper = new GenericTestHeaderMapper() {
			@Override
			protected Collection<String> getTransientHeaderNames() {
				return Arrays.asList("foo", GenericTestHeaders.APP_ID);
			}
		};
		GenericTestProperties properties = createSimpleGenericTestProperties();
		properties.setUserDefinedHeader("foo2", "bar2");

		Map<String, Object> attributes = customMapper.toHeadersFromReply(properties);
		// foo custom header and app Id not mapped
		assertFalse(attributes.containsKey(GenericTestHeaders.APP_ID));
		assertFalse(attributes.containsKey("foo"));
		assertEquals("bar2", attributes.get("foo2"));
		assertEquals("Wrong number of mapped header(s)", 2, attributes.size());
	}

	private GenericTestProperties createSimpleGenericTestProperties() {
		GenericTestProperties properties = new GenericTestProperties();
		properties.setAppId("appId");
		properties.setRequestOnly("request-123");
		properties.setReplyOnly("reply-123");

		properties.setUserDefinedHeader("foo", "bar");
		return properties;
	}

	@Test
	public void fromHeadersToRequest() {
		MessageHeaders messageHeaders = createSimpleMessageHeaders();
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToRequest(messageHeaders, properties);
		assertEquals("myAppId", properties.getAppId());
		assertNull(properties.getTransactionSize());
		assertEquals(true, properties.getRedelivered());
		assertEquals("request-456", properties.getRequestOnly());
		assertNull(properties.getReplyOnly());
		assertEquals("bar", properties.getUserDefinedHeaders().get("foo"));
		assertEquals(1, properties.getUserDefinedHeaders().size());
	}

	@Test
	public void fromHeadersToRequestWithStar() {
		this.mapper.setRequestHeaderNames("*");
		MessageHeaders messageHeaders = createSimpleMessageHeaders();
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToRequest(messageHeaders, properties);
		assertEquals("myAppId", properties.getAppId());
		assertNull(properties.getTransactionSize());
		assertEquals(true, properties.getRedelivered());
		assertEquals("request-456", properties.getRequestOnly());
		assertEquals("reply-456", properties.getReplyOnly());
		assertEquals("bar", properties.getUserDefinedHeaders().get("foo"));
		assertEquals(1, properties.getUserDefinedHeaders().size());
	}

	@Test
	public void fromHeadersToRequestWithStandardRequestPattern() {
		this.mapper.setRequestHeaderNames("foo", GenericTestHeaderMapper.STANDARD_REQUEST_HEADER_NAME_PATTERN);
		MessageHeaders messageHeaders = createSimpleMessageHeaders();
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToRequest(messageHeaders, properties);
		assertEquals("myAppId", properties.getAppId());
		assertNull(properties.getTransactionSize());
		assertEquals(true, properties.getRedelivered());
		assertEquals("request-456", properties.getRequestOnly());
		assertNull(properties.getReplyOnly());
		assertEquals("bar", properties.getUserDefinedHeaders().get("foo"));
		assertEquals(1, properties.getUserDefinedHeaders().size());
	}

	@Test
	public void fromHeadersToReply() {
		MessageHeaders messageHeaders = createSimpleMessageHeaders();
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToReply(messageHeaders, properties);
		assertEquals("myAppId", properties.getAppId());
		assertNull(properties.getTransactionSize());
		assertEquals(true, properties.getRedelivered());
		assertNull(properties.getRequestOnly());
		assertEquals("reply-456", properties.getReplyOnly());
		assertEquals("bar", properties.getUserDefinedHeaders().get("foo"));
		assertEquals(1, properties.getUserDefinedHeaders().size());
	}

	@Test
	public void fromHeadersToReplyWithStar() {
		this.mapper.setReplyHeaderNames("*");
		MessageHeaders messageHeaders = createSimpleMessageHeaders();
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToReply(messageHeaders, properties);
		assertEquals("myAppId", properties.getAppId());
		assertNull(properties.getTransactionSize());
		assertEquals(true, properties.getRedelivered());
		assertEquals("request-456", properties.getRequestOnly());
		assertEquals("reply-456", properties.getReplyOnly());
		assertEquals("bar", properties.getUserDefinedHeaders().get("foo"));
		assertEquals(1, properties.getUserDefinedHeaders().size());
	}


	@Test
	public void fromHeadersToReplyWithStandardReplyPattern() {
		this.mapper.setReplyHeaderNames("foo", GenericTestHeaderMapper.STANDARD_REPLY_HEADER_NAME_PATTERN);
		MessageHeaders messageHeaders = createSimpleMessageHeaders();
		GenericTestProperties properties = new GenericTestProperties();
		this.mapper.fromHeadersToReply(messageHeaders, properties);
		assertEquals("myAppId", properties.getAppId());
		assertNull(properties.getTransactionSize());
		assertEquals(true, properties.getRedelivered());
		assertNull(properties.getRequestOnly());
		assertEquals("reply-456", properties.getReplyOnly());
		assertEquals("bar", properties.getUserDefinedHeaders().get("foo"));
		assertEquals(1, properties.getUserDefinedHeaders().size());
	}

	public MessageHeaders createSimpleMessageHeaders() {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(GenericTestHeaders.APP_ID, "myAppId");
		headers.put(GenericTestHeaders.REDELIVERED, true);
		headers.put(GenericTestHeaders.REQUEST_ONLY, "request-456");
		headers.put(GenericTestHeaders.REPLY_ONLY, "reply-456");

		headers.put("foo", "bar");

		return new MessageHeaders(headers);
	}


	@Test
	public void prefixHeaderPatternMatching() {
		PatternBasedHeaderMatcher strategy =
				new PatternBasedHeaderMatcher(Collections.singleton("foo*"));

		assertMapping(strategy, "foo", true);
		assertMapping(strategy, "foo123", true);
		assertMapping(strategy, "FoO", true);

		assertMapping(strategy, "123foo", false);
		assertMapping(strategy, "_foo", false);
	}

	@Test
	public void suffixHeaderPatternMatching() {
		PatternBasedHeaderMatcher strategy =
				new PatternBasedHeaderMatcher(Collections.singleton("*foo"));

		assertMapping(strategy, "foo", true);
		assertMapping(strategy, "123foo", true);
		assertMapping(strategy, "FoO", true);

		assertMapping(strategy, "foo123", false);
		assertMapping(strategy, "foo_", false);
	}

	@Test
	public void contentHeaderMatching() {
		ContentBasedHeaderMatcher strategy =
				new ContentBasedHeaderMatcher(true, Arrays.asList("foo", "bar"));

		assertMapping(strategy, "foo", true);
		assertMapping(strategy, "bar", true);
		assertMapping(strategy, "FOO", true);
		assertMapping(strategy, "somethingElse", false);
	}

	@Test
	public void contentHeaderReverseMatching() {
		ContentBasedHeaderMatcher strategy =
				new ContentBasedHeaderMatcher(false, Arrays.asList("foo", "bar"));

		assertMapping(strategy, "foo", false);
		assertMapping(strategy, "bar", false);
		assertMapping(strategy, "somethingElse", true);
		assertMapping(strategy, "anything", true);
	}

	@Test
	public void prefixHeaderMatching() {
		PrefixBasedMatcher strategy = new PrefixBasedMatcher(true, "foo_");

		assertMapping(strategy, "foo_", true);
		assertMapping(strategy, "foo_ANYTHING", true);
		assertMapping(strategy, "something_foo_", false);
		assertMapping(strategy, "somethingElse", false);
	}

	@Test
	public void prefixHeaderReverseMatching() {
		PrefixBasedMatcher strategy = new PrefixBasedMatcher(false, "foo_");

		assertMapping(strategy, "foo_", false);
		assertMapping(strategy, "foo_ANYTHING", false);
		assertMapping(strategy, "something_foo_", true);
		assertMapping(strategy, "somethingElse", true);
	}


	protected void assertMapping(HeaderMatcher strategy, String candidate, boolean match) {
		assertEquals("Wrong mapping result for " + candidate + "", match, strategy.matchHeader(candidate));
	}


	private static abstract class GenericTestHeaders {

		public static final String PREFIX = "generic_";

		public static final String APP_ID = PREFIX + "appId";

		public static final String TRANSACTION_SIZE = PREFIX + "transactionSize";

		public static final String REDELIVERED = PREFIX + "redelivered";

		public static final String REQUEST_ONLY = PREFIX + "requestOnly";

		public static final String REPLY_ONLY = PREFIX + "replyOnly";

	}

	private static class GenericTestHeaderMapper extends AbstractHeaderMapper<GenericTestProperties> {


		private GenericTestHeaderMapper() {
			super(GenericTestHeaders.PREFIX,
					Arrays.asList(GenericTestHeaders.APP_ID, GenericTestHeaders.TRANSACTION_SIZE,
							GenericTestHeaders.REDELIVERED, GenericTestHeaders.REQUEST_ONLY),
					Arrays.asList(GenericTestHeaders.APP_ID, GenericTestHeaders.TRANSACTION_SIZE,
							GenericTestHeaders.REDELIVERED, GenericTestHeaders.REPLY_ONLY));
		}

		@Override
		protected Map<String, Object> extractStandardHeaders(GenericTestProperties source) {
			Map<String, Object> result = new HashMap<String, Object>();
			if (StringUtils.hasText(source.getAppId())) {
				result.put(GenericTestHeaders.APP_ID, source.getAppId());
			}
			if (source.getTransactionSize() != null) {
				result.put(GenericTestHeaders.TRANSACTION_SIZE, source.getTransactionSize());
			}
			if (source.getRedelivered() != null) {
				result.put(GenericTestHeaders.REDELIVERED, source.getRedelivered());
			}
			if (StringUtils.hasText(source.getRequestOnly())) {
				result.put(GenericTestHeaders.REQUEST_ONLY, source.getRequestOnly());
			}
			if (StringUtils.hasText(source.getReplyOnly())) {
				result.put(GenericTestHeaders.REPLY_ONLY, source.getReplyOnly());
			}
			return result;

		}

		@Override
		protected Map<String, Object> extractUserDefinedHeaders(GenericTestProperties source) {
			return source.getUserDefinedHeaders();
		}

		@Override
		protected void populateStandardHeaders(Map<String, Object> headers, GenericTestProperties target) {
			String appId = getHeaderIfAvailable(headers, GenericTestHeaders.APP_ID, String.class);
			if (StringUtils.hasText(appId)) {
				target.setAppId(appId);
			}
			Integer transactionSize = getHeaderIfAvailable(headers, GenericTestHeaders.TRANSACTION_SIZE, Integer.class);
			if (transactionSize != null) {
				target.setTransactionSize(transactionSize);
			}
			Boolean redelivered = getHeaderIfAvailable(headers, GenericTestHeaders.REDELIVERED, Boolean.class);
			if (redelivered != null) {
				target.setRedelivered(redelivered);
			}
			String requestOnly = getHeaderIfAvailable(headers, GenericTestHeaders.REQUEST_ONLY, String.class);
			if (StringUtils.hasText(requestOnly)) {
				target.setRequestOnly(requestOnly);
			}
			String replyOnly = getHeaderIfAvailable(headers, GenericTestHeaders.REPLY_ONLY, String.class);
			if (StringUtils.hasText(replyOnly)) {
				target.setReplyOnly(replyOnly);
			}

		}

		@Override
		protected void populateUserDefinedHeader(String headerName, Object headerValue, GenericTestProperties target) {
			target.setUserDefinedHeader(headerName, headerValue);
		}
	}

	private static class GenericTestProperties {

		private String appId;

		private Integer transactionSize;

		private Boolean redelivered;

		private String requestOnly;

		private String replyOnly;

		private final Map<String, Object> userDefinedHeaders = new HashMap<String, Object>();

		private GenericTestProperties() {
		}

		public String getAppId() {
			return appId;
		}

		public void setAppId(String appId) {
			this.appId = appId;
		}

		public Integer getTransactionSize() {
			return transactionSize;
		}

		public void setTransactionSize(Integer transactionSize) {
			this.transactionSize = transactionSize;
		}

		public Boolean getRedelivered() {
			return redelivered;
		}

		public void setRedelivered(boolean redelivered) {
			this.redelivered = redelivered;
		}

		public String getRequestOnly() {
			return requestOnly;
		}

		public void setRequestOnly(String requestOnly) {
			this.requestOnly = requestOnly;
		}

		public String getReplyOnly() {
			return replyOnly;
		}

		public void setReplyOnly(String replyOnly) {
			this.replyOnly = replyOnly;
		}

		public Map<String, Object> getUserDefinedHeaders() {
			return userDefinedHeaders;
		}

		public void setUserDefinedHeader(String name, Object value) {
			this.userDefinedHeaders.put(name, value);
		}
	}
}
