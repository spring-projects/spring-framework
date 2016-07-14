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

import org.junit.Test;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

/**
 * @author Arjen Poutsma
 */
public class ListBasedXMLEventReaderTests {

	private final XMLInputFactory inputFactory = XMLInputFactory.newFactory();

	private final XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();

	@Test
	public void standard() throws Exception {
		String xml = "<foo><bar>baz</bar></foo>";
		List<XMLEvent> events = readEvents(xml);

		ListBasedXMLEventReader reader = new ListBasedXMLEventReader(events);

		StringWriter resultWriter = new StringWriter();
		XMLEventWriter writer = this.outputFactory.createXMLEventWriter(resultWriter);
		writer.add(reader);

		assertXMLEqual(xml, resultWriter.toString());
	}

	private List<XMLEvent> readEvents(String xml) throws XMLStreamException {
		XMLEventReader reader =
				this.inputFactory.createXMLEventReader(new StringReader(xml));
		List<XMLEvent> events = new ArrayList<>();
		while (reader.hasNext()) {
			events.add(reader.nextEvent());
		}
		return events;
	}

}