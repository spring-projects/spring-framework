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

package org.springframework.web.reactive.result.method.annotation;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.http.codec.multipart.Part;
import org.springframework.mock.http.client.reactive.test.MockClientHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import static org.junit.Assert.*;
import static org.springframework.core.ResolvableType.*;
import static org.springframework.web.method.MvcAnnotationPredicates.*;

/**
 * Unit tests for {@link RequestPartMethodArgumentResolver}.
 * @author Rossen Stoyanchev
 */
public class RequestPartMethodArgumentResolverTests {

	private RequestPartMethodArgumentResolver resolver;

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();

	private MultipartHttpMessageWriter writer;


	@Before
	public void setup() throws Exception {
		List<HttpMessageReader<?>> readers = ServerCodecConfigurer.create().getReaders();
		ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();
		this.resolver = new RequestPartMethodArgumentResolver(readers, registry);

		List<HttpMessageWriter<?>> writers = ClientCodecConfigurer.create().getWriters();
		this.writer = new MultipartHttpMessageWriter(writers);
	}


	@Test
	public void supportsParameter() {

		MethodParameter param;

		param = this.testMethod.annot(requestPart()).arg(Person.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annot(requestPart()).arg(Mono.class, Person.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annot(requestPart()).arg(Flux.class, Person.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annot(requestPart()).arg(Part.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annot(requestPart()).arg(Mono.class, Part.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annot(requestPart()).arg(Flux.class, Part.class);
		assertTrue(this.resolver.supportsParameter(param));

		param = this.testMethod.annotNotPresent(RequestPart.class).arg(Person.class);
		assertFalse(this.resolver.supportsParameter(param));
	}


	@Test
	public void person() {
		MethodParameter param = this.testMethod.annot(requestPart()).arg(Person.class);
		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("name", new Person("Jones"));
		Person actual = resolveArgument(param, bodyBuilder);

		assertEquals("Jones", actual.getName());
	}

	@Test
	public void listPerson() {
		MethodParameter param = this.testMethod.annot(requestPart()).arg(List.class, Person.class);
		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("name", new Person("Jones"));
		bodyBuilder.part("name", new Person("James"));
		List<Person> actual = resolveArgument(param, bodyBuilder);

		assertEquals("Jones", actual.get(0).getName());
		assertEquals("James", actual.get(1).getName());
	}

	@Test
	public void monoPerson() {
		MethodParameter param = this.testMethod.annot(requestPart()).arg(Mono.class, Person.class);
		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("name", new Person("Jones"));
		Mono<Person> actual = resolveArgument(param, bodyBuilder);

		assertEquals("Jones", actual.block().getName());
	}

	@Test
	public void fluxPerson() {
		MethodParameter param = this.testMethod.annot(requestPart()).arg(Flux.class, Person.class);
		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("name", new Person("Jones"));
		bodyBuilder.part("name", new Person("James"));
		Flux<Person> actual = resolveArgument(param, bodyBuilder);

		List<Person> persons = actual.collectList().block();
		assertEquals("Jones", persons.get(0).getName());
		assertEquals("James", persons.get(1).getName());
	}

	@Test
	public void part() {
		MethodParameter param = this.testMethod.annot(requestPart()).arg(Part.class);
		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("name", new Person("Jones"));
		Part actual = resolveArgument(param, bodyBuilder);

		DataBuffer buffer = DataBufferUtils.join(actual.content()).block();
		assertEquals("{\"name\":\"Jones\"}", DataBufferTestUtils.dumpString(buffer, StandardCharsets.UTF_8));
	}

	@Test
	public void listPart() {
		MethodParameter param = this.testMethod.annot(requestPart()).arg(List.class, Part.class);
		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("name", new Person("Jones"));
		bodyBuilder.part("name", new Person("James"));
		List<Part> actual = resolveArgument(param, bodyBuilder);

		assertEquals("{\"name\":\"Jones\"}", partToUtf8String(actual.get(0)));
		assertEquals("{\"name\":\"James\"}", partToUtf8String(actual.get(1)));
	}

	@Test
	public void monoPart() {
		MethodParameter param = this.testMethod.annot(requestPart()).arg(Mono.class, Part.class);
		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("name", new Person("Jones"));
		Mono<Part> actual = resolveArgument(param, bodyBuilder);

		Part part = actual.block();
		assertEquals("{\"name\":\"Jones\"}", partToUtf8String(part));
	}

	@Test
	public void fluxPart() {
		MethodParameter param = this.testMethod.annot(requestPart()).arg(Flux.class, Part.class);
		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("name", new Person("Jones"));
		bodyBuilder.part("name", new Person("James"));
		Flux<Part> actual = resolveArgument(param, bodyBuilder);

		List<Part> parts = actual.collectList().block();
		assertEquals("{\"name\":\"Jones\"}", partToUtf8String(parts.get(0)));
		assertEquals("{\"name\":\"James\"}", partToUtf8String(parts.get(1)));
	}

	@Test
	public void personRequired() {
		MethodParameter param = this.testMethod.annot(requestPart()).arg(Person.class);
		ServerWebExchange exchange = createExchange(new MultipartBodyBuilder());
		Mono<Object> result = this.resolver.resolveArgument(param, new BindingContext(), exchange);

		StepVerifier.create(result).expectError(ServerWebInputException.class).verify();
	}

	@Test
	public void personNotRequired() {
		MethodParameter param = this.testMethod.annot(requestPart().notRequired()).arg(Person.class);
		ServerWebExchange exchange = createExchange(new MultipartBodyBuilder());
		Mono<Object> result = this.resolver.resolveArgument(param, new BindingContext(), exchange);

		StepVerifier.create(result).verifyComplete();
	}

	@Test
	public void partRequired() {
		MethodParameter param = this.testMethod.annot(requestPart()).arg(Part.class);
		ServerWebExchange exchange = createExchange(new MultipartBodyBuilder());
		Mono<Object> result = this.resolver.resolveArgument(param, new BindingContext(), exchange);

		StepVerifier.create(result).expectError(ServerWebInputException.class).verify();
	}

	@Test
	public void partNotRequired() {
		MethodParameter param = this.testMethod.annot(requestPart().notRequired()).arg(Part.class);
		ServerWebExchange exchange = createExchange(new MultipartBodyBuilder());
		Mono<Object> result = this.resolver.resolveArgument(param, new BindingContext(), exchange);

		StepVerifier.create(result).verifyComplete();
	}


	@SuppressWarnings("unchecked")
	private <T> T resolveArgument(MethodParameter param, MultipartBodyBuilder builder) {
		ServerWebExchange exchange = createExchange(builder);
		Mono<Object> result = this.resolver.resolveArgument(param, new BindingContext(), exchange);
		Object value = result.block(Duration.ofSeconds(5));

		assertNotNull(value);
		assertTrue(param.getParameterType().isAssignableFrom(value.getClass()));
		return (T) value;
	}

	private ServerWebExchange createExchange(MultipartBodyBuilder builder) {
		MockClientHttpRequest clientRequest = new MockClientHttpRequest(HttpMethod.POST, "/");
		this.writer.write(Mono.just(builder.build()), forClass(MultiValueMap.class),
				MediaType.MULTIPART_FORM_DATA, clientRequest, Collections.emptyMap()).block();

		MockServerHttpRequest serverRequest = MockServerHttpRequest.post("/")
				.contentType(clientRequest.getHeaders().getContentType())
				.body(clientRequest.getBody());

		return MockServerWebExchange.from(serverRequest);
	}

	private String partToUtf8String(Part part) {
		DataBuffer buffer = DataBufferUtils.join(part.content()).block();
		return DataBufferTestUtils.dumpString(buffer, StandardCharsets.UTF_8);
	}


	@SuppressWarnings("unused")
	void handle(
			@RequestPart("name") Person person,
			@RequestPart("name") Mono<Person> personMono,
			@RequestPart("name") Flux<Person> personFlux,
			@RequestPart("name") List<Person> personList,
			@RequestPart("name") Part part,
			@RequestPart("name") Mono<Part> partMono,
			@RequestPart("name") Flux<Part> partFlux,
			@RequestPart("name") List<Part> partList,
			@RequestPart(name = "anotherPart", required = false) Person anotherPerson,
			@RequestPart(name = "anotherPart", required = false) Part anotherPart,
			Person notAnnotated) {}


	private static class Person {

		private String name;

		@JsonCreator
		public Person(@JsonProperty("name") String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

}
