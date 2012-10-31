/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.TestBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.DefaultMessageCodesResolver;
import org.springframework.validation.Errors;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.CallableProcessingInterceptorAdapter;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptorAdapter;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.handler.SimpleMappingExceptionResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * A test fixture with a sub-class of {@link WebMvcConfigurationSupport} that
 * implements the various {@link WebMvcConfigurer} extension points.
 *
 * @author Rossen Stoyanchev
 */
public class WebMvcConfigurationSupportExtensionTests {

	private TestWebMvcConfigurationSupport webConfig;

	private StaticWebApplicationContext webAppContext;


	@Before
	public void setUp() {
		this.webAppContext = new StaticWebApplicationContext();
		this.webAppContext.setServletContext(new MockServletContext(new FileSystemResourceLoader()));
		this.webAppContext.registerSingleton("controller", TestController.class);

		this.webConfig = new TestWebMvcConfigurationSupport();
		this.webConfig.setApplicationContext(this.webAppContext);
		this.webConfig.setServletContext(this.webAppContext.getServletContext());
	}

	@Test
	public void handlerMappings() throws Exception {
		RequestMappingHandlerMapping rmHandlerMapping = webConfig.requestMappingHandlerMapping();
		rmHandlerMapping.setApplicationContext(webAppContext);
		rmHandlerMapping.afterPropertiesSet();
		HandlerExecutionChain chain = rmHandlerMapping.getHandler(new MockHttpServletRequest("GET", "/"));
		assertNotNull(chain.getInterceptors());
		assertEquals(2, chain.getInterceptors().length);
		assertEquals(LocaleChangeInterceptor.class, chain.getInterceptors()[0].getClass());
		assertEquals(ConversionServiceExposingInterceptor.class, chain.getInterceptors()[1].getClass());

		AbstractHandlerMapping handlerMapping = (AbstractHandlerMapping) webConfig.viewControllerHandlerMapping();
		handlerMapping.setApplicationContext(webAppContext);
		assertNotNull(handlerMapping);
		assertEquals(1, handlerMapping.getOrder());
		HandlerExecutionChain handler = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/path"));
		assertNotNull(handler.getHandler());

		handlerMapping = (AbstractHandlerMapping) webConfig.resourceHandlerMapping();
		handlerMapping.setApplicationContext(webAppContext);
		assertNotNull(handlerMapping);
		assertEquals(Integer.MAX_VALUE-1, handlerMapping.getOrder());
		handler = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/resources/foo.gif"));
		assertNotNull(handler.getHandler());

		handlerMapping = (AbstractHandlerMapping) webConfig.defaultServletHandlerMapping();
		handlerMapping.setApplicationContext(webAppContext);
		assertNotNull(handlerMapping);
		assertEquals(Integer.MAX_VALUE, handlerMapping.getOrder());
		handler = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/anyPath"));
		assertNotNull(handler.getHandler());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void requestMappingHandlerAdapter() throws Exception {
		RequestMappingHandlerAdapter adapter = webConfig.requestMappingHandlerAdapter();

		// ConversionService
		String actual = webConfig.mvcConversionService().convert(new TestBean(), String.class);
		assertEquals("converted", actual);

		// Message converters
		assertEquals(1, adapter.getMessageConverters().size());

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
		RequestMappingHandlerAdapter adapter = webConfig.requestMappingHandlerAdapter();

		ConfigurableWebBindingInitializer initializer = (ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();
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

		ContentNegotiationManager manager = webConfig.requestMappingHandlerMapping().getContentNegotiationManager();
		assertEquals(Arrays.asList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(webRequest));

		request.setRequestURI("/foo.xml");
		assertEquals(Arrays.asList(MediaType.APPLICATION_XML), manager.resolveMediaTypes(webRequest));

		request.setRequestURI("/foo.rss");
		assertEquals(Arrays.asList(MediaType.valueOf("application/rss+xml")), manager.resolveMediaTypes(webRequest));

		request.setRequestURI("/foo.atom");
		assertEquals(Arrays.asList(MediaType.APPLICATION_ATOM_XML), manager.resolveMediaTypes(webRequest));

		request.setRequestURI("/foo");
		request.setParameter("f", "json");
		assertEquals(Arrays.asList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(webRequest));
	}

	@Test
	public void exceptionResolvers() throws Exception {
		HandlerExceptionResolverComposite composite = (HandlerExceptionResolverComposite) webConfig.handlerExceptionResolver();
		assertEquals(1, composite.getExceptionResolvers().size());
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
		public Validator getValidator() {
			return new Validator() {
				public void validate(Object target, Errors errors) {
					errors.reject("invalid");
				}
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
				.registerCallableInterceptors(new CallableProcessingInterceptorAdapter() { })
				.registerDeferredResultInterceptors(new DeferredResultProcessingInterceptorAdapter() {});
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
			registry.addViewController("/path");
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/resources/**").addResourceLocations("src/test/java");
		}

		@Override
		public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
			configurer.enable("default");
		}

	}

}
