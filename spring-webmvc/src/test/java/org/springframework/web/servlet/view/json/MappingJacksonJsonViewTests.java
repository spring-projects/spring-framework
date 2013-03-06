/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.servlet.view.json;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerFactory;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ScriptableObject;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Jeremy Grelle
 * @author Arjen Poutsma
 */
public class MappingJacksonJsonViewTests {

	private MappingJacksonJsonView view;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private Context jsContext;

	private ScriptableObject jsScope;

	@Before
	public void setUp() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();

		jsContext = ContextFactory.getGlobal().enterContext();
		jsScope = jsContext.initStandardObjects();

		view = new MappingJacksonJsonView();
	}

	@Test
	public void isExposePathVars() {
		assertEquals("Must not expose path variables", false, view.isExposePathVariables());
	}

	@Test
	public void renderSimpleMap() throws Exception {

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("bindingResult", mock(BindingResult.class, "binding_result"));
		model.put("foo", "bar");

		view.setUpdateContentLength(true);
		view.render(model, request, response);

		assertEquals("no-cache", response.getHeader("Pragma"));
		assertEquals("no-cache, no-store, max-age=0", response.getHeader("Cache-Control"));
		assertNotNull(response.getHeader("Expires"));

		assertEquals(MappingJacksonJsonView.DEFAULT_CONTENT_TYPE, response.getContentType());

		String jsonResult = response.getContentAsString();
		assertTrue(jsonResult.length() > 0);
		assertEquals(jsonResult.length(), response.getContentLength());

		validateResult();
	}

	@Test
	public void renderCaching() throws Exception {
		view.setDisableCaching(false);

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("bindingResult", mock(BindingResult.class, "binding_result"));
		model.put("foo", "bar");

		view.render(model, request, response);

		assertNull(response.getHeader("Pragma"));
		assertNull(response.getHeader("Cache-Control"));
		assertNull(response.getHeader("Expires"));
	}

	@Test
	public void renderSimpleMapPrefixed() throws Exception {
		view.setPrefixJson(true);
		renderSimpleMap();
	}

	@Test
	public void renderSimpleBean() throws Exception {

		Object bean = new TestBeanSimple();
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("bindingResult", mock(BindingResult.class, "binding_result"));
		model.put("foo", bean);

		view.setUpdateContentLength(true);
		view.render(model, request, response);

		assertTrue(response.getContentAsString().length() > 0);
		assertEquals(response.getContentAsString().length(), response.getContentLength());

		validateResult();
	}

	@Test
	public void renderWithPrettyPrint() throws Exception {

		ModelMap model = new ModelMap("foo", new TestBeanSimple());

		view.setPrettyPrint(true);
		view.render(model, request, response);

		String result = response.getContentAsString().replace("\r\n", "\n");
		assertTrue("Pretty printing not applied:\n" + result, result.startsWith("{\n  \"foo\" : {\n    "));

		validateResult();
	}

	@Test
	public void renderSimpleBeanPrefixed() throws Exception {

		view.setPrefixJson(true);
		renderSimpleBean();
	}

	@Test
	public void renderWithCustomSerializerLocatedByAnnotation() throws Exception {

		Object bean = new TestBeanSimpleAnnotated();
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("foo", bean);

		view.render(model, request, response);

		assertTrue(response.getContentAsString().length() > 0);
		assertEquals("{\"foo\":{\"testBeanSimple\":\"custom\"}}", response.getContentAsString());

		validateResult();
	}

	@Test
	public void renderWithCustomSerializerLocatedByFactory() throws Exception {

		SerializerFactory factory = new DelegatingSerializerFactory();
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializerFactory(factory);
		view.setObjectMapper(mapper);

		Object bean = new TestBeanSimple();
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("foo", bean);
		model.put("bar", new TestChildBean());

		view.render(model, request, response);

		String result = response.getContentAsString();
		assertTrue(result.length() > 0);
		assertTrue(result.contains("\"foo\":{\"testBeanSimple\":\"custom\"}"));

		validateResult();
	}

	@Test
	public void renderOnlyIncludedAttributes() throws Exception {

		Set<String> attrs = new HashSet<String>();
		attrs.add("foo");
		attrs.add("baz");
		attrs.add("nil");

		view.setModelKeys(attrs);
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("foo", "foo");
		model.put("bar", "bar");
		model.put("baz", "baz");

		view.render(model, request, response);

		String result = response.getContentAsString();
		assertTrue(result.length() > 0);
		assertTrue(result.contains("\"foo\":\"foo\""));
		assertTrue(result.contains("\"baz\":\"baz\""));

		validateResult();
	}

	@Test
	public void filterSingleKeyModel() throws Exception {
		view.setExtractValueFromSingleKeyModel(true);

		Map<String, Object> model = new HashMap<String, Object>();
		TestBeanSimple bean = new TestBeanSimple();
		model.put("foo", bean);

		Object actual = view.filterModel(model);

		assertSame(bean, actual);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void filterTwoKeyModel() throws Exception {
		view.setExtractValueFromSingleKeyModel(true);

		Map<String, Object> model = new HashMap<String, Object>();
		TestBeanSimple bean1 = new TestBeanSimple();
		TestBeanSimple bean2 = new TestBeanSimple();
		model.put("foo1", bean1);
		model.put("foo2", bean2);

		Object actual = view.filterModel(model);

		assertTrue(actual instanceof Map);
		assertSame(bean1, ((Map) actual).get("foo1"));
		assertSame(bean2, ((Map) actual).get("foo2"));
	}

	private void validateResult() throws Exception {
		Object jsResult =
				jsContext.evaluateString(jsScope, "(" + response.getContentAsString() + ")", "JSON Stream", 1, null);
		assertNotNull("Json Result did not eval as valid JavaScript", jsResult);
	}


	public static class TestBeanSimple {

		private String value = "foo";

		private boolean test = false;

		private long number = 42;

		private TestChildBean child = new TestChildBean();

		public String getValue() {
			return value;
		}

		public boolean getTest() {
			return test;
		}

		public long getNumber() {
			return number;
		}

		public Date getNow() {
			return new Date();
		}

		public TestChildBean getChild() {
			return child;
		}
	}

	@JsonSerialize(using=TestBeanSimpleSerializer.class)
	public static class TestBeanSimpleAnnotated extends TestBeanSimple {

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

	public static class TestBeanSimpleSerializer extends JsonSerializer<TestBeanSimple> {

		@Override
		public void serialize(TestBeanSimple value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException {

			jgen.writeStartObject();
			jgen.writeFieldName("testBeanSimple");
			jgen.writeString("custom");
			jgen.writeEndObject();

		}
	}

	public static class DelegatingSerializerFactory extends SerializerFactory {

		private SerializerFactory delegate = BeanSerializerFactory.instance;

		@Override
		@SuppressWarnings("unchecked")
		public <T> JsonSerializer<T> createSerializer(Class<T> type, SerializationConfig config) {
			if (type == TestBeanSimple.class) {
				return (JsonSerializer<T>) new TestBeanSimpleSerializer();
			}
			else {
				return delegate.createSerializer(type, config);
			}
		}
	}
}
