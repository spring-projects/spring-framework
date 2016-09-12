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

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.Pojo;
import org.springframework.tests.TestSubscriber;

import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 */
public class Jackson2JsonEncoderTests extends AbstractDataBufferAllocatingTestCase {

	private final Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();


	@Test
	public void canEncode() {
		assertTrue(this.encoder.canEncode(null, MediaType.APPLICATION_JSON, Collections.emptyMap()));
		assertFalse(this.encoder.canEncode(null, MediaType.APPLICATION_XML, Collections.emptyMap()));
	}

	@Test
	public void encode() {
		Flux<Pojo> source = Flux.just(
				new Pojo("foo", "bar"),
				new Pojo("foofoo", "barbar"),
				new Pojo("foofoofoo", "barbarbar")
		);
		ResolvableType type = ResolvableType.forClass(Pojo.class);
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory, type, null, Collections.emptyMap());

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
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory, type, null, Collections.emptyMap());

		TestSubscriber.subscribe(output)
				.assertComplete()
				.assertNoError()
				.assertValuesWith(stringConsumer("["),
						stringConsumer("{\"type\":\"foo\"}"),
						stringConsumer(","),
						stringConsumer("{\"type\":\"bar\"}"),
						stringConsumer("]"));
	}

	@Test
	public void jsonView() throws Exception {
		JacksonViewBean bean = new JacksonViewBean();
		bean.setWithView1("with");
		bean.setWithView2("with");
		bean.setWithoutView("without");

		ResolvableType type =  ResolvableType.forClass(JacksonViewBean.class);
		Map<String, Object> hints = Collections.singletonMap(Jackson2JsonEncoder.JSON_VIEW_HINT, MyJacksonView1.class);
		Flux<DataBuffer> output = this.encoder.encode(Mono.just(bean), this.bufferFactory, type, null, hints);

		TestSubscriber.subscribe(output)
				.assertComplete()
				.assertNoError()
				.assertValuesWith(stringConsumer("{\"withView1\":\"with\"}"));
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


	private interface MyJacksonView1 {}

	private interface MyJacksonView2 {}


	@SuppressWarnings("unused")
	private static class JacksonViewBean {

		@JsonView(MyJacksonView1.class)
		private String withView1;

		@JsonView(MyJacksonView2.class)
		private String withView2;

		private String withoutView;

		public String getWithView1() {
			return withView1;
		}

		public void setWithView1(String withView1) {
			this.withView1 = withView1;
		}

		public String getWithView2() {
			return withView2;
		}

		public void setWithView2(String withView2) {
			this.withView2 = withView2;
		}

		public String getWithoutView() {
			return withoutView;
		}

		public void setWithoutView(String withoutView) {
			this.withoutView = withoutView;
		}
	}

}
