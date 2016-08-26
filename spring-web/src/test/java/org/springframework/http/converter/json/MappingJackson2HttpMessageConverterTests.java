/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.converter.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Jackson 2.x converter tests.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class MappingJackson2HttpMessageConverterTests {

	protected static final String NEWLINE_SYSTEM_PROPERTY = System.getProperty("line.separator");

	private final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();


	@Test
	public void canRead() {
		assertTrue(converter.canRead(MyBean.class, new MediaType("application", "json")));
		assertTrue(converter.canRead(Map.class, new MediaType("application", "json")));
	}

	@Test
	public void canWrite() {
		assertTrue(converter.canWrite(MyBean.class, new MediaType("application", "json")));
		assertTrue(converter.canWrite(Map.class, new MediaType("application", "json")));
	}

	@Test  // SPR-7905
	public void canReadAndWriteMicroformats() {
		assertTrue(converter.canRead(MyBean.class, new MediaType("application", "vnd.test-micro-type+json")));
		assertTrue(converter.canWrite(MyBean.class, new MediaType("application", "vnd.test-micro-type+json")));
	}

	@Test
	public void readTyped() throws IOException {
		String body =
				"{\"bytes\":\"AQI=\",\"array\":[\"Foo\",\"Bar\"],\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		MyBean result = (MyBean) converter.read(MyBean.class, inputMessage);
		assertEquals("Foo", result.getString());
		assertEquals(42, result.getNumber());
		assertEquals(42F, result.getFraction(), 0F);
		assertArrayEquals(new String[]{"Foo", "Bar"}, result.getArray());
		assertTrue(result.isBool());
		assertArrayEquals(new byte[]{0x1, 0x2}, result.getBytes());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readUntyped() throws IOException {
		String body =
				"{\"bytes\":\"AQI=\",\"array\":[\"Foo\",\"Bar\"],\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		HashMap<String, Object> result = (HashMap<String, Object>) converter.read(HashMap.class, inputMessage);
		assertEquals("Foo", result.get("string"));
		assertEquals(42, result.get("number"));
		assertEquals(42D, (Double) result.get("fraction"), 0D);
		List<String> array = new ArrayList<>();
		array.add("Foo");
		array.add("Bar");
		assertEquals(array, result.get("array"));
		assertEquals(Boolean.TRUE, result.get("bool"));
		assertEquals("AQI=", result.get("bytes"));
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
		assertTrue(result.contains("\"string\":\"Foo\""));
		assertTrue(result.contains("\"number\":42"));
		assertTrue(result.contains("fraction\":42.0"));
		assertTrue(result.contains("\"array\":[\"Foo\",\"Bar\"]"));
		assertTrue(result.contains("\"bool\":true"));
		assertTrue(result.contains("\"bytes\":\"AQI=\""));
		assertEquals("Invalid content-type", new MediaType("application", "json", StandardCharsets.UTF_8),
				outputMessage.getHeaders().getContentType());
	}

	@Test
	public void writeUTF16() throws IOException {
		MediaType contentType = new MediaType("application", "json", StandardCharsets.UTF_16BE);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		String body = "H\u00e9llo W\u00f6rld";
		converter.write(body, contentType, outputMessage);
		assertEquals("Invalid result", "\"" + body + "\"", outputMessage.getBodyAsString(StandardCharsets.UTF_16BE));
		assertEquals("Invalid content-type", contentType, outputMessage.getHeaders().getContentType());
	}

	@Test(expected = HttpMessageNotReadableException.class)
	public void readInvalidJson() throws IOException {
		String body = "FooBar";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		converter.read(MyBean.class, inputMessage);
	}

	@Test
	public void readValidJsonWithUnknownProperty() throws IOException {
		String body = "{\"string\":\"string\",\"unknownProperty\":\"value\"}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		converter.read(MyBean.class, inputMessage);
		// Assert no HttpMessageNotReadableException is thrown
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readGenerics() throws IOException {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter() {

			@Override
			protected JavaType getJavaType(Type type, Class<?> contextClass) {
				if (type instanceof Class && List.class.isAssignableFrom((Class<?>)type)) {
					return new ObjectMapper().getTypeFactory().constructCollectionType(ArrayList.class, MyBean.class);
				}
				else {
					return super.getJavaType(type, contextClass);
				}
			}
		};
		String body =
				"[{\"bytes\":\"AQI=\",\"array\":[\"Foo\",\"Bar\"],\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}]";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));

		List<MyBean> results = (List<MyBean>) converter.read(List.class, inputMessage);
		assertEquals(1, results.size());
		MyBean result = results.get(0);
		assertEquals("Foo", result.getString());
		assertEquals(42, result.getNumber());
		assertEquals(42F, result.getFraction(), 0F);
		assertArrayEquals(new String[]{"Foo", "Bar"}, result.getArray());
		assertTrue(result.isBool());
		assertArrayEquals(new byte[]{0x1, 0x2}, result.getBytes());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readParameterizedType() throws IOException {
		ParameterizedTypeReference<List<MyBean>> beansList = new ParameterizedTypeReference<List<MyBean>>() {};

		String body =
				"[{\"bytes\":\"AQI=\",\"array\":[\"Foo\",\"Bar\"],\"number\":42,\"string\":\"Foo\",\"bool\":true,\"fraction\":42.0}]";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		List<MyBean> results = (List<MyBean>) converter.read(beansList.getType(), null, inputMessage);
		assertEquals(1, results.size());
		MyBean result = results.get(0);
		assertEquals("Foo", result.getString());
		assertEquals(42, result.getNumber());
		assertEquals(42F, result.getFraction(), 0F);
		assertArrayEquals(new String[]{"Foo", "Bar"}, result.getArray());
		assertTrue(result.isBool());
		assertArrayEquals(new byte[]{0x1, 0x2}, result.getBytes());
	}


	@Test
	public void prettyPrint() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		PrettyPrintBean bean = new PrettyPrintBean();
		bean.setName("Jason");

		this.converter.setPrettyPrint(true);
		this.converter.writeInternal(bean, null, outputMessage);
		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);

		assertEquals("{" + NEWLINE_SYSTEM_PROPERTY + "  \"name\" : \"Jason\"" + NEWLINE_SYSTEM_PROPERTY + "}", result);
	}

	@Test
	public void prefixJson() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.setPrefixJson(true);
		this.converter.writeInternal("foo", null, outputMessage);

		assertEquals(")]}', \"foo\"", outputMessage.getBodyAsString(StandardCharsets.UTF_8));
	}

	@Test
	public void prefixJsonCustom() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.setJsonPrefix(")))");
		this.converter.writeInternal("foo", null, outputMessage);

		assertEquals(")))\"foo\"", outputMessage.getBodyAsString(StandardCharsets.UTF_8));
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
		this.converter.writeInternal(jacksonValue, null, outputMessage);

		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result, containsString("\"withView1\":\"with\""));
		assertThat(result, not(containsString("\"withView2\":\"with\"")));
		assertThat(result, not(containsString("\"withoutView\":\"without\"")));
	}

	@Test
	public void filters() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		JacksonFilteredBean bean = new JacksonFilteredBean();
		bean.setProperty1("value");
		bean.setProperty2("value");

		MappingJacksonValue jacksonValue = new MappingJacksonValue(bean);
		FilterProvider filters = new SimpleFilterProvider().addFilter("myJacksonFilter",
				SimpleBeanPropertyFilter.serializeAllExcept("property2"));
		jacksonValue.setFilters(filters);
		this.converter.writeInternal(jacksonValue, null, outputMessage);

		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result, containsString("\"property1\":\"value\""));
		assertThat(result, not(containsString("\"property2\":\"value\"")));
	}

	@Test
	public void jsonp() throws Exception {
		MappingJacksonValue jacksonValue = new MappingJacksonValue("foo");
		jacksonValue.setSerializationView(MyJacksonView1.class);
		jacksonValue.setJsonpFunction("callback");

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.writeInternal(jacksonValue, null, outputMessage);

		assertEquals("/**/callback(\"foo\");", outputMessage.getBodyAsString(StandardCharsets.UTF_8));
	}

	@Test
	public void jsonpAndJsonView() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		JacksonViewBean bean = new JacksonViewBean();
		bean.setWithView1("with");
		bean.setWithView2("with");
		bean.setWithoutView("without");

		MappingJacksonValue jacksonValue = new MappingJacksonValue(bean);
		jacksonValue.setSerializationView(MyJacksonView1.class);
		jacksonValue.setJsonpFunction("callback");
		this.converter.writeInternal(jacksonValue, null, outputMessage);

		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result, startsWith("/**/callback("));
		assertThat(result, endsWith(");"));
		assertThat(result, containsString("\"withView1\":\"with\""));
		assertThat(result, not(containsString("\"withView2\":\"with\"")));
		assertThat(result, not(containsString("\"withoutView\":\"without\"")));
	}

	@Test  // SPR-13318
	public void writeSubType() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MyBean bean = new MyBean();
		bean.setString("Foo");
		bean.setNumber(42);

		this.converter.writeInternal(bean, MyInterface.class, outputMessage);

		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertTrue(result.contains("\"string\":\"Foo\""));
		assertTrue(result.contains("\"number\":42"));
	}

	@Test  // SPR-13318
	public void writeSubTypeList() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		List<MyBean> beans = new ArrayList<>();
		MyBean foo = new MyBean();
		foo.setString("Foo");
		foo.setNumber(42);
		beans.add(foo);
		MyBean bar = new MyBean();
		bar.setString("Bar");
		bar.setNumber(123);
		beans.add(bar);
		ParameterizedTypeReference<List<MyInterface>> typeReference =
				new ParameterizedTypeReference<List<MyInterface>>() {};

		this.converter.writeInternal(beans, typeReference.getType(), outputMessage);

		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertTrue(result.contains("\"string\":\"Foo\""));
		assertTrue(result.contains("\"number\":42"));
		assertTrue(result.contains("\"string\":\"Bar\""));
		assertTrue(result.contains("\"number\":123"));
	}


	interface MyInterface {

		String getString();

		void setString(String string);
	}


	public static class MyBean implements MyInterface {

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


	public static class PrettyPrintBean {

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
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


	@JsonFilter("myJacksonFilter")
	@SuppressWarnings("unused")
	private static class JacksonFilteredBean {

		private String property1;

		private String property2;

		public String getProperty1() {
			return property1;
		}

		public void setProperty1(String property1) {
			this.property1 = property1;
		}

		public String getProperty2() {
			return property2;
		}

		public void setProperty2(String property2) {
			this.property2 = property2;
		}
	}

}
