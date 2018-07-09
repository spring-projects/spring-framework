/*
 * Copyright 2002-2018 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import static java.util.Collections.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.codec.json.Jackson2JsonEncoder.*;
import static org.springframework.http.codec.json.JacksonViewBean.*;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.Pojo;
import org.springframework.http.codec.ServerSentEvent;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 */
public class Jackson2JsonEncoderTests extends AbstractDataBufferAllocatingTestCase {

	private final Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();


	@Test
	public void canEncode() {
		ResolvableType pojoType = ResolvableType.forClass(Pojo.class);
		assertTrue(this.encoder.canEncode(pojoType, APPLICATION_JSON));
		assertTrue(this.encoder.canEncode(pojoType, APPLICATION_JSON_UTF8));
		assertTrue(this.encoder.canEncode(pojoType, APPLICATION_STREAM_JSON));
		assertTrue(this.encoder.canEncode(pojoType, null));

		// SPR-15464
		assertTrue(this.encoder.canEncode(ResolvableType.NONE, null));

		// SPR-15910
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Object.class), APPLICATION_OCTET_STREAM));
	}

	@Test // SPR-15866
	public void canEncodeWithCustomMimeType() {
		MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(new ObjectMapper(), textJavascript);

		assertEquals(Collections.singletonList(textJavascript), encoder.getEncodableMimeTypes());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void encodableMimeTypesIsImmutable() {
		MimeType textJavascript = new MimeType("text", "javascript", StandardCharsets.UTF_8);
		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(new ObjectMapper(), textJavascript);

		encoder.getMimeTypes().add(new MimeType("text", "ecmascript"));
	}

	@Test
	public void canNotEncode() {
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(String.class), null));
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Pojo.class), APPLICATION_XML));

		ResolvableType sseType = ResolvableType.forClass(ServerSentEvent.class);
		assertFalse(this.encoder.canEncode(sseType, APPLICATION_JSON));
	}

	@Test
	public void encode() throws Exception {
		Flux<Pojo> source = Flux.just(
				new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar")
		);
		ResolvableType type = ResolvableType.forClass(Pojo.class);
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory, type, null, emptyMap());

		StepVerifier.create(output)
				.consumeNextWith(stringConsumer("[" +
						"{\"foo\":\"foo\",\"bar\":\"bar\"}," +
						"{\"foo\":\"foofoo\",\"bar\":\"barbar\"}," +
						"{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}]"))
				.verifyComplete();
	}

	@Test
	public void encodeWithType() throws Exception {
		Flux<ParentClass> source = Flux.just(new Foo(), new Bar());
		ResolvableType type = ResolvableType.forClass(ParentClass.class);
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory, type, null, emptyMap());

		StepVerifier.create(output)
				.consumeNextWith(stringConsumer("[{\"type\":\"foo\"},{\"type\":\"bar\"}]"))
				.verifyComplete();
	}

	@Test
	public void encodeAsStream() throws Exception {
		Flux<Pojo> source = Flux.just(
				new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar")
		);
		ResolvableType type = ResolvableType.forClass(Pojo.class);
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory, type, APPLICATION_STREAM_JSON, emptyMap());

		StepVerifier.create(output)
				.consumeNextWith(stringConsumer("{\"foo\":\"foo\",\"bar\":\"bar\"}\n"))
				.consumeNextWith(stringConsumer("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}\n"))
				.consumeNextWith(stringConsumer("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}\n"))
				.verifyComplete();
	}

	@Test  // SPR-15727
	public void encodeAsStreamWithCustomStreamingType() throws Exception {
		MediaType fooMediaType = new MediaType("application", "foo");
		MediaType barMediaType = new MediaType("application", "bar");
		this.encoder.setStreamingMediaTypes(Arrays.asList(fooMediaType, barMediaType));
		Flux<Pojo> source = Flux.just(
				new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar")
		);
		ResolvableType type = ResolvableType.forClass(Pojo.class);
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory, type, barMediaType, emptyMap());

		StepVerifier.create(output)
				.consumeNextWith(stringConsumer("{\"foo\":\"foo\",\"bar\":\"bar\"}\n"))
				.consumeNextWith(stringConsumer("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}\n"))
				.consumeNextWith(stringConsumer("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}\n"))
				.verifyComplete();
	}

	@Test
	public void fieldLevelJsonView() throws Exception {
		JacksonViewBean bean = new JacksonViewBean();
		bean.setWithView1("with");
		bean.setWithView2("with");
		bean.setWithoutView("without");

		ResolvableType type = ResolvableType.forClass(JacksonViewBean.class);
		Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView1.class);
		Flux<DataBuffer> output = this.encoder.encode(Mono.just(bean), this.bufferFactory, type, null, hints);

		StepVerifier.create(output)
				.consumeNextWith(stringConsumer("{\"withView1\":\"with\"}"))
				.verifyComplete();
	}

	@Test
	public void classLevelJsonView() throws Exception {
		JacksonViewBean bean = new JacksonViewBean();
		bean.setWithView1("with");
		bean.setWithView2("with");
		bean.setWithoutView("without");

		ResolvableType type = ResolvableType.forClass(JacksonViewBean.class);
		Map<String, Object> hints = singletonMap(JSON_VIEW_HINT, MyJacksonView3.class);
		Flux<DataBuffer> output = this.encoder.encode(Mono.just(bean), this.bufferFactory, type, null, hints);

		StepVerifier.create(output)
				.consumeNextWith(stringConsumer("{\"withoutView\":\"without\"}"))
				.verifyComplete();
	}


	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	private static class ParentClass {
	}

	@JsonTypeName("foo")
	private static class Foo extends ParentClass {
	}

	@JsonTypeName("bar")
	private static class Bar extends ParentClass {
	}

}
