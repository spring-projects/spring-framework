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

package org.springframework.http.codec.xml;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.Pojo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 */
public class Jaxb2XmlEncoderTests extends AbstractDataBufferAllocatingTestCase {

	private final Jaxb2XmlEncoder encoder = new Jaxb2XmlEncoder();


	@Test
	public void canEncode() {
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_XML));
		assertTrue(this.encoder.canEncode(ResolvableType.forClass(Pojo.class),
				MediaType.TEXT_XML));
		assertFalse(this.encoder.canEncode(ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_JSON));

		assertTrue(this.encoder.canEncode(
				ResolvableType.forClass(Jaxb2XmlDecoderTests.TypePojo.class),
				MediaType.APPLICATION_XML));

		assertFalse(this.encoder.canEncode(ResolvableType.forClass(getClass()),
				MediaType.APPLICATION_XML));

		// SPR-15464
		assertFalse(this.encoder.canEncode(ResolvableType.NONE, null));
	}

	@Test
	public void encode() {
		Mono<Pojo> source = Mono.just(new Pojo("foofoo", "barbar"));
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory,
				ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_XML, Collections.emptyMap());

		StepVerifier.create(output)
				.consumeNextWith(dataBuffer -> {
					try {
						String s = DataBufferTestUtils
								.dumpString(dataBuffer, StandardCharsets.UTF_8);
						assertThat(s, isSimilarTo("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
								"<pojo><bar>barbar</bar><foo>foofoo</foo></pojo>"));
					}
					finally {
						DataBufferUtils.release(dataBuffer);
					}
				})
				.verifyComplete();
	}

	@Test
	public void encodeElementsWithCommonType() {
		Mono<Container> source = Mono.just(new Container());
		Flux<DataBuffer> output = this.encoder.encode(source, this.bufferFactory,
				ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_XML, Collections.emptyMap());

		StepVerifier.create(output)
				.consumeNextWith(dataBuffer -> {
					try {
						String s = DataBufferTestUtils
								.dumpString(dataBuffer, StandardCharsets.UTF_8);
						assertThat(s, isSimilarTo("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
								"<container><foo><name>name1</name></foo><bar><title>title1</title></bar></container>"));
					}
					finally {
						DataBufferUtils.release(dataBuffer);
					}
				})
				.verifyComplete();
	}


	public static class Model {}

	public static class Foo extends Model {

		private String name;

		public Foo(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class Bar extends Model {

		private String title;

		public Bar(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}
	}

	@XmlRootElement
	public static class Container {

		@XmlElements({
				@XmlElement(name="foo", type=Foo.class),
				@XmlElement(name="bar", type=Bar.class)
		})
		public List<Model> getElements() {
			return Arrays.asList(new Foo("name1"), new Bar("title1"));
		}
	}

}
