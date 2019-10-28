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

package org.springframework.http.converter.xml;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.tests.XmlContent;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class SourceHttpMessageConverterTests {

	private static final String BODY = "<root>Hello World</root>";

	private SourceHttpMessageConverter<Source> converter;

	private String bodyExternal;


	@BeforeEach
	public void setup() throws IOException {
		converter = new SourceHttpMessageConverter<>();
		Resource external = new ClassPathResource("external.txt", getClass());

		bodyExternal = "<!DOCTYPE root SYSTEM \"https://192.168.28.42/1.jsp\" [" +
				"  <!ELEMENT root ANY >\n" +
				"  <!ENTITY ext SYSTEM \"" + external.getURI() + "\" >]><root>&ext;</root>";
	}


	@Test
	public void canRead() {
		assertThat(converter.canRead(Source.class, new MediaType("application", "xml"))).isTrue();
		assertThat(converter.canRead(Source.class, new MediaType("application", "soap+xml"))).isTrue();
	}

	@Test
	public void canWrite() {
		assertThat(converter.canWrite(Source.class, new MediaType("application", "xml"))).isTrue();
		assertThat(converter.canWrite(Source.class, new MediaType("application", "soap+xml"))).isTrue();
		assertThat(converter.canWrite(Source.class, MediaType.ALL)).isTrue();
	}

	@Test
	public void readDOMSource() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(BODY.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		DOMSource result = (DOMSource) converter.read(DOMSource.class, inputMessage);
		Document document = (Document) result.getNode();
		assertThat(document.getDocumentElement().getLocalName()).as("Invalid result").isEqualTo("root");
	}

	@Test
	public void readDOMSourceExternal() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(bodyExternal.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		converter.setSupportDtd(true);
		DOMSource result = (DOMSource) converter.read(DOMSource.class, inputMessage);
		Document document = (Document) result.getNode();
		assertThat(document.getDocumentElement().getLocalName()).as("Invalid result").isEqualTo("root");
		assertThat(document.getDocumentElement().getTextContent()).as("Invalid result").isNotEqualTo("Foo Bar");
	}

	@Test
	public void readDomSourceWithXmlBomb() throws Exception {
		// https://en.wikipedia.org/wiki/Billion_laughs
		// https://msdn.microsoft.com/en-us/magazine/ee335713.aspx
		String content = "<?xml version=\"1.0\"?>\n" +
				"<!DOCTYPE lolz [\n" +
				" <!ENTITY lol \"lol\">\n" +
				" <!ELEMENT lolz (#PCDATA)>\n" +
				" <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n" +
				" <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n" +
				" <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n" +
				" <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n" +
				" <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\n" +
				" <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\n" +
				" <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">\n" +
				" <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">\n" +
				" <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">\n" +
				"]>\n" +
				"<root>&lol9;</root>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes("UTF-8"));

		assertThatExceptionOfType(HttpMessageNotReadableException.class).isThrownBy(() ->
				this.converter.read(DOMSource.class, inputMessage))
			.withMessageContaining("DOCTYPE");
	}

	@Test
	public void readSAXSource() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(BODY.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		SAXSource result = (SAXSource) converter.read(SAXSource.class, inputMessage);
		InputSource inputSource = result.getInputSource();
		String s = FileCopyUtils.copyToString(new InputStreamReader(inputSource.getByteStream()));
		assertThat(XmlContent.from(s)).isSimilarTo(BODY);
	}

	@Test
	public void readSAXSourceExternal() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(bodyExternal.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		converter.setSupportDtd(true);
		SAXSource result = (SAXSource) converter.read(SAXSource.class, inputMessage);
		InputSource inputSource = result.getInputSource();
		XMLReader reader = result.getXMLReader();
		reader.setContentHandler(new DefaultHandler() {
			@Override
			public void characters(char[] ch, int start, int length) {
				String s = new String(ch, start, length);
				assertThat(s).as("Invalid result").isNotEqualTo("Foo Bar");
			}
		});
		reader.parse(inputSource);
	}

	@Test
	public void readSAXSourceWithXmlBomb() throws Exception {
		// https://en.wikipedia.org/wiki/Billion_laughs
		// https://msdn.microsoft.com/en-us/magazine/ee335713.aspx
		String content = "<?xml version=\"1.0\"?>\n" +
				"<!DOCTYPE lolz [\n" +
				" <!ENTITY lol \"lol\">\n" +
				" <!ELEMENT lolz (#PCDATA)>\n" +
				" <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n" +
				" <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n" +
				" <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n" +
				" <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n" +
				" <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\n" +
				" <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\n" +
				" <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">\n" +
				" <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">\n" +
				" <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">\n" +
				"]>\n" +
				"<root>&lol9;</root>";

		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes("UTF-8"));
		SAXSource result = (SAXSource) this.converter.read(SAXSource.class, inputMessage);

		InputSource inputSource = result.getInputSource();
		XMLReader reader = result.getXMLReader();
		assertThatExceptionOfType(SAXException.class).isThrownBy(() ->
				reader.parse(inputSource))
			.withMessageContaining("DOCTYPE");
	}

	@Test
	public void readStAXSource() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(BODY.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		StAXSource result = (StAXSource) converter.read(StAXSource.class, inputMessage);
		XMLStreamReader streamReader = result.getXMLStreamReader();
		assertThat(streamReader.hasNext()).isTrue();
		streamReader.nextTag();
		String s = streamReader.getLocalName();
		assertThat(s).isEqualTo("root");
		s = streamReader.getElementText();
		assertThat(s).isEqualTo("Hello World");
		streamReader.close();
	}

	@Test
	public void readStAXSourceExternal() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(bodyExternal.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		converter.setSupportDtd(true);
		StAXSource result = (StAXSource) converter.read(StAXSource.class, inputMessage);
		XMLStreamReader streamReader = result.getXMLStreamReader();
		assertThat(streamReader.hasNext()).isTrue();
		streamReader.next();
		streamReader.next();
		String s = streamReader.getLocalName();
		assertThat(s).isEqualTo("root");
		try {
			s = streamReader.getElementText();
			assertThat(s).isNotEqualTo("Foo Bar");
		}
		catch (XMLStreamException ex) {
			// Some parsers raise a parse exception
		}
		streamReader.close();
	}

	@Test
	public void readStAXSourceWithXmlBomb() throws Exception {
		// https://en.wikipedia.org/wiki/Billion_laughs
		// https://msdn.microsoft.com/en-us/magazine/ee335713.aspx
		String content = "<?xml version=\"1.0\"?>\n" +
				"<!DOCTYPE lolz [\n" +
				" <!ENTITY lol \"lol\">\n" +
				" <!ELEMENT lolz (#PCDATA)>\n" +
				" <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n" +
				" <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n" +
				" <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n" +
				" <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n" +
				" <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\n" +
				" <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\n" +
				" <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">\n" +
				" <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">\n" +
				" <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">\n" +
				"]>\n" +
				"<root>&lol9;</root>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes("UTF-8"));
		StAXSource result = (StAXSource) this.converter.read(StAXSource.class, inputMessage);

		XMLStreamReader streamReader = result.getXMLStreamReader();
		assertThat(streamReader.hasNext()).isTrue();
		streamReader.next();
		streamReader.next();
		String s = streamReader.getLocalName();
		assertThat(s).isEqualTo("root");
		assertThatExceptionOfType(XMLStreamException.class).isThrownBy(() ->
				streamReader.getElementText())
			.withMessageContaining("\"lol9\"");
	}

	@Test
	public void readStreamSource() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(BODY.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		StreamSource result = (StreamSource) converter.read(StreamSource.class, inputMessage);
		String s = FileCopyUtils.copyToString(new InputStreamReader(result.getInputStream()));
		assertThat(XmlContent.of(s)).isSimilarTo(BODY);
	}

	@Test
	public void readSource() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(BODY.getBytes("UTF-8"));
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
		assertThat(XmlContent.of(outputMessage.getBodyAsString(StandardCharsets.UTF_8)))
				.isSimilarTo("<root>Hello World</root>");
		assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(new MediaType("application", "xml"));
		assertThat(outputMessage.getHeaders().getContentLength()).as("Invalid content-length").isEqualTo(outputMessage.getBodyAsBytes().length);
	}

	@Test
	public void writeSAXSource() throws Exception {
		String xml = "<root>Hello World</root>";
		SAXSource saxSource = new SAXSource(new InputSource(new StringReader(xml)));

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(saxSource, null, outputMessage);
		assertThat(XmlContent.of(outputMessage.getBodyAsString(StandardCharsets.UTF_8)))
				.isSimilarTo("<root>Hello World</root>");
		assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(new MediaType("application", "xml"));
	}

	@Test
	public void writeStreamSource() throws Exception {
		String xml = "<root>Hello World</root>";
		StreamSource streamSource = new StreamSource(new StringReader(xml));

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(streamSource, null, outputMessage);
		assertThat(XmlContent.of(outputMessage.getBodyAsString(StandardCharsets.UTF_8)))
				.isSimilarTo("<root>Hello World</root>");
		assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(new MediaType("application", "xml"));
	}

}
