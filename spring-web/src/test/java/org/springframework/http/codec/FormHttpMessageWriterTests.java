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

package org.springframework.http.codec;

import java.util.Map;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 */
public class FormHttpMessageWriterTests extends AbstractDataBufferAllocatingTestCase {

	private final FormHttpMessageWriter writer = new FormHttpMessageWriter();


	@Test
	public void canWrite() {
		assertTrue(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.APPLICATION_FORM_URLENCODED));

		// No generic information
		assertTrue(this.writer.canWrite(
				ResolvableType.forInstance(new LinkedMultiValueMap<String, String>()),
				MediaType.APPLICATION_FORM_URLENCODED));

		assertFalse(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				null));

		assertFalse(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, Object.class, String.class),
				null));

		assertFalse(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(Map.class, String.class, String.class),
				MediaType.APPLICATION_FORM_URLENCODED));

		assertFalse(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.MULTIPART_FORM_DATA));
	}

	@Test
	public void writeForm() {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.set("name 1", "value 1");
		body.add("name 2", "value 2+1");
		body.add("name 2", "value 2+2");
		body.add("name 3", null);
		MockServerHttpResponse response = new MockServerHttpResponse(this.bufferFactory);
		this.writer.write(Mono.just(body), null, MediaType.APPLICATION_FORM_URLENCODED, response, null).block();

		String expected = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
		StepVerifier.create(response.getBody())
				.consumeNextWith(stringConsumer(
						expected))
				.expectComplete()
				.verify();
		HttpHeaders headers = response.getHeaders();
		assertEquals("application/x-www-form-urlencoded;charset=UTF-8", headers.getContentType().toString());
		assertEquals(expected.length(), headers.getContentLength());
	}

}
