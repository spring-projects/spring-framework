/*
 * Copyright 2002-2020 the original author or authors.
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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

	private Pojo pojo1 = new Pojo("f1", "b1");

	private Pojo pojo2 = new Pojo("f2", "b2");


	public Jackson2JsonDecoderTests() {
		super(new Jackson2JsonDecoder());
	}


	@Override
	@Test
	public void canDecode() {
		assertThat(decoder.canDecode(forClass(Pojo.class), APPLICATION_JSON)).isTrue();
		assertThat(decoder.canDecode(forClass(Pojo.class), APPLICATION_STREAM_JSON)).isTrue();
		assertThat(decoder.canDecode(forClass(Pojo.class), null)).isTrue();

		assertThat(decoder.canDecode(forClass(String.class), null)).isFalse();
		assertThat(decoder.canDecode(forClass(Pojo.class), APPLICATION_XML)).isFalse();
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
				.expectNext(asList(new Pojo("f1", "b1"), new Pojo("f2", "b2")))
				.expectComplete()
				.verify(), null, null);
	}


	@Test
	public void decodeEmptyArrayToFlux() {
		Flux<DataBuffer> input = Flux.from(stringBuffer("[]"));

		testDecode(input, Pojo.class, step -> step.verifyComplete());
	}

	@Test
	public void fieldLevelJsonView() {
		Flux<DataBuffer> input = Flux.from(
				stringBuffer("{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}"));
		ResolvableType elementType = forClass(JacksonViewBean.class);
		Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView1.class);

		testDecode(input, elementType, step -> step
				.consumeNextWith(o -> {
					JacksonViewBean b = (JacksonViewBean) o;
					assertThat(b.getWithView1()).isEqualTo("with");
					assertThat(b.getWithView2()).isNull();
					assertThat(b.getWithoutView()).isNull();
				}), null, hints);
	}

	@Test
	public void classLevelJsonView() {
		Flux<DataBuffer> input = Flux.from(stringBuffer(
				"{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}"));
		ResolvableType elementType = forClass(JacksonViewBean.class);
		Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView3.class);

		testDecode(input, elementType, step -> step
				.consumeNextWith(o -> {
					JacksonViewBean b = (JacksonViewBean) o;
					assertThat(b.getWithoutView()).isEqualTo("without");
					assertThat(b.getWithView1()).isNull();
					assertThat(b.getWithView2()).isNull();
				})
				.verifyComplete(), null, hints);
	}

	@Test
	public void invalidData() {
		Flux<DataBuffer> input =
				Flux.from(stringBuffer("{\"foofoo\": \"foofoo\", \"barbar\": \"barbar\""));
		testDecode(input, Pojo.class, step -> step
				.verifyError(DecodingException.class));
	}

	@Test // gh-22042
	public void decodeWithNullLiteral() {
		Flux<Object> result = this.decoder.decode(Flux.concat(stringBuffer("null")),
				ResolvableType.forType(Pojo.class), MediaType.APPLICATION_JSON, Collections.emptyMap());

		StepVerifier.create(result).expectComplete().verify();
	}

	@Test
	public void noDefaultConstructor() {
		Flux<DataBuffer> input =
				Flux.from(stringBuffer("{\"property1\":\"foo\",\"property2\":\"bar\"}"));
		ResolvableType elementType = forClass(BeanWithNoDefaultConstructor.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(input, elementType, null, emptyMap());
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
	public void decodeNonUtf8Encoding() {
		Mono<DataBuffer> input = stringBuffer("{\"foo\":\"bar\"}", StandardCharsets.UTF_16);

		testDecode(input, ResolvableType.forType(new ParameterizedTypeReference<Map<String, String>>() {
				}),
				step -> step.assertNext(o -> assertThat((Map<String, String>) o).containsEntry("foo", "bar"))
						.verifyComplete(),
				MediaType.parseMediaType("application/json; charset=utf-16"),
				null);
	}

	@Test
	public void decodeMonoNonUtf8Encoding() {
		Mono<DataBuffer> input = stringBuffer("{\"foo\":\"bar\"}", StandardCharsets.UTF_16);

		testDecodeToMono(input, ResolvableType.forType(new ParameterizedTypeReference<Map<String, String>>() {
				}),
				step -> step.assertNext(o -> assertThat((Map<String, String>) o).containsEntry("foo", "bar"))
						.verifyComplete(),
				MediaType.parseMediaType("application/json; charset=utf-16"),
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


	@SuppressWarnings("unused")
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
	public static class TestObject {

		private int test;

		public int getTest() {
			return this.test;
		}
		public void setTest(int test) {
			this.test = test;
		}
	}


	public static class Deserializer extends StdDeserializer<TestObject> {

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
