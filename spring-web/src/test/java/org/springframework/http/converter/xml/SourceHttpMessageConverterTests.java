/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.http.converter.xml;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.util.FileCopyUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * @author Arjen Poutsma
 */
public class SourceHttpMessageConverterTests {

	private SourceHttpMessageConverter<Source> converter;

	@Before
	public void setUp() {
		converter = new SourceHttpMessageConverter<Source>();
	}

	@Test
	public void canRead() {
		assertTrue(converter.canRead(Source.class, new MediaType("application", "xml")));
		assertTrue(converter.canRead(Source.class, new MediaType("application", "soap+xml")));
	}

	@Test
	public void canWrite() {
		assertTrue(converter.canWrite(Source.class, new MediaType("application", "xml")));
		assertTrue(converter.canWrite(Source.class, new MediaType("application", "soap+xml")));
		assertTrue(converter.canWrite(Source.class, MediaType.ALL));
	}

	@Test
	public void readDOMSource() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		DOMSource result = (DOMSource) converter.read(DOMSource.class, inputMessage);
		Document document = (Document) result.getNode();
		assertEquals("Invalid result", "root", document.getDocumentElement().getLocalName());
	}

	@Test
	public void readSAXSource() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		SAXSource result = (SAXSource) converter.read(SAXSource.class, inputMessage);
		InputSource inputSource = result.getInputSource();
		String s = FileCopyUtils.copyToString(new InputStreamReader(inputSource.getByteStream()));
		assertXMLEqual("Invalid result", body, s);
	}

	@Test
	public void readStreamSource() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		StreamSource result = (StreamSource) converter.read(StreamSource.class, inputMessage);
		String s = FileCopyUtils.copyToString(new InputStreamReader(result.getInputStream()));
		assertXMLEqual("Invalid result", body, s);
	}

	@Test
	public void readSource() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		converter.read(Source.class, inputMessage);
	}

	@Test
	public void writeDOMSource() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		Document document = documentBuilderFactory.newDocumentBuilder().newDocument();
		Element rootElement = document.createElement("root");
		document.appendChild(rootElement);
		rootElement.setTextContent("Hello World");
		DOMSource domSource = new DOMSource(document);

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(domSource, null, outputMessage);
		assertXMLEqual("Invalid result", "<root>Hello World</root>",
				outputMessage.getBodyAsString(Charset.forName("UTF-8")));
		assertEquals("Invalid content-type", new MediaType("application", "xml"),
				outputMessage.getHeaders().getContentType());
		assertEquals("Invalid content-length", outputMessage.getBodyAsBytes().length,
				outputMessage.getHeaders().getContentLength());
	}

	@Test
	public void writeSAXSource() throws Exception {
		String xml = "<root>Hello World</root>";
		SAXSource saxSource = new SAXSource(new InputSource(new StringReader(xml)));

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(saxSource, null, outputMessage);
		assertXMLEqual("Invalid result", "<root>Hello World</root>",
				outputMessage.getBodyAsString(Charset.forName("UTF-8")));
		assertEquals("Invalid content-type", new MediaType("application", "xml"),
				outputMessage.getHeaders().getContentType());
	}

	@Test
	public void writeStreamSource() throws Exception {
		String xml = "<root>Hello World</root>";
		StreamSource streamSource = new StreamSource(new StringReader(xml));

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(streamSource, null, outputMessage);
		assertXMLEqual("Invalid result", "<root>Hello World</root>",
				outputMessage.getBodyAsString(Charset.forName("UTF-8")));
		assertEquals("Invalid content-type", new MediaType("application", "xml"),
				outputMessage.getHeaders().getContentType());
	}

}
