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

package org.springframework.http.codec.xml;

import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.Pojo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

/**
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 */
public class Jaxb2EncoderTests extends AbstractDataBufferAllocatingTestCase {

	private Jaxb2Encoder encoder;

	@Before
	public void createEncoder() {
		this.encoder = new Jaxb2Encoder();
	}

	@Test
	public void canEncode() {
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_XML));
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(Pojo.class),
				MediaType.TEXT_XML));
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_JSON));

		assertTrue(this.encoder.canEncode(
				ResolvableType.forClass(Jaxb2DecoderTests.TypePojo.class),
				MediaType.APPLICATION_XML));

		assertFalse(this.encoder.canEncode(ResolvableType.forClass(getClass()),
				MediaType.APPLICATION_XML));
	}

	@Test
	public void encode() {
		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"), new Pojo("foofoofoo", "barbarbar"));
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory,
				ResolvableType.forClass(Pojo.class),
						MediaType.APPLICATION_XML);
		TestSubscriber
				.subscribe(output)
				.assertValuesWith(dataBuffer -> {
			try {
				String s = DataBufferTestUtils
						.dumpString(dataBuffer, StandardCharsets.UTF_8);
				assertThat(s, isSimilarTo("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
						"<pojo><bar>barbar</bar><foo>foofoo</foo></pojo>"));
			}
			finally {
				DataBufferUtils.release(dataBuffer);
			}
		});
	}

}
