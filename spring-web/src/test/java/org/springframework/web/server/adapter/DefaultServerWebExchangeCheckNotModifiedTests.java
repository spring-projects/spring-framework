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

package org.springframework.web.server.adapter;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * "checkNotModified" unit tests for {@link DefaultServerWebExchange}.
 *
 * @author Rossen Stoyanchev
 */
class DefaultServerWebExchangeCheckNotModifiedTests {

	private static final String CURRENT_TIME = "Wed, 09 Apr 2014 09:57:42 GMT";

	private final Instant currentDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);

	private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);


	@BeforeEach
	void setup() {
		this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}


	@Test
	void checkNotModifiedNon2xxStatus() {
		MockServerHttpRequest request = get("/").ifModifiedSince(this.currentDate.toEpochMilli()).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		exchange.getResponse().setStatusCode(HttpStatus.NOT_MODIFIED);

		assertThat(exchange.checkNotModified(this.currentDate)).isFalse();
		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(304);
		assertThat(exchange.getResponse().getHeaders().getLastModified()).isEqualTo(-1);
	}

	@Test // SPR-14559
	void checkNotModifiedInvalidIfNoneMatchHeader() {
		String eTag = "\"etagvalue\"";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch("missingquotes"));
		assertThat(exchange.checkNotModified(eTag)).isFalse();
		assertThat(exchange.getResponse().getStatusCode()).isNull();
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
	}

	@Test
	void checkNotModifiedHeaderAlreadySet() {
		MockServerHttpRequest request = get("/").ifModifiedSince(currentDate.toEpochMilli()).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		exchange.getResponse().getHeaders().add("Last-Modified", CURRENT_TIME);

		assertThat(exchange.checkNotModified(currentDate)).isTrue();
		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(304);
		assertThat(exchange.getResponse().getHeaders().get("Last-Modified").size()).isEqualTo(1);
		assertThat(exchange.getResponse().getHeaders().getFirst("Last-Modified")).isEqualTo(CURRENT_TIME);
	}

	@Test
	void checkNotModifiedTimestamp() throws Exception {
		MockServerHttpRequest request = get("/").ifModifiedSince(currentDate.toEpochMilli()).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(currentDate)).isTrue();

		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(304);
		assertThat(exchange.getResponse().getHeaders().getLastModified()).isEqualTo(currentDate.toEpochMilli());
	}

	@Test
	void checkModifiedTimestamp() {
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		MockServerHttpRequest request = get("/").ifModifiedSince(oneMinuteAgo.toEpochMilli()).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(currentDate)).isFalse();

		assertThat(exchange.getResponse().getStatusCode()).isNull();
		assertThat(exchange.getResponse().getHeaders().getLastModified()).isEqualTo(currentDate.toEpochMilli());
	}

	@Test
	void checkNotModifiedETag() {
		String eTag = "\"Foo\"";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(eTag));

		assertThat(exchange.checkNotModified(eTag)).isTrue();

		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(304);
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
	}

	@Test
	void checkNotModifiedETagWithSeparatorChars() {
		String eTag = "\"Foo, Bar\"";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(eTag));

		assertThat(exchange.checkNotModified(eTag)).isTrue();

		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(304);
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
	}

	@Test
	void checkModifiedETag() {
		String currentETag = "\"Foo\"";
		String oldEtag = "Bar";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(oldEtag));

		assertThat(exchange.checkNotModified(currentETag)).isFalse();

		assertThat(exchange.getResponse().getStatusCode()).isNull();
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(currentETag);
	}

	@Test
	void checkNotModifiedUnpaddedETag() {
		String eTag = "Foo";
		String paddedEtag = String.format("\"%s\"", eTag);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(paddedEtag));

		assertThat(exchange.checkNotModified(eTag)).isTrue();

		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(304);
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(paddedEtag);
	}

	@Test
	void checkModifiedUnpaddedETag() {
		String currentETag = "Foo";
		String oldEtag = "Bar";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(oldEtag));

		assertThat(exchange.checkNotModified(currentETag)).isFalse();

		assertThat(exchange.getResponse().getStatusCode()).isNull();
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(String.format("\"%s\"", currentETag));
	}

	@Test
	void checkNotModifiedWildcardIsIgnored() {
		String eTag = "\"Foo\"";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch("*"));
		assertThat(exchange.checkNotModified(eTag)).isFalse();

		assertThat(exchange.getResponse().getStatusCode()).isNull();
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
	}

	@Test
	void checkNotModifiedETagAndTimestamp() {
		String eTag = "\"Foo\"";
		long time = currentDate.toEpochMilli();
		MockServerHttpRequest request = get("/").ifNoneMatch(eTag).ifModifiedSince(time).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(eTag, currentDate)).isTrue();

		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(304);
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
		assertThat(exchange.getResponse().getHeaders().getLastModified()).isEqualTo(time);
	}

	// SPR-14224
	@Test
	void checkNotModifiedETagAndModifiedTimestamp() {
		String eTag = "\"Foo\"";
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/")
				.ifNoneMatch(eTag)
				.ifModifiedSince(oneMinuteAgo.toEpochMilli())
				);

		assertThat(exchange.checkNotModified(eTag, currentDate)).isTrue();

		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(304);
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
		assertThat(exchange.getResponse().getHeaders().getLastModified()).isEqualTo(currentDate.toEpochMilli());
	}

	@Test
	void checkModifiedETagAndNotModifiedTimestamp() throws Exception {
		String currentETag = "\"Foo\"";
		String oldEtag = "\"Bar\"";
		long time = currentDate.toEpochMilli();
		MockServerHttpRequest request = get("/").ifNoneMatch(oldEtag).ifModifiedSince(time).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(currentETag, currentDate)).isFalse();

		assertThat(exchange.getResponse().getStatusCode()).isNull();
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(currentETag);
		assertThat(exchange.getResponse().getHeaders().getLastModified()).isEqualTo(time);
	}

	@Test
	void checkNotModifiedETagWeakStrong() {
		String eTag = "\"Foo\"";
		String weakEtag = String.format("W/%s", eTag);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(eTag));

		assertThat(exchange.checkNotModified(weakEtag)).isTrue();

		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(304);
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(weakEtag);
	}

	@Test
	void checkNotModifiedETagStrongWeak() {
		String eTag = "\"Foo\"";
		MockServerHttpRequest request = get("/").ifNoneMatch(String.format("W/%s", eTag)).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(eTag)).isTrue();

		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(304);
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
	}

	@Test
	void checkNotModifiedMultipleETags() {
		String eTag = "\"Bar\"";
		String multipleETags = String.format("\"Foo\", %s", eTag);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/").ifNoneMatch(multipleETags));

		assertThat(exchange.checkNotModified(eTag)).isTrue();

		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(304);
		assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(eTag);
	}

	@Test
	void checkNotModifiedTimestampWithLengthPart() throws Exception {
		long epochTime = dateFormat.parse(CURRENT_TIME).getTime();
		String header = "Wed, 09 Apr 2014 09:57:42 GMT; length=13774";
		MockServerHttpRequest request = get("/").header("If-Modified-Since", header).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(Instant.ofEpochMilli(epochTime))).isTrue();

		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(304);
		assertThat(exchange.getResponse().getHeaders().getLastModified()).isEqualTo(epochTime);
	}

	@Test
	void checkModifiedTimestampWithLengthPart() throws Exception {
		long epochTime = dateFormat.parse(CURRENT_TIME).getTime();
		String header = "Tue, 08 Apr 2014 09:57:42 GMT; length=13774";
		MockServerHttpRequest request = get("/").header("If-Modified-Since", header).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(Instant.ofEpochMilli(epochTime))).isFalse();

		assertThat(exchange.getResponse().getStatusCode()).isNull();
		assertThat(exchange.getResponse().getHeaders().getLastModified()).isEqualTo(epochTime);
	}

	@Test
	void checkNotModifiedTimestampConditionalPut() throws Exception {
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		long millis = currentDate.toEpochMilli();
		MockServerHttpRequest request = MockServerHttpRequest.put("/").ifUnmodifiedSince(millis).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(oneMinuteAgo)).isFalse();
		assertThat(exchange.getResponse().getStatusCode()).isNull();
		assertThat(exchange.getResponse().getHeaders().getLastModified()).isEqualTo(-1);
	}

	@Test
	void checkNotModifiedTimestampConditionalPutConflict() throws Exception {
		Instant oneMinuteAgo = currentDate.minusSeconds(60);
		long millis = oneMinuteAgo.toEpochMilli();
		MockServerHttpRequest request = MockServerHttpRequest.put("/").ifUnmodifiedSince(millis).build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		assertThat(exchange.checkNotModified(currentDate)).isTrue();
		assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(412);
		assertThat(exchange.getResponse().getHeaders().getLastModified()).isEqualTo(-1);
	}

}
