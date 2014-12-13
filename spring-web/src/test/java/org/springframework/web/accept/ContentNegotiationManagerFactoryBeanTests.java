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
package org.springframework.web.accept;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link ContentNegotiationManagerFactoryBean} tests.
 * @author Rossen Stoyanchev
 */
public class ContentNegotiationManagerFactoryBeanTests {

	private ContentNegotiationManagerFactoryBean factoryBean;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest servletRequest;


	@Before
	public void setup() {
		this.servletRequest = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(this.servletRequest);

		this.factoryBean = new ContentNegotiationManagerFactoryBean();
		this.factoryBean.setServletContext(this.servletRequest.getServletContext());
	}


	@Test
	public void defaultSettings() throws Exception {
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower.gif");

		assertEquals("Should be able to resolve file extensions by default",
				Arrays.asList(MediaType.IMAGE_GIF), manager.resolveMediaTypes(this.webRequest));

		this.servletRequest.setRequestURI("/flower.xyz");

		assertEquals("Should ignore unknown extensions by default",
				Collections.<MediaType>emptyList(), manager.resolveMediaTypes(this.webRequest));

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.setParameter("format", "gif");

		assertEquals("Should not resolve request parameters by default",
				Collections.<MediaType>emptyList(), manager.resolveMediaTypes(this.webRequest));

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addHeader("Accept", MediaType.IMAGE_GIF_VALUE);

		assertEquals("Should resolve Accept header by default",
				Arrays.asList(MediaType.IMAGE_GIF), manager.resolveMediaTypes(this.webRequest));
	}

	@Test
	public void addMediaTypes() throws Exception {
		Map<String, MediaType> mediaTypes = new HashMap<>();
		mediaTypes.put("json", MediaType.APPLICATION_JSON);
		this.factoryBean.addMediaTypes(mediaTypes);

		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower.json");
		assertEquals(Arrays.asList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(this.webRequest));
	}

	// SPR-10170

	@Test(expected = HttpMediaTypeNotAcceptableException.class)
	public void favorPathExtensionWithUnknownMediaType() throws Exception {
		this.factoryBean.setFavorPathExtension(true);
		this.factoryBean.setIgnoreUnknownPathExtensions(false);
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower.xyz");
		this.servletRequest.addParameter("format", "json");

		manager.resolveMediaTypes(this.webRequest);
	}

	@Test
	public void favorParameter() throws Exception {
		this.factoryBean.setFavorParameter(true);

		Map<String, MediaType> mediaTypes = new HashMap<>();
		mediaTypes.put("json", MediaType.APPLICATION_JSON);
		this.factoryBean.addMediaTypes(mediaTypes);

		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addParameter("format", "json");

		assertEquals(Arrays.asList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(this.webRequest));
	}

	// SPR-10170

	@Test(expected = HttpMediaTypeNotAcceptableException.class)
	public void favorParameterWithUnknownMediaType() throws HttpMediaTypeNotAcceptableException {
		this.factoryBean.setFavorParameter(true);
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.setParameter("format", "xyz");

		manager.resolveMediaTypes(this.webRequest);
	}

	@Test
	public void ignoreAcceptHeader() throws Exception {
		this.factoryBean.setIgnoreAcceptHeader(true);
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addHeader("Accept", MediaType.IMAGE_GIF_VALUE);

		assertEquals(Collections.<MediaType>emptyList(), manager.resolveMediaTypes(this.webRequest));
	}

	@Test
	public void setDefaultContentType() throws Exception {
		this.factoryBean.setDefaultContentType(MediaType.APPLICATION_JSON);
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		assertEquals(Arrays.asList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(this.webRequest));

		// SPR-10513

		this.servletRequest.addHeader("Accept", MediaType.ALL_VALUE);

		assertEquals(Arrays.asList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(this.webRequest));
	}

	// SPR-12286
	@Test
	public void setDefaultContentTypeWithStrategy() throws Exception {
		this.factoryBean.setDefaultContentTypeStrategy(new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON));
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		assertEquals(Arrays.asList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(this.webRequest));

		this.servletRequest.addHeader("Accept", MediaType.ALL_VALUE);
		assertEquals(Arrays.asList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(this.webRequest));
	}

}
