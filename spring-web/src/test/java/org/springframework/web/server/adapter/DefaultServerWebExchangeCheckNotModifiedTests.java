/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.server.adapter;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Parameterized tests for ServletWebRequest
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class DefaultServerWebExchangeCheckNotModifiedTests {

	private static final String CURRENT_TIME = "Wed, 09 Apr 2014 09:57:42 GMT";


	private SimpleDateFormat dateFormat;

	private MockServerHttpRequest request;

	private MockServerHttpResponse response;

	private DefaultServerWebExchange exchange;

	private Instant currentDate;

	@Parameter
	public HttpMethod method;

	@Parameters(name = "{0}")
	static public Iterable<Object[]> safeMethods() {
		return Arrays.asList(new Object[][] {
				{HttpMethod.GET},
				{HttpMethod.HEAD}
		});
	}


	@Before
	public void setUp() throws URISyntaxException {
		currentDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		request = new MockServerHttpRequest(method, "http://example.org");
		response = new MockServerHttpResponse();
		exchange = new DefaultServerWebExchange(request, response, new MockWebSessionManager());
	}

	@Test
	public void checkNotModifiedNon2xxStatus() {
		request.getHeaders().setIfModifiedSince(this.currentDate.toEpochMilli());
		response.setStatusCode(HttpStatus.NOT_MODIFIED);

		assertFalse(exchange.checkNotModified(this.currentDate));
		assertEquals(304, response.getStatusCode().value());
		assertEquals(-1, response.getHeaders().getLastModified());
	}

	@Test // SPR-14559
	public void checkNotModifiedInvalidIfNoneMatchHeader() {
		String eTag = "\"etagvalue\"";
		request.getHeaders().setIfNoneMatch("missingquotes");
		assertFalse(exchange.checkNotModified(eTag));
		assertNull(response.getStatusCode());
		assertEquals(eTag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedHeaderAlreadySet() {
		request.getHeaders().setIfModifiedSince(currentDate.toEpochMilli());
		response.getHeaders().add("Last-Modified", CURRENT_TIME);

		assertTrue(exchange.checkNotModified(currentDate));
		assertEquals(304, response.getStatusCode().value());
		assertEquals(1, response.getHeaders().get("Last-Modified").size());
		assertEquals(CURRENT_TIME, response.getHeaders().getFirst("Last-Modified"));
	}

	@Test
	public void checkNotModifiedTimestamp() throws Exception {
		request.getHeaders().setIfModifiedSince(currentDate.toEpochMilli());

		assertTrue(exchange.checkNotModified(currentDate));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(currentDate.toEpochMilli(), response.getHeaders().getLastModified());
	}

	@Test
	public void checkModifiedTimestamp() {
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		request.getHeaders().setIfModifiedSince(oneMinuteAgo.toEpochMilli());

		assertFalse(exchange.checkNotModified(currentDate));

		assertNull(response.getStatusCode());
		assertEquals(currentDate.toEpochMilli(), response.getHeaders().getLastModified());
	}

	@Test
	public void checkNotModifiedETag() {
		String eTag = "\"Foo\"";
		request.getHeaders().setIfNoneMatch(eTag);

		assertTrue(exchange.checkNotModified(eTag));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(eTag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedETagWithSeparatorChars() {
		String eTag = "\"Foo, Bar\"";
		request.getHeaders().setIfNoneMatch(eTag);

		assertTrue(exchange.checkNotModified(eTag));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(eTag, response.getHeaders().getETag());
	}


	@Test
	public void checkModifiedETag() {
		String currentETag = "\"Foo\"";
		String oldEtag = "Bar";
		request.getHeaders().setIfNoneMatch(oldEtag);

		assertFalse(exchange.checkNotModified(currentETag));

		assertNull(response.getStatusCode());
		assertEquals(currentETag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedUnpaddedETag() {
		String eTag = "Foo";
		String paddedEtag = String.format("\"%s\"", eTag);
		request.getHeaders().setIfNoneMatch(paddedEtag);

		assertTrue(exchange.checkNotModified(eTag));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(paddedEtag, response.getHeaders().getETag());
	}

	@Test
	public void checkModifiedUnpaddedETag() {
		String currentETag = "Foo";
		String oldEtag = "Bar";
		request.getHeaders().setIfNoneMatch(oldEtag);

		assertFalse(exchange.checkNotModified(currentETag));

		assertNull(response.getStatusCode());
		assertEquals(String.format("\"%s\"", currentETag), response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedWildcardIsIgnored() {
		String eTag = "\"Foo\"";
		request.getHeaders().setIfNoneMatch("*");

		assertFalse(exchange.checkNotModified(eTag));

		assertNull(response.getStatusCode());
		assertEquals(eTag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedETagAndTimestamp() {
		String eTag = "\"Foo\"";
		request.getHeaders().setIfNoneMatch(eTag);
		request.getHeaders().setIfModifiedSince(currentDate.toEpochMilli());

		assertTrue(exchange.checkNotModified(eTag, currentDate));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(eTag, response.getHeaders().getETag());
		assertEquals(currentDate.toEpochMilli(), response.getHeaders().getLastModified());
	}

	// SPR-14224
	@Test
	public void checkNotModifiedETagAndModifiedTimestamp() {
		String eTag = "\"Foo\"";
		request.getHeaders().setIfNoneMatch(eTag);
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		request.getHeaders().setIfModifiedSince(oneMinuteAgo.toEpochMilli());

		assertTrue(exchange.checkNotModified(eTag, currentDate));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(eTag, response.getHeaders().getETag());
		assertEquals(currentDate.toEpochMilli(), response.getHeaders().getLastModified());
	}

	@Test
	public void checkModifiedETagAndNotModifiedTimestamp() throws Exception {
		String currentETag = "\"Foo\"";
		String oldEtag = "\"Bar\"";
		request.getHeaders().setIfNoneMatch(oldEtag);
		request.getHeaders().setIfModifiedSince(currentDate.toEpochMilli());

		assertFalse(exchange.checkNotModified(currentETag, currentDate));

		assertNull(response.getStatusCode());
		assertEquals(currentETag, response.getHeaders().getETag());
		assertEquals(currentDate.toEpochMilli(), response.getHeaders().getLastModified());
	}

	@Test
	public void checkNotModifiedETagWeakStrong() {
		String eTag = "\"Foo\"";
		String weakEtag = String.format("W/%s", eTag);
		request.getHeaders().setIfNoneMatch(eTag);

		assertTrue(exchange.checkNotModified(weakEtag));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(weakEtag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedETagStrongWeak() {
		String eTag = "\"Foo\"";
		request.getHeaders().setIfNoneMatch(String.format("W/%s", eTag));

		assertTrue(exchange.checkNotModified(eTag));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(eTag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedMultipleETags() {
		String eTag = "\"Bar\"";
		String multipleETags = String.format("\"Foo\", %s", eTag);
		request.getHeaders().setIfNoneMatch(multipleETags);

		assertTrue(exchange.checkNotModified(eTag));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(eTag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedTimestampWithLengthPart() throws Exception {
		long epochTime = dateFormat.parse(CURRENT_TIME).getTime();
		request.setHttpMethod(HttpMethod.GET);
		request.setHeader("If-Modified-Since", "Wed, 09 Apr 2014 09:57:42 GMT; length=13774");

		assertTrue(exchange.checkNotModified(Instant.ofEpochMilli(epochTime)));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(epochTime, response.getHeaders().getLastModified());
	}

	@Test
	public void checkModifiedTimestampWithLengthPart() throws Exception {
		long epochTime = dateFormat.parse(CURRENT_TIME).getTime();
		request.setHttpMethod(HttpMethod.GET);
		request.setHeader("If-Modified-Since", "Tue, 08 Apr 2014 09:57:42 GMT; length=13774");

		assertFalse(exchange.checkNotModified(Instant.ofEpochMilli(epochTime)));

		assertNull(response.getStatusCode());
		assertEquals(epochTime, response.getHeaders().getLastModified());
	}

	@Test
	public void checkNotModifiedTimestampConditionalPut() throws Exception {
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		request.setHttpMethod(HttpMethod.PUT);
		request.getHeaders().setIfUnmodifiedSince(currentDate.toEpochMilli());

		assertFalse(exchange.checkNotModified(oneMinuteAgo));
		assertNull(response.getStatusCode());
		assertEquals(-1, response.getHeaders().getLastModified());
	}

	@Test
	public void checkNotModifiedTimestampConditionalPutConflict() throws Exception {
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		request.setHttpMethod(HttpMethod.PUT);
		request.getHeaders().setIfUnmodifiedSince(oneMinuteAgo.toEpochMilli());

		assertTrue(exchange.checkNotModified(currentDate));
		assertEquals(412, response.getStatusCode().value());
		assertEquals(-1, response.getHeaders().getLastModified());
	}

}
