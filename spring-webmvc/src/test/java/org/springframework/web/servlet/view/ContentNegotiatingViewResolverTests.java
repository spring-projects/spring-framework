/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.MappingMediaTypeFileExtensionResolver;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
public class ContentNegotiatingViewResolverTests {

	private ContentNegotiatingViewResolver viewResolver;

	private MockHttpServletRequest request;

	@BeforeEach
	public void createViewResolver() {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.setServletContext(new MockServletContext());
		wac.refresh();
		viewResolver = new ContentNegotiatingViewResolver();
		viewResolver.setApplicationContext(wac);
		request = new MockHttpServletRequest("GET", "/test");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@AfterEach
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
		assertThat(result.get(0)).as("Invalid content type").isEqualTo(new MediaType("application", "xhtml+xml"));
	}

	@Test
	public void resolveViewNameWithPathExtension() throws Exception {
		request.setRequestURI("/test");
		request.setParameter("format", "xls");

		String mediaType = "application/vnd.ms-excel";
		ContentNegotiationManager manager = new ContentNegotiationManager(
				new ParameterContentNegotiationStrategy(
						Collections.singletonMap("xls", MediaType.parseMediaType(mediaType))));

		ViewResolver viewResolverMock = mock();
		viewResolver.setContentNegotiationManager(manager);
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));
		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "application_xls");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(null);
		given(viewResolverMock.resolveViewName(viewName + ".xls", locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn(mediaType);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertThat(result).as("Invalid view").isSameAs(viewMock);
	}

	@Test
	public void resolveViewNameWithAcceptHeader() throws Exception {
		request.addHeader("Accept", "application/vnd.ms-excel");

		Map<String, MediaType> mapping = Collections.singletonMap("xls", MediaType.valueOf("application/vnd.ms-excel"));
		MappingMediaTypeFileExtensionResolver extensionsResolver = new MappingMediaTypeFileExtensionResolver(mapping);
		ContentNegotiationManager manager = new ContentNegotiationManager(new HeaderContentNegotiationStrategy());
		manager.addFileExtensionResolvers(extensionsResolver);
		viewResolver.setContentNegotiationManager(manager);

		ViewResolver viewResolverMock = mock();
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		View viewMock = mock(View.class, "application_xls");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(null);
		given(viewResolverMock.resolveViewName(viewName + ".xls", locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn("application/vnd.ms-excel");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertThat(result).as("Invalid view").isSameAs(viewMock);
	}

	@Test
	public void resolveViewNameWithInvalidAcceptHeader() throws Exception {
		request.addHeader("Accept", "application");

		ViewResolver viewResolverMock = mock();
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));
		viewResolver.afterPropertiesSet();

		View result = viewResolver.resolveViewName("test", Locale.ENGLISH);
		assertThat(result).isNull();
	}

	@Test
	public void resolveViewNameWithRequestParameter() throws Exception {
		request.addParameter("format", "xls");

		Map<String, MediaType> mapping = Collections.singletonMap("xls", MediaType.valueOf("application/vnd.ms-excel"));
		ParameterContentNegotiationStrategy paramStrategy = new ParameterContentNegotiationStrategy(mapping);
		viewResolver.setContentNegotiationManager(new ContentNegotiationManager(paramStrategy));

		ViewResolver viewResolverMock = mock();
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));
		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "application_xls");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(null);
		given(viewResolverMock.resolveViewName(viewName + ".xls", locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn("application/vnd.ms-excel");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertThat(result).as("Invalid view").isSameAs(viewMock);
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
		assertThat(result).as("Invalid view").isSameAs(viewMock1);
	}

	@Test
	public void resolveViewNameAcceptHeader() throws Exception {
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

		ViewResolver viewResolverMock1 = mock();
		ViewResolver viewResolverMock2 = mock();
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
		assertThat(result).as("Invalid view").isSameAs(viewMock2);
	}

	// SPR-9160

	@Test
	public void resolveViewNameAcceptHeaderSortByQuality() throws Exception {
		request.addHeader("Accept", "text/plain;q=0.5, application/json");

		viewResolver.setContentNegotiationManager(new ContentNegotiationManager(new HeaderContentNegotiationStrategy()));

		ViewResolver htmlViewResolver = mock();
		ViewResolver jsonViewResolver = mock();
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
		assertThat(result).as("Invalid view").isSameAs(jsonViewMock);
	}

	// SPR-9807

	@Test
	public void resolveViewNameAcceptHeaderWithSuffix() throws Exception {
		request.addHeader("Accept", "application/vnd.example-v2+xml");

		ViewResolver viewResolverMock = mock();
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock));

		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "application_xml");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn("application/*+xml");

		View result = viewResolver.resolveViewName(viewName, locale);

		assertThat(result).as("Invalid view").isSameAs(viewMock);
		assertThat(request.getAttribute(View.SELECTED_CONTENT_TYPE)).isEqualTo(new MediaType("application", "vnd.example-v2+xml"));
	}

	@Test
	public void resolveViewNameAcceptHeaderDefaultView() throws Exception {
		request.addHeader("Accept", "application/json");

		ViewResolver viewResolverMock1 = mock();
		ViewResolver viewResolverMock2 = mock();
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

		View viewMock1 = mock(View.class, "application_xml");
		View viewMock2 = mock(View.class, "text_html");
		View viewMock3 = mock(View.class, "application_json");

		List<View> defaultViews = new ArrayList<>();
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
		assertThat(result).as("Invalid view").isSameAs(viewMock3);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void resolveViewNameFilename() throws Exception {
		request.setRequestURI("/test.html");

		ContentNegotiationManager manager =
				new ContentNegotiationManager(new org.springframework.web.accept.PathExtensionContentNegotiationStrategy());

		ViewResolver viewResolverMock1 = mock(ViewResolver.class, "viewResolver1");
		ViewResolver viewResolverMock2 = mock(ViewResolver.class, "viewResolver2");
		viewResolver.setContentNegotiationManager(manager);
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
		assertThat(result).as("Invalid view").isSameAs(viewMock2);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void resolveViewNameFilenameDefaultView() throws Exception {
		request.setRequestURI("/test.json");

		Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
		org.springframework.web.accept.PathExtensionContentNegotiationStrategy pathStrategy =
				new org.springframework.web.accept.PathExtensionContentNegotiationStrategy(mapping);
		viewResolver.setContentNegotiationManager(new ContentNegotiationManager(pathStrategy));

		ViewResolver viewResolverMock1 = mock();
		ViewResolver viewResolverMock2 = mock();
		viewResolver.setViewResolvers(Arrays.asList(viewResolverMock1, viewResolverMock2));

		View viewMock1 = mock(View.class, "application_xml");
		View viewMock2 = mock(View.class, "text_html");
		View viewMock3 = mock(View.class, "application_json");

		List<View> defaultViews = new ArrayList<>();
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
		assertThat(result).as("Invalid view").isSameAs(viewMock3);
	}

	@Test
	public void resolveViewContentTypeNull() throws Exception {
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

		ViewResolver viewResolverMock = mock();
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "application_xml");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn(null);

		View result = viewResolver.resolveViewName(viewName, locale);
		assertThat(result).as("Invalid view").isNull();
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
		ViewResolver xmlViewResolver = mock();
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
		assertThat(actualView.getClass()).as("Invalid view").isEqualTo(RedirectView.class);
	}

	@Test
	public void resolveViewNoMatch() throws Exception {
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9");

		ViewResolver viewResolverMock = mock();
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "application_xml");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn("application/pdf");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertThat(result).as("Invalid view").isNull();
	}

	@Test
	public void resolveViewNoMatchUseUnacceptableStatus() throws Exception {
		viewResolver.setUseNotAcceptableStatusCode(true);
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9");

		ViewResolver viewResolverMock = mock();
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "application_xml");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn("application/pdf");

		View result = viewResolver.resolveViewName(viewName, locale);
		assertThat(result).as("Invalid view").isNotNull();
		MockHttpServletResponse response = new MockHttpServletResponse();
		result.render(null, request, response);
		assertThat(response.getStatus()).as("Invalid status code set").isEqualTo(406);
	}

	@Test
	public void nestedViewResolverIsNotSpringBean() throws Exception {
		StaticWebApplicationContext webAppContext = new StaticWebApplicationContext();
		webAppContext.setServletContext(new MockServletContext());
		webAppContext.refresh();

		InternalResourceViewResolver nestedResolver = new InternalResourceViewResolver();
		nestedResolver.setApplicationContext(webAppContext);
		nestedResolver.setViewClass(InternalResourceView.class);
		viewResolver.setViewResolvers(new ArrayList<>(Arrays.asList(nestedResolver)));

		FixedContentNegotiationStrategy fixedStrategy = new FixedContentNegotiationStrategy(MediaType.TEXT_HTML);
		viewResolver.setContentNegotiationManager(new ContentNegotiationManager(fixedStrategy));

		viewResolver.afterPropertiesSet();

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		View result = viewResolver.resolveViewName(viewName, locale);
		assertThat(result).as("Invalid view").isNotNull();
	}

	@Test
	public void resolveQualityValue() throws Exception {
		request.addHeader("Accept", "text/html;q=0.9");

		ViewResolver viewResolverMock = mock();
		viewResolver.setViewResolvers(Collections.singletonList(viewResolverMock));

		viewResolver.afterPropertiesSet();

		View viewMock = mock(View.class, "text_html");

		String viewName = "view";
		Locale locale = Locale.ENGLISH;

		given(viewResolverMock.resolveViewName(viewName, locale)).willReturn(viewMock);
		given(viewMock.getContentType()).willReturn("text/html");

		viewResolver.resolveViewName(viewName, locale);

		assertThat(request.getAttribute(View.SELECTED_CONTENT_TYPE)).isEqualTo(MediaType.TEXT_HTML);
	}

}
