/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.IOException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJacksonValue;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Jackson 2.x XML converter tests.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class MappingJackson2XmlHttpMessageConverterTests {

	private final MappingJackson2XmlHttpMessageConverter converter = new MappingJackson2XmlHttpMessageConverter();

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	public void canRead() {
		assertTrue(converter.canRead(MyBean.class, new MediaType("application", "xml")));
		assertTrue(converter.canRead(MyBean.class, new MediaType("text", "xml")));
		assertTrue(converter.canRead(MyBean.class, new MediaType("application", "soap+xml")));
	}

	@Test
	public void canWrite() {
		assertTrue(converter.canWrite(MyBean.class, new MediaType("application", "xml")));
		assertTrue(converter.canWrite(MyBean.class, new MediaType("text", "xml")));
		assertTrue(converter.canWrite(MyBean.class, new MediaType("application", "soap+xml")));
	}

	@Test
	public void read() throws IOException {
		String body = "<MyBean><string>Foo</string><number>42</number><fraction>42.0</fraction><array><array>Foo</array><array>Bar</array></array><bool>true</bool><bytes>AQI=</bytes></MyBean>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		MyBean result = (MyBean) converter.read(MyBean.class, inputMessage);
		assertEquals("Foo", result.getString());
		assertEquals(42, result.getNumber());
		assertEquals(42F, result.getFraction(), 0F);
		assertArrayEquals(new String[]{"Foo", "Bar"}, result.getArray());
		assertTrue(result.isBool());
		assertArrayEquals(new byte[]{0x1, 0x2}, result.getBytes());
	}

	@Test
	public void write() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MyBean body = new MyBean();
		body.setString("Foo");
		body.setNumber(42);
		body.setFraction(42F);
		body.setArray(new String[]{"Foo", "Bar"});
		body.setBool(true);
		body.setBytes(new byte[]{0x1, 0x2});
		converter.write(body, null, outputMessage);
		Charset utf8 = Charset.forName("UTF-8");
		String result = outputMessage.getBodyAsString(utf8);
		assertTrue(result.contains("<string>Foo</string>"));
		assertTrue(result.contains("<number>42</number>"));
		assertTrue(result.contains("<fraction>42.0</fraction>"));
		assertTrue(result.contains("<array><array>Foo</array><array>Bar</array></array>"));
		assertTrue(result.contains("<bool>true</bool>"));
		assertTrue(result.contains("<bytes>AQI=</bytes>"));
		assertEquals("Invalid content-type", new MediaType("application", "xml", utf8),
				outputMessage.getHeaders().getContentType());
	}

	@Test(expected = HttpMessageNotReadableException.class)
	public void readInvalidXml() throws IOException {
		String body = "FooBar";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		converter.read(MyBean.class, inputMessage);
	}

	@Test
	public void readValidXmlWithUnknownProperty() throws IOException {
		String body = "<MyBean><string>string</string><unknownProperty>value</unknownProperty></MyBean>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		converter.read(MyBean.class, inputMessage);
		// Assert no HttpMessageNotReadableException is thrown
	}

	@Test
	public void jsonView() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		JacksonViewBean bean = new JacksonViewBean();
		bean.setWithView1("with");
		bean.setWithView2("with");
		bean.setWithoutView("without");

		MappingJacksonValue jacksonValue = new MappingJacksonValue(bean);
		jacksonValue.setSerializationView(MyJacksonView1.class);
		this.converter.write(jacksonValue, null, outputMessage);

		String result = outputMessage.getBodyAsString(Charset.forName("UTF-8"));
		assertThat(result, containsString("<withView1>with</withView1>"));
		assertThat(result, not(containsString("<withView2>with</withView2>")));
		assertThat(result, not(containsString("<withoutView>without</withoutView>")));
	}

	@Test
	public void customXmlMapper() {
		new MappingJackson2XmlHttpMessageConverter(new MyXmlMapper());
		// Assert no exception is thrown
	}

	@Test
	public void readWithExternalReference() throws IOException {
		String body = "<!DOCTYPE MyBean SYSTEM \"http://192.168.28.42/1.jsp\" [" +
				"  <!ELEMENT root ANY >\n" +
				"  <!ENTITY ext SYSTEM \"" +
				new ClassPathResource("external.txt", getClass()).getURI() +
				"\" >]><MyBean><string>&ext;</string></MyBean>";

		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));

		this.thrown.expect(HttpMessageNotReadableException.class);
		this.converter.read(MyBean.class, inputMessage);
	}

	@Test
	public void readWithXmlBomb() throws IOException {
		// https://en.wikipedia.org/wiki/Billion_laughs
		// https://msdn.microsoft.com/en-us/magazine/ee335713.aspx
		String body = "<?xml version=\"1.0\"?>\n" +
				"<!DOCTYPE lolz [\n" +
				" <!ENTITY lol \"lol\">\n" +
				" <!ELEMENT lolz (#PCDATA)>\n" +
				" <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n" +
				" <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n" +
				" <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n" +
				" <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n" +
				" <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\n" +
				" <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\n" +
				" <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">\n" +
				" <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">\n" +
				" <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">\n" +
				"]>\n" +
				"<MyBean>&lol9;</MyBean>";

		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));

		this.thrown.expect(HttpMessageNotReadableException.class);
		this.converter.read(MyBean.class, inputMessage);
	}


	public static class MyBean {

		private String string;

		private int number;

		private float fraction;

		private String[] array;

		private boolean bool;

		private byte[] bytes;

		public byte[] getBytes() {
			return bytes;
		}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}

		public boolean isBool() {
			return bool;
		}

		public void setBool(boolean bool) {
			this.bool = bool;
		}

		public String getString() {
			return string;
		}

		public void setString(String string) {
			this.string = string;
		}

		public int getNumber() {
			return number;
		}

		public void setNumber(int number) {
			this.number = number;
		}

		public float getFraction() {
			return fraction;
		}

		public void setFraction(float fraction) {
			this.fraction = fraction;
		}

		public String[] getArray() {
			return array;
		}

		public void setArray(String[] array) {
			this.array = array;
		}
	}


	private interface MyJacksonView1 {};

	private interface MyJacksonView2 {};


	@SuppressWarnings("unused")
	private static class JacksonViewBean {

		@JsonView(MyJacksonView1.class)
		private String withView1;

		@JsonView(MyJacksonView2.class)
		private String withView2;

		private String withoutView;

		public String getWithView1() {
			return withView1;
		}

		public void setWithView1(String withView1) {
			this.withView1 = withView1;
		}

		public String getWithView2() {
			return withView2;
		}

		public void setWithView2(String withView2) {
			this.withView2 = withView2;
		}

		public String getWithoutView() {
			return withoutView;
		}

		public void setWithoutView(String withoutView) {
			this.withoutView = withoutView;
		}
	}


	@SuppressWarnings("serial")
	private static class MyXmlMapper extends XmlMapper {
	}

}
