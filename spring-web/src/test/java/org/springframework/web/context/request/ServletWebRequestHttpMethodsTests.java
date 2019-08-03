/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.request;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parameterized tests for {@link ServletWebRequest}.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Markus Malkusch
 */
@RunWith(Parameterized.class)
public class ServletWebRequestHttpMethodsTests {

	private static final String CURRENT_TIME = "Wed, 9 Apr 2014 09:57:42 GMT";

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServletWebRequest request;

	private Date currentDate;

	@Parameter
	public String method;

	@Parameters(name = "{0}")
	static public Iterable<Object[]> safeMethods() {
		return Arrays.asList(new Object[][] {
				{"GET"}, {"HEAD"}
		});
	}


	@Before
	public void setup() {
		currentDate = new Date();
		servletRequest = new MockHttpServletRequest(method, "https://example.org");
		servletResponse = new MockHttpServletResponse();
		request = new ServletWebRequest(servletRequest, servletResponse);
	}


	@Test
	public void checkNotModifiedNon2xxStatus() {
		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);
		servletResponse.setStatus(304);

		assertThat(request.checkNotModified(epochTime)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("Last-Modified")).isNull();
	}

	@Test  // SPR-13516
	public void checkNotModifiedInvalidStatus() {
		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);
		servletResponse.setStatus(0);

		assertThat(request.checkNotModified(epochTime)).isFalse();
	}

	@Test  // SPR-14559
	public void checkNotModifiedInvalidIfNoneMatchHeader() {
		String etag = "\"etagvalue\"";
		servletRequest.addHeader("If-None-Match", "missingquotes");
		assertThat(request.checkNotModified(etag)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
	}

	@Test
	public void checkNotModifiedHeaderAlreadySet() {
		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);
		servletResponse.addHeader("Last-Modified", CURRENT_TIME);

		assertThat(request.checkNotModified(epochTime)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeaders("Last-Modified").size()).isEqualTo(1);
		assertThat(servletResponse.getHeader("Last-Modified")).isEqualTo(CURRENT_TIME);
	}

	@Test
	public void checkNotModifiedTimestamp() {
		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);

		assertThat(request.checkNotModified(epochTime)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getDateHeader("Last-Modified") / 1000).isEqualTo(currentDate.getTime() / 1000);
	}

	@Test
	public void checkModifiedTimestamp() {
		long oneMinuteAgo = currentDate.getTime() - (1000 * 60);
		servletRequest.addHeader("If-Modified-Since", oneMinuteAgo);

		assertThat(request.checkNotModified(currentDate.getTime())).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getDateHeader("Last-Modified") / 1000).isEqualTo(currentDate.getTime() / 1000);
	}

	@Test
	public void checkNotModifiedETag() {
		String etag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", etag);

		assertThat(request.checkNotModified(etag)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
	}

	@Test
	public void checkNotModifiedETagWithSeparatorChars() {
		String etag = "\"Foo, Bar\"";
		servletRequest.addHeader("If-None-Match", etag);

		assertThat(request.checkNotModified(etag)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
	}


	@Test
	public void checkModifiedETag() {
		String currentETag = "\"Foo\"";
		String oldETag = "Bar";
		servletRequest.addHeader("If-None-Match", oldETag);

		assertThat(request.checkNotModified(currentETag)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(currentETag);
	}

	@Test
	public void checkNotModifiedUnpaddedETag() {
		String etag = "Foo";
		String paddedETag = String.format("\"%s\"", etag);
		servletRequest.addHeader("If-None-Match", paddedETag);

		assertThat(request.checkNotModified(etag)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(paddedETag);
	}

	@Test
	public void checkModifiedUnpaddedETag() {
		String currentETag = "Foo";
		String oldETag = "Bar";
		servletRequest.addHeader("If-None-Match", oldETag);

		assertThat(request.checkNotModified(currentETag)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(String.format("\"%s\"", currentETag));
	}

	@Test
	public void checkNotModifiedWildcardIsIgnored() {
		String etag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", "*");

		assertThat(request.checkNotModified(etag)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
	}

	@Test
	public void checkNotModifiedETagAndTimestamp() {
		String etag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", etag);
		servletRequest.addHeader("If-Modified-Since", currentDate.getTime());

		assertThat(request.checkNotModified(etag, currentDate.getTime())).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
		assertThat(servletResponse.getDateHeader("Last-Modified") / 1000).isEqualTo(currentDate.getTime() / 1000);
	}

	@Test  // SPR-14224
	public void checkNotModifiedETagAndModifiedTimestamp() {
		String etag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", etag);
		long currentEpoch = currentDate.getTime();
		long oneMinuteAgo = currentEpoch - (1000 * 60);
		servletRequest.addHeader("If-Modified-Since", oneMinuteAgo);

		assertThat(request.checkNotModified(etag, currentEpoch)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
		assertThat(servletResponse.getDateHeader("Last-Modified") / 1000).isEqualTo(currentDate.getTime() / 1000);
	}

	@Test
	public void checkModifiedETagAndNotModifiedTimestamp() {
		String currentETag = "\"Foo\"";
		String oldETag = "\"Bar\"";
		servletRequest.addHeader("If-None-Match", oldETag);
		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);

		assertThat(request.checkNotModified(currentETag, epochTime)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(currentETag);
		assertThat(servletResponse.getDateHeader("Last-Modified") / 1000).isEqualTo(currentDate.getTime() / 1000);
	}

	@Test
	public void checkNotModifiedETagWeakStrong() {
		String etag = "\"Foo\"";
		String weakETag = String.format("W/%s", etag);
		servletRequest.addHeader("If-None-Match", etag);

		assertThat(request.checkNotModified(weakETag)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(weakETag);
	}

	@Test
	public void checkNotModifiedETagStrongWeak() {
		String etag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", String.format("W/%s", etag));

		assertThat(request.checkNotModified(etag)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
	}

	@Test
	public void checkNotModifiedMultipleETags() {
		String etag = "\"Bar\"";
		String multipleETags = String.format("\"Foo\", %s", etag);
		servletRequest.addHeader("If-None-Match", multipleETags);

		assertThat(request.checkNotModified(etag)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
	}

	@Test
	public void checkNotModifiedTimestampWithLengthPart() {
		long epochTime = ZonedDateTime.parse(CURRENT_TIME, RFC_1123_DATE_TIME).toInstant().toEpochMilli();
		servletRequest.setMethod("GET");
		servletRequest.addHeader("If-Modified-Since", "Wed, 09 Apr 2014 09:57:42 GMT; length=13774");

		assertThat(request.checkNotModified(epochTime)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getDateHeader("Last-Modified") / 1000).isEqualTo(epochTime / 1000);
	}

	@Test
	public void checkModifiedTimestampWithLengthPart() {
		long epochTime = ZonedDateTime.parse(CURRENT_TIME, RFC_1123_DATE_TIME).toInstant().toEpochMilli();
		servletRequest.setMethod("GET");
		servletRequest.addHeader("If-Modified-Since", "Wed, 08 Apr 2014 09:57:42 GMT; length=13774");

		assertThat(request.checkNotModified(epochTime)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getDateHeader("Last-Modified") / 1000).isEqualTo(epochTime / 1000);
	}

	@Test
	public void checkNotModifiedTimestampConditionalPut() {
		long currentEpoch = currentDate.getTime();
		long oneMinuteAgo = currentEpoch - (1000 * 60);
		servletRequest.setMethod("PUT");
		servletRequest.addHeader("If-UnModified-Since", currentEpoch);

		assertThat(request.checkNotModified(oneMinuteAgo)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader("Last-Modified")).isEqualTo(null);
	}

	@Test
	public void checkNotModifiedTimestampConditionalPutConflict() {
		long currentEpoch = currentDate.getTime();
		long oneMinuteAgo = currentEpoch - (1000 * 60);
		servletRequest.setMethod("PUT");
		servletRequest.addHeader("If-UnModified-Since", oneMinuteAgo);

		assertThat(request.checkNotModified(currentEpoch)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(412);
		assertThat(servletResponse.getHeader("Last-Modified")).isEqualTo(null);
	}

}
