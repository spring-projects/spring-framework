/*
 * Copyright 2002-2023 the original author or authors.
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
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * @author Sebastien Deleuze
 */
class MultipartHttpHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	@Override
	protected HttpHandler createHttpHandler() {
		return new HttpWebHandlerAdapter(new CheckRequestHandler());
	}

	@ParameterizedHttpServerTest
	void getMultipartFormData(HttpServer httpServer) throws Exception {
		testMultipart(httpServer, MediaType.MULTIPART_FORM_DATA);
	}

	@ParameterizedHttpServerTest
	void getMultipartMixed(HttpServer httpServer) throws Exception {
		testMultipart(httpServer, MediaType.MULTIPART_MIXED);
	}

	@ParameterizedHttpServerTest
	void getMultipartRelated(HttpServer httpServer) throws Exception {
		testMultipart(httpServer, MediaType.MULTIPART_RELATED);
	}

	private void testMultipart(HttpServer httpServer, MediaType mediaType) throws Exception {
		startServer(httpServer);

		HttpHeaders fooHeaders = new HttpHeaders();
		fooHeaders.setContentType(MediaType.TEXT_PLAIN);
		ClassPathResource fooResource = new ClassPathResource("org/springframework/http/codec/multipart/foo.txt");
		HttpEntity<ClassPathResource> fooPart = new HttpEntity<>(fooResource, fooHeaders);
		HttpEntity<String> barPart = new HttpEntity<>("bar");
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("fooPart", fooPart);
		parts.add("barPart", barPart);

		URI url = URI.create("http://localhost:" + port + "/form-parts");
		ResponseEntity<Void> response = new RestTemplate().exchange(
				RequestEntity.post(url).contentType(mediaType).body(parts), Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}


	static class CheckRequestHandler implements WebHandler {

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
						assertThat(parts).hasSize(2);
						assertThat(parts).containsKey("fooPart");
						assertFooPart(parts.getFirst("fooPart"));
						assertThat(parts).containsKey("barPart");
						assertBarPart(parts.getFirst("barPart"));
					})
					.then();
		}

		private void assertFooPart(Part part) {
			assertThat(part.name()).isEqualTo("fooPart");
			assertThat(part)
				.asInstanceOf(type(FilePart.class))
				.extracting(FilePart::filename).isEqualTo("foo.txt");

			StepVerifier.create(DataBufferUtils.join(part.content()))
					.consumeNextWith(buffer -> {
						assertThat(buffer.readableByteCount()).isEqualTo(12);
						byte[] byteContent = new byte[12];
						buffer.read(byteContent);
						assertThat(new String(byteContent)).isEqualTo("Lorem Ipsum.");
					})
					.verifyComplete();
		}

		private void assertBarPart(Part part) {
			assertThat(part.name()).isEqualTo("barPart");
			assertThat(part)
				.asInstanceOf(type(FormFieldPart.class))
				.extracting(FormFieldPart::value).isEqualTo("bar");
		}
	}

}
