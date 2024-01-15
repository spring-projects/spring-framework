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

package org.springframework.test.web.servlet.result;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.StubMvcResult;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PrintingResultHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see org.springframework.test.web.servlet.samples.standalone.resulthandlers.PrintingResultHandlerSmokeTests
 */
class PrintingResultHandlerTests {

	private final TestPrintingResultHandler handler = new TestPrintingResultHandler();

	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/") {
		@Override
		public boolean isAsyncStarted() {
			return false;
		}
	};

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final StubMvcResult mvcResult = new StubMvcResult(
			this.request, null, null, null, null, null, this.response);


	@Test
	void printRequest() throws Exception {
		this.request.addParameter("param", "paramValue");
		this.request.addHeader("header", "headerValue");
		this.request.setCharacterEncoding("UTF-16");
		String palindrome = "ablE was I ere I saw Elba";
		byte[] bytes = palindrome.getBytes(UTF_16);
		this.request.setContent(bytes);
		this.request.getSession().setAttribute("foo", "bar");

		this.handler.handle(this.mvcResult);

		HttpHeaders headers = new HttpHeaders();
		headers.set("header", "headerValue");

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("param", "paramValue");

		assertValue("MockHttpServletRequest", "HTTP Method", this.request.getMethod());
		assertValue("MockHttpServletRequest", "Request URI", this.request.getRequestURI());
		assertValue("MockHttpServletRequest", "Parameters", params);
		assertValue("MockHttpServletRequest", "Headers", headers);
		assertValue("MockHttpServletRequest", "Body", palindrome);
		assertValue("MockHttpServletRequest", "Session Attrs", Collections.singletonMap("foo", "bar"));
	}

	@Test
	void printRequestWithoutSession() throws Exception {
		this.request.addParameter("param", "paramValue");
		this.request.addHeader("header", "headerValue");
		this.request.setCharacterEncoding("UTF-16");
		String palindrome = "ablE was I ere I saw Elba";
		byte[] bytes = palindrome.getBytes(UTF_16);
		this.request.setContent(bytes);

		this.handler.handle(this.mvcResult);

		HttpHeaders headers = new HttpHeaders();
		headers.set("header", "headerValue");

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("param", "paramValue");

		assertValue("MockHttpServletRequest", "HTTP Method", this.request.getMethod());
		assertValue("MockHttpServletRequest", "Request URI", this.request.getRequestURI());
		assertValue("MockHttpServletRequest", "Parameters", params);
		assertValue("MockHttpServletRequest", "Headers", headers);
		assertValue("MockHttpServletRequest", "Body", palindrome);
	}

	@Test
	void printRequestWithEmptySessionMock() throws Exception {
		this.request.addParameter("param", "paramValue");
		this.request.addHeader("header", "headerValue");
		this.request.setCharacterEncoding("UTF-16");
		String palindrome = "ablE was I ere I saw Elba";
		byte[] bytes = palindrome.getBytes(UTF_16);
		this.request.setContent(bytes);
		this.request.setSession(mock());

		this.handler.handle(this.mvcResult);

		HttpHeaders headers = new HttpHeaders();
		headers.set("header", "headerValue");

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("param", "paramValue");

		assertValue("MockHttpServletRequest", "HTTP Method", this.request.getMethod());
		assertValue("MockHttpServletRequest", "Request URI", this.request.getRequestURI());
		assertValue("MockHttpServletRequest", "Parameters", params);
		assertValue("MockHttpServletRequest", "Headers", headers);
		assertValue("MockHttpServletRequest", "Body", palindrome);
	}

	@Test
	void printResponse() throws Exception {
		Cookie enigmaCookie = new Cookie("enigma", "42");
		enigmaCookie.setHttpOnly(true);
		enigmaCookie.setMaxAge(1234);
		enigmaCookie.setDomain(".example.com");
		enigmaCookie.setPath("/crumbs");
		enigmaCookie.setSecure(true);

		this.response.setStatus(400);
		this.response.addHeader("header", "headerValue");
		this.response.setContentType("text/plain");
		this.response.getWriter().print("content");
		this.response.setForwardedUrl("redirectFoo");
		this.response.sendRedirect("/redirectFoo");
		this.response.addCookie(new Cookie("cookie", "cookieValue"));
		this.response.addCookie(enigmaCookie);

		this.handler.handle(this.mvcResult);

		// Manually validate cookie values since maxAge changes...
		List<String> cookieValues = this.response.getHeaders("Set-Cookie");
		assertThat(cookieValues).satisfiesExactly(
				zero -> assertThat(zero).isEqualTo("cookie=cookieValue"),
				one -> assertThat(one).startsWith("enigma=42; Path=/crumbs; Domain=.example.com; Max-Age=1234; Expires="));

		HttpHeaders headers = new HttpHeaders();
		headers.set("header", "headerValue");
		headers.setContentType(MediaType.TEXT_PLAIN);
		headers.setLocation(URI.create("/redirectFoo"));
		headers.put("Set-Cookie", cookieValues);

		String heading = "MockHttpServletResponse";
		assertValue(heading, "Status", this.response.getStatus());
		assertValue(heading, "Error message", response.getErrorMessage());
		assertValue(heading, "Headers", headers);
		assertValue(heading, "Content type", this.response.getContentType());
		assertValue(heading, "Body", this.response.getContentAsString());
		assertValue(heading, "Forwarded URL", this.response.getForwardedUrl());
		assertValue(heading, "Redirected URL", this.response.getRedirectedUrl());

		Map<String, Map<String, Object>> printedValues = this.handler.getPrinter().printedValues;
		String[] cookies = (String[]) printedValues.get(heading).get("Cookies");
		assertThat(cookies).hasSize(2);
		String cookie1 = cookies[0];
		String cookie2 = cookies[1];
		assertThat(cookie1).startsWith("[" + Cookie.class.getSimpleName());
		assertThat(cookie1).contains("name = 'cookie', value = 'cookieValue'");
		assertThat(cookie1).endsWith("]");
		assertThat(cookie2).startsWith("[" + Cookie.class.getSimpleName());
		assertThat(cookie2).contains("name = 'enigma', value = '42', " +
				"comment = [null], domain = '.example.com', maxAge = 1234, " +
				"path = '/crumbs', secure = true, version = 0, httpOnly = true");
		assertThat(cookie2).endsWith("]");
	}

	@Test
	void printRequestWithCharacterEncoding() throws Exception {
		this.request.setCharacterEncoding("UTF-8");
		this.request.setContent("text".getBytes(UTF_8));

		this.handler.handle(this.mvcResult);

		assertValue("MockHttpServletRequest", "Body", "text");
	}

	@Test
	void printRequestWithoutCharacterEncoding() throws Exception {
		this.handler.handle(this.mvcResult);

		assertValue("MockHttpServletRequest", "Body", "<no character encoding set>");
	}

	@Test
	void printResponseWithCharacterEncoding() throws Exception {
		this.response.setCharacterEncoding("UTF-8");
		this.response.getWriter().print("text");

		this.handler.handle(this.mvcResult);
		assertValue("MockHttpServletResponse", "Body", "text");
	}

	@Test
	void printResponseWithDefaultCharacterEncoding() throws Exception {
		this.response.getWriter().print("text");

		this.handler.handle(this.mvcResult);

		assertValue("MockHttpServletResponse", "Body", "text");
	}

	@Test
	void printHandlerNull() throws Exception {
		StubMvcResult mvcResult = new StubMvcResult(this.request, null, null, null, null, null, this.response);
		this.handler.handle(mvcResult);

		assertValue("Handler", "Type", null);
	}

	@Test
	void printHandler() throws Exception {
		this.mvcResult.setHandler(new Object());
		this.handler.handle(this.mvcResult);

		assertValue("Handler", "Type", Object.class.getName());
	}

	@Test
	void printHandlerMethod() throws Exception {
		HandlerMethod handlerMethod = new HandlerMethod(this, "handle");
		this.mvcResult.setHandler(handlerMethod);
		this.handler.handle(mvcResult);

		assertValue("Handler", "Type", this.getClass().getName());
		assertValue("Handler", "Method", handlerMethod);
	}

	@Test
	void resolvedExceptionNull() throws Exception {
		this.handler.handle(this.mvcResult);

		assertValue("Resolved Exception", "Type", null);
	}

	@Test
	void resolvedException() throws Exception {
		this.mvcResult.setResolvedException(new Exception());
		this.handler.handle(this.mvcResult);

		assertValue("Resolved Exception", "Type", Exception.class.getName());
	}

	@Test
	void modelAndViewNull() throws Exception {
		this.handler.handle(this.mvcResult);

		assertValue("ModelAndView", "View name", null);
		assertValue("ModelAndView", "View", null);
		assertValue("ModelAndView", "Model", null);
	}

	@Test
	void modelAndView() throws Exception {
		BindException bindException = new BindException(new Object(), "target");
		bindException.reject("errorCode");

		ModelAndView mav = new ModelAndView("viewName");
		mav.addObject("attrName", "attrValue");
		mav.addObject(BindingResult.MODEL_KEY_PREFIX + "attrName", bindException);

		this.mvcResult.setMav(mav);
		this.handler.handle(this.mvcResult);

		assertValue("ModelAndView", "View name", "viewName");
		assertValue("ModelAndView", "View", null);
		assertValue("ModelAndView", "Attribute", "attrName");
		assertValue("ModelAndView", "value", "attrValue");
		assertValue("ModelAndView", "errors", bindException.getAllErrors());
	}

	@Test
	void flashMapNull() throws Exception {
		this.handler.handle(mvcResult);

		assertValue("FlashMap", "Type", null);
	}

	@Test
	void flashMap() throws Exception {
		FlashMap flashMap = new FlashMap();
		flashMap.put("attrName", "attrValue");
		this.request.setAttribute(DispatcherServlet.class.getName() + ".OUTPUT_FLASH_MAP", flashMap);

		this.handler.handle(this.mvcResult);

		assertValue("FlashMap", "Attribute", "attrName");
		assertValue("FlashMap", "value", "attrValue");
	}

	private void assertValue(String heading, String label, Object value) {
		Map<String, Map<String, Object>> printedValues = this.handler.getPrinter().printedValues;
		assertThat(printedValues.containsKey(heading)).as("Heading '" + heading + "' not printed").isTrue();
		assertThat(printedValues.get(heading).get(label)).as("For label '" + label + "' under heading '" + heading + "' =>").isEqualTo(value);
	}


	private static class TestPrintingResultHandler extends PrintingResultHandler {

		TestPrintingResultHandler() {
			super(new TestResultValuePrinter());
		}

		@Override
		public TestResultValuePrinter getPrinter() {
			return (TestResultValuePrinter) super.getPrinter();
		}

		private static class TestResultValuePrinter implements ResultValuePrinter {

			private String printedHeading;

			private final Map<String, Map<String, Object>> printedValues = new HashMap<>();

			@Override
			public void printHeading(String heading) {
				this.printedHeading = heading;
				this.printedValues.put(heading, new HashMap<>());
			}

			@Override
			public void printValue(String label, Object value) {
				Assert.notNull(this.printedHeading,
						"Heading not printed before label " + label + " with value " + value);
				this.printedValues.get(this.printedHeading).put(label, value);
			}
		}
	}


	public void handle() {
	}

}
