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

package org.springframework.http.converter.xml;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.web.testfixture.http.MockHttpInputMessage;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.within;

/**
 * Jackson 2.x XML converter tests.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class MappingJackson2XmlHttpMessageConverterTests {

	private final MappingJackson2XmlHttpMessageConverter converter = new MappingJackson2XmlHttpMessageConverter();


	@Test
	public void canRead() {
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "xml"))).isTrue();
		assertThat(converter.canRead(MyBean.class, new MediaType("text", "xml"))).isTrue();
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "soap+xml"))).isTrue();
		assertThat(converter.canRead(MyBean.class, new MediaType("text", "xml", StandardCharsets.UTF_8))).isTrue();
		assertThat(converter.canRead(MyBean.class, new MediaType("text", "xml", StandardCharsets.ISO_8859_1))).isTrue();
	}

	@Test
	public void canWrite() {
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "xml"))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("text", "xml"))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "soap+xml"))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("text", "xml", StandardCharsets.UTF_8))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("text", "xml", StandardCharsets.ISO_8859_1))).isFalse();
	}

	@Test
	public void read() throws IOException {
		String body = "<MyBean>" +
				"<string>Foo</string>" +
				"<number>42</number>" +
				"<fraction>42.0</fraction>" +
				"<array><array>Foo</array>" +
				"<array>Bar</array></array>" +
				"<bool>true</bool>" +
				"<bytes>AQI=</bytes></MyBean>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml"));
		MyBean result = (MyBean) converter.read(MyBean.class, inputMessage);
		assertThat(result.getString()).isEqualTo("Foo");
		assertThat(result.getNumber()).isEqualTo(42);
		assertThat(result.getFraction()).isCloseTo(42F, within(0F));
		assertThat(result.getArray()).isEqualTo(new String[]{"Foo", "Bar"});
		assertThat(result.isBool()).isTrue();
		assertThat(result.getBytes()).isEqualTo(new byte[]{0x1, 0x2});
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
		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result).contains("<string>Foo</string>");
		assertThat(result).contains("<number>42</number>");
		assertThat(result).contains("<fraction>42.0</fraction>");
		assertThat(result).contains("<array><array>Foo</array><array>Bar</array></array>");
		assertThat(result).contains("<bool>true</bool>");
		assertThat(result).contains("<bytes>AQI=</bytes>");
		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type").isEqualTo(new MediaType("application", "xml", StandardCharsets.UTF_8));
	}

	@Test
	public void readInvalidXml() throws IOException {
		String body = "FooBar";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(MediaType.APPLICATION_XML);
		assertThatExceptionOfType(HttpMessageNotReadableException.class).isThrownBy(() ->
				converter.read(MyBean.class, inputMessage));
	}

	@Test
	public void readValidXmlWithUnknownProperty() throws IOException {
		String body = "<MyBean><string>string</string><unknownProperty>value</unknownProperty></MyBean>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(MediaType.APPLICATION_XML);
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

		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result).contains("<withView1>with</withView1>");
		assertThat(result).doesNotContain("<withView2>with</withView2>");
		assertThat(result).doesNotContain("<withoutView>without</withoutView>");
	}

	@Test
	public void customXmlMapper() {
		new MappingJackson2XmlHttpMessageConverter(new MyXmlMapper());
		// Assert no exception is thrown
	}

	@Test
	public void readWithExternalReference() throws IOException {
		String body = "<!DOCTYPE MyBean SYSTEM \"https://192.168.28.42/1.jsp\" [" +
				"  <!ELEMENT root ANY >\n" +
				"  <!ENTITY ext SYSTEM \"" +
				new ClassPathResource("external.txt", getClass()).getURI() +
				"\" >]><MyBean><string>&ext;</string></MyBean>";

		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(MediaType.APPLICATION_XML);

		assertThatExceptionOfType(HttpMessageNotReadableException.class).isThrownBy(() ->
				this.converter.read(MyBean.class, inputMessage));
	}

	@Test
	public void readWithXmlBomb() throws IOException {
		// https://en.wikipedia.org/wiki/Billion_laughs
		// https://msdn.microsoft.com/en-us/magazine/ee335713.aspx
		String body = """
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
				<MyBean>&lol9;</MyBean>""";

		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(MediaType.APPLICATION_XML);

		assertThatExceptionOfType(HttpMessageNotReadableException.class).isThrownBy(() ->
				this.converter.read(MyBean.class, inputMessage));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readNonUnicode() throws Exception {
		String body = "<MyBean>" +
				"<string>føø bår</string>" +
				"</MyBean>";

		Charset charset = StandardCharsets.ISO_8859_1;
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(charset));
		inputMessage.getHeaders().setContentType(new MediaType("application", "xml", charset));
		MyBean result = (MyBean) converter.read(MyBean.class, inputMessage);
		assertThat(result.getString()).isEqualTo("føø bår");
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


	private interface MyJacksonView1 {}

	private interface MyJacksonView2 {}


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
