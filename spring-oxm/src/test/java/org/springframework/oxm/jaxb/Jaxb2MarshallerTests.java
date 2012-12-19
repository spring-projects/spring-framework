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

package org.springframework.oxm.jaxb;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.AbstractMarshallerTests;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.jaxb.test.FlightType;
import org.springframework.oxm.jaxb.test.Flights;
import org.springframework.oxm.jaxb.test.ObjectFactory;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;

import static org.custommonkey.xmlunit.XMLAssert.assertFalse;
import static org.custommonkey.xmlunit.XMLAssert.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;

public class Jaxb2MarshallerTests extends AbstractMarshallerTests {

	private static final String CONTEXT_PATH = "org.springframework.oxm.jaxb.test";

	private Jaxb2Marshaller marshaller;

	private Flights flights;

	@Override
	public Marshaller createMarshaller() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath(CONTEXT_PATH);
		marshaller.afterPropertiesSet();
		return marshaller;
	}

	@Override
	protected Object createFlights() {
		FlightType flight = new FlightType();
		flight.setNumber(42L);
		flights = new Flights();
		flights.getFlight().add(flight);
		return flights;
	}

	@Test
	public void marshalSAXResult() throws Exception {
		ContentHandler handlerMock = createStrictMock(ContentHandler.class);
		handlerMock.setDocumentLocator(isA(Locator.class));
		handlerMock.startDocument();
		handlerMock.startPrefixMapping("", "http://samples.springframework.org/flight");
		handlerMock.startElement(eq("http://samples.springframework.org/flight"), eq("flights"), eq("flights"),
				isA(Attributes.class));
		handlerMock.startElement(eq("http://samples.springframework.org/flight"), eq("flight"), eq("flight"),
				isA(Attributes.class));
		handlerMock.startElement(eq("http://samples.springframework.org/flight"), eq("number"), eq("number"),
				isA(Attributes.class));
		handlerMock.characters(isA(char[].class), eq(0), eq(2));
		handlerMock.endElement("http://samples.springframework.org/flight", "number", "number");
		handlerMock.endElement("http://samples.springframework.org/flight", "flight", "flight");
		handlerMock.endElement("http://samples.springframework.org/flight", "flights", "flights");
		handlerMock.endPrefixMapping("");
		handlerMock.endDocument();
		replay(handlerMock);

		SAXResult result = new SAXResult(handlerMock);
		marshaller.marshal(flights, result);
		verify(handlerMock);
	}

	@Test
	public void lazyInit() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath(CONTEXT_PATH);
		marshaller.setLazyInit(true);
		marshaller.afterPropertiesSet();
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		marshaller.marshal(flights, result);
		assertXMLEqual("Marshaller writes invalid StreamResult", EXPECTED_STRING, writer.toString());
	}

	@Test
	public void properties() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath(CONTEXT_PATH);
		marshaller.setMarshallerProperties(
				Collections.<String, Object>singletonMap(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT,
						Boolean.TRUE));
		marshaller.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void noContextPathOrClassesToBeBound() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.afterPropertiesSet();
	}

	@Test(expected = UncategorizedMappingException.class)
	public void testInvalidContextPath() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath("ab");
		marshaller.afterPropertiesSet();
	}

	@Test(expected = XmlMappingException.class)
	public void marshalInvalidClass() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(FlightType.class);
		marshaller.afterPropertiesSet();
		Result result = new StreamResult(new StringWriter());
		Flights flights = new Flights();
		marshaller.marshal(flights, result);
	}

	@Test
	public void supportsContextPath() throws Exception {
		testSupports();
	}

	@Test
	public void supportsClassesToBeBound() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Flights.class, FlightType.class);
		marshaller.afterPropertiesSet();
		testSupports();
	}

	@Test
	public void supportsPackagesToScan() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setPackagesToScan(new String[] {CONTEXT_PATH});
		marshaller.afterPropertiesSet();
		testSupports();
	}

	private void testSupports() throws Exception {
		assertTrue("Jaxb2Marshaller does not support Flights class", marshaller.supports(Flights.class));
		assertTrue("Jaxb2Marshaller does not support Flights generic type", marshaller.supports((Type)Flights.class));

		assertFalse("Jaxb2Marshaller supports FlightType class", marshaller.supports(FlightType.class));
		assertFalse("Jaxb2Marshaller supports FlightType type", marshaller.supports((Type)FlightType.class));

		Method method = ObjectFactory.class.getDeclaredMethod("createFlight", FlightType.class);
		assertTrue("Jaxb2Marshaller does not support JAXBElement<FlightsType>",
				marshaller.supports(method.getGenericReturnType()));

		marshaller.setSupportJaxbElementClass(true);
		JAXBElement<FlightType> flightTypeJAXBElement = new JAXBElement<FlightType>(new QName("http://springframework.org", "flight"), FlightType.class,
				new FlightType());
		assertTrue("Jaxb2Marshaller does not support JAXBElement<FlightsType>", marshaller.supports(flightTypeJAXBElement.getClass()));

		assertFalse("Jaxb2Marshaller supports class not in context path", marshaller.supports(DummyRootElement.class));
		assertFalse("Jaxb2Marshaller supports type not in context path", marshaller.supports((Type)DummyRootElement.class));
		method = getClass().getDeclaredMethod("createDummyRootElement");
		assertFalse("Jaxb2Marshaller supports JAXBElement not in context path",
				marshaller.supports(method.getGenericReturnType()));

		assertFalse("Jaxb2Marshaller supports class not in context path", marshaller.supports(DummyType.class));
		assertFalse("Jaxb2Marshaller supports type not in context path", marshaller.supports((Type)DummyType.class));
		method = getClass().getDeclaredMethod("createDummyType");
		assertFalse("Jaxb2Marshaller supports JAXBElement not in context path",
				marshaller.supports(method.getGenericReturnType()));

		testSupportsPrimitives();
		testSupportsStandardClasses();
	}

	private void testSupportsPrimitives() {
		final Primitives primitives = new Primitives();
		ReflectionUtils.doWithMethods(Primitives.class, new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Type returnType = method.getGenericReturnType();
				assertTrue("Jaxb2Marshaller does not support JAXBElement<" + method.getName().substring(9) + ">",
						marshaller.supports(returnType));
				try {
					// make sure the marshalling does not result in errors
					Object returnValue = method.invoke(primitives);
					marshaller.marshal(returnValue, new StreamResult(new ByteArrayOutputStream()));
				}
				catch (InvocationTargetException e) {
					fail(e.getMessage());
				}
			}
		}, new ReflectionUtils.MethodFilter() {
			public boolean matches(Method method) {
				return method.getName().startsWith("primitive");
			}
		});
	}

	private void testSupportsStandardClasses() throws Exception {
		final StandardClasses standardClasses = new StandardClasses();
		ReflectionUtils.doWithMethods(StandardClasses.class, new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Type returnType = method.getGenericReturnType();
				assertTrue("Jaxb2Marshaller does not support JAXBElement<" + method.getName().substring(13) + ">",
						marshaller.supports(returnType));
				try {
					// make sure the marshalling does not result in errors
					Object returnValue = method.invoke(standardClasses);
					marshaller.marshal(returnValue, new StreamResult(new ByteArrayOutputStream()));
				}
				catch (InvocationTargetException e) {
					fail(e.getMessage());
				}
			}
		}, new ReflectionUtils.MethodFilter() {
			public boolean matches(Method method) {
				return method.getName().startsWith("standardClass");
			}
		});
	}

	@Test
	public void supportsXmlRootElement() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(DummyRootElement.class, DummyType.class);
		marshaller.afterPropertiesSet();
		assertTrue("Jaxb2Marshaller does not support XmlRootElement class", marshaller.supports(DummyRootElement.class));
		assertTrue("Jaxb2Marshaller does not support XmlRootElement generic type", marshaller.supports((Type)DummyRootElement.class));

		assertFalse("Jaxb2Marshaller supports DummyType class", marshaller.supports(DummyType.class));
		assertFalse("Jaxb2Marshaller supports DummyType type", marshaller.supports((Type)DummyType.class));
	}


	@Test
	public void marshalAttachments() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(BinaryObject.class);
		marshaller.setMtomEnabled(true);
		marshaller.afterPropertiesSet();
		MimeContainer mimeContainer = createMock(MimeContainer.class);

		Resource logo = new ClassPathResource("spring-ws.png", getClass());
		DataHandler dataHandler = new DataHandler(new FileDataSource(logo.getFile()));

		expect(mimeContainer.convertToXopPackage()).andReturn(true);
		mimeContainer.addAttachment(isA(String.class), isA(DataHandler.class));
		expectLastCall().times(3);

		replay(mimeContainer);
		byte[] bytes = FileCopyUtils.copyToByteArray(logo.getInputStream());
		BinaryObject object = new BinaryObject(bytes, dataHandler);
		StringWriter writer = new StringWriter();
		marshaller.marshal(object, new StreamResult(writer), mimeContainer);
		verify(mimeContainer);
		assertTrue("No XML written", writer.toString().length() > 0);
	}

	@XmlRootElement
	public static class DummyRootElement {

		private DummyType t = new DummyType();

	}

	@XmlType
	public static class DummyType {

		private String s = "Hello";
	}

	private JAXBElement<DummyRootElement> createDummyRootElement() {
		return null;
	}

	private JAXBElement<DummyType> createDummyType() {
		return null;
	}

}
