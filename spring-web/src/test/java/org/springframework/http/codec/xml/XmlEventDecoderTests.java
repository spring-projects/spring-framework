/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import org.springframework.core.testfixture.io.buffer.AbstractLeakCheckingTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class XmlEventDecoderTests extends AbstractLeakCheckingTests {

	private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<pojo>" +
			"<foo>foofoo</foo>" +
			"<bar>barbar</bar>" +
			"</pojo>";

	private final XmlEventDecoder decoder = new XmlEventDecoder();


	@Test
	void toXMLEventsAalto() {

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
	void toXMLEventsNonAalto() {
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
	void decodeErrorAalto() {
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
	void decodeErrorNonAalto() {
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
