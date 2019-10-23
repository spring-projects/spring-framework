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

package org.springframework.util.xml;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.junit.jupiter.api.Test;

import org.springframework.tests.XmlContent;

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Arjen Poutsma
 * @author Andrzej Hołowko
 */
class ListBasedXMLEventReaderTests {

	private final XMLInputFactory inputFactory = XMLInputFactory.newInstance();

	private final XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();


	@Test
	void standard() throws Exception {
		String xml = "<foo><bar>baz</bar></foo>";
		List<XMLEvent> events = readEvents(xml);

		ListBasedXMLEventReader reader = new ListBasedXMLEventReader(events);

		StringWriter resultWriter = new StringWriter();
		XMLEventWriter writer = this.outputFactory.createXMLEventWriter(resultWriter);
		writer.add(reader);

		assertThat(XmlContent.from(resultWriter)).isSimilarTo(xml);
	}

	@Test
	void getElementText() throws Exception {
		String xml = "<foo><bar>baz</bar></foo>";
		List<XMLEvent> events = readEvents(xml);

		ListBasedXMLEventReader reader = new ListBasedXMLEventReader(events);

		assertThat(reader.nextEvent().getEventType()).isEqualTo(START_DOCUMENT);
		assertThat(reader.nextEvent().getEventType()).isEqualTo(START_ELEMENT);
		assertThat(reader.nextEvent().getEventType()).isEqualTo(START_ELEMENT);
		assertThat(reader.getElementText()).isEqualTo("baz");
		assertThat(reader.nextEvent().getEventType()).isEqualTo(END_ELEMENT);
		assertThat(reader.nextEvent().getEventType()).isEqualTo(END_DOCUMENT);
	}

	@Test
	void getElementTextThrowsExceptionAtWrongPosition() throws Exception {
		String xml = "<foo><bar>baz</bar></foo>";
		List<XMLEvent> events = readEvents(xml);

		ListBasedXMLEventReader reader = new ListBasedXMLEventReader(events);

		assertThat(reader.nextEvent().getEventType()).isEqualTo(START_DOCUMENT);

		assertThatExceptionOfType(XMLStreamException.class).isThrownBy(
				reader::getElementText)
			.withMessageStartingWith("Not at START_ELEMENT");
	}

	private List<XMLEvent> readEvents(String xml) throws XMLStreamException {
		XMLEventReader reader = this.inputFactory.createXMLEventReader(new StringReader(xml));
		List<XMLEvent> events = new ArrayList<>();
		while (reader.hasNext()) {
			events.add(reader.nextEvent());
		}
		return events;
	}

}
