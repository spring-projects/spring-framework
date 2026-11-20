/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.oxm.xstream;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import com.thoughtworks.xstream.security.AnyTypePermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import org.springframework.util.xml.StaxUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
class XStreamUnmarshallerTests {

	protected static final String INPUT_STRING = "<flight><flightNumber>42</flightNumber></flight>";

	private XStreamMarshaller unmarshaller;


	@BeforeEach
	void createUnmarshaller() {
		unmarshaller = new XStreamMarshaller();
		unmarshaller.setTypePermissions(AnyTypePermission.ANY);
		Map<String, Class<?>> aliases = new HashMap<>();
		aliases.put("flight", Flight.class);
		unmarshaller.setAliases(aliases);
	}


	@Test
	void unmarshalDomSource() throws Exception {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document document = builder.parse(new InputSource(new StringReader(INPUT_STRING)));
		DOMSource source = new DOMSource(document);
		Object flight = unmarshaller.unmarshal(source);
		testFlight(flight);
	}

	@Test
	void unmarshalStaxSourceXmlStreamReader() throws Exception {
		XMLInputFactory inputFactory = XMLInputFactory.newInstance();
		XMLStreamReader streamReader = inputFactory.createXMLStreamReader(new StringReader(INPUT_STRING));
		Source source = StaxUtils.createStaxSource(streamReader);
		Object flights = unmarshaller.unmarshal(source);
		testFlight(flights);
	}

	@Test
	void unmarshalStreamSourceInputStream() throws Exception {
		StreamSource source = new StreamSource(new ByteArrayInputStream(INPUT_STRING.getBytes(StandardCharsets.UTF_8)));
		Object flights = unmarshaller.unmarshal(source);
		testFlight(flights);
	}

	@Test
	void unmarshalStreamSourceReader() throws Exception {
		StreamSource source = new StreamSource(new StringReader(INPUT_STRING));
		Object flights = unmarshaller.unmarshal(source);
		testFlight(flights);
	}


	private void testFlight(Object o) {
		assertThat(o).isInstanceOfSatisfying(Flight.class, flight -> {
			assertThat(flight).as("Flight is null").isNotNull();
			assertThat(flight.getFlightNumber()).as("Number is invalid").isEqualTo(42L);
		});
	}

}

