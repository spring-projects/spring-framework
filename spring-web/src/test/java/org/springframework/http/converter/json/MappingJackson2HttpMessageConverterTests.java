/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.converter.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
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
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Jackson 2.x converter tests.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 */
public class MappingJackson2HttpMessageConverterTests {

	protected static final String NEWLINE_SYSTEM_PROPERTY = System.getProperty("line.separator");

	private final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();


	@Test
	public void canRead() {
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "json"))).isTrue();
		assertThat(converter.canRead(Map.class, new MediaType("application", "json"))).isTrue();
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "json", StandardCharsets.UTF_8))).isTrue();
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "json", StandardCharsets.US_ASCII))).isTrue();
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "json", StandardCharsets.ISO_8859_1))).isTrue();
	}

	@Test
	public void canReadWithObjectMapperRegistrationForType() {
		MediaType halJsonMediaType = MediaType.parseMediaType("application/hal+json");
		MediaType halFormsJsonMediaType = MediaType.parseMediaType("application/prs.hal-forms+json");

		assertThat(converter.canRead(MyBean.class, halJsonMediaType)).isTrue();
		assertThat(converter.canRead(MyBean.class, MediaType.APPLICATION_JSON)).isTrue();
		assertThat(converter.canRead(MyBean.class, halFormsJsonMediaType)).isTrue();
		assertThat(converter.canRead(Map.class, MediaType.APPLICATION_JSON)).isTrue();

		converter.registerObjectMappersForType(MyBean.class, map -> {
			map.put(halJsonMediaType, new ObjectMapper());
			map.put(MediaType.APPLICATION_JSON, new ObjectMapper());
		});

		assertThat(converter.canRead(MyBean.class, halJsonMediaType)).isTrue();
		assertThat(converter.canRead(MyBean.class, MediaType.APPLICATION_JSON)).isTrue();
		assertThat(converter.canRead(MyBean.class, halFormsJsonMediaType)).isFalse();
		assertThat(converter.canRead(Map.class, MediaType.APPLICATION_JSON)).isTrue();
	}

	@Test
	public void canWrite() {
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "json"))).isTrue();
		assertThat(converter.canWrite(Map.class, new MediaType("application", "json"))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "json", StandardCharsets.UTF_8))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "json", StandardCharsets.US_ASCII))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "json", StandardCharsets.ISO_8859_1))).isFalse();
	}

	@Test  // SPR-7905
	public void canReadAndWriteMicroformats() {
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "vnd.test-micro-type+json"))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "vnd.test-micro-type+json"))).isTrue();
	}

	@Test
	public void getSupportedMediaTypes() {
		MediaType[] defaultMediaTypes = {MediaType.APPLICATION_JSON, MediaType.parseMediaType("application/*+json")};
		assertThat(converter.getSupportedMediaTypes()).containsExactly(defaultMediaTypes);
		assertThat(converter.getSupportedMediaTypes(MyBean.class)).containsExactly(defaultMediaTypes);

		MediaType halJson = MediaType.parseMediaType("application/hal+json");
		converter.registerObjectMappersForType(MyBean.class, map -> {
			map.put(halJson, new ObjectMapper());
			map.put(MediaType.APPLICATION_JSON, new ObjectMapper());
		});

		assertThat(converter.getSupportedMediaTypes(MyBean.class)).containsExactly(halJson, MediaType.APPLICATION_JSON);
		assertThat(converter.getSupportedMediaTypes(Map.class)).containsExactly(defaultMediaTypes);
	}

	@Test
	public void readTyped() throws IOException {
		String body = "{" +
				"\"bytes\":\"AQI=\"," +
				"\"array\":[\"Foo\",\"Bar\"]," +
				"\"number\":42," +
				"\"string\":\"Foo\"," +
				"\"bool\":true," +
				"\"fraction\":42.0}";
		InputStream inputStream = spy(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(inputStream);
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		MyBean result = (MyBean) converter.read(MyBean.class, inputMessage);
		assertThat(result.getString()).isEqualTo("Foo");
		assertThat(result.getNumber()).isEqualTo(42);
		assertThat(result.getFraction()).isCloseTo(42F, within(0F));
		assertThat(result.getArray()).isEqualTo(new String[] {"Foo", "Bar"});
		assertThat(result.isBool()).isTrue();
		assertThat(result.getBytes()).isEqualTo(new byte[] {0x1, 0x2});
		verify(inputStream, never()).close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readUntyped() throws IOException {
		String body = "{" +
				"\"bytes\":\"AQI=\"," +
				"\"array\":[\"Foo\",\"Bar\"]," +
				"\"number\":42," +
				"\"string\":\"Foo\"," +
				"\"bool\":true," +
				"\"fraction\":42.0}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		HashMap<String, Object> result = (HashMap<String, Object>) converter.read(HashMap.class, inputMessage);
		assertThat(result.get("string")).isEqualTo("Foo");
		assertThat(result.get("number")).isEqualTo(42);
		assertThat((Double) result.get("fraction")).isCloseTo(42D, within(0D));
		List<String> array = new ArrayList<>();
		array.add("Foo");
		array.add("Bar");
		assertThat(result.get("array")).isEqualTo(array);
		assertThat(result.get("bool")).isEqualTo(Boolean.TRUE);
		assertThat(result.get("bytes")).isEqualTo("AQI=");
	}

	@Test
	public void write() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MyBean body = new MyBean();
		body.setString("Foo");
		body.setNumber(42);
		body.setFraction(42F);
		body.setArray(new String[] {"Foo", "Bar"});
		body.setBool(true);
		body.setBytes(new byte[] {0x1, 0x2});
		converter.write(body, null, outputMessage);
		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result.contains("\"string\":\"Foo\"")).isTrue();
		assertThat(result.contains("\"number\":42")).isTrue();
		assertThat(result.contains("fraction\":42.0")).isTrue();
		assertThat(result.contains("\"array\":[\"Foo\",\"Bar\"]")).isTrue();
		assertThat(result.contains("\"bool\":true")).isTrue();
		assertThat(result.contains("\"bytes\":\"AQI=\"")).isTrue();
		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type").isEqualTo(MediaType.APPLICATION_JSON);
		verify(outputMessage.getBody(), never()).close();
	}

	@Test
	public void writeWithBaseType() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MyBean body = new MyBean();
		body.setString("Foo");
		body.setNumber(42);
		body.setFraction(42F);
		body.setArray(new String[] {"Foo", "Bar"});
		body.setBool(true);
		body.setBytes(new byte[] {0x1, 0x2});
		converter.write(body, MyBase.class, null, outputMessage);
		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result.contains("\"string\":\"Foo\"")).isTrue();
		assertThat(result.contains("\"number\":42")).isTrue();
		assertThat(result.contains("fraction\":42.0")).isTrue();
		assertThat(result.contains("\"array\":[\"Foo\",\"Bar\"]")).isTrue();
		assertThat(result.contains("\"bool\":true")).isTrue();
		assertThat(result.contains("\"bytes\":\"AQI=\"")).isTrue();
		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type").isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	public void writeUTF16() throws IOException {
		MediaType contentType = new MediaType("application", "json", StandardCharsets.UTF_16BE);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		String body = "H\u00e9llo W\u00f6rld";
		converter.write(body, contentType, outputMessage);
		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_16BE)).as("Invalid result").isEqualTo(("\"" + body + "\""));
		assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(contentType);
	}

	@Test
	public void readInvalidJson() throws IOException {
		String body = "FooBar";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		assertThatExceptionOfType(HttpMessageNotReadableException.class)
				.isThrownBy(() -> converter.read(MyBean.class, inputMessage));
	}

	@Test
	public void readValidJsonWithUnknownProperty() throws IOException {
		String body = "{\"string\":\"string\",\"unknownProperty\":\"value\"}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		converter.read(MyBean.class, inputMessage);
		// Assert no HttpMessageNotReadableException is thrown
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readAndWriteGenerics() throws Exception {
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter() {
			@Override
			protected JavaType getJavaType(Type type, @Nullable Class<?> contextClass) {
				if (type instanceof Class && List.class.isAssignableFrom((Class<?>)type)) {
					return new ObjectMapper().getTypeFactory().constructCollectionType(ArrayList.class, MyBean.class);
				}
				else {
					return super.getJavaType(type, contextClass);
				}
			}
		};
		String body = "[{" +
				"\"bytes\":\"AQI=\"," +
				"\"array\":[\"Foo\",\"Bar\"]," +
				"\"number\":42," +
				"\"string\":\"Foo\"," +
				"\"bool\":true," +
				"\"fraction\":42.0}]";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));

		List<MyBean> results = (List<MyBean>) converter.read(List.class, inputMessage);
		assertThat(results.size()).isEqualTo(1);
		MyBean result = results.get(0);
		assertThat(result.getString()).isEqualTo("Foo");
		assertThat(result.getNumber()).isEqualTo(42);
		assertThat(result.getFraction()).isCloseTo(42F, within(0F));
		assertThat(result.getArray()).isEqualTo(new String[] {"Foo", "Bar"});
		assertThat(result.isBool()).isTrue();
		assertThat(result.getBytes()).isEqualTo(new byte[] {0x1, 0x2});

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(results, MediaType.APPLICATION_JSON, outputMessage);
		JSONAssert.assertEquals(body, outputMessage.getBodyAsString(StandardCharsets.UTF_8), true);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readAndWriteParameterizedType() throws Exception {
		ParameterizedTypeReference<List<MyBean>> beansList = new ParameterizedTypeReference<List<MyBean>>() {};

		String body = "[{" +
				"\"bytes\":\"AQI=\"," +
				"\"array\":[\"Foo\",\"Bar\"]," +
				"\"number\":42," +
				"\"string\":\"Foo\"," +
				"\"bool\":true," +
				"\"fraction\":42.0}]";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		List<MyBean> results = (List<MyBean>) converter.read(beansList.getType(), null, inputMessage);
		assertThat(results.size()).isEqualTo(1);
		MyBean result = results.get(0);
		assertThat(result.getString()).isEqualTo("Foo");
		assertThat(result.getNumber()).isEqualTo(42);
		assertThat(result.getFraction()).isCloseTo(42F, within(0F));
		assertThat(result.getArray()).isEqualTo(new String[] {"Foo", "Bar"});
		assertThat(result.isBool()).isTrue();
		assertThat(result.getBytes()).isEqualTo(new byte[] {0x1, 0x2});

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(results, beansList.getType(), MediaType.APPLICATION_JSON, outputMessage);
		JSONAssert.assertEquals(body, outputMessage.getBodyAsString(StandardCharsets.UTF_8), true);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void writeParameterizedBaseType() throws Exception {
		ParameterizedTypeReference<List<MyBean>> beansList = new ParameterizedTypeReference<List<MyBean>>() {};
		ParameterizedTypeReference<List<MyBase>> baseList = new ParameterizedTypeReference<List<MyBase>>() {};

		String body = "[{" +
				"\"bytes\":\"AQI=\"," +
				"\"array\":[\"Foo\",\"Bar\"]," +
				"\"number\":42," +
				"\"string\":\"Foo\"," +
				"\"bool\":true," +
				"\"fraction\":42.0}]";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		List<MyBean> results = (List<MyBean>) converter.read(beansList.getType(), null, inputMessage);
		assertThat(results.size()).isEqualTo(1);
		MyBean result = results.get(0);
		assertThat(result.getString()).isEqualTo("Foo");
		assertThat(result.getNumber()).isEqualTo(42);
		assertThat(result.getFraction()).isCloseTo(42F, within(0F));
		assertThat(result.getArray()).isEqualTo(new String[] {"Foo", "Bar"});
		assertThat(result.isBool()).isTrue();
		assertThat(result.getBytes()).isEqualTo(new byte[] {0x1, 0x2});

		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(results, baseList.getType(), MediaType.APPLICATION_JSON, outputMessage);
		JSONAssert.assertEquals(body, outputMessage.getBodyAsString(StandardCharsets.UTF_8), true);
	}

	@Test
	public void prettyPrint() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		PrettyPrintBean bean = new PrettyPrintBean();
		bean.setName("Jason");

		this.converter.setPrettyPrint(true);
		this.converter.writeInternal(bean, null, outputMessage);
		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);

		assertThat(result).isEqualTo(("{" + NEWLINE_SYSTEM_PROPERTY +
				"  \"name\" : \"Jason\"" + NEWLINE_SYSTEM_PROPERTY + "}"));
	}

	@Test
	public void prettyPrintWithSse() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		outputMessage.getHeaders().setContentType(MediaType.TEXT_EVENT_STREAM);
		PrettyPrintBean bean = new PrettyPrintBean();
		bean.setName("Jason");

		this.converter.setPrettyPrint(true);
		this.converter.writeInternal(bean, null, outputMessage);
		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);

		assertThat(result).isEqualTo("{\ndata:  \"name\" : \"Jason\"\ndata:}");
	}

	@Test
	public void prefixJson() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.setPrefixJson(true);
		this.converter.writeInternal("foo", null, outputMessage);

		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(")]}', \"foo\"");
	}

	@Test
	public void prefixJsonCustom() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.setJsonPrefix(")))");
		this.converter.writeInternal("foo", null, outputMessage);

		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(")))\"foo\"");
	}

	@Test
	public void fieldLevelJsonView() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		JacksonViewBean bean = new JacksonViewBean();
		bean.setWithView1("with");
		bean.setWithView2("with");
		bean.setWithoutView("without");

		MappingJacksonValue jacksonValue = new MappingJacksonValue(bean);
		jacksonValue.setSerializationView(MyJacksonView1.class);
		this.converter.writeInternal(jacksonValue, null, outputMessage);

		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result).contains("\"withView1\":\"with\"");
		assertThat(result).doesNotContain("\"withView2\":\"with\"");
		assertThat(result).doesNotContain("\"withoutView\":\"without\"");
	}

	@Test
	public void classLevelJsonView() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		JacksonViewBean bean = new JacksonViewBean();
		bean.setWithView1("with");
		bean.setWithView2("with");
		bean.setWithoutView("without");

		MappingJacksonValue jacksonValue = new MappingJacksonValue(bean);
		jacksonValue.setSerializationView(MyJacksonView3.class);
		this.converter.writeInternal(jacksonValue, null, outputMessage);

		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result).doesNotContain("\"withView1\":\"with\"");
		assertThat(result).doesNotContain("\"withView2\":\"with\"");
		assertThat(result).contains("\"withoutView\":\"without\"");
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
		assertThat(result).contains("\"property1\":\"value\"");
		assertThat(result).doesNotContain("\"property2\":\"value\"");
	}

	@Test  // SPR-13318
	public void writeSubType() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MyBean bean = new MyBean();
		bean.setString("Foo");
		bean.setNumber(42);

		this.converter.writeInternal(bean, MyInterface.class, outputMessage);

		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result.contains("\"string\":\"Foo\"")).isTrue();
		assertThat(result.contains("\"number\":42")).isTrue();
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
		assertThat(result.contains("\"string\":\"Foo\"")).isTrue();
		assertThat(result.contains("\"number\":42")).isTrue();
		assertThat(result.contains("\"string\":\"Bar\"")).isTrue();
		assertThat(result.contains("\"number\":123")).isTrue();
	}

	@Test
	public void readWithNoDefaultConstructor() throws Exception {
		String body = "{\"property1\":\"foo\",\"property2\":\"bar\"}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		assertThatExceptionOfType(HttpMessageConversionException.class).isThrownBy(() ->
				converter.read(BeanWithNoDefaultConstructor.class, inputMessage))
			.withMessageStartingWith("Type definition error:");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readNonUnicode() throws Exception {
		String body = "{\"føø\":\"bår\"}";
		Charset charset = StandardCharsets.ISO_8859_1;
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(charset));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json", charset));
		HashMap<String, Object> result = (HashMap<String, Object>) this.converter.read(HashMap.class, inputMessage);

		assertThat(result).containsExactly(entry("føø", "bår"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void readAscii() throws Exception {
		String body = "{\"foo\":\"bar\"}";
		Charset charset = StandardCharsets.US_ASCII;
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(charset));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json", charset));
		HashMap<String, Object> result = (HashMap<String, Object>) this.converter.read(HashMap.class, inputMessage);

		assertThat(result).containsExactly(entry("foo", "bar"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void writeAscii() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Map<String,Object> body = new HashMap<>();
		body.put("foo", "bar");
		Charset charset = StandardCharsets.US_ASCII;
		MediaType contentType = new MediaType("application", "json", charset);
		converter.write(body, contentType, outputMessage);

		String result = outputMessage.getBodyAsString(charset);
		assertThat(result).isEqualTo("{\"foo\":\"bar\"}");
		assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(contentType);
	}


	interface MyInterface {

		String getString();

		void setString(String string);
	}


	public static class MyBase implements MyInterface{

		private String string;

		@Override
		public String getString() {
			return string;
		}

		@Override
		public void setString(String string) {
			this.string = string;
		}
	}


	public static class MyBean extends MyBase {

		private int number;

		private float fraction;

		private String[] array;

		private boolean bool;

		private byte[] bytes;

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

		public boolean isBool() {
			return bool;
		}

		public void setBool(boolean bool) {
			this.bool = bool;
		}

		public byte[] getBytes() {
			return bytes;
		}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
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


	private interface MyJacksonView1 {}

	private interface MyJacksonView2 {}

	private interface MyJacksonView3 {}


	@SuppressWarnings("unused")
	@JsonView(MyJacksonView3.class)
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


	@SuppressWarnings("unused")
	private static class BeanWithNoDefaultConstructor {

		private final String property1;

		private final String property2;

		public BeanWithNoDefaultConstructor(String property1, String property2) {
			this.property1 = property1;
			this.property2 = property2;
		}

		public String getProperty1() {
			return property1;
		}

		public String getProperty2() {
			return property2;
		}
	}

}
