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
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractLeakCheckingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.Pojo;
import org.springframework.http.codec.xml.jaxb.XmlRootElement;
import org.springframework.http.codec.xml.jaxb.XmlRootElementWithName;
import org.springframework.http.codec.xml.jaxb.XmlRootElementWithNameAndNamespace;
import org.springframework.http.codec.xml.jaxb.XmlType;
import org.springframework.http.codec.xml.jaxb.XmlTypeWithName;
import org.springframework.http.codec.xml.jaxb.XmlTypeWithNameAndNamespace;

import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 */
public class Jaxb2XmlDecoderTests extends AbstractLeakCheckingTestCase {

	private static final String POJO_ROOT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<pojo>" +
			"<foo>foofoo</foo>" +
			"<bar>barbar</bar>" +
			"</pojo>";

	private static final String POJO_CHILD =
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
					"<root>" +
					"<pojo>" +
					"<foo>foo</foo>" +
					"<bar>bar</bar>" +
					"</pojo>" +
					"<pojo>" +
					"<foo>foofoo</foo>" +
					"<bar>barbar</bar>" +
					"</pojo>" +
					"<root/>";


	private final Jaxb2XmlDecoder decoder = new Jaxb2XmlDecoder();

	private final XmlEventDecoder xmlEventDecoder = new XmlEventDecoder();


	@Test
	public void canDecode() {
		assertTrue(this.decoder.canDecode(ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_XML));
		assertTrue(this.decoder.canDecode(ResolvableType.forClass(Pojo.class),
				MediaType.TEXT_XML));
		assertFalse(this.decoder.canDecode(ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_JSON));
		assertTrue(this.decoder.canDecode(ResolvableType.forClass(TypePojo.class),
				MediaType.APPLICATION_XML));
		assertFalse(this.decoder.canDecode(ResolvableType.forClass(getClass()),
				MediaType.APPLICATION_XML));
	}

	@Test
	public void splitOneBranches() {
		Flux<XMLEvent> xmlEvents = this.xmlEventDecoder
				.decode(stringBuffer(POJO_ROOT), null, null, Collections.emptyMap());
		Flux<List<XMLEvent>> result = this.decoder.split(xmlEvents, new QName("pojo"));

		StepVerifier.create(result)
				.consumeNextWith(events -> {
					assertEquals(8, events.size());
					assertStartElement(events.get(0), "pojo");
					assertStartElement(events.get(1), "foo");
					assertCharacters(events.get(2), "foofoo");
					assertEndElement(events.get(3), "foo");
					assertStartElement(events.get(4), "bar");
					assertCharacters(events.get(5), "barbar");
					assertEndElement(events.get(6), "bar");
					assertEndElement(events.get(7), "pojo");
				})
				.expectComplete()
				.verify();
	}

	@Test
	public void splitMultipleBranches() throws Exception {
		Flux<XMLEvent> xmlEvents = this.xmlEventDecoder
				.decode(stringBuffer(POJO_CHILD), null, null, Collections.emptyMap());
		Flux<List<XMLEvent>> result = this.decoder.split(xmlEvents, new QName("pojo"));


		StepVerifier.create(result)
				.consumeNextWith(events -> {
					assertEquals(8, events.size());
					assertStartElement(events.get(0), "pojo");
					assertStartElement(events.get(1), "foo");
					assertCharacters(events.get(2), "foo");
					assertEndElement(events.get(3), "foo");
					assertStartElement(events.get(4), "bar");
					assertCharacters(events.get(5), "bar");
					assertEndElement(events.get(6), "bar");
					assertEndElement(events.get(7), "pojo");
				})
				.consumeNextWith(events -> {
					assertEquals(8, events.size());
					assertStartElement(events.get(0), "pojo");
					assertStartElement(events.get(1), "foo");
					assertCharacters(events.get(2), "foofoo");
					assertEndElement(events.get(3), "foo");
					assertStartElement(events.get(4), "bar");
					assertCharacters(events.get(5), "barbar");
					assertEndElement(events.get(6), "bar");
					assertEndElement(events.get(7), "pojo");
				})
				.expectComplete()
				.verify();
	}

	private static void assertStartElement(XMLEvent event, String expectedLocalName) {
		assertTrue(event.isStartElement());
		assertEquals(expectedLocalName, event.asStartElement().getName().getLocalPart());
	}

	private static void assertEndElement(XMLEvent event, String expectedLocalName) {
		assertTrue(event.isEndElement());
		assertEquals(expectedLocalName, event.asEndElement().getName().getLocalPart());
	}

	private static void assertCharacters(XMLEvent event, String expectedData) {
		assertTrue(event.isCharacters());
		assertEquals(expectedData, event.asCharacters().getData());
	}

	@Test
	public void decodeSingleXmlRootElement() throws Exception {
		Mono<DataBuffer> source = stringBuffer(POJO_ROOT);
		Mono<Object> output = this.decoder.decodeToMono(source, ResolvableType.forClass(Pojo.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext(new Pojo("foofoo", "barbar"))
				.expectComplete()
				.verify();
	}

	@Test
	public void decodeSingleXmlTypeElement() throws Exception {
		Mono<DataBuffer> source = stringBuffer(POJO_ROOT);
		Mono<Object> output = this.decoder.decodeToMono(source, ResolvableType.forClass(TypePojo.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext(new TypePojo("foofoo", "barbar"))
				.expectComplete()
				.verify();
	}

	@Test
	public void decodeMultipleXmlRootElement() throws Exception {
		Mono<DataBuffer> source = stringBuffer(POJO_CHILD);
		Flux<Object> output = this.decoder.decode(source, ResolvableType.forClass(Pojo.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext(new Pojo("foo", "bar"))
				.expectNext(new Pojo("foofoo", "barbar"))
				.expectComplete()
				.verify();
	}

	@Test
	public void decodeMultipleXmlTypeElement() throws Exception {
		Mono<DataBuffer> source = stringBuffer(POJO_CHILD);
		Flux<Object> output = this.decoder.decode(source, ResolvableType.forClass(TypePojo.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectNext(new TypePojo("foo", "bar"))
				.expectNext(new TypePojo("foofoo", "barbar"))
				.expectComplete()
				.verify();
	}

	@Test
	public void decodeError() throws Exception {
		Flux<DataBuffer> source = Flux.concat(
				stringBuffer("<pojo>"),
				Flux.error(new RuntimeException()));

		Mono<Object> output = this.decoder.decodeToMono(source, ResolvableType.forClass(Pojo.class),
				null, Collections.emptyMap());

		StepVerifier.create(output)
				.expectError(RuntimeException.class)
				.verify();
	}

	@Test
	public void toExpectedQName() {
		assertEquals(new QName("pojo"), this.decoder.toQName(Pojo.class));
		assertEquals(new QName("pojo"), this.decoder.toQName(TypePojo.class));

		assertEquals(new QName("namespace", "name"),
				this.decoder.toQName(XmlRootElementWithNameAndNamespace.class));
		assertEquals(new QName("namespace", "name"),
				this.decoder.toQName(XmlRootElementWithName.class));
		assertEquals(new QName("namespace", "xmlRootElement"),
				this.decoder.toQName(XmlRootElement.class));

		assertEquals(new QName("namespace", "name"),
				this.decoder.toQName(XmlTypeWithNameAndNamespace.class));
		assertEquals(new QName("namespace", "name"),
				this.decoder.toQName(XmlTypeWithName.class));
		assertEquals(new QName("namespace", "xmlType"),
				this.decoder.toQName(XmlType.class));

	}

	private Mono<DataBuffer> stringBuffer(String value) {
		return Mono.defer(() -> {
			byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
			DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
			buffer.write(bytes);
			return Mono.just(buffer);
		});
	}


	@javax.xml.bind.annotation.XmlType(name = "pojo")
	public static class TypePojo {

		private String foo;

		private String bar;

		public TypePojo() {
		}

		public TypePojo(String foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}

		public String getFoo() {
			return this.foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof TypePojo) {
				TypePojo other = (TypePojo) o;
				return this.foo.equals(other.foo) && this.bar.equals(other.bar);
			}
			return false;
		}

		@Override
		public int hashCode() {
			int result = this.foo.hashCode();
			result = 31 * result + this.bar.hashCode();
			return result;
		}
	}
}
