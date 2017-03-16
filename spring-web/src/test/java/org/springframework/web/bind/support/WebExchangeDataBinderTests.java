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

package org.springframework.web.bind.support;

import java.beans.PropertyEditorSupport;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link WebExchangeDataBinder}.
 *
 * @author Rossen Stoyanchev
 */
public class WebExchangeDataBinderTests {

	private WebExchangeDataBinder binder;

	private TestBean testBean;


	@Before
	public void setUp() throws Exception {
		this.testBean = new TestBean();
		this.binder = new WebExchangeDataBinder(this.testBean, "person");
		this.binder.registerCustomEditor(ITestBean.class, new TestBeanPropertyEditor());
	}


	@Test
	public void testBindingWithNestedObjectCreation() throws Exception {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("spouse", "someValue");
		formData.add("spouse.name", "test");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));

		assertNotNull(this.testBean.getSpouse());
		assertEquals("test", testBean.getSpouse().getName());
	}

	@Test
	public void testFieldPrefixCausesFieldReset() throws Exception {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("_postProcessed", "visible");
		formData.add("postProcessed", "on");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertTrue(this.testBean.isPostProcessed());

		formData.remove("postProcessed");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertFalse(this.testBean.isPostProcessed());
	}

	@Test
	public void testFieldPrefixCausesFieldResetWithIgnoreUnknownFields() throws Exception {
		this.binder.setIgnoreUnknownFields(false);

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("_postProcessed", "visible");
		formData.add("postProcessed", "on");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertTrue(this.testBean.isPostProcessed());

		formData.remove("postProcessed");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertFalse(this.testBean.isPostProcessed());
	}

	@Test
	public void testFieldDefault() throws Exception {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("!postProcessed", "off");
		formData.add("postProcessed", "on");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertTrue(this.testBean.isPostProcessed());

		formData.remove("postProcessed");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertFalse(this.testBean.isPostProcessed());
	}

	@Test
	public void testFieldDefaultPreemptsFieldMarker() throws Exception {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("!postProcessed", "on");
		formData.add("_postProcessed", "visible");
		formData.add("postProcessed", "on");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertTrue(this.testBean.isPostProcessed());

		formData.remove("postProcessed");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertTrue(this.testBean.isPostProcessed());

		formData.remove("!postProcessed");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertFalse(this.testBean.isPostProcessed());
	}

	@Test
	public void testFieldDefaultNonBoolean() throws Exception {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("!name", "anonymous");
		formData.add("name", "Scott");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertEquals("Scott", this.testBean.getName());

		formData.remove("name");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertEquals("anonymous", this.testBean.getName());
	}

	@Test
	public void testWithCommaSeparatedStringArray() throws Exception {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("stringArray", "bar");
		formData.add("stringArray", "abc");
		formData.add("stringArray", "123,def");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertEquals("Expected all three items to be bound", 3, this.testBean.getStringArray().length);

		formData.remove("stringArray");
		formData.add("stringArray", "123,def");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));
		assertEquals("Expected only 1 item to be bound", 1, this.testBean.getStringArray().length);
	}

	@Test
	public void testBindingWithNestedObjectCreationAndWrongOrder() throws Exception {
		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("spouse.name", "test");
		formData.add("spouse", "someValue");
		this.binder.bind(exchange(formData)).block(Duration.ofMillis(5000));

		assertNotNull(this.testBean.getSpouse());
		assertEquals("test", this.testBean.getSpouse().getName());
	}

	@Test
	public void testBindingWithQueryParams() throws Exception {
		String url = "/path?spouse=someValue&spouse.name=test";
		MockServerHttpRequest request = MockServerHttpRequest.post(url).build();
		this.binder.bind(request.toExchange()).block(Duration.ofSeconds(5));

		assertNotNull(this.testBean.getSpouse());
		assertEquals("test", this.testBean.getSpouse().getName());
	}

	private String generateForm(MultiValueMap<String, String> form) {
		StringBuilder builder = new StringBuilder();
		try {
			for (Iterator<String> names = form.keySet().iterator(); names.hasNext();) {
				String name = names.next();
				for (Iterator<String> values = form.get(name).iterator(); values.hasNext();) {
					String value = values.next();
					builder.append(URLEncoder.encode(name, "UTF-8"));
					if (value != null) {
						builder.append('=');
						builder.append(URLEncoder.encode(value, "UTF-8"));
						if (values.hasNext()) {
							builder.append('&');
						}
					}
				}
				if (names.hasNext()) {
					builder.append('&');
				}
			}
		}
		catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
		return builder.toString();
	}

	private ServerWebExchange exchange(MultiValueMap<String, String> formData) {
		return MockServerHttpRequest
				.post("/")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(generateForm(formData))
				.toExchange();
	}


	private static class TestBeanPropertyEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) {
			setValue(new TestBean());
		}
	}

}
