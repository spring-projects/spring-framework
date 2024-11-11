/*
 * Copyright 2002-2024 the original author or authors.
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

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.web.testfixture.http.MockHttpInputMessage;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.within;

/**
 * Jackson 2.x converter tests.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 */
class MappingJackson2HttpMessageConverterTests {

	private final MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();


	@Test
	void canRead() {
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "json"))).isTrue();
		assertThat(converter.canRead(Map.class, new MediaType("application", "json"))).isTrue();
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "json", StandardCharsets.UTF_8))).isTrue();
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "json", StandardCharsets.US_ASCII))).isTrue();
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "json", StandardCharsets.ISO_8859_1))).isTrue();
	}

	@Test
	void canReadWithObjectMapperRegistrationForType() {
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
	void canWrite() {
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "json"))).isTrue();
		assertThat(converter.canWrite(Map.class, new MediaType("application", "json"))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "json", StandardCharsets.UTF_8))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "json", StandardCharsets.US_ASCII))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "json", StandardCharsets.ISO_8859_1))).isFalse();
	}

	@Test  // SPR-7905
	void canReadAndWriteMicroformats() {
		assertThat(converter.canRead(MyBean.class, new MediaType("application", "vnd.test-micro-type+json"))).isTrue();
		assertThat(converter.canWrite(MyBean.class, new MediaType("application", "vnd.test-micro-type+json"))).isTrue();
	}

	@Test
	void getSupportedMediaTypes() {
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
	void readTyped() throws IOException {
		String body = "{" +
				"\"bytes\":\"AQI=\"," +
				"\"array\":[\"Foo\",\"Bar\"]," +
				"\"number\":42," +
				"\"string\":\"Foo\"," +
				"\"bool\":true," +
				"\"fraction\":42.0}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		MyBean result = (MyBean) converter.read(MyBean.class, inputMessage);
		assertThat(result.getString()).isEqualTo("Foo");
		assertThat(result.getNumber()).isEqualTo(42);
		assertThat(result.getFraction()).isCloseTo(42F, within(0F));
		assertThat(result.getArray()).isEqualTo(new String[] {"Foo", "Bar"});
		assertThat(result.isBool()).isTrue();
		assertThat(result.getBytes()).isEqualTo(new byte[] {0x1, 0x2});
	}

	@Test
	@SuppressWarnings("unchecked")
	void readUntyped() throws IOException {
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
		assertThat(result).containsEntry("string", "Foo");
		assertThat(result).containsEntry("number", 42);
		assertThat((Double) result.get("fraction")).isCloseTo(42D, within(0D));
		List<String> array = new ArrayList<>();
		array.add("Foo");
		array.add("Bar");
		assertThat(result).containsEntry("array", array);
		assertThat(result).containsEntry("bool", Boolean.TRUE);
		assertThat(result).containsEntry("bytes", "AQI=");
	}

	@Test
	void write() throws IOException {
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
		assertThat(result).contains("\"string\":\"Foo\"");
		assertThat(result).contains("\"number\":42");
		assertThat(result).contains("fraction\":42.0");
		assertThat(result).contains("\"array\":[\"Foo\",\"Bar\"]");
		assertThat(result).contains("\"bool\":true");
		assertThat(result).contains("\"bytes\":\"AQI=\"");
		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type").isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	void writeWithBaseType() throws IOException {
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
		assertThat(result).contains("\"string\":\"Foo\"");
		assertThat(result).contains("\"number\":42");
		assertThat(result).contains("fraction\":42.0");
		assertThat(result).contains("\"array\":[\"Foo\",\"Bar\"]");
		assertThat(result).contains("\"bool\":true");
		assertThat(result).contains("\"bytes\":\"AQI=\"");
		assertThat(outputMessage.getHeaders().getContentType())
				.as("Invalid content-type").isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	void writeUTF16() throws IOException {
		MediaType contentType = new MediaType("application", "json", StandardCharsets.UTF_16BE);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		String body = "H\u00e9llo W\u00f6rld";
		converter.write(body, contentType, outputMessage);
		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_16BE)).as("Invalid result").isEqualTo(("\"" + body + "\""));
		assertThat(outputMessage.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(contentType);
	}

	@Test
	void readInvalidJson() {
		String body = "FooBar";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		assertThatExceptionOfType(HttpMessageNotReadableException.class)
				.isThrownBy(() -> converter.read(MyBean.class, inputMessage));
	}

	@Test
	void readValidJsonWithUnknownProperty() throws IOException {
		String body = "{\"string\":\"string\",\"unknownProperty\":\"value\"}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json"));
		converter.read(MyBean.class, inputMessage);
		// Assert no HttpMessageNotReadableException is thrown
	}

	@Test
	@SuppressWarnings("unchecked")
	void readAndWriteGenerics() throws Exception {
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
		assertThat(results).hasSize(1);
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
	void readAndWriteParameterizedType() throws Exception {
		ParameterizedTypeReference<List<MyBean>> beansList = new ParameterizedTypeReference<>() {};

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
		assertThat(results).hasSize(1);
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
	void writeParameterizedBaseType() throws Exception {
		ParameterizedTypeReference<List<MyBean>> beansList = new ParameterizedTypeReference<>() {};
		ParameterizedTypeReference<List<MyBase>> baseList = new ParameterizedTypeReference<>() {};

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
		assertThat(results).hasSize(1);
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

	// gh-24498
	@Test
	void writeOptional() throws IOException {
		ParameterizedTypeReference<Optional<MyParent>> optionalParent = new ParameterizedTypeReference<>() {};
		Optional<MyParent> result = Optional.of(new Impl1());
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(result, optionalParent.getType(), MediaType.APPLICATION_JSON, outputMessage);

		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8))
				.contains("@type");
	}

	@Test
	void prettyPrint() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		PrettyPrintBean bean = new PrettyPrintBean();
		bean.setName("Jason");

		this.converter.setPrettyPrint(true);
		this.converter.writeInternal(bean, null, outputMessage);
		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);

		assertThat(result).isEqualToNormalizingNewlines("""
			{
			\s "name" : "Jason"
			}""");
	}

	@Test
	void prettyPrintWithSse() throws Exception {
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
	void prefixJson() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.setPrefixJson(true);
		this.converter.writeInternal("foo", null, outputMessage);

		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(")]}', \"foo\"");
	}

	@Test
	void prefixJsonCustom() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		this.converter.setJsonPrefix(")))");
		this.converter.writeInternal("foo", null, outputMessage);

		assertThat(outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(")))\"foo\"");
	}

	@Test
	void fieldLevelJsonView() throws Exception {
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
	void classLevelJsonView() throws Exception {
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
	void filters() throws Exception {
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
	void writeSubType() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MyBean bean = new MyBean();
		bean.setString("Foo");
		bean.setNumber(42);

		this.converter.writeInternal(bean, MyInterface.class, outputMessage);

		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result).contains("\"string\":\"Foo\"");
		assertThat(result).contains("\"number\":42");
	}

	@Test  // SPR-13318
	void writeSubTypeList() throws Exception {
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
				new ParameterizedTypeReference<>() {};

		this.converter.writeInternal(beans, typeReference.getType(), outputMessage);

		String result = outputMessage.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result).contains("\"string\":\"Foo\"");
		assertThat(result).contains("\"number\":42");
		assertThat(result).contains("\"string\":\"Bar\"");
		assertThat(result).contains("\"number\":123");
	}

	@Test // gh-27511
	void readWithNoDefaultConstructor() throws Exception {
		String body = "{\"property1\":\"foo\",\"property2\":\"bar\"}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		BeanWithNoDefaultConstructor bean =
				(BeanWithNoDefaultConstructor)converter.read(BeanWithNoDefaultConstructor.class, inputMessage);
		assertThat(bean.property1).isEqualTo("foo");
		assertThat(bean.property2).isEqualTo("bar");
	}

	@Test
	@SuppressWarnings("unchecked")
	void readNonUnicode() throws Exception {
		String body = "{\"føø\":\"bår\"}";
		Charset charset = StandardCharsets.ISO_8859_1;
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(charset));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json", charset));
		HashMap<String, Object> result = (HashMap<String, Object>) this.converter.read(HashMap.class, inputMessage);

		assertThat(result).containsExactly(entry("føø", "bår"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void readAscii() throws Exception {
		String body = "{\"foo\":\"bar\"}";
		Charset charset = StandardCharsets.US_ASCII;
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(charset));
		inputMessage.getHeaders().setContentType(new MediaType("application", "json", charset));
		HashMap<String, Object> result = (HashMap<String, Object>) this.converter.read(HashMap.class, inputMessage);

		assertThat(result).containsExactly(entry("foo", "bar"));
	}

	@Test
	void writeAscii() throws Exception {
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

	@Test
	void readWithCustomized() throws IOException {
		MappingJackson2HttpMessageConverterWithCustomization customizedConverter =
				new MappingJackson2HttpMessageConverterWithCustomization();
		String body = "{\"property\":\"Value1\"}";
		MockHttpInputMessage inputMessage1 = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		MockHttpInputMessage inputMessage2 = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage1.getHeaders().setContentType(new MediaType("application", "json"));
		inputMessage2.getHeaders().setContentType(new MediaType("application", "json"));

		assertThatExceptionOfType(HttpMessageNotReadableException.class)
				.isThrownBy(() -> converter.read(MyCustomizedBean.class, inputMessage1));

		MyCustomizedBean customizedResult = (MyCustomizedBean) customizedConverter.read(MyCustomizedBean.class, inputMessage2);
		assertThat(customizedResult.getProperty()).isEqualTo(MyCustomEnum.VAL1);
	}

	@Test
	void writeWithCustomized() throws IOException {
		MappingJackson2HttpMessageConverterWithCustomization customizedConverter =
				new MappingJackson2HttpMessageConverterWithCustomization();
		MockHttpOutputMessage outputMessage1 = new MockHttpOutputMessage();
		MockHttpOutputMessage outputMessage2 = new MockHttpOutputMessage();
		MyCustomizedBean body = new MyCustomizedBean();
		body.setProperty(MyCustomEnum.VAL2);
		converter.write(body, null, outputMessage1);
		customizedConverter.write(body, null, outputMessage2);
		String result1 = outputMessage1.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result1).contains("\"property\":\"VAL2\"");
		String result2 = outputMessage2.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result2).contains("\"property\":\"Value2\"");
	}

	@Test
	void repeatableWrites() throws IOException {
		MockHttpOutputMessage outputMessage1 = new MockHttpOutputMessage();
		MyBean body = new MyBean();
		body.setString("Foo");
		converter.write(body, null, outputMessage1);
		String result = outputMessage1.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result).contains("\"string\":\"Foo\"");

		MockHttpOutputMessage outputMessage2 = new MockHttpOutputMessage();
		converter.write(body, null, outputMessage2);
		result = outputMessage2.getBodyAsString(StandardCharsets.UTF_8);
		assertThat(result).contains("\"string\":\"Foo\"");
	}



	interface MyInterface {

		String getString();

		void setString(String string);
	}


	public static class MyBase implements MyInterface {

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

	public static class MyCustomizedBean {

		private MyCustomEnum property;

		public MyCustomEnum getProperty() {
			return property;
		}

		public void setProperty(MyCustomEnum property) {
			this.property = property;
		}
	}

	public enum MyCustomEnum {
		VAL1,
		VAL2;

		@Override
		public String toString() {
			return this == VAL1 ? "Value1" : "Value2";
		}
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
	@JsonSubTypes(value = {@JsonSubTypes.Type(value = Impl1.class),
			@JsonSubTypes.Type(value = Impl2.class)})
	public interface MyParent {
	}

	public static class Impl1 implements MyParent {
	}

	public static class Impl2 implements MyParent {
	}

	private static class MappingJackson2HttpMessageConverterWithCustomization extends MappingJackson2HttpMessageConverter {

		@Override
		protected ObjectReader customizeReader(ObjectReader reader, JavaType javaType) {
			return reader.with(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		}

		@Override
		protected ObjectWriter customizeWriter(ObjectWriter writer, JavaType javaType, MediaType contentType) {
			return writer.with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		}
	}

}
