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
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import reactor.core.publisher.Flux;
import reactor.core.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.MediaType;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.custommonkey.xmlunit.XMLAssert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 */
public class Jaxb2EncoderTests extends AbstractAllocatingTestCase {

	private Jaxb2Encoder encoder;

	@Before
	public void createEncoder() {
		encoder = new Jaxb2Encoder();
	}

	@Test
	public void canEncode() {
		assertTrue(encoder.canEncode(ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_XML));
		assertTrue(encoder.canEncode(ResolvableType.forClass(Pojo.class),
				MediaType.TEXT_XML));
		assertFalse(encoder.canEncode(ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_JSON));

		assertTrue(encoder.canEncode(
				ResolvableType.forClass(Jaxb2DecoderTests.TypePojo.class),
				MediaType.APPLICATION_XML));

		assertFalse(encoder.canEncode(ResolvableType.forClass(getClass()),
				MediaType.APPLICATION_XML));
	}

	@Test
	public void encode() {
		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"), new Pojo("foofoofoo", "barbarbar"));
		Flux<String> output =
				encoder.encode(source, allocator, ResolvableType.forClass(Pojo.class),
						MediaType.APPLICATION_XML).map(chunk -> DataBufferTestUtils
						.dumpString(chunk, StandardCharsets.UTF_8));
		TestSubscriber<String> testSubscriber = new TestSubscriber<>();
		testSubscriber.bindTo(output).assertValuesWith(s -> {
			try {
				assertXMLEqual("<pojo><bar>barbar</bar><foo>foofoo</foo></pojo>", s);
			}
			catch (SAXException | IOException e) {
				fail(e.getMessage());
			}
		});
	}

}
