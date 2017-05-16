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

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
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
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	private WebClient webClient;


	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		this.webClient = WebClient.create("http://localhost:" + this.port);
	}


	@Override
	protected HttpHandler createHttpHandler() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(TestConfiguration.class);
		wac.refresh();
		return WebHttpHandlerBuilder.webHandler(new DispatcherHandler(wac)).build();
	}

	@Test
	public void requestPart() {
		Mono<ClientResponse> result = webClient
				.post()
				.uri("/requestPart")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(generateBody()))
				.exchange();

		StepVerifier
				.create(result)
				.consumeNextWith(response -> assertEquals(HttpStatus.OK, response.statusCode()))
				.verifyComplete();
	}

	@Test
	public void requestBodyMap() {
		Mono<String> result = webClient
				.post()
				.uri("/requestBodyMap")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(generateBody()))
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.consumeNextWith(body -> assertEquals("Map[barPart,fooPart]", body))
				.verifyComplete();
	}

	@Test
	public void requestBodyFlux() {
		Mono<String> result = webClient
				.post()
				.uri("/requestBodyFlux")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(generateBody()))
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.consumeNextWith(body -> assertEquals("Flux[barPart,fooPart]", body))
				.verifyComplete();
	}

	@Test
	public void modelAttribute() {
		Mono<String> result = webClient
				.post()
				.uri("/modelAttribute")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(generateBody()))
				.retrieve()
				.bodyToMono(String.class);

		StepVerifier.create(result)
				.consumeNextWith(body -> assertEquals("TestBean[barPart=bar,fooPart=foo.txt]", body))
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

		@PostMapping("/requestPart")
		void requestPart(@RequestPart FormFieldPart barPart, @RequestPart Mono<FilePart> fooPart) {
			assertEquals("bar", barPart.value());
			assertEquals("foo.txt", fooPart.block(Duration.ZERO).filename());
		}

		@PostMapping("/requestBodyMap")
		Mono<String> requestBodyMap(@RequestBody Mono<MultiValueMap<String, Part>> parts) {
			return parts.map(map -> map.toSingleValueMap().entrySet().stream()
					.map(Map.Entry::getKey).sorted().collect(Collectors.joining(",", "Map[", "]")));
		}

		@PostMapping("/requestBodyFlux")
		Mono<String> requestBodyFlux(@RequestBody Flux<Part> parts) {
			return parts.map(Part::name).collectList()
					.map(names -> names.stream().sorted().collect(Collectors.joining(",", "Flux[", "]")));
		}

		@PostMapping("/modelAttribute")
		String modelAttribute(@ModelAttribute TestBean testBean) {
			return testBean.toString();
		}
	}

	static class TestBean {

		private String barPart;

		private FilePart fooPart;


		public String getBarPart() {
			return this.barPart;
		}

		public void setBarPart(String barPart) {
			this.barPart = barPart;
		}

		public FilePart getFooPart() {
			return this.fooPart;
		}

		public void setFooPart(FilePart fooPart) {
			this.fooPart = fooPart;
		}

		@Override
		public String toString() {
			return "TestBean[barPart=" + getBarPart() + ",fooPart=" + getFooPart().filename() + "]";
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
