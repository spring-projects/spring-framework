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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.junit.Assert.assertEquals;

public class MultipartIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private AnnotationConfigApplicationContext wac;

	private WebClient webClient;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		this.webClient = WebClient.create("http://localhost:" + this.port);
	}


	@Override
	protected HttpHandler createHttpHandler() {
		this.wac = new AnnotationConfigApplicationContext();
		this.wac.register(TestConfiguration.class);
		this.wac.refresh();

		return WebHttpHandlerBuilder.webHandler(new DispatcherHandler(this.wac)).build();
	}

	@Test
	public void map() {
		test("/map");
	}

	@Test
	public void multiValueMap() {
		test("/multivaluemap");
	}

	@Test
	public void partParam() {
		test("/partparam");
	}

	@Test
	public void part() {
		test("/part");
	}

	private void test(String uri) {
		Mono<ClientResponse> result = webClient
				.post()
				.uri(uri)
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(generateBody()))
				.exchange();

		StepVerifier
				.create(result)
				.consumeNextWith(response -> assertEquals(HttpStatus.OK, response.statusCode()))
				.verifyComplete();
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

	@RestController
	@SuppressWarnings("unused")
	static class MultipartController {

		@PostMapping("/map")
		void map(@RequestParam Map<String, Part> parts) {
			assertEquals(2, parts.size());
			assertEquals("foo.txt", parts.get("fooPart").getFilename().get());
			assertEquals("bar", parts.get("barPart").getContentAsString().block());
		}

		@PostMapping("/multivaluemap")
		void multiValueMap(@RequestParam MultiValueMap<String, Part> parts) {
			Map<String, Part> map = parts.toSingleValueMap();
			assertEquals(2, map.size());
			assertEquals("foo.txt", map.get("fooPart").getFilename().get());
			assertEquals("bar", map.get("barPart").getContentAsString().block());
		}

		@PostMapping("/partparam")
		void partParam(@RequestParam Part fooPart) {
			assertEquals("foo.txt", fooPart.getFilename().get());
		}

		@PostMapping("/part")
		void part(@RequestPart Part fooPart) {
			assertEquals("foo.txt", fooPart.getFilename().get());
		}

	}

	@Configuration
	@EnableWebFlux
	@SuppressWarnings("unused")
	static class TestConfiguration {

		@Bean
		public MultipartController multipartController() {
			return new MultipartController();
		}
	}

}
