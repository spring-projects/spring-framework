/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.StringWriter;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;

import org.junit.Before;
import org.junit.Test;

import static org.custommonkey.xmlunit.XMLAssert.*;

public class XMLEventStreamWriterTests {

	private static final String XML =
			"<?pi content?><root xmlns='namespace'><prefix:child xmlns:prefix='namespace2'><!--comment-->content</prefix:child></root>";

	private XMLEventStreamWriter streamWriter;

	private StringWriter stringWriter;

	@Before
	public void createStreamReader() throws Exception {
		stringWriter = new StringWriter();
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(stringWriter);
		streamWriter = new XMLEventStreamWriter(eventWriter, XMLEventFactory.newInstance());
	}

	@Test
	public void write() throws Exception {
		streamWriter.writeStartDocument();
		streamWriter.writeProcessingInstruction("pi", "content");
		streamWriter.writeStartElement("namespace", "root");
		streamWriter.writeDefaultNamespace("namespace");
		streamWriter.writeStartElement("prefix", "child", "namespace2");
		streamWriter.writeNamespace("prefix", "namespace2");
		streamWriter.writeComment("comment");
		streamWriter.writeCharacters("content");
		streamWriter.writeEndElement();
		streamWriter.writeEndElement();
		streamWriter.writeEndDocument();

		assertXMLEqual(XML, stringWriter.toString());
	}


}
