/*
 * Copyright 2002-2024 the original author or authors.
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
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
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

	private static final Instant NOW = Instant.now();

	private final MockHttpServletRequest servletRequest = new MockHttpServletRequest();

	private final MockHttpServletResponse servletResponse = new MockHttpServletResponse();

	private final ServletWebRequest request = new ServletWebRequest(servletRequest, servletResponse);


	@Test
	void ifMatchWildcardShouldMatchWhenETagPresent() {
		setUpRequest("PUT");
		servletRequest.addHeader(HttpHeaders.IF_MATCH, "*");
		assertThat(request.checkNotModified("\"SomeETag\"")).isFalse();
	}

	@Test
	void ifMatchWildcardShouldMatchETagMissing() {
		setUpRequest("PUT");
		servletRequest.addHeader(HttpHeaders.IF_MATCH, "*");
		assertThat(request.checkNotModified("")).isTrue();
		assertPreconditionFailed();
	}

	@Test
	void ifMatchValueShouldMatchWhenETagMatches() {
		setUpRequest("PUT");
		servletRequest.addHeader(HttpHeaders.IF_MATCH, "\"first\"");
		servletRequest.addHeader(HttpHeaders.IF_MATCH, "\"second\"");
		assertThat(request.checkNotModified("\"second\"")).isFalse();
	}

	@Test
	void ifMatchValueShouldRejectWhenETagDoesNotMatch() {
		setUpRequest("PUT");
		servletRequest.addHeader(HttpHeaders.IF_MATCH, "\"first\"");
		assertThat(request.checkNotModified("\"second\"")).isTrue();
		assertPreconditionFailed();
	}

	@Test
	void ifMatchValueShouldUseStrongComparison() {
		setUpRequest("PUT");
		String eTag = "\"spring\"";
		servletRequest.addHeader(HttpHeaders.IF_MATCH, "W/" + eTag);
		assertThat(request.checkNotModified(eTag)).isTrue();
		assertPreconditionFailed();
	}

	@SafeHttpMethodsTest
	void ifMatchShouldOnlyBeConsideredForUnsafeMethods(String method) {
		setUpRequest(method);
		servletRequest.addHeader(HttpHeaders.IF_MATCH, "*");
		assertThat(request.checkNotModified("\"spring\"")).isFalse();
	}

	@Test
	void ifUnModifiedSinceShouldMatchValueWhenLater() {
		setUpRequest("PUT");
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
		servletRequest.addHeader(HttpHeaders.IF_UNMODIFIED_SINCE, now.toEpochMilli());
		assertThat(request.checkNotModified(oneMinuteAgo.toEpochMilli())).isFalse();
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader(HttpHeaders.LAST_MODIFIED)).isNull();
	}

	@Test
	void ifUnModifiedSinceShouldNotMatchValueWhenEarlier() {
		setUpRequest("PUT");
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
		servletRequest.addHeader(HttpHeaders.IF_UNMODIFIED_SINCE, oneMinuteAgo.toEpochMilli());
		assertThat(request.checkNotModified(now.toEpochMilli())).isTrue();
		assertPreconditionFailed();
	}

	@SafeHttpMethodsTest
	void ifUnModifiedSinceShouldSetHeadersWithSafeMethod(String method) {
		setUpRequest(method);
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
		servletRequest.addHeader(HttpHeaders.IF_UNMODIFIED_SINCE, now.toEpochMilli());
		assertThat(request.checkNotModified(oneMinuteAgo.toEpochMilli())).isFalse();
		assertOkWithLastModified(oneMinuteAgo);
	}

	@SafeHttpMethodsTest
	void ifNoneMatchShouldMatchIdenticalETagValue(String method) {
		setUpRequest(method);
		String etag = "\"spring\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
		assertThat(request.checkNotModified(etag)).isTrue();
		assertNotModified(etag, null);
	}

	@SafeHttpMethodsTest
	void ifNoneMatchShouldMatchETagWithSeparatorChar(String method) {
		setUpRequest(method);
		String etag = "\"spring,framework\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
		assertThat(request.checkNotModified(etag)).isTrue();
		assertNotModified(etag, null);
	}

	@SafeHttpMethodsTest
	void ifNoneMatchShouldNotMatchDifferentETag(String method) {
		setUpRequest(method);
		String etag = "\"framework\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "\"spring\"");
		assertThat(request.checkNotModified(etag)).isFalse();
		assertOkWithETag(etag);
	}

	// gh-19127
	@SafeHttpMethodsTest
	void ifNoneMatchShouldNotFailForUnquotedETag(String method) {
		setUpRequest(method);
		String etag = "\"etagvalue\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "missingquotes");
		assertThat(request.checkNotModified(etag)).isFalse();
		assertOkWithETag(etag);
	}

	@SafeHttpMethodsTest
	void ifNoneMatchShouldMatchPaddedETag(String method) {
		setUpRequest(method);
		String etag = "spring";
		String paddedEtag = String.format("\"%s\"", etag);
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, paddedEtag);
		assertThat(request.checkNotModified(etag)).isTrue();
		assertNotModified(paddedEtag, null);
	}

	@SafeHttpMethodsTest
	void ifNoneMatchShouldIgnoreWildcard(String method) {
		setUpRequest(method);
		String etag = "\"spring\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "*");
		assertThat(request.checkNotModified(etag)).isFalse();
		assertOkWithETag(etag);
	}

	@Test
	void ifNoneMatchShouldRejectWildcardForUnsafeMethods() {
		setUpRequest("PUT");
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "*");
		assertThat(request.checkNotModified("\"spring\"")).isTrue();
		assertPreconditionFailed();
	}

	@SafeHttpMethodsTest
	void ifNoneMatchValueShouldUseWeakComparison(String method) {
		setUpRequest(method);
		String etag = "\"spring\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "W/" + etag);
		assertThat(request.checkNotModified(etag)).isTrue();
		assertNotModified(etag, null);
	}

	@SafeHttpMethodsTest
	void ifModifiedSinceShouldMatchIfDatesEqual(String method) {
		setUpRequest(method);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, NOW.toEpochMilli());
		assertThat(request.checkNotModified(NOW.toEpochMilli())).isTrue();
		assertNotModified(null, NOW);
	}

	@SafeHttpMethodsTest
	void ifModifiedSinceShouldNotMatchIfDateAfter(String method) {
		setUpRequest(method);
		Instant oneMinuteLater = NOW.plus(1, ChronoUnit.MINUTES);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, NOW.toEpochMilli());
		assertThat(request.checkNotModified(oneMinuteLater.toEpochMilli())).isFalse();
		assertOkWithLastModified(oneMinuteLater);
	}

	@SafeHttpMethodsTest
	void ifModifiedSinceShouldNotOverrideResponseStatus(String method) {
		setUpRequest(method);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, NOW.toEpochMilli());
		servletResponse.setStatus(304);
		assertThat(request.checkNotModified(NOW.toEpochMilli())).isFalse();
		assertNotModified(null, null);
	}

	@SafeHttpMethodsTest
		// SPR-13516
	void ifModifiedSinceShouldNotFailForInvalidResponseStatus(String method) {
		setUpRequest(method);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, NOW.toEpochMilli());
		servletResponse.setStatus(0);
		assertThat(request.checkNotModified(NOW.toEpochMilli())).isFalse();
	}

	@SafeHttpMethodsTest
	void ifModifiedSinceShouldNotFailForTimestampWithLengthPart(String method) {
		setUpRequest(method);
		long epochTime = ZonedDateTime.parse(CURRENT_TIME, RFC_1123_DATE_TIME).toInstant().toEpochMilli();
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, "Wed, 09 Apr 2014 09:57:42 GMT; length=13774");

		assertThat(request.checkNotModified(epochTime)).isTrue();
		assertNotModified(null, Instant.ofEpochMilli(epochTime));
	}

	@SafeHttpMethodsTest
	void IfNoneMatchAndIfNotModifiedSinceShouldMatchWhenSameETagAndDate(String method) {
		setUpRequest(method);
		String etag = "\"spring\"";
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, NOW.toEpochMilli());
		assertThat(request.checkNotModified(etag, NOW.toEpochMilli())).isTrue();
		assertNotModified(etag, NOW);
	}

	@SafeHttpMethodsTest
	void IfNoneMatchAndIfNotModifiedSinceShouldMatchWhenSameETagAndLaterDate(String method) {
		setUpRequest(method);
		String etag = "\"spring\"";
		Instant oneMinuteLater = NOW.plus(1, ChronoUnit.MINUTES);
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteLater.toEpochMilli());
		assertThat(request.checkNotModified(etag, NOW.toEpochMilli())).isTrue();
		assertNotModified(etag, NOW);
	}

	@SafeHttpMethodsTest
	void IfNoneMatchAndIfNotModifiedSinceShouldNotMatchWhenDifferentETag(String method) {
		setUpRequest(method);
		String etag = "\"framework\"";
		Instant oneMinuteLater = NOW.plus(1, ChronoUnit.MINUTES);
		servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "\"spring\"");
		servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteLater.toEpochMilli());
		assertThat(request.checkNotModified(etag, NOW.toEpochMilli())).isFalse();
		assertOkWithETag(etag);
		assertOkWithLastModified(NOW);
	}


	private void setUpRequest(String method) {
		this.servletRequest.setMethod(method);
		this.servletRequest.setRequestURI("https://example.org");
	}

	private void assertPreconditionFailed() {
		assertThat(this.servletResponse.getStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED.value());
	}

	private void assertNotModified(@Nullable String eTag, @Nullable Instant lastModified) {
		assertThat(this.servletResponse.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
		if (eTag != null) {
			assertThat(servletResponse.getHeader(HttpHeaders.ETAG)).isEqualTo(eTag);
		}
		if (lastModified != null) {
			assertThat(servletResponse.getDateHeader(HttpHeaders.LAST_MODIFIED) / 1000)
					.isEqualTo(lastModified.toEpochMilli() / 1000);
		}
	}

	private void assertOkWithETag(String eTag) {
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getHeader(HttpHeaders.ETAG)).isEqualTo(eTag);
	}

	private void assertOkWithLastModified(Instant lastModified) {
		assertThat(servletResponse.getStatus()).isEqualTo(200);
		assertThat(servletResponse.getDateHeader(HttpHeaders.LAST_MODIFIED) / 1000)
				.isEqualTo(lastModified.toEpochMilli() / 1000);
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest(name = "[{index}] {0}")
	@ValueSource(strings = {"GET", "HEAD"})
	@interface SafeHttpMethodsTest {
	}

}
