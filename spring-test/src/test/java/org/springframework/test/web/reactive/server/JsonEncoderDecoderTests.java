/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageReader;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JsonEncoderDecoder}.
 *
 * @author Stephane Nicoll
 */
class JsonEncoderDecoderTests {

	private static final JsonMapper jsonMapper = new JsonMapper();

	private static final HttpMessageWriter<?> jacksonMessageWriter = new EncoderHttpMessageWriter<>(
			new JacksonJsonEncoder(jsonMapper));

	private static final HttpMessageReader<?> jacksonMessageReader = new DecoderHttpMessageReader<>(
			new JacksonJsonDecoder(jsonMapper));

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
		assertThat(jsonEncoderDecoder.encoder()).isInstanceOf(JacksonJsonEncoder.class);
		assertThat(jsonEncoderDecoder.decoder()).isInstanceOf(JacksonJsonDecoder.class);
	}

}
