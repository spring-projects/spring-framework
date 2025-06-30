/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http.codec.xml;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.testfixture.codec.AbstractEncoderTests;
import org.springframework.core.testfixture.xml.XmlContent;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.xml.Pojo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.core.io.buffer.DataBufferUtils.release;

/**
 * Tests for {@link Jaxb2XmlEncoder}.
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 */
class Jaxb2XmlEncoderTests extends AbstractEncoderTests<Jaxb2XmlEncoder> {

	public Jaxb2XmlEncoderTests() {
		super(new Jaxb2XmlEncoder());
	}

	@Override
	@Test
	protected void canEncode() {
		assertThat(this.encoder.canEncode(forClass(Pojo.class), MediaType.APPLICATION_XML)).isTrue();
		assertThat(this.encoder.canEncode(forClass(Pojo.class), MediaType.TEXT_XML)).isTrue();
		assertThat(this.encoder.canEncode(forClass(Pojo.class), new MediaType("application", "foo+xml"))).isTrue();
		assertThat(this.encoder.canEncode(forClass(Pojo.class), MediaType.APPLICATION_JSON)).isFalse();

		assertThat(this.encoder.canEncode(forClass(TypePojo.class), MediaType.APPLICATION_XML)).isTrue();
		assertThat(this.encoder.canEncode(forClass(getClass()), MediaType.APPLICATION_XML)).isFalse();

		assertThat(this.encoder.canEncode(forClass(JAXBElement.class), MediaType.APPLICATION_XML)).isTrue();
		assertThat(this.encoder.canEncode(forClass(JAXBElementSubclass.class), MediaType.APPLICATION_XML)).isTrue();

		// SPR-15464
		assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
	}

	@Override
	@Test
	protected void encode() {
		Mono<Pojo> input = Mono.just(new Pojo("foofoo", "barbar"));

		testEncode(input, Pojo.class, step -> step
				.consumeNextWith(expectXml(
						"<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
								"<pojo><bar>barbar</bar><foo>foofoo</foo></pojo>"))
				.verifyComplete());
	}

	@Test
	void encodeJaxbElement() {
		Mono<JAXBElement<Pojo>> input = Mono.just(new JAXBElement<>(new QName("baz"), Pojo.class,
				new Pojo("foofoo", "barbar")));

		testEncode(input, Pojo.class, step -> step
				.consumeNextWith(expectXml(
						"<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
								"<baz><bar>barbar</bar><foo>foofoo</foo></baz>"))
				.verifyComplete());
	}

	@Test
	void encodeError() {
		Flux<Pojo> input = Flux.error(RuntimeException::new);
		testEncode(input, Pojo.class, step -> step.expectError(RuntimeException.class).verify());
	}

	@Test
	void encodeElementsWithCommonType() {
		Mono<Container> input = Mono.just(new Container());

		testEncode(input, Pojo.class, step -> step
				.consumeNextWith(expectXml(
						"<?xml version='1.0' encoding='UTF-8' standalone='yes'?>" +
								"<container>" +
								"<foo><name>name1</name></foo><bar><title>title1</title></bar>" +
								"</container>"))
				.verifyComplete());
	}

	protected Consumer<DataBuffer> expectXml(String expected) {
		return dataBuffer -> {
			byte[] resultBytes = new byte[dataBuffer.readableByteCount()];
			dataBuffer.read(resultBytes);
			release(dataBuffer);
			String actual = new String(resultBytes, UTF_8);
			assertThat(XmlContent.from(actual)).isSimilarTo(expected);
		};
	}

	public static class JAXBElementSubclass extends JAXBElement<Pojo> {
		@Serial
		private static final long serialVersionUID = 1L;

		protected static final QName NAME = new QName("http://foo/schema/common/1.0", "Pojo");

		public JAXBElementSubclass() {
			super(NAME, Pojo.class, null, null);
		}
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
