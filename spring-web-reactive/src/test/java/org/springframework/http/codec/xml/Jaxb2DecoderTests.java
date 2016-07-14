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

package org.springframework.http.codec.xml;

import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.codec.Pojo;
import org.springframework.http.codec.xml.jaxb.XmlRootElement;
import org.springframework.http.codec.xml.jaxb.XmlRootElementWithName;
import org.springframework.http.codec.xml.jaxb.XmlRootElementWithNameAndNamespace;
import org.springframework.http.codec.xml.jaxb.XmlType;
import org.springframework.http.codec.xml.jaxb.XmlTypeWithName;
import org.springframework.http.codec.xml.jaxb.XmlTypeWithNameAndNamespace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sebastien Deleuze
 */
public class Jaxb2DecoderTests extends AbstractDataBufferAllocatingTestCase {

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
				.decode(Flux.just(stringBuffer(POJO_ROOT)), null, null);
		Flux<List<XMLEvent>> result = this.decoder.split(xmlEvents, new QName("pojo"));

		TestSubscriber
				.subscribe(result)
				.assertNoError()
				.assertComplete()
				.assertValuesWith(events -> {
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
		Flux<XMLEvent> xmlEvents = this.xmlEventDecoder
				.decode(Flux.just(stringBuffer(POJO_CHILD)), null, null);
		Flux<List<XMLEvent>> result = this.decoder.split(xmlEvents, new QName("pojo"));

		TestSubscriber
				.subscribe(result)
				.assertNoError()
				.assertComplete()
				.assertValuesWith(events -> {
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
				this.decoder.decode(source, ResolvableType.forClass(Pojo.class), null);

		TestSubscriber
				.subscribe(output)
				.assertNoError()
				.assertComplete()
				.assertValues(new Pojo("foofoo", "barbar"));
	}

	@Test
	public void decodeSingleXmlTypeElement() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(POJO_ROOT));
		Flux<Object> output = this.decoder
				.decode(source, ResolvableType.forClass(TypePojo.class), null);

		TestSubscriber
				.subscribe(output)
				.assertNoError()
				.assertComplete()
				.assertValues(new TypePojo("foofoo", "barbar"));
	}

	@Test
	public void decodeMultipleXmlRootElement() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(POJO_CHILD));
		Flux<Object> output =
				this.decoder.decode(source, ResolvableType.forClass(Pojo.class), null);

		TestSubscriber
				.subscribe(output)
				.assertNoError()
				.assertComplete()
				.assertValues(new Pojo("foo", "bar"), new Pojo("foofoo", "barbar"));
	}

	@Test
	public void decodeMultipleXmlTypeElement() throws Exception {
		Flux<DataBuffer> source = Flux.just(stringBuffer(POJO_CHILD));
		Flux<Object> output = this.decoder
				.decode(source, ResolvableType.forClass(TypePojo.class), null);

		TestSubscriber
				.subscribe(output)
				.assertNoError()
				.assertComplete()
				.assertValues(new TypePojo("foo", "bar"), new TypePojo("foofoo", "barbar"));
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
