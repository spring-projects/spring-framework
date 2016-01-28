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

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;

import static org.junit.Assert.*;
import reactor.core.test.TestSubscriber;

/**
 * @author Sebastien Deleuze
 */
public class Jaxb2DecoderTests extends AbstractAllocatingTestCase {

	private final Jaxb2Decoder decoder = new Jaxb2Decoder();

	@Test
	public void canDecode() {
		assertTrue(decoder.canDecode(null, MediaType.APPLICATION_XML));
		assertTrue(decoder.canDecode(null, MediaType.TEXT_XML));
		assertFalse(decoder.canDecode(null, MediaType.APPLICATION_JSON));
	}

	@Test
	public void decode() {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><pojo><bar>barbar</bar><foo>foofoo</foo></pojo>"));
		Flux<Object> output = decoder.decode(source, ResolvableType.forClass(Pojo.class), null);
		TestSubscriber<Object> testSubscriber = new TestSubscriber<>();
		testSubscriber.bindTo(output)
				.assertValues(new Pojo("foofoo", "barbar"));
	}

}
