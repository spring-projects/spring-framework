/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.codec;


import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.subscriber.Verifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ResourceRegionHttpMessageWriter}.
 * @author Brian Clozel
 */
public class ResourceRegionHttpMessageWriterTests {

	private ResourceRegionHttpMessageWriter writer = new ResourceRegionHttpMessageWriter();

	private MockServerHttpResponse response = new MockServerHttpResponse();

	private Resource resource;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();


	@Before
	public void setUp() throws Exception {
		String content = "Spring Framework test resource content.";
		this.resource = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
	}


	@Test
	public void defaultContentType() throws Exception {
		assertEquals(MimeTypeUtils.APPLICATION_OCTET_STREAM,
				this.writer.getDefaultContentType(ResolvableType.forClass(ResourceRegion.class)));
	}

	@Test
	public void writableMediaTypes() throws Exception {
		assertThat(this.writer.getWritableMediaTypes(),
				containsInAnyOrder(MimeTypeUtils.APPLICATION_OCTET_STREAM, MimeTypeUtils.ALL));
	}

	@Test
	public void shouldWriteResourceRegion() throws Exception {

		ResourceRegion region = new ResourceRegion(this.resource, 0, 6);

		Mono<Void> mono = this.writer.write(Mono.just(region), ResolvableType.forClass(ResourceRegion.class),
				MediaType.TEXT_PLAIN, this.response, Collections.emptyMap());
		Verifier.create(mono).expectComplete().verify();

		assertThat(this.response.getHeaders().getContentType(), is(MediaType.TEXT_PLAIN));
		assertThat(this.response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE), is("bytes 0-5/39"));
		assertThat(this.response.getHeaders().getContentLength(), is(6L));

		Mono<String> result = response.getBodyAsString();
		Verifier.create(result).expectNext("Spring").expectComplete().verify();
	}

	@Test
	public void shouldWriteMultipleResourceRegions() throws Exception {
		Flux<ResourceRegion> regions = Flux.just(
				new ResourceRegion(this.resource, 0, 6),
				new ResourceRegion(this.resource, 7, 9),
				new ResourceRegion(this.resource, 17, 4),
				new ResourceRegion(this.resource, 22, 17)
		);
		String boundary = MimeTypeUtils.generateMultipartBoundaryString();
		Map<String, Object> hints = new HashMap<>(1);
		hints.put(ResourceRegionHttpMessageWriter.BOUNDARY_STRING_HINT, boundary);

		Mono<Void> mono = this.writer.write(regions, ResolvableType.forClass(ResourceRegion.class),
				MediaType.TEXT_PLAIN, this.response, hints);
		Verifier.create(mono).expectComplete().verify();

		HttpHeaders headers = this.response.getHeaders();
		assertThat(headers.getContentType().toString(), startsWith("multipart/byteranges;boundary=" + boundary));

		Mono<String> result = response.getBodyAsString();

		Verifier.create(result)
				.consumeNextWith(content -> {
					String[] ranges = StringUtils
							.tokenizeToStringArray(content, "\r\n", false, true);
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
					assertArrayEquals(expected, ranges);
				})
				.expectComplete()
				.verify();
	}

}
