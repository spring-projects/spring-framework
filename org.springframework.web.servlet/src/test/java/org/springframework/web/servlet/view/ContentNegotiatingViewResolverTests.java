/*
 * Copyright 2002-2010 the original author or authors.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.easymock.EasyMock.*;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/**
 * @author Arjen Poutsma
 */
public class ContentNegotiatingViewResolverTests {

	private ContentNegotiatingViewResolver viewResolver;

	private MockHttpServletRequest request;

	@Before
	public void createViewResolver() {
		viewResolver = new ContentNegotiatingViewResolver();
		request = new MockHttpServletRequest("GET", "/test");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@After
	public void resetRequestContextHolder() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	public void getMediaTypeFromFilenameMediaTypes() {
		viewResolver.setMediaTypes(Collections.singletonMap("HTML", "application/xhtml+xml"));
		assertEquals("Invalid content type", new MediaType("application", "xhtml+xml"),
				viewResolver.getMediaTypeFromFilename("test.html"));
	}

	@Test
	public void getMediaTypeFromFilenameJaf() {
		assertEquals("Invalid content type", new MediaType("text", "html"),
				viewResolver.getMediaTypeFromFilename("test.html"));
	}

	@Test
	public void getMediaTypeFromFilenameNoJaf() {
		viewResolver.setUseJaf(false);
		assertNull("Invalid content type", viewResolver.getMediaTypeFromFilename("test.html"));
	}

	@Test
	public void getMediaTypeFilename() {
		request.setRequestURI("/test.html?foo=bar");
		List<MediaType> result = viewResolver.getMediaTypes(request);
		assertEquals("Invalid content type", Collections.singletonList(new MediaType("text", "html")), result);
		viewResolver.setMediaTypes(Collections.singletonMap("html", "application/xhtml+xml"));
		result = viewResolver.getMediaTypes(request);
		assertEquals("Invalid content type", Collections.singletonList(new MediaType("application", "xhtml+xml")),
				result);
	}

	@Test
	public void getMediaTypeParameter() {
		viewResolver.setFavorParameter(true);
		viewResolver.setMediaTypes(Collections.singletonMap("html", "application/xhtml+xml"));
		request.addParameter("format", "html");
		List<MediaType> result = viewResolver.getMediaTypes(request);
		assertEquals("Invalid content type", Collections.singletonList(new MediaType("application", "xhtml+xml")),
				result);
	}

	@Test
	public void getMediaTypeAcceptHeader() {
		request.addHeader("Accept", "text/html,application/xml;q=0.9,application/xhtml+xml,*/*;q=0.8");
		List<MediaType> result = viewResolver.getMediaTypes(request);
		assertEquals("Invalid amount of media types", 4, result.size());
		assertEquals("Invalid content type", new MediaType("text", "html"), result.get(0));
		assertEquals("Invalid content type", new MediaType("application", "xhtml+xml"), result.get(1));
		assertEquals("Invalid content type", new MediaType("application", "xml", Collections.singletonMap("q", "0.9")),
				result.get(2));
		assertEquals("Invalid content type", new MediaType("*", "*", Collections.singletonMap("q", "0.8")),
				result.get(3));
	}

	@Test
	public void getDefaultContentType() {
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		viewResolver.setIgnoreAcceptHeader(true);
		viewResolver.setDefaultContentType(new MediaType("application", "pdf"));
		List<MediaType> result = viewResolver.getMediaTypes(request);
		assertEquals("Invalid amount of media types", 1, result.size());
		assertEquals("Invalid content type", new MediaType("application", "pdf"), result.get(0));
	}

	@Test
	public void resolveViewNameWithPathExtension() throws Exception {
		request.setRequestURI("/test.xls");

		ViewResolver viewResolverMock = createMock(ViewResolver.class);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		View viewMock = createMock("application_xls", View.class);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock.resolveViewName(viewName, locale)).andReturn(null);
		expect(viewResolverMock.resolveViewName(viewName + ".xls", locale)).andReturn(viewMock);
		expect(viewMock.getContentType()).andReturn("application/vnd.ms-excel").anyTimes();

		replay(viewResolverMock, viewMock);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock, result);

		verify(viewResolverMock, viewMock);
	}

	@Test
	public void resolveViewNameWithAcceptHeader() throws Exception {
		request.addHeader("Accept", "application/vnd.ms-excel");

		viewResolver.setMediaTypes(Collections.singletonMap("xls", "application/vnd.ms-excel"));

		ViewResolver viewResolverMock = createMock(ViewResolver.class);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		View viewMock = createMock("application_xls", View.class);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock.resolveViewName(viewName, locale)).andReturn(null);
		expect(viewResolverMock.resolveViewName(viewName + ".xls", locale)).andReturn(viewMock);
		expect(viewMock.getContentType()).andReturn("application/vnd.ms-excel").anyTimes();

		replay(viewResolverMock, viewMock);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock, result);
		verify(viewResolverMock, viewMock);
	}

	@Test
	public void resolveViewNameWithRequestParameter() throws Exception {
		request.addParameter("format", "xls");

		viewResolver.setFavorParameter(true);
		viewResolver.setMediaTypes(Collections.singletonMap("xls", "application/vnd.ms-excel"));

		ViewResolver viewResolverMock = createMock(ViewResolver.class);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		View viewMock = createMock("application_xls", View.class);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock.resolveViewName(viewName, locale)).andReturn(null);
		expect(viewResolverMock.resolveViewName(viewName + ".xls", locale)).andReturn(viewMock);
		expect(viewMock.getContentType()).andReturn("application/vnd.ms-excel").anyTimes();

		replay(viewResolverMock, viewMock);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock, result);

		verify(viewResolverMock, viewMock);
	}

	@Test
	public void resolveViewNameWithDefaultContentType() throws Exception {
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

		viewResolver.setIgnoreAcceptHeader(true);
		viewResolver.setDefaultContentType(new MediaType("application", "xml"));

		ViewResolver viewResolverMock1 = createMock("viewResolver1", ViewResolver.class);
		ViewResolver viewResolverMock2 = createMock("viewResolver2", ViewResolver.class);
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

		View viewMock1 = createMock("application_xml", View.class);
		View viewMock2 = createMock("text_html", View.class);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock1.resolveViewName(viewName, locale)).andReturn(viewMock1);
		expect(viewResolverMock2.resolveViewName(viewName, locale)).andReturn(viewMock2);
		expect(viewMock1.getContentType()).andReturn("application/xml").anyTimes();
		expect(viewMock2.getContentType()).andReturn("text/html;charset=ISO-8859-1").anyTimes();

		replay(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock1, result);

		verify(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2);
	}

	@Test
	public void resolveViewNameAcceptHeader() throws Exception {
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

		ViewResolver viewResolverMock1 = createMock(ViewResolver.class);
		ViewResolver viewResolverMock2 = createMock(ViewResolver.class);
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

		View viewMock1 = createMock("application_xml", View.class);
		View viewMock2 = createMock("text_html", View.class);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock1.resolveViewName(viewName, locale)).andReturn(viewMock1);
		expect(viewResolverMock2.resolveViewName(viewName, locale)).andReturn(viewMock2);
		expect(viewMock1.getContentType()).andReturn("application/xml").anyTimes();
		expect(viewMock2.getContentType()).andReturn("text/html;charset=ISO-8859-1").anyTimes();

		replay(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock2, result);

		verify(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2);
	}

	@Test
	public void resolveViewNameAcceptHeaderDefaultView() throws Exception {
		request.addHeader("Accept", "application/json");

		ViewResolver viewResolverMock1 = createMock(ViewResolver.class);
		ViewResolver viewResolverMock2 = createMock(ViewResolver.class);
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

		View viewMock1 = createMock("application_xml", View.class);
		View viewMock2 = createMock("text_html", View.class);
		View viewMock3 = createMock("application_json", View.class);

		List<View> defaultViews = new ArrayList<View>();
		defaultViews.add(viewMock3);
		viewResolver.setDefaultViews(defaultViews);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock1.resolveViewName(viewName, locale)).andReturn(viewMock1);
		expect(viewResolverMock2.resolveViewName(viewName, locale)).andReturn(viewMock2);
		expect(viewMock1.getContentType()).andReturn("application/xml").anyTimes();
		expect(viewMock2.getContentType()).andReturn("text/html;charset=ISO-8859-1").anyTimes();
		expect(viewMock3.getContentType()).andReturn("application/json").anyTimes();

		replay(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2, viewMock3);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock3, result);

		verify(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2, viewMock3);
	}

	@Test
	public void resolveViewNameFilename() throws Exception {
		request.setRequestURI("/test.html");

		ViewResolver viewResolverMock1 = createMock("viewResolver1", ViewResolver.class);
		ViewResolver viewResolverMock2 = createMock("viewResolver2", ViewResolver.class);
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

		View viewMock1 = createMock("application_xml", View.class);
		View viewMock2 = createMock("text_html", View.class);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock1.resolveViewName(viewName, locale)).andReturn(viewMock1);
		expect(viewResolverMock1.resolveViewName(viewName + ".html", locale)).andReturn(null);
		expect(viewResolverMock2.resolveViewName(viewName, locale)).andReturn(null);
		expect(viewResolverMock2.resolveViewName(viewName + ".html", locale)).andReturn(viewMock2);
		expect(viewMock1.getContentType()).andReturn("application/xml").anyTimes();
		expect(viewMock2.getContentType()).andReturn("text/html;charset=ISO-8859-1").anyTimes();

		replay(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock2, result);

		verify(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2);
	}

	@Test
	public void resolveViewNameFilenameDefaultView() throws Exception {
		request.setRequestURI("/test.json");

		Map<String, String> mediaTypes = new HashMap<String, String>();
		mediaTypes.put("json", "application/json");
		viewResolver.setMediaTypes(mediaTypes);

		ViewResolver viewResolverMock1 = createMock(ViewResolver.class);
		ViewResolver viewResolverMock2 = createMock(ViewResolver.class);
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

		View viewMock1 = createMock("application_xml", View.class);
		View viewMock2 = createMock("text_html", View.class);
		View viewMock3 = createMock("application_json", View.class);

		List<View> defaultViews = new ArrayList<View>();
		defaultViews.add(viewMock3);
		viewResolver.setDefaultViews(defaultViews);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock1.resolveViewName(viewName, locale)).andReturn(viewMock1);
		expect(viewResolverMock1.resolveViewName(viewName + ".json", locale)).andReturn(null);
		expect(viewResolverMock2.resolveViewName(viewName, locale)).andReturn(viewMock2);
		expect(viewResolverMock2.resolveViewName(viewName + ".json", locale)).andReturn(null);
		expect(viewMock1.getContentType()).andReturn("application/xml").anyTimes();
		expect(viewMock2.getContentType()).andReturn("text/html;charset=ISO-8859-1").anyTimes();
		expect(viewMock3.getContentType()).andReturn("application/json").anyTimes();

		replay(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2, viewMock3);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock3, result);

		verify(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2, viewMock3);
	}

	@Test
	public void resolveViewContentTypeNull() throws Exception {
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

		ViewResolver viewResolverMock = createMock(ViewResolver.class);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		View viewMock = createMock("application_xml", View.class);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock.resolveViewName(viewName, locale)).andReturn(viewMock);
		expect(viewMock.getContentType()).andReturn(null).anyTimes();

		replay(viewResolverMock, viewMock);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertNull("Invalid view", result);

		verify(viewResolverMock, viewMock);
	}

	@Test
	public void resolveViewNoMatch() throws Exception {
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9");

		ViewResolver viewResolverMock = createMock(ViewResolver.class);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		View viewMock = createMock("application_xml", View.class);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock.resolveViewName(viewName, locale)).andReturn(viewMock);
		expect(viewMock.getContentType()).andReturn("application/pdf").anyTimes();

		replay(viewResolverMock, viewMock);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertNull("Invalid view", result);

		verify(viewResolverMock, viewMock);
	}

	@Test
	public void resolveViewNoMatchUseUnacceptableStatus() throws Exception {
		viewResolver.setUseNotAcceptableStatusCode(true);
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9");

		ViewResolver viewResolverMock = createMock(ViewResolver.class);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		View viewMock = createMock("application_xml", View.class);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock.resolveViewName(viewName, locale)).andReturn(viewMock);
		expect(viewMock.getContentType()).andReturn("application/pdf").anyTimes();

		replay(viewResolverMock, viewMock);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertNotNull("Invalid view", result);
		MockHttpServletResponse response = new MockHttpServletResponse();
		result.render(null, request, response);
		assertEquals("Invalid status code set", 406, response.getStatus());

		verify(viewResolverMock, viewMock);
	}

}
