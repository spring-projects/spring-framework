/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.oxm;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;

import org.custommonkey.xmlunit.XMLUnit;

import org.junit.Before;
import org.junit.Test;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import org.springframework.util.xml.StaxUtils;

import static org.custommonkey.xmlunit.XMLAssert.*;
import static org.junit.Assert.assertTrue;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
public abstract class AbstractMarshallerTests<M extends Marshaller> {

	protected static final String EXPECTED_STRING =
			"<tns:flights xmlns:tns=\"http://samples.springframework.org/flight\">" +
					"<tns:flight><tns:number>42</tns:number></tns:flight></tns:flights>";

	protected M marshaller;

	protected Object flights;

	@Before
	public final void setUp() throws Exception {
		marshaller = createMarshaller();
		flights = createFlights();
		XMLUnit.setIgnoreWhitespace(true);
	}

	protected abstract M createMarshaller() throws Exception;

	protected abstract Object createFlights();

	@Test
	public void marshalDOMResult() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
		Document result = builder.newDocument();
		DOMResult domResult = new DOMResult(result);
		marshaller.marshal(flights, domResult);
		Document expected = builder.newDocument();
		Element flightsElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:flights");
		Attr namespace = expected.createAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:tns");
		namespace.setNodeValue("http://samples.springframework.org/flight");
		flightsElement.setAttributeNode(namespace);
		expected.appendChild(flightsElement);
		Element flightElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:flight");
		flightsElement.appendChild(flightElement);
		Element numberElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:number");
		flightElement.appendChild(numberElement);
		Text text = expected.createTextNode("42");
		numberElement.appendChild(text);
		assertXMLEqual("Marshaller writes invalid DOMResult", expected, result);
	}

	@Test
	public void marshalEmptyDOMResult() throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
		DOMResult domResult = new DOMResult();
		marshaller.marshal(flights, domResult);
		assertTrue("DOMResult does not contain a Document", domResult.getNode() instanceof Document);
		Document result = (Document) domResult.getNode();
		Document expected = builder.newDocument();
		Element flightsElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:flights");
		Attr namespace = expected.createAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:tns");
		namespace.setNodeValue("http://samples.springframework.org/flight");
		flightsElement.setAttributeNode(namespace);
		expected.appendChild(flightsElement);
		Element flightElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:flight");
		flightsElement.appendChild(flightElement);
		Element numberElement = expected.createElementNS("http://samples.springframework.org/flight", "tns:number");
		flightElement.appendChild(numberElement);
		Text text = expected.createTextNode("42");
		numberElement.appendChild(text);
		assertXMLEqual("Marshaller writes invalid DOMResult", expected, result);
	}

	@Test
	public void marshalStreamResultWriter() throws Exception {
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void marshalStreamResultOutputStream() throws Exception {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		StreamResult result = new StreamResult(os);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING,
				new String(os.toByteArray(), "UTF-8"));
	}

	@Test
	public void marshalStaxResultStreamWriter() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
		Result result = StaxUtils.createStaxResult(streamWriter);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void marshalStaxResultEventWriter() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);
		Result result = StaxUtils.createStaxResult(eventWriter);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void marshalJaxp14StaxResultStreamWriter() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(writer);
		StAXResult result = new StAXResult(streamWriter);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void marshalJaxp14StaxResultEventWriter() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		StringWriter writer = new StringWriter();
		XMLEventWriter eventWriter = outputFactory.createXMLEventWriter(writer);
		StAXResult result = new StAXResult(eventWriter);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

}
