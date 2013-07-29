/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.oxm.jaxb.xmlregistry;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBElement;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.junit.Assert.*;
/**
 * Unit tests for SPR-10714 - Currently @XmlRegistry annotated classes are not
 * included in the scans of JAB2 annotated classes, when a package name is specified 
 * as a property of the JAXB2 Marshaller/Unmarshaller. 
 *
 * @author Biju Kunjummen
 * 
 */
public class XmlRegistryJaxb2MarshallingTests {

	private static final String INPUT_STRING="<brand-airplane><name>test</name></brand-airplane>";
	private Jaxb2Marshaller marshaller;

	@Before
	public final void setUp() throws Exception {
		marshaller = createMarshaller();
	}
	
	public Jaxb2Marshaller createMarshaller() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setPackagesToScan(new String[]{"org.springframework.oxm.jaxb.xmlregistry"});
		marshaller.afterPropertiesSet();
		return marshaller;
	}	

	@Test
	public void testUnmarshalAnXmlReferingToAWrappedXmlElementDecl() {
		Source source = new StreamSource(new StringReader(INPUT_STRING));
		@SuppressWarnings("unchecked")
		JAXBElement<Airplane> airplane = (JAXBElement<Airplane>)marshaller.unmarshal(source);
		assertEquals("Unmarshalling via explicit @XmlRegistry tag should return correct type", "test", airplane.getValue().getName());
	}

	@Test
	public void testMarshalAWrappedObjectHoldingAnXmlElementDeclElement() {
		Airplane airplane = new Airplane();
		airplane.setName("test");
		StringWriter writer = new StringWriter();
		Result result = new StreamResult(writer);
		marshaller.marshal(airplane, result );
		assertTrue("Marshalling should use root Element", writer.toString().contains("<airplane><name>test</name></airplane>"));
	}
}
