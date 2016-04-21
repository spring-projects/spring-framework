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

package org.springframework.core.codec.support;

import java.io.IOException;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class ResourceDecoderTests extends AbstractAllocatingTestCase {

	private final ResourceDecoder decoder = new ResourceDecoder();

	@Test
	public void canDecode() throws Exception {
		assertTrue(decoder.canDecode(ResolvableType.forClass(InputStreamResource.class),
				MediaType.TEXT_PLAIN));
		assertTrue(decoder.canDecode(ResolvableType.forClass(ByteArrayResource.class),
				MediaType.TEXT_PLAIN));
		assertTrue(decoder.canDecode(ResolvableType.forClass(Resource.class),
				MediaType.TEXT_PLAIN));
		assertTrue(decoder.canDecode(ResolvableType.forClass(InputStreamResource.class),
				MediaType.APPLICATION_JSON));
	}

	@Test
	public void decode() throws Exception {
		DataBuffer fooBuffer = stringBuffer("foo");
		DataBuffer barBuffer = stringBuffer("bar");
		Flux<DataBuffer> source = Flux.just(fooBuffer, barBuffer);

		Flux<Resource> result =
				decoder.decode(source, ResolvableType.forClass(Resource.class), null);

		TestSubscriber<Resource> testSubscriber = new TestSubscriber<>();
		testSubscriber.bindTo(result).
				assertNoError().
				assertComplete().
				assertValuesWith(resource -> {
					try {
						byte[] bytes =
								StreamUtils.copyToByteArray(resource.getInputStream());
						assertEquals("foobar", new String(bytes));
					}
					catch (IOException e) {
						fail(e.getMessage());
					}
				});

	}

}