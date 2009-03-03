package org.springframework.http.converter.xml;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;

/** @author Arjen Poutsma */
public class MarshallingHttpMessageConverterTest {

	private MarshallingHttpMessageConverter converter;

	private Marshaller marshaller;

	private Unmarshaller unmarshaller;

	@Before
	public void setUp() {
		marshaller = createMock(Marshaller.class);
		unmarshaller = createMock(Unmarshaller.class);

		converter = new MarshallingHttpMessageConverter(marshaller, unmarshaller);
	}

	@Test
	public void read() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));

		expect(unmarshaller.unmarshal(isA(StreamSource.class))).andReturn(body);

		replay(marshaller, unmarshaller);
		String result = (String) converter.read(Object.class, inputMessage);
		assertEquals("Invalid result", body, result);
		verify(marshaller, unmarshaller);
	}

	@Test
	public void write() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();

		marshaller.marshal(eq(body), isA(StreamResult.class));

		replay(marshaller, unmarshaller);
		converter.write(body, outputMessage);
		assertEquals("Invalid content-type", new MediaType("application", "xml"),
				outputMessage.getHeaders().getContentType());
		verify(marshaller, unmarshaller);
	}
}
