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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Test fixture for {@link CrossOrigin @CrossOrigin} annotated methods.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author Nicolas Labrot
 */
public class CrossOriginTests {

	private final TestRequestMappingInfoHandlerMapping handlerMapping = new TestRequestMappingInfoHandlerMapping();

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	@Rule
	public ExpectedException exception = ExpectedException.none();


	@Before
	public void setup() {
		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		Properties props = new Properties();
		props.setProperty("myOrigin", "http://example.com");
		wac.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("ps", props));
		wac.registerSingleton("ppc", PropertySourcesPlaceholderConfigurer.class);
		wac.refresh();

		this.handlerMapping.setRemoveSemicolonContent(false);
		wac.getAutowireCapableBeanFactory().initializeBean(this.handlerMapping, "hm");

		this.request.setMethod("GET");
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain.com/");
	}


	@Test
	public void noAnnotationWithoutOrigin() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/no");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		assertNull(getCorsConfiguration(chain, false));
	}

	@Test  // SPR-12931
	public void noAnnotationWithOrigin() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setRequestURI("/no");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		assertNull(getCorsConfiguration(chain, false));
	}

	@Test  // SPR-12931
	public void noAnnotationPostWithOrigin() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setMethod("POST");
		this.request.setRequestURI("/no");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		assertNull(getCorsConfiguration(chain, false));
	}

	@Test
	public void defaultAnnotation() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setRequestURI("/default");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(new String[] {"GET"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] {"*"}, config.getAllowedOrigins().toArray());
		assertNull(config.getAllowCredentials());
		assertArrayEquals(new String[] {"*"}, config.getAllowedHeaders().toArray());
		assertTrue(CollectionUtils.isEmpty(config.getExposedHeaders()));
		assertEquals(new Long(1800), config.getMaxAge());
	}

	@Test
	public void customized() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setRequestURI("/customized");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(new String[] {"DELETE"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] {"http://site1.com", "http://site2.com"}, config.getAllowedOrigins().toArray());
		assertArrayEquals(new String[] {"header1", "header2"}, config.getAllowedHeaders().toArray());
		assertArrayEquals(new String[] {"header3", "header4"}, config.getExposedHeaders().toArray());
		assertEquals(new Long(123), config.getMaxAge());
		assertFalse(config.getAllowCredentials());
	}

	@Test
	public void customOriginDefinedViaValueAttribute() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setRequestURI("/customOrigin");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertEquals(Arrays.asList("http://example.com"), config.getAllowedOrigins());
		assertNull(config.getAllowCredentials());
	}

	@Test
	public void customOriginDefinedViaPlaceholder() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setRequestURI("/someOrigin");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertEquals(Arrays.asList("http://example.com"), config.getAllowedOrigins());
		assertNull(config.getAllowCredentials());
	}

	@Test
	public void bogusAllowCredentialsValue() throws Exception {
		exception.expect(IllegalStateException.class);
		exception.expectMessage(containsString("@CrossOrigin's allowCredentials"));
		exception.expectMessage(containsString("current value is [bogus]"));
		this.handlerMapping.registerHandler(new MethodLevelControllerWithBogusAllowCredentialsValue());
	}

	@Test
	public void classLevel() throws Exception {
		this.handlerMapping.registerHandler(new ClassLevelController());

		this.request.setRequestURI("/foo");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(new String[] {"GET"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] {"*"}, config.getAllowedOrigins().toArray());
		assertFalse(config.getAllowCredentials());

		this.request.setRequestURI("/bar");
		chain = this.handlerMapping.getHandler(request);
		config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(new String[] {"GET"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] {"*"}, config.getAllowedOrigins().toArray());
		assertFalse(config.getAllowCredentials());

		this.request.setRequestURI("/baz");
		chain = this.handlerMapping.getHandler(request);
		config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(new String[] {"GET"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] {"*"}, config.getAllowedOrigins().toArray());
		assertTrue(config.getAllowCredentials());
	}

	@Test // SPR-13468
	public void classLevelComposedAnnotation() throws Exception {
		this.handlerMapping.registerHandler(new ClassLevelMappingWithComposedAnnotation());

		this.request.setRequestURI("/foo");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(new String[] {"GET"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] {"http://foo.com"}, config.getAllowedOrigins().toArray());
		assertTrue(config.getAllowCredentials());
	}

	@Test // SPR-13468
	public void methodLevelComposedAnnotation() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelMappingWithComposedAnnotation());

		this.request.setRequestURI("/foo");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(new String[] {"GET"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] {"http://foo.com"}, config.getAllowedOrigins().toArray());
		assertTrue(config.getAllowCredentials());
	}

	@Test
	public void preFlightRequest() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setMethod("OPTIONS");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.setRequestURI("/default");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertNotNull(config);
		assertArrayEquals(new String[] {"GET"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] {"*"}, config.getAllowedOrigins().toArray());
		assertNull(config.getAllowCredentials());
		assertArrayEquals(new String[] {"*"}, config.getAllowedHeaders().toArray());
		assertTrue(CollectionUtils.isEmpty(config.getExposedHeaders()));
		assertEquals(new Long(1800), config.getMaxAge());
	}

	@Test
	public void ambiguousHeaderPreFlightRequest() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setMethod("OPTIONS");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1");
		this.request.setRequestURI("/ambiguous-header");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertNotNull(config);
		assertArrayEquals(new String[] {"*"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] {"*"}, config.getAllowedOrigins().toArray());
		assertArrayEquals(new String[] {"*"}, config.getAllowedHeaders().toArray());
		assertTrue(config.getAllowCredentials());
		assertTrue(CollectionUtils.isEmpty(config.getExposedHeaders()));
		assertNull(config.getMaxAge());
	}

	@Test
	public void ambiguousProducesPreFlightRequest() throws Exception {
		this.handlerMapping.registerHandler(new MethodLevelController());
		this.request.setMethod("OPTIONS");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.request.setRequestURI("/ambiguous-produces");
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertNotNull(config);
		assertArrayEquals(new String[] {"*"}, config.getAllowedMethods().toArray());
		assertArrayEquals(new String[] {"*"}, config.getAllowedOrigins().toArray());
		assertArrayEquals(new String[] {"*"}, config.getAllowedHeaders().toArray());
		assertTrue(config.getAllowCredentials());
		assertTrue(CollectionUtils.isEmpty(config.getExposedHeaders()));
		assertNull(config.getMaxAge());
	}

	@Test
	public void preFlightRequestWithoutRequestMethodHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/default");
		request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		assertNull(this.handlerMapping.getHandler(request));
	}


	private CorsConfiguration getCorsConfiguration(HandlerExecutionChain chain, boolean isPreFlightRequest) {
		if (isPreFlightRequest) {
			Object handler = chain.getHandler();
			assertTrue(handler.getClass().getSimpleName().equals("PreFlightHandler"));
			DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
			return (CorsConfiguration)accessor.getPropertyValue("config");
		}
		else {
			HandlerInterceptor[] interceptors = chain.getInterceptors();
			if (interceptors != null) {
				for (HandlerInterceptor interceptor : interceptors) {
					if (interceptor.getClass().getSimpleName().equals("CorsInterceptor")) {
						DirectFieldAccessor accessor = new DirectFieldAccessor(interceptor);
						return (CorsConfiguration) accessor.getPropertyValue("config");
					}
				}
			}
		}
		return null;
	}


	@Controller
	private static class MethodLevelController {

		@GetMapping("/no")
		public void noAnnotation() {
		}

		@PostMapping("/no")
		public void noAnnotationPost() {
		}

		@CrossOrigin
		@GetMapping(path = "/default")
		public void defaultAnnotation() {
		}

		@CrossOrigin
		@GetMapping(path = "/default", params = "q")
		public void defaultAnnotationWithParams() {
		}

		@CrossOrigin
		@GetMapping(path = "/ambiguous-header", headers = "header1=a")
		public void ambigousHeader1a() {
		}

		@CrossOrigin
		@GetMapping(path = "/ambiguous-header", headers = "header1=b")
		public void ambigousHeader1b() {
		}

		@CrossOrigin
		@GetMapping(path = "/ambiguous-produces", produces = "application/xml")
		public String ambigousProducesXml() {
			return "<a></a>";
		}

		@CrossOrigin
		@GetMapping(path = "/ambiguous-produces", produces = "application/json")
		public String ambigousProducesJson() {
			return "{}";
		}

		@CrossOrigin(origins = { "http://site1.com", "http://site2.com" },
				allowedHeaders = { "header1", "header2" },
				exposedHeaders = { "header3", "header4" },
				methods = RequestMethod.DELETE,
				maxAge = 123,
				allowCredentials = "false")
		@RequestMapping(path = "/customized", method = { RequestMethod.GET, RequestMethod.POST })
		public void customized() {
		}

		@CrossOrigin("http://example.com")
		@RequestMapping("/customOrigin")
		public void customOriginDefinedViaValueAttribute() {
		}

		@CrossOrigin("${myOrigin}")
		@RequestMapping("/someOrigin")
		public void customOriginDefinedViaPlaceholder() {
		}
	}


	@Controller
	private static class MethodLevelControllerWithBogusAllowCredentialsValue {

		@CrossOrigin(allowCredentials = "bogus")
		@RequestMapping("/bogus")
		public void bogusAllowCredentialsValue() {
		}
	}


	@Controller
	@CrossOrigin(allowCredentials = "false")
	private static class ClassLevelController {

		@RequestMapping(path = "/foo", method = RequestMethod.GET)
		public void foo() {
		}

		@CrossOrigin
		@RequestMapping(path = "/bar", method = RequestMethod.GET)
		public void bar() {
		}

		@CrossOrigin(allowCredentials = "true")
		@RequestMapping(path = "/baz", method = RequestMethod.GET)
		public void baz() {
		}

	}


	@Target({ElementType.METHOD, ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@CrossOrigin
	private @interface ComposedCrossOrigin {

		String[] origins() default {};

		String allowCredentials() default "";
	}


	@Controller
	@ComposedCrossOrigin(origins = "http://foo.com", allowCredentials = "true")
	private static class ClassLevelMappingWithComposedAnnotation {

		@RequestMapping(path = "/foo", method = RequestMethod.GET)
		public void foo() {
		}
	}


	@Controller
	private static class MethodLevelMappingWithComposedAnnotation {

		@RequestMapping(path = "/foo", method = RequestMethod.GET)
		@ComposedCrossOrigin(origins = "http://foo.com", allowCredentials = "true")
		public void foo() {
		}
	}


	private static class TestRequestMappingInfoHandlerMapping extends RequestMappingHandlerMapping {

		public void registerHandler(Object handler) {
			super.detectHandlerMethods(handler);
		}

		@Override
		protected boolean isHandler(Class<?> beanType) {
			return AnnotationUtils.findAnnotation(beanType, Controller.class) != null;
		}

		@Override
		protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
			RequestMapping annotation = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
			if (annotation != null) {
				return new RequestMappingInfo(
						new PatternsRequestCondition(annotation.value(), getUrlPathHelper(), getPathMatcher(), true, true),
						new RequestMethodsRequestCondition(annotation.method()),
						new ParamsRequestCondition(annotation.params()),
						new HeadersRequestCondition(annotation.headers()),
						new ConsumesRequestCondition(annotation.consumes(), annotation.headers()),
						new ProducesRequestCondition(annotation.produces(), annotation.headers()), null);
			}
			else {
				return null;
			}
		}
	}

}
