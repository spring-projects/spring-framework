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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
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
import jakarta.servlet.http.Part;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMultipartMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Integration tests for {@link MockMvcTester}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 */
@SpringJUnitWebConfig
public class MockMvcTesterIntegrationTests {

	private static final MockMultipartFile file = new MockMultipartFile("file", "content.txt", null,
			"value".getBytes(StandardCharsets.UTF_8));


	private final MockMvcTester mvc;

	MockMvcTesterIntegrationTests(WebApplicationContext wac) {
		this.mvc = MockMvcTester.from(wac);
	}

	@Nested
	class PerformTests {

		@Test
		void syncRequestWithDefaultExchange() {
			assertThat(mvc.get().uri("/greet")).hasStatusOk();
		}

		@Test
		void asyncRequestWithDefaultExchange() {
			assertThat(mvc.get().uri("/streaming").param("timeToWait", "100")).hasStatusOk()
					.hasBodyTextEqualTo("name=Joe&someBoolean=true");
		}

		@Test
		void asyncMultipartRequestWithDefaultExchange() {
			assertThat(mvc.post().uri("/multipart-streaming").multipart()
					.file(file).param("timeToWait", "100"))
					.hasStatusOk().hasBodyTextEqualTo("name=Joe&file=content.txt");
		}

		@Test
		void syncRequestWithExplicitExchange() {
			assertThat(mvc.get().uri("/greet").exchange()).hasStatusOk();
		}

		@Test
		void asyncRequestWithExplicitExchange() {
			assertThat(mvc.get().uri("/streaming").param("timeToWait", "100").exchange())
					.hasStatusOk().hasBodyTextEqualTo("name=Joe&someBoolean=true");
		}

		@Test
		void asyncMultipartRequestWitExplicitExchange() {
			assertThat(mvc.post().uri("/multipart-streaming").multipart()
					.file(file).param("timeToWait", "100").exchange())
					.hasStatusOk().hasBodyTextEqualTo("name=Joe&file=content.txt");
		}

		@Test
		void syncRequestWithExplicitExchangeIgnoresDuration() {
			Duration timeToWait = mock(Duration.class);
			assertThat(mvc.get().uri("/greet").exchange(timeToWait)).hasStatusOk();
			verifyNoInteractions(timeToWait);
		}

		@Test
		void asyncRequestWithExplicitExchangeAndEnoughTimeToWait() {
			assertThat(mvc.get().uri("/streaming").param("timeToWait", "100").exchange(Duration.ofMillis(200)))
					.hasStatusOk().hasBodyTextEqualTo("name=Joe&someBoolean=true");
		}

		@Test
		void asyncMultipartRequestWithExplicitExchangeAndEnoughTimeToWait() {
			assertThat(mvc.post().uri("/multipart-streaming").multipart()
					.file(file).param("timeToWait", "100").exchange(Duration.ofMillis(200)))
					.hasStatusOk().hasBodyTextEqualTo("name=Joe&file=content.txt");
		}

		@Test
		void asyncRequestWithExplicitExchangeAndNotEnoughTimeToWait() {
			MockMvcRequestBuilder builder = mvc.get().uri("/streaming").param("timeToWait", "500");
			assertThatIllegalStateException()
					.isThrownBy(() -> builder.exchange(Duration.ofMillis(100)))
					.withMessageContaining("was not set during the specified timeToWait=100");
		}

		@Test
		void asyncMultipartRequestWithExplicitExchangeAndNotEnoughTimeToWait() {
			MockMultipartMvcRequestBuilder builder = mvc.post().uri("/multipart-streaming").multipart()
					.file(file).param("timeToWait", "500");
			assertThatIllegalStateException()
					.isThrownBy(() -> builder.exchange(Duration.ofMillis(100)))
					.withMessageContaining("was not set during the specified timeToWait=100");
		}
	}

	@Nested
	class RequestTests {

		@Test
		void hasAsyncStartedTrue() {
			assertThat(mvc.get().uri("/callable").accept(MediaType.APPLICATION_JSON).asyncExchange())
					.request().hasAsyncStarted(true);
		}

		@Test
		void hasAsyncStartedForMultipartTrue() {
			assertThat(mvc.post().uri("/multipart-streaming").multipart()
					.file(file).param("timeToWait", "100").asyncExchange())
					.request().hasAsyncStarted(true);
		}

		@Test
		void hasAsyncStartedFalse() {
			assertThat(mvc.get().uri("/greet").asyncExchange()).request().hasAsyncStarted(false);
		}

		@Test
		void hasAsyncStartedForMultipartFalse() {
			assertThat(mvc.put().uri("/multipart-put").multipart().file(file).asyncExchange())
					.request().hasAsyncStarted(false);
		}

		@Test
		void attributes() {
			assertThat(mvc.get().uri("/greet")).request().attributes()
					.containsKey(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}

		@Test
		void sessionAttributes() {
			assertThat(mvc.get().uri("/locale")).request().sessionAttributes()
					.containsOnly(entry("locale", Locale.UK));
		}
	}

	@Nested
	class MultipartTests {

		private final MockMultipartFile JSON_PART_FILE = new MockMultipartFile("json", "json", "application/json", """
				{
					"name": "test"
				}""".getBytes(StandardCharsets.UTF_8));

		@Test
		void multipartWithPut() {
			assertThat(mvc.put().uri("/multipart-put").multipart().file(file).file(JSON_PART_FILE))
					.hasStatusOk()
					.hasViewName("index")
					.model().contains(entry("name", "file"));
		}

		@Test
		void multipartWithMissingPart() {
			assertThat(mvc.put().uri("/multipart-put").multipart().file(JSON_PART_FILE))
					.hasStatus(HttpStatus.BAD_REQUEST)
					.failure().isInstanceOfSatisfying(MissingServletRequestPartException.class,
							ex -> assertThat(ex.getRequestPartName()).isEqualTo("file"));
		}

		@Test
		void multipartWithNamedPart() {
			MockPart part = new MockPart("part", "content.txt", "value".getBytes(StandardCharsets.UTF_8));
			assertThat(mvc.post().uri("/part").multipart().part(part).file(JSON_PART_FILE))
					.hasStatusOk()
					.hasViewName("index")
					.model().contains(entry("part", "content.txt"), entry("name", "test"));
		}
	}

	@Nested
	class CookieTests {

		@Test
		void containsCookie() {
			Cookie cookie = new Cookie("test", "value");
			assertThat(withCookie(cookie).get().uri("/greet")).cookies().containsCookie("test");
		}

		@Test
		void hasValue() {
			Cookie cookie = new Cookie("test", "value");
			assertThat(withCookie(cookie).get().uri("/greet")).cookies().hasValue("test", "value");
		}

		private MockMvcTester withCookie(Cookie cookie) {
			return MockMvcTester.of(List.of(new TestController()), builder -> builder.addInterceptors(
					new HandlerInterceptor() {
						@Override
						public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
							response.addCookie(cookie);
							return true;
						}
					}).build());
		}
	}

	@Nested
	class StatusTests {

		@Test
		void statusOk() {
			assertThat(mvc.get().uri("/greet")).hasStatusOk();
		}

		@Test
		void statusSeries() {
			assertThat(mvc.get().uri("/greet")).hasStatus2xxSuccessful();
		}
	}

	@Nested
	class HeadersTests {

		@Test
		void shouldAssertHeader() {
			assertThat(mvc.get().uri("/greet"))
					.hasHeader("Content-Type", "text/plain;charset=ISO-8859-1");
		}

		@Test
		void shouldAssertHeaderWithCallback() {
			assertThat(mvc.get().uri("/greet")).headers().satisfies(textContent("ISO-8859-1"));
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
			assertThat(mvc.get().uri("/persons/{0}", "Andy")).hasViewName("persons/index");
		}

		@Test
		void viewNameWithCustomAssertion() {
			assertThat(mvc.get().uri("/persons/{0}", "Andy")).viewName().startsWith("persons");
		}

		@Test
		void containsAttributes() {
			assertThat(mvc.post().uri("/persons").param("name", "Andy")).model()
					.containsOnlyKeys("name").containsEntry("name", "Andy");
		}

		@Test
		void hasErrors() {
			assertThat(mvc.post().uri("/persons")).model().hasErrors();
		}

		@Test
		void hasAttributeErrors() {
			assertThat(mvc.post().uri("/persons")).model().hasAttributeErrors("person");
		}

		@Test
		void hasAttributeErrorsCount() {
			assertThat(mvc.post().uri("/persons")).model().extractingBindingResult("person").hasErrorsCount(1);
		}
	}

	@Nested
	class FlashTests {

		@Test
		void containsAttributes() {
			assertThat(mvc.post().uri("/persons").param("name", "Andy")).flash()
					.containsOnlyKeys("message").hasEntrySatisfying("message",
							value -> assertThat(value).isInstanceOfSatisfying(String.class,
									stringValue -> assertThat(stringValue).startsWith("success")));
		}
	}

	@Nested
	class BodyTests {

		@Test
		void asyncResult() {
			MvcTestResult result = mvc.get().uri("/callable").accept(MediaType.APPLICATION_JSON).asyncExchange();
			assertThat(result.getMvcResult().getAsyncResult())
					.asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
					.containsOnly(entry("key", "value"));
		}

		@Test
		void stringContent() {
			assertThat(mvc.get().uri("/greet")).body().asString().isEqualTo("hello");
		}

		@Test
		void jsonPathContent() {
			assertThat(mvc.get().uri("/message")).bodyJson()
					.extractingPath("$.message").asString().isEqualTo("hello");
		}

		@Test
		void jsonContentCanLoadResourceFromClasspath() {
			assertThat(mvc.get().uri("/message")).bodyJson().isLenientlyEqualTo(
					new ClassPathResource("message.json", MockMvcTesterIntegrationTests.class));
		}

		@Test
		void jsonContentUsingResourceLoaderClass() {
			assertThat(mvc.get().uri("/message")).bodyJson().withResourceLoadClass(MockMvcTesterIntegrationTests.class)
					.isLenientlyEqualTo("message.json");
		}
	}

	@Nested
	class HandlerTests {

		@Test
		void handlerOn404() {
			assertThat(mvc.get().uri("/unknown-resource")).handler().isNull();
		}

		@Test
		void hasType() {
			assertThat(mvc.get().uri("/greet")).handler().hasType(TestController.class);
		}

		@Test
		void isMethodHandler() {
			assertThat(mvc.get().uri("/greet")).handler().isMethodHandler();
		}

		@Test
		void isInvokedOn() {
			assertThat(mvc.get().uri("/callable")).handler()
					.isInvokedOn(AsyncController.class, AsyncController::getCallable);
		}
	}

	@Nested
	class DebugTests {

		private final PrintStream standardOut = System.out;

		private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();

		@BeforeEach
		public void setUp() {
			System.setOut(new PrintStream(capturedOut));
		}

		@AfterEach
		public void tearDown() {
			System.setOut(standardOut);
		}

		@Test
		void debugUsesSystemOutByDefault() {
			assertThat(mvc.get().uri("/greet")).debug().hasStatusOk();
			assertThat(capturedOut()).contains("MockHttpServletRequest:", "MockHttpServletResponse:");
		}

		@Test
		void debugCanPrintToCustomOutputStream() {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			assertThat(mvc.get().uri("/greet")).debug(out).hasStatusOk();
			assertThat(out.toString(StandardCharsets.UTF_8))
					.contains("MockHttpServletRequest:", "MockHttpServletResponse:");
			assertThat(capturedOut()).isEmpty();
		}

		@Test
		void debugCanPrintToCustomWriter() {
			StringWriter out = new StringWriter();
			assertThat(mvc.get().uri("/greet")).debug(out).hasStatusOk();
			assertThat(out.toString())
					.contains("MockHttpServletRequest:", "MockHttpServletResponse:");
			assertThat(capturedOut()).isEmpty();
		}

		private String capturedOut() {
			return this.capturedOut.toString(StandardCharsets.UTF_8);
		}

	}

	@Nested
	class ExceptionTests {

		@Test
		void hasFailedWithUnresolvedException() {
			assertThat(mvc.get().uri("/error/1")).hasFailed();
		}

		@Test
		void hasFailedWithResolvedException() {
			assertThat(mvc.get().uri("/error/2")).hasFailed().hasStatus(HttpStatus.PAYMENT_REQUIRED);
		}

		@Test
		void doesNotHaveFailedWithoutException() {
			assertThat(mvc.get().uri("/greet")).doesNotHaveFailed();
		}

		@Test
		void doesNotHaveFailedWithUnresolvedException() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(mvc.get().uri("/error/1")).doesNotHaveFailed())
					.withMessage("Expected request to succeed, but it failed");
		}

		@Test
		void doesNotHaveFailedWithResolvedException() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(mvc.get().uri("/error/2")).doesNotHaveFailed())
					.withMessage("Expected request to succeed, but it failed");
		}

		@Test
		void hasFailedWithoutException() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(mvc.get().uri("/greet")).hasFailed())
					.withMessage("Expected request to fail, but it succeeded");
		}

		@Test
		void failureWithUnresolvedException() {
			assertThat(mvc.get().uri("/error/1")).failure()
					.isInstanceOf(ServletException.class)
					.cause().isInstanceOf(IllegalStateException.class).hasMessage("Expected");
		}

		@Test
		void failureWithResolvedException() {
			assertThat(mvc.get().uri("/error/2")).failure()
					.isInstanceOfSatisfying(ResponseStatusException.class, ex ->
							assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED));
		}

		@Test
		void failureWithoutException() {
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertThat(mvc.get().uri("/greet")).failure())
					.withMessage("Expected request to fail, but it succeeded");
		}

		// Check that assertions fail immediately if request failed with unresolved exception

		@Test
		void assertAndApplyWithUnresolvedException() {
			testAssertionFailureWithUnresolvableException(
					result -> assertThat(result).apply(mvcResult -> {}));
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
			MvcTestResult result = mvc.get().uri("/error/1").exchange();
			assertThatExceptionOfType(AssertionError.class)
					.isThrownBy(() -> assertions.accept(result))
					.withMessageContainingAll("Request failed unexpectedly:",
							ServletException.class.getName(), IllegalStateException.class.getName(),
							"Expected");
		}
	}

	@Test
	void hasForwardUrl() {
		assertThat(mvc.get().uri("/persons/John")).hasForwardedUrl("persons/index");
	}

	@Test
	void hasRedirectUrl() {
		assertThat(mvc.post().uri("/persons").param("name", "Andy")).hasStatus(HttpStatus.FOUND)
				.hasRedirectedUrl("/persons/Andy");
	}

	@Test
	void satisfiesAllowsAdditionalAssertions() {
		assertThat(mvc.get().uri("/greet")).satisfies(result -> {
			assertThat(result).isInstanceOf(MvcTestResult.class);
			assertThat(result).hasStatusOk();
		});
	}

	@Test
	void resultMatcherCanBeReused() throws Exception {
		MvcTestResult result = mvc.get().uri("/greet").exchange();
		ResultMatcher matcher = mock(ResultMatcher.class);
		assertThat(result).matches(matcher);
		verify(matcher).match(result.getMvcResult());
	}

	@Test
	void resultMatcherFailsWithDedicatedException() {
		ResultMatcher matcher = result -> assertThat(result.getResponse().getStatus())
				.isEqualTo(HttpStatus.NOT_FOUND.value());
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertThat(mvc.get().uri("/greet")).matches(matcher))
				.withMessageContaining("expected: 404").withMessageContaining(" but was: 200");
	}

	@Test
	void shouldApplyResultHandler() { // Spring RESTDocs example
		AtomicBoolean applied = new AtomicBoolean();
		assertThat(mvc.get().uri("/greet")).apply(result -> applied.set(true));
		assertThat(applied).isTrue();
	}


	@Configuration
	@EnableWebMvc
	@Import({ TestController.class, PersonController.class, AsyncController.class,
			MultipartController.class, SessionController.class, ErrorController.class })
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

		@GetMapping("/streaming")
		StreamingResponseBody streaming(@RequestParam long timeToWait) {
			return out -> {
				PrintStream stream = new PrintStream(out, true, StandardCharsets.UTF_8);
				stream.print("name=Joe");
				try {
					Thread.sleep(timeToWait);
					stream.print("&someBoolean=true");
				}
				catch (InterruptedException e) {
					/* no-op */
				}
			};
		}
	}

	@Controller
	static class MultipartController {

		@PostMapping("/part")
		ModelAndView part(@RequestPart Part part, @RequestPart Map<String, String> json) {
			Map<String, Object> model = new HashMap<>(json);
			model.put(part.getName(), part.getSubmittedFileName());
			return new ModelAndView("index", model);
		}

		@PutMapping("/multipart-put")
		ModelAndView multiPartViaHttpPut(@RequestParam MultipartFile file) {
			return new ModelAndView("index", Map.of("name", file.getName()));
		}

		@PostMapping("/multipart-streaming")
		StreamingResponseBody streaming(@RequestParam MultipartFile file, @RequestParam long timeToWait) {
			return out -> {
				PrintStream stream = new PrintStream(out, true, StandardCharsets.UTF_8);
				stream.print("name=Joe");
				try {
					Thread.sleep(timeToWait);
					stream.print("&file=" + file.getOriginalFilename());
				}
				catch (InterruptedException e) {
					/* no-op */
				}
			};
		}
	}

	@Controller
	@SessionAttributes("locale")
	static class SessionController {

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
	static class ErrorController {

		@GetMapping("/error/1")
		public String one() {
			throw new IllegalStateException("Expected");
		}

		@GetMapping("/error/2")
		public String two() {
			throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED);
		}

		@GetMapping("/error/validation/{id}")
		public String validation(@PathVariable @Size(max = 4) String id) {
			return "Hello " + id;
		}
	}

}
