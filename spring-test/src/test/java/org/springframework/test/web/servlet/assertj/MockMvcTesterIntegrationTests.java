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

package org.springframework.test.web.servlet.assertj;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link MockMvcTester}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
@SpringJUnitConfig
@WebAppConfiguration
public class MockMvcTesterIntegrationTests {

	private final MockMvcTester mockMvc;

	MockMvcTesterIntegrationTests(WebApplicationContext wac) {
		this.mockMvc = MockMvcTester.from(wac);
	}

	@Nested
	class RequestTests {

		@Test
		void hasAsyncStartedTrue() {
			assertThat(perform(get("/callable").accept(MediaType.APPLICATION_JSON)))
					.request().hasAsyncStarted(true);
		}

		@Test
		void hasAsyncStartedFalse() {
			assertThat(perform(get("/greet"))).request().hasAsyncStarted(false);
		}

		@Test
		void attributes() {
			assertThat(perform(get("/greet"))).request().attributes()
					.containsKey(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}

		@Test
		void sessionAttributes() {
			assertThat(perform(get("/locale"))).request().sessionAttributes()
					.containsOnly(entry("locale", Locale.UK));
		}
	}

	@Nested
	class CookieTests {

		@Test
		void containsCookie() {
			Cookie cookie = new Cookie("test", "value");
			assertThat(performWithCookie(cookie, get("/greet"))).cookies().containsCookie("test");
		}

		@Test
		void hasValue() {
			Cookie cookie = new Cookie("test", "value");
			assertThat(performWithCookie(cookie, get("/greet"))).cookies().hasValue("test", "value");
		}

		private MvcTestResult performWithCookie(Cookie cookie, MockHttpServletRequestBuilder request) {
			MockMvcTester mockMvc = MockMvcTester.of(List.of(new TestController()), builder -> builder.addInterceptors(
					new HandlerInterceptor() {
						@Override
						public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
							response.addCookie(cookie);
							return true;
						}
					}).build());
			return mockMvc.perform(request);
		}
	}

	@Nested
	class StatusTests {

		@Test
		void statusOk() {
			assertThat(perform(get("/greet"))).hasStatusOk();
		}

		@Test
		void statusSeries() {
			assertThat(perform(get("/greet"))).hasStatus2xxSuccessful();
		}

	}

	@Nested
	class HeadersTests {

		@Test
		void shouldAssertHeader() {
			assertThat(perform(get("/greet")))
					.hasHeader("Content-Type", "text/plain;charset=ISO-8859-1");
		}

		@Test
		void shouldAssertHeaderWithCallback() {
			assertThat(perform(get("/greet"))).headers().satisfies(textContent("ISO-8859-1"));
		}

		private Consumer<HttpHeaders> textContent(String charset) {
			return headers -> assertThat(headers).containsEntry(
					"Content-Type", List.of("text/plain;charset=%s".formatted(charset)));
		}

	}

	@Nested
	class ModelAndViewTests {

		@Test
		void hasViewName() {
			assertThat(perform(get("/persons/{0}", "Andy"))).hasViewName("persons/index");
		}

		@Test
		void viewNameWithCustomAssertion() {
			assertThat(perform(get("/persons/{0}", "Andy"))).viewName().startsWith("persons");
		}

		@Test
		void containsAttributes() {
			assertThat(perform(post("/persons").param("name", "Andy"))).model()
					.containsOnlyKeys("name").containsEntry("name", "Andy");
		}

		@Test
		void hasErrors() {
			assertThat(perform(post("/persons"))).model().hasErrors();
		}

		@Test
		void hasAttributeErrors() {
			assertThat(perform(post("/persons"))).model().hasAttributeErrors("person");
		}

		@Test
		void hasAttributeErrorsCount() {
			assertThat(perform(post("/persons"))).model().extractingBindingResult("person").hasErrorsCount(1);
		}

	}

	@Nested
	class FlashTests {

		@Test
		void containsAttributes() {
			assertThat(perform(post("/persons").param("name", "Andy"))).flash()
					.containsOnlyKeys("message").hasEntrySatisfying("message",
							value -> assertThat(value).isInstanceOfSatisfying(String.class,
									stringValue -> assertThat(stringValue).startsWith("success")));
		}
	}

	@Nested
	class BodyTests {

		@Test
		void asyncResult() {
			assertThat(perform(get("/callable").accept(MediaType.APPLICATION_JSON)))
					.asyncResult().asInstanceOf(map(String.class, Object.class))
					.containsOnly(entry("key", "value"));
		}

		@Test
		void stringContent() {
			assertThat(perform(get("/greet"))).body().asString().isEqualTo("hello");
		}

		@Test
		void jsonPathContent() {
			assertThat(perform(get("/message"))).bodyJson()
					.extractingPath("$.message").asString().isEqualTo("hello");
		}

		@Test
		void jsonContentCanLoadResourceFromClasspath() {
			assertThat(perform(get("/message"))).bodyJson().isLenientlyEqualTo(
					new ClassPathResource("message.json", MockMvcTesterIntegrationTests.class));
		}

		@Test
		void jsonContentUsingResourceLoaderClass() {
			assertThat(perform(get("/message"))).bodyJson().withResourceLoadClass(MockMvcTesterIntegrationTests.class)
					.isLenientlyEqualTo("message.json");
		}

	}

	@Nested
	class HandlerTests {

		@Test
		void handlerOn404() {
			assertThat(perform(get("/unknown-resource"))).handler().isNull();
		}

		@Test
		void hasType() {
			assertThat(perform(get("/greet"))).handler().hasType(TestController.class);
		}

		@Test
		void isMethodHandler() {
			assertThat(perform(get("/greet"))).handler().isMethodHandler();
		}

		@Test
		void isInvokedOn() {
			assertThat(perform(get("/callable"))).handler()
					.isInvokedOn(AsyncController.class, AsyncController::getCallable);
		}

	}

	@Nested
	class ExceptionTests {

		@Test
		void doesNotHaveUnresolvedException() {
			assertThat(perform(get("/greet"))).doesNotHaveUnresolvedException();
		}

		@Test
		void hasUnresolvedException() {
			assertThat(perform(get("/error/1"))).hasUnresolvedException();
		}

		@Test
		void doesNotHaveUnresolvedExceptionWithUnresolvedException() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(perform(get("/error/1"))).doesNotHaveUnresolvedException())
					.withMessage("Expecting request to have succeeded but it has failed");
		}

		@Test
		void hasUnresolvedExceptionWithoutUnresolvedException() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(perform(get("/greet"))).hasUnresolvedException())
					.withMessage("Expecting request to have failed but it has succeeded");
		}

		@Test
		void unresolvedExceptionWithFailedRequest() {
			assertThat(perform(get("/error/1"))).unresolvedException()
					.isInstanceOf(ServletException.class)
					.cause().isInstanceOf(IllegalStateException.class).hasMessage("Expected");
		}

		@Test
		void unresolvedExceptionWithSuccessfulRequest() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(perform(get("/greet"))).unresolvedException())
					.withMessage("Expecting request to have failed but it has succeeded");
		}

		// Check that assertions fail immediately if request has failed with unresolved exception

		@Test
		void assertAndApplyWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).apply(mvcResult -> {}));
		}

		@Test
		void assertAsyncResultWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).asyncResult());
		}

		@Test
		void assertContentTypeWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).contentType());
		}

		@Test
		void assertCookiesWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).cookies());
		}

		@Test
		void assertFlashWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).flash());
		}

		@Test
		void assertStatusWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).hasStatus(3));
		}

		@Test
		void assertHeaderWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).headers());
		}

		@Test
		void assertViewNameWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).hasViewName("test"));
		}

		@Test
		void assertForwardedUrlWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).hasForwardedUrl("test"));
		}

		@Test
		void assertRedirectedUrlWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).hasRedirectedUrl("test"));
		}

		@Test
		void assertRequestWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).request());
		}

		@Test
		void assertModelWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).model());
		}

		@Test
		void assertBodyWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).body());
		}


		private void testAssertionFailureWithUnresolvableException(Consumer<MvcTestResult> assertions) {
			MvcTestResult result = perform(get("/error/1"));
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertions.accept(result))
					.withMessageContainingAll("Request has failed unexpectedly:",
							ServletException.class.getName(), IllegalStateException.class.getName(),
							"Expected");
		}

	}

	@Test
	void hasForwardUrl() {
		assertThat(perform(get("/persons/John"))).hasForwardedUrl("persons/index");
	}

	@Test
	void hasRedirectUrl() {
		assertThat(perform(post("/persons").param("name", "Andy"))).hasStatus(HttpStatus.FOUND)
				.hasRedirectedUrl("/persons/Andy");
	}

	@Test
	void satisfiesAllowsAdditionalAssertions() {
		assertThat(this.mockMvc.perform(get("/greet"))).satisfies(result -> {
			assertThat(result).isInstanceOf(MvcTestResult.class);
			assertThat(result).hasStatusOk();
		});
	}

	@Test
	void resultMatcherCanBeReused() {
		assertThat(this.mockMvc.perform(get("/greet"))).matches(status().isOk());
	}

	@Test
	void resultMatcherFailsWithDedicatedException() {
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(this.mockMvc.perform(get("/greet")))
						.matches(status().isNotFound()))
				.withMessageContaining("Status expected:<404> but was:<200>");
	}

	@Test
	void shouldApplyResultHandler() { // Spring RESTDocs example
		AtomicBoolean applied = new AtomicBoolean();
		assertThat(this.mockMvc.perform(get("/greet"))).apply(result -> applied.set(true));
		assertThat(applied).isTrue();
	}


	private MvcTestResult perform(MockHttpServletRequestBuilder builder) {
		return this.mockMvc.perform(builder);
	}


	@Configuration
	@EnableWebMvc
	@Import({ TestController.class, PersonController.class, AsyncController.class,
			SessionController.class, ErrorController.class })
	static class WebConfiguration {
	}

	@RestController
	static class TestController {

		@GetMapping(path = "/greet", produces = "text/plain")
		String greet() {
			return "hello";
		}

		@GetMapping(path = "/message", produces = MediaType.APPLICATION_JSON_VALUE)
		String message() {
			return "{\"message\": \"hello\"}";
		}
	}

	@Controller
	@RequestMapping("/persons")
	static class PersonController {

		@GetMapping("/{name}")
		public String get(@PathVariable String name, Model model) {
			model.addAttribute(new Person(name));
			return "persons/index";
		}

		@PostMapping
		String create(@Valid Person person, Errors errors, RedirectAttributes redirectAttrs) {
			if (errors.hasErrors()) {
				return "persons/add";
			}
			redirectAttrs.addAttribute("name", person.getName());
			redirectAttrs.addFlashAttribute("message", "success!");
			return "redirect:/persons/{name}";
		}
	}

	@RestController
	static class AsyncController {

		@GetMapping("/callable")
		public Callable<Map<String, String>> getCallable() {
			return () -> Collections.singletonMap("key", "value");
		}
	}

	@Controller
	@SessionAttributes("locale")
	private static class SessionController {

		@ModelAttribute
		void populate(Model model) {
			model.addAttribute("locale", Locale.UK);
		}

		@RequestMapping("/locale")
		String handle() {
			return "view";
		}
	}

	@Controller
	private static class ErrorController {

		@GetMapping("/error/1")
		public String one() {
			throw new IllegalStateException("Expected");
		}

		@GetMapping("/error/validation/{id}")
		public String validation(@PathVariable @Size(max = 4) String id) {
			return "Hello " + id;
		}
	}

}
