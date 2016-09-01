/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.config;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityResultHandler;
import org.springframework.web.reactive.result.view.HttpMessageWriterView;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.*;
import static org.springframework.http.MediaType.*;

/**
 * Unit tests for {@link WebReactiveConfiguration}.
 * @author Rossen Stoyanchev
 */
public class WebReactiveConfigurationTests {

	private MockServerHttpRequest request;

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		this.request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(this.request, response, new MockWebSessionManager());
	}


	@Test
	public void requestMappingHandlerMapping() throws Exception {
		ApplicationContext context = loadConfig(WebReactiveConfiguration.class);

		String name = "requestMappingHandlerMapping";
		RequestMappingHandlerMapping mapping = context.getBean(name, RequestMappingHandlerMapping.class);
		assertNotNull(mapping);

		assertEquals(0, mapping.getOrder());

		assertTrue(mapping.useSuffixPatternMatch());
		assertTrue(mapping.useTrailingSlashMatch());
		assertTrue(mapping.useRegisteredSuffixPatternMatch());

		name = "mvcContentTypeResolver";
		RequestedContentTypeResolver resolver = context.getBean(name, RequestedContentTypeResolver.class);
		assertSame(resolver, mapping.getContentTypeResolver());

		this.request.setUri(new URI("/path.json"));
		List<MediaType> list = Collections.singletonList(MediaType.APPLICATION_JSON);
		assertEquals(list, resolver.resolveMediaTypes(this.exchange));

		this.request.setUri(new URI("/path.xml"));
		assertEquals(Collections.emptyList(), resolver.resolveMediaTypes(this.exchange));
	}

	@Test
	public void customPathMatchConfig() throws Exception {
		ApplicationContext context = loadConfig(CustomPatchMatchConfig.class);

		String name = "requestMappingHandlerMapping";
		RequestMappingHandlerMapping mapping = context.getBean(name, RequestMappingHandlerMapping.class);
		assertNotNull(mapping);

		assertFalse(mapping.useSuffixPatternMatch());
		assertFalse(mapping.useTrailingSlashMatch());
	}

	@Test
	public void requestMappingHandlerAdapter() throws Exception {
		ApplicationContext context = loadConfig(WebReactiveConfiguration.class);

		String name = "requestMappingHandlerAdapter";
		RequestMappingHandlerAdapter adapter = context.getBean(name,  RequestMappingHandlerAdapter.class);
		assertNotNull(adapter);

		List<HttpMessageReader<?>> readers = adapter.getMessageReaders();
		assertEquals(5, readers.size());

		assertHasMessageReader(readers, ByteBuffer.class, APPLICATION_OCTET_STREAM);
		assertHasMessageReader(readers, String.class, TEXT_PLAIN);
		assertHasMessageReader(readers, Resource.class, IMAGE_PNG);
		assertHasMessageReader(readers, TestBean.class, APPLICATION_XML);
		assertHasMessageReader(readers, TestBean.class, APPLICATION_JSON);
		assertHasMessageReader(readers, TestBean.class, null);

		name = "mvcConversionService";
		ConversionService service = context.getBean(name, ConversionService.class);
		assertSame(service, adapter.getConversionService());

		name = "mvcValidator";
		Validator validator = context.getBean(name, Validator.class);
		assertSame(validator, adapter.getValidator());
		assertEquals(OptionalValidatorFactoryBean.class, validator.getClass());
	}

	@Test
	public void customMessageConverterConfig() throws Exception {
		ApplicationContext context = loadConfig(CustomMessageConverterConfig.class);

		String name = "requestMappingHandlerAdapter";
		RequestMappingHandlerAdapter adapter = context.getBean(name, RequestMappingHandlerAdapter.class);
		assertNotNull(adapter);

		List<HttpMessageReader<?>> messageReaders = adapter.getMessageReaders();
		assertEquals(2, messageReaders.size());

		assertHasMessageReader(messageReaders, String.class, TEXT_PLAIN);
		assertHasMessageReader(messageReaders, TestBean.class, APPLICATION_XML);
	}

	@Test
	public void responseEntityResultHandler() throws Exception {
		ApplicationContext context = loadConfig(WebReactiveConfiguration.class);

		String name = "responseEntityResultHandler";
		ResponseEntityResultHandler handler = context.getBean(name, ResponseEntityResultHandler.class);
		assertNotNull(handler);

		assertEquals(0, handler.getOrder());

		List<HttpMessageWriter<?>> writers = handler.getMessageWriters();
		assertEquals(6, writers.size());

		assertHasMessageWriter(writers, ByteBuffer.class, APPLICATION_OCTET_STREAM);
		assertHasMessageWriter(writers, String.class, TEXT_PLAIN);
		assertHasMessageWriter(writers, Resource.class, IMAGE_PNG);
		assertHasMessageWriter(writers, TestBean.class, APPLICATION_XML);
		assertHasMessageWriter(writers, TestBean.class, APPLICATION_JSON);
		assertHasMessageWriter(writers, TestBean.class, MediaType.parseMediaType("text/event-stream"));

		name = "mvcContentTypeResolver";
		RequestedContentTypeResolver resolver = context.getBean(name, RequestedContentTypeResolver.class);
		assertSame(resolver, handler.getContentTypeResolver());
	}

	@Test
	public void responseBodyResultHandler() throws Exception {
		ApplicationContext context = loadConfig(WebReactiveConfiguration.class);

		String name = "responseBodyResultHandler";
		ResponseBodyResultHandler handler = context.getBean(name, ResponseBodyResultHandler.class);
		assertNotNull(handler);

		assertEquals(100, handler.getOrder());

		List<HttpMessageWriter<?>> writers = handler.getMessageWriters();
		assertEquals(6, writers.size());

		assertHasMessageWriter(writers, ByteBuffer.class, APPLICATION_OCTET_STREAM);
		assertHasMessageWriter(writers, String.class, TEXT_PLAIN);
		assertHasMessageWriter(writers, Resource.class, IMAGE_PNG);
		assertHasMessageWriter(writers, TestBean.class, APPLICATION_XML);
		assertHasMessageWriter(writers, TestBean.class, APPLICATION_JSON);
		assertHasMessageWriter(writers, TestBean.class, null);

		name = "mvcContentTypeResolver";
		RequestedContentTypeResolver resolver = context.getBean(name, RequestedContentTypeResolver.class);
		assertSame(resolver, handler.getContentTypeResolver());
	}

	@Test
	public void viewResolutionResultHandler() throws Exception {
		ApplicationContext context = loadConfig(CustomViewResolverConfig.class);

		String name = "viewResolutionResultHandler";
		ViewResolutionResultHandler handler = context.getBean(name, ViewResolutionResultHandler.class);
		assertNotNull(handler);

		assertEquals(Ordered.LOWEST_PRECEDENCE, handler.getOrder());

		List<ViewResolver> resolvers = handler.getViewResolvers();
		assertEquals(1, resolvers.size());
		assertEquals(FreeMarkerViewResolver.class, resolvers.get(0).getClass());

		List<View> views = handler.getDefaultViews();
		assertEquals(1, views.size());

		MimeType type = MimeTypeUtils.parseMimeType("application/json;charset=UTF-8");
		assertEquals(type, views.get(0).getSupportedMediaTypes().get(0));
	}


	private void assertHasMessageReader(List<HttpMessageReader<?>> readers, Class<?> clazz, MediaType mediaType) {
		ResolvableType type = ResolvableType.forClass(clazz);
		assertTrue(readers.stream()
				.filter(c -> mediaType == null || c.canRead(type, mediaType))
				.findAny()
				.isPresent());
	}

	private void assertHasMessageWriter(List<HttpMessageWriter<?>> writers, Class<?> clazz, MediaType mediaType) {
		ResolvableType type = ResolvableType.forClass(clazz);
		assertTrue(writers.stream()
				.filter(c -> mediaType == null || c.canWrite(type, mediaType))
				.findAny()
				.isPresent());
	}

	private ApplicationContext loadConfig(Class<?>... configurationClasses) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(configurationClasses);
		context.refresh();
		return context;
	}


	@Configuration
	static class CustomPatchMatchConfig extends WebReactiveConfiguration {

		@Override
		public void configurePathMatching(PathMatchConfigurer configurer) {
			configurer.setUseSuffixPatternMatch(false);
			configurer.setUseTrailingSlashMatch(false);
		}
	}

	@Configuration
	static class CustomMessageConverterConfig extends WebReactiveConfiguration {

		@Override
		protected void configureMessageReaders(List<HttpMessageReader<?>> messageReaders) {
			messageReaders.add(new DecoderHttpMessageReader<>(new StringDecoder()));
		}

		@Override
		protected void configureMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
			messageWriters.add(new EncoderHttpMessageWriter<>(new CharSequenceEncoder()));
		}

		@Override
		protected void extendMessageReaders(List<HttpMessageReader<?>> messageReaders) {
			messageReaders.add(new DecoderHttpMessageReader<>(new Jaxb2XmlDecoder()));
		}

		@Override
		protected void extendMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
			messageWriters.add(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
		}
	}

	@Configuration @SuppressWarnings("unused")
	static class CustomViewResolverConfig extends WebReactiveConfiguration {

		@Override
		protected void configureViewResolvers(ViewResolverRegistry registry) {
			registry.freeMarker();
			registry.defaultViews(new HttpMessageWriterView(new Jackson2JsonEncoder()));
		}

		@Bean
		public FreeMarkerConfigurer freeMarkerConfig() {
			return new FreeMarkerConfigurer();
		}

	}

	@XmlRootElement
	static class TestBean {
	}
}
