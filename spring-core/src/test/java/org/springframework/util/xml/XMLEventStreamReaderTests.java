/*
 * Copyright 2002-2011 the original author or authors.
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
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.junit.Before;
import org.junit.Test;

import static org.custommonkey.xmlunit.XMLAssert.*;

public class XMLEventStreamReaderTests {

	private static final String XML =
			"<?pi content?><root xmlns='namespace'><prefix:child xmlns:prefix='namespace2'>content</prefix:child></root>"
			;

	private XMLEventStreamReader streamReader;

	@Before
	public void createStreamReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLEventReader eventReader = inputFactory.createXMLEventReader(new StringReader(XML));
		streamReader = new XMLEventStreamReader(eventReader);
	}

	@Test
	public void readAll() throws Exception {
		while (streamReader.hasNext()) {
			streamReader.next();
		}
	}

	@Test
	public void readCorrect() throws Exception {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StaxSource source = new StaxSource(streamReader);
		StringWriter writer = new StringWriter();
		transformer.transform(source, new StreamResult(writer));
		assertXMLEqual(XML, writer.toString());
	}

}