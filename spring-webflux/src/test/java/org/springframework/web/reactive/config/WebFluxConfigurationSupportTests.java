/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.config;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.protobuf.Message;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.junit.jupiter.api.Test;

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
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.handler.AbstractUrlHandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.resource.ResourceUrlProvider;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
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
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.core.ResolvableType.forClassWithGenerics;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.IMAGE_PNG;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * Unit tests for {@link WebFluxConfigurationSupport}.
 *
 * @author Rossen Stoyanchev
 */
public class WebFluxConfigurationSupportTests {

	@Test
	public void requestMappingHandlerMapping() {
		ApplicationContext context = loadConfig(WebFluxConfig.class);
		final Field field = ReflectionUtils.findField(PathPatternParser.class, "matchOptionalTrailingSeparator");
		ReflectionUtils.makeAccessible(field);

		String name = "requestMappingHandlerMapping";
		RequestMappingHandlerMapping mapping = context.getBean(name, RequestMappingHandlerMapping.class);
		assertThat(mapping).isNotNull();

		assertThat(mapping.getOrder()).isEqualTo(0);

		PathPatternParser patternParser = mapping.getPathPatternParser();
		assertThat(patternParser).isNotNull();
		boolean matchOptionalTrailingSlash = (boolean) ReflectionUtils.getField(field, patternParser);
		assertThat(matchOptionalTrailingSlash).isTrue();

		name = "webFluxContentTypeResolver";
		RequestedContentTypeResolver resolver = context.getBean(name, RequestedContentTypeResolver.class);
		assertThat(mapping.getContentTypeResolver()).isSameAs(resolver);

		ServerWebExchange exchange = MockServerWebExchange.from(get("/path").accept(MediaType.APPLICATION_JSON));
		assertThat(resolver.resolveMediaTypes(exchange))
				.isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));
	}

	@Test
	public void customPathMatchConfig() {
		ApplicationContext context = loadConfig(CustomPatchMatchConfig.class);
		final Field field = ReflectionUtils.findField(PathPatternParser.class, "matchOptionalTrailingSeparator");
		ReflectionUtils.makeAccessible(field);

		String name = "requestMappingHandlerMapping";
		RequestMappingHandlerMapping mapping = context.getBean(name, RequestMappingHandlerMapping.class);
		assertThat(mapping).isNotNull();

		PathPatternParser patternParser = mapping.getPathPatternParser();
		assertThat(patternParser).isNotNull();
		boolean matchOptionalTrailingSlash = (boolean) ReflectionUtils.getField(field, patternParser);
		assertThat(matchOptionalTrailingSlash).isFalse();

		Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();
		assertThat(map.size()).isEqualTo(1);
		assertThat(map.keySet().iterator().next().getPatternsCondition().getPatterns())
				.isEqualTo(Collections.singleton(new PathPatternParser().parse("/api/user/{id}")));
	}

	@Test
	public void requestMappingHandlerAdapter() {
		ApplicationContext context = loadConfig(WebFluxConfig.class);

		String name = "requestMappingHandlerAdapter";
		RequestMappingHandlerAdapter adapter = context.getBean(name, RequestMappingHandlerAdapter.class);
		assertThat(adapter).isNotNull();

		List<HttpMessageReader<?>> readers = adapter.getMessageReaders();
		assertThat(readers.size()).isEqualTo(14);

		ResolvableType multiValueMapType = forClassWithGenerics(MultiValueMap.class, String.class, String.class);

		assertHasMessageReader(readers, forClass(byte[].class), APPLICATION_OCTET_STREAM);
		assertHasMessageReader(readers, forClass(ByteBuffer.class), APPLICATION_OCTET_STREAM);
		assertHasMessageReader(readers, forClass(String.class), TEXT_PLAIN);
		assertHasMessageReader(readers, forClass(Resource.class), IMAGE_PNG);
		assertHasMessageReader(readers, forClass(Message.class), new MediaType("application", "x-protobuf"));
		assertHasMessageReader(readers, multiValueMapType, APPLICATION_FORM_URLENCODED);
		assertHasMessageReader(readers, forClass(TestBean.class), APPLICATION_XML);
		assertHasMessageReader(readers, forClass(TestBean.class), APPLICATION_JSON);
		assertHasMessageReader(readers, forClass(TestBean.class), new MediaType("application", "x-jackson-smile"));
		assertHasMessageReader(readers, forClass(TestBean.class), null);

		WebBindingInitializer bindingInitializer = adapter.getWebBindingInitializer();
		assertThat(bindingInitializer).isNotNull();
		WebExchangeDataBinder binder = new WebExchangeDataBinder(new Object());
		bindingInitializer.initBinder(binder);

		name = "webFluxConversionService";
		ConversionService service = context.getBean(name, ConversionService.class);
		assertThat(binder.getConversionService()).isSameAs(service);

		name = "webFluxValidator";
		Validator validator = context.getBean(name, Validator.class);
		assertThat(binder.getValidator()).isSameAs(validator);
	}

	@Test
	public void customMessageConverterConfig() {
		ApplicationContext context = loadConfig(CustomMessageConverterConfig.class);

		String name = "requestMappingHandlerAdapter";
		RequestMappingHandlerAdapter adapter = context.getBean(name, RequestMappingHandlerAdapter.class);
		assertThat(adapter).isNotNull();

		List<HttpMessageReader<?>> messageReaders = adapter.getMessageReaders();
		assertThat(messageReaders.size()).isEqualTo(2);

		assertHasMessageReader(messageReaders, forClass(String.class), TEXT_PLAIN);
		assertHasMessageReader(messageReaders, forClass(TestBean.class), APPLICATION_XML);
	}

	@Test
	public void responseEntityResultHandler() {
		ApplicationContext context = loadConfig(WebFluxConfig.class);

		String name = "responseEntityResultHandler";
		ResponseEntityResultHandler handler = context.getBean(name, ResponseEntityResultHandler.class);
		assertThat(handler).isNotNull();

		assertThat(handler.getOrder()).isEqualTo(0);

		List<HttpMessageWriter<?>> writers = handler.getMessageWriters();
		assertThat(writers.size()).isEqualTo(13);

		assertHasMessageWriter(writers, forClass(byte[].class), APPLICATION_OCTET_STREAM);
		assertHasMessageWriter(writers, forClass(ByteBuffer.class), APPLICATION_OCTET_STREAM);
		assertHasMessageWriter(writers, forClass(String.class), TEXT_PLAIN);
		assertHasMessageWriter(writers, forClass(Resource.class), IMAGE_PNG);
		assertHasMessageWriter(writers, forClass(Message.class), new MediaType("application", "x-protobuf"));
		assertHasMessageWriter(writers, forClass(TestBean.class), APPLICATION_XML);
		assertHasMessageWriter(writers, forClass(TestBean.class), APPLICATION_JSON);
		assertHasMessageWriter(writers, forClass(TestBean.class), new MediaType("application", "x-jackson-smile"));
		assertHasMessageWriter(writers, forClass(TestBean.class), MediaType.parseMediaType("text/event-stream"));

		name = "webFluxContentTypeResolver";
		RequestedContentTypeResolver resolver = context.getBean(name, RequestedContentTypeResolver.class);
		assertThat(handler.getContentTypeResolver()).isSameAs(resolver);
	}

	@Test
	public void responseBodyResultHandler() {
		ApplicationContext context = loadConfig(WebFluxConfig.class);

		String name = "responseBodyResultHandler";
		ResponseBodyResultHandler handler = context.getBean(name, ResponseBodyResultHandler.class);
		assertThat(handler).isNotNull();

		assertThat(handler.getOrder()).isEqualTo(100);

		List<HttpMessageWriter<?>> writers = handler.getMessageWriters();
		assertThat(writers.size()).isEqualTo(13);

		assertHasMessageWriter(writers, forClass(byte[].class), APPLICATION_OCTET_STREAM);
		assertHasMessageWriter(writers, forClass(ByteBuffer.class), APPLICATION_OCTET_STREAM);
		assertHasMessageWriter(writers, forClass(String.class), TEXT_PLAIN);
		assertHasMessageWriter(writers, forClass(Resource.class), IMAGE_PNG);
		assertHasMessageWriter(writers, forClass(Message.class), new MediaType("application", "x-protobuf"));
		assertHasMessageWriter(writers, forClass(TestBean.class), APPLICATION_XML);
		assertHasMessageWriter(writers, forClass(TestBean.class), APPLICATION_JSON);
		assertHasMessageWriter(writers, forClass(TestBean.class), new MediaType("application", "x-jackson-smile"));
		assertHasMessageWriter(writers, forClass(TestBean.class), null);

		name = "webFluxContentTypeResolver";
		RequestedContentTypeResolver resolver = context.getBean(name, RequestedContentTypeResolver.class);
		assertThat(handler.getContentTypeResolver()).isSameAs(resolver);
	}

	@Test
	public void viewResolutionResultHandler() {
		ApplicationContext context = loadConfig(CustomViewResolverConfig.class);

		String name = "viewResolutionResultHandler";
		ViewResolutionResultHandler handler = context.getBean(name, ViewResolutionResultHandler.class);
		assertThat(handler).isNotNull();

		assertThat(handler.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);

		List<ViewResolver> resolvers = handler.getViewResolvers();
		assertThat(resolvers.size()).isEqualTo(1);
		assertThat(resolvers.get(0).getClass()).isEqualTo(FreeMarkerViewResolver.class);

		List<View> views = handler.getDefaultViews();
		assertThat(views.size()).isEqualTo(1);

		MimeType type = MimeTypeUtils.parseMimeType("application/json");
		assertThat(views.get(0).getSupportedMediaTypes().get(0)).isEqualTo(type);
	}

	@Test
	public void resourceHandler() {
		ApplicationContext context = loadConfig(CustomResourceHandlingConfig.class);

		String name = "resourceHandlerMapping";
		AbstractUrlHandlerMapping handlerMapping = context.getBean(name, AbstractUrlHandlerMapping.class);
		assertThat(handlerMapping).isNotNull();

		assertThat(handlerMapping.getOrder()).isEqualTo((Ordered.LOWEST_PRECEDENCE - 1));

		SimpleUrlHandlerMapping urlHandlerMapping = (SimpleUrlHandlerMapping) handlerMapping;
		WebHandler webHandler = (WebHandler) urlHandlerMapping.getUrlMap().get("/images/**");
		assertThat(webHandler).isNotNull();
	}

	@Test
	public void resourceUrlProvider() throws Exception {
		ApplicationContext context = loadConfig(WebFluxConfig.class);

		String name = "resourceUrlProvider";
		ResourceUrlProvider resourceUrlProvider = context.getBean(name, ResourceUrlProvider.class);
		assertThat(resourceUrlProvider).isNotNull();
	}


	private void assertHasMessageReader(List<HttpMessageReader<?>> readers, ResolvableType type, MediaType mediaType) {
		assertThat(readers.stream().anyMatch(c -> mediaType == null || c.canRead(type, mediaType))).isTrue();
	}

	private void assertHasMessageWriter(List<HttpMessageWriter<?>> writers, ResolvableType type, MediaType mediaType) {
		assertThat(writers.stream().anyMatch(c -> mediaType == null || c.canWrite(type, mediaType))).isTrue();
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
			configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class));
		}

		@Bean
		UserController userController() {
			return new UserController();
		}
	}


	@RestController
	@RequestMapping("/user")
	static class UserController {

		@GetMapping("/{id}")
		public Principal getUser() {
			return mock(Principal.class);
		}
	}


	@Configuration
	static class CustomMessageConverterConfig extends WebFluxConfigurationSupport {

		@Override
		protected void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
			configurer.registerDefaults(false);
			configurer.customCodecs().register(StringDecoder.textPlainOnly());
			configurer.customCodecs().register(new Jaxb2XmlDecoder());
			configurer.customCodecs().register(CharSequenceEncoder.textPlainOnly());
			configurer.customCodecs().register(new Jaxb2XmlEncoder());
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
