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
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
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
public class Jackson2JsonDecoderTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void canDecode() {
		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();

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

	@Test
	public void decodePojo() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"));
		ResolvableType elementType = forClass(Pojo.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(source, elementType, null,
				emptyMap());

		StepVerifier.create(flux)
				.expectNext(new Pojo("foofoo", "barbar"))
				.verifyComplete();
	}

	@Test
	public void decodePojoWithError() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer("{\"foo\":}"));
		ResolvableType elementType = forClass(Pojo.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(source, elementType, null,
				emptyMap());

		StepVerifier.create(flux).verifyError(CodecException.class);
	}

	@Test
	public void decodeToList() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		ResolvableType elementType = ResolvableType.forClassWithGenerics(List.class, Pojo.class);
		Mono<Object> mono = new Jackson2JsonDecoder().decodeToMono(source, elementType,
				null, emptyMap());

		StepVerifier.create(mono)
				.expectNext(asList(new Pojo("f1", "b1"), new Pojo("f2", "b2")))
				.expectComplete()
				.verify();
	}

	@Test
	public void decodeArrayToFlux() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		ResolvableType elementType = forClass(Pojo.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(source, elementType, null,
				emptyMap());

		StepVerifier.create(flux)
				.expectNext(new Pojo("f1", "b1"))
				.expectNext(new Pojo("f2", "b2"))
				.verifyComplete();
	}

	@Test
	public void decodeStreamToFlux() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer("{\"bar\":\"b1\",\"foo\":\"f1\"}"),
				stringBuffer("{\"bar\":\"b2\",\"foo\":\"f2\"}"));

		ResolvableType elementType = forClass(Pojo.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(source, elementType, APPLICATION_STREAM_JSON,
				emptyMap());

		StepVerifier.create(flux)
				.expectNext(new Pojo("f1", "b1"))
				.expectNext(new Pojo("f2", "b2"))
				.verifyComplete();
	}

	@Test
	public void decodeEmptyArrayToFlux() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer("[]"));
		ResolvableType elementType = forClass(Pojo.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(source, elementType, null, emptyMap());

		StepVerifier.create(flux)
				.expectNextCount(0)
				.verifyComplete();
	}

	@Test
	public void fieldLevelJsonView() throws Exception {
		Flux<DataBuffer> source = Flux.just(
				stringBuffer("{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}"));
		ResolvableType elementType = forClass(JacksonViewBean.class);
		Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView1.class);
		Flux<JacksonViewBean> flux = new Jackson2JsonDecoder()
				.decode(source, elementType, null, hints).cast(JacksonViewBean.class);

		StepVerifier.create(flux)
				.consumeNextWith(b -> {
					assertTrue(b.getWithView1().equals("with"));
					assertNull(b.getWithView2());
					assertNull(b.getWithoutView());
				})
				.verifyComplete();
	}

	@Test
	public void classLevelJsonView() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}"));
		ResolvableType elementType = forClass(JacksonViewBean.class);
		Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView3.class);
		Flux<JacksonViewBean> flux = new Jackson2JsonDecoder()
				.decode(source, elementType, null, hints).cast(JacksonViewBean.class);

		StepVerifier.create(flux)
				.consumeNextWith(b -> {
					assertNull(b.getWithView1());
					assertNull(b.getWithView2());
					assertTrue(b.getWithoutView().equals("without"));
				})
				.verifyComplete();
	}

	@Test
	public void decodeEmptyBodyToMono() throws Exception {
		Flux<DataBuffer> source = Flux.empty();
		ResolvableType elementType = forClass(Pojo.class);
		Mono<Object> mono = new Jackson2JsonDecoder().decodeToMono(source, elementType, null, emptyMap());

		StepVerifier.create(mono)
				.expectNextCount(0)
				.verifyComplete();
	}

	@Test
	public void invalidData() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer( "{\"foofoo\": \"foofoo\", \"barbar\": \"barbar\"}"));
		ResolvableType elementType = forClass(Pojo.class);
		Flux<Object> flux = new Jackson2JsonDecoder(new ObjectMapper()).decode(source, elementType, null, emptyMap());
		StepVerifier.create(flux).verifyErrorMatches(ex -> ex instanceof DecodingException);
	}

	@Test
	public void noDefaultConstructor() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer( "{\"property1\":\"foo\",\"property2\":\"bar\"}"));
		ResolvableType elementType = forClass(BeanWithNoDefaultConstructor.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(source, elementType, null, emptyMap());
		StepVerifier.create(flux).verifyError(CodecException.class);
	}

	@Test  // SPR-15975
	public void  customDeserializer() {
		DataBuffer buffer = new DefaultDataBufferFactory().wrap("{\"test\": 1}".getBytes());

		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(new ObjectMapper());
		Flux<TestObject> decoded = decoder.decode(Mono.just(buffer),
				ResolvableType.forClass(TestObject.class), null, null).cast(TestObject.class);

		StepVerifier.create(decoded)
				.assertNext(v -> assertEquals(1, v.getTest()))
				.verifyComplete();
	}


	private static class BeanWithNoDefaultConstructor {

		private final String property1;

		private final String property2;

		public BeanWithNoDefaultConstructor(String property1, String property2) {
			this.property1 = property1;
			this.property2 = property2;
		}

		public String getProperty1() {
			return property1;
		}

		public String getProperty2() {
			return property2;
		}
	}


	@JsonDeserialize(using = Deserializer.class)
	public static class TestObject {

		private int test;

		public int getTest() {
			return test;
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
