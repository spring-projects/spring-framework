/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.reactive.codec.encoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import reactor.Flux;

import org.springframework.core.codec.support.Jaxb2Encoder;
import org.springframework.http.MediaType;
import org.springframework.reactive.codec.Pojo;

/**
 * @author Sebastien Deleuze
 */
public class Jaxb2EncoderTests {

	private final Jaxb2Encoder encoder = new Jaxb2Encoder();

	@Test
	public void canEncode() {
		assertTrue(encoder.canEncode(null, MediaType.APPLICATION_XML));
		assertTrue(encoder.canEncode(null, MediaType.TEXT_XML));
		assertFalse(encoder.canEncode(null, MediaType.APPLICATION_JSON));
	}

	@Test
	public void encode() throws InterruptedException {
		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"), new Pojo("foofoofoo", "barbarbar"));
		Flux<String> output = encoder.encode(source, null, null).map(chunk -> {
			byte[] b = new byte[chunk.remaining()];
			chunk.get(b);
			return new String(b, StandardCharsets.UTF_8);
		});
		List<String> results = StreamSupport.stream(output.toIterable().spliterator(), false).collect(toList());
		assertEquals(2, results.size());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><pojo><bar>barbar</bar><foo>foofoo</foo></pojo>", results.get(0));
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><pojo><bar>barbarbar</bar><foo>foofoofoo</foo></pojo>", results.get(1));
	}

}
