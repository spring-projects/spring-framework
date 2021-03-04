/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractDecoderTests;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonViewBean.MyJacksonView1;
import org.springframework.http.codec.json.JacksonViewBean.MyJacksonView3;
import org.springframework.util.MimeType;
import org.springframework.web.testfixture.xml.Pojo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_NDJSON;
import static org.springframework.http.MediaType.APPLICATION_STREAM_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.codec.json.Jackson2CodecSupport.JSON_VIEW_HINT;

/**
 * Unit tests for {@link Jackson2JsonDecoder}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class Jackson2JsonDecoderTests extends AbstractDecoderTests<Jackson2JsonDecoder> {

	private final Pojo pojo1 = new Pojo("f1", "b1");

	private final Pojo pojo2 = new Pojo("f2", "b2");


	public Jackson2JsonDecoderTests() {
		super(new Jackson2JsonDecoder());
	}


	@Override
	@Test
	public void canDecode() {
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), APPLICATION_JSON)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), APPLICATION_NDJSON)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), APPLICATION_STREAM_JSON)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), null)).isTrue();

		assertThat(decoder.canDecode(ResolvableType.forClass(String.class), null)).isFalse();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), APPLICATION_XML)).isFalse();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Pojo.class),
				new MediaType("application", "json", StandardCharsets.UTF_8))).isTrue();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Pojo.class),
				new MediaType("application", "json", StandardCharsets.US_ASCII))).isTrue();
		assertThat(this.decoder.canDecode(ResolvableType.forClass(Pojo.class),
				new MediaType("application", "json", StandardCharsets.ISO_8859_1))).isTrue();
	}

	@Test
	public void canDecodeWithObjectMapperRegistrationForType() {
				MediaType halJsonMediaType = MediaType.parseMediaType("application/hal+json");
		MediaType halFormsJsonMediaType = MediaType.parseMediaType("application/prs.hal-forms+json");

		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), halJsonMediaType)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), MediaType.APPLICATION_JSON)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), halFormsJsonMediaType)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Map.class), MediaType.APPLICATION_JSON)).isTrue();

		decoder.registerObjectMappersForType(Pojo.class, map -> {
			map.put(halJsonMediaType, new ObjectMapper());
			map.put(MediaType.APPLICATION_JSON, new ObjectMapper());
		});

		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), halJsonMediaType)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), MediaType.APPLICATION_JSON)).isTrue();
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo.class), halFormsJsonMediaType)).isFalse();
		assertThat(decoder.canDecode(ResolvableType.forClass(Map.class), MediaType.APPLICATION_JSON)).isTrue();

	}

	@Test  // SPR-15866
	public void canDecodeWithProvidedMimeType() {
		MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(new ObjectMapper(), textJavascript);

		assertThat(decoder.getDecodableMimeTypes()).isEqualTo(Collections.singletonList(textJavascript));
	}

	@Test
	public void decodableMimeTypesIsImmutable() {
		MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(new ObjectMapper(), textJavascript);

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				decoder.getMimeTypes().add(new MimeType("text", "ecmascript")));
	}

	@Override
	@Test
	public void decode() {
		Flux<DataBuffer> input = Flux.concat(
				stringBuffer("[{\"bar\":\"b1\",\"foo\":\"f1\"},"),
				stringBuffer("{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		testDecodeAll(input, Pojo.class, step -> step
				.expectNext(pojo1)
				.expectNext(pojo2)
				.verifyComplete());
	}

	@Override
	@Test
	public void decodeToMono() {
		Flux<DataBuffer> input = Flux.concat(
				stringBuffer("[{\"bar\":\"b1\",\"foo\":\"f1\"},"),
				stringBuffer("{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		ResolvableType elementType = ResolvableType.forClassWithGenerics(List.class, Pojo.class);

		testDecodeToMonoAll(input, elementType, step -> step
				.expectNext(Arrays.asList(new Pojo("f1", "b1"), new Pojo("f2", "b2")))
				.expectComplete()
				.verify(), null, null);
	}


	@Test
	public void decodeEmptyArrayToFlux() {
		Flux<DataBuffer> input = Flux.from(stringBuffer("[]"));

		testDecode(input, Pojo.class, StepVerifier.LastStep::verifyComplete);
	}

	@Test
	public void fieldLevelJsonView() {
		Flux<DataBuffer> input = Flux.from(stringBuffer(
				"{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}"));

		ResolvableType elementType = ResolvableType.forClass(JacksonViewBean.class);
		Map<String, Object> hints = Collections.singletonMap(JSON_VIEW_HINT, MyJacksonView1.class);

		testDecode(input, elementType, step -> step
				.consumeNextWith(value -> {
					JacksonViewBean bean = (JacksonViewBean) value;
					assertThat(bean.getWithView1()).isEqualTo("with");
					assertThat(bean.getWithView2()).isNull();
					assertThat(bean.getWithoutView()).isNull();
				}), null, hints);
	}

	@Test
	public void classLevelJsonView() {
		Flux<DataBuffer> input = Flux.from(stringBuffer(
				"{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}"));

		ResolvableType elementType = ResolvableType.forClass(JacksonViewBean.class);
		Map<String, Object> hints = Collections.singletonMap(JSON_VIEW_HINT, MyJacksonView3.class);

		testDecode(input, elementType, step -> step
				.consumeNextWith(value -> {
					JacksonViewBean bean = (JacksonViewBean) value;
					assertThat(bean.getWithoutView()).isEqualTo("without");
					assertThat(bean.getWithView1()).isNull();
					assertThat(bean.getWithView2()).isNull();
				})
				.verifyComplete(), null, hints);
	}

	@Test
	public void invalidData() {
		Flux<DataBuffer> input = Flux.from(stringBuffer("{\"foofoo\": \"foofoo\", \"barbar\": \"barbar\""));
		testDecode(input, Pojo.class, step -> step.verifyError(DecodingException.class));
	}

	@Test // gh-22042
	public void decodeWithNullLiteral() {
		Flux<Object> result = this.decoder.decode(Flux.concat(stringBuffer("null")),
				ResolvableType.forType(Pojo.class), MediaType.APPLICATION_JSON, Collections.emptyMap());

		StepVerifier.create(result).expectComplete().verify();
	}

	@Test
	public void noDefaultConstructor() {
		Flux<DataBuffer> input = Flux.from(stringBuffer("{\"property1\":\"foo\",\"property2\":\"bar\"}"));
		ResolvableType elementType = ResolvableType.forClass(BeanWithNoDefaultConstructor.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(input, elementType, null, Collections.emptyMap());
		StepVerifier.create(flux).verifyError(CodecException.class);
	}

	@Test  // SPR-15975
	public void  customDeserializer() {
		Mono<DataBuffer> input = stringBuffer("{\"test\": 1}");

		testDecode(input, TestObject.class, step -> step
				.consumeNextWith(o -> assertThat(o.getTest()).isEqualTo(1))
				.verifyComplete()
		);
	}

	@Test
	public void bigDecimalFlux() {
		Flux<DataBuffer> input = stringBuffer("[ 1E+2 ]").flux();

		testDecode(input, BigDecimal.class, step -> step
				.expectNext(new BigDecimal("1E+2"))
				.verifyComplete()
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void decodeNonUtf8Encoding() {
		Mono<DataBuffer> input = stringBuffer("{\"foo\":\"bar\"}", StandardCharsets.UTF_16);
		ResolvableType type = ResolvableType.forType(new ParameterizedTypeReference<Map<String, String>>() {});

		testDecode(input, type, step -> step
						.assertNext(value -> assertThat((Map<String, String>) value).containsEntry("foo", "bar"))
						.verifyComplete(),
				MediaType.parseMediaType("application/json; charset=utf-16"),
				null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void decodeNonUnicode() {
		Flux<DataBuffer> input = Flux.concat(stringBuffer("{\"føø\":\"bår\"}", StandardCharsets.ISO_8859_1));
		ResolvableType type = ResolvableType.forType(new ParameterizedTypeReference<Map<String, String>>() {});

		testDecode(input, type, step -> step
						.assertNext(o -> assertThat((Map<String, String>) o).containsEntry("føø", "bår"))
						.verifyComplete(),
				MediaType.parseMediaType("application/json; charset=iso-8859-1"),
				null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void decodeMonoNonUtf8Encoding() {
		Mono<DataBuffer> input = stringBuffer("{\"foo\":\"bar\"}", StandardCharsets.UTF_16);
		ResolvableType type = ResolvableType.forType(new ParameterizedTypeReference<Map<String, String>>() {});

		testDecodeToMono(input, type, step -> step
						.assertNext(value -> assertThat((Map<String, String>) value).containsEntry("foo", "bar"))
						.verifyComplete(),
				MediaType.parseMediaType("application/json; charset=utf-16"),
				null);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void decodeAscii() {
		Flux<DataBuffer> input = Flux.concat(stringBuffer("{\"foo\":\"bar\"}", StandardCharsets.US_ASCII));
		ResolvableType type = ResolvableType.forType(new ParameterizedTypeReference<Map<String, String>>() {});

		testDecode(input, type, step -> step
						.assertNext(value -> assertThat((Map<String, String>) value).containsEntry("foo", "bar"))
						.verifyComplete(),
				MediaType.parseMediaType("application/json; charset=us-ascii"),
				null);
	}


	private Mono<DataBuffer> stringBuffer(String value) {
		return stringBuffer(value, StandardCharsets.UTF_8);
	}

	private Mono<DataBuffer> stringBuffer(String value, Charset charset) {
		return Mono.defer(() -> {
			byte[] bytes = value.getBytes(charset);
			DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
			buffer.write(bytes);
			return Mono.just(buffer);
		});
	}


	private static class BeanWithNoDefaultConstructor {

		private final String property1;

		private final String property2;

		public BeanWithNoDefaultConstructor(String property1, String property2) {
			this.property1 = property1;
			this.property2 = property2;
		}

		public String getProperty1() {
			return this.property1;
		}

		public String getProperty2() {
			return this.property2;
		}
	}


	@JsonDeserialize(using = Deserializer.class)
	private static class TestObject {

		private int test;

		public int getTest() {
			return this.test;
		}
		public void setTest(int test) {
			this.test = test;
		}
	}


	private static class Deserializer extends StdDeserializer<TestObject> {

		private static final long serialVersionUID = 1L;

		protected Deserializer() {
			super(TestObject.class);
		}

		@Override
		public TestObject deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			JsonNode node = p.readValueAsTree();
			TestObject result = new TestObject();
			result.setTest(node.get("test").asInt());
			return result;
		}
	}

}
