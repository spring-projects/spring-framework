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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
 * Unit tests for {@link Jackson2JsonDecoder}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class Jackson2JsonDecoderTests extends AbstractDataBufferAllocatingTestCase {

	@Test
	public void canDecode() {
		Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();

		assertTrue(decoder.canDecode(null, MediaType.APPLICATION_JSON));
		assertFalse(decoder.canDecode(null, MediaType.APPLICATION_XML));
	}

	@Test
	public void decodePojo() {
		Flux<DataBuffer> source = Flux.just(stringBuffer("{\"foo\": \"foofoo\", \"bar\": \"barbar\"}"));
		ResolvableType elementType = ResolvableType.forClass(Pojo.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(source, elementType, null,
				Collections.emptyMap());

		TestSubscriber.subscribe(flux).assertNoError().assertComplete().
				assertValues(new Pojo("foofoo", "barbar"));
	}

	@Test
	public void decodeToList() {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		ResolvableType elementType = ResolvableType.forClassWithGenerics(List.class, Pojo.class);
		Mono<Object> mono = new Jackson2JsonDecoder().decodeToMono(source, elementType,
				null, Collections.emptyMap());

		TestSubscriber.subscribe(mono).assertNoError().assertComplete().
				assertValues(Arrays.asList(new Pojo("f1", "b1"), new Pojo("f2", "b2")));
	}

	@Test
	public void decodeToFlux() {
		Flux<DataBuffer> source = Flux.just(stringBuffer(
				"[{\"bar\":\"b1\",\"foo\":\"f1\"},{\"bar\":\"b2\",\"foo\":\"f2\"}]"));

		ResolvableType elementType = ResolvableType.forClass(Pojo.class);
		Flux<Object> flux = new Jackson2JsonDecoder().decode(source, elementType, null,
				Collections.emptyMap());

		TestSubscriber.subscribe(flux).assertNoError().assertComplete().
				assertValues(new Pojo("f1", "b1"), new Pojo("f2", "b2"));
	}

	@Test
	public void jsonView() {
		Flux<DataBuffer> source = Flux.just(
				stringBuffer("{\"withView1\" : \"with\", \"withView2\" : \"with\", \"withoutView\" : \"without\"}"));
		ResolvableType elementType =  ResolvableType.forClass(JacksonViewBean.class);
		Map<String, Object> hints = Collections.singletonMap(Jackson2JsonDecoder.JSON_VIEW_HINT, MyJacksonView1.class);
		Flux<JacksonViewBean> flux = new Jackson2JsonDecoder()
				.decode(source, elementType, null, hints).cast(JacksonViewBean.class);

		TestSubscriber
				.subscribe(flux)
				.assertNoError()
				.assertComplete()
				.assertValuesWith(b -> {
					assertTrue(b.getWithView1().equals("with"));
					assertNull(b.getWithView2());
					assertNull(b.getWithoutView());
				});
	}

	@Test
	public void decodeEmptyBodyToMono() {
		Flux<DataBuffer> source = Flux.empty();
		ResolvableType elementType = ResolvableType.forClass(Pojo.class);
		Mono<Object> flux = new Jackson2JsonDecoder().decodeToMono(source, elementType,
				null, Collections.emptyMap());

		TestSubscriber.subscribe(flux)
				.assertNoError()
				.assertComplete()
				.assertValueCount(0);
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
