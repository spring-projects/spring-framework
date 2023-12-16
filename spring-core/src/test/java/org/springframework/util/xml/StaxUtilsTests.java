/*
 * Copyright 2002-2023 the original author or authors.
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
