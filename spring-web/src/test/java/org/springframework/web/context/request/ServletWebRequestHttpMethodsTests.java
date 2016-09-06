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

package org.springframework.web.context.request;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

/**
 * Parameterized tests for ServletWebRequest
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Markus Malkusch
 */
@RunWith(Parameterized.class)
public class ServletWebRequestHttpMethodsTests {

	private static final String CURRENT_TIME = "Wed, 09 Apr 2014 09:57:42 GMT";

	private SimpleDateFormat dateFormat;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServletWebRequest request;

	private Date currentDate;

	@Parameter
	public String method;

	@Parameters(name = "{0}")
	static public Iterable<Object[]> safeMethods() {
		return Arrays.asList(new Object[][] {
				{"GET"},
				{"HEAD"}
		});
	}

	@Before
	public void setUp() {
		currentDate = new Date();
		dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		servletRequest = new MockHttpServletRequest(method, "http://example.org");
		servletResponse = new MockHttpServletResponse();
		request = new ServletWebRequest(servletRequest, servletResponse);
	}

	@Test
	public void checkNotModifiedNon2xxStatus() {
		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);
		servletResponse.setStatus(304);

		assertFalse(request.checkNotModified(epochTime));
		assertEquals(304, servletResponse.getStatus());
		assertNull(servletResponse.getHeader("Last-Modified"));
	}

	// SPR-13516
	@Test
	public void checkNotModifiedInvalidStatus() {
		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);
		servletResponse.setStatus(0);

		assertFalse(request.checkNotModified(epochTime));
	}

	@Test // SPR-14559
	public void checkNotModifiedInvalidIfNoneMatchHeader() {
		String eTag = "\"etagvalue\"";
		servletRequest.addHeader("If-None-Match", "missingquotes");
		assertFalse(request.checkNotModified(eTag));
		assertEquals(200, servletResponse.getStatus());
		assertEquals(eTag, servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedHeaderAlreadySet() {
		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);
		servletResponse.addHeader("Last-Modified", CURRENT_TIME);

		assertTrue(request.checkNotModified(epochTime));
		assertEquals(304, servletResponse.getStatus());
		assertEquals(1, servletResponse.getHeaders("Last-Modified").size());
		assertEquals(CURRENT_TIME, servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkNotModifiedTimestamp() throws Exception {
		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);

		assertTrue(request.checkNotModified(epochTime));

		assertEquals(304, servletResponse.getStatus());
		assertEquals(dateFormat.format(epochTime), servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkModifiedTimestamp() {
		long oneMinuteAgo = currentDate.getTime() - (1000 * 60);
		servletRequest.addHeader("If-Modified-Since", oneMinuteAgo);

		assertFalse(request.checkNotModified(currentDate.getTime()));

		assertEquals(200, servletResponse.getStatus());
		assertEquals(dateFormat.format(currentDate.getTime()), servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkNotModifiedETag() {
		String eTag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", eTag);

		assertTrue(request.checkNotModified(eTag));

		assertEquals(304, servletResponse.getStatus());
		assertEquals(eTag, servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedETagWithSeparatorChars() {
		String eTag = "\"Foo, Bar\"";
		servletRequest.addHeader("If-None-Match", eTag);

		assertTrue(request.checkNotModified(eTag));

		assertEquals(304, servletResponse.getStatus());
		assertEquals(eTag, servletResponse.getHeader("ETag"));
	}


	@Test
	public void checkModifiedETag() {
		String currentETag = "\"Foo\"";
		String oldEtag = "Bar";
		servletRequest.addHeader("If-None-Match", oldEtag);

		assertFalse(request.checkNotModified(currentETag));

		assertEquals(200, servletResponse.getStatus());
		assertEquals(currentETag, servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedUnpaddedETag() {
		String eTag = "Foo";
		String paddedEtag = String.format("\"%s\"", eTag);
		servletRequest.addHeader("If-None-Match", paddedEtag);

		assertTrue(request.checkNotModified(eTag));

		assertEquals(304, servletResponse.getStatus());
		assertEquals(paddedEtag, servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkModifiedUnpaddedETag() {
		String currentETag = "Foo";
		String oldEtag = "Bar";
		servletRequest.addHeader("If-None-Match", oldEtag);

		assertFalse(request.checkNotModified(currentETag));

		assertEquals(200, servletResponse.getStatus());
		assertEquals(String.format("\"%s\"", currentETag), servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedWildcardIsIgnored() {
		String eTag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", "*");

		assertFalse(request.checkNotModified(eTag));

		assertEquals(200, servletResponse.getStatus());
		assertEquals(eTag, servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedETagAndTimestamp() {
		String eTag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", eTag);
		servletRequest.addHeader("If-Modified-Since", currentDate.getTime());

		assertTrue(request.checkNotModified(eTag, currentDate.getTime()));

		assertEquals(304, servletResponse.getStatus());
		assertEquals(eTag, servletResponse.getHeader("ETag"));
		assertEquals(dateFormat.format(currentDate.getTime()), servletResponse.getHeader("Last-Modified"));
	}

	// SPR-14224
	@Test
	public void checkNotModifiedETagAndModifiedTimestamp() {
		String eTag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", eTag);
		long currentEpoch = currentDate.getTime();
		long oneMinuteAgo = currentEpoch - (1000 * 60);
		servletRequest.addHeader("If-Modified-Since", oneMinuteAgo);

		assertTrue(request.checkNotModified(eTag, currentEpoch));

		assertEquals(304, servletResponse.getStatus());
		assertEquals(eTag, servletResponse.getHeader("ETag"));
		assertEquals(dateFormat.format(currentEpoch), servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkModifiedETagAndNotModifiedTimestamp() throws Exception {
		String currentETag = "\"Foo\"";
		String oldEtag = "\"Bar\"";
		servletRequest.addHeader("If-None-Match", oldEtag);
		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);

		assertFalse(request.checkNotModified(currentETag, epochTime));

		assertEquals(200, servletResponse.getStatus());
		assertEquals(currentETag, servletResponse.getHeader("ETag"));
		assertEquals(dateFormat.format(epochTime), servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkNotModifiedETagWeakStrong() {
		String eTag = "\"Foo\"";
		String weakEtag = String.format("W/%s", eTag);
		servletRequest.addHeader("If-None-Match", eTag);

		assertTrue(request.checkNotModified(weakEtag));

		assertEquals(304, servletResponse.getStatus());
		assertEquals(weakEtag, servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedETagStrongWeak() {
		String eTag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", String.format("W/%s", eTag));

		assertTrue(request.checkNotModified(eTag));

		assertEquals(304, servletResponse.getStatus());
		assertEquals(eTag, servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedMultipleETags() {
		String eTag = "\"Bar\"";
		String multipleETags = String.format("\"Foo\", %s", eTag);
		servletRequest.addHeader("If-None-Match", multipleETags);

		assertTrue(request.checkNotModified(eTag));

		assertEquals(304, servletResponse.getStatus());
		assertEquals(eTag, servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedTimestampWithLengthPart() throws Exception {
		long epochTime = dateFormat.parse(CURRENT_TIME).getTime();
		servletRequest.setMethod("GET");
		servletRequest.addHeader("If-Modified-Since", "Wed, 09 Apr 2014 09:57:42 GMT; length=13774");

		assertTrue(request.checkNotModified(epochTime));

		assertEquals(304, servletResponse.getStatus());
		assertEquals(dateFormat.format(epochTime), servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkModifiedTimestampWithLengthPart() throws Exception {
		long epochTime = dateFormat.parse(CURRENT_TIME).getTime();
		servletRequest.setMethod("GET");
		servletRequest.addHeader("If-Modified-Since", "Wed, 08 Apr 2014 09:57:42 GMT; length=13774");

		assertFalse(request.checkNotModified(epochTime));

		assertEquals(200, servletResponse.getStatus());
		assertEquals(dateFormat.format(epochTime), servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkNotModifiedTimestampConditionalPut() throws Exception {
		long currentEpoch = currentDate.getTime();
		long oneMinuteAgo = currentEpoch - (1000 * 60);
		servletRequest.setMethod("PUT");
		servletRequest.addHeader("If-UnModified-Since", currentEpoch);

		assertFalse(request.checkNotModified(oneMinuteAgo));
		assertEquals(200, servletResponse.getStatus());
		assertEquals(null, servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkNotModifiedTimestampConditionalPutConflict() throws Exception {
		long currentEpoch = currentDate.getTime();
		long oneMinuteAgo = currentEpoch - (1000 * 60);
		servletRequest.setMethod("PUT");
		servletRequest.addHeader("If-UnModified-Since", oneMinuteAgo);

		assertTrue(request.checkNotModified(currentEpoch));
		assertEquals(412, servletResponse.getStatus());
		assertEquals(null, servletResponse.getHeader("Last-Modified"));
	}

}
