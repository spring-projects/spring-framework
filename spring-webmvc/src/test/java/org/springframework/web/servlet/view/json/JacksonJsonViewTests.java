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

package org.springframework.web.servlet.view.json;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.SerializerFactoryConfig;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.BeanSerializerFactory;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.SerializerFactory;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.http.MediaType;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JacksonJsonView}.
 *
 * @author Sebastien Deleuze
 */
class JacksonJsonViewTests {

	private JacksonJsonView view = new JacksonJsonView();

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private Context jsContext = ContextFactory.getGlobal().enterContext();

	private ScriptableObject jsScope = jsContext.initStandardObjects();


	@Test
	void isExposePathVars() {
		assertThat(view.isExposePathVariables()).as("Must not expose path variables").isFalse();
	}

	@Test
	void renderSimpleMap() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("bindingResult", mock(BindingResult.class, "binding_result"));
		model.put("foo", "bar");

		view.setUpdateContentLength(true);
		view.render(model, request, response);

		assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");

		MediaType mediaType = MediaType.parseMediaType(response.getContentType());
		assertThat(mediaType.isCompatibleWith(MediaType.parseMediaType(JacksonJsonView.DEFAULT_CONTENT_TYPE))).isTrue();

		String jsonResult = response.getContentAsString();
		assertThat(jsonResult).isNotEmpty();
		assertThat(response.getContentLength()).isEqualTo(jsonResult.length());

		validateResult();
	}

	@Test
	void renderWithSelectedContentType() throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("foo", "bar");

		view.render(model, request, response);
		MediaType mediaType = MediaType.parseMediaType(response.getContentType());
		assertThat(mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();

		request.setAttribute(View.SELECTED_CONTENT_TYPE, new MediaType("application", "vnd.example-v2+xml"));
		view.render(model, request, response);

		mediaType = MediaType.parseMediaType(response.getContentType());
		assertThat(mediaType.isCompatibleWith(MediaType.parseMediaType("application/vnd.example-v2+xml"))).isTrue();
	}

	@Test
	void renderCaching() throws Exception {
		view.setDisableCaching(false);

		Map<String, Object> model = new HashMap<>();
		model.put("bindingResult", mock(BindingResult.class, "binding_result"));
		model.put("foo", "bar");

		view.render(model, request, response);

		assertThat(response.getHeader("Cache-Control")).isNull();
	}

	@Test
	void renderSimpleMapPrefixed() throws Exception {
		view.setPrefixJson(true);
		renderSimpleMap();
	}

	@Test
	void renderSimpleBean() throws Exception {
		Object bean = new TestBeanSimple();
		Map<String, Object> model = new HashMap<>();
		model.put("bindingResult", mock(BindingResult.class, "binding_result"));
		model.put("foo", bean);

		view.setUpdateContentLength(true);
		view.render(model, request, response);

		assertThat(response.getContentAsString()).isNotEmpty();
		assertThat(response.getContentLength()).isEqualTo(response.getContentAsString().length());

		validateResult();
	}

	@Test
	void renderWithPrettyPrint() throws Exception {
		ModelMap model = new ModelMap("foo", new TestBeanSimple());

		view = new JacksonJsonView(JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build());
		view.render(model, request, response);

		String result = response.getContentAsString().replace("\r\n", "\n");
		assertThat(result).as("Pretty printing not applied:\n" + result).startsWith("{\n  \"foo\" : {\n    ");

		validateResult();
	}

	@Test
	void renderSimpleBeanPrefixed() throws Exception {
		view.setPrefixJson(true);
		renderSimpleBean();
		assertThat(response.getContentAsString()).startsWith(")]}', ");
	}

	@Test
	void renderSimpleBeanNotPrefixed() throws Exception {
		view.setPrefixJson(false);
		renderSimpleBean();
		assertThat(response.getContentAsString()).doesNotStartWith(")]}', ");
	}

	@Test
	void renderWithCustomSerializerLocatedByFactory() throws Exception {
		SerializerFactory factory = new DelegatingSerializerFactory(null);
		JsonMapper mapper = JsonMapper.builder().serializerFactory(factory).build();
		view = new JacksonJsonView(mapper);

		Object bean = new TestBeanSimple();
		Map<String, Object> model = new HashMap<>();
		model.put("foo", bean);
		model.put("bar", new TestChildBean());

		view.render(model, request, response);

		String result = response.getContentAsString();
		assertThat(result).isNotEmpty();
		assertThat(result).contains("\"foo\":{\"testBeanSimple\":\"custom\"}");

		validateResult();
	}

	@Test
	void renderOnlyIncludedAttributes() throws Exception {

		Set<String> attrs = new HashSet<>();
		attrs.add("foo");
		attrs.add("baz");
		attrs.add("nil");

		view.setModelKeys(attrs);
		Map<String, Object> model = new HashMap<>();
		model.put("foo", "foo");
		model.put("bar", "bar");
		model.put("baz", "baz");

		view.render(model, request, response);

		String result = response.getContentAsString();
		assertThat(result).isNotEmpty();
		assertThat(result).contains("\"foo\":\"foo\"");
		assertThat(result).contains("\"baz\":\"baz\"");

		validateResult();
	}

	@Test
	void filterSingleKeyModel() {
		view.setExtractValueFromSingleKeyModel(true);

		Map<String, Object> model = new HashMap<>();
		TestBeanSimple bean = new TestBeanSimple();
		model.put("foo", bean);

		Object actual = view.filterModel(model, request);

		assertThat(actual).isSameAs(bean);
	}

	@SuppressWarnings("rawtypes")
	@Test
	void filterTwoKeyModel() {
		view.setExtractValueFromSingleKeyModel(true);

		Map<String, Object> model = new HashMap<>();
		TestBeanSimple bean1 = new TestBeanSimple();
		TestBeanSimple bean2 = new TestBeanSimple();
		model.put("foo1", bean1);
		model.put("foo2", bean2);

		Object actual = view.filterModel(model, request);

		assertThat(actual).isInstanceOf(Map.class);
		assertThat(((Map) actual).get("foo1")).isSameAs(bean1);
		assertThat(((Map) actual).get("foo2")).isSameAs(bean2);
	}

	@Test
	void renderSimpleBeanWithJsonView() throws Exception {
		Object bean = new TestBeanSimple();
		Map<String, Object> model = new HashMap<>();
		model.put("bindingResult", mock(BindingResult.class, "binding_result"));
		model.put("foo", bean);
		model.put(JsonView.class.getName(), MyJacksonView1.class);

		view.setUpdateContentLength(true);
		view.render(model, request, response);

		String content = response.getContentAsString();
		assertThat(content).isNotEmpty();
		assertThat(response.getContentLength()).isEqualTo(content.length());
		assertThat(content).contains("foo");
		assertThat(content).doesNotContain("boo");
		assertThat(content).doesNotContain(JsonView.class.getName());
	}

	@Test
	void renderSimpleBeanWithFilters() throws Exception {
		TestSimpleBeanFiltered bean = new TestSimpleBeanFiltered();
		bean.setProperty1("value");
		bean.setProperty2("value");
		Map<String, Object> model = new HashMap<>();
		model.put("bindingResult", mock(BindingResult.class, "binding_result"));
		model.put("foo", bean);
		FilterProvider filters = new SimpleFilterProvider().addFilter("myJacksonFilter",
				SimpleBeanPropertyFilter.serializeAllExcept("property2"));
		model.put(FilterProvider.class.getName(), filters);

		view.setUpdateContentLength(true);
		view.render(model, request, response);

		String content = response.getContentAsString();
		assertThat(content).isNotEmpty();
		assertThat(response.getContentLength()).isEqualTo(content.length());
		assertThat(content).contains("\"property1\":\"value\"");
		assertThat(content).doesNotContain("\"property2\":\"value\"");
		assertThat(content).doesNotContain(FilterProvider.class.getName());
	}

	private void validateResult() throws Exception {
		String json = response.getContentAsString();
		DirectFieldAccessor viewAccessor = new DirectFieldAccessor(view);
		String jsonPrefix = (String)viewAccessor.getPropertyValue("jsonPrefix");
		if (jsonPrefix != null) {
			json = json.substring(5);
		}
		Object jsResult = jsContext.evaluateString(jsScope, "(" + json + ")", "JSON Stream", 1, null);
		assertThat(jsResult).as("Json Result did not eval as valid JavaScript").isNotNull();
		MediaType mediaType = MediaType.parseMediaType(response.getContentType());
		assertThat(mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
	}


	public interface MyJacksonView1 {
	}


	public interface MyJacksonView2 {
	}


	@SuppressWarnings("unused")
	public static class TestBeanSimple {

		@JsonView(MyJacksonView1.class)
		private String property1 = "foo";

		private boolean test = false;

		@JsonView(MyJacksonView2.class)
		private String property2 = "boo";

		private TestChildBean child = new TestChildBean();

		public String getProperty1() {
			return property1;
		}

		public boolean getTest() {
			return test;
		}

		public String getProperty2() {
			return property2;
		}

		public Date getNow() {
			return new Date();
		}

		public TestChildBean getChild() {
			return child;
		}
	}


	public static class TestChildBean {

		private String value = "bar";

		private String baz = null;

		private TestBeanSimple parent = null;

		public String getValue() {
			return value;
		}

		public String getBaz() {
			return baz;
		}

		public TestBeanSimple getParent() {
			return parent;
		}

		public void setParent(TestBeanSimple parent) {
			this.parent = parent;
		}
	}


	public static class TestBeanSimpleSerializer extends ValueSerializer<Object> {

		@Override
		public void serialize(Object value, JsonGenerator jgen, SerializationContext ctxt) throws JacksonException {
			jgen.writeStartObject();
			jgen.writeStringProperty("testBeanSimple", "custom");
			jgen.writeEndObject();
		}
	}


	@JsonFilter("myJacksonFilter")
	@SuppressWarnings("unused")
	private static class TestSimpleBeanFiltered {

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


	@SuppressWarnings("serial")
	public static class DelegatingSerializerFactory extends BeanSerializerFactory {

		protected DelegatingSerializerFactory(SerializerFactoryConfig config) {
			super(config);
		}

		@Override
		public ValueSerializer<Object> createSerializer(SerializationContext ctxt, JavaType origType,
				BeanDescription.Supplier beanDescRef, JsonFormat.Value formatOverrides) {
			if (origType.getRawClass() == TestBeanSimple.class) {
				return new TestBeanSimpleSerializer();
			}
			else {
				return super.createSerializer(ctxt, origType, beanDescRef, formatOverrides);
			}
		}

		@Override
		public SerializerFactory withConfig(SerializerFactoryConfig config) {
			return this;
		}
	}

}
