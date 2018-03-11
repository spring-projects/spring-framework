/*
 * Copyright 2002-2018 the original author or authors.
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

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

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
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.handler.AbstractUrlHandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
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
import org.springframework.web.server.WebHandler;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.junit.Assert.*;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.core.ResolvableType.forClassWithGenerics;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.IMAGE_PNG;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.get;

/**
 * Unit tests for {@link WebFluxConfigurationSupport}.
 *
 * @author Rossen Stoyanchev
 */
public class WebFluxConfigurationSupportTests {

	@Test
	public void requestMappingHandlerMapping() throws Exception {
		ApplicationContext context = loadConfig(WebFluxConfig.class);
		final Field field = ReflectionUtils.findField(PathPatternParser.class, "matchOptionalTrailingSeparator");
		ReflectionUtils.makeAccessible(field);

		String name = "requestMappingHandlerMapping";
		RequestMappingHandlerMapping mapping = context.getBean(name, RequestMappingHandlerMapping.class);
		assertNotNull(mapping);

		assertEquals(0, mapping.getOrder());

		PathPatternParser patternParser = mapping.getPathPatternParser();
		assertNotNull(patternParser);
		boolean matchOptionalTrailingSlash = (boolean) ReflectionUtils.getField(field, patternParser);
		assertTrue(matchOptionalTrailingSlash);

		name = "webFluxContentTypeResolver";
		RequestedContentTypeResolver resolver = context.getBean(name, RequestedContentTypeResolver.class);
		assertSame(resolver, mapping.getContentTypeResolver());

		ServerWebExchange exchange = MockServerWebExchange.from(get("/path").accept(MediaType.APPLICATION_JSON));
		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON), resolver.resolveMediaTypes(exchange));
	}

	@Test
	public void customPathMatchConfig() throws Exception {
		ApplicationContext context = loadConfig(CustomPatchMatchConfig.class);
		final Field field = ReflectionUtils.findField(PathPatternParser.class, "matchOptionalTrailingSeparator");
		ReflectionUtils.makeAccessible(field);

		String name = "requestMappingHandlerMapping";
		RequestMappingHandlerMapping mapping = context.getBean(name, RequestMappingHandlerMapping.class);
		assertNotNull(mapping);

		PathPatternParser patternParser = mapping.getPathPatternParser();
		assertNotNull(patternParser);
		boolean matchOptionalTrailingSlash = (boolean) ReflectionUtils.getField(field, patternParser);
		assertFalse(matchOptionalTrailingSlash);
	}

	@Test
	public void requestMappingHandlerAdapter() throws Exception {
		ApplicationContext context = loadConfig(WebFluxConfig.class);

		String name = "requestMappingHandlerAdapter";
		RequestMappingHandlerAdapter adapter = context.getBean(name, RequestMappingHandlerAdapter.class);
		assertNotNull(adapter);

		List<HttpMessageReader<?>> readers = adapter.getMessageReaders();
		assertEquals(12, readers.size());

		ResolvableType multiValueMapType = forClassWithGenerics(MultiValueMap.class, String.class, String.class);

		assertHasMessageReader(readers, forClass(byte[].class), APPLICATION_OCTET_STREAM);
		assertHasMessageReader(readers, forClass(ByteBuffer.class), APPLICATION_OCTET_STREAM);
		assertHasMessageReader(readers, forClass(String.class), TEXT_PLAIN);
		assertHasMessageReader(readers, forClass(Resource.class), IMAGE_PNG);
		assertHasMessageReader(readers, multiValueMapType, APPLICATION_FORM_URLENCODED);
		assertHasMessageReader(readers, forClass(TestBean.class), APPLICATION_XML);
		assertHasMessageReader(readers, forClass(TestBean.class), APPLICATION_JSON);
		assertHasMessageReader(readers, forClass(TestBean.class), new MediaType("application", "x-jackson-smile"));
		assertHasMessageReader(readers, forClass(TestBean.class), null);

		WebBindingInitializer bindingInitializer = adapter.getWebBindingInitializer();
		assertNotNull(bindingInitializer);
		WebExchangeDataBinder binder = new WebExchangeDataBinder(new Object());
		bindingInitializer.initBinder(binder);

		name = "webFluxConversionService";
		ConversionService service = context.getBean(name, ConversionService.class);
		assertSame(service, binder.getConversionService());

		name = "webFluxValidator";
		Validator validator = context.getBean(name, Validator.class);
		assertSame(validator, binder.getValidator());
	}

	@Test
	public void customMessageConverterConfig() throws Exception {
		ApplicationContext context = loadConfig(CustomMessageConverterConfig.class);

		String name = "requestMappingHandlerAdapter";
		RequestMappingHandlerAdapter adapter = context.getBean(name, RequestMappingHandlerAdapter.class);
		assertNotNull(adapter);

		List<HttpMessageReader<?>> messageReaders = adapter.getMessageReaders();
		assertEquals(2, messageReaders.size());

		assertHasMessageReader(messageReaders, forClass(String.class), TEXT_PLAIN);
		assertHasMessageReader(messageReaders, forClass(TestBean.class), APPLICATION_XML);
	}

	@Test
	public void responseEntityResultHandler() throws Exception {
		ApplicationContext context = loadConfig(WebFluxConfig.class);

		String name = "responseEntityResultHandler";
		ResponseEntityResultHandler handler = context.getBean(name, ResponseEntityResultHandler.class);
		assertNotNull(handler);

		assertEquals(0, handler.getOrder());

		List<HttpMessageWriter<?>> writers = handler.getMessageWriters();
		assertEquals(10, writers.size());

		assertHasMessageWriter(writers, forClass(byte[].class), APPLICATION_OCTET_STREAM);
		assertHasMessageWriter(writers, forClass(ByteBuffer.class), APPLICATION_OCTET_STREAM);
		assertHasMessageWriter(writers, forClass(String.class), TEXT_PLAIN);
		assertHasMessageWriter(writers, forClass(Resource.class), IMAGE_PNG);
		assertHasMessageWriter(writers, forClass(TestBean.class), APPLICATION_XML);
		assertHasMessageWriter(writers, forClass(TestBean.class), APPLICATION_JSON);
		assertHasMessageWriter(writers, forClass(TestBean.class), new MediaType("application", "x-jackson-smile"));
		assertHasMessageWriter(writers, forClass(TestBean.class), MediaType.parseMediaType("text/event-stream"));

		name = "webFluxContentTypeResolver";
		RequestedContentTypeResolver resolver = context.getBean(name, RequestedContentTypeResolver.class);
		assertSame(resolver, handler.getContentTypeResolver());
	}

	@Test
	public void responseBodyResultHandler() throws Exception {
		ApplicationContext context = loadConfig(WebFluxConfig.class);

		String name = "responseBodyResultHandler";
		ResponseBodyResultHandler handler = context.getBean(name, ResponseBodyResultHandler.class);
		assertNotNull(handler);

		assertEquals(100, handler.getOrder());

		List<HttpMessageWriter<?>> writers = handler.getMessageWriters();
		assertEquals(10, writers.size());

		assertHasMessageWriter(writers, forClass(byte[].class), APPLICATION_OCTET_STREAM);
		assertHasMessageWriter(writers, forClass(ByteBuffer.class), APPLICATION_OCTET_STREAM);
		assertHasMessageWriter(writers, forClass(String.class), TEXT_PLAIN);
		assertHasMessageWriter(writers, forClass(Resource.class), IMAGE_PNG);
		assertHasMessageWriter(writers, forClass(TestBean.class), APPLICATION_XML);
		assertHasMessageWriter(writers, forClass(TestBean.class), APPLICATION_JSON);
		assertHasMessageWriter(writers, forClass(TestBean.class), new MediaType("application", "x-jackson-smile"));
		assertHasMessageWriter(writers, forClass(TestBean.class), null);

		name = "webFluxContentTypeResolver";
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

	@Test
	public void resourceHandler() throws Exception {
		ApplicationContext context = loadConfig(CustomResourceHandlingConfig.class);

		String name = "resourceHandlerMapping";
		AbstractUrlHandlerMapping handlerMapping = context.getBean(name, AbstractUrlHandlerMapping.class);
		assertNotNull(handlerMapping);

		assertEquals(Ordered.LOWEST_PRECEDENCE - 1, handlerMapping.getOrder());

		SimpleUrlHandlerMapping urlHandlerMapping = (SimpleUrlHandlerMapping) handlerMapping;
		WebHandler webHandler = (WebHandler) urlHandlerMapping.getUrlMap().get("/images/**");
		assertNotNull(webHandler);
	}


	private void assertHasMessageReader(List<HttpMessageReader<?>> readers, ResolvableType type, MediaType mediaType) {
		assertTrue(readers.stream().anyMatch(c -> mediaType == null || c.canRead(type, mediaType)));
	}

	private void assertHasMessageWriter(List<HttpMessageWriter<?>> writers, ResolvableType type, MediaType mediaType) {
		assertTrue(writers.stream().anyMatch(c -> mediaType == null || c.canWrite(type, mediaType)));
	}

	private ApplicationContext loadConfig(Class<?>... configurationClasses) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(configurationClasses);
		context.refresh();
		return context;
	}


	@EnableWebFlux
	static class WebFluxConfig {
	}


	@Configuration
	static class CustomPatchMatchConfig extends WebFluxConfigurationSupport {

		@Override
		public void configurePathMatching(PathMatchConfigurer configurer) {
			configurer.setUseTrailingSlashMatch(false);
		}
	}


	@Configuration
	static class CustomMessageConverterConfig extends WebFluxConfigurationSupport {

		@Override
		protected void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
			configurer.registerDefaults(false);
			configurer.customCodecs().decoder(StringDecoder.textPlainOnly());
			configurer.customCodecs().decoder(new Jaxb2XmlDecoder());
			configurer.customCodecs().encoder(CharSequenceEncoder.textPlainOnly());
			configurer.customCodecs().encoder(new Jaxb2XmlEncoder());
		}
	}


	@Configuration
	@SuppressWarnings("unused")
	static class CustomViewResolverConfig extends WebFluxConfigurationSupport {

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


	@Configuration
	static class CustomResourceHandlingConfig extends WebFluxConfigurationSupport {

		@Override
		protected void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/images/**").addResourceLocations("/images/");
		}
	}


	@XmlRootElement
	static class TestBean {
	}

}
