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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.joda.time.DateTime;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.method.support.CompositeUriComponentsContributor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewRequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.ViewResolverComposite;
import org.springframework.web.util.UrlPathHelper;

import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.databind.MapperFeature.*;
import static org.junit.Assert.*;

/**
 * Integration tests for {@link WebMvcConfigurationSupport} (imported via
 * {@link EnableWebMvc @EnableWebMvc}).
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
public class WebMvcConfigurationSupportTests {

	@Test
	public void requestMappingHandlerMapping() throws Exception {
		ApplicationContext context = initContext(WebConfig.class, ScopedController.class, ScopedProxyController.class);
		RequestMappingHandlerMapping handlerMapping = context.getBean(RequestMappingHandlerMapping.class);
		assertEquals(0, handlerMapping.getOrder());

		HandlerExecutionChain chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/"));
		assertNotNull(chain);
		assertNotNull(chain.getInterceptors());
		assertEquals(ConversionServiceExposingInterceptor.class, chain.getInterceptors()[0].getClass());

		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/scoped"));
		assertNotNull("HandlerExecutionChain for '/scoped' mapping should not be null.", chain);

		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/scopedProxy"));
		assertNotNull("HandlerExecutionChain for '/scopedProxy' mapping should not be null.", chain);
	}

	@Test
	public void emptyHandlerMappings() {
		ApplicationContext context = initContext(WebConfig.class);

		Map<String, HandlerMapping> handlerMappings = context.getBeansOfType(HandlerMapping.class);
		assertFalse(handlerMappings.containsKey("viewControllerHandlerMapping"));
		assertFalse(handlerMappings.containsKey("resourceHandlerMapping"));
		assertFalse(handlerMappings.containsKey("defaultServletHandlerMapping"));

		Object nullBean = context.getBean("viewControllerHandlerMapping");
		assertTrue(nullBean.equals(null));

		nullBean = context.getBean("resourceHandlerMapping");
		assertTrue(nullBean.equals(null));

		nullBean = context.getBean("defaultServletHandlerMapping");
		assertTrue(nullBean.equals(null));
	}

	@Test
	public void beanNameHandlerMapping() throws Exception {
		ApplicationContext context = initContext(WebConfig.class);
		BeanNameUrlHandlerMapping handlerMapping = context.getBean(BeanNameUrlHandlerMapping.class);
		assertEquals(2, handlerMapping.getOrder());

		HttpServletRequest request = new MockHttpServletRequest("GET", "/testController");
		HandlerExecutionChain chain = handlerMapping.getHandler(request);

		assertNotNull(chain);
		assertNotNull(chain.getInterceptors());
		assertEquals(3, chain.getInterceptors().length);
		assertEquals(ConversionServiceExposingInterceptor.class, chain.getInterceptors()[1].getClass());
		assertEquals(ResourceUrlProviderExposingInterceptor.class, chain.getInterceptors()[2].getClass());
	}

	@Test
	public void requestMappingHandlerAdapter() throws Exception {
		ApplicationContext context = initContext(WebConfig.class);
		RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);
		List<HttpMessageConverter<?>> converters = adapter.getMessageConverters();
		assertEquals(12, converters.size());
		converters.stream()
				.filter(converter -> converter instanceof AbstractJackson2HttpMessageConverter)
				.forEach(converter -> {
					ObjectMapper mapper = ((AbstractJackson2HttpMessageConverter) converter).getObjectMapper();
					assertFalse(mapper.getDeserializationConfig().isEnabled(DEFAULT_VIEW_INCLUSION));
					assertFalse(mapper.getSerializationConfig().isEnabled(DEFAULT_VIEW_INCLUSION));
					assertFalse(mapper.getDeserializationConfig().isEnabled(FAIL_ON_UNKNOWN_PROPERTIES));
					if (converter instanceof MappingJackson2XmlHttpMessageConverter) {
						assertEquals(XmlMapper.class, mapper.getClass());
					}
				});

		ConfigurableWebBindingInitializer initializer =
				(ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();
		assertNotNull(initializer);

		ConversionService conversionService = initializer.getConversionService();
		assertNotNull(conversionService);
		assertTrue(conversionService instanceof FormattingConversionService);

		Validator validator = initializer.getValidator();
		assertNotNull(validator);
		assertTrue(validator instanceof LocalValidatorFactoryBean);

		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(adapter);
		@SuppressWarnings("unchecked")
		List<Object> bodyAdvice = (List<Object>) fieldAccessor.getPropertyValue("requestResponseBodyAdvice");
		assertEquals(2, bodyAdvice.size());
		assertEquals(JsonViewRequestBodyAdvice.class, bodyAdvice.get(0).getClass());
		assertEquals(JsonViewResponseBodyAdvice.class, bodyAdvice.get(1).getClass());
	}

	@Test
	public void uriComponentsContributor() throws Exception {
		ApplicationContext context = initContext(WebConfig.class);
		CompositeUriComponentsContributor uriComponentsContributor = context.getBean(
				MvcUriComponentsBuilder.MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME,
				CompositeUriComponentsContributor.class);

		assertNotNull(uriComponentsContributor);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void handlerExceptionResolver() throws Exception {
		ApplicationContext context = initContext(WebConfig.class);
		HandlerExceptionResolverComposite compositeResolver =
				context.getBean("handlerExceptionResolver", HandlerExceptionResolverComposite.class);

		assertEquals(0, compositeResolver.getOrder());
		List<HandlerExceptionResolver> expectedResolvers = compositeResolver.getExceptionResolvers();

		assertEquals(ExceptionHandlerExceptionResolver.class, expectedResolvers.get(0).getClass());
		assertEquals(ResponseStatusExceptionResolver.class, expectedResolvers.get(1).getClass());
		assertEquals(DefaultHandlerExceptionResolver.class, expectedResolvers.get(2).getClass());

		ExceptionHandlerExceptionResolver eher = (ExceptionHandlerExceptionResolver) expectedResolvers.get(0);
		assertNotNull(eher.getApplicationContext());

		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(eher);
		List<Object> interceptors = (List<Object>) fieldAccessor.getPropertyValue("responseBodyAdvice");
		assertEquals(1, interceptors.size());
		assertEquals(JsonViewResponseBodyAdvice.class, interceptors.get(0).getClass());

		LocaleContextHolder.setLocale(Locale.ENGLISH);
		try {
			ResponseStatusExceptionResolver rser = (ResponseStatusExceptionResolver) expectedResolvers.get(1);
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
			MockHttpServletResponse response = new MockHttpServletResponse();
			rser.resolveException(request, response, context.getBean(TestController.class), new UserAlreadyExistsException());
			assertEquals("User already exists!", response.getErrorMessage());
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	@Test
	public void customArgumentResolvers() {
		ApplicationContext context = initContext(CustomArgumentResolverConfig.class);
		RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);
		HandlerExceptionResolverComposite composite = context.getBean(HandlerExceptionResolverComposite.class);

		assertNotNull(adapter);
		assertEquals(1, adapter.getCustomArgumentResolvers().size());
		assertEquals(TestArgumentResolver.class, adapter.getCustomArgumentResolvers().get(0).getClass());
		assertEquals(1, adapter.getCustomReturnValueHandlers().size());
		assertEquals(TestReturnValueHandler.class, adapter.getCustomReturnValueHandlers().get(0).getClass());

		assertNotNull(composite);
		assertEquals(3, composite.getExceptionResolvers().size());
		assertEquals(ExceptionHandlerExceptionResolver.class, composite.getExceptionResolvers().get(0).getClass());

		ExceptionHandlerExceptionResolver resolver =
				(ExceptionHandlerExceptionResolver) composite.getExceptionResolvers().get(0);

		assertEquals(1, resolver.getCustomArgumentResolvers().size());
		assertEquals(TestArgumentResolver.class, resolver.getCustomArgumentResolvers().get(0).getClass());
		assertEquals(1, resolver.getCustomReturnValueHandlers().size());
		assertEquals(TestReturnValueHandler.class, resolver.getCustomReturnValueHandlers().get(0).getClass());
	}


	@Test
	public void mvcViewResolver() {
		ApplicationContext context = initContext(WebConfig.class);
		ViewResolverComposite resolver = context.getBean("mvcViewResolver", ViewResolverComposite.class);

		assertNotNull(resolver);
		assertEquals(1, resolver.getViewResolvers().size());
		assertEquals(InternalResourceViewResolver.class, resolver.getViewResolvers().get(0).getClass());
		assertEquals(Ordered.LOWEST_PRECEDENCE, resolver.getOrder());
	}

	@Test
	public void mvcViewResolverWithExistingResolver() throws Exception {
		ApplicationContext context = initContext(WebConfig.class, ViewResolverConfig.class);
		ViewResolverComposite resolver = context.getBean("mvcViewResolver", ViewResolverComposite.class);

		assertNotNull(resolver);
		assertEquals(0, resolver.getViewResolvers().size());
		assertEquals(Ordered.LOWEST_PRECEDENCE, resolver.getOrder());
		assertNull(resolver.resolveViewName("anyViewName", Locale.ENGLISH));
	}

	@Test
	public void mvcViewResolverWithOrderSet() {
		ApplicationContext context = initContext(CustomViewResolverOrderConfig.class);
		ViewResolverComposite resolver = context.getBean("mvcViewResolver", ViewResolverComposite.class);

		assertNotNull(resolver);
		assertEquals(1, resolver.getViewResolvers().size());
		assertEquals(InternalResourceViewResolver.class, resolver.getViewResolvers().get(0).getClass());
		assertEquals(123, resolver.getOrder());
	}

	@Test
	public void defaultPathMatchConfiguration() throws Exception {
		ApplicationContext context = initContext(WebConfig.class);
		UrlPathHelper urlPathHelper = context.getBean(UrlPathHelper.class);
		PathMatcher pathMatcher = context.getBean(PathMatcher.class);

		assertNotNull(urlPathHelper);
		assertNotNull(pathMatcher);
		assertEquals(AntPathMatcher.class, pathMatcher.getClass());
	}

	private ApplicationContext initContext(Class<?>... configClasses) {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(configClasses);
		context.refresh();
		return context;
	}


	@EnableWebMvc
	@Configuration
	static class WebConfig {

		@Bean("/testController")
		public TestController testController() {
			return new TestController();
		}

		@Bean
		public MessageSource messageSource() {
			StaticMessageSource messageSource = new StaticMessageSource();
			messageSource.addMessage("exception.user.exists", Locale.ENGLISH, "User already exists!");
			return messageSource;
		}
	}


	@Configuration
	static class ViewResolverConfig {

		@Bean
		public ViewResolver beanNameViewResolver() {
			return new BeanNameViewResolver();
		}
	}


	@EnableWebMvc
	@Configuration
	static class CustomViewResolverOrderConfig implements WebMvcConfigurer {

		@Override
		public void configureViewResolvers(ViewResolverRegistry registry) {
			registry.jsp();
			registry.order(123);
		}
	}

	@EnableWebMvc
	@Configuration
	static class CustomArgumentResolverConfig implements WebMvcConfigurer {

		@Override
		public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
			resolvers.add(new TestArgumentResolver());
		}

		@Override
		public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
			handlers.add(new TestReturnValueHandler());
		}
	}


	@Controller
	private static class TestController {

		@RequestMapping("/")
		public void handle() {
		}

		@RequestMapping("/foo/{id}/bar/{date}")
		public HttpEntity<Void> methodWithTwoPathVariables(@PathVariable Integer id,
				@DateTimeFormat(iso = ISO.DATE) @PathVariable DateTime date) {
			return null;
		}
	}


	@Controller
	@Scope("prototype")
	private static class ScopedController {

		@RequestMapping("/scoped")
		public void handle() {
		}
	}


	@Controller
	@Scope(scopeName = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
	static class ScopedProxyController {

		@RequestMapping("/scopedProxy")
		public void handle() {
		}
	}


	@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "exception.user.exists")
	@SuppressWarnings("serial")
	private static class UserAlreadyExistsException extends RuntimeException {
	}

	private static class TestArgumentResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return false;
		}

		@Override
		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
				NativeWebRequest request, WebDataBinderFactory factory) {
			return null;
		}
	}

	private static class TestReturnValueHandler implements HandlerMethodReturnValueHandler {

		@Override
		public boolean supportsReturnType(MethodParameter returnType) {
			return false;
		}

		@Override
		public void handleReturnValue(Object value, MethodParameter parameter,
				ModelAndViewContainer container, NativeWebRequest request) {
		}
	}

}
