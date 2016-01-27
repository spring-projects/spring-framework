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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;

import org.springframework.http.MediaType;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 */
public class JacksonJsonEncoderTests extends AbstractAllocatingTestCase {

	private JacksonJsonEncoder encoder;

	@Before
	public void createEncoder() {
		encoder = new JacksonJsonEncoder(allocator);
	}

	@Test
	public void canWrite() {
		assertTrue(encoder.canEncode(null, MediaType.APPLICATION_JSON));
		assertFalse(encoder.canEncode(null, MediaType.APPLICATION_XML));
	}

	@Test
	public void write() throws InterruptedException {
		Flux<Pojo> source = Flux.just(new Pojo("foofoo", "barbar"), new Pojo("foofoofoo", "barbarbar"));
		Flux<String> output = encoder.encode(source, null, null).map(chunk -> {
			byte[] b = new byte[chunk.readableByteCount()];
			chunk.read(b);
			return new String(b, StandardCharsets.UTF_8);
		});
		List<String> results = StreamSupport.stream(output.toIterable().spliterator(), false).collect(toList());
		assertEquals(2, results.size());
		assertEquals("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}", results.get(0));
		assertEquals("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}", results.get(1));
	}

}
