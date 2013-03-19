/*
 * Copyright 2002-2013 the original author or authors.
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.MappingMediaTypeFileExtensionResolver;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Arjen Poutsma
 */
public class ContentNegotiatingViewResolverTests {

	private ContentNegotiatingViewResolver viewResolver;

	private MockHttpServletRequest request;

	@Before
	public void createViewResolver() {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		wac.refresh();
		viewResolver = new ContentNegotiatingViewResolver();
		viewResolver.setApplicationContext(wac);
		request = new MockHttpServletRequest("GET", "/test");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@After
	public void resetRequestContextHolder() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	public void getMediaTypeAcceptHeaderWithProduces() throws Exception {
		Set<MediaType> producibleTypes = Collections.singleton(MediaType.APPLICATION_XHTML_XML);
		request.setAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, producibleTypes);
		request.addHeader("Accept", "text/html,application/xml;q=0.9,application/xhtml+xml,*/*;q=0.8");
		viewResolver.afterPropertiesSet();
		List<MediaType> result = viewResolver.getMediaTypes(request);
		assertEquals("Invalid content type", new MediaType("application", "xhtml+xml"), result.get(0));
	}

	@Test
	public void resolveViewNameWithPathExtension() throws Exception {
		request.setRequestURI("/test.xls");

		ViewResolver viewResolverMock = mock(ViewResolver.class);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));
		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "application_xls");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(null);
		given(viewResolverMock.resolveViewName(viewName + ".xls", locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn("application/vnd.ms-excel");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock, result);
	}

	@Test
	public void resolveViewNameWithAcceptHeader() throws Exception {
		request.addHeader("Accept", "application/vnd.ms-excel");

		Map<String, MediaType> mapping = Collections.singletonMap("xls", MediaType.valueOf("application/vnd.ms-excel"));
		MappingMediaTypeFileExtensionResolver extensionsResolver = new MappingMediaTypeFileExtensionResolver(mapping);
		ContentNegotiationManager manager = new ContentNegotiationManager(new HeaderContentNegotiationStrategy());
		manager.addFileExtensionResolvers(extensionsResolver);
		viewResolver.setContentNegotiationManager(manager);

		ViewResolver viewResolverMock = mock(ViewResolver.class);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		View viewMock = mock(View.class, "application_xls");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(null);
		given(viewResolverMock.resolveViewName(viewName + ".xls", locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn("application/vnd.ms-excel");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock, result);
	}

	@Test
	public void resolveViewNameWithInvalidAcceptHeader() throws Exception {
		request.addHeader("Accept", "application");

		viewResolver.afterPropertiesSet();
		View result = viewResolver.resolveViewName("test", Locale.ENGLISH);
		assertNull(result);
	}

	@Test
	public void resolveViewNameWithRequestParameter() throws Exception {
		request.addParameter("format", "xls");

		Map<String, MediaType> mapping = Collections.singletonMap("xls", MediaType.valueOf("application/vnd.ms-excel"));
		ParameterContentNegotiationStrategy paramStrategy = new ParameterContentNegotiationStrategy(mapping);
		viewResolver.setContentNegotiationManager(new ContentNegotiationManager(paramStrategy));

		ViewResolver viewResolverMock = mock(ViewResolver.class);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));
		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "application_xls");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(null);
		given(viewResolverMock.resolveViewName(viewName + ".xls", locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn("application/vnd.ms-excel");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock, result);
	}

	@Test
	public void resolveViewNameWithDefaultContentType() throws Exception {
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

		MediaType mediaType = new MediaType("application", "xml");
		FixedContentNegotiationStrategy fixedStrategy = new FixedContentNegotiationStrategy(mediaType);
		viewResolver.setContentNegotiationManager(new ContentNegotiationManager(fixedStrategy));

		ViewResolver viewResolverMock1 = mock(ViewResolver.class, "viewResolver1");
		ViewResolver viewResolverMock2 = mock(ViewResolver.class, "viewResolver2");
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));
		viewResolver.afterPropertiesSet();

		View viewMock1 = mock(View.class, "application_xml");
		View viewMock2 = mock(View.class, "text_html");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock1.resolveViewName(viewName, locale)).willReturn(viewMock1);
		given(viewResolverMock2.resolveViewName(viewName, locale)).willReturn(viewMock2);
		given(viewMock1.getContentType()).willReturn("application/xml");
		given(viewMock2.getContentType()).willReturn("text/html;charset=ISO-8859-1");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock1, result);
	}

	@Test
	public void resolveViewNameAcceptHeader() throws Exception {
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

		ViewResolver viewResolverMock1 = mock(ViewResolver.class);
		ViewResolver viewResolverMock2 = mock(ViewResolver.class);
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

		viewResolver.afterPropertiesSet();

		View viewMock1 = mock(View.class, "application_xml");
		View viewMock2 = mock(View.class, "text_html");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock1.resolveViewName(viewName, locale)).willReturn(viewMock1);
		given(viewResolverMock2.resolveViewName(viewName, locale)).willReturn(viewMock2);
		given(viewMock1.getContentType()).willReturn("application/xml");
		given(viewMock2.getContentType()).willReturn("text/html;charset=ISO-8859-1");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock2, result);
	}

	// SPR-9160

	@Test
	public void resolveViewNameAcceptHeaderSortByQuality() throws Exception {
		request.addHeader("Accept", "text/plain;q=0.5, application/json");

		viewResolver.setContentNegotiationManager(new ContentNegotiationManager(new HeaderContentNegotiationStrategy()));

		ViewResolver htmlViewResolver = mock(ViewResolver.class);
		ViewResolver jsonViewResolver = mock(ViewResolver.class);
		viewResolver.setViewResolvers(Arrays.asList(htmlViewResolver, jsonViewResolver));

		View htmlView = mock(View.class, "text_html");
		View jsonViewMock = mock(View.class, "application_json");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(htmlViewResolver.resolveViewName(viewName, locale)).willReturn(htmlView);
		given(jsonViewResolver.resolveViewName(viewName, locale)).willReturn(jsonViewMock);
		given(htmlView.getContentType()).willReturn("text/html");
		given(jsonViewMock.getContentType()).willReturn("application/json");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", jsonViewMock, result);
	}

	// SPR-9807

	@Test
	public void resolveViewNameAcceptHeaderWithSuffix() throws Exception {
		request.addHeader("Accept", "application/vnd.example-v2+xml");

		ViewResolver viewResolverMock = mock(ViewResolver.class);
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock));

		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "application_xml");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn("application/*+xml");

		View result = viewResolver.resolveViewName(viewName, locale);

		assertSame("Invalid view", viewMock, result);
		assertEquals(new MediaType("application", "vnd.example-v2+xml"), request.getAttribute(View.SELECTED_CONTENT_TYPE));
	}

	@Test
	public void resolveViewNameAcceptHeaderDefaultView() throws Exception {
		request.addHeader("Accept", "application/json");

		ViewResolver viewResolverMock1 = mock(ViewResolver.class);
		ViewResolver viewResolverMock2 = mock(ViewResolver.class);
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

		View viewMock1 = mock(View.class, "application_xml");
		View viewMock2 = mock(View.class, "text_html");
		View viewMock3 = mock(View.class, "application_json");

		List<View> defaultViews = new ArrayList<View>();
		defaultViews.add(viewMock3);
		viewResolver.setDefaultViews(defaultViews);

		viewResolver.afterPropertiesSet();

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock1.resolveViewName(viewName, locale)).willReturn(viewMock1);
		given(viewResolverMock2.resolveViewName(viewName, locale)).willReturn(viewMock2);
		given(viewMock1.getContentType()).willReturn("application/xml");
		given(viewMock2.getContentType()).willReturn("text/html;charset=ISO-8859-1");
		given(viewMock3.getContentType()).willReturn("application/json");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock3, result);
	}

	@Test
	public void resolveViewNameFilename() throws Exception {
		request.setRequestURI("/test.html");

		ViewResolver viewResolverMock1 = mock(ViewResolver.class, "viewResolver1");
		ViewResolver viewResolverMock2 = mock(ViewResolver.class, "viewResolver2");
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

		viewResolver.afterPropertiesSet();

		View viewMock1 = mock(View.class, "application_xml");
		View viewMock2 = mock(View.class, "text_html");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock1.resolveViewName(viewName, locale)).willReturn(viewMock1);
		given(viewResolverMock1.resolveViewName(viewName + ".html", locale)).willReturn(null);
		given(viewResolverMock2.resolveViewName(viewName, locale)).willReturn(null);
		given(viewResolverMock2.resolveViewName(viewName + ".html", locale)).willReturn(viewMock2);
		given(viewMock1.getContentType()).willReturn("application/xml");
		given(viewMock2.getContentType()).willReturn("text/html;charset=ISO-8859-1");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock2, result);
	}

	@Test
	public void resolveViewNameFilenameDefaultView() throws Exception {
		request.setRequestURI("/test.json");

		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		PathExtensionContentNegotiationStrategy pathStrategy = new PathExtensionContentNegotiationStrategy(mapping);
		viewResolver.setContentNegotiationManager(new ContentNegotiationManager(pathStrategy));

		ViewResolver viewResolverMock1 = mock(ViewResolver.class);
		ViewResolver viewResolverMock2 = mock(ViewResolver.class);
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

		View viewMock1 = mock(View.class, "application_xml");
		View viewMock2 = mock(View.class, "text_html");
		View viewMock3 = mock(View.class, "application_json");

		List<View> defaultViews = new ArrayList<View>();
		defaultViews.add(viewMock3);
		viewResolver.setDefaultViews(defaultViews);

		viewResolver.afterPropertiesSet();

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock1.resolveViewName(viewName, locale)).willReturn(viewMock1);
		given(viewResolverMock1.resolveViewName(viewName + ".json", locale)).willReturn(null);
		given(viewResolverMock2.resolveViewName(viewName, locale)).willReturn(viewMock2);
		given(viewResolverMock2.resolveViewName(viewName + ".json", locale)).willReturn(null);
		given(viewMock1.getContentType()).willReturn("application/xml");
		given(viewMock2.getContentType()).willReturn("text/html;charset=ISO-8859-1");
		given(viewMock3.getContentType()).willReturn("application/json");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertSame("Invalid view", viewMock3, result);
	}

	@Test
	public void resolveViewContentTypeNull() throws Exception {
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

		ViewResolver viewResolverMock = mock(ViewResolver.class);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "application_xml");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn(null);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertNull("Invalid view", result);
	}

	@Test
	public void resolveViewNameRedirectView() throws Exception {
		request.addHeader("Accept", "application/json");
		request.setRequestURI("/test");

		StaticWebApplicationContext webAppContext = new StaticWebApplicationContext();
		webAppContext.setServletContext(new MockServletContext());
		webAppContext.refresh();

		UrlBasedViewResolver urlViewResolver = new InternalResourceViewResolver();
		urlViewResolver.setApplicationContext(webAppContext);
		ViewResolver xmlViewResolver = mock(ViewResolver.class);
		viewResolver.setViewResolvers(Arrays.<ViewResolver>asList(xmlViewResolver, urlViewResolver));

		View xmlView = mock(View.class, "application_xml");
		View jsonView = mock(View.class, "application_json");
		viewResolver.setDefaultViews(Arrays.asList(jsonView));

		viewResolver.afterPropertiesSet();

		String viewName = "redirect:anotherTest";
		Locale locale = Locale.ENGLISH;

		given(xmlViewResolver.resolveViewName(viewName, locale)).willReturn(xmlView);
		given(jsonView.getContentType()).willReturn("application/json");

		View actualView = viewResolver.resolveViewName(viewName, locale);
		assertEquals("Invalid view", RedirectView.class, actualView.getClass());
	}

	@Test
	public void resolveViewNoMatch() throws Exception {
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9");

		ViewResolver viewResolverMock = mock(ViewResolver.class);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "application_xml");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn("application/pdf");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertNull("Invalid view", result);
	}

	@Test
	public void resolveViewNoMatchUseUnacceptableStatus() throws Exception {
		viewResolver.setUseNotAcceptableStatusCode(true);
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9");

		ViewResolver viewResolverMock = mock(ViewResolver.class);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "application_xml");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn("application/pdf");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertNotNull("Invalid view", result);
		MockHttpServletResponse response = new MockHttpServletResponse();
		result.render(null, request, response);
		assertEquals("Invalid status code set", 406, response.getStatus());
	}

	@Test
	public void nestedViewResolverIsNotSpringBean() throws Exception {
		StaticWebApplicationContext webAppContext = new StaticWebApplicationContext();
		webAppContext.setServletContext(new MockServletContext());
		webAppContext.refresh();

		InternalResourceViewResolver nestedResolver = new InternalResourceViewResolver();
		nestedResolver.setApplicationContext(webAppContext);
		nestedResolver.setViewClass(InternalResourceView.class);
		viewResolver.setViewResolvers(new ArrayList<ViewResolver>(Arrays.asList(nestedResolver)));

		FixedContentNegotiationStrategy fixedStrategy = new FixedContentNegotiationStrategy(MediaType.TEXT_HTML);
		viewResolver.setContentNegotiationManager(new ContentNegotiationManager(fixedStrategy));

		viewResolver.afterPropertiesSet();

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		View result = viewResolver.resolveViewName(viewName, locale);
		assertNotNull("Invalid view", result);
	}

}
