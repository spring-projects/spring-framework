/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.Ordered;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.util.AntPathMatcher;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.ViewResolverComposite;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;
import org.springframework.web.util.UrlPathHelper;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.DEFAULT_VIEW_INCLUSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_ATOM_XML;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;

/**
 * A test fixture with a sub-class of {@link WebMvcConfigurationSupport} that also
 * implements the various {@link WebMvcConfigurer} extension points.
 *
 * The former doesn't implement the latter but the two must have compatible
 * callback method signatures to support moving from simple to advanced
 * configuration -- i.e. dropping @EnableWebMvc + WebMvcConfigurer and extending
 * directly from WebMvcConfigurationSupport.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class WebMvcConfigurationSupportExtensionTests {

	private TestWebMvcConfigurationSupport config;

	private StaticWebApplicationContext context;


	@Before
	public void setUp() {
		this.context = new StaticWebApplicationContext();
		this.context.setServletContext(new MockServletContext(new FileSystemResourceLoader()));
		this.context.registerSingleton("controller", TestController.class);
		this.context.registerSingleton("userController", UserController.class);

		this.config = new TestWebMvcConfigurationSupport();
		this.config.setApplicationContext(this.context);
		this.config.setServletContext(this.context.getServletContext());
	}

	@Test
	public void handlerMappings() throws Exception {
		RequestMappingHandlerMapping rmHandlerMapping = this.config.requestMappingHandlerMapping();
		rmHandlerMapping.setApplicationContext(this.context);
		rmHandlerMapping.afterPropertiesSet();
		assertEquals(TestPathHelper.class, rmHandlerMapping.getUrlPathHelper().getClass());
		assertEquals(TestPathMatcher.class, rmHandlerMapping.getPathMatcher().getClass());
		HandlerExecutionChain chain = rmHandlerMapping.getHandler(new MockHttpServletRequest("GET", "/"));
		assertNotNull(chain);
		assertNotNull(chain.getInterceptors());
		assertEquals(3, chain.getInterceptors().length);
		assertEquals(LocaleChangeInterceptor.class, chain.getInterceptors()[0].getClass());
		assertEquals(ConversionServiceExposingInterceptor.class, chain.getInterceptors()[1].getClass());
		assertEquals(ResourceUrlProviderExposingInterceptor.class, chain.getInterceptors()[2].getClass());

		Map<RequestMappingInfo, HandlerMethod> map = rmHandlerMapping.getHandlerMethods();
		assertEquals(2, map.size());
		RequestMappingInfo info = map.entrySet().stream()
				.filter(entry -> entry.getValue().getBeanType().equals(UserController.class))
				.findFirst()
				.orElseThrow(() -> new AssertionError("UserController bean not found"))
				.getKey();
		assertEquals(Collections.singleton("/api/user/{id}"), info.getPatternsCondition().getPatterns());

		AbstractHandlerMapping handlerMapping = (AbstractHandlerMapping) this.config.viewControllerHandlerMapping();
		handlerMapping.setApplicationContext(this.context);
		assertNotNull(handlerMapping);
		assertEquals(1, handlerMapping.getOrder());
		assertEquals(TestPathHelper.class, handlerMapping.getUrlPathHelper().getClass());
		assertEquals(TestPathMatcher.class, handlerMapping.getPathMatcher().getClass());
		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/path"));
		assertNotNull(chain);
		assertNotNull(chain.getHandler());
		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/bad"));
		assertNotNull(chain);
		assertNotNull(chain.getHandler());
		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/old"));
		assertNotNull(chain);
		assertNotNull(chain.getHandler());

		handlerMapping = (AbstractHandlerMapping) this.config.resourceHandlerMapping();
		handlerMapping.setApplicationContext(this.context);
		assertNotNull(handlerMapping);
		assertEquals(Integer.MAX_VALUE - 1, handlerMapping.getOrder());
		assertEquals(TestPathHelper.class, handlerMapping.getUrlPathHelper().getClass());
		assertEquals(TestPathMatcher.class, handlerMapping.getPathMatcher().getClass());
		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/resources/foo.gif"));
		assertNotNull(chain);
		assertNotNull(chain.getHandler());
		assertEquals(Arrays.toString(chain.getInterceptors()), 4, chain.getInterceptors().length);
		// PathExposingHandlerInterceptor at chain.getInterceptors()[0]
		assertEquals(LocaleChangeInterceptor.class, chain.getInterceptors()[1].getClass());
		assertEquals(ConversionServiceExposingInterceptor.class, chain.getInterceptors()[2].getClass());
		assertEquals(ResourceUrlProviderExposingInterceptor.class, chain.getInterceptors()[3].getClass());

		handlerMapping = (AbstractHandlerMapping) this.config.defaultServletHandlerMapping();
		handlerMapping.setApplicationContext(this.context);
		assertNotNull(handlerMapping);
		assertEquals(Integer.MAX_VALUE, handlerMapping.getOrder());
		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/anyPath"));
		assertNotNull(chain);
		assertNotNull(chain.getHandler());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void requestMappingHandlerAdapter() throws Exception {
		RequestMappingHandlerAdapter adapter = this.config.requestMappingHandlerAdapter();

		// ConversionService
		String actual = this.config.mvcConversionService().convert(new TestBean(), String.class);
		assertEquals("converted", actual);

		// Message converters
		List<HttpMessageConverter<?>> converters = adapter.getMessageConverters();
		assertEquals(2, converters.size());
		assertEquals(StringHttpMessageConverter.class, converters.get(0).getClass());
		assertEquals(MappingJackson2HttpMessageConverter.class, converters.get(1).getClass());
		ObjectMapper objectMapper = ((MappingJackson2HttpMessageConverter) converters.get(1)).getObjectMapper();
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(DEFAULT_VIEW_INCLUSION));
		assertFalse(objectMapper.getSerializationConfig().isEnabled(DEFAULT_VIEW_INCLUSION));
		assertFalse(objectMapper.getDeserializationConfig().isEnabled(FAIL_ON_UNKNOWN_PROPERTIES));

		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(adapter);

		// Custom argument resolvers and return value handlers
		List<HandlerMethodArgumentResolver> argResolvers =
			(List<HandlerMethodArgumentResolver>) fieldAccessor.getPropertyValue("customArgumentResolvers");
		assertEquals(1, argResolvers.size());

		List<HandlerMethodReturnValueHandler> handlers =
			(List<HandlerMethodReturnValueHandler>) fieldAccessor.getPropertyValue("customReturnValueHandlers");
		assertEquals(1, handlers.size());

		// Async support options
		assertEquals(ConcurrentTaskExecutor.class, fieldAccessor.getPropertyValue("taskExecutor").getClass());
		assertEquals(2500L, fieldAccessor.getPropertyValue("asyncRequestTimeout"));

		CallableProcessingInterceptor[] callableInterceptors =
				(CallableProcessingInterceptor[]) fieldAccessor.getPropertyValue("callableInterceptors");
		assertEquals(1, callableInterceptors.length);

		DeferredResultProcessingInterceptor[] deferredResultInterceptors =
				(DeferredResultProcessingInterceptor[]) fieldAccessor.getPropertyValue("deferredResultInterceptors");
		assertEquals(1, deferredResultInterceptors.length);

		assertEquals(false, fieldAccessor.getPropertyValue("ignoreDefaultModelOnRedirect"));
	}

	@Test
	public void webBindingInitializer() throws Exception {
		RequestMappingHandlerAdapter adapter = this.config.requestMappingHandlerAdapter();

		ConfigurableWebBindingInitializer initializer =
				(ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();

		assertNotNull(initializer);

		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(null, "");
		initializer.getValidator().validate(null, bindingResult);
		assertEquals("invalid", bindingResult.getAllErrors().get(0).getCode());

		String[] codes = initializer.getMessageCodesResolver().resolveMessageCodes("invalid", null);
		assertEquals("custom.invalid", codes[0]);
	}

	@Test
	public void contentNegotiation() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo.json");
		NativeWebRequest webRequest = new ServletWebRequest(request);

		RequestMappingHandlerMapping mapping = this.config.requestMappingHandlerMapping();
		ContentNegotiationManager manager = mapping.getContentNegotiationManager();
		assertEquals(Collections.singletonList(APPLICATION_JSON), manager.resolveMediaTypes(webRequest));

		request.setRequestURI("/foo.xml");
		assertEquals(Collections.singletonList(APPLICATION_XML), manager.resolveMediaTypes(webRequest));

		request.setRequestURI("/foo.rss");
		assertEquals(Collections.singletonList(MediaType.valueOf("application/rss+xml")),
				manager.resolveMediaTypes(webRequest));

		request.setRequestURI("/foo.atom");
		assertEquals(Collections.singletonList(APPLICATION_ATOM_XML), manager.resolveMediaTypes(webRequest));

		request.setRequestURI("/foo");
		request.setParameter("f", "json");
		assertEquals(Collections.singletonList(APPLICATION_JSON), manager.resolveMediaTypes(webRequest));

		request.setRequestURI("/resources/foo.gif");
		SimpleUrlHandlerMapping handlerMapping = (SimpleUrlHandlerMapping) this.config.resourceHandlerMapping();
		handlerMapping.setApplicationContext(this.context);
		HandlerExecutionChain chain = handlerMapping.getHandler(request);
		assertNotNull(chain);
		ResourceHttpRequestHandler handler = (ResourceHttpRequestHandler) chain.getHandler();
		assertNotNull(handler);
		assertSame(manager, handler.getContentNegotiationManager());
	}

	@Test
	public void exceptionResolvers() throws Exception {
		List<HandlerExceptionResolver> resolvers = ((HandlerExceptionResolverComposite)
				this.config.handlerExceptionResolver()).getExceptionResolvers();

		assertEquals(2, resolvers.size());
		assertEquals(ResponseStatusExceptionResolver.class, resolvers.get(0).getClass());
		assertEquals(SimpleMappingExceptionResolver.class, resolvers.get(1).getClass());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void viewResolvers() throws Exception {
		ViewResolverComposite viewResolver = (ViewResolverComposite) this.config.mvcViewResolver();
		assertEquals(Ordered.HIGHEST_PRECEDENCE, viewResolver.getOrder());
		List<ViewResolver> viewResolvers = viewResolver.getViewResolvers();

		DirectFieldAccessor accessor = new DirectFieldAccessor(viewResolvers.get(0));
		assertEquals(1, viewResolvers.size());
		assertEquals(ContentNegotiatingViewResolver.class, viewResolvers.get(0).getClass());
		assertFalse((Boolean) accessor.getPropertyValue("useNotAcceptableStatusCode"));
		assertNotNull(accessor.getPropertyValue("contentNegotiationManager"));

		List<View> defaultViews = (List<View>)accessor.getPropertyValue("defaultViews");
		assertNotNull(defaultViews);
		assertEquals(1, defaultViews.size());
		assertEquals(MappingJackson2JsonView.class, defaultViews.get(0).getClass());

		viewResolvers = (List<ViewResolver>)accessor.getPropertyValue("viewResolvers");
		assertNotNull(viewResolvers);
		assertEquals(1, viewResolvers.size());
		assertEquals(InternalResourceViewResolver.class, viewResolvers.get(0).getClass());
		accessor = new DirectFieldAccessor(viewResolvers.get(0));
		assertEquals("/", accessor.getPropertyValue("prefix"));
		assertEquals(".jsp", accessor.getPropertyValue("suffix"));
	}

	@Test
	public void crossOrigin() {
		Map<String, CorsConfiguration> configs = this.config.getCorsConfigurations();
		assertEquals(1, configs.size());
		assertEquals("*", configs.get("/resources/**").getAllowedOrigins().get(0));
	}


	@Controller
	private static class TestController {

		@RequestMapping("/")
		public void handle() {
		}
	}

	/**
	 * Since WebMvcConfigurationSupport does not implement WebMvcConfigurer, the purpose
	 * of this test class is also to ensure the two are in sync with each other. Effectively
	 * that ensures that application config classes that use the combo {@code @EnableWebMvc}
	 * plus WebMvcConfigurer can switch to extending WebMvcConfigurationSupport directly for
	 * more advanced configuration needs.
	 */
	private class TestWebMvcConfigurationSupport extends WebMvcConfigurationSupport implements WebMvcConfigurer {

		@Override
		public void addFormatters(FormatterRegistry registry) {
			registry.addConverter(new Converter<TestBean, String>() {
				@Override
				public String convert(TestBean source) {
					return "converted";
				}
			});
		}

		@Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			converters.add(new MappingJackson2HttpMessageConverter());
		}

		@Override
		public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
			converters.add(0, new StringHttpMessageConverter());
		}

		@Override
		public Validator getValidator() {
			return new Validator() {
				@Override
				public void validate(@Nullable Object target, Errors errors) {
					errors.reject("invalid");
				}
				@Override
				public boolean supports(Class<?> clazz) {
					return true;
				}
			};
		}

		@Override
		public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
			configurer.favorParameter(true).parameterName("f");
		}

		@Override
		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
			configurer.setDefaultTimeout(2500).setTaskExecutor(new ConcurrentTaskExecutor())
				.registerCallableInterceptors(new CallableProcessingInterceptor() { })
				.registerDeferredResultInterceptors(new DeferredResultProcessingInterceptor() {});
		}

		@Override
		public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
			argumentResolvers.add(new ModelAttributeMethodProcessor(true));
		}

		@Override
		public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
			returnValueHandlers.add(new ModelAttributeMethodProcessor(true));
		}

		@Override
		public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
			exceptionResolvers.add(new SimpleMappingExceptionResolver());
		}

		@Override
		public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
			exceptionResolvers.add(0, new ResponseStatusExceptionResolver());
		}

		@Override
		public void configurePathMatch(PathMatchConfigurer configurer) {
			configurer.setPathMatcher(new TestPathMatcher());
			configurer.setUrlPathHelper(new TestPathHelper());
			configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class));
		}

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(new LocaleChangeInterceptor());
		}

		@SuppressWarnings("serial")
		@Override
		public MessageCodesResolver getMessageCodesResolver() {
			return new DefaultMessageCodesResolver() {
				@Override
				public String[] resolveMessageCodes(String errorCode, String objectName) {
					return new String[] { "custom." + errorCode };
				}
			};
		}

		@Override
		public void addViewControllers(ViewControllerRegistry registry) {
			registry.addViewController("/path").setViewName("view");
			registry.addRedirectViewController("/old", "/new").setStatusCode(HttpStatus.PERMANENT_REDIRECT);
			registry.addStatusController("/bad", HttpStatus.NOT_FOUND);
		}

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.enableContentNegotiation(new MappingJackson2JsonView());
			registry.jsp("/", ".jsp");
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/resources/**").addResourceLocations("src/test/java");
		}

		@Override
		public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
			configurer.enable("default");
		}

		@Override
		public void addCorsMappings(CorsRegistry registry) {
			registry.addMapping("/resources/**");
		}

	}

	private class TestPathHelper extends UrlPathHelper {}

	private class TestPathMatcher extends AntPathMatcher {}


	@RestController
	@RequestMapping("/user")
	static class UserController {

		@GetMapping("/{id}")
		public Principal getUser() {
			return mock(Principal.class);
		}
	}

}
