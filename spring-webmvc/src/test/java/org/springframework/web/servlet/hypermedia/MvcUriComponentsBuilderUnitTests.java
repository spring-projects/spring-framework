/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.web.servlet.hypermedia;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.web.servlet.hypermedia.MvcUriComponentsBuilder.*;

/**
 * Unit tests for {@link MvcUriComponentsBuilder}.
 * 
 * @author Oliver Gierke
 * @author Dietrich Schulten
 */
public class MvcUriComponentsBuilderUnitTests extends TestUtils {

	@Test
	public void createsLinkToControllerRoot() {

		URI link = from(PersonControllerImpl.class).build().toUri();
		assertThat(link.toString(), Matchers.endsWith("/people"));
	}

	@Test
	public void createsLinkToParameterizedControllerRoot() {

		URI link = from(PersonsAddressesController.class, 15).build().toUri();
		assertThat(link.toString(), endsWith("/people/15/addresses"));
	}

	/**
	 * @see #70
	 */
	@Test
	public void createsLinkToMethodOnParameterizedControllerRoot() {

		URI link = from(
				methodOn(PersonsAddressesController.class, 15).getAddressesForCountry(
						"DE")).build().toUri();
		assertThat(link.toString(), endsWith("/people/15/addresses/DE"));
	}

	@Test
	public void createsLinkToSubResource() {

		URI link = from(PersonControllerImpl.class).pathSegment("something").build().toUri();
		assertThat(link.toString(), endsWith("/people/something"));
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsControllerWithMultipleMappings() {
		from(InvalidController.class);
	}

	@Test
	public void createsLinkToUnmappedController() {

		URI link = from(UnmappedController.class).build().toUri();
		assertThat(link.toString(), is("http://localhost/"));
	}

	@Test
	public void appendingNullIsANoOp() {

		URI link = from(PersonControllerImpl.class).path(null).build().toUri();
		assertThat(link.toString(), endsWith("/people"));
	}

	@Test
	public void linksToMethod() {

		URI link = from(methodOn(ControllerWithMethods.class).myMethod(null)).build().toUri();
		assertPointsToMockServer(link);
		assertThat(link.toString(), endsWith("/something/else"));
	}

	@Test
	public void linksToMethodWithPathVariable() {

		URI link = from(methodOn(ControllerWithMethods.class).methodWithPathVariable("1")).build().toUri();
		assertPointsToMockServer(link);
		assertThat(link.toString(), endsWith("/something/1/foo"));
	}

	/**
	 * @see #33
	 */
	@Test
	public void usesForwardedHostAsHostIfHeaderIsSet() {

		request.addHeader("X-Forwarded-Host", "somethingDifferent");

		URI link = from(PersonControllerImpl.class).build().toUri();
		assertThat(link.toString(), startsWith("http://somethingDifferent"));
	}

	/**
	 * @see #26, #39
	 */
	@Test
	public void linksToMethodWithPathVariableAndRequestParams() {

		URI link = from(
				methodOn(ControllerWithMethods.class).methodForNextPage("1", 10, 5)).build().toUri();

		UriComponents components = toComponents(link);
		assertThat(components.getPath(), is("/something/1/foo"));

		MultiValueMap<String, String> queryParams = components.getQueryParams();
		assertThat(queryParams.get("limit"), contains("5"));
		assertThat(queryParams.get("offset"), contains("10"));
	}

	/**
	 * @see #26, #39
	 */
	@Test
	public void linksToMethodWithPathVariableAndMultiValueRequestParams() {

		URI link = from(
				methodOn(ControllerWithMethods.class).methodWithMultiValueRequestParams(
						"1", Arrays.asList(3, 7), 5)).build().toUri();

		UriComponents components = toComponents(link);
		assertThat(components.getPath(), is("/something/1/foo"));

		MultiValueMap<String, String> queryParams = components.getQueryParams();
		assertThat(queryParams.get("limit"), contains("5"));
		assertThat(queryParams.get("items"), containsInAnyOrder("3", "7"));
	}

	/**
	 * @see #90
	 */
	@Test
	public void usesForwardedHostAndPortFromHeader() {

		request.addHeader("X-Forwarded-Host", "foobar:8088");

		URI link = from(PersonControllerImpl.class).build().toUri();
		assertThat(link.toString(), startsWith("http://foobar:8088"));
	}

	/**
	 * @see #90
	 */
	@Test
	public void usesFirstHostOfXForwardedHost() {

		request.addHeader("X-Forwarded-Host", "barfoo:8888, localhost:8088");

		URI link = from(PersonControllerImpl.class).build().toUri();
		assertThat(link.toString(), startsWith("http://barfoo:8888"));
	}

	private static UriComponents toComponents(URI link) {
		return UriComponentsBuilder.fromUri(link).build();
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

	class UnmappedController {

	}

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
}
