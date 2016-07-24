/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link ContentNegotiationManagerFactoryBean} tests.
 *
 * @author Rossen Stoyanchev
 */
public class ContentNegotiationManagerFactoryBeanTests {

	private ContentNegotiationManagerFactoryBean factoryBean;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest servletRequest;


	@Before
	public void setup() {
		TestServletContext servletContext = new TestServletContext();
		servletContext.getMimeTypes().put("foo", "application/foo");

		this.servletRequest = new MockHttpServletRequest(servletContext);
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
				Collections.singletonList(MediaType.IMAGE_GIF), manager.resolveMediaTypes(this.webRequest));

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
				Collections.singletonList(MediaType.IMAGE_GIF), manager.resolveMediaTypes(this.webRequest));
	}

	@Test
	public void favorPath() throws Exception {
		this.factoryBean.setFavorPathExtension(true);
		this.factoryBean.addMediaTypes(Collections.singletonMap("bar", new MediaType("application", "bar")));
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower.foo");
		assertEquals(Collections.singletonList(new MediaType("application", "foo")),
				manager.resolveMediaTypes(this.webRequest));

		this.servletRequest.setRequestURI("/flower.bar");
		assertEquals(Collections.singletonList(new MediaType("application", "bar")),
				manager.resolveMediaTypes(this.webRequest));

		this.servletRequest.setRequestURI("/flower.gif");
		assertEquals(Collections.singletonList(MediaType.IMAGE_GIF), manager.resolveMediaTypes(this.webRequest));
	}

	@Test
	public void favorPathWithJafTurnedOff() throws Exception {
		this.factoryBean.setFavorPathExtension(true);
		this.factoryBean.setUseJaf(false);
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower.foo");
		assertEquals(Collections.emptyList(), manager.resolveMediaTypes(this.webRequest));

		this.servletRequest.setRequestURI("/flower.gif");
		assertEquals(Collections.emptyList(), manager.resolveMediaTypes(this.webRequest));
	}

	@Test(expected = HttpMediaTypeNotAcceptableException.class)  // SPR-10170
	public void favorPathWithIgnoreUnknownPathExtensionTurnedOff() throws Exception {
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

		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON),
				manager.resolveMediaTypes(this.webRequest));
	}

	@Test(expected = HttpMediaTypeNotAcceptableException.class)  // SPR-10170
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

		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON),
				manager.resolveMediaTypes(this.webRequest));

		// SPR-10513
		this.servletRequest.addHeader("Accept", MediaType.ALL_VALUE);
		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON),
				manager.resolveMediaTypes(this.webRequest));
	}

	@Test  // SPR-12286
	public void setDefaultContentTypeWithStrategy() throws Exception {
		this.factoryBean.setDefaultContentTypeStrategy(new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON));
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON),
				manager.resolveMediaTypes(this.webRequest));

		this.servletRequest.addHeader("Accept", MediaType.ALL_VALUE);
		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON),
				manager.resolveMediaTypes(this.webRequest));
	}


	private static class TestServletContext extends MockServletContext {

		private final Map<String, String> mimeTypes = new HashMap<>();

		public Map<String, String> getMimeTypes() {
			return this.mimeTypes;
		}

		@Override
		public String getMimeType(String filePath) {
			String extension = StringUtils.getFilenameExtension(filePath);
			return getMimeTypes().get(extension);
		}
	}

}
