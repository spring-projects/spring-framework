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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.tests.TestSubscriber;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

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

		TestSubscriber.subscribe(this.writer.write(Mono.just(region), ResolvableType.forClass(ResourceRegion.class),
				MediaType.TEXT_PLAIN, this.response, Collections.emptyMap())).assertComplete();

		assertThat(this.response.getHeaders().getContentType(), is(MediaType.TEXT_PLAIN));
		assertThat(this.response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE), is("bytes 0-5/39"));
		assertThat(this.response.getHeaders().getContentLength(), is(6L));

		Mono<String> result = reduceToString(this.response.getBody(), this.response.bufferFactory());
		TestSubscriber.subscribe(result).assertComplete().assertValues("Spring");
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

		TestSubscriber.subscribe(
				this.writer.write(regions, ResolvableType.forClass(ResourceRegion.class),
						MediaType.TEXT_PLAIN, this.response, hints))
				.assertComplete();

		HttpHeaders headers = this.response.getHeaders();
		assertThat(headers.getContentType().toString(), startsWith("multipart/byteranges;boundary=" + boundary));

		Mono<String> result = reduceToString(this.response.getBody(), this.response.bufferFactory());
		TestSubscriber
				.subscribe(result).assertNoError()
				.assertComplete()
				.assertValuesWith(content -> {
					String[] ranges = StringUtils.tokenizeToStringArray(content, "\r\n", false, true);

					assertThat(ranges[0], is("--" + boundary));
					assertThat(ranges[1], is("Content-Type: text/plain"));
					assertThat(ranges[2], is("Content-Range: bytes 0-5/39"));
					assertThat(ranges[3], is("Spring"));

					assertThat(ranges[4], is("--" + boundary));
					assertThat(ranges[5], is("Content-Type: text/plain"));
					assertThat(ranges[6], is("Content-Range: bytes 7-15/39"));
					assertThat(ranges[7], is("Framework"));

					assertThat(ranges[8], is("--" + boundary));
					assertThat(ranges[9], is("Content-Type: text/plain"));
					assertThat(ranges[10], is("Content-Range: bytes 17-20/39"));
					assertThat(ranges[11], is("test"));

					assertThat(ranges[12], is("--" + boundary));
					assertThat(ranges[13], is("Content-Type: text/plain"));
					assertThat(ranges[14], is("Content-Range: bytes 22-38/39"));
					assertThat(ranges[15], is("resource content."));

					assertThat(ranges[16], is("--" + boundary + "--"));
				});
	}

	private Mono<String> reduceToString(Publisher<DataBuffer> buffers, DataBufferFactory bufferFactory) {

		return Flux.from(buffers)
				.reduce(bufferFactory.allocateBuffer(), (previous, current) -> {
					previous.write(current);
					DataBufferUtils.release(current);
					return previous;
				})
				.map(buffer -> DataBufferTestUtils.dumpString(buffer, StandardCharsets.UTF_8));
	}

}
