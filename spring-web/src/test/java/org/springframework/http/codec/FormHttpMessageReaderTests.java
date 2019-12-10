/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.http.codec;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractLeakCheckingTests;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
public class FormHttpMessageReaderTests extends AbstractLeakCheckingTests {

	private final FormHttpMessageReader reader = new FormHttpMessageReader();


	@Test
	public void canRead() {
		assertThat(this.reader.canRead(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.APPLICATION_FORM_URLENCODED)).isTrue();

		assertThat(this.reader.canRead(
				ResolvableType.forInstance(new LinkedMultiValueMap<String, String>()),
				MediaType.APPLICATION_FORM_URLENCODED)).isTrue();

		assertThat(this.reader.canRead(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				MediaType.APPLICATION_FORM_URLENCODED)).isFalse();

		assertThat(this.reader.canRead(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, Object.class, String.class),
				MediaType.APPLICATION_FORM_URLENCODED)).isFalse();

		assertThat(this.reader.canRead(
				ResolvableType.forClassWithGenerics(Map.class, String.class, String.class),
				MediaType.APPLICATION_FORM_URLENCODED)).isFalse();

		assertThat(this.reader.canRead(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.MULTIPART_FORM_DATA)).isFalse();
	}

	@Test
	public void readFormAsMono() {
		String body = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
		MockServerHttpRequest request = request(body);
		MultiValueMap<String, String> result = this.reader.readMono(null, request, null).block();

		assertThat(result.size()).as("Invalid result").isEqualTo(3);
		assertThat(result.getFirst("name 1")).as("Invalid result").isEqualTo("value 1");
		List<String> values = result.get("name 2");
		assertThat(values.size()).as("Invalid result").isEqualTo(2);
		assertThat(values.get(0)).as("Invalid result").isEqualTo("value 2+1");
		assertThat(values.get(1)).as("Invalid result").isEqualTo("value 2+2");
		assertThat(result.getFirst("name 3")).as("Invalid result").isNull();
	}

	@Test
	public void readFormAsFlux() {
		String body = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
		MockServerHttpRequest request = request(body);
		MultiValueMap<String, String> result = this.reader.read(null, request, null).single().block();

		assertThat(result.size()).as("Invalid result").isEqualTo(3);
		assertThat(result.getFirst("name 1")).as("Invalid result").isEqualTo("value 1");
		List<String> values = result.get("name 2");
		assertThat(values.size()).as("Invalid result").isEqualTo(2);
		assertThat(values.get(0)).as("Invalid result").isEqualTo("value 2+1");
		assertThat(values.get(1)).as("Invalid result").isEqualTo("value 2+2");
		assertThat(result.getFirst("name 3")).as("Invalid result").isNull();
	}

	@Test
	public void readFormError() {
		DataBuffer fooBuffer = stringBuffer("name=value");
		Flux<DataBuffer> body =
				Flux.just(fooBuffer).concatWith(Flux.error(new RuntimeException()));
		MockServerHttpRequest request = request(body);

		Flux<MultiValueMap<String, String>> result = this.reader.read(null, request, null);
		StepVerifier.create(result)
				.expectError()
				.verify();
	}


	private MockServerHttpRequest request(String body) {
		return request(Mono.just(stringBuffer(body)));
	}

	private MockServerHttpRequest request(Publisher<? extends DataBuffer> body) {
		return MockServerHttpRequest
					.method(HttpMethod.GET, "/")
					.header(HttpHeaders.CONTENT_TYPE,  MediaType.APPLICATION_FORM_URLENCODED_VALUE)
					.body(body);
	}

	private DataBuffer stringBuffer(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
		buffer.write(bytes);
		return buffer;
	}

}
