/*
 * Copyright 2002-2009 the original author or authors.
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

import java.nio.charset.Charset;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import static org.custommonkey.xmlunit.XMLAssert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;

/** @author Arjen Poutsma */
public class Jaxb2RootElementHttpMessageConverterTest {

	private Jaxb2RootElementHttpMessageConverter converter;

	private RootElement rootElement;

	private RootElement rootElementCglib;

	@Before
	public void setUp() {
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
	public void canRead() throws Exception {
		assertTrue("Converter does not support reading @XmlRootElement", converter.canRead(RootElement.class, null));
		assertTrue("Converter does not support reading @XmlType", converter.canRead(Type.class, null));
	}

	@Test
	public void canWrite() throws Exception {
		assertTrue("Converter does not support writing @XmlRootElement", converter.canWrite(RootElement.class, null));
		assertTrue("Converter does not support writing @XmlRootElement subclass", converter.canWrite(RootElementSubclass.class, null));
		assertTrue("Converter does not support writing @XmlRootElement subclass", converter.canWrite(rootElementCglib.getClass(), null));
		assertFalse("Converter supports writing @XmlType", converter.canWrite(Type.class, null));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlRootElement() throws Exception {
		byte[] body = "<rootElement><type s=\"Hello World\"/></rootElement>".getBytes("UTF-8");
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		RootElement result = (RootElement) converter.read((Class) RootElement.class, inputMessage);
		assertEquals("Invalid result", "Hello World", result.type.s);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlRootElementSubclass() throws Exception {
		byte[] body = "<rootElement><type s=\"Hello World\"/></rootElement>".getBytes("UTF-8");
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		RootElementSubclass result = (RootElementSubclass) converter.read((Class) RootElementSubclass.class, inputMessage);
		assertEquals("Invalid result", "Hello World", result.type.s);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readXmlType() throws Exception {
		byte[] body = "<foo s=\"Hello World\"/>".getBytes("UTF-8");
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		Type result = (Type) converter.read((Class) Type.class, inputMessage);
		assertEquals("Invalid result", "Hello World", result.s);
	}

	@Test
	public void writeXmlRootElement() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(rootElement, null, outputMessage);
		assertEquals("Invalid content-type", new MediaType("application", "xml"),
				outputMessage.getHeaders().getContentType());
		assertXMLEqual("Invalid result", "<rootElement><type s=\"Hello World\"/></rootElement>",
				outputMessage.getBodyAsString(Charset.forName("UTF-8")));
	}

	@Test
	public void writeXmlRootElementSubclass() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(rootElementCglib, null, outputMessage);
		assertEquals("Invalid content-type", new MediaType("application", "xml"),
				outputMessage.getHeaders().getContentType());
		assertXMLEqual("Invalid result", "<rootElement><type s=\"Hello World\"/></rootElement>",
				outputMessage.getBodyAsString(Charset.forName("UTF-8")));
	}

	@XmlRootElement
	public static class RootElement {

		@XmlElement
		public Type type = new Type();

	}

	@XmlType
	public static class Type {

		@XmlAttribute
		public String s = "Hello World";

	}

	public static class RootElementSubclass extends RootElement {

	}

}
