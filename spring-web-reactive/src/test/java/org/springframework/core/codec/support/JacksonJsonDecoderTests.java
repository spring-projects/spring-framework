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

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 */
public class JacksonJsonDecoderTests extends AbstractDataBufferAllocatingTestCase {

	private final JacksonJsonDecoder decoder = new JacksonJsonDecoder();

	@Test
	public void canDecode() {
		assertTrue(this.decoder.canDecode(null, MediaType.APPLICATION_JSON));
		assertFalse(this.decoder.canDecode(null, MediaType.APPLICATION_XML));
	}

	@Test
	public void decode() {
		Flux<DataBuffer> source =
				Flux.just(stringBuffer("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"));
		Flux<Object> output =
				this.decoder.decode(source, ResolvableType.forClass(Pojo.class), null);
		TestSubscriber
				.subscribe(output)
				.assertValues(new Pojo("foofoo", "barbar"));
	}

}
