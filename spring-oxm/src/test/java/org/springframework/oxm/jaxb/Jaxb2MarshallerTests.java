/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.oxm.jaxb;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;

import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xmlunit.diff.DifferenceEvaluator;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.testfixture.xml.XmlContent;
import org.springframework.oxm.AbstractMarshallerTests;
import org.springframework.oxm.UncategorizedMappingException;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.jaxb.test.FlightType;
import org.springframework.oxm.jaxb.test.Flights;
import org.springframework.oxm.jaxb.test.ObjectFactory;
import org.springframework.oxm.mime.MimeContainer;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.xmlunit.diff.ComparisonType.XML_STANDALONE;
import static org.xmlunit.diff.DifferenceEvaluators.Default;
import static org.xmlunit.diff.DifferenceEvaluators.chain;
import static org.xmlunit.diff.DifferenceEvaluators.downgradeDifferencesToEqual;

/**
 * @author Arjen Poutsma
 * @author Biju Kunjummen
 * @author Sam Brannen
 */
class Jaxb2MarshallerTests extends AbstractMarshallerTests<Jaxb2Marshaller> {

	private static final String CONTEXT_PATH = "org.springframework.oxm.jaxb.test";

	private Flights flights;


	@Override
	protected Jaxb2Marshaller createMarshaller() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
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
	void marshalSAXResult() throws Exception {
		ContentHandler contentHandler = mock(ContentHandler.class);
		SAXResult result = new SAXResult(contentHandler);
		marshaller.marshal(flights, result);
		InOrder ordered = inOrder(contentHandler);
		ordered.verify(contentHandler).setDocumentLocator(isA(Locator.class));
		ordered.verify(contentHandler).startDocument();
		ordered.verify(contentHandler).startPrefixMapping("", "http://samples.springframework.org/flight");
		ordered.verify(contentHandler).startElement(eq("http://samples.springframework.org/flight"), eq("flights"), eq("flights"), isA(Attributes.class));
		ordered.verify(contentHandler).startElement(eq("http://samples.springframework.org/flight"), eq("flight"), eq("flight"), isA(Attributes.class));
		ordered.verify(contentHandler).startElement(eq("http://samples.springframework.org/flight"), eq("number"), eq("number"), isA(Attributes.class));
		ordered.verify(contentHandler).characters(isA(char[].class), eq(0), eq(2));
		ordered.verify(contentHandler).endElement("http://samples.springframework.org/flight", "number", "number");
		ordered.verify(contentHandler).endElement("http://samples.springframework.org/flight", "flight", "flight");
		ordered.verify(contentHandler).endElement("http://samples.springframework.org/flight", "flights", "flights");
		ordered.verify(contentHandler).endPrefixMapping("");
		ordered.verify(contentHandler).endDocument();
	}

	@Test
	void lazyInit() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath(CONTEXT_PATH);
		marshaller.setLazyInit(true);
		marshaller.afterPropertiesSet();
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		marshaller.marshal(flights, result);
		DifferenceEvaluator ev = chain(Default, downgradeDifferencesToEqual(XML_STANDALONE));
		assertThat(XmlContent.from(writer)).isSimilarTo(EXPECTED_STRING, ev);
	}

	@Test
	void properties() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath(CONTEXT_PATH);
		marshaller.setMarshallerProperties(
				Collections.singletonMap(jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE));
		marshaller.afterPropertiesSet();
	}

	@Test
	void noContextPathOrClassesToBeBound() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		assertThatIllegalArgumentException().isThrownBy(marshaller::afterPropertiesSet);
	}

	@Test
	void testInvalidContextPath() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setContextPath("ab");
		assertThatExceptionOfType(UncategorizedMappingException.class).isThrownBy(marshaller::afterPropertiesSet);
	}

	@Test
	void marshalInvalidClass() throws Exception {
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(FlightType.class);
		marshaller.afterPropertiesSet();
		Result result = new StreamResult(new StringWriter());
		Flights flights = new Flights();
		assertThatExceptionOfType(XmlMappingException.class).isThrownBy(() -> marshaller.marshal(flights, result));
	}

	@Test
	void supportsContextPath() throws Exception {
		testSupports();
	}

	@Test
	void supportsClassesToBeBound() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(Flights.class, FlightType.class);
		marshaller.afterPropertiesSet();
		testSupports();
	}

	@Test
	void supportsPackagesToScan() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setPackagesToScan(CONTEXT_PATH);
		marshaller.afterPropertiesSet();
		testSupports();
	}

	private void testSupports() throws Exception {
		assertThat(marshaller.supports(Flights.class)).as("Jaxb2Marshaller does not support Flights class").isTrue();
		assertThat(marshaller.supports((Type)Flights.class)).as("Jaxb2Marshaller does not support Flights generic type").isTrue();

		assertThat(marshaller.supports(FlightType.class)).as("Jaxb2Marshaller supports FlightType class").isFalse();
		assertThat(marshaller.supports((Type)FlightType.class)).as("Jaxb2Marshaller supports FlightType type").isFalse();

		Method method = ObjectFactory.class.getDeclaredMethod("createFlight", FlightType.class);
		assertThat(marshaller.supports(method.getGenericReturnType())).as("Jaxb2Marshaller does not support JAXBElement<FlightsType>").isTrue();

		marshaller.setSupportJaxbElementClass(true);
		JAXBElement<FlightType> flightTypeJAXBElement = new JAXBElement<>(new QName("https://springframework.org", "flight"), FlightType.class,
				new FlightType());
		assertThat(marshaller.supports(flightTypeJAXBElement.getClass())).as("Jaxb2Marshaller does not support JAXBElement<FlightsType>").isTrue();

		assertThat(marshaller.supports(DummyRootElement.class)).as("Jaxb2Marshaller supports class not in context path").isFalse();
		assertThat(marshaller.supports((Type)DummyRootElement.class)).as("Jaxb2Marshaller supports type not in context path").isFalse();
		method = getClass().getDeclaredMethod("createDummyRootElement");
		assertThat(marshaller.supports(method.getGenericReturnType())).as("Jaxb2Marshaller supports JAXBElement not in context path").isFalse();

		assertThat(marshaller.supports(DummyType.class)).as("Jaxb2Marshaller supports class not in context path").isFalse();
		assertThat(marshaller.supports((Type)DummyType.class)).as("Jaxb2Marshaller supports type not in context path").isFalse();
		method = getClass().getDeclaredMethod("createDummyType");
		assertThat(marshaller.supports(method.getGenericReturnType())).as("Jaxb2Marshaller supports JAXBElement not in context path").isFalse();

		testSupportsPrimitives();
		testSupportsStandardClasses();
	}

	private void testSupportsPrimitives() {
		final Primitives primitives = new Primitives();
		ReflectionUtils.doWithMethods(Primitives.class, method -> {
				Type returnType = method.getGenericReturnType();
				assertThat(marshaller.supports(returnType))
					.as("Jaxb2Marshaller does not support JAXBElement<" + method.getName().substring(9) + ">")
					.isTrue();
				try {
					// make sure the marshalling does not result in errors
					Object returnValue = method.invoke(primitives);
					marshaller.marshal(returnValue, new StreamResult(new ByteArrayOutputStream()));
				}
				catch (InvocationTargetException e) {
					throw new AssertionError(e.getMessage(), e);
				}
			},
			method -> method.getName().startsWith("primitive")
		);
	}

	private void testSupportsStandardClasses() throws Exception {
		final StandardClasses standardClasses = new StandardClasses();
		ReflectionUtils.doWithMethods(StandardClasses.class, method -> {
				Type returnType = method.getGenericReturnType();
				assertThat(marshaller.supports(returnType))
					.as("Jaxb2Marshaller does not support JAXBElement<" + method.getName().substring(13) + ">")
					.isTrue();
				try {
					// make sure the marshalling does not result in errors
					Object returnValue = method.invoke(standardClasses);
					marshaller.marshal(returnValue, new StreamResult(new ByteArrayOutputStream()));
				}
				catch (InvocationTargetException e) {
					throw new AssertionError(e.getMessage(), e);
				}
			},
			method -> method.getName().startsWith("standardClass")
		);
	}

	@Test
	void supportsXmlRootElement() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(DummyRootElement.class, DummyType.class);
		marshaller.afterPropertiesSet();
		assertThat(marshaller.supports(DummyRootElement.class)).as("Jaxb2Marshaller does not support XmlRootElement class").isTrue();
		assertThat(marshaller.supports((Type)DummyRootElement.class)).as("Jaxb2Marshaller does not support XmlRootElement generic type").isTrue();

		assertThat(marshaller.supports(DummyType.class)).as("Jaxb2Marshaller supports DummyType class").isFalse();
		assertThat(marshaller.supports((Type)DummyType.class)).as("Jaxb2Marshaller supports DummyType type").isFalse();
	}


	@Test
	void marshalAttachments() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(BinaryObject.class);
		marshaller.setMtomEnabled(true);
		marshaller.afterPropertiesSet();
		MimeContainer mimeContainer = mock(MimeContainer.class);

		Resource logo = new ClassPathResource("spring-ws.png", getClass());
		DataHandler dataHandler = new DataHandler(new FileDataSource(logo.getFile()));

		given(mimeContainer.convertToXopPackage()).willReturn(true);
		byte[] bytes = FileCopyUtils.copyToByteArray(logo.getInputStream());
		BinaryObject object = new BinaryObject(bytes, dataHandler);
		StringWriter writer = new StringWriter();
		marshaller.marshal(object, new StreamResult(writer), mimeContainer);
		assertThat(writer.toString().length() > 0).as("No XML written").isTrue();
		verify(mimeContainer, times(3)).addAttachment(isA(String.class), isA(DataHandler.class));
	}

	@Test  // SPR-10714
	void marshalAWrappedObjectHoldingAnXmlElementDeclElement() throws Exception {
		marshaller = new Jaxb2Marshaller();
		marshaller.setPackagesToScan("org.springframework.oxm.jaxb");
		marshaller.afterPropertiesSet();
		Airplane airplane = new Airplane();
		airplane.setName("test");
		StringWriter writer = new StringWriter();
		Result result = new StreamResult(writer);
		marshaller.marshal(airplane, result);
		DifferenceEvaluator ev = chain(Default, downgradeDifferencesToEqual(XML_STANDALONE));
		assertThat(XmlContent.from(writer)).isSimilarTo("<airplane><name>test</name></airplane>", ev);
	}

	@Test  // SPR-10806
	void unmarshalStreamSourceWithXmlOptions() throws Exception {
		final jakarta.xml.bind.Unmarshaller unmarshaller = mock(jakarta.xml.bind.Unmarshaller.class);
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller() {
			@Override
			public jakarta.xml.bind.Unmarshaller createUnmarshaller() {
				return unmarshaller;
			}
		};

		// 1. external-general-entities and dtd support disabled (default)

		marshaller.unmarshal(new StreamSource("1"));
		ArgumentCaptor<SAXSource> sourceCaptor = ArgumentCaptor.forClass(SAXSource.class);
		verify(unmarshaller).unmarshal(sourceCaptor.capture());

		SAXSource result = sourceCaptor.getValue();
		assertThat(result.getXMLReader().getFeature("http://apache.org/xml/features/disallow-doctype-decl")).isEqualTo(true);
		assertThat(result.getXMLReader().getFeature("http://xml.org/sax/features/external-general-entities")).isEqualTo(false);

		// 2. external-general-entities and dtd support enabled

		reset(unmarshaller);
		marshaller.setProcessExternalEntities(true);
		marshaller.setSupportDtd(true);

		marshaller.unmarshal(new StreamSource("1"));
		verify(unmarshaller).unmarshal(sourceCaptor.capture());

		result = sourceCaptor.getValue();
		assertThat(result.getXMLReader().getFeature("http://apache.org/xml/features/disallow-doctype-decl")).isEqualTo(false);
		assertThat(result.getXMLReader().getFeature("http://xml.org/sax/features/external-general-entities")).isEqualTo(true);
	}

	@Test  // SPR-10806
	void unmarshalSaxSourceWithXmlOptions() throws Exception {
		final jakarta.xml.bind.Unmarshaller unmarshaller = mock(jakarta.xml.bind.Unmarshaller.class);
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller() {
			@Override
			public jakarta.xml.bind.Unmarshaller createUnmarshaller() {
				return unmarshaller;
			}
		};

		// 1. external-general-entities and dtd support disabled (default)

		marshaller.unmarshal(new SAXSource(new InputSource("1")));
		ArgumentCaptor<SAXSource> sourceCaptor = ArgumentCaptor.forClass(SAXSource.class);
		verify(unmarshaller).unmarshal(sourceCaptor.capture());

		SAXSource result = sourceCaptor.getValue();
		assertThat(result.getXMLReader().getFeature("http://apache.org/xml/features/disallow-doctype-decl")).isEqualTo(true);
		assertThat(result.getXMLReader().getFeature("http://xml.org/sax/features/external-general-entities")).isEqualTo(false);

		// 2. external-general-entities and dtd support enabled

		reset(unmarshaller);
		marshaller.setProcessExternalEntities(true);
		marshaller.setSupportDtd(true);

		marshaller.unmarshal(new SAXSource(new InputSource("1")));
		verify(unmarshaller).unmarshal(sourceCaptor.capture());

		result = sourceCaptor.getValue();
		assertThat(result.getXMLReader().getFeature("http://apache.org/xml/features/disallow-doctype-decl")).isEqualTo(false);
		assertThat(result.getXMLReader().getFeature("http://xml.org/sax/features/external-general-entities")).isEqualTo(true);
	}


	@XmlRootElement
	@SuppressWarnings("unused")
	public static class DummyRootElement {

		private DummyType t = new DummyType();
	}

	@XmlType
	@SuppressWarnings("unused")
	public static class DummyType {

		private String s = "Hello";
	}

	@SuppressWarnings("unused")
	private JAXBElement<DummyRootElement> createDummyRootElement() {
		return null;
	}

	@SuppressWarnings("unused")
	private JAXBElement<DummyType> createDummyType() {
		return null;
	}

}
