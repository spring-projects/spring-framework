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

import org.jetbrains.annotations.NotNull;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * "checkNotModified" unit tests for {@link DefaultServerWebExchange}.
 *
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class DefaultServerWebExchangeCheckNotModifiedTests {

	private static final String CURRENT_TIME = "Wed, 09 Apr 2014 09:57:42 GMT";


	private SimpleDateFormat dateFormat;

	private MockServerHttpRequest request;

	private MockServerHttpResponse response = new MockServerHttpResponse();

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
		this.currentDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		this.dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}


	@Test
	public void checkNotModifiedNon2xxStatus() {
		this.request = request().ifModifiedSince(this.currentDate.toEpochMilli()).build();
		this.response.setStatusCode(HttpStatus.NOT_MODIFIED);

		assertFalse(createExchange().checkNotModified(this.currentDate));
		assertEquals(304, response.getStatusCode().value());
		assertEquals(-1, response.getHeaders().getLastModified());
	}

	@Test // SPR-14559
	public void checkNotModifiedInvalidIfNoneMatchHeader() {
		String eTag = "\"etagvalue\"";
		this.request = request().ifNoneMatch("missingquotes").build();
		assertFalse(createExchange().checkNotModified(eTag));
		assertNull(response.getStatusCode());
		assertEquals(eTag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedHeaderAlreadySet() {
		this.request = request().ifModifiedSince(currentDate.toEpochMilli()).build();
		this.response.getHeaders().add("Last-Modified", CURRENT_TIME);

		assertTrue(createExchange().checkNotModified(currentDate));
		assertEquals(304, response.getStatusCode().value());
		assertEquals(1, response.getHeaders().get("Last-Modified").size());
		assertEquals(CURRENT_TIME, response.getHeaders().getFirst("Last-Modified"));
	}

	@Test
	public void checkNotModifiedTimestamp() throws Exception {
		this.request = request().ifModifiedSince(currentDate.toEpochMilli()).build();

		assertTrue(createExchange().checkNotModified(currentDate));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(currentDate.toEpochMilli(), response.getHeaders().getLastModified());
	}

	@Test
	public void checkModifiedTimestamp() {
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		this.request = request().ifModifiedSince(oneMinuteAgo.toEpochMilli()).build();

		assertFalse(createExchange().checkNotModified(currentDate));

		assertNull(response.getStatusCode());
		assertEquals(currentDate.toEpochMilli(), response.getHeaders().getLastModified());
	}

	@Test
	public void checkNotModifiedETag() {
		String eTag = "\"Foo\"";
		this.request = request().ifNoneMatch(eTag).build();

		assertTrue(createExchange().checkNotModified(eTag));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(eTag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedETagWithSeparatorChars() {
		String eTag = "\"Foo, Bar\"";
		this.request = request().ifNoneMatch(eTag).build();

		assertTrue(createExchange().checkNotModified(eTag));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(eTag, response.getHeaders().getETag());
	}


	@Test
	public void checkModifiedETag() {
		String currentETag = "\"Foo\"";
		String oldEtag = "Bar";
		this.request = request().ifNoneMatch(oldEtag).build();

		assertFalse(createExchange().checkNotModified(currentETag));

		assertNull(response.getStatusCode());
		assertEquals(currentETag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedUnpaddedETag() {
		String eTag = "Foo";
		String paddedEtag = String.format("\"%s\"", eTag);
		this.request = request().ifNoneMatch(paddedEtag).build();

		assertTrue(createExchange().checkNotModified(eTag));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(paddedEtag, response.getHeaders().getETag());
	}

	@Test
	public void checkModifiedUnpaddedETag() {
		String currentETag = "Foo";
		String oldEtag = "Bar";
		this.request = request().ifNoneMatch(oldEtag).build();

		assertFalse(createExchange().checkNotModified(currentETag));

		assertNull(response.getStatusCode());
		assertEquals(String.format("\"%s\"", currentETag), response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedWildcardIsIgnored() {
		String eTag = "\"Foo\"";
		this.request = request().ifNoneMatch("*").build();
		assertFalse(createExchange().checkNotModified(eTag));

		assertNull(response.getStatusCode());
		assertEquals(eTag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedETagAndTimestamp() {
		String eTag = "\"Foo\"";
		this.request = request().ifNoneMatch(eTag).ifModifiedSince(currentDate.toEpochMilli()).build();

		assertTrue(createExchange().checkNotModified(eTag, currentDate));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(eTag, response.getHeaders().getETag());
		assertEquals(currentDate.toEpochMilli(), response.getHeaders().getLastModified());
	}

	// SPR-14224
	@Test
	public void checkNotModifiedETagAndModifiedTimestamp() {
		String eTag = "\"Foo\"";
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		this.request = request().ifNoneMatch(eTag).ifModifiedSince(oneMinuteAgo.toEpochMilli()).build();

		assertTrue(createExchange().checkNotModified(eTag, currentDate));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(eTag, response.getHeaders().getETag());
		assertEquals(currentDate.toEpochMilli(), response.getHeaders().getLastModified());
	}

	@Test
	public void checkModifiedETagAndNotModifiedTimestamp() throws Exception {
		String currentETag = "\"Foo\"";
		String oldEtag = "\"Bar\"";
		this.request = request().ifNoneMatch(oldEtag).ifModifiedSince(currentDate.toEpochMilli()).build();

		assertFalse(createExchange().checkNotModified(currentETag, currentDate));

		assertNull(response.getStatusCode());
		assertEquals(currentETag, response.getHeaders().getETag());
		assertEquals(currentDate.toEpochMilli(), response.getHeaders().getLastModified());
	}

	@Test
	public void checkNotModifiedETagWeakStrong() {
		String eTag = "\"Foo\"";
		String weakEtag = String.format("W/%s", eTag);
		this.request = request().ifNoneMatch(eTag).build();

		assertTrue(createExchange().checkNotModified(weakEtag));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(weakEtag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedETagStrongWeak() {
		String eTag = "\"Foo\"";
		this.request = request().ifNoneMatch(String.format("W/%s", eTag)).build();

		assertTrue(createExchange().checkNotModified(eTag));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(eTag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedMultipleETags() {
		String eTag = "\"Bar\"";
		String multipleETags = String.format("\"Foo\", %s", eTag);
		this.request = request().ifNoneMatch(multipleETags).build();

		assertTrue(createExchange().checkNotModified(eTag));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(eTag, response.getHeaders().getETag());
	}

	@Test
	public void checkNotModifiedTimestampWithLengthPart() throws Exception {
		long epochTime = dateFormat.parse(CURRENT_TIME).getTime();
		this.request = request().header("If-Modified-Since", "Wed, 09 Apr 2014 09:57:42 GMT; length=13774").build();

		assertTrue(createExchange().checkNotModified(Instant.ofEpochMilli(epochTime)));

		assertEquals(304, response.getStatusCode().value());
		assertEquals(epochTime, response.getHeaders().getLastModified());
	}

	@Test
	public void checkModifiedTimestampWithLengthPart() throws Exception {
		long epochTime = dateFormat.parse(CURRENT_TIME).getTime();
		this.request = request().header("If-Modified-Since", "Tue, 08 Apr 2014 09:57:42 GMT; length=13774").build();

		assertFalse(createExchange().checkNotModified(Instant.ofEpochMilli(epochTime)));

		assertNull(response.getStatusCode());
		assertEquals(epochTime, response.getHeaders().getLastModified());
	}

	@Test
	public void checkNotModifiedTimestampConditionalPut() throws Exception {
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		long millis = currentDate.toEpochMilli();
		this.request = MockServerHttpRequest.put("http://example.org").ifUnmodifiedSince(millis).build();

		assertFalse(createExchange().checkNotModified(oneMinuteAgo));
		assertNull(response.getStatusCode());
		assertEquals(-1, response.getHeaders().getLastModified());
	}

	@Test
	public void checkNotModifiedTimestampConditionalPutConflict() throws Exception {
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		long millis = oneMinuteAgo.toEpochMilli();
		this.request = MockServerHttpRequest.put("http://example.org").ifUnmodifiedSince(millis).build();

		assertTrue(createExchange().checkNotModified(currentDate));
		assertEquals(412, response.getStatusCode().value());
		assertEquals(-1, response.getHeaders().getLastModified());
	}

	@NotNull
	private MockServerHttpRequest.BaseBuilder<?> request() {
		return MockServerHttpRequest.get("http://example.org");
	}

	@NotNull
	private DefaultServerWebExchange createExchange() {
		return new DefaultServerWebExchange(this.request, this.response);
	}

}
