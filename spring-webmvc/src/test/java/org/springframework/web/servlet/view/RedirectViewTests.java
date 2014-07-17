/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.servlet.support.RequestDataValueProcessorWrapper;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.util.WebUtils;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;


/**
 * Tests for redirect view, and query string construction.
 * Doesn't test URL encoding, although it does check that it's called.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Arjen Poutsma
 * @since 27.05.2003
 */
public class RedirectViewTests {

	@Test(expected = IllegalArgumentException.class)
	public void noUrlSet() throws Exception {
		RedirectView rv = new RedirectView();
		rv.afterPropertiesSet();
	}

	@Test
	public void http11() throws Exception {
		RedirectView rv = new RedirectView();
		rv.setUrl("http://url.somewhere.com");
		rv.setHttp10Compatible(false);
		MockHttpServletRequest request = createRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
		request.setAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE, new SessionFlashMapManager());
		rv.render(new HashMap<String, Object>(), request, response);
		assertEquals(303, response.getStatus());
		assertEquals("http://url.somewhere.com", response.getHeader("Location"));
	}

	private MockHttpServletRequest createRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
		request.setAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE, new SessionFlashMapManager());
		return request;
	}

	@Test
	public void explicitStatusCodeHttp11() throws Exception {
		RedirectView rv = new RedirectView();
		rv.setUrl("http://url.somewhere.com");
		rv.setHttp10Compatible(false);
		rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
		MockHttpServletRequest request = createRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		rv.render(new HashMap<String, Object>(), request, response);
		assertEquals(301, response.getStatus());
		assertEquals("http://url.somewhere.com", response.getHeader("Location"));
	}

	@Test
	public void explicitStatusCodeHttp10() throws Exception {
		RedirectView rv = new RedirectView();
		rv.setUrl("http://url.somewhere.com");
		rv.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
		MockHttpServletRequest request = createRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		rv.render(new HashMap<String, Object>(), request, response);
		assertEquals(301, response.getStatus());
		assertEquals("http://url.somewhere.com", response.getHeader("Location"));
	}

	@Test
	public void attributeStatusCodeHttp11() throws Exception {
		RedirectView rv = new RedirectView();
		rv.setUrl("http://url.somewhere.com");
		rv.setHttp10Compatible(false);
		MockHttpServletRequest request = createRequest();
		request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.CREATED);
		MockHttpServletResponse response = new MockHttpServletResponse();
		rv.render(new HashMap<String, Object>(), request, response);
		assertEquals(201, response.getStatus());
		assertEquals("http://url.somewhere.com", response.getHeader("Location"));
	}

	@Test
	public void flashMap() throws Exception {
		RedirectView rv = new RedirectView();
		rv.setUrl("http://url.somewhere.com/path");
		rv.setHttp10Compatible(false);
		MockHttpServletRequest request = createRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		FlashMap flashMap = new FlashMap();
		flashMap.put("successMessage", "yay!");
		request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, flashMap);
		ModelMap model = new ModelMap("id", "1");
		rv.render(model, request, response);
		assertEquals(303, response.getStatus());
		assertEquals("http://url.somewhere.com/path?id=1", response.getHeader("Location"));

		assertEquals("/path", flashMap.getTargetRequestPath());
		assertEquals(model, flashMap.getTargetRequestParams().toSingleValueMap());
	}

	@Test
	public void updateTargetUrl() throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("requestDataValueProcessor", RequestDataValueProcessorWrapper.class);
		wac.setServletContext(new MockServletContext());
		wac.refresh();

		RequestDataValueProcessor mockProcessor = mock(RequestDataValueProcessor.class);
		wac.getBean(RequestDataValueProcessorWrapper.class).setRequestDataValueProcessor(mockProcessor);

		RedirectView rv = new RedirectView();
		rv.setApplicationContext(wac);	// Init RedirectView with WebAppCxt
		rv.setUrl("/path");

		MockHttpServletRequest request = createRequest();
		request.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);
		HttpServletResponse response = new MockHttpServletResponse();

		given(mockProcessor.processUrl(request, "/path")).willReturn("/path?key=123");

		rv.render(new ModelMap(), request, response);

		verify(mockProcessor).processUrl(request, "/path");
	}


	@Test
	public void updateTargetUrlWithContextLoader() throws Exception {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("requestDataValueProcessor", RequestDataValueProcessorWrapper.class);

		MockServletContext servletContext = new MockServletContext();
		ContextLoader contextLoader = new ContextLoader(wac);
		contextLoader.initWebApplicationContext(servletContext);

		try {
			RequestDataValueProcessor mockProcessor = mock(RequestDataValueProcessor.class);
			wac.getBean(RequestDataValueProcessorWrapper.class).setRequestDataValueProcessor(mockProcessor);

			RedirectView rv = new RedirectView();
			rv.setUrl("/path");

			MockHttpServletRequest request = createRequest();
			HttpServletResponse response = new MockHttpServletResponse();

			given(mockProcessor.processUrl(request, "/path")).willReturn("/path?key=123");

			rv.render(new ModelMap(), request, response);

			verify(mockProcessor).processUrl(request, "/path");
		}
		finally {
			contextLoader.closeWebApplicationContext(servletContext);
		}
	}

	@Test
	public void emptyMap() throws Exception {
		String url = "/myUrl";
		doTest(new HashMap<String, Object>(), url, false, url);
	}

	@Test
	public void emptyMapWithContextRelative() throws Exception {
		String url = "/myUrl";
		doTest(new HashMap<String, Object>(), url, true, url);
	}

	@Test
	public void singleParam() throws Exception {
		String url = "http://url.somewhere.com";
		String key = "foo";
		String val = "bar";
		Map<String, String> model = new HashMap<String, String>();
		model.put(key, val);
		String expectedUrlForEncoding = url + "?" + key + "=" + val;
		doTest(model, url, false, expectedUrlForEncoding);
	}

	@Test
	public void singleParamWithoutExposingModelAttributes() throws Exception {
		String url = "http://url.somewhere.com";
		String key = "foo";
		String val = "bar";
		Map<String, String> model = new HashMap<String, String>();
		model.put(key, val);
		String expectedUrlForEncoding = url; // + "?" + key + "=" + val;
		doTest(model, url, false, false, expectedUrlForEncoding);
	}

	@Test
	public void paramWithAnchor() throws Exception {
		String url = "http://url.somewhere.com/test.htm#myAnchor";
		String key = "foo";
		String val = "bar";
		Map<String, String> model = new HashMap<String, String>();
		model.put(key, val);
		String expectedUrlForEncoding = "http://url.somewhere.com/test.htm" + "?" + key + "=" + val + "#myAnchor";
		doTest(model, url, false, expectedUrlForEncoding);
	}

	@Test
	public void contextRelativeQueryParam() throws Exception {
		String url = "/test.html?id=1";
		doTest(new HashMap<String, Object>(), url, true, url);
	}

	@Test
	public void twoParams() throws Exception {
		String url = "http://url.somewhere.com";
		String key = "foo";
		String val = "bar";
		String key2 = "thisIsKey2";
		String val2 = "andThisIsVal2";
		Map<String, String> model = new HashMap<String, String>();
		model.put(key, val);
		model.put(key2, val2);
		try {
			String expectedUrlForEncoding = "http://url.somewhere.com?" + key + "=" + val + "&" + key2 + "=" + val2;
			doTest(model, url, false, expectedUrlForEncoding);
		}
		catch (AssertionError err) {
			// OK, so it's the other order... probably on Sun JDK 1.6 or IBM JDK 1.5
			String expectedUrlForEncoding = "http://url.somewhere.com?" + key2 + "=" + val2 + "&" + key + "=" + val;
			doTest(model, url, false, expectedUrlForEncoding);
		}
	}

	@Test
	public void arrayParam() throws Exception {
		String url = "http://url.somewhere.com";
		String key = "foo";
		String[] val = new String[] {"bar", "baz"};
		Map<String, String[]> model = new HashMap<String, String[]>();
		model.put(key, val);
		try {
			String expectedUrlForEncoding = "http://url.somewhere.com?" + key + "=" + val[0] + "&" + key + "=" + val[1];
			doTest(model, url, false, expectedUrlForEncoding);
		}
		catch (AssertionError err) {
			// OK, so it's the other order... probably on Sun JDK 1.6 or IBM JDK 1.5
			String expectedUrlForEncoding = "http://url.somewhere.com?" + key + "=" + val[1] + "&" + key + "=" + val[0];
			doTest(model, url, false, expectedUrlForEncoding);
		}
	}

	@Test
	public void collectionParam() throws Exception {
		String url = "http://url.somewhere.com";
		String key = "foo";
		List<String> val = new ArrayList<String>();
		val.add("bar");
		val.add("baz");
		Map<String, List<String>> model = new HashMap<String, List<String>>();
		model.put(key, val);
		try {
			String expectedUrlForEncoding = "http://url.somewhere.com?" + key + "=" + val.get(0) + "&" + key + "=" + val.get(1);
			doTest(model, url, false, expectedUrlForEncoding);
		}
		catch (AssertionError err) {
			// OK, so it's the other order... probably on Sun JDK 1.6 or IBM JDK 1.5
			String expectedUrlForEncoding = "http://url.somewhere.com?" + key + "=" + val.get(1) + "&" + key + "=" + val.get(0);
			doTest(model, url, false, expectedUrlForEncoding);
		}
	}

	@Test
	public void objectConversion() throws Exception {
		String url = "http://url.somewhere.com";
		String key = "foo";
		String val = "bar";
		String key2 = "int2";
		Object val2 = new Long(611);
		String key3 = "tb";
		Object val3 = new TestBean();
		Map<String, Object> model = new HashMap<String, Object>();
		model.put(key, val);
		model.put(key2, val2);
		model.put(key3, val3);
		String expectedUrlForEncoding = "http://url.somewhere.com?" + key + "=" + val + "&" + key2 + "=" + val2;
		doTest(model, url, false, expectedUrlForEncoding);
	}

	// SPR-11821

	@Test
	public void encodingParam() throws Exception {

		// setup mock components.
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		FlashMap flashMap = new FlashMap();
		flashMap.put("anyKey", "anyValue");
		request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, flashMap);
		request.setAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE,
				new SessionFlashMapManager());
		request.setContextPath("/context");

		// setup model.
		String url = "/path";
		String paramName1 = "key1 ";
		String paramName2 = "key2+";
		String paramName3 = "key3&";
		String paramName4 = "key4=";
		String paramName5 = "key5abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~:@/?!$'()*,;";
		String paramValue5 = "value5abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~:@/?!$'()*,;";

		Map<String, Object> model = new LinkedHashMap<String, Object>();
		// parameters that require encoding
		model.put(paramName1, "value1 ");
		model.put(paramName2, "value2+");
		model.put(paramName3, "value3&");
		model.put(paramName4, "value4=");
		// parameters that not require encoding
		model.put(paramName5, paramValue5);

		// do test.
		RedirectView redirectView = new RedirectView(url, true);
		redirectView.renderMergedOutputModel(model, request, response);

		// do assertion.
		StringBuilder expectedRedirectedUrl = new StringBuilder();
		expectedRedirectedUrl.append(request.getContextPath()).append(url);
		// encoded
		expectedRedirectedUrl.append("?").append("key1%20").append("=").append(
				"value1%20");
		expectedRedirectedUrl.append("&").append("key2%2B").append("=").append(
				"value2%2B");
		expectedRedirectedUrl.append("&").append("key3%26").append("=").append(
				"value3%26");
		expectedRedirectedUrl.append("&").append("key4%3D").append("=").append(
				"value4%3D");
		// not encoded
		expectedRedirectedUrl.append("&").append(paramName5).append("=").append(
				paramValue5);
		assertThat(response.getRedirectedUrl(), is(expectedRedirectedUrl.toString()));

		MultiValueMap<String, String> savedTargetRequestParams = flashMap.getTargetRequestParams();
		String[] targetParameterNames = { paramName1, paramName2, paramName3, paramName4,
			paramName5 };
		assertThat(savedTargetRequestParams.size(), is(targetParameterNames.length));
		for (String targetParameterName : targetParameterNames) {
			assertThat(targetParameterName + " is not equals.",
					savedTargetRequestParams.getFirst(targetParameterName),
					is(model.get(targetParameterName)));
		}

	}

	private void doTest(Map<String, ?> map, String url, boolean contextRelative, String expectedUrlForEncoding)
			throws Exception {
		doTest(map, url, contextRelative, true, expectedUrlForEncoding);
	}

	private void doTest(final Map<String, ?> map, final String url, final boolean contextRelative,
			final boolean exposeModelAttributes, String expectedUrlForEncoding) throws Exception {

		class TestRedirectView extends RedirectView {

			public boolean queryPropertiesCalled = false;

			/**
			 * Test whether this callback method is called with correct args
			 */
			@Override
			protected Map<String, Object> queryProperties(Map<String, Object> model) {
				// They may not be the same model instance, but they're still equal
				assertTrue("Map and model must be equal.", map.equals(model));
				this.queryPropertiesCalled = true;
				return super.queryProperties(model);
			}
		}

		TestRedirectView rv = new TestRedirectView();
		rv.setUrl(url);
		rv.setContextRelative(contextRelative);
		rv.setExposeModelAttributes(exposeModelAttributes);

		HttpServletRequest request = mock(HttpServletRequest.class, "request");
		if (exposeModelAttributes) {
			given(request.getCharacterEncoding()).willReturn(WebUtils.DEFAULT_CHARACTER_ENCODING);
		}
		if (contextRelative) {
			expectedUrlForEncoding = "/context" + expectedUrlForEncoding;
			given(request.getContextPath()).willReturn("/context");
		}

		given(request.getAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE)).willReturn(new FlashMap());

		FlashMapManager flashMapManager = new SessionFlashMapManager();
		given(request.getAttribute(DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE)).willReturn(flashMapManager);

		HttpServletResponse response = mock(HttpServletResponse.class, "response");
		given(response.encodeRedirectURL(expectedUrlForEncoding)).willReturn(expectedUrlForEncoding);
		response.sendRedirect(expectedUrlForEncoding);

		rv.render(map, request, response);
		if (exposeModelAttributes) {
			assertTrue("queryProperties() should have been called.", rv.queryPropertiesCalled);
		}
	}

}
