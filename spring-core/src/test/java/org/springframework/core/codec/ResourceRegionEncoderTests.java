/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Collections;
import java.util.function.Consumer;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.LeakAwareDataBufferFactory;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

/**
 * Test cases for {@link ResourceRegionEncoder} class.
 *
 * @author Brian Clozel
 */
public class ResourceRegionEncoderTests  {

	private ResourceRegionEncoder encoder = new ResourceRegionEncoder();

	private DataBufferFactory bufferFactory = new LeakAwareDataBufferFactory();


	@Test
	public void canEncode() {
		ResolvableType resourceRegion = ResolvableType.forClass(ResourceRegion.class);
		MimeType allMimeType = MimeType.valueOf("*/*");

		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Resource.class),
				MimeTypeUtils.APPLICATION_OCTET_STREAM));
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Resource.class), allMimeType));
		assertTrue(this.encoder.canEncode(resourceRegion, MimeTypeUtils.APPLICATION_OCTET_STREAM));
		assertTrue(this.encoder.canEncode(resourceRegion, allMimeType));

		// SPR-15464
		assertFalse(this.encoder.canEncode(ResolvableType.NONE, null));
	}

	@Test
	public void shouldEncodeResourceRegionFileResource() throws Exception {
		ResourceRegion region = new ResourceRegion(
				new ClassPathResource("ResourceRegionEncoderTests.txt", getClass()), 0, 6);
		Flux<DataBuffer> result = this.encoder.encode(Mono.just(region), this.bufferFactory,
				ResolvableType.forClass(ResourceRegion.class),
				MimeTypeUtils.APPLICATION_OCTET_STREAM,
				Collections.emptyMap());

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("Spring"))
				.expectComplete()
				.verify();
	}

	@Test
	public void shouldEncodeMultipleResourceRegionsFileResource() throws Exception {
		Resource resource = new ClassPathResource("ResourceRegionEncoderTests.txt", getClass());
		Flux<ResourceRegion> regions = Flux.just(
				new ResourceRegion(resource, 0, 6),
				new ResourceRegion(resource, 7, 9),
				new ResourceRegion(resource, 17, 4),
				new ResourceRegion(resource, 22, 17)
		);
		String boundary = MimeTypeUtils.generateMultipartBoundaryString();

		Flux<DataBuffer> result = this.encoder.encode(regions, this.bufferFactory,
				ResolvableType.forClass(ResourceRegion.class),
				MimeType.valueOf("text/plain"),
				Collections.singletonMap(ResourceRegionEncoder.BOUNDARY_STRING_HINT, boundary)
		);

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("\r\n--" + boundary + "\r\n"))
				.consumeNextWith(stringConsumer("Content-Type: text/plain\r\n"))
				.consumeNextWith(stringConsumer("Content-Range: bytes 0-5/39\r\n\r\n"))
				.consumeNextWith(stringConsumer("Spring"))
				.consumeNextWith(stringConsumer("\r\n--" + boundary + "\r\n"))
				.consumeNextWith(stringConsumer("Content-Type: text/plain\r\n"))
				.consumeNextWith(stringConsumer("Content-Range: bytes 7-15/39\r\n\r\n"))
				.consumeNextWith(stringConsumer("Framework"))
				.consumeNextWith(stringConsumer("\r\n--" + boundary + "\r\n"))
				.consumeNextWith(stringConsumer("Content-Type: text/plain\r\n"))
				.consumeNextWith(stringConsumer("Content-Range: bytes 17-20/39\r\n\r\n"))
				.consumeNextWith(stringConsumer("test"))
				.consumeNextWith(stringConsumer("\r\n--" + boundary + "\r\n"))
				.consumeNextWith(stringConsumer("Content-Type: text/plain\r\n"))
				.consumeNextWith(stringConsumer("Content-Range: bytes 22-38/39\r\n\r\n"))
				.consumeNextWith(stringConsumer("resource content."))
				.consumeNextWith(stringConsumer("\r\n--" + boundary + "--"))
				.expectComplete()
				.verify();
	}

	@Test
	public void nonExisting() {
		Resource resource = new ClassPathResource("ResourceRegionEncoderTests.txt", getClass());
		Resource nonExisting = new ClassPathResource("does not exist", getClass());
		Flux<ResourceRegion> regions = Flux.just(
				new ResourceRegion(resource, 0, 6),
				new ResourceRegion(nonExisting, 0, 6));

		String boundary = MimeTypeUtils.generateMultipartBoundaryString();

		Flux<DataBuffer> result = this.encoder.encode(regions, this.bufferFactory,
				ResolvableType.forClass(ResourceRegion.class),
				MimeType.valueOf("text/plain"),
				Collections.singletonMap(ResourceRegionEncoder.BOUNDARY_STRING_HINT, boundary));

		StepVerifier.create(result)
				.consumeNextWith(stringConsumer("\r\n--" + boundary + "\r\n"))
				.consumeNextWith(stringConsumer("Content-Type: text/plain\r\n"))
				.consumeNextWith(stringConsumer("Content-Range: bytes 0-5/39\r\n\r\n"))
				.consumeNextWith(stringConsumer("Spring"))
				.expectError(EncodingException.class)
				.verify();
	}

	protected Consumer<DataBuffer> stringConsumer(String expected) {
		return dataBuffer -> {
			String value =
					DataBufferTestUtils.dumpString(dataBuffer, UTF_8);
			DataBufferUtils.release(dataBuffer);
			assertEquals(expected, value);
		};
	}


}
