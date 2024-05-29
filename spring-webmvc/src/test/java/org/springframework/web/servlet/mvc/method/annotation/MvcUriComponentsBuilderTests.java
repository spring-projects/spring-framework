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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Savepoint;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.testfixture.servlet.MockFilterChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromController;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMappingName;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodName;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.relativeTo;

/**
 * Tests for {@link MvcUriComponentsBuilder}.
 *
 * @author Oliver Gierke
 * @author Dietrich Schulten
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@SuppressWarnings("unused")
public class MvcUriComponentsBuilderTests {

	private final MockHttpServletRequest request = new MockHttpServletRequest();


	@BeforeEach
	void setup() {
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(this.request));
	}

	@AfterEach
	void reset() {
		RequestContextHolder.resetRequestAttributes();
	}


	@Test
	void fromControllerPlain() {
		UriComponents uriComponents = fromController(PersonControllerImpl.class).build();
		assertThat(uriComponents.toUriString()).endsWith("/people");
	}

	@Test
	void fromControllerUriTemplate() {
		UriComponents uriComponents = fromController(PersonsAddressesController.class).buildAndExpand(15);
		assertThat(uriComponents.toUriString()).endsWith("/people/15/addresses");
	}

	@Test
	void fromControllerSubResource() {
		UriComponents uriComponents = fromController(PersonControllerImpl.class).pathSegment("something").build();

		assertThat(uriComponents.toUriString()).endsWith("/people/something");
	}

	@Test
	void fromControllerTwoTypeLevelMappings() {
		UriComponents uriComponents = fromController(InvalidController.class).build();
		assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/persons");
	}

	@Test
	void fromControllerNotMapped() {
		UriComponents uriComponents = fromController(UnmappedController.class).build();
		assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/");
	}

	@Test
	void fromControllerWithCustomBaseUrlViaStaticCall() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://example.org:9090/base");
		UriComponents uriComponents = fromController(builder, PersonControllerImpl.class).build();

		assertThat(uriComponents.toString()).isEqualTo("https://example.org:9090/base/people");
		assertThat(builder.toUriString()).isEqualTo("https://example.org:9090/base");
	}

	@Test
	void fromControllerWithCustomBaseUrlViaInstance() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://example.org:9090/base");
		MvcUriComponentsBuilder mvcBuilder = relativeTo(builder);
		UriComponents uriComponents = mvcBuilder.withController(PersonControllerImpl.class).build();

		assertThat(uriComponents.toString()).isEqualTo("https://example.org:9090/base/people");
		assertThat(builder.toUriString()).isEqualTo("https://example.org:9090/base");
	}

	@Test
	void fromControllerWithPlaceholder() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("test",
				Map.of("context.test.mapping", "people")));
		initWebApplicationContext(WebConfig.class, environment);
		UriComponents uriComponents = fromController(ConfigurablePersonController.class).build();
		assertThat(uriComponents.toUriString()).endsWith("/people");
	}

	@Test
	void fromControllerWithPlaceholderAndMissingValue() {
		StandardEnvironment environment = new StandardEnvironment();
		assertThat(environment.containsProperty("context.test.mapping")).isFalse();
		initWebApplicationContext(WebConfig.class, environment);
		UriComponents uriComponents = fromController(ConfigurablePersonController.class).build();
		assertThat(uriComponents.toUriString()).endsWith("/${context.test.mapping}");
	}

	@Test
	void fromControllerWithPlaceholderAndNoValueResolver() {
		UriComponents uriComponents = fromController(ConfigurablePersonController.class).build();
		assertThat(uriComponents.toUriString()).endsWith("/${context.test.mapping}");
	}

	@Test
	void usesForwardedHostAsHostIfHeaderIsSet() throws Exception {
		this.request.setScheme("https");
		this.request.addHeader("X-Forwarded-Host", "somethingDifferent");
		adaptRequestFromForwardedHeaders();
		UriComponents uriComponents = fromController(PersonControllerImpl.class).build();

		assertThat(uriComponents.toUriString()).startsWith("https://somethingDifferent");
	}

	@Test
	void usesForwardedHostAndPortFromHeader() throws Exception {
		this.request.setScheme("https");
		request.addHeader("X-Forwarded-Host", "foobar:8088");
		adaptRequestFromForwardedHeaders();
		UriComponents uriComponents = fromController(PersonControllerImpl.class).build();

		assertThat(uriComponents.toUriString()).startsWith("https://foobar:8088");
	}

	@Test
	void usesFirstHostOfXForwardedHost() throws Exception {
		this.request.setScheme("https");
		this.request.addHeader("X-Forwarded-Host", "barfoo:8888, localhost:8088");
		adaptRequestFromForwardedHeaders();
		UriComponents uriComponents = fromController(PersonControllerImpl.class).build();

		assertThat(uriComponents.toUriString()).startsWith("https://barfoo:8888");
	}

	// SPR-16668
	private void adaptRequestFromForwardedHeaders() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		new ForwardedHeaderFilter().doFilter(this.request, new MockHttpServletResponse(), chain);
		HttpServletRequest adaptedRequest = (HttpServletRequest) chain.getRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(adaptedRequest));
	}

	@Test
	void fromMethodNamePathVariable() {
		UriComponents uriComponents = fromMethodName(ControllerWithMethods.class,
				"methodWithPathVariable", "1").build();

		assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/something/1/foo");
	}

	@Test
	void fromMethodNameTypeLevelPathVariable() {
		this.request.setContextPath("/myapp");
		UriComponents uriComponents = fromMethodName(
				PersonsAddressesController.class, "getAddressesForCountry", "DE").buildAndExpand("1");

		assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/myapp/people/1/addresses/DE");
	}

	@Test
	void fromMethodNameTwoPathVariables() {
		UriComponents uriComponents = fromMethodName(
				ControllerWithMethods.class, "methodWithTwoPathVariables", 1, "2009-10-31").build();

		assertThat(uriComponents.getPath()).isEqualTo("/something/1/foo/2009-10-31");
	}

	@Test
	void fromMethodNameWithPathVarAndRequestParam() {
		UriComponents uriComponents = fromMethodName(
				ControllerWithMethods.class, "methodForNextPage", "1", 10, 5).build();

		assertThat(uriComponents.getPath()).isEqualTo("/something/1/foo");
		MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
		assertThat(queryParams.get("limit")).contains("5");
		assertThat(queryParams.get("offset")).contains("10");
	}

	@Test  // SPR-12977
	public void fromMethodNameWithBridgedMethod() {
		UriComponents uriComponents = fromMethodName(PersonCrudController.class, "get", (long) 42).build();

		assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/42");
	}

	@Test  // SPR-11391
	public void fromMethodNameTypeLevelPathVariableWithoutArgumentValue() {
		UriComponents uriComponents = fromMethodName(UserContactController.class, "showCreate", 123).build();

		assertThat(uriComponents.getPath()).isEqualTo("/user/123/contacts/create");
	}

	@Test
	void fromMethodNameInUnmappedController() {
		UriComponents uriComponents = fromMethodName(UnmappedController.class, "requestMappingMethod").build();

		assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/");
	}

	@Test  // gh-29897
	public void fromMethodNameInUnmappedControllerMethod() {
		UriComponents uriComponents = fromMethodName(UnmappedControllerMethod.class, "getMethod").build();

		assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/path");
	}

	@Test
	void fromMethodNameWithCustomBaseUrlViaStaticCall() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://example.org:9090/base");
		UriComponents uriComponents = fromMethodName(builder, ControllerWithMethods.class,
				"methodWithPathVariable", "1").build();

		assertThat(uriComponents.toString()).isEqualTo("https://example.org:9090/base/something/1/foo");
		assertThat(builder.toUriString()).isEqualTo("https://example.org:9090/base");
	}

	@Test
	void fromMethodNameWithCustomBaseUrlViaInstance() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://example.org:9090/base");
		MvcUriComponentsBuilder mvcBuilder = relativeTo(builder);
		UriComponents uriComponents = mvcBuilder.withMethodName(ControllerWithMethods.class,
				"methodWithPathVariable", "1").build();

		assertThat(uriComponents.toString()).isEqualTo("https://example.org:9090/base/something/1/foo");
		assertThat(builder.toUriString()).isEqualTo("https://example.org:9090/base");
	}

	@Test  // SPR-14405
	public void fromMethodNameWithOptionalParam() {
		UriComponents uriComponents = fromMethodName(ControllerWithMethods.class,
				"methodWithOptionalParam", new Object[] {null}).build();

		assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/something/optional-param");
	}

	@Test  // gh-22656
	public void fromMethodNameWithOptionalNamedParam() {
		UriComponents uriComponents = fromMethodName(ControllerWithMethods.class,
				"methodWithOptionalNamedParam", Optional.of("foo")).build();

		assertThat(uriComponents.toUriString())
				.isEqualTo("http://localhost/something/optional-param-with-name?search=foo");
	}

	@Test
	void fromMethodNameWithMetaAnnotation() {
		UriComponents uriComponents = fromMethodName(MetaAnnotationController.class, "handleInput").build();

		assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/input");
	}

	@Test
	void fromMethodNameConfigurablePath() {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("test",
				Map.of("method.test.mapping", "custom")));
		initWebApplicationContext(WebConfig.class, environment);
		UriComponents uriComponents = fromMethodName(ControllerWithMethods.class,
				"methodWithConfigurableMapping", "1").build();
		assertThat(uriComponents.toUriString()).isEqualTo("http://localhost/something/custom/1/foo");
	}

	@Test
	void fromMethodNameWithAnnotationsOnInterface() {
		initWebApplicationContext(WebConfig.class);
		UriComponents uriComponents = fromMethodName(HelloController.class, "get", "test").build();

		assertThat(uriComponents.toString()).isEqualTo("http://localhost/hello/test");
	}

	@Test
	void fromMethodCallOnSubclass() {
		UriComponents uriComponents = fromMethodCall(on(ExtendedController.class).myMethod(null)).build();

		assertThat(uriComponents.toUriString()).startsWith("http://localhost");
		assertThat(uriComponents.toUriString()).endsWith("/extended/else");
	}

	@Test
	void fromMethodCallPlain() {
		UriComponents uriComponents = fromMethodCall(on(ControllerWithMethods.class).myMethod(null)).build();

		assertThat(uriComponents.toUriString()).startsWith("http://localhost");
		assertThat(uriComponents.toUriString()).endsWith("/something/else");
	}

	@Test
	void fromMethodCallPlainWithNoArguments() {
		UriComponents uriComponents = fromMethodCall(on(ControllerWithMethods.class).myMethod()).build();

		assertThat(uriComponents.toUriString()).startsWith("http://localhost");
		assertThat(uriComponents.toUriString()).endsWith("/something/noarg");
	}

	@Test
	void fromMethodCallPlainOnInterface() {
		UriComponents uriComponents = fromMethodCall(on(ControllerInterface.class).myMethod(null)).build();

		assertThat(uriComponents.toUriString()).startsWith("http://localhost");
		assertThat(uriComponents.toUriString()).endsWith("/something/else");
	}

	@Test
	void fromMethodCallPlainWithNoArgumentsOnInterface() {
		UriComponents uriComponents = fromMethodCall(on(ControllerInterface.class).myMethod()).build();

		assertThat(uriComponents.toUriString()).startsWith("http://localhost");
		assertThat(uriComponents.toUriString()).endsWith("/something/noarg");
	}

	@Test
	void fromMethodCallWithTypeLevelUriVars() {
		UriComponents uriComponents = fromMethodCall(
				on(PersonsAddressesController.class).getAddressesForCountry("DE")).buildAndExpand(15);

		assertThat(uriComponents.toUriString()).endsWith("/people/15/addresses/DE");
	}

	@Test
	void fromMethodCallWithPathVariable() {
		UriComponents uriComponents = fromMethodCall(
				on(ControllerWithMethods.class).methodWithPathVariable("1")).build();

		assertThat(uriComponents.toUriString()).startsWith("http://localhost");
		assertThat(uriComponents.toUriString()).endsWith("/something/1/foo");
	}

	@Test
	void fromMethodCallWithPathVariableAndRequestParams() {
		UriComponents uriComponents = fromMethodCall(
				on(ControllerWithMethods.class).methodForNextPage("1", 10, 5)).build();

		assertThat(uriComponents.getPath()).isEqualTo("/something/1/foo");

		MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
		assertThat(queryParams.get("limit")).contains("5");
		assertThat(queryParams.get("offset")).contains("10");
	}

	@Test
	void fromMethodCallWithPathVariableAndMultiValueRequestParams() {
		UriComponents uriComponents = fromMethodCall(
				on(ControllerWithMethods.class).methodWithMultiValueRequestParams("1", Arrays.asList(3, 7), 5)).build();

		assertThat(uriComponents.getPath()).isEqualTo("/something/1/foo");

		MultiValueMap<String, String> queryParams = uriComponents.getQueryParams();
		assertThat(queryParams.get("limit")).contains("5");
		assertThat(queryParams.get("items")).containsExactly("3", "7");
	}

	@Test
	void fromMethodCallWithCustomBaseUrlViaStaticCall() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://example.org:9090/base");
		UriComponents uriComponents = fromMethodCall(builder, on(ControllerWithMethods.class).myMethod(null)).build();

		assertThat(uriComponents.toString()).isEqualTo("https://example.org:9090/base/something/else");
		assertThat(builder.toUriString()).isEqualTo("https://example.org:9090/base");
	}

	@Test
	void fromMethodCallWithCustomBaseUrlViaInstance() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("https://example.org:9090/base");
		MvcUriComponentsBuilder mvcBuilder = relativeTo(builder);
		UriComponents result = mvcBuilder.withMethodCall(on(ControllerWithMethods.class).myMethod(null)).build();

		assertThat(result.toString()).isEqualTo("https://example.org:9090/base/something/else");
		assertThat(builder.toUriString()).isEqualTo("https://example.org:9090/base");
	}

	@Test  // SPR-16710
	public void fromMethodCallWithModelAndViewReturnType() {
		UriComponents uriComponents = fromMethodCall(
				on(BookingControllerWithModelAndView.class).getBooking(21L)).buildAndExpand(42);

		assertThat(uriComponents.encode().toUri().toString()).isEqualTo("http://localhost/hotels/42/bookings/21");
	}

	@Test  // SPR-16710
	public void fromMethodCallWithObjectReturnType() {
		UriComponents uriComponents = fromMethodCall(
				on(BookingControllerWithObject.class).getBooking(21L)).buildAndExpand(42);

		assertThat(uriComponents.encode().toUri().toString()).isEqualTo("http://localhost/hotels/42/bookings/21");
	}

	@Test  // SPR-16710
	public void fromMethodCallWithStringReturnType() {
		assertThatIllegalStateException().isThrownBy(() -> {
				UriComponents uriComponents = fromMethodCall(
						on(BookingControllerWithString.class).getBooking(21L)).buildAndExpand(42);
				uriComponents.encode().toUri().toString();
		});
	}

	@Test  // SPR-16710
	public void fromMethodNameWithStringReturnType() {
		UriComponents uriComponents = fromMethodName(
				BookingControllerWithString.class, "getBooking", 21L).buildAndExpand(42);

		assertThat(uriComponents.encode().toUri().toString()).isEqualTo("http://localhost/hotels/42/bookings/21");
	}

	@Test  // gh-30210
	public void fromMethodCallWithCharSequenceReturnType() {
		UriComponents uriComponents = fromMethodCall(
				on(BookingControllerWithCharSequence.class).getBooking(21L)).buildAndExpand(42);

		assertThat(uriComponents.encode().toUri().toString()).isEqualTo("http://localhost/hotels/42/bookings/21");
	}

	@Test  // gh-30210
	public void fromMethodCallWithJdbc30115ReturnType() {
		UriComponents uriComponents = fromMethodCall(
				on(BookingControllerWithJdbcSavepoint.class).getBooking(21L)).buildAndExpand(42);

		assertThat(uriComponents.encode().toUri().toString()).isEqualTo("http://localhost/hotels/42/bookings/21");
	}

	@Test
	void fromMappingNamePlain() {
		initWebApplicationContext(WebConfig.class);

		this.request.setServerName("example.org");
		this.request.setServerPort(9999);
		this.request.setContextPath("/base");

		String mappingName = "PAC#getAddressesForCountry";
		String url = fromMappingName(mappingName).arg(0, "DE").buildAndExpand(123);
		assertThat(url).isEqualTo("/base/people/123/addresses/DE");
	}

	@Test
	void fromMappingNameWithCustomBaseUrl() {
		initWebApplicationContext(WebConfig.class);

		UriComponentsBuilder baseUrl = UriComponentsBuilder.fromUriString("https://example.org:9999/base");
		MvcUriComponentsBuilder mvcBuilder = relativeTo(baseUrl);
		String url = mvcBuilder.withMappingName("PAC#getAddressesForCountry").arg(0, "DE").buildAndExpand(123);
		assertThat(url).isEqualTo("https://example.org:9999/base/people/123/addresses/DE");
	}

	@Test  // SPR-17027
	public void fromMappingNameWithEncoding() {
		initWebApplicationContext(WebConfig.class);

		this.request.setServerName("example.org");
		this.request.setServerPort(9999);
		this.request.setContextPath("/base");

		String mappingName = "PAC#getAddressesForCountry";
		String url = fromMappingName(mappingName).arg(0, "DE;FR").encode().buildAndExpand("_+_");
		assertThat(url).isEqualTo("/base/people/_%2B_/addresses/DE%3BFR");
	}

	@Test
	void fromMappingNameWithPathWithoutLeadingSlash() {
		initWebApplicationContext(PathWithoutLeadingSlashConfig.class);

		this.request.setServerName("example.org");
		this.request.setServerPort(9999);
		this.request.setContextPath("/base");

		String mappingName = "PWLSC#getAddressesForCountry";
		String url = fromMappingName(mappingName).arg(0, "DE;FR").encode().buildAndExpand("_+_");
		assertThat(url).isEqualTo("/base/people/DE%3BFR");
	}

	@Test
	void fromControllerWithPrefix() {
		initWebApplicationContext(PathPrefixWebConfig.class);

		this.request.setScheme("https");
		this.request.setServerName("example.org");
		this.request.setServerPort(9999);
		this.request.setContextPath("/base");

		assertThat(fromController(PersonsAddressesController.class).buildAndExpand("123").toString())
				.isEqualTo("https://example.org:9999/base/api/people/123/addresses");
	}

	@Test
	void fromMethodWithPrefix() {
		initWebApplicationContext(PathPrefixWebConfig.class);

		this.request.setScheme("https");
		this.request.setServerName("example.org");
		this.request.setServerPort(9999);
		this.request.setContextPath("/base");

		String url = fromMethodCall(on(PersonsAddressesController.class)
				.getAddressesForCountry("DE"))
				.buildAndExpand("123")
				.toString();

		assertThat(url).isEqualTo("https://example.org:9999/base/api/people/123/addresses/DE");
	}

	private void initWebApplicationContext(Class<?> configClass) {
		initWebApplicationContext(configClass, null);
	}

	private void initWebApplicationContext(Class<?> configClass, @Nullable ConfigurableEnvironment environment) {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		if (environment != null) {
			context.setEnvironment(environment);
		}
		context.setServletContext(new MockServletContext());
		context.register(configClass);
		context.refresh();
		this.request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
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


	static class PersonControllerImpl implements PersonController {
	}


	@Controller
	@RequestMapping("/people/{id}/addresses")
	static class PersonsAddressesController {

		@RequestMapping("/{country}")
		HttpEntity<Void> getAddressesForCountry(@PathVariable String country) {
			return null;
		}
	}

	@Controller
	@RequestMapping({"people"})
	static class PathWithoutLeadingSlashController {

		@RequestMapping("/{country}")
		HttpEntity<Void> getAddressesForCountry(@PathVariable String country) {
			return null;
		}
	}

	@RequestMapping({"/persons", "/people"})
	private static class InvalidController {
	}


	@RequestMapping("/${context.test.mapping}")
	interface ConfigurablePersonController {
	}


	private static class UnmappedController {

		@RequestMapping
		public void requestMappingMethod() {
		}
	}


	@RequestMapping("/path")
	private static class UnmappedControllerMethod {

		@GetMapping
		public void getMethod() {
		}
	}


	@RequestMapping("/something")
	static class ControllerWithMethods {

		@RequestMapping("/else")
		HttpEntity<Void> myMethod(@RequestBody Object payload) {
			return null;
		}

		@RequestMapping("/noarg")
		HttpEntity<Void> myMethod() {
			return null;
		}

		@RequestMapping("/{id}/foo")
		HttpEntity<Void> methodWithPathVariable(@PathVariable String id) {
			return null;
		}

		@RequestMapping("/{id}/foo/{date}")
		HttpEntity<Void> methodWithTwoPathVariables(
				@PathVariable Integer id, @DateTimeFormat(iso = ISO.DATE) @PathVariable Date date) {
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

		@GetMapping("/optional-param-with-name")
		HttpEntity<Void> methodWithOptionalNamedParam(@RequestParam("search") Optional<String> q) {
			return null;
		}

		@RequestMapping("/${method.test.mapping}/{id}/foo")
		HttpEntity<Void> methodWithConfigurableMapping(@PathVariable String id) {
			return null;
		}
	}


	@RequestMapping("/extended")
	@SuppressWarnings("WeakerAccess")
	static class ExtendedController extends ControllerWithMethods {
	}


	@RequestMapping("/something")
	public interface ControllerInterface {

		@RequestMapping("/else")
		HttpEntity<Void> myMethod(@RequestBody Object payload);

		@RequestMapping("/noarg")
		HttpEntity<Void> myMethod();
	}


	@RequestMapping("/user/{userId}/contacts")
	static class UserContactController {

		@RequestMapping("/create")
		public String showCreate(@PathVariable Integer userId) {
			return null;
		}
	}


	abstract static class AbstractCrudController<T, ID> {

		abstract T get(ID id);
	}


	static class PersonCrudController extends AbstractCrudController<Person, Long> {

		@Override
		@RequestMapping(path = "/{id}", method = RequestMethod.GET)
		public Person get(@PathVariable Long id) {
			return new Person();
		}
	}


	@Controller
	static class MetaAnnotationController {

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

		@AliasFor(annotation = RequestMapping.class)
		String[] path() default {};
	}


	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Bean
		public PersonsAddressesController controller() {
			return new PersonsAddressesController();
		}
	}


	@EnableWebMvc
	static class PathWithoutLeadingSlashConfig implements WebMvcConfigurer {

		@Bean
		public PathWithoutLeadingSlashController controller() {
			return new PathWithoutLeadingSlashController();
		}
	}


	@EnableWebMvc
	static class PathPrefixWebConfig implements WebMvcConfigurer {

		@Override
		public void configurePathMatch(PathMatchConfigurer configurer) {
			configurer.addPathPrefix("/api", PersonsAddressesController.class::equals);
		}

		@Bean
		public PersonsAddressesController controller() {
			return new PersonsAddressesController();
		}
	}


	@Controller
	@RequestMapping("/hotels/{hotel}")
	static class BookingControllerWithModelAndView {

		@GetMapping("/bookings/{booking}")
		public ModelAndView getBooking(@PathVariable Long booking) {
			return new ModelAndView("url");
		}
	}


	@Controller
	@RequestMapping("/hotels/{hotel}")
	static class BookingControllerWithObject {

		@GetMapping("/bookings/{booking}")
		public Object getBooking(@PathVariable Long booking) {
			return "url";
		}
	}


	@Controller
	@RequestMapping("/hotels/{hotel}")
	static class BookingControllerWithString {

		@GetMapping("/bookings/{booking}")
		public String getBooking(@PathVariable Long booking) {
			return "url";
		}
	}


	@Controller
	@RequestMapping("/hotels/{hotel}")
	static class BookingControllerWithCharSequence {

		@GetMapping("/bookings/{booking}")
		public CharSequence getBooking(@PathVariable Long booking) {
			return "url";
		}
	}


	@Controller
	@RequestMapping("/hotels/{hotel}")
	static class BookingControllerWithJdbcSavepoint {

		@GetMapping("/bookings/{booking}")
		public Savepoint getBooking(@PathVariable Long booking) {
			return null;
		}
	}


	interface HelloInterface {

		@GetMapping("/hello/{name}")
		ResponseEntity<String> get(@PathVariable String name);
	}


	@Controller
	static class HelloController implements HelloInterface {

		@Override
		public ResponseEntity<String> get(String name) {
			return ResponseEntity.ok("Hello " + name);
		}
	}

}
