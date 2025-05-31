/*
 * Copyright 2002-2025 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.json.JacksonViewBean.MyJacksonView1;
import org.springframework.http.codec.json.JacksonViewBean.MyJacksonView3;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.testfixture.xml.Pojo;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_NDJSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.APPLICATION_XML;

/**
 * Tests for {@link Jackson2JsonEncoder}.
 * @author Sebastien Deleuze
 */
@SuppressWarnings("removal")
class Jackson2JsonEncoderTests extends AbstractEncoderTests<Jackson2JsonEncoder> {

	public Jackson2JsonEncoderTests() {
		super(new Jackson2JsonEncoder());
	}

	@Override
	@Test
	public void canEncode() {
		ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
		assertThat(this.encoder.canEncode(pojoType, APPLICATION_JSON)).isTrue();
		assertThat(this.encoder.canEncode(pojoType, APPLICATION_NDJSON)).isTrue();
		assertThat(this.encoder.canEncode(pojoType, null)).isTrue();

		assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class),
				new MediaType("application", "json", StandardCharsets.UTF_8))).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class),
				new MediaType("application", "json", StandardCharsets.US_ASCII))).isTrue();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class),
				new MediaType("application", "json", StandardCharsets.ISO_8859_1))).isFalse();

		// SPR-15464
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isTrue();

		// SPR-15910
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Object.class), APPLICATION_OCTET_STREAM)).isFalse();
	}

	@Override
	@Test
	@SuppressWarnings("deprecation")
	public void encode() throws Exception {
		Flux<Object> input = Flux.just(new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar"));

		testEncodeAll(input, ResolvableType.forClass(Pojo.class), APPLICATION_NDJSON, null, step -> step
				.consumeNextWith(expectString("{\"foo\":\"foo\",\"bar\":\"bar\"}\n"))
				.consumeNextWith(expectString("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}\n"))
				.consumeNextWith(expectString("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}\n"))
				.verifyComplete()
		);
	}

	@Test  // SPR-15866
	public void canEncodeWithCustomMimeType() {
		MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(new ObjectMapper(), textJavascript);

		assertThat(encoder.getEncodableMimeTypes()).isEqualTo(Collections.singletonList(textJavascript));
	}

	@Test
	void encodableMimeTypesIsImmutable() {
		MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(new ObjectMapper(), textJavascript);

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				encoder.getMimeTypes().add(new MimeType("text", "ecmascript")));
	}

	@Test
	void canNotEncode() {
		assertThat(this.encoder.canEncode(ResolvableType.forClass(String.class), null)).isFalse();
		assertThat(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), APPLICATION_XML)).isFalse();

		ResolvableType sseType = ResolvableType.forClass(ServerSentEvent.class);
		assertThat(this.encoder.canEncode(sseType, APPLICATION_JSON)).isFalse();
	}

	@Test
	void encodeNonStream() {
		Flux<Pojo> input = Flux.just(
				new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar")
		);

		testEncode(input, Pojo.class, step -> step
				.consumeNextWith(expectString("[{\"foo\":\"foo\",\"bar\":\"bar\"}"))
				.consumeNextWith(expectString(",{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"))
				.consumeNextWith(expectString(",{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}"))
				.consumeNextWith(expectString("]"))
				.verifyComplete());
	}

	@Test
	void encodeNonStreamEmpty() {
		testEncode(Flux.empty(), Pojo.class, step -> step
				.consumeNextWith(expectString("["))
				.consumeNextWith(expectString("]"))
				.verifyComplete());
	}

	@Test  // gh-29038
	void encodeNonStreamWithErrorAsFirstSignal() {
		String message = "I'm a teapot";
		Flux<Object> input = Flux.error(new IllegalStateException(message));

		Flux<DataBuffer> output = this.encoder.encode(
				input, this.bufferFactory, ResolvableType.forClass(Pojo.class), null, null);

		StepVerifier.create(output).expectErrorMessage(message).verify();
	}

	@Test
	void encodeWithType() {
		Flux<ParentClass> input = Flux.just(new Foo(), new Bar());

		testEncode(input, ParentClass.class, step -> step
				.consumeNextWith(expectString("[{\"type\":\"foo\"}"))
				.consumeNextWith(expectString(",{\"type\":\"bar\"}"))
				.consumeNextWith(expectString("]"))
				.verifyComplete());
	}


	@Test  // SPR-15727
	public void encodeStreamWithCustomStreamingType() {
		MediaType fooMediaType = new MediaType("application", "foo");
		MediaType barMediaType = new MediaType("application", "bar");
		this.encoder.setStreamingMediaTypes(Arrays.asList(fooMediaType, barMediaType));
		Flux<Pojo> input = Flux.just(
				new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar")
		);

		testEncode(input, ResolvableType.forClass(Pojo.class), barMediaType, null, step -> step
				.consumeNextWith(expectString("{\"foo\":\"foo\",\"bar\":\"bar\"}\n"))
				.consumeNextWith(expectString("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}\n"))
				.consumeNextWith(expectString("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}\n"))
				.verifyComplete()
		);
	}

	@Test
	void fieldLevelJsonView() {
		JacksonViewBean bean = new JacksonViewBean();
		bean.setWithView1("with");
		bean.setWithView2("with");
		bean.setWithoutView("without");
		Mono<JacksonViewBean> input = Mono.just(bean);

		ResolvableType type = ResolvableType.forClass(JacksonViewBean.class);
		Map<String, Object> hints = singletonMap(org.springframework.http.codec.json.Jackson2CodecSupport.JSON_VIEW_HINT,
				MyJacksonView1.class);

		testEncode(input, type, null, hints, step -> step
				.consumeNextWith(expectString("{\"withView1\":\"with\"}"))
				.verifyComplete()
		);
	}

	@Test
	void classLevelJsonView() {
		JacksonViewBean bean = new JacksonViewBean();
		bean.setWithView1("with");
		bean.setWithView2("with");
		bean.setWithoutView("without");
		Mono<JacksonViewBean> input = Mono.just(bean);

		ResolvableType type = ResolvableType.forClass(JacksonViewBean.class);
		Map<String, Object> hints = singletonMap(org.springframework.http.codec.json.Jackson2CodecSupport.JSON_VIEW_HINT,
				MyJacksonView3.class);

		testEncode(input, type, null, hints, step -> step
				.consumeNextWith(expectString("{\"withoutView\":\"without\"}"))
				.verifyComplete()
		);
	}

	@Test
	void jacksonValue() {
		JacksonViewBean bean = new JacksonViewBean();
		bean.setWithView1("with");
		bean.setWithView2("with");
		bean.setWithoutView("without");

		MappingJacksonValue jacksonValue = new MappingJacksonValue(bean);
		jacksonValue.setSerializationView(MyJacksonView1.class);

		ResolvableType type = ResolvableType.forClass(MappingJacksonValue.class);

		testEncode(Mono.just(jacksonValue), type, null, Collections.emptyMap(), step -> step
				.consumeNextWith(expectString("{\"withView1\":\"with\"}"))
				.verifyComplete()
		);
	}

	@Test  // gh-28045
	public void jacksonValueUnwrappedBeforeObjectMapperSelection() {
		JacksonViewBean bean = new JacksonViewBean();
		bean.setWithView1("with");
		bean.setWithView2("with");
		bean.setWithoutView("without");

		MappingJacksonValue jacksonValue = new MappingJacksonValue(bean);
		jacksonValue.setSerializationView(MyJacksonView1.class);

		ResolvableType type = ResolvableType.forClass(MappingJacksonValue.class);

		MediaType halMediaType = MediaType.parseMediaType("application/hal+json");
		ObjectMapper mapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);
		this.encoder.registerObjectMappersForType(JacksonViewBean.class, map -> map.put(halMediaType, mapper));

		testEncode(Mono.just(jacksonValue), type, halMediaType, Collections.emptyMap(), step -> step
				.consumeNextWith(expectString("""
					{
					\s "withView1" : "with"
					}"""))
				.verifyComplete()
		);
	}

	@Test  // gh-22771
	public void encodeWithFlushAfterWriteOff() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, false);
		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(mapper);

		Flux<DataBuffer> result = encoder.encode(Flux.just(new Pojo("foo", "bar")), this.bufferFactory,
				ResolvableType.forClass(Pojo.class), MimeTypeUtils.APPLICATION_JSON, Collections.emptyMap());

		StepVerifier.create(result)
				.consumeNextWith(expectString("[{\"foo\":\"foo\",\"bar\":\"bar\"}"))
				.consumeNextWith(expectString("]"))
				.expectComplete()
				.verify(Duration.ofSeconds(5));
	}

	@Test
	void encodeAscii() {
		Mono<Object> input = Mono.just(new Pojo("foo", "bar"));
		MimeType mimeType = new MimeType("application", "json", StandardCharsets.US_ASCII);

		testEncode(input, ResolvableType.forClass(Pojo.class), mimeType, null, step -> step
				.consumeNextWith(expectString("{\"foo\":\"foo\",\"bar\":\"bar\"}"))
				.verifyComplete()
		);
	}


	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
	private static class ParentClass {
	}

	@JsonTypeName("foo")
	private static class Foo extends ParentClass {
	}

	@JsonTypeName("bar")
	private static class Bar extends ParentClass {
	}

}
