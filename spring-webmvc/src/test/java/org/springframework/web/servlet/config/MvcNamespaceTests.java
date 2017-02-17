/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
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
import org.hamcrest.Matchers;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockRequestDispatcher;
import org.springframework.mock.web.test.MockServletContext;
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
import org.springframework.web.context.request.async.CallableProcessingInterceptorAdapter;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptorAdapter;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.handler.UserRoleAuthorizationInterceptor;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.AppCacheManifestTransformer;
import org.springframework.web.servlet.resource.CachingResourceResolver;
import org.springframework.web.servlet.resource.CachingResourceTransformer;
import org.springframework.web.servlet.resource.ContentVersionStrategy;
import org.springframework.web.servlet.resource.CssLinkResourceTransformer;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;
import org.springframework.web.servlet.resource.FixedVersionStrategy;
import org.springframework.web.servlet.resource.GzipResourceResolver;
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
import org.springframework.web.servlet.view.velocity.VelocityConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;
import org.springframework.web.util.UrlPathHelper;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

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
 */
public class MvcNamespaceTests {

	public static final String VIEWCONTROLLER_BEAN_NAME =
			"org.springframework.web.servlet.config.viewControllerHandlerMapping";


	private GenericWebApplicationContext appContext;

	private TestController handler;

	private HandlerMethod handlerMethod;


	@Before
	public void setUp() throws Exception {
		TestMockServletContext servletContext = new TestMockServletContext();
		appContext = new GenericWebApplicationContext();
		appContext.setServletContext(servletContext);
		LocaleContextHolder.setLocale(Locale.US);

		String attributeName = WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE;
		appContext.getServletContext().setAttribute(attributeName, appContext);

		handler = new TestController();
		Method method = TestController.class.getMethod("testBind", Date.class, Double.class, TestBean.class, BindingResult.class);
		handlerMethod = new InvocableHandlerMethod(handler, method);
	}


	@Test
	public void testDefaultConfig() throws Exception {
		loadBeanDefinitions("mvc-config.xml", 14);

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(0, mapping.getOrder());
		assertTrue(mapping.getUrlPathHelper().shouldRemoveSemicolonContent());
		mapping.setDefaultHandler(handlerMethod);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo.json");
		NativeWebRequest webRequest = new ServletWebRequest(request);
		ContentNegotiationManager manager = mapping.getContentNegotiationManager();
		assertEquals(Collections.singletonList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(webRequest));

		RequestMappingHandlerAdapter adapter = appContext.getBean(RequestMappingHandlerAdapter.class);
		assertNotNull(adapter);
		assertEquals(false, new DirectFieldAccessor(adapter).getPropertyValue("ignoreDefaultModelOnRedirect"));

		List<HttpMessageConverter<?>> converters = adapter.getMessageConverters();
		assertTrue(converters.size() > 0);
		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof AbstractJackson2HttpMessageConverter) {
				ObjectMapper objectMapper = ((AbstractJackson2HttpMessageConverter) converter).getObjectMapper();
				assertFalse(objectMapper.getDeserializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));
				assertFalse(objectMapper.getSerializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION));
				assertFalse(objectMapper.getDeserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
				if (converter instanceof MappingJackson2XmlHttpMessageConverter) {
					assertEquals(XmlMapper.class, objectMapper.getClass());
				}
			}
		}

		assertNotNull(appContext.getBean(FormattingConversionServiceFactoryBean.class));
		assertNotNull(appContext.getBean(ConversionService.class));
		assertNotNull(appContext.getBean(LocalValidatorFactoryBean.class));
		assertNotNull(appContext.getBean(Validator.class));

		// default web binding initializer behavior test
		request = new MockHttpServletRequest("GET", "/");
		request.addParameter("date", "2009-10-31");
		request.addParameter("percent", "99.99%");
		MockHttpServletResponse response = new MockHttpServletResponse();

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(1, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[0] instanceof ConversionServiceExposingInterceptor);
		ConversionServiceExposingInterceptor interceptor = (ConversionServiceExposingInterceptor) chain.getInterceptors()[0];
		interceptor.preHandle(request, response, handlerMethod);
		assertSame(appContext.getBean(ConversionService.class), request.getAttribute(ConversionService.class.getName()));

		adapter.handle(request, response, handlerMethod);
		assertTrue(handler.recordedValidationError);
		assertEquals(LocalDate.parse("2009-10-31").toDate(), handler.date);
		assertEquals(Double.valueOf(0.9999), handler.percent);

		CompositeUriComponentsContributor uriComponentsContributor = this.appContext.getBean(
				MvcUriComponentsBuilder.MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME,
				CompositeUriComponentsContributor.class);

		assertNotNull(uriComponentsContributor);
	}

	@Test(expected = TypeMismatchException.class)
	public void testCustomConversionService() throws Exception {
		loadBeanDefinitions("mvc-config-custom-conversion-service.xml", 14);

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertNotNull(mapping);
		mapping.setDefaultHandler(handlerMethod);

		// default web binding initializer behavior test
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setRequestURI("/accounts/12345");
		request.addParameter("date", "2009-10-31");
		MockHttpServletResponse response = new MockHttpServletResponse();

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(1, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[0] instanceof ConversionServiceExposingInterceptor);
		ConversionServiceExposingInterceptor interceptor = (ConversionServiceExposingInterceptor) chain.getInterceptors()[0];
		interceptor.preHandle(request, response, handler);
		assertSame(appContext.getBean("conversionService"), request.getAttribute(ConversionService.class.getName()));

		RequestMappingHandlerAdapter adapter = appContext.getBean(RequestMappingHandlerAdapter.class);
		assertNotNull(adapter);
		adapter.handle(request, response, handlerMethod);
	}

	@Test
	public void testCustomValidator() throws Exception {
		doTestCustomValidator("mvc-config-custom-validator.xml");
	}

	@Test
	public void testCustomValidator32() throws Exception {
		doTestCustomValidator("mvc-config-custom-validator-32.xml");
	}

	private void doTestCustomValidator(String xml) throws Exception {
		loadBeanDefinitions(xml, 14);

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertNotNull(mapping);
		assertFalse(mapping.getUrlPathHelper().shouldRemoveSemicolonContent());

		RequestMappingHandlerAdapter adapter = appContext.getBean(RequestMappingHandlerAdapter.class);
		assertNotNull(adapter);
		assertEquals(true, new DirectFieldAccessor(adapter).getPropertyValue("ignoreDefaultModelOnRedirect"));

		// default web binding initializer behavior test
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("date", "2009-10-31");
		MockHttpServletResponse response = new MockHttpServletResponse();
		adapter.handle(request, response, handlerMethod);

		assertTrue(appContext.getBean(TestValidator.class).validatorInvoked);
		assertFalse(handler.recordedValidationError);
	}

	@Test
	public void testInterceptors() throws Exception {
		loadBeanDefinitions("mvc-config-interceptors.xml", 21);

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertNotNull(mapping);
		mapping.setDefaultHandler(handlerMethod);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setRequestURI("/accounts/12345");
		request.addParameter("locale", "en");
		request.addParameter("theme", "green");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(5, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[0] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[1] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof WebRequestHandlerInterceptorAdapter);
		assertTrue(chain.getInterceptors()[3] instanceof ThemeChangeInterceptor);
		assertTrue(chain.getInterceptors()[4] instanceof UserRoleAuthorizationInterceptor);

		request.setRequestURI("/admin/users");
		chain = mapping.getHandler(request);
		assertEquals(3, chain.getInterceptors().length);

		request.setRequestURI("/logged/accounts/12345");
		chain = mapping.getHandler(request);
		assertEquals(5, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[4] instanceof WebRequestHandlerInterceptorAdapter);

		request.setRequestURI("/foo/logged");
		chain = mapping.getHandler(request);
		assertEquals(5, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[4] instanceof WebRequestHandlerInterceptorAdapter);
	}

	@Test
	public void testResources() throws Exception {
		loadBeanDefinitions("mvc-config-resources.xml", 20);

		HttpRequestHandlerAdapter adapter = appContext.getBean(HttpRequestHandlerAdapter.class);
		assertNotNull(adapter);

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		ContentNegotiationManager manager = mapping.getContentNegotiationManager();

		ResourceHttpRequestHandler handler = appContext.getBean(ResourceHttpRequestHandler.class);
		assertNotNull(handler);
		assertSame(manager, handler.getContentNegotiationManager());

		SimpleUrlHandlerMapping resourceMapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(resourceMapping);
		assertEquals(Ordered.LOWEST_PRECEDENCE - 1, resourceMapping.getOrder());

		BeanNameUrlHandlerMapping beanNameMapping = appContext.getBean(BeanNameUrlHandlerMapping.class);
		assertNotNull(beanNameMapping);
		assertEquals(2, beanNameMapping.getOrder());

		ResourceUrlProvider urlProvider = appContext.getBean(ResourceUrlProvider.class);
		assertNotNull(urlProvider);

		Map<String, MappedInterceptor> beans = appContext.getBeansOfType(MappedInterceptor.class);
		List<Class<?>> interceptors = beans.values().stream()
				.map(mappedInterceptor -> mappedInterceptor.getInterceptor().getClass())
				.collect(Collectors.toList());
		assertThat(interceptors, containsInAnyOrder(ConversionServiceExposingInterceptor.class,
				ResourceUrlProviderExposingInterceptor.class));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/resources/foo.css");
		request.setMethod("GET");

		HandlerExecutionChain chain = resourceMapping.getHandler(request);
		assertNotNull(chain);
		assertTrue(chain.getHandler() instanceof ResourceHttpRequestHandler);

		MockHttpServletResponse response = new MockHttpServletResponse();
		for (HandlerInterceptor interceptor : chain.getInterceptors()) {
			interceptor.preHandle(request, response, chain.getHandler());
		}
		ModelAndView mv = adapter.handle(request, response, chain.getHandler());
		assertNull(mv);
	}

	@Test
	public void testResourcesWithOptionalAttributes() throws Exception {
		loadBeanDefinitions("mvc-config-resources-optional-attrs.xml", 10);

		SimpleUrlHandlerMapping mapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(5, mapping.getOrder());
		assertNotNull(mapping.getUrlMap().get("/resources/**"));

		ResourceHttpRequestHandler handler = appContext.getBean((String) mapping.getUrlMap().get("/resources/**"),
				ResourceHttpRequestHandler.class);
		assertNotNull(handler);
		assertEquals(3600, handler.getCacheSeconds());
	}

	@Test
	public void testResourcesWithResolversTransformers() throws Exception {
		loadBeanDefinitions("mvc-config-resources-chain.xml", 11);

		SimpleUrlHandlerMapping mapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(mapping);
		assertNotNull(mapping.getUrlMap().get("/resources/**"));
		ResourceHttpRequestHandler handler = appContext.getBean((String) mapping.getUrlMap().get("/resources/**"),
				ResourceHttpRequestHandler.class);
		assertNotNull(handler);

		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		assertThat(resolvers, Matchers.hasSize(4));
		assertThat(resolvers.get(0), Matchers.instanceOf(CachingResourceResolver.class));
		assertThat(resolvers.get(1), Matchers.instanceOf(VersionResourceResolver.class));
		assertThat(resolvers.get(2), Matchers.instanceOf(WebJarsResourceResolver.class));
		assertThat(resolvers.get(3), Matchers.instanceOf(PathResourceResolver.class));

		CachingResourceResolver cachingResolver = (CachingResourceResolver) resolvers.get(0);
		assertThat(cachingResolver.getCache(), Matchers.instanceOf(ConcurrentMapCache.class));
		assertEquals("test-resource-cache", cachingResolver.getCache().getName());

		VersionResourceResolver versionResolver = (VersionResourceResolver) resolvers.get(1);
		assertThat(versionResolver.getStrategyMap().get("/**/*.js"),
				Matchers.instanceOf(FixedVersionStrategy.class));
		assertThat(versionResolver.getStrategyMap().get("/**"),
				Matchers.instanceOf(ContentVersionStrategy.class));

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers, Matchers.hasSize(3));
		assertThat(transformers.get(0), Matchers.instanceOf(CachingResourceTransformer.class));
		assertThat(transformers.get(1), Matchers.instanceOf(CssLinkResourceTransformer.class));
		assertThat(transformers.get(2), Matchers.instanceOf(AppCacheManifestTransformer.class));

		CachingResourceTransformer cachingTransformer = (CachingResourceTransformer) transformers.get(0);
		assertThat(cachingTransformer.getCache(), Matchers.instanceOf(ConcurrentMapCache.class));
		assertEquals("test-resource-cache", cachingTransformer.getCache().getName());
	}

	@Test
	public void testResourcesWithResolversTransformersCustom() throws Exception {
		loadBeanDefinitions("mvc-config-resources-chain-no-auto.xml", 12);

		SimpleUrlHandlerMapping mapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(mapping);
		assertNotNull(mapping.getUrlMap().get("/resources/**"));
		ResourceHttpRequestHandler handler = appContext.getBean((String) mapping.getUrlMap().get("/resources/**"),
				ResourceHttpRequestHandler.class);
		assertNotNull(handler);

		assertThat(handler.getCacheControl().getHeaderValue(),
				Matchers.equalTo(CacheControl.maxAge(1, TimeUnit.HOURS)
						.sMaxAge(30, TimeUnit.MINUTES).cachePublic().getHeaderValue()));

		List<ResourceResolver> resolvers = handler.getResourceResolvers();
		assertThat(resolvers, Matchers.hasSize(3));
		assertThat(resolvers.get(0), Matchers.instanceOf(VersionResourceResolver.class));
		assertThat(resolvers.get(1), Matchers.instanceOf(GzipResourceResolver.class));
		assertThat(resolvers.get(2), Matchers.instanceOf(PathResourceResolver.class));

		VersionResourceResolver versionResolver = (VersionResourceResolver) resolvers.get(0);
		assertThat(versionResolver.getStrategyMap().get("/**/*.js"),
				Matchers.instanceOf(FixedVersionStrategy.class));
		assertThat(versionResolver.getStrategyMap().get("/**"),
				Matchers.instanceOf(ContentVersionStrategy.class));

		List<ResourceTransformer> transformers = handler.getResourceTransformers();
		assertThat(transformers, Matchers.hasSize(2));
		assertThat(transformers.get(0), Matchers.instanceOf(CachingResourceTransformer.class));
		assertThat(transformers.get(1), Matchers.instanceOf(AppCacheManifestTransformer.class));
	}

	@Test
	public void testDefaultServletHandler() throws Exception {
		loadBeanDefinitions("mvc-config-default-servlet.xml", 6);

		HttpRequestHandlerAdapter adapter = appContext.getBean(HttpRequestHandlerAdapter.class);
		assertNotNull(adapter);

		DefaultServletHttpRequestHandler handler = appContext.getBean(DefaultServletHttpRequestHandler.class);
		assertNotNull(handler);

		SimpleUrlHandlerMapping mapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(Ordered.LOWEST_PRECEDENCE, mapping.getOrder());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/foo.css");
		request.setMethod("GET");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertTrue(chain.getHandler() instanceof DefaultServletHttpRequestHandler);

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = adapter.handle(request, response, chain.getHandler());
		assertNull(mv);
	}

	@Test
	public void testDefaultServletHandlerWithOptionalAttributes() throws Exception {
		loadBeanDefinitions("mvc-config-default-servlet-optional-attrs.xml", 6);

		HttpRequestHandlerAdapter adapter = appContext.getBean(HttpRequestHandlerAdapter.class);
		assertNotNull(adapter);

		DefaultServletHttpRequestHandler handler = appContext.getBean(DefaultServletHttpRequestHandler.class);
		assertNotNull(handler);

		SimpleUrlHandlerMapping mapping = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(mapping);
		assertEquals(Ordered.LOWEST_PRECEDENCE, mapping.getOrder());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/foo.css");
		request.setMethod("GET");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertTrue(chain.getHandler() instanceof DefaultServletHttpRequestHandler);

		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mv = adapter.handle(request, response, chain.getHandler());
		assertNull(mv);
	}

	@Test
	public void testBeanDecoration() throws Exception {
		loadBeanDefinitions("mvc-config-bean-decoration.xml", 16);

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertNotNull(mapping);
		mapping.setDefaultHandler(handlerMethod);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(3, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[0] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[1] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof ThemeChangeInterceptor);
		LocaleChangeInterceptor interceptor = (LocaleChangeInterceptor) chain.getInterceptors()[1];
		assertEquals("lang", interceptor.getParamName());
		ThemeChangeInterceptor interceptor2 = (ThemeChangeInterceptor) chain.getInterceptors()[2];
		assertEquals("style", interceptor2.getParamName());
	}

	@Test
	public void testViewControllers() throws Exception {
		loadBeanDefinitions("mvc-config-view-controllers.xml", 19);

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertNotNull(mapping);
		mapping.setDefaultHandler(handlerMethod);

		BeanNameUrlHandlerMapping beanNameMapping = appContext.getBean(BeanNameUrlHandlerMapping.class);
		assertNotNull(beanNameMapping);
		assertEquals(2, beanNameMapping.getOrder());

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertEquals(3, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[0] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[1] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof ThemeChangeInterceptor);

		SimpleUrlHandlerMapping mapping2 = appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(mapping2);

		SimpleControllerHandlerAdapter adapter = appContext.getBean(SimpleControllerHandlerAdapter.class);
		assertNotNull(adapter);

		request = new MockHttpServletRequest("GET", "/foo");
		chain = mapping2.getHandler(request);
		assertEquals(4, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[3] instanceof ThemeChangeInterceptor);
		ModelAndView mv = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertNull(mv.getViewName());

		request = new MockHttpServletRequest("GET", "/myapp/app/bar");
		request.setContextPath("/myapp");
		request.setServletPath("/app");
		chain = mapping2.getHandler(request);
		assertEquals(4, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[3] instanceof ThemeChangeInterceptor);
		mv = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertEquals("baz", mv.getViewName());

		request = new MockHttpServletRequest("GET", "/myapp/app/");
		request.setContextPath("/myapp");
		request.setServletPath("/app");
		chain = mapping2.getHandler(request);
		assertEquals(4, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[3] instanceof ThemeChangeInterceptor);
		mv = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertEquals("root", mv.getViewName());

		request = new MockHttpServletRequest("GET", "/myapp/app/old");
		request.setContextPath("/myapp");
		request.setServletPath("/app");
		request.setQueryString("a=b");
		chain = mapping2.getHandler(request);
		mv = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertNotNull(mv.getView());
		assertEquals(RedirectView.class, mv.getView().getClass());
		RedirectView redirectView = (RedirectView) mv.getView();
		MockHttpServletResponse response = new MockHttpServletResponse();
		redirectView.render(Collections.emptyMap(), request, response);
		assertEquals("/new?a=b", response.getRedirectedUrl());
		assertEquals(308, response.getStatus());

		request = new MockHttpServletRequest("GET", "/bad");
		chain = mapping2.getHandler(request);
		response = new MockHttpServletResponse();
		mv = adapter.handle(request, response, chain.getHandler());
		assertNull(mv);
		assertEquals(404, response.getStatus());
	}

	/** WebSphere gives trailing servlet path slashes by default!! */
	@Test
	public void testViewControllersOnWebSphere() throws Exception {
		loadBeanDefinitions("mvc-config-view-controllers.xml", 19);

		SimpleUrlHandlerMapping mapping2 = appContext.getBean(SimpleUrlHandlerMapping.class);
		SimpleControllerHandlerAdapter adapter = appContext.getBean(SimpleControllerHandlerAdapter.class);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/myapp/app/bar");
		request.setContextPath("/myapp");
		request.setServletPath("/app/");
		request.setAttribute("com.ibm.websphere.servlet.uri_non_decoded", "/myapp/app/bar");
		HandlerExecutionChain chain = mapping2.getHandler(request);
		assertEquals(4, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[3] instanceof ThemeChangeInterceptor);
		ModelAndView mv2 = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertEquals("baz", mv2.getViewName());

		request.setRequestURI("/myapp/app/");
		request.setContextPath("/myapp");
		request.setServletPath("/app/");
		chain = mapping2.getHandler(request);
		assertEquals(4, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[3] instanceof ThemeChangeInterceptor);
		ModelAndView mv3 = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertEquals("root", mv3.getViewName());

		request.setRequestURI("/myapp/");
		request.setContextPath("/myapp");
		request.setServletPath("/");
		chain = mapping2.getHandler(request);
		assertEquals(4, chain.getInterceptors().length);
		assertTrue(chain.getInterceptors()[1] instanceof ConversionServiceExposingInterceptor);
		assertTrue(chain.getInterceptors()[2] instanceof LocaleChangeInterceptor);
		assertTrue(chain.getInterceptors()[3] instanceof ThemeChangeInterceptor);
		mv3 = adapter.handle(request, new MockHttpServletResponse(), chain.getHandler());
		assertEquals("root", mv3.getViewName());
	}

	@Test
	public void testViewControllersDefaultConfig() {
		loadBeanDefinitions("mvc-config-view-controllers-minimal.xml", 7);

		SimpleUrlHandlerMapping hm = this.appContext.getBean(SimpleUrlHandlerMapping.class);
		assertNotNull(hm);
		ParameterizableViewController viewController = (ParameterizableViewController) hm.getUrlMap().get("/path");
		assertNotNull(viewController);
		assertEquals("home", viewController.getViewName());

		ParameterizableViewController redirectViewController = (ParameterizableViewController) hm.getUrlMap().get("/old");
		assertNotNull(redirectViewController);
		assertThat(redirectViewController.getView(), Matchers.instanceOf(RedirectView.class));

		ParameterizableViewController statusViewController = (ParameterizableViewController) hm.getUrlMap().get("/bad");
		assertNotNull(statusViewController);
		assertEquals(404, statusViewController.getStatusCode().value());

		BeanNameUrlHandlerMapping beanNameMapping = this.appContext.getBean(BeanNameUrlHandlerMapping.class);
		assertNotNull(beanNameMapping);
		assertEquals(2, beanNameMapping.getOrder());
	}

	@Test
	public void testContentNegotiationManager() throws Exception {
		loadBeanDefinitions("mvc-config-content-negotiation-manager.xml", 15);

		RequestMappingHandlerMapping mapping = appContext.getBean(RequestMappingHandlerMapping.class);
		ContentNegotiationManager manager = mapping.getContentNegotiationManager();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo.xml");
		NativeWebRequest webRequest = new ServletWebRequest(request);
		assertEquals(Collections.singletonList(MediaType.valueOf("application/rss+xml")),
				manager.resolveMediaTypes(webRequest));

		ViewResolverComposite compositeResolver = this.appContext.getBean(ViewResolverComposite.class);
		assertNotNull(compositeResolver);
		assertEquals("Actual: " + compositeResolver.getViewResolvers(), 1, compositeResolver.getViewResolvers().size());

		ViewResolver resolver = compositeResolver.getViewResolvers().get(0);
		assertEquals(ContentNegotiatingViewResolver.class, resolver.getClass());
		ContentNegotiatingViewResolver cnvr = (ContentNegotiatingViewResolver) resolver;
		assertSame(manager, cnvr.getContentNegotiationManager());
	}

	@Test
	public void testAsyncSupportOptions() throws Exception {
		loadBeanDefinitions("mvc-config-async-support.xml", 15);

		RequestMappingHandlerAdapter adapter = appContext.getBean(RequestMappingHandlerAdapter.class);
		assertNotNull(adapter);

		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(adapter);
		assertEquals(ConcurrentTaskExecutor.class, fieldAccessor.getPropertyValue("taskExecutor").getClass());
		assertEquals(2500L, fieldAccessor.getPropertyValue("asyncRequestTimeout"));

		CallableProcessingInterceptor[] callableInterceptors =
				(CallableProcessingInterceptor[]) fieldAccessor.getPropertyValue("callableInterceptors");
		assertEquals(1, callableInterceptors.length);

		DeferredResultProcessingInterceptor[] deferredResultInterceptors =
				(DeferredResultProcessingInterceptor[]) fieldAccessor.getPropertyValue("deferredResultInterceptors");
		assertEquals(1, deferredResultInterceptors.length);
	}

	@Test
	public void testViewResolution() throws Exception {
		loadBeanDefinitions("mvc-config-view-resolution.xml", 7);

		ViewResolverComposite compositeResolver = this.appContext.getBean(ViewResolverComposite.class);
		assertNotNull(compositeResolver);
		assertEquals("Actual: " + compositeResolver.getViewResolvers(), 9, compositeResolver.getViewResolvers().size());
		assertEquals(Ordered.LOWEST_PRECEDENCE, compositeResolver.getOrder());

		List<ViewResolver> resolvers = compositeResolver.getViewResolvers();
		assertEquals(BeanNameViewResolver.class, resolvers.get(0).getClass());

		ViewResolver resolver = resolvers.get(1);
		assertEquals(InternalResourceViewResolver.class, resolver.getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(resolver);
		assertEquals(InternalResourceView.class, accessor.getPropertyValue("viewClass"));

		assertEquals(TilesViewResolver.class, resolvers.get(2).getClass());

		resolver = resolvers.get(3);
		assertThat(resolver, instanceOf(FreeMarkerViewResolver.class));
		accessor = new DirectFieldAccessor(resolver);
		assertEquals("freemarker-", accessor.getPropertyValue("prefix"));
		assertEquals(".freemarker", accessor.getPropertyValue("suffix"));
		assertArrayEquals(new String[] {"my*", "*Report"}, (String[]) accessor.getPropertyValue("viewNames"));
		assertEquals(1024, accessor.getPropertyValue("cacheLimit"));

		resolver = resolvers.get(4);
		assertThat(resolver, instanceOf(VelocityViewResolver.class));
		accessor = new DirectFieldAccessor(resolver);
		assertEquals("", accessor.getPropertyValue("prefix"));
		assertEquals(".vm", accessor.getPropertyValue("suffix"));
		assertEquals(0, accessor.getPropertyValue("cacheLimit"));

		resolver = resolvers.get(5);
		assertThat(resolver, instanceOf(GroovyMarkupViewResolver.class));
		accessor = new DirectFieldAccessor(resolver);
		assertEquals("", accessor.getPropertyValue("prefix"));
		assertEquals(".tpl", accessor.getPropertyValue("suffix"));
		assertEquals(1024, accessor.getPropertyValue("cacheLimit"));

		resolver = resolvers.get(6);
		assertThat(resolver, instanceOf(ScriptTemplateViewResolver.class));
		accessor = new DirectFieldAccessor(resolver);
		assertEquals("", accessor.getPropertyValue("prefix"));
		assertEquals("", accessor.getPropertyValue("suffix"));
		assertEquals(1024, accessor.getPropertyValue("cacheLimit"));

		assertEquals(InternalResourceViewResolver.class, resolvers.get(7).getClass());
		assertEquals(InternalResourceViewResolver.class, resolvers.get(8).getClass());

		TilesConfigurer tilesConfigurer = appContext.getBean(TilesConfigurer.class);
		assertNotNull(tilesConfigurer);
		String[] definitions = {
				"/org/springframework/web/servlet/resource/tiles/tiles1.xml",
				"/org/springframework/web/servlet/resource/tiles/tiles2.xml"
		};
		accessor = new DirectFieldAccessor(tilesConfigurer);
		assertArrayEquals(definitions, (String[]) accessor.getPropertyValue("definitions"));
		assertTrue((boolean) accessor.getPropertyValue("checkRefresh"));
		assertEquals(UnresolvingLocaleDefinitionsFactory.class, accessor.getPropertyValue("definitionsFactoryClass"));
		assertEquals(SpringBeanPreparerFactory.class, accessor.getPropertyValue("preparerFactoryClass"));

		FreeMarkerConfigurer freeMarkerConfigurer = appContext.getBean(FreeMarkerConfigurer.class);
		assertNotNull(freeMarkerConfigurer);
		accessor = new DirectFieldAccessor(freeMarkerConfigurer);
		assertArrayEquals(new String[] {"/", "/test"}, (String[]) accessor.getPropertyValue("templateLoaderPaths"));

		VelocityConfigurer velocityConfigurer = appContext.getBean(VelocityConfigurer.class);
		assertNotNull(velocityConfigurer);
		accessor = new DirectFieldAccessor(velocityConfigurer);
		assertEquals("/test", accessor.getPropertyValue("resourceLoaderPath"));

		GroovyMarkupConfigurer groovyMarkupConfigurer = appContext.getBean(GroovyMarkupConfigurer.class);
		assertNotNull(groovyMarkupConfigurer);
		assertEquals("/test", groovyMarkupConfigurer.getResourceLoaderPath());
		assertTrue(groovyMarkupConfigurer.isAutoIndent());
		assertFalse(groovyMarkupConfigurer.isCacheTemplates());

		ScriptTemplateConfigurer scriptTemplateConfigurer = appContext.getBean(ScriptTemplateConfigurer.class);
		assertNotNull(scriptTemplateConfigurer);
		assertEquals("render", scriptTemplateConfigurer.getRenderFunction());
		assertEquals(MediaType.TEXT_PLAIN_VALUE, scriptTemplateConfigurer.getContentType());
		assertEquals("ISO-8859-1", scriptTemplateConfigurer.getCharset().name());
		assertEquals("classpath:", scriptTemplateConfigurer.getResourceLoaderPath());
		assertFalse(scriptTemplateConfigurer.isSharedEngine());
		String[] scripts = { "org/springframework/web/servlet/view/script/nashorn/render.js" };
		accessor = new DirectFieldAccessor(scriptTemplateConfigurer);
		assertArrayEquals(scripts, (String[]) accessor.getPropertyValue("scripts"));
	}

	@Test
	public void testViewResolutionWithContentNegotiation() throws Exception {
		loadBeanDefinitions("mvc-config-view-resolution-content-negotiation.xml", 7);

		ViewResolverComposite compositeResolver = this.appContext.getBean(ViewResolverComposite.class);
		assertNotNull(compositeResolver);
		assertEquals(1, compositeResolver.getViewResolvers().size());
		assertEquals(Ordered.HIGHEST_PRECEDENCE, compositeResolver.getOrder());

		List<ViewResolver> resolvers = compositeResolver.getViewResolvers();
		assertEquals(ContentNegotiatingViewResolver.class, resolvers.get(0).getClass());
		ContentNegotiatingViewResolver cnvr = (ContentNegotiatingViewResolver) resolvers.get(0);
		assertEquals(7, cnvr.getViewResolvers().size());
		assertEquals(1, cnvr.getDefaultViews().size());
		assertTrue(cnvr.isUseNotAcceptableStatusCode());

		String beanName = "contentNegotiationManager";
		DirectFieldAccessor accessor = new DirectFieldAccessor(cnvr);
		ContentNegotiationManager manager = (ContentNegotiationManager) accessor.getPropertyValue(beanName);
		assertNotNull(manager);
		assertSame(manager, this.appContext.getBean(ContentNegotiationManager.class));
		assertSame(manager, this.appContext.getBean("mvcContentNegotiationManager"));
	}

	@Test
	public void testViewResolutionWithOrderSet() throws Exception {
		loadBeanDefinitions("mvc-config-view-resolution-custom-order.xml", 1);

		ViewResolverComposite compositeResolver = this.appContext.getBean(ViewResolverComposite.class);
		assertNotNull(compositeResolver);
		assertEquals("Actual: " + compositeResolver.getViewResolvers(), 1, compositeResolver.getViewResolvers().size());
		assertEquals(123, compositeResolver.getOrder());
	}

	@Test
	public void testPathMatchingHandlerMappings() throws Exception {
		loadBeanDefinitions("mvc-config-path-matching-mappings.xml", 23);

		RequestMappingHandlerMapping requestMapping = appContext.getBean(RequestMappingHandlerMapping.class);
		assertNotNull(requestMapping);
		assertEquals(TestPathHelper.class, requestMapping.getUrlPathHelper().getClass());
		assertEquals(TestPathMatcher.class, requestMapping.getPathMatcher().getClass());

		SimpleUrlHandlerMapping viewController = appContext.getBean(VIEWCONTROLLER_BEAN_NAME, SimpleUrlHandlerMapping.class);
		assertNotNull(viewController);
		assertEquals(TestPathHelper.class, viewController.getUrlPathHelper().getClass());
		assertEquals(TestPathMatcher.class, viewController.getPathMatcher().getClass());

		for (SimpleUrlHandlerMapping handlerMapping : appContext.getBeansOfType(SimpleUrlHandlerMapping.class).values()) {
			assertNotNull(handlerMapping);
			assertEquals(TestPathHelper.class, handlerMapping.getUrlPathHelper().getClass());
			assertEquals(TestPathMatcher.class, handlerMapping.getPathMatcher().getClass());
		}
	}

	@Test
	public void testCorsMinimal() throws Exception {
		loadBeanDefinitions("mvc-config-cors-minimal.xml", 14);

		String[] beanNames = appContext.getBeanNamesForType(AbstractHandlerMapping.class);
		assertEquals(2, beanNames.length);
		for (String beanName : beanNames) {
			AbstractHandlerMapping handlerMapping = (AbstractHandlerMapping)appContext.getBean(beanName);
			assertNotNull(handlerMapping);
			Map<String, CorsConfiguration> configs = handlerMapping.getCorsConfigurations();
			assertNotNull(configs);
			assertEquals(1, configs.size());
			CorsConfiguration config = configs.get("/**");
			assertNotNull(config);
			assertArrayEquals(new String[]{"*"}, config.getAllowedOrigins().toArray());
			assertArrayEquals(new String[]{"GET", "HEAD", "POST"}, config.getAllowedMethods().toArray());
			assertArrayEquals(new String[]{"*"}, config.getAllowedHeaders().toArray());
			assertNull(config.getExposedHeaders());
			assertTrue(config.getAllowCredentials());
			assertEquals(new Long(1800), config.getMaxAge());
		}
	}

	@Test
	public void testCors() throws Exception {
		loadBeanDefinitions("mvc-config-cors.xml", 14);

		String[] beanNames = appContext.getBeanNamesForType(AbstractHandlerMapping.class);
		assertEquals(2, beanNames.length);
		for (String beanName : beanNames) {
			AbstractHandlerMapping handlerMapping = (AbstractHandlerMapping)appContext.getBean(beanName);
			assertNotNull(handlerMapping);
			Map<String, CorsConfiguration> configs = handlerMapping.getCorsConfigurations();
			assertNotNull(configs);
			assertEquals(2, configs.size());
			CorsConfiguration config = configs.get("/api/**");
			assertNotNull(config);
			assertArrayEquals(new String[]{"http://domain1.com", "http://domain2.com"}, config.getAllowedOrigins().toArray());
			assertArrayEquals(new String[]{"GET", "PUT"}, config.getAllowedMethods().toArray());
			assertArrayEquals(new String[]{"header1", "header2", "header3"}, config.getAllowedHeaders().toArray());
			assertArrayEquals(new String[]{"header1", "header2"}, config.getExposedHeaders().toArray());
			assertFalse(config.getAllowCredentials());
			assertEquals(Long.valueOf(123), config.getMaxAge());
			config = configs.get("/resources/**");
			assertArrayEquals(new String[]{"http://domain1.com"}, config.getAllowedOrigins().toArray());
			assertArrayEquals(new String[]{"GET", "HEAD", "POST"}, config.getAllowedMethods().toArray());
			assertArrayEquals(new String[]{"*"}, config.getAllowedHeaders().toArray());
			assertNull(config.getExposedHeaders());
			assertTrue(config.getAllowCredentials());
			assertEquals(Long.valueOf(1800), config.getMaxAge());
		}
	}


	private void loadBeanDefinitions(String fileName, int expectedBeanCount) {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
		ClassPathResource resource = new ClassPathResource(fileName, AnnotationDrivenBeanDefinitionParserTests.class);
		reader.loadBeanDefinitions(resource);
		String names = Arrays.toString(this.appContext.getBeanDefinitionNames());
		assertEquals("Bean names: " + names, expectedBeanCount, appContext.getBeanDefinitionCount());
		appContext.refresh();
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
		public void validate(Object target, Errors errors) {
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
	}


	public static class TestCallableProcessingInterceptor extends CallableProcessingInterceptorAdapter {
	}


	public static class TestDeferredResultProcessingInterceptor extends DeferredResultProcessingInterceptorAdapter {
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
