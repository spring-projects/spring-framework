/*
 * Copyright 2002-2022 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parameterized tests for {@link ServletWebRequest}.
 *
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Markus Malkusch
 * @author Sam Brannen
 */
class ServletWebRequestHttpMethodsTests {

	private static final String CURRENT_TIME = "Wed, 9 Apr 2014 09:57:42 GMT";

	private final MockHttpServletRequest servletRequest = new MockHttpServletRequest();

	private final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

	private final ServletWebRequest request = new ServletWebRequest(servletRequest, servletResponse);

	private final Date currentDate = new Date();


	@ParameterizedHttpMethodTest
	void checkNotModifiedNon2xxStatus(String method) {
		setUpRequest(method);

		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);
		servletResponse.setStatus(304);

		assertThat(request.checkNotModified(epochTime)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("Last-Modified")).isNull();
	}

	@ParameterizedHttpMethodTest  // SPR-13516
	void checkNotModifiedInvalidStatus(String method) {
		setUpRequest(method);

		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);
		servletResponse.setStatus(0);

		assertThat(request.checkNotModified(epochTime)).isFalse();
	}

	@ParameterizedHttpMethodTest  // SPR-14559
	void checkNotModifiedInvalidIfNoneMatchHeader(String method) {
		setUpRequest(method);

		String etag = "\"etagvalue\"";
		servletRequest.addHeader("If-None-Match", "missingquotes");
		assertThat(request.checkNotModified(etag)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedHeaderAlreadySet(String method) {
		setUpRequest(method);

		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);
		servletResponse.addHeader("Last-Modified", CURRENT_TIME);

		assertThat(request.checkNotModified(epochTime)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeaders("Last-Modified").size()).isEqualTo(1);
		assertThat(servletResponse.getHeader("Last-Modified")).isEqualTo(CURRENT_TIME);
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedTimestamp(String method) {
		setUpRequest(method);

		long epochTime = currentDate.getTime();
		servletRequest.addHeader("If-Modified-Since", epochTime);

		assertThat(request.checkNotModified(epochTime)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getDateHeader("Last-Modified") / 1000).isEqualTo(currentDate.getTime() / 1000);
	}

	@ParameterizedHttpMethodTest
	void checkModifiedTimestamp(String method) {
		setUpRequest(method);

		long oneMinuteAgo = currentDate.getTime() - (1000 * 60);
		servletRequest.addHeader("If-Modified-Since", oneMinuteAgo);

		assertThat(request.checkNotModified(currentDate.getTime())).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getDateHeader("Last-Modified") / 1000).isEqualTo(currentDate.getTime() / 1000);
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETag(String method) {
		setUpRequest(method);

		String etag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", etag);

		assertThat(request.checkNotModified(etag)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETagWithSeparatorChars(String method) {
		setUpRequest(method);

		String etag = "\"Foo, Bar\"";
		servletRequest.addHeader("If-None-Match", etag);

		assertThat(request.checkNotModified(etag)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
	}


	@ParameterizedHttpMethodTest
	void checkModifiedETag(String method) {
		setUpRequest(method);

		String currentETag = "\"Foo\"";
		String oldETag = "Bar";
		servletRequest.addHeader("If-None-Match", oldETag);

		assertThat(request.checkNotModified(currentETag)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(currentETag);
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedUnpaddedETag(String method) {
		setUpRequest(method);

		String etag = "Foo";
		String paddedETag = String.format("\"%s\"", etag);
		servletRequest.addHeader("If-None-Match", paddedETag);

		assertThat(request.checkNotModified(etag)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(paddedETag);
	}

	@ParameterizedHttpMethodTest
	void checkModifiedUnpaddedETag(String method) {
		setUpRequest(method);

		String currentETag = "Foo";
		String oldETag = "Bar";
		servletRequest.addHeader("If-None-Match", oldETag);

		assertThat(request.checkNotModified(currentETag)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(String.format("\"%s\"", currentETag));
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedWildcardIsIgnored(String method) {
		setUpRequest(method);

		String etag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", "*");

		assertThat(request.checkNotModified(etag)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETagAndTimestamp(String method) {
		setUpRequest(method);

		String etag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", etag);
		servletRequest.addHeader("If-Modified-Since", currentDate.getTime());

		assertThat(request.checkNotModified(etag, currentDate.getTime())).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
		assertThat(servletResponse.getDateHeader("Last-Modified") / 1000).isEqualTo(currentDate.getTime() / 1000);
	}

	@ParameterizedHttpMethodTest  // SPR-14224
	void checkNotModifiedETagAndModifiedTimestamp(String method) {
		setUpRequest(method);

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

	@ParameterizedHttpMethodTest
	void checkModifiedETagAndNotModifiedTimestamp(String method) {
		setUpRequest(method);

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

	@ParameterizedHttpMethodTest
	void checkNotModifiedETagWeakStrong(String method) {
		setUpRequest(method);

		String etag = "\"Foo\"";
		String weakETag = String.format("W/%s", etag);
		servletRequest.addHeader("If-None-Match", etag);

		assertThat(request.checkNotModified(weakETag)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(weakETag);
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedETagStrongWeak(String method) {
		setUpRequest(method);

		String etag = "\"Foo\"";
		servletRequest.addHeader("If-None-Match", String.format("W/%s", etag));

		assertThat(request.checkNotModified(etag)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedMultipleETags(String method) {
		setUpRequest(method);

		String etag = "\"Bar\"";
		String multipleETags = String.format("\"Foo\", %s", etag);
		servletRequest.addHeader("If-None-Match", multipleETags);

		assertThat(request.checkNotModified(etag)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getHeader("ETag")).isEqualTo(etag);
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedTimestampWithLengthPart(String method) {
		setUpRequest(method);

		long epochTime = ZonedDateTime.parse(CURRENT_TIME, RFC_1123_DATE_TIME).toInstant().toEpochMilli();
		servletRequest.setMethod("GET");
		servletRequest.addHeader("If-Modified-Since", "Wed, 09 Apr 2014 09:57:42 GMT; length=13774");

		assertThat(request.checkNotModified(epochTime)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(304);
		assertThat(servletResponse.getDateHeader("Last-Modified") / 1000).isEqualTo(epochTime / 1000);
	}

	@ParameterizedHttpMethodTest
	void checkModifiedTimestampWithLengthPart(String method) {
		setUpRequest(method);

		long epochTime = ZonedDateTime.parse(CURRENT_TIME, RFC_1123_DATE_TIME).toInstant().toEpochMilli();
		servletRequest.setMethod("GET");
		servletRequest.addHeader("If-Modified-Since", "Wed, 08 Apr 2014 09:57:42 GMT; length=13774");

		assertThat(request.checkNotModified(epochTime)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getDateHeader("Last-Modified") / 1000).isEqualTo(epochTime / 1000);
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedTimestampConditionalPut(String method) {
		setUpRequest(method);

		long currentEpoch = currentDate.getTime();
		long oneMinuteAgo = currentEpoch - (1000 * 60);
		servletRequest.setMethod("PUT");
		servletRequest.addHeader("If-UnModified-Since", currentEpoch);

		assertThat(request.checkNotModified(oneMinuteAgo)).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader("Last-Modified")).isNull();
	}

	@ParameterizedHttpMethodTest
	void checkNotModifiedTimestampConditionalPutConflict(String method) {
		setUpRequest(method);

		long currentEpoch = currentDate.getTime();
		long oneMinuteAgo = currentEpoch - (1000 * 60);
		servletRequest.setMethod("PUT");
		servletRequest.addHeader("If-UnModified-Since", oneMinuteAgo);

		assertThat(request.checkNotModified(currentEpoch)).isTrue();
		assertThat(servletResponse.getStatus()).isEqualTo(412);
		assertThat(servletResponse.getHeader("Last-Modified")).isNull();
	}

	private void setUpRequest(String method) {
		this.servletRequest.setMethod(method);
		this.servletRequest.setRequestURI("https://example.org");
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest(name = "[{index}] {0}")
	@ValueSource(strings = { "GET", "HEAD" })
	@interface ParameterizedHttpMethodTest {
	}

}
