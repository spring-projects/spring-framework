/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.web.servlet.result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.StubMvcResult;
import org.springframework.test.web.servlet.result.PrintingResultHandler;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;

/**
 * Tests for {@link PrintingResultHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class PrintingResultHandlerTests {

	private TestPrintingResultHandler handler;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private StubMvcResult mvcResult;


	@Before
	public void setup() {
		this.handler = new TestPrintingResultHandler();
		this.request = new MockHttpServletRequest("GET", "/") {
			public boolean isAsyncStarted() {
				return false;
			}
		};
		this.response = new MockHttpServletResponse();
		this.mvcResult = new StubMvcResult(this.request, null, null, null, null, null, this.response);
	}

	@Test
	public void testPrintRequest() throws Exception {
		this.request.addParameter("param", "paramValue");
		this.request.addHeader("header", "headerValue");

		this.handler.handle(this.mvcResult);

		HttpHeaders headers = new HttpHeaders();
		headers.set("header", "headerValue");

		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
		params.add("param", "paramValue");

		assertValue("MockHttpServletRequest", "HTTP Method", this.request.getMethod());
		assertValue("MockHttpServletRequest", "Request URI", this.request.getRequestURI());
		assertValue("MockHttpServletRequest", "Parameters", params);
		assertValue("MockHttpServletRequest", "Headers", headers);
	}

	@Test
	public void testPrintResponse() throws Exception {
		this.response.setStatus(400, "error");
		this.response.addHeader("header", "headerValue");
		this.response.setContentType("text/plain");
		this.response.getWriter().print("content");
		this.response.setForwardedUrl("redirectFoo");
		this.response.sendRedirect("/redirectFoo");
		this.response.addCookie(new Cookie("cookie", "cookieValue"));

		this.handler.handle(this.mvcResult);

		HttpHeaders headers = new HttpHeaders();
		headers.set("header", "headerValue");
		headers.setContentType(MediaType.TEXT_PLAIN);
		headers.setLocation(new URI("/redirectFoo"));

		assertValue("MockHttpServletResponse", "Status", this.response.getStatus());
		assertValue("MockHttpServletResponse", "Error message", response.getErrorMessage());
		assertValue("MockHttpServletResponse", "Headers", headers);
		assertValue("MockHttpServletResponse", "Content type", this.response.getContentType());
		assertValue("MockHttpServletResponse", "Body", this.response.getContentAsString());
		assertValue("MockHttpServletResponse", "Forwarded URL", this.response.getForwardedUrl());
		assertValue("MockHttpServletResponse", "Redirected URL", this.response.getRedirectedUrl());
	}

	@Test
	public void testPrintHandlerNull() throws Exception {
		StubMvcResult mvcResult = new StubMvcResult(this.request, null, null, null, null, null, this.response);
		this.handler.handle(mvcResult);

		assertValue("Handler", "Type", null);
	}

	@Test
	public void testPrintHandler() throws Exception {
		this.mvcResult.setHandler(new Object());
		this.handler.handle(this.mvcResult);

		assertValue("Handler", "Type", Object.class.getName());
	}

	@Test
	public void testPrintHandlerMethod() throws Exception {
		HandlerMethod handlerMethod = new HandlerMethod(this, "handle");
		this.mvcResult.setHandler(handlerMethod);
		this.handler.handle(mvcResult);

		assertValue("Handler", "Type", this.getClass().getName());
		assertValue("Handler", "Method", handlerMethod);
	}

	@Test
	public void testResolvedExceptionNull() throws Exception {
		this.handler.handle(this.mvcResult);

		assertValue("Resolved Exception", "Type", null);
	}

	@Test
	public void testResolvedException() throws Exception {
		this.mvcResult.setResolvedException(new Exception());
		this.handler.handle(this.mvcResult);

		assertValue("Resolved Exception", "Type", Exception.class.getName());
	}

	@Test
	public void testModelAndViewNull() throws Exception {
		this.handler.handle(this.mvcResult);

		assertValue("ModelAndView", "View name", null);
		assertValue("ModelAndView", "View", null);
		assertValue("ModelAndView", "Model", null);
	}

	@Test
	public void testModelAndView() throws Exception {
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
	public void testFlashMapNull() throws Exception {
		this.handler.handle(mvcResult);

		assertValue("FlashMap", "Type", null);
	}

	@Test
	public void testFlashMap() throws Exception {
		FlashMap flashMap = new FlashMap();
		flashMap.put("attrName", "attrValue");
		this.request.setAttribute(DispatcherServlet.class.getName() + ".OUTPUT_FLASH_MAP", flashMap);

		this.handler.handle(this.mvcResult);

		assertValue("FlashMap", "Attribute", "attrName");
		assertValue("FlashMap", "value", "attrValue");
	}

	private void assertValue(String heading, String label, Object value) {
		Map<String, Map<String, Object>> printedValues = this.handler.getPrinter().printedValues;
		assertTrue("Heading " + heading + " not printed", printedValues.containsKey(heading));
		assertEquals(value, printedValues.get(heading).get(label));
	}


	private static class TestPrintingResultHandler extends PrintingResultHandler {

		public TestPrintingResultHandler() {
			super(new TestResultValuePrinter());
		}

		@Override
		public TestResultValuePrinter getPrinter() {
			return (TestResultValuePrinter) super.getPrinter();
		}

		private static class TestResultValuePrinter implements ResultValuePrinter {

			private String printedHeading;

			private Map<String, Map<String, Object>> printedValues = new HashMap<String, Map<String, Object>>();

			public void printHeading(String heading) {
				this.printedHeading = heading;
				this.printedValues.put(heading, new HashMap<String, Object>());
			}

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
