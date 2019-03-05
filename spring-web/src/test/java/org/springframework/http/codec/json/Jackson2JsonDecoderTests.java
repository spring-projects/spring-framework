/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.http.codec.json;

import java.io.IOException;
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
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoderTestCase;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.Pojo;
import org.springframework.util.MimeType;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.junit.Assert.*;
import static org.springframework.core.ResolvableType.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.codec.json.Jackson2JsonDecoder.*;
import static org.springframework.http.codec.json.JacksonViewBean.*;

/**
 * Unit tests for {@link Jackson2JsonDecoder}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class Jackson2JsonDecoderTests extends AbstractDecoderTestCase<Jackson2JsonDecoder> {

	private Pojo pojo1 = new Pojo("f1", "b1");

	private Pojo pojo2 = new Pojo("f2", "b2");


	public Jackson2JsonDecoderTests() {
		super(new Jackson2JsonDecoder());
	}


	@Override
	@Test
	public void canDecode() {
		assertTrue(decoder.canDecode(forClass(Pojo.class), APPLICATION_JSON));
		assertTrue(decoder.canDecode(forClass(Pojo.class), APPLICATION_JSON_UTF8));
		assertTrue(decoder.canDecode(forClass(Pojo.class), APPLICATION_STREAM_JSON));
		assertTrue(decoder.canDecode(forClass(Pojo.class), null));

		assertFalse(decoder.canDecode(forClass(String.class), null));
		assertFalse(decoder.canDecode(forClass(Pojo.class), APPLICATION_XML));
	}

	@Test  // SPR-15866
	public void canDecodeWithProvidedMimeType() {
		MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(new ObjectMapper(), textJavascript);

		assertEquals(Collections.singletonList(textJavascript), decoder.getDecodableMimeTypes());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void decodableMimeTypesIsImmutable() {
		MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(new ObjectMapper(), textJavascript);

		decoder.getMimeTypes().add(new MimeType("text", "ecmascript"));
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
					assertEquals("with", b.getWithView1());
					assertNull(b.getWithView2());
					assertNull(b.getWithoutView());
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
					assertEquals("without", b.getWithoutView());
					assertNull(b.getWithView1());
					assertNull(b.getWithView2());
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
				.consumeNextWith(o -> assertEquals(1, o.getTest()))
				.verifyComplete()
		);
	}

	private Mono<DataBuffer> stringBuffer(String value) {
		return Mono.defer(() -> {
			byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
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
