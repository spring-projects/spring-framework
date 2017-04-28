/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.codec.multipart;

import java.io.IOException;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.codec.multipart.MultipartHttpMessageReader.*;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Sebastien Deleuze
 */
public class SynchronossMultipartHttpMessageReaderTests {

	@Test
	public void resolveParts() throws IOException {
		ServerHttpRequest request = generateMultipartRequest();
		MultipartHttpMessageReader multipartReader = new SynchronossMultipartHttpMessageReader();
		MultiValueMap<String, Part> parts = multipartReader.readMono(MULTIPART_VALUE_TYPE, request, emptyMap()).block();
		assertEquals(2, parts.size());

		assertTrue(parts.containsKey("fooPart"));
		Part part = parts.getFirst("fooPart");
		assertEquals("fooPart", part.getName());
		Optional<String> filename = part.getFilename();
		assertTrue(filename.isPresent());
		assertEquals("foo.txt", filename.get());
		DataBuffer buffer = part
				.getContent()
				.reduce((s1, s2) -> s1.write(s2))
				.block();
		assertEquals(12, buffer.readableByteCount());
		byte[] byteContent = new byte[12];
		buffer.read(byteContent);
		assertEquals("Lorem\nIpsum\n", new String(byteContent));

		assertTrue(parts.containsKey("barPart"));
		part = parts.getFirst("barPart");
		assertEquals("barPart", part.getName());
		filename = part.getFilename();
		assertFalse(filename.isPresent());
		assertEquals("bar", part.getContentAsString().block());
	}

	@Test
	public void bodyError() {
		ServerHttpRequest request = generateErrorMultipartRequest();
		MultipartHttpMessageReader multipartReader = new SynchronossMultipartHttpMessageReader();
		StepVerifier.create(multipartReader.readMono(MULTIPART_VALUE_TYPE, request, emptyMap()))
				.verifyError();
	}

	private ServerHttpRequest generateMultipartRequest() throws IOException {
		HttpHeaders fooHeaders = new HttpHeaders();
		fooHeaders.setContentType(MediaType.TEXT_PLAIN);
		ClassPathResource fooResource = new ClassPathResource("org/springframework/http/codec/multipart/foo.txt");
		HttpEntity<ClassPathResource> fooPart = new HttpEntity<>(fooResource, fooHeaders);
		HttpEntity<String> barPart = new HttpEntity<>("bar");
		FormHttpMessageConverter converter = new FormHttpMessageConverter();
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("fooPart", fooPart);
		parts.add("barPart", barPart);
		converter.write(parts, MULTIPART_FORM_DATA, outputMessage);
		byte[] content = outputMessage.getBodyAsBytes();
		MockServerHttpRequest request = MockServerHttpRequest
				.post("/foo")
				.header(CONTENT_TYPE, outputMessage.getHeaders().getContentType().toString())
				.header(CONTENT_LENGTH, String.valueOf(content.length))
				.body(new String(content));
		return request;
	}

	private ServerHttpRequest generateErrorMultipartRequest() {
		DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
		MockServerHttpRequest request = MockServerHttpRequest
				.post("/foo")
				.header(CONTENT_TYPE, MULTIPART_FORM_DATA.toString())
				.body(Flux.just(bufferFactory.wrap("invalid content".getBytes())));
		return request;
	}

}
