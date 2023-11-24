/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

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
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.handler.ConversionServiceExposingInterceptor;
import org.springframework.web.servlet.handler.HandlerExceptionResolverComposite;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewRequestBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.JsonViewResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;
import org.springframework.web.servlet.support.SessionFlashMapManager;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.ViewResolverComposite;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.UrlPathHelper;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.DEFAULT_VIEW_INCLUSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.DispatcherServlet.FLASH_MAP_MANAGER_BEAN_NAME;
import static org.springframework.web.servlet.DispatcherServlet.LOCALE_RESOLVER_BEAN_NAME;
import static org.springframework.web.servlet.DispatcherServlet.REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME;
import static org.springframework.web.servlet.DispatcherServlet.THEME_RESOLVER_BEAN_NAME;

/**
 * Integration tests for {@link WebMvcConfigurationSupport} (imported via
 * {@link EnableWebMvc @EnableWebMvc}).
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Marten Deinum
 */
public class WebMvcConfigurationSupportTests {

	@Test
	public void requestMappingHandlerMapping() throws Exception {
		ApplicationContext context = initContext(WebConfig.class, ScopedController.class, ScopedProxyController.class);
		RequestMappingHandlerMapping handlerMapping = context.getBean(RequestMappingHandlerMapping.class);
		assertThat(handlerMapping.getOrder()).isEqualTo(0);

		HandlerExecutionChain chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/"));
		assertThat(chain).isNotNull();
		HandlerInterceptor[] interceptors = chain.getInterceptors();
		assertThat(interceptors).isNotNull();
		assertThat(interceptors[0].getClass()).isEqualTo(ConversionServiceExposingInterceptor.class);

		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/scoped"));
		assertThat(chain).as("HandlerExecutionChain for '/scoped' mapping should not be null.").isNotNull();

		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/scopedProxy"));
		assertThat(chain).as("HandlerExecutionChain for '/scopedProxy' mapping should not be null.").isNotNull();
	}

	@Test
	public void emptyHandlerMappings() {
		ApplicationContext context = initContext(WebConfig.class);

		Map<String, HandlerMapping> handlerMappings = context.getBeansOfType(HandlerMapping.class);
		assertThat(handlerMappings.containsKey("viewControllerHandlerMapping")).isFalse();
		assertThat(handlerMappings.containsKey("resourceHandlerMapping")).isFalse();
		assertThat(handlerMappings.containsKey("defaultServletHandlerMapping")).isFalse();

		Object nullBean = context.getBean("viewControllerHandlerMapping");
		assertThat(nullBean.equals(null)).isTrue();

		nullBean = context.getBean("resourceHandlerMapping");
		assertThat(nullBean.equals(null)).isTrue();

		nullBean = context.getBean("defaultServletHandlerMapping");
		assertThat(nullBean.equals(null)).isTrue();
	}

	@Test
	public void beanNameHandlerMapping() throws Exception {
		ApplicationContext context = initContext(WebConfig.class);
		BeanNameUrlHandlerMapping handlerMapping = context.getBean(BeanNameUrlHandlerMapping.class);
		assertThat(handlerMapping.getOrder()).isEqualTo(2);

		HttpServletRequest request = new MockHttpServletRequest("GET", "/testController");
		HandlerExecutionChain chain = handlerMapping.getHandler(request);

		assertThat(chain).isNotNull();
		HandlerInterceptor[] interceptors = chain.getInterceptors();
		assertThat(interceptors).isNotNull();
		assertThat(interceptors).hasSize(3);
		assertThat(interceptors[1].getClass()).isEqualTo(ConversionServiceExposingInterceptor.class);
		assertThat(interceptors[2].getClass()).isEqualTo(ResourceUrlProviderExposingInterceptor.class);
	}

	@Test
	public void requestMappingHandlerAdapter() {
		ApplicationContext context = initContext(WebConfig.class);
		RequestMappingHandlerAdapter adapter = context.getBean(RequestMappingHandlerAdapter.class);
		List<HttpMessageConverter<?>> converters = adapter.getMessageConverters();
		assertThat(converters).hasSizeGreaterThanOrEqualTo(14);
		converters.stream()
				.filter(converter -> converter instanceof AbstractJackson2HttpMessageConverter)
				.forEach(converter -> {
					ObjectMapper mapper = ((AbstractJackson2HttpMessageConverter) converter).getObjectMapper();
					assertThat(mapper.getDeserializationConfig().isEnabled(DEFAULT_VIEW_INCLUSION)).isFalse();
					assertThat(mapper.getSerializationConfig().isEnabled(DEFAULT_VIEW_INCLUSION)).isFalse();
					assertThat(mapper.getDeserializationConfig().isEnabled(FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
					if (converter instanceof MappingJackson2XmlHttpMessageConverter) {
						assertThat(mapper.getClass()).isEqualTo(XmlMapper.class);
					}
				});

		ConfigurableWebBindingInitializer initializer =
				(ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();
		assertThat(initializer).isNotNull();

		ConversionService conversionService = initializer.getConversionService();
		assertThat(conversionService).isNotNull();
		boolean condition1 = conversionService instanceof FormattingConversionService;
		assertThat(condition1).isTrue();

		Validator validator = initializer.getValidator();
		assertThat(validator).isNotNull();
		boolean condition = validator instanceof LocalValidatorFactoryBean;
		assertThat(condition).isTrue();

		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(adapter);
		@SuppressWarnings("unchecked")
		List<Object> bodyAdvice = (List<Object>) fieldAccessor.getPropertyValue("requestResponseBodyAdvice");
		assertThat(bodyAdvice).hasSize(2);
		assertThat(bodyAdvice.get(0).getClass()).isEqualTo(JsonViewRequestBodyAdvice.class);
		assertThat(bodyAdvice.get(1).getClass()).isEqualTo(JsonViewResponseBodyAdvice.class);
	}

	@Test
	public void uriComponentsContributor() {
		ApplicationContext context = initContext(WebConfig.class);
		CompositeUriComponentsContributor uriComponentsContributor = context.getBean(
				MvcUriComponentsBuilder.MVC_URI_COMPONENTS_CONTRIBUTOR_BEAN_NAME,
				CompositeUriComponentsContributor.class);

		assertThat(uriComponentsContributor).isNotNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void handlerExceptionResolver() {
		ApplicationContext context = initContext(WebConfig.class);
		HandlerExceptionResolverComposite compositeResolver =
				context.getBean("handlerExceptionResolver", HandlerExceptionResolverComposite.class);

		assertThat(compositeResolver.getOrder()).isEqualTo(0);
		List<HandlerExceptionResolver> expectedResolvers = compositeResolver.getExceptionResolvers();

		assertThat(expectedResolvers.get(0).getClass()).isEqualTo(ExceptionHandlerExceptionResolver.class);
		assertThat(expectedResolvers.get(1).getClass()).isEqualTo(ResponseStatusExceptionResolver.class);
		assertThat(expectedResolvers.get(2).getClass()).isEqualTo(DefaultHandlerExceptionResolver.class);

		ExceptionHandlerExceptionResolver eher = (ExceptionHandlerExceptionResolver) expectedResolvers.get(0);
		assertThat(eher.getApplicationContext()).isNotNull();

		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(eher);
		List<Object> interceptors = (List<Object>) fieldAccessor.getPropertyValue("responseBodyAdvice");
		assertThat(interceptors).hasSize(1);
		assertThat(interceptors.get(0).getClass()).isEqualTo(JsonViewResponseBodyAdvice.class);

		LocaleContextHolder.setLocale(Locale.ENGLISH);
		try {
			ResponseStatusExceptionResolver rser = (ResponseStatusExceptionResolver) expectedResolvers.get(1);
			MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
			MockHttpServletResponse response = new MockHttpServletResponse();
			rser.resolveException(request, response, context.getBean(TestController.class), new UserAlreadyExistsException());
			assertThat(response.getErrorMessage()).isEqualTo("User already exists!");
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

		assertThat(adapter).isNotNull();
		assertThat(adapter.getCustomArgumentResolvers()).hasSize(1);
		assertThat(adapter.getCustomArgumentResolvers().get(0).getClass()).isEqualTo(TestArgumentResolver.class);
		assertThat(adapter.getCustomReturnValueHandlers()).hasSize(1);
		assertThat(adapter.getCustomReturnValueHandlers().get(0).getClass()).isEqualTo(TestReturnValueHandler.class);

		assertThat(composite).isNotNull();
		assertThat(composite.getExceptionResolvers()).hasSize(3);
		assertThat(composite.getExceptionResolvers().get(0).getClass()).isEqualTo(ExceptionHandlerExceptionResolver.class);

		ExceptionHandlerExceptionResolver resolver =
				(ExceptionHandlerExceptionResolver) composite.getExceptionResolvers().get(0);

		assertThat(resolver.getCustomArgumentResolvers()).hasSize(1);
		assertThat(resolver.getCustomArgumentResolvers().get(0).getClass()).isEqualTo(TestArgumentResolver.class);
		assertThat(resolver.getCustomReturnValueHandlers()).hasSize(1);
		assertThat(resolver.getCustomReturnValueHandlers().get(0).getClass()).isEqualTo(TestReturnValueHandler.class);
	}


	@Test
	public void mvcViewResolver() {
		ApplicationContext context = initContext(WebConfig.class);
		ViewResolverComposite resolver = context.getBean("mvcViewResolver", ViewResolverComposite.class);

		assertThat(resolver).isNotNull();
		assertThat(resolver.getViewResolvers()).hasSize(1);
		assertThat(resolver.getViewResolvers().get(0).getClass()).isEqualTo(InternalResourceViewResolver.class);
		assertThat(resolver.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
	}

	@Test
	public void mvcViewResolverWithExistingResolver() throws Exception {
		ApplicationContext context = initContext(WebConfig.class, ViewResolverConfig.class);
		ViewResolverComposite resolver = context.getBean("mvcViewResolver", ViewResolverComposite.class);

		assertThat(resolver).isNotNull();
		assertThat(resolver.getViewResolvers()).isEmpty();
		assertThat(resolver.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
		assertThat(resolver.resolveViewName("anyViewName", Locale.ENGLISH)).isNull();
	}

	@Test
	public void mvcViewResolverWithOrderSet() {
		ApplicationContext context = initContext(CustomViewResolverOrderConfig.class);
		ViewResolverComposite resolver = context.getBean("mvcViewResolver", ViewResolverComposite.class);

		assertThat(resolver).isNotNull();
		assertThat(resolver.getViewResolvers()).hasSize(1);
		assertThat(resolver.getViewResolvers().get(0).getClass()).isEqualTo(InternalResourceViewResolver.class);
		assertThat(resolver.getOrder()).isEqualTo(123);
	}

	@Test
	public void defaultPathMatchConfiguration() {
		ApplicationContext context = initContext(WebConfig.class);
		UrlPathHelper urlPathHelper = context.getBean(UrlPathHelper.class);
		PathMatcher pathMatcher = context.getBean(PathMatcher.class);

		assertThat(urlPathHelper).isNotNull();
		assertThat(pathMatcher).isNotNull();
		assertThat(pathMatcher.getClass()).isEqualTo(AntPathMatcher.class);
	}

	@Test
	public void defaultLocaleResolverConfiguration() {
		ApplicationContext context = initContext(WebConfig.class);
		LocaleResolver localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);

		assertThat(localeResolver).isNotNull();
		assertThat(localeResolver).isInstanceOf(AcceptHeaderLocaleResolver.class);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void defaultThemeResolverConfiguration() {
		ApplicationContext context = initContext(WebConfig.class);
		org.springframework.web.servlet.ThemeResolver themeResolver =
				context.getBean(THEME_RESOLVER_BEAN_NAME, org.springframework.web.servlet.ThemeResolver.class);

		assertThat(themeResolver).isNotNull();
		assertThat(themeResolver).isInstanceOf(org.springframework.web.servlet.theme.FixedThemeResolver.class);
	}

	@Test
	public void defaultFlashMapManagerConfiguration() {
		ApplicationContext context = initContext(WebConfig.class);
		FlashMapManager flashMapManager = context.getBean(FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);

		assertThat(flashMapManager).isNotNull();
		assertThat(flashMapManager).isInstanceOf(SessionFlashMapManager.class);
	}

	@Test
	public void defaultRequestToViewNameConfiguration() throws Exception {
		ApplicationContext context = initContext(WebConfig.class);
		RequestToViewNameTranslator requestToViewNameTranslator;
		requestToViewNameTranslator = context.getBean(REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME,
				RequestToViewNameTranslator.class);

		assertThat(requestToViewNameTranslator).isNotNull();
		assertThat(requestToViewNameTranslator).isInstanceOf(DefaultRequestToViewNameTranslator.class);
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
				@DateTimeFormat(iso = ISO.DATE) @PathVariable Date date) {
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
