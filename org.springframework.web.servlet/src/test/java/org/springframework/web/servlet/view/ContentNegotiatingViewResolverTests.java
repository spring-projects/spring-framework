/*
 * Copyright 2002-2009 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

/** @author Arjen Poutsma */
public class ContentNegotiatingViewResolverTests {

	private ContentNegotiatingViewResolver viewResolver;

	@Before
	public void createViewResolver() {
		viewResolver = new ContentNegotiatingViewResolver();
	}

	@Test
	public void getMediaTypeFromFilename() {
		assertEquals("Invalid content type", new MediaType("text", "html"),
				viewResolver.getMediaTypeFromFilename("test.html"));
		viewResolver.setMediaTypes(Collections.singletonMap("HTML", "application/xhtml+xml"));
		assertEquals("Invalid content type", new MediaType("application", "xhtml+xml"),
				viewResolver.getMediaTypeFromFilename("test.html"));
	}

	@Test
	public void getMediaTypeFilename() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.html?foo=bar");
		List<MediaType> result = viewResolver.getMediaTypes(request);
		assertEquals("Invalid content type", Collections.singletonList(new MediaType("text", "html")), result);
		viewResolver.setMediaTypes(Collections.singletonMap("html", "application/xhtml+xml"));
		result = viewResolver.getMediaTypes(request);
		assertEquals("Invalid content type", Collections.singletonList(new MediaType("application", "xhtml+xml")),
				result);
	}

	@Test
	public void getMediaTypeAcceptHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
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
	public void resolveViewNameAcceptHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		ViewResolver viewResolverMock1 = createMock(ViewResolver.class);
		ViewResolver viewResolverMock2 = createMock(ViewResolver.class);
		List<ViewResolver> viewResolverMocks = new ArrayList<ViewResolver>();
		viewResolverMocks.add(viewResolverMock1);
		viewResolverMocks.add(viewResolverMock2);
		viewResolver.setViewResolvers(viewResolverMocks);

		View viewMock1 = createMock("application_xml", View.class);
		View viewMock2 = createMock("text_html", View.class);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock1.resolveViewName(viewName, locale)).andReturn(viewMock1);
		expect(viewResolverMock2.resolveViewName(viewName, locale)).andReturn(viewMock2);
		expect(viewMock1.getContentType()).andReturn("application/xml");
		expect(viewMock2.getContentType()).andReturn("text/html;charset=ISO-8859-1");

		replay(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock2, result);

		verify(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2);
	}
	
	@Test
	public void resolveViewNameFilename() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test.html");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		ViewResolver viewResolverMock1 = createMock(ViewResolver.class);
		ViewResolver viewResolverMock2 = createMock(ViewResolver.class);
		List<ViewResolver> viewResolverMocks = new ArrayList<ViewResolver>();
		viewResolverMocks.add(viewResolverMock1);
		viewResolverMocks.add(viewResolverMock2);
		viewResolver.setViewResolvers(viewResolverMocks);

		View viewMock1 = createMock("application_xml", View.class);
		View viewMock2 = createMock("text_html", View.class);

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		expect(viewResolverMock1.resolveViewName(viewName, locale)).andReturn(viewMock1);
		expect(viewResolverMock2.resolveViewName(viewName, locale)).andReturn(viewMock2);
		expect(viewMock1.getContentType()).andReturn("application/xml");
		expect(viewMock2.getContentType()).andReturn("text/html;charset=ISO-8859-1");

		replay(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock2, result);

		verify(viewResolverMock1, viewResolverMock2, viewMock1, viewMock2);
	}

}
