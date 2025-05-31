/*
 * Copyright 2002-2025 the original author or authors.
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

import java.nio.charset.StandardCharsets;

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.diff.DifferenceEvaluator;

import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.testfixture.xml.XmlContent;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.testfixture.http.MockHttpInputMessage;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.xmlunit.diff.ComparisonType.XML_STANDALONE;
import static org.xmlunit.diff.DifferenceEvaluators.Default;
import static org.xmlunit.diff.DifferenceEvaluators.chain;
import static org.xmlunit.diff.DifferenceEvaluators.downgradeDifferencesToEqual;

/**
 * Tests for {@link Jaxb2RootElementHttpMessageConverter}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
class Jaxb2RootElementHttpMessageConverterTests {

	private Jaxb2RootElementHttpMessageConverter converter;

	private RootElement rootElement;

	private RootElement rootElementCglib;


	@BeforeEach
	void setup() {
		converter = new Jaxb2RootElementHttpMessageConverter();
		rootElement = new RootElement();
		DefaultAopProxyFactory proxyFactory = new DefaultAopProxyFactory();
		AdvisedSupport advisedSupport = new AdvisedSupport();
		advisedSupport.setTarget(rootElement);
		advisedSupport.setProxyTargetClass(true);
		AopProxy proxy = proxyFactory.createAopProxy(advisedSupport);
		rootElementCglib = (RootElement) proxy.getProxy();
	}


	@Test
	void canRead() {
		assertThat(converter.canRead(RootElement.class, null))
				.as("Converter does not support reading @XmlRootElement").isTrue();
		assertThat(converter.canRead(Type.class, null))
				.as("Converter does not support reading @XmlType").isTrue();
	}

	@Test
	void canWrite() {
		assertThat(converter.canWrite(RootElement.class, null))
				.as("Converter does not support writing @XmlRootElement").isTrue();
		assertThat(converter.canWrite(RootElementSubclass.class, null))
				.as("Converter does not support writing @XmlRootElement subclass").isTrue();
		assertThat(converter.canWrite(rootElementCglib.getClass(), null))
				.as("Converter does not support writing @XmlRootElement subclass").isTrue();
		assertThat(converter.canWrite(JAXBElement.class, null))
				.as("Converter does not support writing JAXBElement").isTrue();
		assertThat(converter.canWrite(Type.class, null))
				.as("Converter supports writing @XmlType").isFalse();
	}

	@Test
	void readXmlRootElement() throws Exception {
		byte[] body = "<rootElement><type s=\"Hello World\"/></rootElement>".getBytes(StandardCharsets.UTF_8);
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		RootElement result = (RootElement) converter.read(RootElement.class, inputMessage);
		assertThat(result.type.s).as("Invalid result").isEqualTo("Hello World");
	}

	@Test
	void readXmlRootElementSubclass() throws Exception {
		byte[] body = "<rootElement><type s=\"Hello World\"/></rootElement>".getBytes(StandardCharsets.UTF_8);
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		RootElementSubclass result = (RootElementSubclass) converter.read(RootElementSubclass.class, inputMessage);
		assertThat(result.getType().s).as("Invalid result").isEqualTo("Hello World");
	}

	@Test
	void readXmlType() throws Exception {
		byte[] body = "<foo s=\"Hello World\"/>".getBytes(StandardCharsets.UTF_8);
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		Type result = (Type) converter.read(Type.class, inputMessage);
		assertThat(result.s).as("Invalid result").isEqualTo("Hello World");
	}

	@Test
	void readXmlRootElementExternalEntityDisabled() throws Exception {
		Resource external = new ClassPathResource("external.txt", getClass());
		String content = "<!DOCTYPE root SYSTEM \"https://192.168.28.42/1.jsp\" [" +
				"  <!ELEMENT external ANY >\n" +
				"  <!ENTITY ext SYSTEM \"" + external.getURI() + "\" >]>" +
				"  <rootElement><external>&ext;</external></rootElement>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
		converter.setSupportDtd(true);
		RootElement rootElement = (RootElement) converter.read(RootElement.class, inputMessage);

		assertThat(rootElement.external).isEmpty();
	}

	@Test
	void readXmlRootElementExternalEntityEnabled() throws Exception {
		Resource external = new ClassPathResource("external.txt", getClass());
		String content = "<!DOCTYPE root [" +
				"  <!ELEMENT external ANY >\n" +
				"  <!ENTITY ext SYSTEM \"" + external.getURI() + "\" >]>" +
				"  <rootElement><external>&ext;</external></rootElement>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
		this.converter.setProcessExternalEntities(true);
		RootElement rootElement = (RootElement) converter.read(RootElement.class, inputMessage);

		assertThat(rootElement.external).isEqualTo("Foo Bar");
	}

	@Test
	void testXmlBomb() throws Exception {
		// https://en.wikipedia.org/wiki/Billion_laughs
		// https://msdn.microsoft.com/en-us/magazine/ee335713.aspx
		String content = """
				<?xml version="1.0"?>
				<!DOCTYPE lolz [
					<!ENTITY lol "lol">
					<!ELEMENT lolz (#PCDATA)>
					<!ENTITY lol1 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
					<!ENTITY lol2 "&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;">
					<!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
					<!ENTITY lol4 "&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;">
					<!ENTITY lol5 "&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;">
					<!ENTITY lol6 "&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;">
					<!ENTITY lol7 "&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;">
					<!ENTITY lol8 "&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;">
					<!ENTITY lol9 "&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;">
				]>
				<rootElement><external>&lol9;</external></rootElement>""";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(content.getBytes(StandardCharsets.UTF_8));
		assertThatExceptionOfType(HttpMessageNotReadableException.class)
				.isThrownBy(() -> this.converter.read(RootElement.class, inputMessage))
				.withMessageContaining("DOCTYPE");
	}

	@Test
	void readXmlRootElementHeaderCharset() throws Exception {
		byte[] body = "<rootElement><type s=\"Hellø Wørld\"/></rootElement>".getBytes(StandardCharsets.ISO_8859_1);
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		inputMessage.getHeaders().setContentType(MediaType.parseMediaType("application/xml;charset=iso-8859-1"));
		RootElement result = (RootElement) converter.read(RootElement.class, inputMessage);
		assertThat(result.type.s).as("Invalid result").isEqualTo("Hellø Wørld");
	}

	@Test
	void writeXmlRootElement() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(rootElement, null, outputMessage);
		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type").isEqualTo(MediaType.APPLICATION_XML);
		DifferenceEvaluator ev = chain(Default, downgradeDifferencesToEqual(XML_STANDALONE));
		assertThat(XmlContent.of(outputMessage.getBodyAsString(StandardCharsets.UTF_8)))
				.isSimilarTo("<rootElement><type s=\"Hello World\"/></rootElement>", ev);
	}

	@Test
	void writeJaxbElementRootElement() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		JAXBElement<?> jaxbElement = new JAXBElement<>(new QName("custom"), MyCustomElement.class, new MyCustomElement("field1", "field2"));
		converter.write(jaxbElement, null, outputMessage);
		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type").isEqualTo(MediaType.APPLICATION_XML);
		DifferenceEvaluator ev = chain(Default, downgradeDifferencesToEqual(XML_STANDALONE));
		assertThat(XmlContent.of(outputMessage.getBodyAsString(StandardCharsets.UTF_8)))
				.isSimilarTo("<custom><field1>field1</field1><field2>field2</field2></custom>", ev);
	}

	@Test
	void writeXmlRootElementSubclass() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(rootElementCglib, null, outputMessage);
		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type").isEqualTo(MediaType.APPLICATION_XML);
		DifferenceEvaluator ev = chain(Default, downgradeDifferencesToEqual(XML_STANDALONE));
		assertThat(XmlContent.of(outputMessage.getBodyAsString(StandardCharsets.UTF_8)))
				.isSimilarTo("<rootElement><type s=\"Hello World\"/></rootElement>", ev);
	}

	// SPR-11488

	@Test
	void customizeMarshaller() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MyJaxb2RootElementHttpMessageConverter myConverter = new MyJaxb2RootElementHttpMessageConverter();
		myConverter.write(new MyRootElement(new MyCustomElement("a", "b")), null, outputMessage);
		DifferenceEvaluator ev = chain(Default, downgradeDifferencesToEqual(XML_STANDALONE));
		assertThat(XmlContent.of(outputMessage.getBodyAsString(StandardCharsets.UTF_8)))
				.isSimilarTo("<myRootElement><element>a|||b</element></myRootElement>", ev);
	}

	@Test
	void customizeUnmarshaller() throws Exception {
		byte[] body = "<myRootElement><element>a|||b</element></myRootElement>".getBytes(StandardCharsets.UTF_8);
		MyJaxb2RootElementHttpMessageConverter myConverter = new MyJaxb2RootElementHttpMessageConverter();
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		MyRootElement result = (MyRootElement) myConverter.read(MyRootElement.class, inputMessage);
		assertThat(result.getElement().getField1()).isEqualTo("a");
		assertThat(result.getElement().getField2()).isEqualTo("b");
	}


	@XmlRootElement
	public static class RootElement {

		private Type type = new Type();

		@XmlElement(required=false)
		public String external;

		public Type getType() {
			return this.type;
		}

		@XmlElement
		public void setType(Type type) {
			this.type = type;
		}
	}


	@XmlType
	public static class Type {

		@XmlAttribute
		public String s = "Hello World";

	}


	public static class RootElementSubclass extends RootElement {
	}


	public static class MyJaxb2RootElementHttpMessageConverter extends Jaxb2RootElementHttpMessageConverter {

		@Override
		protected void customizeMarshaller(Marshaller marshaller) {
			marshaller.setAdapter(new MyCustomElementAdapter());
		}

		@Override
		protected void customizeUnmarshaller(Unmarshaller unmarshaller) {
			unmarshaller.setAdapter(new MyCustomElementAdapter());
		}
	}


	public static class MyCustomElement {

		private String field1;

		private String field2;

		public MyCustomElement() {
		}

		public MyCustomElement(String field1, String field2) {
			this.field1 = field1;
			this.field2 = field2;
		}

		public String getField1() {
			return field1;
		}

		public void setField1(String field1) {
			this.field1 = field1;
		}

		public String getField2() {
			return field2;
		}

		public void setField2(String field2) {
			this.field2 = field2;
		}
	}


	@XmlRootElement
	public static class MyRootElement {

		private MyCustomElement element;

		public MyRootElement() {

		}

		public MyRootElement(MyCustomElement element) {
			this.element = element;
		}

		@XmlJavaTypeAdapter(MyCustomElementAdapter.class)
		public MyCustomElement getElement() {
			return element;
		}

		public void setElement(MyCustomElement element) {
			this.element = element;
		}
	}


	public static class MyCustomElementAdapter extends XmlAdapter<String, MyCustomElement> {

		@Override
		public String marshal(MyCustomElement c) throws Exception {
			return c.getField1() + "|||" + c.getField2();
		}

		@Override
		public MyCustomElement unmarshal(String c) throws Exception {
			String[] t = c.split("\\|\\|\\|");
			return new MyCustomElement(t[0], t[1]);
		}

	}

}
