/*
 * Copyright 2002-present the original author or authors.
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

import java.awt.Image;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.URLDataSource;
import jakarta.xml.bind.JAXBElement;

/**
 * Used by {@link org.springframework.oxm.jaxb.Jaxb2MarshallerTests}.
 *
 * @author Arjen Poutsma
 */
public class StandardClasses {

	private static final QName NAME = new QName("https://springframework.org/oxm-test", "standard-classes");

	private DatatypeFactory factory;

	public StandardClasses() throws DatatypeConfigurationException {
		factory = DatatypeFactory.newInstance();
	}

	/*
	java.net.URI
	javax.xml.datatype.XMLGregorianCalendarxs:anySimpleType
	javax.xml.datatype.Duration
	java.lang.Object
	java.awt.Image
	jakarta.activation.DataHandler
	javax.xml.transform.Source
	java.util.UUID
		 */
	public JAXBElement<String> standardClassString() {
		return new JAXBElement<>(NAME, String.class, "42");
	}

	public JAXBElement<BigInteger> standardClassBigInteger() {
		return new JAXBElement<>(NAME, BigInteger.class, new BigInteger("42"));
	}

	public JAXBElement<BigDecimal> standardClassBigDecimal() {
		return new JAXBElement<>(NAME, BigDecimal.class, new BigDecimal("42.0"));
	}

	public JAXBElement<Calendar> standardClassCalendar() {
		return new JAXBElement<>(NAME, Calendar.class, Calendar.getInstance());
	}

	public JAXBElement<GregorianCalendar> standardClassGregorianCalendar() {
		return new JAXBElement<>(NAME, GregorianCalendar.class, (GregorianCalendar) Calendar.getInstance());
	}

	public JAXBElement<Date> standardClassDate() {
		return new JAXBElement<>(NAME, Date.class, new Date());
	}

	public JAXBElement<QName> standardClassQName() {
		return new JAXBElement<>(NAME, QName.class, NAME);
	}

	public JAXBElement<URI> standardClassURI() {
		return new JAXBElement<>(NAME, URI.class, URI.create("https://springframework.org"));
	}

	public JAXBElement<XMLGregorianCalendar> standardClassXMLGregorianCalendar() throws DatatypeConfigurationException {
		XMLGregorianCalendar calendar =
				factory.newXMLGregorianCalendar((GregorianCalendar) Calendar.getInstance());
		return new JAXBElement<>(NAME, XMLGregorianCalendar.class, calendar);
	}

	public JAXBElement<Duration> standardClassDuration() {
		Duration duration = factory.newDuration(42000);
		return new JAXBElement<>(NAME, Duration.class, duration);
	}

	public JAXBElement<Image> standardClassImage() throws IOException {
		Image image = ImageIO.read(getClass().getResourceAsStream("spring-ws.png"));
		return new JAXBElement<>(NAME, Image.class, image);
	}

	public JAXBElement<DataHandler> standardClassDataHandler() {
		DataSource dataSource = new URLDataSource(getClass().getResource("spring-ws.png"));
		DataHandler dataHandler = new DataHandler(dataSource);
		return new JAXBElement<>(NAME, DataHandler.class, dataHandler);
	}

	/* The following should work according to the spec, but doesn't on the JAXB2 implementation including in JDK 1.6.0_17
	public JAXBElement<Source> standardClassSource() {
		StringReader reader = new StringReader("<foo/>");
		return new JAXBElement<Source>(NAME, Source.class, new StreamSource(reader));
	}
	*/

	public JAXBElement<UUID> standardClassUUID() {
		return new JAXBElement<>(NAME, UUID.class, UUID.randomUUID());
	}

}
