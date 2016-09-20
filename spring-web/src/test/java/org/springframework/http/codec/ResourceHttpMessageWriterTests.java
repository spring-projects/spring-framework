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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.tests.TestSubscriber;
import org.springframework.util.MimeTypeUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ResourceHttpMessageWriter}.
 *
 * @author Brian Clozel
 */
public class ResourceHttpMessageWriterTests {

	private ResourceHttpMessageWriter writer = new ResourceHttpMessageWriter();

	private MockServerHttpRequest request = new MockServerHttpRequest();

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
	public void writableMediaTypes() throws Exception {
		assertThat(this.writer.getWritableMediaTypes(),
				containsInAnyOrder(MimeTypeUtils.APPLICATION_OCTET_STREAM, MimeTypeUtils.ALL));
	}

	@Test
	public void shouldWriteResource() throws Exception {
		TestSubscriber.subscribe(this.writer.write(Mono.just(resource), null, ResolvableType.forClass(Resource.class),
				MediaType.TEXT_PLAIN, this.request, this.response, Collections.emptyMap())).assertComplete();

		assertThat(this.response.getHeaders().getContentType(), is(MediaType.TEXT_PLAIN));
		assertThat(this.response.getHeaders().getContentLength(), is(39L));

		Mono<String> result = reduceToString(this.response.getBody(), this.response.bufferFactory());
		TestSubscriber.subscribe(result).assertComplete().assertValues("Spring Framework test resource content.");
	}

	@Test
	public void shouldWriteResourceRange() throws Exception {
		this.request.getHeaders().setRange(Collections.singletonList(HttpRange.createByteRange(0, 5)));

		TestSubscriber.subscribe(this.writer.write(Mono.just(resource), null, ResolvableType.forClass(Resource.class),
				MediaType.TEXT_PLAIN, this.request, this.response, Collections.emptyMap())).assertComplete();

		assertThat(this.response.getHeaders().getContentType(), is(MediaType.TEXT_PLAIN));
		assertThat(this.response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE), is("bytes 0-5/39"));
		assertThat(this.response.getHeaders().getContentLength(), is(6L));

		Mono<String> result = reduceToString(this.response.getBody(), this.response.bufferFactory());
		TestSubscriber.subscribe(result).assertComplete().assertValues("Spring");
	}

	@Test
	public void shouldThrowErrorInvalidRange() throws Exception {
		this.request.getHeaders().set(HttpHeaders.RANGE, "invalid");

		this.expectedException.expect(IllegalArgumentException.class);
		TestSubscriber.subscribe(this.writer.write(Mono.just(resource), null, ResolvableType.forClass(Resource.class),
				MediaType.TEXT_PLAIN, this.request, this.response, Collections.emptyMap()));
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
