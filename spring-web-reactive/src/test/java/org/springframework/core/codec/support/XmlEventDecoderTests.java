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

import javax.xml.stream.events.XMLEvent;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.test.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Arjen Poutsma
 */
public class XmlEventDecoderTests extends AbstractAllocatingTestCase {

	private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<pojo>" +
			"<foo>foofoo</foo>" +
			"<bar>barbar</bar>" +
			"</pojo>";

	private XmlEventDecoder decoder = new XmlEventDecoder();

	@Test
	public void toXMLEvents() {

		Flux<XMLEvent> events = decoder.decode(Flux.just(stringBuffer(XML)), null, null);

		TestSubscriber<XMLEvent> testSubscriber = new TestSubscriber<>();
		testSubscriber.bindTo(events).
				assertNoError().
				assertComplete().
				assertValuesWith(e -> assertTrue(e.isStartDocument()),
						e -> assertStartElement(e, "pojo"),
						e -> assertStartElement(e, "foo"),
						e -> assertCharacters(e, "foofoo"),
						e -> assertEndElement(e, "foo"),
						e -> assertStartElement(e, "bar"),
						e -> assertCharacters(e, "barbar"),
						e -> assertEndElement(e, "bar"),
						e -> assertEndElement(e, "pojo"));
	}

	private static void assertStartElement(XMLEvent event, String expectedLocalName) {
		assertTrue(event.isStartElement());
		assertEquals(expectedLocalName, event.asStartElement().getName().getLocalPart());
	}

	private static void assertEndElement(XMLEvent event, String expectedLocalName) {
		assertTrue(event + " is no end element", event.isEndElement());
		assertEquals(expectedLocalName, event.asEndElement().getName().getLocalPart());
	}

	private static void assertCharacters(XMLEvent event, String expectedData) {
		assertTrue(event.isCharacters());
		assertEquals(expectedData, event.asCharacters().getData());
	}


}