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
package org.springframework.web.bind;

import java.beans.PropertyEditorSupport;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

/**
 * Unit tests for {@link WebExchangeDataBinder}.
 *
 * @author Rossen Stoyanchev
 */
public class WebExchangeDataBinderTests {

	private static final ResolvableType ELEMENT_TYPE = ResolvableType.forClass(MultiValueMap.class);


	private WebExchangeDataBinder binder;

	private TestBean testBean;

	private ServerWebExchange exchange;

	@Mock
	private HttpMessageReader<MultiValueMap<String, String>> formReader;

	private MultiValueMap<String, String> formData;


	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		this.testBean = new TestBean();
		this.binder = new WebExchangeDataBinder(this.testBean, "person");
		this.binder.registerCustomEditor(ITestBean.class, new TestBeanPropertyEditor());
		this.binder.setFormReader(this.formReader);

		MockServerHttpRequest request = new MockServerHttpRequest();
		MockServerHttpResponse response = new MockServerHttpResponse();
		WebSessionManager sessionManager = new DefaultWebSessionManager();
		this.exchange = new DefaultServerWebExchange(request, response, sessionManager);

		request.getHeaders().setContentType(APPLICATION_FORM_URLENCODED);

		this.formData = new LinkedMultiValueMap<>();
		when(this.formReader.canRead(ELEMENT_TYPE, APPLICATION_FORM_URLENCODED)).thenReturn(true);
		when(this.formReader.readMono(ELEMENT_TYPE, request, Collections.emptyMap()))
				.thenReturn(Mono.just(formData));
	}


	@Test
	public void testBindingWithNestedObjectCreation() throws Exception {
		this.formData.add("spouse", "someValue");
		this.formData.add("spouse.name", "test");
		this.binder.bind(this.exchange).blockMillis(5000);

		assertNotNull(this.testBean.getSpouse());
		assertEquals("test", testBean.getSpouse().getName());
	}

	@Test
	public void testFieldPrefixCausesFieldReset() throws Exception {
		this.formData.add("_postProcessed", "visible");
		this.formData.add("postProcessed", "on");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertTrue(this.testBean.isPostProcessed());

		this.formData.remove("postProcessed");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertFalse(this.testBean.isPostProcessed());
	}

	@Test
	public void testFieldPrefixCausesFieldResetWithIgnoreUnknownFields() throws Exception {
		this.binder.setIgnoreUnknownFields(false);

		this.formData.add("_postProcessed", "visible");
		this.formData.add("postProcessed", "on");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertTrue(this.testBean.isPostProcessed());

		this.formData.remove("postProcessed");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertFalse(this.testBean.isPostProcessed());
	}

	@Test
	public void testFieldDefault() throws Exception {
		this.formData.add("!postProcessed", "off");
		this.formData.add("postProcessed", "on");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertTrue(this.testBean.isPostProcessed());

		this.formData.remove("postProcessed");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertFalse(this.testBean.isPostProcessed());
	}

	@Test
	public void testFieldDefaultPreemptsFieldMarker() throws Exception {
		this.formData.add("!postProcessed", "on");
		this.formData.add("_postProcessed", "visible");
		this.formData.add("postProcessed", "on");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertTrue(this.testBean.isPostProcessed());

		this.formData.remove("postProcessed");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertTrue(this.testBean.isPostProcessed());

		this.formData.remove("!postProcessed");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertFalse(this.testBean.isPostProcessed());
	}

	@Test
	public void testFieldDefaultNonBoolean() throws Exception {
		this.formData.add("!name", "anonymous");
		this.formData.add("name", "Scott");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertEquals("Scott", this.testBean.getName());

		this.formData.remove("name");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertEquals("anonymous", this.testBean.getName());
	}

	@Test
	public void testWithCommaSeparatedStringArray() throws Exception {
		this.formData.add("stringArray", "bar");
		this.formData.add("stringArray", "abc");
		this.formData.add("stringArray", "123,def");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertEquals("Expected all three items to be bound", 3, this.testBean.getStringArray().length);

		this.formData.remove("stringArray");
		this.formData.add("stringArray", "123,def");
		this.binder.bind(this.exchange).blockMillis(5000);
		assertEquals("Expected only 1 item to be bound", 1, this.testBean.getStringArray().length);
	}

	@Test
	public void testBindingWithNestedObjectCreationAndWrongOrder() throws Exception {
		this.formData.add("spouse.name", "test");
		this.formData.add("spouse", "someValue");
		this.binder.bind(this.exchange).blockMillis(5000);

		assertNotNull(this.testBean.getSpouse());
		assertEquals("test", this.testBean.getSpouse().getName());
	}

	@Test
	public void testBindingWithQueryParams() throws Exception {
		MultiValueMap<String, String> queryParams = this.exchange.getRequest().getQueryParams();
		queryParams.add("spouse", "someValue");
		queryParams.add("spouse.name", "test");
		this.binder.bind(this.exchange).blockMillis(5000);

		assertNotNull(this.testBean.getSpouse());
		assertEquals("test", this.testBean.getSpouse().getName());
	}



	private static class TestBeanPropertyEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) {
			setValue(new TestBean());
		}
	}

}
