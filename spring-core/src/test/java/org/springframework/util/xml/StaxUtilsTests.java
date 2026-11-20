/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.xml;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class StaxUtilsTests {

	@Test
	void isStaxSourceInvalid() {
		assertThat(StaxUtils.isStaxSource(new DOMSource())).as("A StAX Source").isFalse();
		assertThat(StaxUtils.isStaxSource(new SAXSource())).as("A StAX Source").isFalse();
		assertThat(StaxUtils.isStaxSource(new StreamSource())).as("A StAX Source").isFalse();
	}

	@Test
	void isStaxSource() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		String expected = "<element/>";
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(expected));
		Source source = StaxUtils.createCustomStaxSource(streamReader);

		assertThat(StaxUtils.isStaxSource(source)).as("Not a StAX Source").isTrue();
	}

	@Test
	void isStaxSourceJaxp14() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		String expected = "<element/>";
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(expected));
		StAXSource source = new StAXSource(streamReader);

		assertThat(StaxUtils.isStaxSource(source)).as("Not a StAX Source").isTrue();
	}

	@Test
	void isStaxResultInvalid() {
		assertThat(StaxUtils.isStaxResult(new DOMResult())).as("A StAX Result").isFalse();
		assertThat(StaxUtils.isStaxResult(new SAXResult())).as("A StAX Result").isFalse();
		assertThat(StaxUtils.isStaxResult(new StreamResult())).as("A StAX Result").isFalse();
	}

	@Test
	void isStaxResult() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(new StringWriter());
		Result result = StaxUtils.createCustomStaxResult(streamWriter);

		assertThat(StaxUtils.isStaxResult(result)).as("Not a StAX Result").isTrue();
	}

	@Test
	void isStaxResultJaxp14() throws Exception {
		XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
		XMLStreamWriter streamWriter = outputFactory.createXMLStreamWriter(new StringWriter());
		StAXResult result = new StAXResult(streamWriter);

		assertThat(StaxUtils.isStaxResult(result)).as("Not a StAX Result").isTrue();
	}

}
