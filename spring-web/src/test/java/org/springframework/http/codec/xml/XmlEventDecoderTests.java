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
import javax.xml.stream.events.XMLEvent;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.AbstractLeakCheckingTestCase;
import org.springframework.core.io.buffer.DataBuffer;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class XmlEventDecoderTests extends AbstractLeakCheckingTestCase {

	private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<pojo>" +
			"<foo>foofoo</foo>" +
			"<bar>barbar</bar>" +
			"</pojo>";

	private XmlEventDecoder decoder = new XmlEventDecoder();

	@Test
	public void toXMLEventsAalto() {

		Flux<XMLEvent> events =
				this.decoder.decode(stringBuffer(XML), null, null, Collections.emptyMap());

		StepVerifier.create(events)
				.consumeNextWith(e -> assertTrue(e.isStartDocument()))
				.consumeNextWith(e -> assertStartElement(e, "pojo"))
				.consumeNextWith(e -> assertStartElement(e, "foo"))
				.consumeNextWith(e -> assertCharacters(e, "foofoo"))
				.consumeNextWith(e -> assertEndElement(e, "foo"))
				.consumeNextWith(e -> assertStartElement(e, "bar"))
				.consumeNextWith(e -> assertCharacters(e, "barbar"))
				.consumeNextWith(e -> assertEndElement(e, "bar"))
				.consumeNextWith(e -> assertEndElement(e, "pojo"))
				.expectComplete()
				.verify();
	}

	@Test
	public void toXMLEventsNonAalto() {
		decoder.useAalto = false;

		Flux<XMLEvent> events =
				this.decoder.decode(stringBuffer(XML), null, null, Collections.emptyMap());

		StepVerifier.create(events)
				.consumeNextWith(e -> assertTrue(e.isStartDocument()))
				.consumeNextWith(e -> assertStartElement(e, "pojo"))
				.consumeNextWith(e -> assertStartElement(e, "foo"))
				.consumeNextWith(e -> assertCharacters(e, "foofoo"))
				.consumeNextWith(e -> assertEndElement(e, "foo"))
				.consumeNextWith(e -> assertStartElement(e, "bar"))
				.consumeNextWith(e -> assertCharacters(e, "barbar"))
				.consumeNextWith(e -> assertEndElement(e, "bar"))
				.consumeNextWith(e -> assertEndElement(e, "pojo"))
				.consumeNextWith(e -> assertTrue(e.isEndDocument()))
				.expectComplete()
				.verify();
	}

	@Test
	public void decodeErrorAalto() {
		Flux<DataBuffer> source = Flux.concat(
				stringBuffer("<pojo>"),
				Flux.error(new RuntimeException()));

		Flux<XMLEvent> events =
				this.decoder.decode(source, null, null, Collections.emptyMap());

		StepVerifier.create(events)
				.consumeNextWith(e -> assertTrue(e.isStartDocument()))
				.consumeNextWith(e -> assertStartElement(e, "pojo"))
				.expectError(RuntimeException.class)
				.verify();
	}

	@Test
	public void decodeErrorNonAalto() {
		decoder.useAalto = false;

		Flux<DataBuffer> source = Flux.concat(
				stringBuffer("<pojo>"),
				Flux.error(new RuntimeException()));

		Flux<XMLEvent> events =
				this.decoder.decode(source, null, null, Collections.emptyMap());

		StepVerifier.create(events)
				.expectError(RuntimeException.class)
				.verify();
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

	private Mono<DataBuffer> stringBuffer(String value) {
		return Mono.defer(() -> {
			byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
			DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
			buffer.write(bytes);
			return Mono.just(buffer);
		});
	}

}
