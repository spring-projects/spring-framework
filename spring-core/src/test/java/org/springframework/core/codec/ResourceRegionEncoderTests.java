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

package org.springframework.core.codec;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.tests.TestSubscriber;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

/**
 * Test cases for {@link ResourceRegionEncoder} class.
 *
 * @author Brian Clozel
 */
public class ResourceRegionEncoderTests extends AbstractDataBufferAllocatingTestCase {

	private ResourceRegionEncoder encoder;

	private Resource resource;

	@Before
	public void setUp() {
		this.encoder = new ResourceRegionEncoder();
		String content = "Spring Framework test resource content.";
		this.resource = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
		this.bufferFactory = new DefaultDataBufferFactory();
	}

	@Test
	public void canEncode() {
		ResolvableType resourceRegion = ResolvableType.forClass(ResourceRegion.class);
		MimeType allMimeType = MimeType.valueOf("*/*");

		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Resource.class),
				MimeTypeUtils.APPLICATION_OCTET_STREAM));
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Resource.class), allMimeType));
		assertTrue(this.encoder.canEncode(resourceRegion, MimeTypeUtils.APPLICATION_OCTET_STREAM));
		assertTrue(this.encoder.canEncode(resourceRegion, allMimeType));
	}

	@Test
	public void shouldEncodeResourceRegion() throws Exception {

		ResourceRegion region = new ResourceRegion(this.resource, 0, 6);
		Flux<DataBuffer> result = this.encoder.encode(Mono.just(region), this.bufferFactory,
				ResolvableType.forClass(ResourceRegion.class), MimeTypeUtils.APPLICATION_OCTET_STREAM
				, Collections.emptyMap());

		TestSubscriber.subscribe(result)
				.assertNoError()
				.assertComplete()
				.assertValuesWith(stringConsumer("Spring"));
	}

	@Test
	public void shouldEncodeMultipleResourceRegions() throws Exception {

		Flux<ResourceRegion> regions = Flux.just(
				new ResourceRegion(this.resource, 0, 6),
				new ResourceRegion(this.resource, 7, 9),
				new ResourceRegion(this.resource, 17, 4),
				new ResourceRegion(this.resource, 22, 17)
		);
		String boundary = MimeTypeUtils.generateMultipartBoundaryString();

		Flux<DataBuffer> result = this.encoder.encode(regions, this.bufferFactory,
				ResolvableType.forClass(ResourceRegion.class),
				MimeType.valueOf("text/plain"),
				Collections.singletonMap(ResourceRegionEncoder.BOUNDARY_STRING_HINT, boundary)
		);

		Mono<DataBuffer> reduced = result
				.reduce(bufferFactory.allocateBuffer(), (previous, current) -> {
					previous.write(current);
					DataBufferUtils.release(current);
					return previous;
				});

		TestSubscriber
				.subscribe(reduced)
				.assertNoError()
				.assertComplete()
				.assertValuesWith(dataBuffer -> {
					String content = DataBufferTestUtils.dumpString(dataBuffer, StandardCharsets.UTF_8);
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

}
