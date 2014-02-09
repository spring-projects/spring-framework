/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpEntity;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponents;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.*;

/**
 * Unit tests for {@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder}.
 *
 * @author Oliver Gierke
 * @author Dietrich Schulten
 * @author Rossen Stoyanchev
 */
public class MvcUriComponentsBuilderTests {

	private MockHttpServletRequest request;


	@Before
	public void setUp() {
		this.request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(this.request));
	}

	@After
	public void tearDown() {
		RequestContextHolder.resetRequestAttributes();
	}


	@Test
	public void testFromController() {
		UriComponents uriComponents = fromController(PersonControllerImpl.class).build();
		assertThat(uriComponents.toUriString(), Matchers.endsWith("/people"));
	}

	@Test
	public void testFromControllerUriTemplate() {
		UriComponents uriComponents = fromController(PersonsAddressesController.class).buildAndExpand(15);
		assertThat(uriComponents.toUriString(), endsWith("/people/15/addresses"));
	}

	@Test
	public void testFromControllerSubResource() {
		UriComponents uriComponents = fromController(PersonControllerImpl.class).pathSegment("something").build();

		assertThat(uriComponents.toUriString(), endsWith("/people/something"));
	}

	@Test
	public void testFromControllerTwoTypeLevelMappings() {
		UriComponents uriComponents = fromController(InvalidController.class).build();
		assertThat(uriComponents.toUriString(), is("http://localhost/persons"));
	}

	@Test
	public void testFromControllerNotMapped() {
		UriComponents uriComponents = fromController(UnmappedController.class).build();
		assertThat(uriComponents.toUriString(), is("http://localhost/"));
	}

	@Test
	public void testFromMethodNamePathVariable() throws Exception {
		UriComponents uriComponents = fromMethodName(
				ControllerWithMethods.class, "methodWithPathVariable", new Object[]{"1"}).build();

		assertThat(uriComponents.toUriString(), is("http://localhost/something/1/foo"));
	}

	@Test
	public void testFromMethodNameTypeLevelPathVariable() throws Exception {
		this.request.setContextPath("/myapp");
		UriComponents uriComponents = fromMethodName(
				PersonsAddressesController.class, "getAddressesForCountry", "DE").buildAndExpand("1");

		assertThat(uriComponents.toUriString(), is("http://localhost/myapp/people/1/addresses/DE"));
	}

	@Test
	public void testFromMethodNameTwoPathVariables() throws Exception {
		DateTime now = DateTime.now();
		UriComponents uriComponents = fromMethodName(
				ControllerWithMethods.class, "methodWithTwoPathVariables", 1, now).build();

		assertThat(uriComponents.getPath(), is("/something/1/foo/" + ISODateTimeFormat.date().print(now)));
	}

	@Test
	public void testFromMethodNameWithPathVarAndRequestParam() throws Exception {
		UriComponents uriComponents = fromMethodName(
				ControllerWithMethods.class, "methodForNextPage", "1", 10, 5).build();

		assertThat(uriComponents.getPath(), is("/something/1/foo"));
		MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
		assertThat(queryParams.get("limit"), contains("5"));
		assertThat(queryParams.get("offset"), contains("10"));
	}

	// SPR-11391

	@Test
	public void testFromMethodNameTypeLevelPathVariableWithoutArgumentValue() throws Exception {
		UriComponents uriComponents = fromMethodName(UserContactController.class, "showCreate", 123).build();

		assertThat(uriComponents.getPath(), is("/user/123/contacts/create"));
	}

	@Test
	public void testFromMethodNameNotMapped() throws Exception {
		UriComponents uriComponents = fromMethodName(UnmappedController.class, "unmappedMethod").build();

		assertThat(uriComponents.toUriString(), is("http://localhost/"));
	}

	@Test
	public void testFromMethodCall() {
		UriComponents uriComponents = fromMethodCall(on(ControllerWithMethods.class).myMethod(null)).build();

		assertThat(uriComponents.toUriString(), startsWith("http://localhost"));
		assertThat(uriComponents.toUriString(), endsWith("/something/else"));
	}

	@Test
	public void testFromMethodCallWithTypeLevelUriVars() {
		UriComponents uriComponents = fromMethodCall(on(
				PersonsAddressesController.class).getAddressesForCountry("DE")).buildAndExpand(15);

		assertThat(uriComponents.toUriString(), endsWith("/people/15/addresses/DE"));
	}


	@Test
	public void testFromMethodCallWithPathVar() {
		UriComponents uriComponents = fromMethodCall(on(
				ControllerWithMethods.class).methodWithPathVariable("1")).build();

		assertThat(uriComponents.toUriString(), startsWith("http://localhost"));
		assertThat(uriComponents.toUriString(), endsWith("/something/1/foo"));
	}

	@Test
	public void testFromMethodCallWithPathVarAndRequestParams() {
		UriComponents uriComponents = fromMethodCall(on(
				ControllerWithMethods.class).methodForNextPage("1", 10, 5)).build();

		assertThat(uriComponents.getPath(), is("/something/1/foo"));

		MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
		assertThat(queryParams.get("limit"), contains("5"));
		assertThat(queryParams.get("offset"), contains("10"));
	}

	@Test
	public void testFromMethodCallWithPathVarAndMultiValueRequestParams() {
		UriComponents uriComponents = fromMethodCall(on(
				ControllerWithMethods.class).methodWithMultiValueRequestParams("1", Arrays.asList(3, 7), 5)).build();

		assertThat(uriComponents.getPath(), is("/something/1/foo"));

		MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
		assertThat(queryParams.get("limit"), contains("5"));
		assertThat(queryParams.get("items"), containsInAnyOrder("3", "7"));
	}

	@Test
	public void usesForwardedHostAsHostIfHeaderIsSet() {
		this.request.addHeader("X-Forwarded-Host", "somethingDifferent");
		UriComponents uriComponents = fromController(PersonControllerImpl.class).build();

		assertThat(uriComponents.toUriString(), startsWith("http://somethingDifferent"));
	}

	@Test
	public void usesForwardedHostAndPortFromHeader() {
		request.addHeader("X-Forwarded-Host", "foobar:8088");
		UriComponents uriComponents = fromController(PersonControllerImpl.class).build();

		assertThat(uriComponents.toUriString(), startsWith("http://foobar:8088"));
	}

	@Test
	public void usesFirstHostOfXForwardedHost() {
		request.addHeader("X-Forwarded-Host", "barfoo:8888, localhost:8088");
		UriComponents uriComponents = fromController(PersonControllerImpl.class).build();

		assertThat(uriComponents.toUriString(), startsWith("http://barfoo:8888"));
	}


	static class Person {

		Long id;

		public Long getId() {
			return id;
		}
	}

	@RequestMapping("/people")
	interface PersonController {

	}

	class PersonControllerImpl implements PersonController {

	}

	@SuppressWarnings("unused")
	@RequestMapping("/people/{id}/addresses")
	static class PersonsAddressesController {

		@RequestMapping("/{country}")
		public HttpEntity<Void> getAddressesForCountry(@PathVariable String country) {
			return null;
		}
	}

	@RequestMapping({ "/persons", "/people" })
	class InvalidController {

	}

	@SuppressWarnings("unused")
	class UnmappedController {

		@RequestMapping
		public void unmappedMethod() {
		}
	}

	@SuppressWarnings("unused")
	@RequestMapping("/something")
	static class ControllerWithMethods {

		@RequestMapping("/else")
		HttpEntity<Void> myMethod(@RequestBody Object payload) {
			return null;
		}

		@RequestMapping("/{id}/foo")
		HttpEntity<Void> methodWithPathVariable(@PathVariable String id) {
			return null;
		}

		@RequestMapping("/{id}/foo/{date}")
		HttpEntity<Void> methodWithTwoPathVariables(
				@PathVariable Integer id, @DateTimeFormat(iso = ISO.DATE) @PathVariable DateTime date) {
			return null;
		}

		@RequestMapping(value = "/{id}/foo")
		HttpEntity<Void> methodForNextPage(@PathVariable String id,
				@RequestParam Integer offset, @RequestParam Integer limit) {
			return null;
		}

		@RequestMapping(value = "/{id}/foo")
		HttpEntity<Void> methodWithMultiValueRequestParams(@PathVariable String id,
				@RequestParam List<Integer> items, @RequestParam Integer limit) {
			return null;
		}
	}

	@RequestMapping("/user/{userId}/contacts")
	static class UserContactController {

		@RequestMapping("/create")
		public String showCreate(@PathVariable Integer userId) {
			return null;
		}
	}

}
