/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.codec;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.get;

/**
 * Unit tests for {@link ResourceHttpMessageWriter}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class ResourceHttpMessageWriterTests {

	private static final Map<String, Object> HINTS = Collections.emptyMap();


	private final ResourceHttpMessageWriter writer = new ResourceHttpMessageWriter();

	private final MockServerHttpResponse response = new MockServerHttpResponse();

	private final Mono<Resource> input = Mono.just(new ByteArrayResource(
			"Spring Framework test resource content.".getBytes(StandardCharsets.UTF_8)));


	@Test
	public void getWritableMediaTypes() throws Exception {
		assertThat(this.writer.getWritableMediaTypes(),
				containsInAnyOrder(MimeTypeUtils.APPLICATION_OCTET_STREAM, MimeTypeUtils.ALL));
	}

	@Test
	public void writeResource() throws Exception {

		testWrite(get("/").build());

		assertThat(this.response.getHeaders().getContentType(), is(TEXT_PLAIN));
		assertThat(this.response.getHeaders().getContentLength(), is(39L));
		assertThat(this.response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES), is("bytes"));

		String content = "Spring Framework test resource content.";
		StepVerifier.create(this.response.getBodyAsString()).expectNext(content).expectComplete().verify();
	}

	@Test
	public void writeSingleRegion() throws Exception {

		testWrite(get("/").range(of(0, 5)).build());

		assertThat(this.response.getHeaders().getContentType(), is(TEXT_PLAIN));
		assertThat(this.response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE), is("bytes 0-5/39"));
		assertThat(this.response.getHeaders().getContentLength(), is(6L));

		StepVerifier.create(this.response.getBodyAsString()).expectNext("Spring").expectComplete().verify();
	}

	@Test
	public void writeMultipleRegions() throws Exception {

		testWrite(get("/").range(of(0,5), of(7,15), of(17,20), of(22,38)).build());

		HttpHeaders headers = this.response.getHeaders();
		String contentType = headers.getContentType().toString();
		String boundary = contentType.substring(30);

		assertThat(contentType, startsWith("multipart/byteranges;boundary="));

		StepVerifier.create(this.response.getBodyAsString())
				.consumeNextWith(content -> {
					String[] actualRanges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);
					String[] expected = new String[] {
							"--" + boundary,
							"Content-Type: text/plain",
							"Content-Range: bytes 0-5/39",
							"Spring",
							"--" + boundary,
							"Content-Type: text/plain",
							"Content-Range: bytes 7-15/39",
							"Framework",
							"--" + boundary,
							"Content-Type: text/plain",
							"Content-Range: bytes 17-20/39",
							"test",
							"--" + boundary,
							"Content-Type: text/plain",
							"Content-Range: bytes 22-38/39",
							"resource content.",
							"--" + boundary + "--"
					};
					assertArrayEquals(expected, actualRanges);
				})
				.expectComplete()
				.verify();
	}

	@Test
	public void invalidRange() throws Exception {

		testWrite(get("/").header(HttpHeaders.RANGE, "invalid").build());

		assertThat(this.response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES), is("bytes"));
		assertThat(this.response.getStatusCode(), is(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE));
	}


	private void testWrite(MockServerHttpRequest request) {
		Mono<Void> mono = this.writer.write(this.input, null, null, TEXT_PLAIN, request, this.response, HINTS);
		StepVerifier.create(mono).expectComplete().verify();
	}

	private static HttpRange of(int first, int last) {
		return HttpRange.createByteRange(first, last);
	}

}
