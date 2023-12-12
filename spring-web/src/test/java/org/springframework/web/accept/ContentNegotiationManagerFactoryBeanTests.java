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

package org.springframework.web.accept;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test fixture for {@link ContentNegotiationManagerFactoryBean} tests.
 *
 * @author Rossen Stoyanchev
 */
class ContentNegotiationManagerFactoryBeanTests {

	private ContentNegotiationManagerFactoryBean factoryBean;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest servletRequest;


	@BeforeEach
	void setup() {
		TestServletContext servletContext = new TestServletContext();
		servletContext.getMimeTypes().put("foo", "application/foo");

		this.servletRequest = new MockHttpServletRequest(servletContext);
		this.webRequest = new ServletWebRequest(this.servletRequest);

		this.factoryBean = new ContentNegotiationManagerFactoryBean();
		this.factoryBean.setServletContext(this.servletRequest.getServletContext());
	}


	@Test
	void defaultSettings() throws Exception {
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower.gif");

		assertThat(manager.resolveMediaTypes(this.webRequest))
				.as("Should not resolve file extensions by default")
				.containsExactly(MediaType.ALL);

		this.servletRequest.setRequestURI("/flower.foobarbaz");

		assertThat(manager.resolveMediaTypes(this.webRequest))
				.as("Should ignore unknown extensions by default")
				.isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.setParameter("format", "gif");

		assertThat(manager.resolveMediaTypes(this.webRequest))
				.as("Should not resolve request parameters by default")
				.isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addHeader("Accept", MediaType.IMAGE_GIF_VALUE);

		assertThat(manager.resolveMediaTypes(this.webRequest))
				.as("Should resolve Accept header by default")
				.isEqualTo(Collections.singletonList(MediaType.IMAGE_GIF));
	}

	@Test
	void explicitStrategies() throws Exception {
		Map<String, MediaType> mediaTypes = Collections.singletonMap("bar", new MediaType("application", "bar"));
		ParameterContentNegotiationStrategy strategy1 = new ParameterContentNegotiationStrategy(mediaTypes);
		HeaderContentNegotiationStrategy strategy2 = new HeaderContentNegotiationStrategy();
		List<ContentNegotiationStrategy> strategies = Arrays.asList(strategy1, strategy2);
		this.factoryBean.setStrategies(strategies);
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		assertThat(manager.getStrategies()).isEqualTo(strategies);

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addParameter("format", "bar");
		assertThat(manager.resolveMediaTypes(this.webRequest))
				.isEqualTo(Collections.singletonList(new MediaType("application", "bar")));

	}

	@Test
	@SuppressWarnings("deprecation")
	void favorPath() throws Exception {
		this.factoryBean.setFavorPathExtension(true);
		this.factoryBean.addMediaType("bar", new MediaType("application", "bar"));
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower.foo");
		assertThat(manager.resolveMediaTypes(this.webRequest))
				.isEqualTo(Collections.singletonList(new MediaType("application", "foo")));

		this.servletRequest.setRequestURI("/flower.bar");
		assertThat(manager.resolveMediaTypes(this.webRequest))
				.isEqualTo(Collections.singletonList(new MediaType("application", "bar")));

		this.servletRequest.setRequestURI("/flower.gif");
		assertThat(manager.resolveMediaTypes(this.webRequest))
				.isEqualTo(Collections.singletonList(MediaType.IMAGE_GIF));
	}

	@Test // SPR-10170
	@SuppressWarnings("deprecation")
	void favorPathWithIgnoreUnknownPathExtensionTurnedOff() {
		this.factoryBean.setFavorPathExtension(true);
		this.factoryBean.setIgnoreUnknownPathExtensions(false);
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower.foobarbaz");
		this.servletRequest.addParameter("format", "json");

		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class).isThrownBy(() ->
				manager.resolveMediaTypes(this.webRequest));
	}

	@Test
	void favorParameter() throws Exception {
		this.factoryBean.setFavorParameter(true);
		this.factoryBean.addMediaType("json", MediaType.APPLICATION_JSON);

		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addParameter("format", "json");

		assertThat(manager.resolveMediaTypes(this.webRequest))
				.isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));
	}

	@Test // SPR-10170
	void favorParameterWithUnknownMediaType() {
		this.factoryBean.setFavorParameter(true);
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.setParameter("format", "invalid");

		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
				.isThrownBy(() -> manager.resolveMediaTypes(this.webRequest));
	}

	@Test
	@SuppressWarnings("deprecation")
	void mediaTypeMappingsWithoutPathAndParameterStrategies() {
		this.factoryBean.setFavorPathExtension(false);
		this.factoryBean.setFavorParameter(false);

		Properties properties = new Properties();
		properties.put("JSon", "application/json");

		this.factoryBean.setMediaTypes(properties);
		this.factoryBean.addMediaType("pdF", MediaType.APPLICATION_PDF);
		this.factoryBean.addMediaTypes(Collections.singletonMap("xML", MediaType.APPLICATION_XML));

		ContentNegotiationManager manager = this.factoryBean.build();
		assertThat(manager.getMediaTypeMappings())
				.hasSize(3)
				.containsEntry("json", MediaType.APPLICATION_JSON)
				.containsEntry("pdf", MediaType.APPLICATION_PDF)
				.containsEntry("xml", MediaType.APPLICATION_XML);
	}

	@Test
	@SuppressWarnings("deprecation")
	void fileExtensions() {
		this.factoryBean.setFavorPathExtension(false);
		this.factoryBean.setFavorParameter(false);

		Properties properties = new Properties();
		properties.put("json", "application/json");
		properties.put("pdf", "application/pdf");
		properties.put("xml", "application/xml");
		this.factoryBean.setMediaTypes(properties);

		this.factoryBean.addMediaType("jsON", MediaType.APPLICATION_JSON);
		this.factoryBean.addMediaType("pdF", MediaType.APPLICATION_PDF);

		this.factoryBean.addMediaTypes(Collections.singletonMap("JSon", MediaType.APPLICATION_JSON));
		this.factoryBean.addMediaTypes(Collections.singletonMap("xML", MediaType.APPLICATION_XML));

		ContentNegotiationManager manager = this.factoryBean.build();
		assertThat(manager.getAllFileExtensions()).containsExactlyInAnyOrder("json", "xml", "pdf");

	}

	@Test
	void ignoreAcceptHeader() throws Exception {
		this.factoryBean.setIgnoreAcceptHeader(true);
		this.factoryBean.setFavorParameter(true);
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addHeader("Accept", MediaType.IMAGE_GIF_VALUE);

		assertThat(manager.resolveMediaTypes(this.webRequest))
				.isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
	}

	@Test
	void setDefaultContentType() throws Exception {
		this.factoryBean.setDefaultContentType(MediaType.APPLICATION_JSON);
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		assertThat(manager.resolveMediaTypes(this.webRequest)).element(0).isEqualTo(MediaType.APPLICATION_JSON);

		// SPR-10513
		this.servletRequest.addHeader("Accept", MediaType.ALL_VALUE);
		assertThat(manager.resolveMediaTypes(this.webRequest)).element(0).isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test // SPR-15367
	void setDefaultContentTypes() throws Exception {
		List<MediaType> mediaTypes = Arrays.asList(MediaType.APPLICATION_JSON, MediaType.ALL);
		this.factoryBean.setDefaultContentTypes(mediaTypes);
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		assertThat(manager.resolveMediaTypes(this.webRequest)).isEqualTo(mediaTypes);

		this.servletRequest.addHeader("Accept", MediaType.ALL_VALUE);
		assertThat(manager.resolveMediaTypes(this.webRequest)).isEqualTo(mediaTypes);
	}

	@Test  // SPR-12286
	void setDefaultContentTypeWithStrategy() throws Exception {
		this.factoryBean.setDefaultContentTypeStrategy(new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON));
		this.factoryBean.afterPropertiesSet();
		ContentNegotiationManager manager = this.factoryBean.getObject();

		assertThat(manager.resolveMediaTypes(this.webRequest))
				.isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));

		this.servletRequest.addHeader("Accept", MediaType.ALL_VALUE);
		assertThat(manager.resolveMediaTypes(this.webRequest))
				.isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));
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
