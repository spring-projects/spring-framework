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

package org.springframework.http.codec.json;

import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

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
class JacksonCsvEncoderTests extends AbstractEncoderTests<org.springframework.http.codec.json.JacksonCsvEncoderTests.JacksonCsvEncoder> {

	public JacksonCsvEncoderTests() {
		super(new JacksonCsvEncoder());
	}

	@Test
	@Override
	public void canEncode() throws Exception {
		ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
		assertThat(this.encoder.canEncode(pojoType, JacksonCsvEncoder.TEXT_CSV)).isTrue();
	}

	@Test
	@Override
	public void encode() throws Exception {
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
		testEncode(input, Pojo.class, step -> step.verifyComplete());
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
			setStreamingMediaTypes(Arrays.asList(TEXT_CSV));
		}

		@Override
		protected ObjectWriter customizeWriter(ObjectWriter writer, MimeType mimeType, ResolvableType elementType, Map<String, Object> hints) {
			CsvMapper mapper = (CsvMapper) getObjectMapper();
			return writer.with(mapper.schemaFor(elementType.toClass()).withHeader());
		}
	}
}
