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

package org.springframework.web.client;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Arjen Poutsma
 */
public class RestTemplateIntegrationTests extends AbstractJettyServerTestCase {

	private final RestTemplate template = new RestTemplate(new HttpComponentsClientHttpRequestFactory());


	@Test
	public void getString() {
		String s = template.getForObject(baseUrl + "/{method}", String.class, "get");
		assertEquals("Invalid content", helloWorld, s);
	}

	@Test
	public void getEntity() {
		ResponseEntity<String> entity = template.getForEntity(baseUrl + "/{method}", String.class, "get");
		assertEquals("Invalid content", helloWorld, entity.getBody());
		assertFalse("No headers", entity.getHeaders().isEmpty());
		assertEquals("Invalid content-type", textContentType, entity.getHeaders().getContentType());
		assertEquals("Invalid status code", HttpStatus.OK, entity.getStatusCode());
	}

	@Test
	public void getNoResponse() {
		String s = template.getForObject(baseUrl + "/get/nothing", String.class);
		assertNull("Invalid content", s);
	}

	@Test
	public void getNoContentTypeHeader() throws UnsupportedEncodingException {
		byte[] bytes = template.getForObject(baseUrl + "/get/nocontenttype", byte[].class);
		assertArrayEquals("Invalid content", helloWorld.getBytes("UTF-8"), bytes);
	}

	@Test
	public void getNoContent() {
		String s = template.getForObject(baseUrl + "/status/nocontent", String.class);
		assertNull("Invalid content", s);

		ResponseEntity<String> entity = template.getForEntity(baseUrl + "/status/nocontent", String.class);
		assertEquals("Invalid response code", HttpStatus.NO_CONTENT, entity.getStatusCode());
		assertNull("Invalid content", entity.getBody());
	}

	@Test
	public void getNotModified() {
		String s = template.getForObject(baseUrl + "/status/notmodified", String.class);
		assertNull("Invalid content", s);

		ResponseEntity<String> entity = template.getForEntity(baseUrl + "/status/notmodified", String.class);
		assertEquals("Invalid response code", HttpStatus.NOT_MODIFIED, entity.getStatusCode());
		assertNull("Invalid content", entity.getBody());
	}

	@Test
	public void postForLocation() throws URISyntaxException {
		URI location = template.postForLocation(baseUrl + "/{method}", helloWorld, "post");
		assertEquals("Invalid location", new URI(baseUrl + "/post/1"), location);
	}

	@Test
	public void postForLocationEntity() throws URISyntaxException {
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);
		URI location = template.postForLocation(baseUrl + "/{method}", entity, "post");
		assertEquals("Invalid location", new URI(baseUrl + "/post/1"), location);
	}

	@Test
	public void postForObject() throws URISyntaxException {
		String s = template.postForObject(baseUrl + "/{method}", helloWorld, String.class, "post");
		assertEquals("Invalid content", helloWorld, s);
	}

	@Test
	public void patchForObject() throws URISyntaxException {
		String s = template.patchForObject(baseUrl + "/{method}", helloWorld, String.class, "patch");
		assertEquals("Invalid content", helloWorld, s);
	}

	@Test
	public void notFound() {
		try {
			template.execute(baseUrl + "/status/notfound", HttpMethod.GET, null, null);
			fail("HttpClientErrorException expected");
		}
		catch (HttpClientErrorException ex) {
			assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
			assertNotNull(ex.getStatusText());
			assertNotNull(ex.getResponseBodyAsString());
		}
	}

	@Test
	public void serverError() {
		try {
			template.execute(baseUrl + "/status/server", HttpMethod.GET, null, null);
			fail("HttpServerErrorException expected");
		}
		catch (HttpServerErrorException ex) {
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
			assertNotNull(ex.getStatusText());
			assertNotNull(ex.getResponseBodyAsString());
		}
	}

	@Test
	public void optionsForAllow() throws URISyntaxException {
		Set<HttpMethod> allowed = template.optionsForAllow(new URI(baseUrl + "/get"));
		assertEquals("Invalid response",
				EnumSet.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD, HttpMethod.TRACE), allowed);
	}

	@Test
	public void uri() throws InterruptedException, URISyntaxException {
		String result = template.getForObject(baseUrl + "/uri/{query}", String.class, "Z\u00fcrich");
		assertEquals("Invalid request URI", "/uri/Z%C3%BCrich", result);

		result = template.getForObject(baseUrl + "/uri/query={query}", String.class, "foo@bar");
		assertEquals("Invalid request URI", "/uri/query=foo@bar", result);

		result = template.getForObject(baseUrl + "/uri/query={query}", String.class, "T\u014dky\u014d");
		assertEquals("Invalid request URI", "/uri/query=T%C5%8Dky%C5%8D", result);
	}

	@Test
	public void multipart() throws UnsupportedEncodingException {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("name 1", "value 1");
		parts.add("name 2", "value 2+1");
		parts.add("name 2", "value 2+2");
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("logo", logo);

		template.postForLocation(baseUrl + "/multipart", parts);
	}

	@Test
	public void form() throws UnsupportedEncodingException {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("name 1", "value 1");
		form.add("name 2", "value 2+1");
		form.add("name 2", "value 2+2");

		template.postForLocation(baseUrl + "/form", form);
	}

	@Test
	public void exchangeGet() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);
		ResponseEntity<String> response =
				template.exchange(baseUrl + "/{method}", HttpMethod.GET, requestEntity, String.class, "get");
		assertEquals("Invalid content", helloWorld, response.getBody());
	}

	@Test
	public void exchangePost() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		requestHeaders.setContentType(MediaType.TEXT_PLAIN);
		HttpEntity<String> requestEntity = new HttpEntity<>(helloWorld, requestHeaders);
		HttpEntity<Void> result = template.exchange(baseUrl + "/{method}", HttpMethod.POST, requestEntity, Void.class, "post");
		assertEquals("Invalid location", new URI(baseUrl + "/post/1"), result.getHeaders().getLocation());
		assertFalse(result.hasBody());
	}

	@Test
	public void jsonPostForObject() throws URISyntaxException {
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
		MySampleBean bean = new MySampleBean();
		bean.setWith1("with");
		bean.setWith2("with");
		bean.setWithout("without");
		HttpEntity<MySampleBean> entity = new HttpEntity<>(bean, entityHeaders);
		String s = template.postForObject(baseUrl + "/jsonpost", entity, String.class);
		assertTrue(s.contains("\"with1\":\"with\""));
		assertTrue(s.contains("\"with2\":\"with\""));
		assertTrue(s.contains("\"without\":\"without\""));
	}

	@Test
	public void jsonPostForObjectWithJacksonView() throws URISyntaxException {
		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
		MySampleBean bean = new MySampleBean("with", "with", "without");
		MappingJacksonValue jacksonValue = new MappingJacksonValue(bean);
		jacksonValue.setSerializationView(MyJacksonView1.class);
		HttpEntity<MappingJacksonValue> entity = new HttpEntity<>(jacksonValue, entityHeaders);
		String s = template.postForObject(baseUrl + "/jsonpost", entity, String.class);
		assertTrue(s.contains("\"with1\":\"with\""));
		assertFalse(s.contains("\"with2\":\"with\""));
		assertFalse(s.contains("\"without\":\"without\""));
	}

	@Test  // SPR-12123
	public void serverPort() {
		String s = template.getForObject("http://localhost:{port}/get", String.class, port);
		assertEquals("Invalid content", helloWorld, s);
	}

	@Test  // SPR-13154
	public void jsonPostForObjectWithJacksonTypeInfoList() throws URISyntaxException {
		List<ParentClass> list = new ArrayList<>();
		list.add(new Foo("foo"));
		list.add(new Bar("bar"));
		ParameterizedTypeReference<?> typeReference = new ParameterizedTypeReference<List<ParentClass>>() {};
		RequestEntity<List<ParentClass>> entity = RequestEntity
				.post(new URI(baseUrl + "/jsonpost"))
				.contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
				.body(list, typeReference.getType());
		String content = template.exchange(entity, String.class).getBody();
		assertTrue(content.contains("\"type\":\"foo\""));
		assertTrue(content.contains("\"type\":\"bar\""));
	}


	public interface MyJacksonView1 {};

	public interface MyJacksonView2 {};


	public static class MySampleBean {

		@JsonView(MyJacksonView1.class)
		private String with1;

		@JsonView(MyJacksonView2.class)
		private String with2;

		private String without;

		private MySampleBean() {
		}

		private MySampleBean(String with1, String with2, String without) {
			this.with1 = with1;
			this.with2 = with2;
			this.without = without;
		}

		public String getWith1() {
			return with1;
		}

		public void setWith1(String with1) {
			this.with1 = with1;
		}

		public String getWith2() {
			return with2;
		}

		public void setWith2(String with2) {
			this.with2 = with2;
		}

		public String getWithout() {
			return without;
		}

		public void setWithout(String without) {
			this.without = without;
		}
	}


	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	public static class ParentClass {

		private String parentProperty;

		public ParentClass() {
		}

		public ParentClass(String parentProperty) {
			this.parentProperty = parentProperty;
		}

		public String getParentProperty() {
			return parentProperty;
		}

		public void setParentProperty(String parentProperty) {
			this.parentProperty = parentProperty;
		}
	}


	@JsonTypeName("foo")
	public static class Foo extends ParentClass {

		public Foo() {
		}

		public Foo(String parentProperty) {
			super(parentProperty);
		}
	}


	@JsonTypeName("bar")
	public static class Bar extends ParentClass {

		public Bar() {
		}

		public Bar(String parentProperty) {
			super(parentProperty);
		}
	}

}
