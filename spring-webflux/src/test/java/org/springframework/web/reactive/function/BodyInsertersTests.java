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

package org.springframework.web.reactive.function;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonView;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.client.reactive.test.MockClientHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.Assert.*;
import static org.springframework.http.codec.json.AbstractJackson2Codec.*;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
public class BodyInsertersTests {

	private BodyInserter.Context context;

	private Map<String, Object> hints;


	@Before
	public void createContext() {
		final List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
		messageWriters.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		messageWriters.add(new EncoderHttpMessageWriter<>(new CharSequenceEncoder()));
		messageWriters.add(new ResourceHttpMessageWriter());
		messageWriters.add(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
		Jackson2JsonEncoder jsonEncoder = new Jackson2JsonEncoder();
		messageWriters.add(new EncoderHttpMessageWriter<>(jsonEncoder));
		messageWriters
				.add(new ServerSentEventHttpMessageWriter(Collections.singletonList(jsonEncoder)));
		messageWriters.add(new FormHttpMessageWriter());

		this.context = new BodyInserter.Context() {
			@Override
			public Supplier<Stream<HttpMessageWriter<?>>> messageWriters() {
				return messageWriters::stream;
			}
			@Override
			public Map<String, Object> hints() {
				return hints;
			}
		};
		this.hints = new HashMap<>();
	}


	@Test
	public void ofString() throws Exception {
		String body = "foo";
		BodyInserter<String, ReactiveHttpOutputMessage> inserter = BodyInserters.fromObject(body);

		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		StepVerifier.create(result).expectComplete().verify();

		ByteBuffer byteBuffer = ByteBuffer.wrap(body.getBytes(UTF_8));
		DataBuffer buffer = new DefaultDataBufferFactory().wrap(byteBuffer);
		StepVerifier.create(response.getBody())
				.expectNext(buffer)
				.expectComplete()
				.verify();
	}

	@Test
	public void ofObject() throws Exception {
		User body = new User("foo", "bar");
		BodyInserter<User, ReactiveHttpOutputMessage> inserter = BodyInserters.fromObject(body);
		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		StepVerifier.create(result).expectComplete().verify();

		StepVerifier.create(response.getBodyAsString())
				.expectNext("{\"username\":\"foo\",\"password\":\"bar\"}")
				.expectComplete()
				.verify();
	}

	@Test
	public void ofObjectWithHints() throws Exception {
		User body = new User("foo", "bar");
		BodyInserter<User, ReactiveHttpOutputMessage> inserter = BodyInserters.fromObject(body);
		this.hints.put(JSON_VIEW_HINT, SafeToSerialize.class);
		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		StepVerifier.create(result).expectComplete().verify();

		StepVerifier.create(response.getBodyAsString())
				.expectNext("{\"username\":\"foo\"}")
				.expectComplete()
				.verify();
	}

	@Test
	public void ofPublisher() throws Exception {
		Flux<String> body = Flux.just("foo");
		BodyInserter<Flux<String>, ReactiveHttpOutputMessage> inserter = BodyInserters.fromPublisher(body, String.class);

		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		StepVerifier.create(result).expectComplete().verify();

		ByteBuffer byteBuffer = ByteBuffer.wrap("foo".getBytes(UTF_8));
		DataBuffer buffer = new DefaultDataBufferFactory().wrap(byteBuffer);
		StepVerifier.create(response.getBody())
				.expectNext(buffer)
				.expectComplete()
				.verify();
	}

	@Test
	public void ofResource() throws Exception {
		Resource body = new ClassPathResource("response.txt", getClass());
		BodyInserter<Resource, ReactiveHttpOutputMessage> inserter = BodyInserters.fromResource(body);

		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		StepVerifier.create(result).expectComplete().verify();

		byte[] expectedBytes = Files.readAllBytes(body.getFile().toPath());

		StepVerifier.create(response.getBody())
				.consumeNextWith(dataBuffer -> {
					byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
					dataBuffer.read(resultBytes);
					assertArrayEquals(expectedBytes, resultBytes);
				})
				.expectComplete()
				.verify();
	}

	@Test
	public void ofServerSentEventFlux() throws Exception {
		ServerSentEvent<String> event = ServerSentEvent.builder("foo").build();
		Flux<ServerSentEvent<String>> body = Flux.just(event);
		BodyInserter<Flux<ServerSentEvent<String>>, ServerHttpResponse> inserter =
				BodyInserters.fromServerSentEvents(body);

		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		StepVerifier.create(result).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void ofServerSentEventClass() throws Exception {
		Flux<String> body = Flux.just("foo");
		BodyInserter<Flux<String>, ServerHttpResponse> inserter =
				BodyInserters.fromServerSentEvents(body, String.class);

		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		StepVerifier.create(result).expectNextCount(0).expectComplete().verify();
	}

	@Test
	public void ofFormData() throws Exception {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.set("name 1", "value 1");
		body.add("name 2", "value 2+1");
		body.add("name 2", "value 2+2");
		body.add("name 3", null);

		BodyInserter<MultiValueMap<String, String>, ClientHttpRequest>
				inserter = BodyInserters.fromFormData(body);

		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://example.com"));
		Mono<Void> result = inserter.insert(request, this.context);
		StepVerifier.create(result).expectComplete().verify();

		StepVerifier.create(request.getBody())
				.consumeNextWith(dataBuffer -> {
					byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
					dataBuffer.read(resultBytes);
					assertArrayEquals("name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3".getBytes(StandardCharsets.UTF_8),
							resultBytes);
				})
				.expectComplete()
				.verify();

	}

	@Test
	public void ofDataBuffers() throws Exception {
		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		BodyInserter<Flux<DataBuffer>, ReactiveHttpOutputMessage> inserter = BodyInserters.fromDataBuffers(body);

		MockServerHttpResponse response = new MockServerHttpResponse();
		Mono<Void> result = inserter.insert(response, this.context);
		StepVerifier.create(result).expectComplete().verify();

		StepVerifier.create(response.getBody())
				.expectNext(dataBuffer)
				.expectComplete()
				.verify();
	}


	interface SafeToSerialize {}


	private static class User {

		@JsonView(SafeToSerialize.class)
		private String username;

		private String password;

		public User() {
		}

		public User(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}

}
