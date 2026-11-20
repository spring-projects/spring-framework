/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec;

import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class FormHttpMessageWriterTests extends AbstractLeakCheckingTests {

	private final FormHttpMessageWriter writer = new FormHttpMessageWriter();


	@Test
	void canWrite() {
		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.APPLICATION_FORM_URLENCODED)).isTrue();

		// No generic information
		assertThat(this.writer.canWrite(
				ResolvableType.forInstance(new LinkedMultiValueMap<>()),
				MediaType.APPLICATION_FORM_URLENCODED)).isTrue();

		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Object.class),
				null)).isFalse();

		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, Object.class, String.class),
				null)).isFalse();

		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(Map.class, String.class, String.class),
				MediaType.APPLICATION_FORM_URLENCODED)).isFalse();

		assertThat(this.writer.canWrite(
				ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.MULTIPART_FORM_DATA)).isFalse();
	}

	@Test
	void writeForm() {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.set("name 1", "value 1");
		body.add("name 2", "value 2+1");
		body.add("name 2", "value 2+2");
		body.add("name 3", null);
		MockServerHttpResponse response = new MockServerHttpResponse(this.bufferFactory);
		this.writer.write(Mono.just(body), null, MediaType.APPLICATION_FORM_URLENCODED, response, null).block();

		String expected = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
		StepVerifier.create(response.getBody())
				.consumeNextWith(stringConsumer(expected))
				.expectComplete()
				.verify();
		HttpHeaders headers = response.getHeaders();
		assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_FORM_URLENCODED);
		assertThat(headers.getContentLength()).isEqualTo(expected.length());
	}

	private Consumer<DataBuffer> stringConsumer(String expected) {
		return dataBuffer -> {
			String value = dataBuffer.toString(UTF_8);
			DataBufferUtils.release(dataBuffer);
			assertThat(value).isEqualTo(expected);
		};
	}


}
