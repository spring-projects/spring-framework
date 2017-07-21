/*
 * Copyright 2012-2017 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromController;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodName;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

/**
 * Unit tests for {@link org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder}.
 *
 * @author Oliver Gierke
 * @author Dietrich Schulten
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@SuppressWarnings("unused")
public class MvcUriComponentsBuilderTests {

	private final MockHttpServletRequest request = new MockHttpServletRequest();


	@Before
	public void setup() {
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(this.request));
	}

	@After
	public void reset() {
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
	public void testFromControllerWithCustomBaseUrlViaStaticCall() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://example.org:9090/base");
		UriComponents uriComponents = fromController(builder, PersonControllerImpl.class).build();

		assertEquals("http://example.org:9090/base/people", uriComponents.toString());
		assertEquals("http://example.org:9090/base", builder.toUriString());
	}

	@Test
	public void testFromControllerWithCustomBaseUrlViaInstance() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://example.org:9090/base");
		MvcUriComponentsBuilder mvcBuilder = MvcUriComponentsBuilder.relativeTo(builder);
		UriComponents uriComponents = mvcBuilder.withController(PersonControllerImpl.class).build();

		assertEquals("http://example.org:9090/base/people", uriComponents.toString());
		assertEquals("http://example.org:9090/base", builder.toUriString());
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

	// SPR-12977

	@Test
	public void fromMethodNameWithBridgedMethod() throws Exception {
		UriComponents uriComponents = fromMethodName(PersonCrudController.class, "get", (long) 42).build();
		assertThat(uriComponents.toUriString(), is("http://localhost/42"));
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
	public void testFromMethodNameWithCustomBaseUrlViaStaticCall() throws Exception {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://example.org:9090/base");
		UriComponents uriComponents = fromMethodName(builder, ControllerWithMethods.class,
				"methodWithPathVariable", new Object[] {"1"}).build();

		assertEquals("http://example.org:9090/base/something/1/foo", uriComponents.toString());
		assertEquals("http://example.org:9090/base", builder.toUriString());
	}

	@Test
	public void testFromMethodNameWithCustomBaseUrlViaInstance() throws Exception {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://example.org:9090/base");
		MvcUriComponentsBuilder mvcBuilder = MvcUriComponentsBuilder.relativeTo(builder);
		UriComponents uriComponents = mvcBuilder.withMethodName(ControllerWithMethods.class,
				"methodWithPathVariable", new Object[] {"1"}).build();

		assertEquals("http://example.org:9090/base/something/1/foo", uriComponents.toString());
		assertEquals("http://example.org:9090/base", builder.toUriString());
	}

	@Test
	public void testFromMethodNameWithMetaAnnotation() throws Exception {
		UriComponents uriComponents = fromMethodName(MetaAnnotationController.class, "handleInput").build();
		assertThat(uriComponents.toUriString(), is("http://localhost/input"));
	}

	@Test // SPR-14405
	public void testFromMappingNameWithOptionalParam() throws Exception {
		UriComponents uriComponents = fromMethodName(ControllerWithMethods.class,
				"methodWithOptionalParam", new Object[] {null}).build();

		assertThat(uriComponents.toUriString(), is("http://localhost/something/optional-param"));
	}

	@Test
	public void testFromMethodCall() {
		UriComponents uriComponents = fromMethodCall(on(ControllerWithMethods.class).myMethod(null)).build();

		assertThat(uriComponents.toUriString(), startsWith("http://localhost"));
		assertThat(uriComponents.toUriString(), endsWith("/something/else"));
	}

 	@Test
	public void testFromMethodCallOnSubclass() {
		UriComponents uriComponents = fromMethodCall(on(ExtendedController.class).myMethod(null)).build();

		assertThat(uriComponents.toUriString(), startsWith("http://localhost"));
		assertThat(uriComponents.toUriString(), endsWith("/extended/else"));
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
	public void testFromMethodCallWithCustomBaseUrlViaStaticCall() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://example.org:9090/base");
		UriComponents uriComponents = fromMethodCall(builder, on(ControllerWithMethods.class).myMethod(null)).build();

		assertEquals("http://example.org:9090/base/something/else", uriComponents.toString());
		assertEquals("http://example.org:9090/base", builder.toUriString());
	}

	@Test
	public void testFromMethodCallWithCustomBaseUrlViaInstance() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://example.org:9090/base");
		MvcUriComponentsBuilder mvcBuilder = MvcUriComponentsBuilder.relativeTo(builder);
		UriComponents result = mvcBuilder.withMethodCall(on(ControllerWithMethods.class).myMethod(null)).build();

		assertEquals("http://example.org:9090/base/something/else", result.toString());
		assertEquals("http://example.org:9090/base", builder.toUriString());
	}

	@Test
	public void testFromMappingName() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(WebConfig.class);
		context.refresh();

		this.request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
		this.request.setServerName("example.org");
		this.request.setServerPort(9999);
		this.request.setContextPath("/base");

		String mappingName = "PAC#getAddressesForCountry";
		String url = MvcUriComponentsBuilder.fromMappingName(mappingName).arg(0, "DE").buildAndExpand(123);
		assertEquals("/base/people/123/addresses/DE", url);
	}

	@Test
	public void testFromMappingNameWithCustomBaseUrl() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(WebConfig.class);
		context.refresh();

		this.request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);

		UriComponentsBuilder baseUrl = UriComponentsBuilder.fromUriString("http://example.org:9999/base");
		MvcUriComponentsBuilder mvcBuilder = MvcUriComponentsBuilder.relativeTo(baseUrl);
		String url = mvcBuilder.withMappingName("PAC#getAddressesForCountry").arg(0, "DE").buildAndExpand(123);
		assertEquals("http://example.org:9999/base/people/123/addresses/DE", url);
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


	private class PersonControllerImpl implements PersonController {
	}


	@RequestMapping("/people/{id}/addresses")
	private static class PersonsAddressesController {

		@RequestMapping("/{country}")
		HttpEntity<Void> getAddressesForCountry(@PathVariable String country) {
			return null;
		}
	}


	@RequestMapping({"/persons", "/people"})
	private class InvalidController {
	}


	private class UnmappedController {

		@RequestMapping
		public void unmappedMethod() {
		}
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

		@RequestMapping("/optional-param")
		HttpEntity<Void> methodWithOptionalParam(@RequestParam(defaultValue = "") String q) {
			return null;
		}
	}


	@RequestMapping("/extended")
	@SuppressWarnings("WeakerAccess")
	static class ExtendedController extends ControllerWithMethods {
	}


	@RequestMapping("/user/{userId}/contacts")
	private static class UserContactController {

		@RequestMapping("/create")
		public String showCreate(@PathVariable Integer userId) {
			return null;
		}
	}


	static abstract class AbstractCrudController<T, ID> {

		abstract T get(ID id);
	}


	private static class PersonCrudController extends AbstractCrudController<Person, Long> {

		@RequestMapping(path = "/{id}", method = RequestMethod.GET)
		public Person get(@PathVariable Long id) {
			return new Person();
		}
	}


	@Controller
	private static class MetaAnnotationController {

		@RequestMapping
		public void handle() {
		}

		@PostJson(path = "/input")
		public void handleInput() {
		}
	}


	@RequestMapping(method = RequestMethod.POST,
			produces = MediaType.APPLICATION_JSON_VALUE,
			consumes = MediaType.APPLICATION_JSON_VALUE)
	@Target({ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	private @interface PostJson {

		String[] path() default {};
	}


	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Bean
		public PersonsAddressesController controller() {
			return new PersonsAddressesController();
		}
	}

}
