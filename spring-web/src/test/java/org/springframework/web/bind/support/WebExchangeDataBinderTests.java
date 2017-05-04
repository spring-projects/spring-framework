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

package org.springframework.web.bind.support;

import java.beans.PropertyEditorSupport;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.FormHttpMessageWriter;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.mock.http.client.reactive.test.MockClientHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.core.ResolvableType.forClassWithGenerics;

/**
 * Unit tests for {@link WebExchangeDataBinder}.
 *
 * @author Rossen Stoyanchev
 */
public class WebExchangeDataBinderTests {

	private TestBean testBean;

	private WebExchangeDataBinder binder;


	@Before
	public void setup() throws Exception {
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

	@Test
	public void testMultipart() throws Exception {

		MultipartBean bean = new MultipartBean();
		WebExchangeDataBinder binder = new WebExchangeDataBinder(bean);

		MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
		data.add("name", "bar");
		data.add("someList", "123");
		data.add("someList", "abc");
		data.add("someArray", "dec");
		data.add("someArray", "456");
		data.add("part", new ClassPathResource("org/springframework/http/codec/multipart/foo.txt"));
		data.add("somePartList", new ClassPathResource("org/springframework/http/codec/multipart/foo.txt"));
		data.add("somePartList", new ClassPathResource("org/springframework/http/server/reactive/spring.png"));
		binder.bind(exchangeMultipart(data)).block(Duration.ofMillis(5000));

		assertEquals("bar", bean.getName());
		assertEquals(Arrays.asList("123", "abc"), bean.getSomeList());
		assertArrayEquals(new String[] {"dec", "456"}, bean.getSomeArray());
		assertEquals("foo.txt", bean.getPart().filename());
		assertEquals(2, bean.getSomePartList().size());
		assertEquals("foo.txt", bean.getSomePartList().get(0).filename());
		assertEquals("spring.png", bean.getSomePartList().get(1).filename());
	}



	private ServerWebExchange exchange(MultiValueMap<String, String> formData) {

		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, "/");

		new FormHttpMessageWriter().write(Mono.just(formData),
				forClassWithGenerics(MultiValueMap.class, String.class, String.class),
				MediaType.APPLICATION_FORM_URLENCODED, request, Collections.emptyMap()).block();

		return MockServerHttpRequest
				.post("/")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(request.getBody())
				.toExchange();
	}

	private ServerWebExchange exchangeMultipart(MultiValueMap<String, ?> multipartData) {

		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.POST, "/");

		new MultipartHttpMessageWriter().write(Mono.just(multipartData), forClass(MultiValueMap.class),
				MediaType.MULTIPART_FORM_DATA, request, Collections.emptyMap()).block();

		return MockServerHttpRequest
				.post("/")
				.contentType(request.getHeaders().getContentType())
				.body(request.getBody())
				.toExchange();
	}


	private static class TestBeanPropertyEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) {
			setValue(new TestBean());
		}
	}

	private static class MultipartBean {

		private String name;

		private List<?> someList;

		private String[] someArray;

		private FilePart part;

		private List<FilePart> somePartList;


		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<?> getSomeList() {
			return this.someList;
		}

		public void setSomeList(List<?> someList) {
			this.someList = someList;
		}

		public String[] getSomeArray() {
			return this.someArray;
		}

		public void setSomeArray(String[] someArray) {
			this.someArray = someArray;
		}

		public FilePart getPart() {
			return this.part;
		}

		public void setPart(FilePart part) {
			this.part = part;
		}

		public List<FilePart> getSomePartList() {
			return this.somePartList;
		}

		public void setSomePartList(List<FilePart> somePartList) {
			this.somePartList = somePartList;
		}
	}

}
