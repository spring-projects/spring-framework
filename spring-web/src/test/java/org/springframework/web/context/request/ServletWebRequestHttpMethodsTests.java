/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

	private SimpleDateFormat dateFormat;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServletWebRequest request;

	private String method;

	@Parameters
	static public Iterable<Object[]> safeMethods() {
		return Arrays.asList(new Object[][] {
				{"GET"},
				{"HEAD"}
		});
	}

	public ServletWebRequestHttpMethodsTests(String method) {
		this.method = method;
	}

	@Before
	public void setUp() {
		dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		servletRequest = new MockHttpServletRequest(method, "http://example.org");
		servletResponse = new MockHttpServletResponse();
		request = new ServletWebRequest(servletRequest, servletResponse);
	}

	@Test
	public void checkNotModifiedTimestamp() {
		long currentTime = new Date().getTime();
		servletRequest.addHeader("If-Modified-Since", currentTime);

		assertTrue(request.checkNotModified(currentTime));

		assertEquals(304, servletResponse.getStatus());
		assertEquals("" + currentTime, servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkModifiedTimestamp() {
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		servletRequest.addHeader("If-Modified-Since", oneMinuteAgo);

		assertFalse(request.checkNotModified(currentTime));

		assertEquals(200, servletResponse.getStatus());
		assertEquals("" + currentTime, servletResponse.getHeader("Last-Modified"));
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
	public void checkNotModifiedWildcardETag() {
		String eTag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", "*");

		assertTrue(request.checkNotModified(eTag));

		assertEquals(304, servletResponse.getStatus());
		assertEquals(eTag, servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedETagAndTimestamp() {
		String eTag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", eTag);
		long currentTime = new Date().getTime();
		servletRequest.addHeader("If-Modified-Since", currentTime);

		assertTrue(request.checkNotModified(eTag, currentTime));

		assertEquals(304, servletResponse.getStatus());
		assertEquals(eTag, servletResponse.getHeader("ETag"));
		assertEquals("" + currentTime, servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkNotModifiedETagAndModifiedTimestamp() {
		String eTag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", eTag);
		long currentTime = new Date().getTime();
		long oneMinuteAgo = currentTime - (1000 * 60);
		servletRequest.addHeader("If-Modified-Since", oneMinuteAgo);

		assertFalse(request.checkNotModified(eTag, currentTime));

		assertEquals(200, servletResponse.getStatus());
		assertEquals(eTag, servletResponse.getHeader("ETag"));
		assertEquals("" + currentTime, servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkModifiedETagAndNotModifiedTimestamp() {
		String currentETag = "\"Foo\"";
		String oldEtag = "\"Bar\"";
		servletRequest.addHeader("If-None-Match", oldEtag);
		long currentTime = new Date().getTime();
		servletRequest.addHeader("If-Modified-Since", currentTime);

		assertFalse(request.checkNotModified(currentETag, currentTime));

		assertEquals(200, servletResponse.getStatus());
		assertEquals(currentETag, servletResponse.getHeader("ETag"));
		assertEquals("" + currentTime, servletResponse.getHeader("Last-Modified"));
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
		long currentTime = dateFormat.parse("Wed, 09 Apr 2014 09:57:42 GMT").getTime();
		servletRequest.setMethod("GET");
		servletRequest.addHeader("If-Modified-Since", "Wed, 09 Apr 2014 09:57:42 GMT; length=13774");

		assertTrue(request.checkNotModified(currentTime));

		assertEquals(304, servletResponse.getStatus());
		assertEquals("" + currentTime, servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkModifiedTimestampWithLengthPart() throws Exception {
		long currentTime = dateFormat.parse("Wed, 09 Apr 2014 09:57:42 GMT").getTime();
		servletRequest.setMethod("GET");
		servletRequest.addHeader("If-Modified-Since", "Wed, 08 Apr 2014 09:57:42 GMT; length=13774");

		assertFalse(request.checkNotModified(currentTime));

		assertEquals(200, servletResponse.getStatus());
		assertEquals("" + currentTime, servletResponse.getHeader("Last-Modified"));
	}

}
