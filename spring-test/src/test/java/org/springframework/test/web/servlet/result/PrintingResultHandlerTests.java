/*
 * Copyright 2002-2017 the original author or authors.
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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.junit.Test;
import org.mockito.Mockito;

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

import static org.junit.Assert.*;

/**
 * Unit tests for {@link PrintingResultHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see org.springframework.test.web.servlet.samples.standalone.resulthandlers.PrintingResultHandlerSmokeTests
 */
public class PrintingResultHandlerTests {

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
	public void printRequest() throws Exception {
		this.request.addParameter("param", "paramValue");
		this.request.addHeader("header", "headerValue");
		this.request.setCharacterEncoding("UTF-16");
		String palindrome = "ablE was I ere I saw Elba";
		byte[] bytes = palindrome.getBytes("UTF-16");
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
	public void printRequestWithoutSession() throws Exception {
		this.request.addParameter("param", "paramValue");
		this.request.addHeader("header", "headerValue");
		this.request.setCharacterEncoding("UTF-16");
		String palindrome = "ablE was I ere I saw Elba";
		byte[] bytes = palindrome.getBytes("UTF-16");
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
	public void printRequestWithEmptySessionMock() throws Exception {
		this.request.addParameter("param", "paramValue");
		this.request.addHeader("header", "headerValue");
		this.request.setCharacterEncoding("UTF-16");
		String palindrome = "ablE was I ere I saw Elba";
		byte[] bytes = palindrome.getBytes("UTF-16");
		this.request.setContent(bytes);
		this.request.setSession(Mockito.mock(HttpSession.class));

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
	@SuppressWarnings("deprecation")
	public void printResponse() throws Exception {
		Cookie enigmaCookie = new Cookie("enigma", "42");
		enigmaCookie.setComment("This is a comment");
		enigmaCookie.setHttpOnly(true);
		enigmaCookie.setMaxAge(1234);
		enigmaCookie.setDomain(".example.com");
		enigmaCookie.setPath("/crumbs");
		enigmaCookie.setSecure(true);

		this.response.setStatus(400, "error");
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
		assertEquals(2, cookieValues.size());
		assertEquals("cookie=cookieValue", cookieValues.get(0));
		assertTrue("Actual: " + cookieValues.get(1), cookieValues.get(1).startsWith(
				"enigma=42; Path=/crumbs; Domain=.example.com; Max-Age=1234; Expires="));

		HttpHeaders headers = new HttpHeaders();
		headers.set("header", "headerValue");
		headers.setContentType(MediaType.TEXT_PLAIN);
		headers.setLocation(new URI("/redirectFoo"));
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
		assertEquals(2, cookies.length);
		String cookie1 = cookies[0];
		String cookie2 = cookies[1];
		assertTrue(cookie1.startsWith("[" + Cookie.class.getSimpleName()));
		assertTrue(cookie1.contains("name = 'cookie', value = 'cookieValue'"));
		assertTrue(cookie1.endsWith("]"));
		assertTrue(cookie2.startsWith("[" + Cookie.class.getSimpleName()));
		assertTrue(cookie2.contains("name = 'enigma', value = '42', " +
				"comment = 'This is a comment', domain = '.example.com', maxAge = 1234, " +
				"path = '/crumbs', secure = true, version = 0, httpOnly = true"));
		assertTrue(cookie2.endsWith("]"));
	}

	@Test
	public void printRequestWithCharacterEncoding() throws Exception {
		this.request.setCharacterEncoding("UTF-8");
		this.request.setContent("text".getBytes("UTF-8"));

		this.handler.handle(this.mvcResult);

		assertValue("MockHttpServletRequest", "Body", "text");
	}

	@Test
	public void printRequestWithoutCharacterEncoding() throws Exception {
		this.handler.handle(this.mvcResult);

		assertValue("MockHttpServletRequest", "Body", "<no character encoding set>");
	}

	@Test
	public void printResponseWithCharacterEncoding() throws Exception {
		this.response.setCharacterEncoding("UTF-8");
		this.response.getWriter().print("text");

		this.handler.handle(this.mvcResult);
		assertValue("MockHttpServletResponse", "Body", "text");
	}

	@Test
	public void printResponseWithDefaultCharacterEncoding() throws Exception {
		this.response.getWriter().print("text");

		this.handler.handle(this.mvcResult);

		assertValue("MockHttpServletResponse", "Body", "text");
	}

	@Test
	public void printResponseWithoutCharacterEncoding() throws Exception {
		this.response.setCharacterEncoding(null);
		this.response.getWriter().print("text");

		this.handler.handle(this.mvcResult);

		assertValue("MockHttpServletResponse", "Body", "<no character encoding set>");
	}

	@Test
	public void printHandlerNull() throws Exception {
		StubMvcResult mvcResult = new StubMvcResult(this.request, null, null, null, null, null, this.response);
		this.handler.handle(mvcResult);

		assertValue("Handler", "Type", null);
	}

	@Test
	public void printHandler() throws Exception {
		this.mvcResult.setHandler(new Object());
		this.handler.handle(this.mvcResult);

		assertValue("Handler", "Type", Object.class.getName());
	}

	@Test
	public void printHandlerMethod() throws Exception {
		HandlerMethod handlerMethod = new HandlerMethod(this, "handle");
		this.mvcResult.setHandler(handlerMethod);
		this.handler.handle(mvcResult);

		assertValue("Handler", "Type", this.getClass().getName());
		assertValue("Handler", "Method", handlerMethod);
	}

	@Test
	public void resolvedExceptionNull() throws Exception {
		this.handler.handle(this.mvcResult);

		assertValue("Resolved Exception", "Type", null);
	}

	@Test
	public void resolvedException() throws Exception {
		this.mvcResult.setResolvedException(new Exception());
		this.handler.handle(this.mvcResult);

		assertValue("Resolved Exception", "Type", Exception.class.getName());
	}

	@Test
	public void modelAndViewNull() throws Exception {
		this.handler.handle(this.mvcResult);

		assertValue("ModelAndView", "View name", null);
		assertValue("ModelAndView", "View", null);
		assertValue("ModelAndView", "Model", null);
	}

	@Test
	public void modelAndView() throws Exception {
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
	public void flashMapNull() throws Exception {
		this.handler.handle(mvcResult);

		assertValue("FlashMap", "Type", null);
	}

	@Test
	public void flashMap() throws Exception {
		FlashMap flashMap = new FlashMap();
		flashMap.put("attrName", "attrValue");
		this.request.setAttribute(DispatcherServlet.class.getName() + ".OUTPUT_FLASH_MAP", flashMap);

		this.handler.handle(this.mvcResult);

		assertValue("FlashMap", "Attribute", "attrName");
		assertValue("FlashMap", "value", "attrValue");
	}

	private void assertValue(String heading, String label, Object value) {
		Map<String, Map<String, Object>> printedValues = this.handler.getPrinter().printedValues;
		assertTrue("Heading '" + heading + "' not printed", printedValues.containsKey(heading));
		assertEquals("For label '" + label + "' under heading '" + heading + "' =>", value,
				printedValues.get(heading).get(label));
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

			private Map<String, Map<String, Object>> printedValues = new HashMap<>();

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
