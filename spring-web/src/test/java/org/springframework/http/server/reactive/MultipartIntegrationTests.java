/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.server.reactive;

import java.net.URI;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;

import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 */
public class MultipartIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	@Override
	protected HttpHandler createHttpHandler() {
		return new HttpWebHandlerAdapter(new CheckRequestHandler());
	}

	@Test
	public void getFormParts() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		RequestEntity<MultiValueMap<String, Object>> request = RequestEntity
				.post(new URI("http://localhost:" + port + "/form-parts"))
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(generateBody());
		ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
	}

	private MultiValueMap<String, Object> generateBody() {
		HttpHeaders fooHeaders = new HttpHeaders();
		fooHeaders.setContentType(MediaType.TEXT_PLAIN);
		ClassPathResource fooResource = new ClassPathResource("org/springframework/http/codec/multipart/foo.txt");
		HttpEntity<ClassPathResource> fooPart = new HttpEntity<>(fooResource, fooHeaders);
		HttpEntity<String> barPart = new HttpEntity<>("bar");
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("fooPart", fooPart);
		parts.add("barPart", barPart);
		return parts;
	}


	public static class CheckRequestHandler implements WebHandler {

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			if (exchange.getRequest().getURI().getPath().equals("/form-parts")) {
				return assertGetFormParts(exchange);
			}
			return Mono.error(new AssertionError());
		}

		private Mono<Void> assertGetFormParts(ServerWebExchange exchange) {
			return exchange
					.getMultipartData()
					.doOnNext(parts -> {
						assertEquals(2, parts.size());
						assertTrue(parts.containsKey("fooPart"));
						assertFooPart(parts.getFirst("fooPart"));
						assertTrue(parts.containsKey("barPart"));
						assertBarPart(parts.getFirst("barPart"));
					})
					.then();
		}

		private void assertFooPart(Part part) {
			assertEquals("fooPart", part.name());
			assertTrue(part instanceof FilePart);
			assertEquals("foo.txt", ((FilePart) part).filename());

			StepVerifier.create(DataBufferUtils.join(part.content()))
					.consumeNextWith(buffer -> {
						assertEquals(12, buffer.readableByteCount());
						byte[] byteContent = new byte[12];
						buffer.read(byteContent);
						assertEquals("Lorem Ipsum.", new String(byteContent));
					})
					.verifyComplete();
		}

		private void assertBarPart(Part part) {
			assertEquals("barPart", part.name());
			assertTrue(part instanceof FormFieldPart);
			assertEquals("bar", ((FormFieldPart) part).value());
		}
	}

}
