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

package org.springframework.web.servlet.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.RequestDispatcher;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.tiles.definition.UnresolvingLocaleDefinitionsFactory;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.TypeMismatchException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.util.PathMatcher;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.ThemeResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.handler.UserRoleAuthorizationInterceptor;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.CachingResourceResolver;
import org.springframework.web.servlet.resource.CachingResourceTransformer;
import org.springframework.web.servlet.resource.ContentVersionStrategy;
import org.springframework.web.servlet.resource.CssLinkResourceTransformer;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.FixedVersionStrategy;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceTransformer;
import org.springframework.web.servlet.resource.ResourceUrlProvider;
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;
import org.springframework.web.servlet.resource.VersionResourceResolver;
import org.springframework.web.servlet.resource.WebJarsResourceResolver;
import org.springframework.web.servlet.theme.ThemeChangeInterceptor;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.view.ViewResolverComposite;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;
import org.springframework.web.servlet.view.groovy.GroovyMarkupConfigurer;
import org.springframework.web.servlet.view.groovy.GroovyMarkupViewResolver;
import org.springframework.web.servlet.view.script.ScriptTemplateConfigurer;
import org.springframework.web.servlet.view.script.ScriptTemplateViewResolver;
import org.springframework.web.servlet.view.tiles3.SpringBeanPreparerFactory;
import org.springframework.web.servlet.view.tiles3.TilesConfigurer;
import org.springframework.web.servlet.view.tiles3.TilesViewResolver;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockRequestDispatcher;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.UrlPathHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests loading actual MVC namespace configuration.
 *
 * @author Keith Donald
 * @author Arjen Poutsma
 * @author Jeremy Grelle
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Kazuki Shimizu
 * @author Sam Brannen
 * @author Marten Deinum
 */
public class MvcNamespaceTests {

	public static final String VIEWCONTROLLER_BEAN_NAME =
			"org.springframework.web.servlet.config.viewControllerHandlerMapping";


	private XmlWebApplicationContext appContext;

	private TestController handler;

	private HandlerMethod handlerMethod;


	@BeforeEach
	public void setup() throws Exception {
		TestMockServletContext servletContext = new TestMockServletContext();
		appContext = new XmlWebApplicationContext();
		appContext.setServletContext(servletContext);
		LocaleContextHolder.setLocale(Locale.US);

		String attributeName = WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE;
		appContext.getServletContext().setAttribute(attributeName, appContext);

		handler = new TestController();
		handlerMethod = new InvocableHandlerMethod(handler, TestController.class.getMethod("testBind",
				Date.class, Double.class, TestBean.class, BindingResult.class));
	}


	@Test
	public void testDefaultConfig() throws Exception {
		loadBeanDefinitions("mvc-config.xml");

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertThat(mapping).isNotNull();
		assertThat(mapping.getOrder()).isEqualTo(0);
		assertThat(mapping.getUrlPathHelper().shouldRemoveSemicolonContent()).isTrue();
		mapping.setDefaultHandler(handlerMethod);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo.json");
		NativeWebRequest webRequest = new ServletWebRequest(request);
		ContentNegotiationManager manager = mapping.getContentNegotiationManager();
		assertThat(manager.resolveMediaTypes(webRequest)).isEqualTo(Collections.singletonList(MediaType.APPLICATION_JSON));

		RequestMappingHandlerAdapter adapter = appContext.getBean(RequestMappingHandlerAdapter.class);
		assertThat(adapter).isNotNull();
		assertThat(new DirectFieldAccessor(adapter).getPropertyValue("ignoreDefaultModelOnRedirect")).isEqualTo(false);

		List<HttpMessageConverter<?>> converters = adapter.getMessageConverters();
		assertThat(converters.size() > 0).isTrue();
		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof AbstractJackson2HttpMessageConverter) {
				ObjectMapper objectMapper = ((AbstractJackson2HttpMessageConverter) converter).getObjectMapper();
				assertThat(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isFalse();
				assertThat(objectMapper.getSerializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isFalse();
				assertThat(objectMapper.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
				if (converter instanceof MappingJackson2XmlHttpMessageConverter) {
					assertThat(objectMapper.getClass()).isEqualTo(XmlMapper.class);
				}
			}
		}

		assertThat(appContext.getBean(FormattingConversionServiceFactoryBean.class)).isNotNull();
		assertThat(appContext.getBean(ConversionService.class)).isNotNull();
		assertThat(appContext.getBean(LocalValidatorFactoryBean.class)).isNotNull();
		assertThat(appContext.getBean(Validator.class)).isNotNull();
		assertThat(appContext.getBean("themeResolver", ThemeResolver.class)).isNotNull();
		assertThat(appContext.getBean("localeResolver", LocaleResolver.class)).isNotNull();
		assertThat(appContext.getBean("flashMapManager", FlashMapManager.class)).isNotNull();
		assertThat(appContext.getBean("viewNameTranslator", RequestToViewNameTranslator.class)).isNotNull();

		// default web binding initializer behavior test
		request = new MockHttpServletRequest("GET", "/");
		request.addParameter("date", "2009-10-31");
		request.addParameter("percent", "99.99%");
		MockHttpServletResponse response = new MockHttpServletResponse();

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(1);
		assertThat(chain.getInterceptorList().get(0) instanceof ConversionServiceExposingInterceptor).isTrue();
		ConversionServiceExposingInterceptor interceptor = (ConversionServiceExposingInterceptor) chain.getInterceptorList().get(0);
		interceptor.preHandle(request, response, handlerMethod);
		assertThat(request.getAttribute(ConversionService.class.getName())).isSameAs(appContext.getBean(ConversionService.class));

		adapter.handle(request, response, handlerMethod);
		assertThat(handler.recordedValidationError).isTrue();
		assertThat(handler.date).isEqualTo(LocalDate.parse("2009-10-31").toDate());
		assertThat(handler.percent).isEqualTo(Double.valueOf(0.9999));

		CompositeUriComponentsContributor uriComponentsContributor = this.appContext.getBean(
				MvcUriComponentsBuilder.MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME,
				CompositeUriComponentsContributor.class);

		assertThat(uriComponentsContributor).isNotNull();

		String name = "mvcHandlerMappingIntrospector";
		HandlerMappingIntrospector introspector = this.appContext.getBean(name, HandlerMappingIntrospector.class);
		assertThat(introspector).isNotNull();
		assertThat(introspector.getHandlerMappings().size()).isEqualTo(2);
		assertThat(introspector.getHandlerMappings().get(0)).isSameAs(mapping);
		assertThat(introspector.getHandlerMappings().get(1).getClass()).isEqualTo(BeanNameUrlHandlerMapping.class);
	}

	@Test
	public void testCustomConversionService() throws Exception {
		loadBeanDefinitions("mvc-config-custom-conversion-service.xml");

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertThat(mapping).isNotNull();
		mapping.setDefaultHandler(handlerMethod);

		// default web binding initializer behavior test
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setRequestURI("/accounts/12345");
		request.addParameter("date", "2009-10-31");
		MockHttpServletResponse response = new MockHttpServletResponse();

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(1);
		assertThat(chain.getInterceptorList().get(0) instanceof ConversionServiceExposingInterceptor).isTrue();
		ConversionServiceExposingInterceptor interceptor = (ConversionServiceExposingInterceptor) chain.getInterceptorList().get(0);
		interceptor.preHandle(request, response, handler);
		assertThat(request.getAttribute(ConversionService.class.getName())).isSameAs(appContext.getBean("conversionService"));

		RequestMappingHandlerAdapter adapter = appContext.getBean(RequestMappingHandlerAdapter.class);
		assertThat(adapter).isNotNull();
		assertThatExceptionOfType(TypeMismatchException.class).isThrownBy(() ->
				adapter.handle(request, response, handlerMethod));
	}

	@Test
	public void testCustomValidator() throws Exception {
		doTestCustomValidator("mvc-config-custom-validator.xml");
	}

	private void doTestCustomValidator(String xml) throws Exception {
		loadBeanDefinitions(xml);

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertThat(mapping).isNotNull();
		assertThat(mapping.getUrlPathHelper().shouldRemoveSemicolonContent()).isFalse();

		RequestMappingHandlerAdapter adapter = appContext.getBean(RequestMappingHandlerAdapter.class);
		assertThat(adapter).isNotNull();
		assertThat(new DirectFieldAccessor(adapter).getPropertyValue("ignoreDefaultModelOnRedirect")).isEqualTo(true);

		// default web binding initializer behavior test
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("date", "2009-10-31");
		MockHttpServletResponse response = new MockHttpServletResponse();
		adapter.handle(request, response, handlerMethod);

		assertThat(appContext.getBean(TestValidator.class).validatorInvoked).isTrue();
		assertThat(handler.recordedValidationError).isFalse();
	}

	@Test
	public void testInterceptors() throws Exception {
		loadBeanDefinitions("mvc-config-interceptors.xml");

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertThat(mapping).isNotNull();
		mapping.setDefaultHandler(handlerMethod);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setRequestURI("/accounts/12345");
		request.addParameter("locale", "en");
		request.addParameter("theme", "green");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(4);
		assertThat(chain.getInterceptorList().get(0) instanceof ConversionServiceExposingInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(1) instanceof LocaleChangeInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(2) instanceof ThemeChangeInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(3) instanceof UserRoleAuthorizationInterceptor).isTrue();

		request.setRequestURI("/admin/users");
		chain = mapping.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(2);

		request.setRequestURI("/logged/accounts/12345");
		chain = mapping.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(3);

		request.setRequestURI("/foo/logged");
		chain = mapping.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(3);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testResources() throws Exception {
		loadBeanDefinitions("mvc-config-resources.xml");

		HttpRequestHandlerAdapter adapter = appContext.getBean(HttpRequestHandlerAdapter.class);
		assertThat(adapter).isNotNull();

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		ContentNegotiationManager manager = mapping.getContentNegotiationManager();

		ResourceHttpRequestHandler handler = appContext.getBean(ResourceHttpRequestHandler.class);
		assertThat(handler).isNotNull();
		assertThat(handler.getContentNegotiationManager()).isSameAs(manager);

		SimpleUrlHandlerMapping resourceMapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertThat(resourceMapping).isNotNull();
		assertThat(resourceMapping.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE - 1);

		BeanNameUrlHandlerMapping beanNameMapping = appContext.getBean(BeanNameUrlHandlerMapping.class);
		assertThat(beanNameMapping).isNotNull();
		assertThat(beanNameMapping.getOrder()).isEqualTo(2);

		ResourceUrlProvider urlProvider = appContext.getBean(ResourceUrlProvider.class);
		assertThat(urlProvider).isNotNull();

		Map<String, MappedInterceptor> beans = appContext.getBeansOfType(MappedInterceptor.class);
		List<Class<?>> interceptors = beans.values().stream()
				.map(mappedInterceptor -> mappedInterceptor.getInterceptor().getClass())
				.collect(Collectors.toList());
		assertThat(interceptors).contains(ConversionServiceExposingInterceptor.class,
				ResourceUrlProviderExposingInterceptor.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/resources/foo.css");
		request.setMethod("GET");

		HandlerExecutionChain chain = resourceMapping.getHandler(request);
		assertThat(chain).isNotNull();
		assertThat(chain.getHandler() instanceof ResourceHttpRequestHandler).isTrue();

		MockHttpServletResponse response = new MockHttpServletResponse();
		for (HandlerInterceptor interceptor : chain.getInterceptorList()) {
			interceptor.preHandle(request, response, chain.getHandler());
		}
		ModelAndView mv = adapter.handle(request, response, chain.getHandler());
		assertThat((Object) mv).isNull();
	}

	@Test
	public void testResourcesWithOptionalAttributes() throws Exception {
		loadBeanDefinitions("mvc-config-resources-optional-attrs.xml");

		SimpleUrlHandlerMapping mapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertThat(mapping).isNotNull();
		assertThat(mapping.getOrder()).isEqualTo(5);
		assertThat(mapping.getUrlMap().get("/resources/**")).isNotNull();

		ResourceHttpRequestHandler handler = appContext.getBean((String) mapping.getUrlMap().get("/resources/**"),
				ResourceHttpRequestHandler.class);
		assertThat(handler).isNotNull();
		assertThat(handler.getCacheSeconds()).isEqualTo(3600);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testResourcesWithResolversTransformers() throws Exception {
		loadBeanDefinitions("mvc-config-resources-chain.xml");

		SimpleUrlHandlerMapping mapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertThat(mapping).isNotNull();
		assertThat(mapping.getUrlMap().get("/resources/**")).isNotNull();
		String beanName = (String) mapping.getUrlMap().get("/resources/**");
		ResourceHttpRequestHandler handler = appContext.getBean(beanName, ResourceHttpRequestHandler.class);
		assertThat(handler).isNotNull();

		assertThat(handler.getUrlPathHelper()).isNotNull();

		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		assertThat(resolvers).hasSize(4);
		assertThat(resolvers.get(0)).isInstanceOf(CachingResourceResolver.class);
		assertThat(resolvers.get(1)).isInstanceOf(VersionResourceResolver.class);
		assertThat(resolvers.get(2)).isInstanceOf(WebJarsResourceResolver.class);
		assertThat(resolvers.get(3)).isInstanceOf(PathResourceResolver.class);

		CachingResourceResolver cachingResolver = (CachingResourceResolver) resolvers.get(0);
		assertThat(cachingResolver.getCache()).isInstanceOf(ConcurrentMapCache.class);
		assertThat(cachingResolver.getCache().getName()).isEqualTo("test-resource-cache");

		VersionResourceResolver versionResolver = (VersionResourceResolver) resolvers.get(1);
		assertThat(versionResolver.getStrategyMap().get("/**/*.js"))
				.isInstanceOf(FixedVersionStrategy.class);
		assertThat(versionResolver.getStrategyMap().get("/**"))
				.isInstanceOf(ContentVersionStrategy.class);

		PathResourceResolver pathResolver = (PathResourceResolver) resolvers.get(3);
		Map<Resource, Charset> locationCharsets = pathResolver.getLocationCharsets();
		assertThat(locationCharsets.size()).isEqualTo(1);
		assertThat(locationCharsets.values().iterator().next()).isEqualTo(StandardCharsets.ISO_8859_1);

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers).hasSize(3);
		assertThat(transformers.get(0)).isInstanceOf(CachingResourceTransformer.class);
		assertThat(transformers.get(1)).isInstanceOf(CssLinkResourceTransformer.class);
		assertThat(transformers.get(2)).isInstanceOf(org.springframework.web.servlet.resource.AppCacheManifestTransformer.class);

		CachingResourceTransformer cachingTransformer = (CachingResourceTransformer) transformers.get(0);
		assertThat(cachingTransformer.getCache()).isInstanceOf(ConcurrentMapCache.class);
		assertThat(cachingTransformer.getCache().getName()).isEqualTo("test-resource-cache");
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testResourcesWithResolversTransformersCustom() throws Exception {
		loadBeanDefinitions("mvc-config-resources-chain-no-auto.xml");

		SimpleUrlHandlerMapping mapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertThat(mapping).isNotNull();
		assertThat(mapping.getUrlMap().get("/resources/**")).isNotNull();
		ResourceHttpRequestHandler handler = appContext.getBean((String) mapping.getUrlMap().get("/resources/**"),
				ResourceHttpRequestHandler.class);
		assertThat(handler).isNotNull();

		assertThat(handler.getCacheControl().getHeaderValue())
				.isEqualTo(CacheControl.maxAge(1, TimeUnit.HOURS)
						.sMaxAge(30, TimeUnit.MINUTES).cachePublic().getHeaderValue());

		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		assertThat(resolvers).hasSize(3);
		assertThat(resolvers.get(0)).isInstanceOf(VersionResourceResolver.class);
		assertThat(resolvers.get(1)).isInstanceOf(EncodedResourceResolver.class);
		assertThat(resolvers.get(2)).isInstanceOf(PathResourceResolver.class);

		VersionResourceResolver versionResolver = (VersionResourceResolver) resolvers.get(0);
		assertThat(versionResolver.getStrategyMap().get("/**/*.js"))
				.isInstanceOf(FixedVersionStrategy.class);
		assertThat(versionResolver.getStrategyMap().get("/**"))
				.isInstanceOf(ContentVersionStrategy.class);

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers).hasSize(2);
		assertThat(transformers.get(0)).isInstanceOf(CachingResourceTransformer.class);
		assertThat(transformers.get(1)).isInstanceOf(org.springframework.web.servlet.resource.AppCacheManifestTransformer.class);
	}

	@Test
	public void testDefaultServletHandler() throws Exception {
		loadBeanDefinitions("mvc-config-default-servlet.xml");

		HttpRequestHandlerAdapter adapter = appContext.getBean(HttpRequestHandlerAdapter.class);
		assertThat(adapter).isNotNull();

		DefaultServletHttpRequestHandler handler = appContext.getBean(DefaultServletHttpRequestHandler.class);
		assertThat(handler).isNotNull();

		SimpleUrlHandlerMapping mapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertThat(mapping).isNotNull();
		assertThat(mapping.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/foo.css");
		request.setMethod("GET");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertThat(chain.getHandler() instanceof DefaultServletHttpRequestHandler).isTrue();

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = adapter.handle(request, response, chain.getHandler());
		assertThat((Object) mv).isNull();
	}

	@Test
	public void testDefaultServletHandlerWithOptionalAttributes() throws Exception {
		loadBeanDefinitions("mvc-config-default-servlet-optional-attrs.xml");

		HttpRequestHandlerAdapter adapter = appContext.getBean(HttpRequestHandlerAdapter.class);
		assertThat(adapter).isNotNull();

		DefaultServletHttpRequestHandler handler = appContext.getBean(DefaultServletHttpRequestHandler.class);
		assertThat(handler).isNotNull();

		SimpleUrlHandlerMapping mapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertThat(mapping).isNotNull();
		assertThat(mapping.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/foo.css");
		request.setMethod("GET");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertThat(chain.getHandler() instanceof DefaultServletHttpRequestHandler).isTrue();

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = adapter.handle(request, response, chain.getHandler());
		assertThat((Object) mv).isNull();
	}

	@Test
	public void testBeanDecoration() throws Exception {
		loadBeanDefinitions("mvc-config-bean-decoration.xml");

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertThat(mapping).isNotNull();
		mapping.setDefaultHandler(handlerMethod);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(3);
		assertThat(chain.getInterceptorList().get(0) instanceof ConversionServiceExposingInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(1) instanceof LocaleChangeInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(2) instanceof ThemeChangeInterceptor).isTrue();
		LocaleChangeInterceptor interceptor = (LocaleChangeInterceptor) chain.getInterceptorList().get(1);
		assertThat(interceptor.getParamName()).isEqualTo("lang");
		ThemeChangeInterceptor interceptor2 = (ThemeChangeInterceptor) chain.getInterceptorList().get(2);
		assertThat(interceptor2.getParamName()).isEqualTo("style");
	}

	@Test
	public void testViewControllers() throws Exception {
		loadBeanDefinitions("mvc-config-view-controllers.xml");

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertThat(mapping).isNotNull();
		mapping.setDefaultHandler(handlerMethod);

		BeanNameUrlHandlerMapping beanNameMapping = appContext.getBean(BeanNameUrlHandlerMapping.class);
		assertThat(beanNameMapping).isNotNull();
		assertThat(beanNameMapping.getOrder()).isEqualTo(2);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(3);
		assertThat(chain.getInterceptorList().get(0) instanceof ConversionServiceExposingInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(1) instanceof LocaleChangeInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(2) instanceof ThemeChangeInterceptor).isTrue();

		SimpleUrlHandlerMapping mapping2 = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertThat(mapping2).isNotNull();

		SimpleControllerHandlerAdapter adapter = appContext.getBean(SimpleControllerHandlerAdapter.class);
		assertThat(adapter).isNotNull();

		request = new MockHttpServletRequest("GET", "/foo");
		chain = mapping2.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(4);
		assertThat(chain.getInterceptorList().get(1) instanceof ConversionServiceExposingInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(2) instanceof LocaleChangeInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(3) instanceof ThemeChangeInterceptor).isTrue();
		ModelAndView mv = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertThat((Object) mv.getViewName()).isNull();

		request = new MockHttpServletRequest("GET", "/myapp/app/bar");
		request.setContextPath("/myapp");
		request.setServletPath("/app");
		chain = mapping2.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(4);
		assertThat(chain.getInterceptorList().get(1) instanceof ConversionServiceExposingInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(2) instanceof LocaleChangeInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(3) instanceof ThemeChangeInterceptor).isTrue();
		mv = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertThat(mv.getViewName()).isEqualTo("baz");

		request = new MockHttpServletRequest("GET", "/myapp/app/");
		request.setContextPath("/myapp");
		request.setServletPath("/app");
		chain = mapping2.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(4);
		assertThat(chain.getInterceptorList().get(1) instanceof ConversionServiceExposingInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(2) instanceof LocaleChangeInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(3) instanceof ThemeChangeInterceptor).isTrue();
		mv = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertThat(mv.getViewName()).isEqualTo("root");

		request = new MockHttpServletRequest("GET", "/myapp/app/old");
		request.setContextPath("/myapp");
		request.setServletPath("/app");
		request.setQueryString("a=b");
		chain = mapping2.getHandler(request);
		mv = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertThat(mv.getView()).isNotNull();
		assertThat(mv.getView().getClass()).isEqualTo(RedirectView.class);
		RedirectView redirectView = (RedirectView) mv.getView();
		MockHttpServletResponse response = new MockHttpServletResponse();
		redirectView.render(Collections.emptyMap(), request, response);
		assertThat(response.getRedirectedUrl()).isEqualTo("/new?a=b");
		assertThat(response.getStatus()).isEqualTo(308);

		request = new MockHttpServletRequest("GET", "/bad");
		chain = mapping2.getHandler(request);
		response = new MockHttpServletResponse();
		mv = adapter.handle(request, response, chain.getHandler());
		assertThat(mv).isNull();
		assertThat(response.getStatus()).isEqualTo(404);
	}

	/** WebSphere gives trailing servlet path slashes by default!! */
	@Test
	public void testViewControllersOnWebSphere() throws Exception {
		loadBeanDefinitions("mvc-config-view-controllers.xml");

		SimpleUrlHandlerMapping mapping2 = appContext.getBean(SimpleUrlHandlerMapping.class);
		SimpleControllerHandlerAdapter adapter = appContext.getBean(SimpleControllerHandlerAdapter.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/myapp/app/bar");
		request.setContextPath("/myapp");
		request.setServletPath("/app/");
		request.setAttribute("com.ibm.websphere.servlet.uri_non_decoded", "/myapp/app/bar");
		HandlerExecutionChain chain = mapping2.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(4);
		assertThat(chain.getInterceptorList().get(1) instanceof ConversionServiceExposingInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(2) instanceof LocaleChangeInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(3) instanceof ThemeChangeInterceptor).isTrue();
		ModelAndView mv2 = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertThat(mv2.getViewName()).isEqualTo("baz");

		request.setRequestURI("/myapp/app/");
		request.setContextPath("/myapp");
		request.setServletPath("/app/");
		chain = mapping2.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(4);
		assertThat(chain.getInterceptorList().get(1) instanceof ConversionServiceExposingInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(2) instanceof LocaleChangeInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(3) instanceof ThemeChangeInterceptor).isTrue();
		ModelAndView mv3 = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertThat(mv3.getViewName()).isEqualTo("root");

		request.setRequestURI("/myapp/");
		request.setContextPath("/myapp");
		request.setServletPath("/");
		chain = mapping2.getHandler(request);
		assertThat(chain.getInterceptorList().size()).isEqualTo(4);
		assertThat(chain.getInterceptorList().get(1) instanceof ConversionServiceExposingInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(2) instanceof LocaleChangeInterceptor).isTrue();
		assertThat(chain.getInterceptorList().get(3) instanceof ThemeChangeInterceptor).isTrue();
		mv3 = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertThat(mv3.getViewName()).isEqualTo("root");
	}

	@Test
	public void testViewControllersDefaultConfig() {
		loadBeanDefinitions("mvc-config-view-controllers-minimal.xml");

		SimpleUrlHandlerMapping hm = this.appContext.getBean(SimpleUrlHandlerMapping.class);
		assertThat(hm).isNotNull();
		ParameterizableViewController viewController = (ParameterizableViewController) hm.getUrlMap().get("/path");
		assertThat(viewController).isNotNull();
		assertThat(viewController.getViewName()).isEqualTo("home");

		ParameterizableViewController redirectViewController = (ParameterizableViewController) hm.getUrlMap().get("/old");
		assertThat(redirectViewController).isNotNull();
		assertThat(redirectViewController.getView()).isInstanceOf(RedirectView.class);

		ParameterizableViewController statusViewController = (ParameterizableViewController) hm.getUrlMap().get("/bad");
		assertThat(statusViewController).isNotNull();
		assertThat(statusViewController.getStatusCode().value()).isEqualTo(404);

		BeanNameUrlHandlerMapping beanNameMapping = this.appContext.getBean(BeanNameUrlHandlerMapping.class);
		assertThat(beanNameMapping).isNotNull();
		assertThat(beanNameMapping.getOrder()).isEqualTo(2);
	}

	@Test
	public void testContentNegotiationManager() throws Exception {
		loadBeanDefinitions("mvc-config-content-negotiation-manager.xml");

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		ContentNegotiationManager manager = mapping.getContentNegotiationManager();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo.xml");
		NativeWebRequest webRequest = new ServletWebRequest(request);
		assertThat(manager.resolveMediaTypes(webRequest)).isEqualTo(Collections.singletonList(MediaType.valueOf("application/rss+xml")));

		ViewResolverComposite compositeResolver = this.appContext.getBean(ViewResolverComposite.class);
		assertThat(compositeResolver).isNotNull();
		assertThat(compositeResolver.getViewResolvers().size()).as("Actual: " + compositeResolver.getViewResolvers()).isEqualTo(1);

		ViewResolver resolver = compositeResolver.getViewResolvers().get(0);
		assertThat(resolver.getClass()).isEqualTo(ContentNegotiatingViewResolver.class);
		ContentNegotiatingViewResolver cnvr = (ContentNegotiatingViewResolver) resolver;
		assertThat(cnvr.getContentNegotiationManager()).isSameAs(manager);
	}

	@Test
	public void testAsyncSupportOptions() throws Exception {
		loadBeanDefinitions("mvc-config-async-support.xml");

		RequestMappingHandlerAdapter adapter = appContext.getBean(RequestMappingHandlerAdapter.class);
		assertThat(adapter).isNotNull();

		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(adapter);
		assertThat(fieldAccessor.getPropertyValue("taskExecutor").getClass()).isEqualTo(ConcurrentTaskExecutor.class);
		assertThat(fieldAccessor.getPropertyValue("asyncRequestTimeout")).isEqualTo(2500L);

		CallableProcessingInterceptor[] callableInterceptors =
				(CallableProcessingInterceptor[]) fieldAccessor.getPropertyValue("callableInterceptors");
		assertThat(callableInterceptors.length).isEqualTo(1);

		DeferredResultProcessingInterceptor[] deferredResultInterceptors =
				(DeferredResultProcessingInterceptor[]) fieldAccessor.getPropertyValue("deferredResultInterceptors");
		assertThat(deferredResultInterceptors.length).isEqualTo(1);
	}

	@Test
	public void testViewResolution() throws Exception {
		loadBeanDefinitions("mvc-config-view-resolution.xml");

		ViewResolverComposite compositeResolver = this.appContext.getBean(ViewResolverComposite.class);
		assertThat(compositeResolver).isNotNull();
		assertThat(compositeResolver.getViewResolvers().size()).as("Actual: " + compositeResolver.getViewResolvers()).isEqualTo(8);
		assertThat(compositeResolver.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);

		List<ViewResolver> resolvers = compositeResolver.getViewResolvers();
		assertThat(resolvers.get(0).getClass()).isEqualTo(BeanNameViewResolver.class);

		ViewResolver resolver = resolvers.get(1);
		assertThat(resolver.getClass()).isEqualTo(InternalResourceViewResolver.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(resolver);
		assertThat(accessor.getPropertyValue("viewClass")).isEqualTo(InternalResourceView.class);

		assertThat(resolvers.get(2).getClass()).isEqualTo(TilesViewResolver.class);

		resolver = resolvers.get(3);
		assertThat(resolver).isInstanceOf(FreeMarkerViewResolver.class);
		accessor = new DirectFieldAccessor(resolver);
		assertThat(accessor.getPropertyValue("prefix")).isEqualTo("freemarker-");
		assertThat(accessor.getPropertyValue("suffix")).isEqualTo(".freemarker");
		assertThat((String[]) accessor.getPropertyValue("viewNames")).isEqualTo(new String[] {"my*", "*Report"});
		assertThat(accessor.getPropertyValue("cacheLimit")).isEqualTo(1024);

		resolver = resolvers.get(4);
		assertThat(resolver).isInstanceOf(GroovyMarkupViewResolver.class);
		accessor = new DirectFieldAccessor(resolver);
		assertThat(accessor.getPropertyValue("prefix")).isEqualTo("");
		assertThat(accessor.getPropertyValue("suffix")).isEqualTo(".tpl");
		assertThat(accessor.getPropertyValue("cacheLimit")).isEqualTo(1024);

		resolver = resolvers.get(5);
		assertThat(resolver).isInstanceOf(ScriptTemplateViewResolver.class);
		accessor = new DirectFieldAccessor(resolver);
		assertThat(accessor.getPropertyValue("prefix")).isEqualTo("");
		assertThat(accessor.getPropertyValue("suffix")).isEqualTo("");
		assertThat(accessor.getPropertyValue("cacheLimit")).isEqualTo(1024);

		assertThat(resolvers.get(6).getClass()).isEqualTo(InternalResourceViewResolver.class);
		assertThat(resolvers.get(7).getClass()).isEqualTo(InternalResourceViewResolver.class);

		TilesConfigurer tilesConfigurer = appContext.getBean(TilesConfigurer.class);
		assertThat(tilesConfigurer).isNotNull();
		String[] definitions = {
				"/org/springframework/web/servlet/resource/tiles/tiles1.xml",
				"/org/springframework/web/servlet/resource/tiles/tiles2.xml"
		};
		accessor = new DirectFieldAccessor(tilesConfigurer);
		assertThat((String[]) accessor.getPropertyValue("definitions")).isEqualTo(definitions);
		assertThat((boolean) accessor.getPropertyValue("checkRefresh")).isTrue();
		assertThat(accessor.getPropertyValue("definitionsFactoryClass")).isEqualTo(UnresolvingLocaleDefinitionsFactory.class);
		assertThat(accessor.getPropertyValue("preparerFactoryClass")).isEqualTo(SpringBeanPreparerFactory.class);

		FreeMarkerConfigurer freeMarkerConfigurer = appContext.getBean(FreeMarkerConfigurer.class);
		assertThat(freeMarkerConfigurer).isNotNull();
		accessor = new DirectFieldAccessor(freeMarkerConfigurer);
		assertThat((String[]) accessor.getPropertyValue("templateLoaderPaths")).isEqualTo(new String[] {"/", "/test"});

		GroovyMarkupConfigurer groovyMarkupConfigurer = appContext.getBean(GroovyMarkupConfigurer.class);
		assertThat(groovyMarkupConfigurer).isNotNull();
		assertThat(groovyMarkupConfigurer.getResourceLoaderPath()).isEqualTo("/test");
		assertThat(groovyMarkupConfigurer.isAutoIndent()).isTrue();
		assertThat(groovyMarkupConfigurer.isCacheTemplates()).isFalse();

		ScriptTemplateConfigurer scriptTemplateConfigurer = appContext.getBean(ScriptTemplateConfigurer.class);
		assertThat(scriptTemplateConfigurer).isNotNull();
		assertThat(scriptTemplateConfigurer.getRenderFunction()).isEqualTo("render");
		assertThat(scriptTemplateConfigurer.getContentType()).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(scriptTemplateConfigurer.getCharset()).isEqualTo(StandardCharsets.ISO_8859_1);
		assertThat(scriptTemplateConfigurer.getResourceLoaderPath()).isEqualTo("classpath:");
		assertThat(scriptTemplateConfigurer.isSharedEngine()).isFalse();
		String[] scripts = { "org/springframework/web/servlet/view/script/nashorn/render.js" };
		accessor = new DirectFieldAccessor(scriptTemplateConfigurer);
		assertThat((String[]) accessor.getPropertyValue("scripts")).isEqualTo(scripts);
	}

	@Test
	public void testViewResolutionWithContentNegotiation() throws Exception {
		loadBeanDefinitions("mvc-config-view-resolution-content-negotiation.xml");

		ViewResolverComposite compositeResolver = this.appContext.getBean(ViewResolverComposite.class);
		assertThat(compositeResolver).isNotNull();
		assertThat(compositeResolver.getViewResolvers().size()).isEqualTo(1);
		assertThat(compositeResolver.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);

		List<ViewResolver> resolvers = compositeResolver.getViewResolvers();
		assertThat(resolvers.get(0).getClass()).isEqualTo(ContentNegotiatingViewResolver.class);
		ContentNegotiatingViewResolver cnvr = (ContentNegotiatingViewResolver) resolvers.get(0);
		assertThat(cnvr.getViewResolvers().size()).isEqualTo(6);
		assertThat(cnvr.getDefaultViews().size()).isEqualTo(1);
		assertThat(cnvr.isUseNotAcceptableStatusCode()).isTrue();

		String beanName = "contentNegotiationManager";
		DirectFieldAccessor accessor = new DirectFieldAccessor(cnvr);
		ContentNegotiationManager manager = (ContentNegotiationManager) accessor.getPropertyValue(beanName);
		assertThat(manager).isNotNull();
		assertThat(this.appContext.getBean(ContentNegotiationManager.class)).isSameAs(manager);
		assertThat(this.appContext.getBean("mvcContentNegotiationManager")).isSameAs(manager);
	}

	@Test
	public void testViewResolutionWithOrderSet() throws Exception {
		loadBeanDefinitions("mvc-config-view-resolution-custom-order.xml");

		ViewResolverComposite compositeResolver = this.appContext.getBean(ViewResolverComposite.class);
		assertThat(compositeResolver).isNotNull();
		assertThat(compositeResolver.getViewResolvers().size()).as("Actual: " + compositeResolver.getViewResolvers()).isEqualTo(1);
		assertThat(compositeResolver.getOrder()).isEqualTo(123);
	}

	@Test
	public void testPathMatchingHandlerMappings() throws Exception {
		loadBeanDefinitions("mvc-config-path-matching-mappings.xml");

		RequestMappingHandlerMapping requestMapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertThat(requestMapping).isNotNull();
		assertThat(requestMapping.getUrlPathHelper().getClass()).isEqualTo(TestPathHelper.class);
		assertThat(requestMapping.getPathMatcher().getClass()).isEqualTo(TestPathMatcher.class);

		SimpleUrlHandlerMapping viewController = appContext.getBean(VIEWCONTROLLER_BEAN_NAME, SimpleUrlHandlerMapping.class);
		assertThat(viewController).isNotNull();
		assertThat(viewController.getUrlPathHelper().getClass()).isEqualTo(TestPathHelper.class);
		assertThat(viewController.getPathMatcher().getClass()).isEqualTo(TestPathMatcher.class);

		for (SimpleUrlHandlerMapping handlerMapping : appContext.getBeansOfType(SimpleUrlHandlerMapping.class).values()) {
			assertThat(handlerMapping).isNotNull();
			assertThat(handlerMapping.getUrlPathHelper().getClass()).isEqualTo(TestPathHelper.class);
			assertThat(handlerMapping.getPathMatcher().getClass()).isEqualTo(TestPathMatcher.class);
		}
	}

	@Test
	public void testCorsMinimal() throws Exception {
		loadBeanDefinitions("mvc-config-cors-minimal.xml");

		String[] beanNames = appContext.getBeanNamesForType(AbstractHandlerMapping.class);
		assertThat(beanNames.length).isEqualTo(2);
		for (String beanName : beanNames) {
			AbstractHandlerMapping handlerMapping = (AbstractHandlerMapping)appContext.getBean(beanName);
			assertThat(handlerMapping).isNotNull();
			DirectFieldAccessor accessor = new DirectFieldAccessor(handlerMapping);
			Map<String, CorsConfiguration> configs = ((UrlBasedCorsConfigurationSource) accessor
					.getPropertyValue("corsConfigurationSource")).getCorsConfigurations();
			assertThat(configs).isNotNull();
			assertThat(configs.size()).isEqualTo(1);
			CorsConfiguration config = configs.get("/**");
			assertThat(config).isNotNull();
			assertThat(config.getAllowedOrigins().toArray()).isEqualTo(new String[]{"*"});
			assertThat(config.getAllowedMethods().toArray()).isEqualTo(new String[]{"GET", "HEAD", "POST"});
			assertThat(config.getAllowedHeaders().toArray()).isEqualTo(new String[]{"*"});
			assertThat(config.getExposedHeaders()).isNull();
			assertThat(config.getAllowCredentials()).isNull();
			assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(1800));
		}
	}

	@Test
	public void testCors() {
		loadBeanDefinitions("mvc-config-cors.xml");

		String[] beanNames = appContext.getBeanNamesForType(AbstractHandlerMapping.class);
		assertThat(beanNames.length).isEqualTo(2);
		for (String beanName : beanNames) {
			AbstractHandlerMapping handlerMapping = (AbstractHandlerMapping)appContext.getBean(beanName);
			assertThat(handlerMapping).isNotNull();
			DirectFieldAccessor accessor = new DirectFieldAccessor(handlerMapping);
			Map<String, CorsConfiguration> configs = ((UrlBasedCorsConfigurationSource) accessor
					.getPropertyValue("corsConfigurationSource")).getCorsConfigurations();
			assertThat(configs).isNotNull();
			assertThat(configs.size()).isEqualTo(2);
			CorsConfiguration config = configs.get("/api/**");
			assertThat(config).isNotNull();
			assertThat(config.getAllowedOrigins().toArray()).isEqualTo(new String[]{"https://domain1.com", "https://domain2.com"});
			assertThat(config.getAllowedOriginPatterns().toArray()).isEqualTo(new String[]{"http://*.domain.com"});
			assertThat(config.getAllowedMethods().toArray()).isEqualTo(new String[]{"GET", "PUT"});
			assertThat(config.getAllowedHeaders().toArray()).isEqualTo(new String[]{"header1", "header2", "header3"});
			assertThat(config.getExposedHeaders().toArray()).isEqualTo(new String[]{"header1", "header2"});
			assertThat(config.getAllowCredentials()).isFalse();
			assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(123));
			config = configs.get("/resources/**");
			assertThat(config.getAllowedOrigins().toArray()).isEqualTo(new String[]{"https://domain1.com"});
			assertThat(config.getAllowedMethods().toArray()).isEqualTo(new String[]{"GET", "HEAD", "POST"});
			assertThat(config.getAllowedHeaders().toArray()).isEqualTo(new String[]{"*"});
			assertThat(config.getExposedHeaders()).isNull();
			assertThat(config.getAllowCredentials()).isNull();
			assertThat(config.getMaxAge()).isEqualTo(1800L);
		}
	}


	private void loadBeanDefinitions(String fileName) {
		this.appContext.setConfigLocation("classpath:org/springframework/web/servlet/config/" + fileName);
		this.appContext.refresh();
	}


	@DateTimeFormat(iso = ISO.DATE)
	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface IsoDate {
	}


	@NumberFormat(style = NumberFormat.Style.PERCENT)
	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface PercentNumber {
	}


	@Validated(MyGroup.class)
	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MyValid {
	}


	@Controller
	public static class TestController {

		private Date date;

		private Double percent;

		private boolean recordedValidationError;

		@RequestMapping
		public void testBind(@RequestParam @IsoDate Date date,
				@RequestParam(required = false) @PercentNumber Double percent,
				@MyValid TestBean bean, BindingResult result) {

			this.date = date;
			this.percent = percent;
			this.recordedValidationError = (result.getErrorCount() == 1);
		}
	}


	public static class TestValidator implements Validator {

		boolean validatorInvoked;

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		public void validate(@Nullable Object target, Errors errors) {
			this.validatorInvoked = true;
		}
	}


	@Retention(RetentionPolicy.RUNTIME)
	public @interface MyGroup {
	}


	private static class TestBean {

		@NotNull(groups = MyGroup.class)
		private String field;

		@SuppressWarnings("unused")
		public String getField() {
			return field;
		}

		@SuppressWarnings("unused")
		public void setField(String field) {
			this.field = field;
		}
	}


	private static class TestMockServletContext extends MockServletContext {

		@Override
		public RequestDispatcher getNamedDispatcher(String path) {
			if (path.equals("default") || path.equals("custom")) {
				return new MockRequestDispatcher("/");
			}
			else {
				return null;
			}
		}

		@Override
		public String getVirtualServerName() {
			return null;
		}
	}


	public static class TestCallableProcessingInterceptor implements CallableProcessingInterceptor {
	}


	public static class TestDeferredResultProcessingInterceptor implements DeferredResultProcessingInterceptor {
	}


	public static class TestPathMatcher implements PathMatcher {

		@Override
		public boolean isPattern(String path) {
			return false;
		}

		@Override
		public boolean match(String pattern, String path) {
			return path.matches(pattern);
		}

		@Override
		public boolean matchStart(String pattern, String path) {
			return false;
		}

		@Override
		public String extractPathWithinPattern(String pattern, String path) {
			return null;
		}

		@Override
		public Map<String, String> extractUriTemplateVariables(String pattern, String path) {
			return null;
		}

		@Override
		public Comparator<String> getPatternComparator(String path) {
			return null;
		}

		@Override
		public String combine(String pattern1, String pattern2) {
			return null;
		}
	}


	public static class TestPathHelper extends UrlPathHelper {
	}


	public static class TestCacheManager implements CacheManager {

		@Override
		public Cache getCache(String name) {
			return new ConcurrentMapCache(name);
		}

		@Override
		public Collection<String> getCacheNames() {
			return null;
		}
	}

}
