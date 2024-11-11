/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageReader;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonEncoderDecoder}.
 *
 * @author Stephane Nicoll
 */
class JsonEncoderDecoderTests {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static final HttpMessageWriter<?> jacksonMessageWriter = new EncoderHttpMessageWriter<>(
			new Jackson2JsonEncoder(objectMapper));

	private static final HttpMessageReader<?> jacksonMessageReader = new DecoderHttpMessageReader<>(
			new Jackson2JsonDecoder(objectMapper));

	@Test
	void fromWithEmptyWriters() {
		assertThat(JsonEncoderDecoder.from(List.of(), List.of(jacksonMessageReader))).isNull();
	}

	@Test
	void fromWithEmptyReaders() {
		assertThat(JsonEncoderDecoder.from(List.of(jacksonMessageWriter), List.of())).isNull();
	}

	@Test
	void fromWithSuitableWriterAndNoReader() {
		assertThat(JsonEncoderDecoder.from(List.of(jacksonMessageWriter), List.of(new ResourceHttpMessageReader()))).isNull();
	}

	@Test
	void fromWithSuitableReaderAndNoWriter() {
		assertThat(JsonEncoderDecoder.from(List.of(new ResourceHttpMessageWriter()), List.of(jacksonMessageReader))).isNull();
	}

	@Test
	void fromWithNoSuitableReaderAndWriter() {
		JsonEncoderDecoder jsonEncoderDecoder = JsonEncoderDecoder.from(
				List.of(new ResourceHttpMessageWriter(), jacksonMessageWriter),
				List.of(new ResourceHttpMessageReader(), jacksonMessageReader));
		assertThat(jsonEncoderDecoder).isNotNull();
		assertThat(jsonEncoderDecoder.encoder()).isInstanceOf(Jackson2JsonEncoder.class);
		assertThat(jsonEncoderDecoder.decoder()).isInstanceOf(Jackson2JsonDecoder.class);
	}

}
