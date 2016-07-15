/*
 * Copyright 2002-2016 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.Pojo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 */
public class JacksonJsonEncoderTests extends AbstractDataBufferAllocatingTestCase {

	private JacksonJsonEncoder encoder;


	@Before
	public void createEncoder() {
		this.encoder = new JacksonJsonEncoder();
	}


	@Test
	public void canEncode() {
		assertTrue(this.encoder.canEncode(null, MediaType.APPLICATION_JSON));
		assertFalse(this.encoder.canEncode(null, MediaType.APPLICATION_XML));
	}

	@Test
	public void encode() {
		Flux<Pojo> source = Flux.just(
				new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar")
		);
		ResolvableType type = ResolvableType.forClass(Pojo.class);
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory, type, null);

		TestSubscriber.subscribe(output)
				.assertComplete()
				.assertNoError()
				.assertValuesWith(
						stringConsumer("["),
						stringConsumer("{\"foo\":\"foo\",\"bar\":\"bar\"}"),
						stringConsumer(","),
						stringConsumer("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"),
						stringConsumer(","),
						stringConsumer("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}"),
						stringConsumer("]")
				);
	}

	@Test
	public void encodeWithType() {
		Flux<ParentClass> source = Flux.just(new Foo(), new Bar());
		ResolvableType type = ResolvableType.forClass(ParentClass.class);
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory, type, null);

		TestSubscriber.subscribe(output)
				.assertComplete()
				.assertNoError()
				.assertValuesWith(stringConsumer("["),
						stringConsumer("{\"type\":\"foo\"}"),
						stringConsumer(","),
						stringConsumer("{\"type\":\"bar\"}"),
						stringConsumer("]"));
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
