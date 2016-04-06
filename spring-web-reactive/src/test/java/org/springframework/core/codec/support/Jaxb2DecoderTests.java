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

package org.springframework.core.codec.support;

import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.support.jaxb.XmlRootElement;
import org.springframework.core.codec.support.jaxb.XmlRootElementWithName;
import org.springframework.core.codec.support.jaxb.XmlRootElementWithNameAndNamespace;
import org.springframework.core.codec.support.jaxb.XmlType;
import org.springframework.core.codec.support.jaxb.XmlTypeWithName;
import org.springframework.core.codec.support.jaxb.XmlTypeWithNameAndNamespace;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;

import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 */
public class Jaxb2DecoderTests extends AbstractAllocatingTestCase {

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

	private final Jaxb2Decoder decoder = new Jaxb2Decoder();

	private final XmlEventDecoder xmlEventDecoder = new XmlEventDecoder();


	@Test
	public void canDecode() {
		assertTrue(decoder.canDecode(ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_XML));
		assertTrue(decoder.canDecode(ResolvableType.forClass(Pojo.class),
				MediaType.TEXT_XML));
		assertFalse(decoder.canDecode(ResolvableType.forClass(Pojo.class),
				MediaType.APPLICATION_JSON));

		assertTrue(decoder.canDecode(ResolvableType.forClass(TypePojo.class),
				MediaType.APPLICATION_XML));

		assertFalse(decoder.canDecode(ResolvableType.forClass(getClass()),
				MediaType.APPLICATION_XML));
	}

	@Test
	public void splitOneBranches() {
		Flux<XMLEvent> xmlEvents =
				xmlEventDecoder.decode(Flux.just(stringBuffer(POJO_ROOT)), null, null);
		Flux<List<XMLEvent>> result = decoder.split(xmlEvents, new QName("pojo"));

		TestSubscriber<List<XMLEvent>> resultSubscriber = new TestSubscriber<>();
		resultSubscriber.bindTo(result).
				assertNoError().
				assertComplete().
				assertValuesWith(events -> {
					assertEquals(8, events.size());
					assertStartElement(events.get(0), "pojo");
					assertStartElement(events.get(1), "foo");
					assertCharacters(events.get(2), "foofoo");
					assertEndElement(events.get(3), "foo");
					assertStartElement(events.get(4), "bar");
					assertCharacters(events.get(5), "barbar");
					assertEndElement(events.get(6), "bar");
					assertEndElement(events.get(7), "pojo");
				});


	}

	@Test
	public void splitMultipleBranches() {
		Flux<XMLEvent> xmlEvents =
				xmlEventDecoder.decode(Flux.just(stringBuffer(POJO_CHILD)), null, null);
		Flux<List<XMLEvent>> result = decoder.split(xmlEvents, new QName("pojo"));

		TestSubscriber<List<XMLEvent>> resultSubscriber = new TestSubscriber<>();
		resultSubscriber.bindTo(result).
				assertNoError().
				assertComplete().
				assertValuesWith(events -> {
					assertEquals(8, events.size());
					assertStartElement(events.get(0), "pojo");
					assertStartElement(events.get(1), "foo");
					assertCharacters(events.get(2), "foo");
					assertEndElement(events.get(3), "foo");
					assertStartElement(events.get(4), "bar");
					assertCharacters(events.get(5), "bar");
					assertEndElement(events.get(6), "bar");
					assertEndElement(events.get(7), "pojo");
				}, events -> {
					assertEquals(8, events.size());
					assertStartElement(events.get(0), "pojo");
					assertStartElement(events.get(1), "foo");
					assertCharacters(events.get(2), "foofoo");
					assertEndElement(events.get(3), "foo");
					assertStartElement(events.get(4), "bar");
					assertCharacters(events.get(5), "barbar");
					assertEndElement(events.get(6), "bar");
					assertEndElement(events.get(7), "pojo");
				});
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
		Flux<DataBuffer> source = Flux.just(stringBuffer(POJO_ROOT));
		Flux<Object> output =
				decoder.decode(source, ResolvableType.forClass(Pojo.class), null);

		TestSubscriber<Object> testSubscriber = new TestSubscriber<>();

		testSubscriber.bindTo(output).
				assertNoError().
				assertComplete().
				assertValues(new Pojo("foofoo", "barbar")

				);
	}

	@Test
	public void decodeSingleXmlTypeElement() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(POJO_ROOT));
		Flux<Object> output =
				decoder.decode(source, ResolvableType.forClass(TypePojo.class), null);

		TestSubscriber<Object> testSubscriber = new TestSubscriber<>();

		testSubscriber.bindTo(output).
				assertNoError().
				assertComplete().
				assertValues(new TypePojo("foofoo", "barbar")

				);
	}

	@Test
	public void decodeMultipleXmlRootElement() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(POJO_CHILD));
		Flux<Object> output =
				decoder.decode(source, ResolvableType.forClass(Pojo.class), null);

		TestSubscriber<Object> testSubscriber = new TestSubscriber<>();

		testSubscriber.bindTo(output).
				assertNoError().
				assertComplete().
				assertValues(new Pojo("foo", "bar"), new Pojo("foofoo", "barbar")

				);
	}

	@Test
	public void decodeMultipleXmlTypeElement() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(POJO_CHILD));
		Flux<Object> output =
				decoder.decode(source, ResolvableType.forClass(TypePojo.class), null);

		TestSubscriber<Object> testSubscriber = new TestSubscriber<>();

		testSubscriber.bindTo(output).
				assertNoError().
				assertComplete().
				assertValues(new TypePojo("foo", "bar"), new TypePojo("foofoo", "barbar")

				);
	}

	@Test
	public void toExpectedQName() {
		assertEquals(new QName("pojo"), decoder.toQName(Pojo.class));
		assertEquals(new QName("pojo"), decoder.toQName(TypePojo.class));

		assertEquals(new QName("namespace", "name"),
				decoder.toQName(XmlRootElementWithNameAndNamespace.class));
		assertEquals(new QName("namespace", "name"),
				decoder.toQName(XmlRootElementWithName.class));
		assertEquals(new QName("namespace", "xmlRootElement"),
				decoder.toQName(XmlRootElement.class));

		assertEquals(new QName("namespace", "name"),
				decoder.toQName(XmlTypeWithNameAndNamespace.class));
		assertEquals(new QName("namespace", "name"),
				decoder.toQName(XmlTypeWithName.class));
		assertEquals(new QName("namespace", "xmlType"), decoder.toQName(XmlType.class));

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

	}
}
