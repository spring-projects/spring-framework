/*
 * Copyright 2002-2019 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.xml.stream.events.XMLEvent;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class XmlEventDecoderTests extends AbstractLeakCheckingTests {

	private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<pojo>" +
			"<foo>foofoo</foo>" +
			"<bar>barbar</bar>" +
			"</pojo>";

	private XmlEventDecoder decoder = new XmlEventDecoder();


	@Test
	public void toXMLEventsAalto() {

		Flux<XMLEvent> events =
				this.decoder.decode(stringBufferMono(XML), null, null, Collections.emptyMap());

		StepVerifier.create(events)
				.consumeNextWith(e -> assertThat(e.isStartDocument()).isTrue())
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
				this.decoder.decode(stringBufferMono(XML), null, null, Collections.emptyMap());

		StepVerifier.create(events)
				.consumeNextWith(e -> assertThat(e.isStartDocument()).isTrue())
				.consumeNextWith(e -> assertStartElement(e, "pojo"))
				.consumeNextWith(e -> assertStartElement(e, "foo"))
				.consumeNextWith(e -> assertCharacters(e, "foofoo"))
				.consumeNextWith(e -> assertEndElement(e, "foo"))
				.consumeNextWith(e -> assertStartElement(e, "bar"))
				.consumeNextWith(e -> assertCharacters(e, "barbar"))
				.consumeNextWith(e -> assertEndElement(e, "bar"))
				.consumeNextWith(e -> assertEndElement(e, "pojo"))
				.consumeNextWith(e -> assertThat(e.isEndDocument()).isTrue())
				.expectComplete()
				.verify();
	}

	@Test
	public void toXMLEventsWithLimit() {

		this.decoder.setMaxInMemorySize(6);

		Flux<String> source = Flux.just(
				"<pojo>", "<foo>", "foofoo", "</foo>", "<bar>", "barbarbar", "</bar>", "</pojo>");

		Flux<XMLEvent> events = this.decoder.decode(
				source.map(this::stringBuffer), null, null, Collections.emptyMap());

		StepVerifier.create(events)
				.consumeNextWith(e -> assertThat(e.isStartDocument()).isTrue())
				.consumeNextWith(e -> assertStartElement(e, "pojo"))
				.consumeNextWith(e -> assertStartElement(e, "foo"))
				.consumeNextWith(e -> assertCharacters(e, "foofoo"))
				.consumeNextWith(e -> assertEndElement(e, "foo"))
				.consumeNextWith(e -> assertStartElement(e, "bar"))
				.expectError(DataBufferLimitException.class)
				.verify();
	}

	@Test
	public void decodeErrorAalto() {
		Flux<DataBuffer> source = Flux.concat(
				stringBufferMono("<pojo>"),
				Flux.error(new RuntimeException()));

		Flux<XMLEvent> events =
				this.decoder.decode(source, null, null, Collections.emptyMap());

		StepVerifier.create(events)
				.consumeNextWith(e -> assertThat(e.isStartDocument()).isTrue())
				.consumeNextWith(e -> assertStartElement(e, "pojo"))
				.expectError(RuntimeException.class)
				.verify();
	}

	@Test
	public void decodeErrorNonAalto() {
		decoder.useAalto = false;

		Flux<DataBuffer> source = Flux.concat(
				stringBufferMono("<pojo>"),
				Flux.error(new RuntimeException()));

		Flux<XMLEvent> events =
				this.decoder.decode(source, null, null, Collections.emptyMap());

		StepVerifier.create(events)
				.expectError(RuntimeException.class)
				.verify();
	}

	private static void assertStartElement(XMLEvent event, String expectedLocalName) {
		assertThat(event.isStartElement()).isTrue();
		assertThat(event.asStartElement().getName().getLocalPart()).isEqualTo(expectedLocalName);
	}

	private static void assertEndElement(XMLEvent event, String expectedLocalName) {
		assertThat(event.isEndElement()).as(event + " is no end element").isTrue();
		assertThat(event.asEndElement().getName().getLocalPart()).isEqualTo(expectedLocalName);
	}

	private static void assertCharacters(XMLEvent event, String expectedData) {
		assertThat(event.isCharacters()).isTrue();
		assertThat(event.asCharacters().getData()).isEqualTo(expectedData);
	}

	private DataBuffer stringBuffer(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = this.bufferFactory.allocateBuffer(bytes.length);
		buffer.write(bytes);
		return buffer;
	}

	private Mono<DataBuffer> stringBufferMono(String value) {
		return Mono.defer(() -> Mono.just(stringBuffer(value)));
	}

}
