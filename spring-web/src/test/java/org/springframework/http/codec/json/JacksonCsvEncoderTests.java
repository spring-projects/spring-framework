/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.json;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier.LastStep;

import org.springframework.core.ResolvableType;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractJackson2Encoder} for the CSV variant and how resources are managed.
 * @author Brian Clozel
 */
@SuppressWarnings("removal")
class JacksonCsvEncoderTests extends AbstractEncoderTests<org.springframework.http.codec.json.JacksonCsvEncoderTests.JacksonCsvEncoder> {

	public JacksonCsvEncoderTests() {
		super(new JacksonCsvEncoder());
	}

	@Test
	@Override
	protected void canEncode() throws Exception {
		ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
		assertThat(this.encoder.canEncode(pojoType, JacksonCsvEncoder.TEXT_CSV)).isTrue();
	}

	@Test
	@Override
	protected void encode() throws Exception {
		Flux<Object> input = Flux.just(new Pojo("spring", "framework"),
				new Pojo("spring", "data"),
				new Pojo("spring", "boot"));

		testEncode(input, Pojo.class, step -> step
				.consumeNextWith(expectString("bar,foo\nframework,spring\n"))
				.consumeNextWith(expectString("data,spring\n"))
				.consumeNextWith(expectString("boot,spring\n"))
				.verifyComplete());
	}

	@Test
	// See gh-30493
	// this test did not fail directly but logged a NullPointerException dropped by the reactive pipeline
	void encodeEmptyFlux() {
		Flux<Object> input = Flux.empty();
		testEncode(input, Pojo.class, LastStep::verifyComplete);
	}

	static class JacksonCsvEncoder extends AbstractJackson2Encoder {
		public static final MediaType TEXT_CSV = new MediaType("text", "csv");

		public JacksonCsvEncoder() {
			this(CsvMapper.builder().build(), TEXT_CSV);
		}

		@Override
		protected byte[] getStreamingMediaTypeSeparator(MimeType mimeType) {
			// CsvMapper emits newlines
			return new byte[0];
		}

		public JacksonCsvEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
			super(mapper, mimeTypes);
			Assert.isInstanceOf(CsvMapper.class, mapper);
			setStreamingMediaTypes(List.of(TEXT_CSV));
		}

		@Override
		protected ObjectWriter customizeWriter(ObjectWriter writer, MimeType mimeType, ResolvableType elementType, Map<String, Object> hints) {
			var mapper = (CsvMapper) getObjectMapper();
			return writer.with(mapper.schemaFor(elementType.toClass()).withHeader());
		}
	}
}
