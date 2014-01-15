/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.request;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.multipart.MultipartRequest;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Markus Malkusch
 * @since 26.07.2006
 */
public class ServletWebRequestTests {

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;

	private ServletWebRequest request;

	@Before
	public void setUp() {
		servletRequest = new MockHttpServletRequest();
		servletResponse = new MockHttpServletResponse();
		request = new ServletWebRequest(servletRequest, servletResponse);
	}

	@Test
	public void parameters() {
		servletRequest.addParameter("param1", "value1");
		servletRequest.addParameter("param2", "value2");
		servletRequest.addParameter("param2", "value2a");

		assertEquals("value1", request.getParameter("param1"));
		assertEquals(1, request.getParameterValues("param1").length);
		assertEquals("value1", request.getParameterValues("param1")[0]);
		assertEquals("value2", request.getParameter("param2"));
		assertEquals(2, request.getParameterValues("param2").length);
		assertEquals("value2", request.getParameterValues("param2")[0]);
		assertEquals("value2a", request.getParameterValues("param2")[1]);

		Map paramMap = request.getParameterMap();
		assertEquals(2, paramMap.size());
		assertEquals(1, ((String[]) paramMap.get("param1")).length);
		assertEquals("value1", ((String[]) paramMap.get("param1"))[0]);
		assertEquals(2, ((String[]) paramMap.get("param2")).length);
		assertEquals("value2", ((String[]) paramMap.get("param2"))[0]);
		assertEquals("value2a", ((String[]) paramMap.get("param2"))[1]);
	}

	@Test
	public void locale() {
		servletRequest.addPreferredLocale(Locale.UK);

		assertEquals(Locale.UK, request.getLocale());
	}

	@Test
	public void nativeRequest() {
		assertSame(servletRequest, request.getNativeRequest());
		assertSame(servletRequest, request.getNativeRequest(ServletRequest.class));
		assertSame(servletRequest, request.getNativeRequest(HttpServletRequest.class));
		assertSame(servletRequest, request.getNativeRequest(MockHttpServletRequest.class));
		assertNull(request.getNativeRequest(MultipartRequest.class));
		assertSame(servletResponse, request.getNativeResponse());
		assertSame(servletResponse, request.getNativeResponse(ServletResponse.class));
		assertSame(servletResponse, request.getNativeResponse(HttpServletResponse.class));
		assertSame(servletResponse, request.getNativeResponse(MockHttpServletResponse.class));
		assertNull(request.getNativeResponse(MultipartRequest.class));
	}

	@Test
	public void decoratedNativeRequest() {
		HttpServletRequest decoratedRequest = new HttpServletRequestWrapper(servletRequest);
		HttpServletResponse decoratedResponse = new HttpServletResponseWrapper(servletResponse);
		ServletWebRequest request = new ServletWebRequest(decoratedRequest, decoratedResponse);
		assertSame(decoratedRequest, request.getNativeRequest());
		assertSame(decoratedRequest, request.getNativeRequest(ServletRequest.class));
		assertSame(decoratedRequest, request.getNativeRequest(HttpServletRequest.class));
		assertSame(servletRequest, request.getNativeRequest(MockHttpServletRequest.class));
		assertNull(request.getNativeRequest(MultipartRequest.class));
		assertSame(decoratedResponse, request.getNativeResponse());
		assertSame(decoratedResponse, request.getNativeResponse(ServletResponse.class));
		assertSame(decoratedResponse, request.getNativeResponse(HttpServletResponse.class));
		assertSame(servletResponse, request.getNativeResponse(MockHttpServletResponse.class));
		assertNull(request.getNativeResponse(MultipartRequest.class));
	}

	@Test
	public void checkNotModifiedTimeStampForGET() {
		long currentTime = new Date().getTime();
		servletRequest.setMethod("GET");
		servletRequest.addHeader("If-Modified-Since", currentTime);

		request.checkNotModified(currentTime);

		assertEquals(304, servletResponse.getStatus());
	}

	@Test
	public void checkModifiedTimeStampForGET() {
		long currentTime = new Date().getTime();
		long oneMinuteAgo  = currentTime - (1000 * 60);
		servletRequest.setMethod("GET");
		servletRequest.addHeader("If-Modified-Since", oneMinuteAgo);

		request.checkNotModified(currentTime);

		assertEquals(200, servletResponse.getStatus());
		assertEquals(""+currentTime, servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkNotModifiedETagForGET() {
		String eTag = "\"Foo\"";
		servletRequest.setMethod("GET");
		servletRequest.addHeader("If-None-Match", eTag );

		request.checkNotModified(eTag);

		assertEquals(304, servletResponse.getStatus());
	}

	@Test
	public void checkModifiedETagForGET() {
		String currentETag = "\"Foo\"";
		String oldEtag = "Bar";
		servletRequest.setMethod("GET");
		servletRequest.addHeader("If-None-Match", oldEtag);

		request.checkNotModified(currentETag);

		assertEquals(200, servletResponse.getStatus());
		assertEquals(currentETag, servletResponse.getHeader("ETag"));
	}

	@Test
	public void checkNotModifiedTimeStampForHEAD() {
		long currentTime = new Date().getTime();
		servletRequest.setMethod("HEAD");
		servletRequest.addHeader("If-Modified-Since", currentTime);

		request.checkNotModified(currentTime);

		assertEquals(304, servletResponse.getStatus());
	}

	@Test
	public void checkModifiedTimeStampForHEAD() {
		long currentTime = new Date().getTime();
		long oneMinuteAgo  = currentTime - (1000 * 60);
		servletRequest.setMethod("HEAD");
		servletRequest.addHeader("If-Modified-Since", oneMinuteAgo);

		request.checkNotModified(currentTime);

		assertEquals(200, servletResponse.getStatus());
		assertEquals(""+currentTime, servletResponse.getHeader("Last-Modified"));
	}

	@Test
	public void checkNotModifiedETagForHEAD() {
		String eTag = "\"Foo\"";
		servletRequest.setMethod("HEAD");
		servletRequest.addHeader("If-None-Match", eTag );

		request.checkNotModified(eTag);

		assertEquals(304, servletResponse.getStatus());
	}

	@Test
	public void checkModifiedETagForHEAD() {
		String currentETag = "\"Foo\"";
		String oldEtag = "Bar";
		servletRequest.setMethod("HEAD");
		servletRequest.addHeader("If-None-Match", oldEtag);

		request.checkNotModified(currentETag);

		assertEquals(200, servletResponse.getStatus());
		assertEquals(currentETag, servletResponse.getHeader("ETag"));
	}

}
