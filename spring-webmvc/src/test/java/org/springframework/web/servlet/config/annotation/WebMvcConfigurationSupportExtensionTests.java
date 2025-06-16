/*
 * Copyright 2002-2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.Ordered;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.stereotype.Controller;
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
import org.springframework.web.servlet.HandlerInterceptor;
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
import org.springframework.web.servlet.resource.ResourceUrlProviderExposingInterceptor;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.ViewResolverComposite;
import org.springframework.web.servlet.view.json.JacksonJsonView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.UrlPathHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;

/**
 * A test fixture with a subclass of {@link WebMvcConfigurationSupport} that also
 * implements the various {@link WebMvcConfigurer} extension points.
 * <p>
 * The former doesn't implement the latter but the two must have compatible
 * callback method signatures to support moving from simple to advanced
 * configuration -- i.e. dropping @EnableWebMvc + WebMvcConfigurer and extending
 * directly from WebMvcConfigurationSupport.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class WebMvcConfigurationSupportExtensionTests {

	private TestWebMvcConfigurationSupport config;

	private StaticWebApplicationContext context;


	@BeforeEach
	void setUp() {
		this.context = new StaticWebApplicationContext();
		this.context.setServletContext(new MockServletContext(new FileSystemResourceLoader()));
		this.context.registerSingleton("controller", TestController.class);
		this.context.registerSingleton("userController", UserController.class);

		this.config = new TestWebMvcConfigurationSupport();
		this.config.setApplicationContext(this.context);
		this.config.setServletContext(this.context.getServletContext());
	}

	@SuppressWarnings("removal")
	@Test
	void handlerMappings() throws Exception {
		RequestMappingHandlerMapping rmHandlerMapping = this.config.requestMappingHandlerMapping(
				this.config.mvcContentNegotiationManager(), this.config.mvcApiVersionStrategy(),
				this.config.mvcConversionService(), this.config.mvcResourceUrlProvider());
		rmHandlerMapping.setApplicationContext(this.context);
		rmHandlerMapping.afterPropertiesSet();
		assertThat(rmHandlerMapping.getUrlPathHelper().getClass()).isEqualTo(TestPathHelper.class);
		assertThat(rmHandlerMapping.getPathMatcher().getClass()).isEqualTo(TestPathMatcher.class);
		HandlerExecutionChain chain = rmHandlerMapping.getHandler(new MockHttpServletRequest("GET", "/"));
		assertThat(chain).isNotNull();
		HandlerInterceptor[] interceptors = chain.getInterceptors();
		assertThat(interceptors).isNotNull();
		assertThat(interceptors).hasSize(4);
		assertThat(interceptors[0].getClass().getSimpleName()).isEqualTo("CorsInterceptor");
		assertThat(interceptors[1].getClass()).isEqualTo(LocaleChangeInterceptor.class);
		assertThat(interceptors[2].getClass()).isEqualTo(ConversionServiceExposingInterceptor.class);
		assertThat(interceptors[3].getClass()).isEqualTo(ResourceUrlProviderExposingInterceptor.class);

		Map<RequestMappingInfo, HandlerMethod> map = rmHandlerMapping.getHandlerMethods();
		assertThat(map).hasSize(2);
		RequestMappingInfo info = map.entrySet().stream()
				.filter(entry -> entry.getValue().getBeanType().equals(UserController.class))
				.findFirst()
				.orElseThrow(() -> new AssertionError("UserController bean not found"))
				.getKey();
		assertThat(info.getPatternValues()).isEqualTo(Collections.singleton("/api/user/{id}"));

		AbstractHandlerMapping handlerMapping = (AbstractHandlerMapping) this.config.viewControllerHandlerMapping(
				this.config.mvcConversionService(), this.config.mvcResourceUrlProvider());
		handlerMapping.setApplicationContext(this.context);
		assertThat(handlerMapping).isNotNull();
		assertThat(handlerMapping.getOrder()).isEqualTo(1);
		assertThat(handlerMapping.getUrlPathHelper().getClass()).isEqualTo(TestPathHelper.class);
		assertThat(handlerMapping.getPathMatcher().getClass()).isEqualTo(TestPathMatcher.class);
		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/path"));
		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isNotNull();
		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/bad"));
		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isNotNull();
		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/old"));
		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isNotNull();

		handlerMapping = (AbstractHandlerMapping) this.config.resourceHandlerMapping(
				this.config.mvcContentNegotiationManager(), this.config.mvcConversionService(),
				this.config.mvcResourceUrlProvider());
		handlerMapping.setApplicationContext(this.context);
		assertThat(handlerMapping).isNotNull();
		assertThat(handlerMapping.getOrder()).isEqualTo((Integer.MAX_VALUE - 1));
		assertThat(handlerMapping.getUrlPathHelper().getClass()).isEqualTo(TestPathHelper.class);
		assertThat(handlerMapping.getPathMatcher().getClass()).isEqualTo(TestPathMatcher.class);
		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/resources/foo.gif"));
		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isNotNull();
		interceptors = chain.getInterceptors();
		assertThat(interceptors.length).as(Arrays.toString(interceptors)).isEqualTo(5);
		assertThat(interceptors[0].getClass().getSimpleName()).isEqualTo("CorsInterceptor");
		// PathExposingHandlerInterceptor at interceptors[1]
		assertThat(interceptors[2].getClass()).isEqualTo(LocaleChangeInterceptor.class);
		assertThat(interceptors[3].getClass()).isEqualTo(ConversionServiceExposingInterceptor.class);
		assertThat(interceptors[4].getClass()).isEqualTo(ResourceUrlProviderExposingInterceptor.class);

		handlerMapping = (AbstractHandlerMapping) this.config.defaultServletHandlerMapping();
		handlerMapping.setApplicationContext(this.context);
		assertThat(handlerMapping).isNotNull();
		assertThat(handlerMapping.getOrder()).isEqualTo(Integer.MAX_VALUE);
		chain = handlerMapping.getHandler(new MockHttpServletRequest("GET", "/anyPath"));
		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isNotNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	void requestMappingHandlerAdapter() {
		RequestMappingHandlerAdapter adapter = this.config.requestMappingHandlerAdapter(
				this.config.mvcContentNegotiationManager(), this.config.mvcConversionService(),
				this.config.mvcValidator());

		// ConversionService
		String actual = this.config.mvcConversionService().convert(new TestBean(), String.class);
		assertThat(actual).isEqualTo("converted");

		// Message converters
		List<HttpMessageConverter<?>> converters = adapter.getMessageConverters();
		assertThat(converters).hasSize(2);
		assertThat(converters.get(0).getClass()).isEqualTo(StringHttpMessageConverter.class);
		assertThat(converters.get(1).getClass()).isEqualTo(JacksonJsonHttpMessageConverter.class);
		ObjectMapper objectMapper = ((JacksonJsonHttpMessageConverter) converters.get(1)).getObjectMapper();
		assertThat(objectMapper.deserializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isFalse();
		assertThat(objectMapper.deserializationConfig().isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
		assertThat(objectMapper.serializationConfig().isEnabled(MapperFeature.DEFAULT_VIEW_INCLUSION)).isFalse();

		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(adapter);

		// Custom argument resolvers and return value handlers
		List<HandlerMethodArgumentResolver> argResolvers =
				(List<HandlerMethodArgumentResolver>) fieldAccessor.getPropertyValue("customArgumentResolvers");
		assertThat(argResolvers).hasSize(1);

		List<HandlerMethodReturnValueHandler> handlers =
				(List<HandlerMethodReturnValueHandler>) fieldAccessor.getPropertyValue("customReturnValueHandlers");
		assertThat(handlers).hasSize(1);

		// Async support options
		assertThat(fieldAccessor.getPropertyValue("taskExecutor").getClass()).isEqualTo(ConcurrentTaskExecutor.class);
		assertThat(fieldAccessor.getPropertyValue("asyncRequestTimeout")).isEqualTo(2500L);

		CallableProcessingInterceptor[] callableInterceptors =
				(CallableProcessingInterceptor[]) fieldAccessor.getPropertyValue("callableInterceptors");
		assertThat(callableInterceptors).hasSize(1);

		DeferredResultProcessingInterceptor[] deferredResultInterceptors =
				(DeferredResultProcessingInterceptor[]) fieldAccessor.getPropertyValue("deferredResultInterceptors");
		assertThat(deferredResultInterceptors).hasSize(1);
	}

	@Test
	void webBindingInitializer() {
		RequestMappingHandlerAdapter adapter = this.config.requestMappingHandlerAdapter(
				this.config.mvcContentNegotiationManager(), this.config.mvcConversionService(),
				this.config.mvcValidator());

		ConfigurableWebBindingInitializer initializer =
				(ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();

		assertThat(initializer).isNotNull();

		BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(null, "");
		initializer.getValidator().validate(null, bindingResult);
		assertThat(bindingResult.getAllErrors().get(0).getCode()).isEqualTo("invalid");

		String[] codes = initializer.getMessageCodesResolver().resolveMessageCodes("invalid", null);
		assertThat(codes[0]).isEqualTo("custom.invalid");
	}

	@Test
	public void contentNegotiation() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		NativeWebRequest webRequest = new ServletWebRequest(request);

		RequestMappingHandlerMapping mapping = this.config.requestMappingHandlerMapping(
				this.config.mvcContentNegotiationManager(), this.config.mvcApiVersionStrategy(),
				this.config.mvcConversionService(), this.config.mvcResourceUrlProvider());

		request.setParameter("f", "json");
		ContentNegotiationManager manager = mapping.getContentNegotiationManager();
		assertThat(manager.resolveMediaTypes(webRequest)).isEqualTo(Collections.singletonList(APPLICATION_JSON));

		request.setParameter("f", "xml");
		assertThat(manager.resolveMediaTypes(webRequest)).isEqualTo(Collections.singletonList(APPLICATION_XML));

		SimpleUrlHandlerMapping handlerMapping = (SimpleUrlHandlerMapping) this.config.resourceHandlerMapping(
				this.config.mvcContentNegotiationManager(), this.config.mvcConversionService(),
				this.config.mvcResourceUrlProvider());
		handlerMapping.setApplicationContext(this.context);

		request = new MockHttpServletRequest("GET", "/resources/foo.gif");
		HandlerExecutionChain chain = handlerMapping.getHandler(request);
		assertThat(chain).isNotNull();
	}

	@Test
	void exceptionResolvers() {
		List<HandlerExceptionResolver> resolvers = ((HandlerExceptionResolverComposite)
				this.config.handlerExceptionResolver(null)).getExceptionResolvers();

		assertThat(resolvers).hasSize(2);
		assertThat(resolvers.get(0).getClass()).isEqualTo(ResponseStatusExceptionResolver.class);
		assertThat(resolvers.get(1).getClass()).isEqualTo(SimpleMappingExceptionResolver.class);
	}

	@SuppressWarnings("unchecked")
	@Test
	void viewResolvers() {
		ViewResolverComposite viewResolver = (ViewResolverComposite) this.config.mvcViewResolver(
				this.config.mvcContentNegotiationManager());
		assertThat(viewResolver.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
		List<ViewResolver> viewResolvers = viewResolver.getViewResolvers();

		DirectFieldAccessor accessor = new DirectFieldAccessor(viewResolvers.get(0));
		assertThat(viewResolvers).hasSize(1);
		assertThat(viewResolvers.get(0).getClass()).isEqualTo(ContentNegotiatingViewResolver.class);
		assertThat((Boolean) accessor.getPropertyValue("useNotAcceptableStatusCode")).isFalse();
		assertThat(accessor.getPropertyValue("contentNegotiationManager")).isNotNull();

		List<View> defaultViews = (List<View>) accessor.getPropertyValue("defaultViews");
		assertThat(defaultViews).isNotNull();
		assertThat(defaultViews).hasSize(1);
		assertThat(defaultViews.get(0).getClass()).isEqualTo(JacksonJsonView.class);

		viewResolvers = (List<ViewResolver>) accessor.getPropertyValue("viewResolvers");
		assertThat(viewResolvers).isNotNull();
		assertThat(viewResolvers).hasSize(1);
		assertThat(viewResolvers.get(0).getClass()).isEqualTo(InternalResourceViewResolver.class);
		accessor = new DirectFieldAccessor(viewResolvers.get(0));
		assertThat(accessor.getPropertyValue("prefix")).isEqualTo("/");
		assertThat(accessor.getPropertyValue("suffix")).isEqualTo(".jsp");
	}

	@Test
	void crossOrigin() {
		Map<String, CorsConfiguration> configs = this.config.getCorsConfigurations();
		assertThat(configs).hasSize(1);
		assertThat(configs.get("/resources/**").getAllowedOrigins()).containsExactly("*");
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
	private static class TestWebMvcConfigurationSupport extends WebMvcConfigurationSupport implements WebMvcConfigurer {

		@Override
		public void addFormatters(FormatterRegistry registry) {
			registry.addConverter(TestBean.class, String.class, testBean -> "converted");
		}

		@Override
		public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
			converters.add(new JacksonJsonHttpMessageConverter());
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
		public void configureApiVersioning(ApiVersionConfigurer configurer) {
			configurer.useRequestHeader("X-API-Version");
		}

		@Override
		@SuppressWarnings("deprecation")
		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
			configurer.setDefaultTimeout(2500).setTaskExecutor(new ConcurrentTaskExecutor())
					.registerCallableInterceptors(new CallableProcessingInterceptor() {})
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

		@SuppressWarnings("removal")
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

		@Override
		public MessageCodesResolver getMessageCodesResolver() {
			return new DefaultMessageCodesResolver() {
				@Override
				public String[] resolveMessageCodes(String errorCode, String objectName) {
					return new String[] {"custom." + errorCode};
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
			registry.enableContentNegotiation(new JacksonJsonView());
			registry.jsp("/", ".jsp");
		}

		@Override
		public void addResourceHandlers(ResourceHandlerRegistry registry) {
			registry.addResourceHandler("/resources/**").addResourceLocations("src/test/java/");
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

	private static class TestPathHelper extends UrlPathHelper {
	}

	private static class TestPathMatcher extends AntPathMatcher {
	}


	@RestController
	@RequestMapping("/user")
	static class UserController {

		@GetMapping("/{id}")
		public Principal getUser() {
			return mock();
		}
	}

}
