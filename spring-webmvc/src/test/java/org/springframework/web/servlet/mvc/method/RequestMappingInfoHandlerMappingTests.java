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

package org.springframework.web.servlet.mvc.method;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.util.UrlPathHelper;

/**
 * Test fixture with {@link RequestMappingInfoHandlerMapping}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoHandlerMappingTests {

	private TestRequestMappingInfoHandlerMapping handlerMapping;

	private HandlerMethod fooMethod;

	private HandlerMethod fooParamMethod;

	private HandlerMethod barMethod;

	private HandlerMethod emptyMethod;

	@Before
	public void setUp() throws Exception {
		TestController testController = new TestController();

		this.fooMethod = new HandlerMethod(testController, "foo");
		this.fooParamMethod = new HandlerMethod(testController, "fooParam");
		this.barMethod = new HandlerMethod(testController, "bar");
		this.emptyMethod = new HandlerMethod(testController, "empty");

		this.handlerMapping = new TestRequestMappingInfoHandlerMapping();
		this.handlerMapping.registerHandler(testController);
		this.handlerMapping.setRemoveSemicolonContent(false);
	}

	@Test
	public void getMappingPathPatterns() throws Exception {
		RequestMappingInfo info = new RequestMappingInfo(
				new PatternsRequestCondition("/foo/*", "/foo", "/bar/*", "/bar"), null, null, null, null, null, null);
		Set<String> paths = this.handlerMapping.getMappingPathPatterns(info);
		HashSet<String> expected = new HashSet<String>(Arrays.asList("/foo/*", "/foo", "/bar/*", "/bar"));

		assertEquals(expected, paths);
	}

	@Test
	public void directMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(request).getHandler();
		assertEquals(this.fooMethod.getMethod(), hm.getMethod());
	}

	@Test
	public void globMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bar");
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(request).getHandler();
		assertEquals(this.barMethod.getMethod(), hm.getMethod());
	}

	@Test
	public void emptyPathMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(request).getHandler();
		assertEquals(this.emptyMethod.getMethod(), hm.getMethod());

		request = new MockHttpServletRequest("GET", "/");
		hm = (HandlerMethod) this.handlerMapping.getHandler(request).getHandler();
		assertEquals(this.emptyMethod.getMethod(), hm.getMethod());
	}

	@Test
	public void bestMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("p", "anything");
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(request).getHandler();
		assertEquals(this.fooParamMethod.getMethod(), hm.getMethod());
	}

	@Test
	public void requestMethodNotAllowed() throws Exception {
		try {
			MockHttpServletRequest request = new MockHttpServletRequest("POST", "/bar");
			this.handlerMapping.getHandler(request);
			fail("HttpRequestMethodNotSupportedException expected");
		}
		catch (HttpRequestMethodNotSupportedException ex) {
			assertArrayEquals("Invalid supported methods", new String[]{"GET", "HEAD"}, ex.getSupportedMethods());
		}
	}

	// SPR-9603

	@Test(expected=HttpMediaTypeNotAcceptableException.class)
	public void requestMethodMatchFalsePositive() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");
		request.addHeader("Accept", "application/xml");

		this.handlerMapping.registerHandler(new UserController());
		this.handlerMapping.getHandler(request);
	}

	@Test
	public void mediaTypeNotSupported() throws Exception {
		testMediaTypeNotSupported("/person/1");

		// SPR-8462
		testMediaTypeNotSupported("/person/1/");
		testMediaTypeNotSupported("/person/1.json");
	}

	private void testMediaTypeNotSupported(String url) throws Exception {
		try {
			MockHttpServletRequest request = new MockHttpServletRequest("PUT", url);
			request.setContentType("application/json");
			this.handlerMapping.getHandler(request);
			fail("HttpMediaTypeNotSupportedException expected");
		}
		catch (HttpMediaTypeNotSupportedException ex) {
			assertEquals("Invalid supported consumable media types",
					Arrays.asList(new MediaType("application", "xml")), ex.getSupportedMediaTypes());
		}
	}

	@Test
	public void mediaTypeNotAccepted() throws Exception {
		testMediaTypeNotAccepted("/persons");

		// SPR-8462
		testMediaTypeNotAccepted("/persons/");
		testMediaTypeNotAccepted("/persons.json");
	}

	private void testMediaTypeNotAccepted(String url) throws Exception {
		try {
			MockHttpServletRequest request = new MockHttpServletRequest("GET", url);
			request.addHeader("Accept", "application/json");
			this.handlerMapping.getHandler(request);
			fail("HttpMediaTypeNotAcceptableException expected");
		}
		catch (HttpMediaTypeNotAcceptableException ex) {
			assertEquals("Invalid supported producible media types",
					Arrays.asList(new MediaType("application", "xml")), ex.getSupportedMediaTypes());
		}
	}

	@Test
	public void uriTemplateVariables() {
		PatternsRequestCondition patterns = new PatternsRequestCondition("/{path1}/{path2}");
		RequestMappingInfo key = new RequestMappingInfo(patterns, null, null, null, null, null, null);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/1/2");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);
		this.handlerMapping.handleMatch(key, lookupPath, request);

		@SuppressWarnings("unchecked")
		Map<String, String> uriVariables =
			(Map<String, String>) request.getAttribute(
					HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

		assertNotNull(uriVariables);
		assertEquals("1", uriVariables.get("path1"));
		assertEquals("2", uriVariables.get("path2"));
	}

	// SPR-9098

	@Test
	public void uriTemplateVariablesDecode() {
		PatternsRequestCondition patterns = new PatternsRequestCondition("/{group}/{identifier}");
		RequestMappingInfo key = new RequestMappingInfo(patterns, null, null, null, null, null, null);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/group/a%2Fb");

		UrlPathHelper pathHelper = new UrlPathHelper();
		pathHelper.setUrlDecode(false);
		String lookupPath = pathHelper.getLookupPathForRequest(request);

		this.handlerMapping.setUrlPathHelper(pathHelper);
		this.handlerMapping.handleMatch(key, lookupPath, request);

		@SuppressWarnings("unchecked")
		Map<String, String> uriVariables =
			(Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

		assertNotNull(uriVariables);
		assertEquals("group", uriVariables.get("group"));
		assertEquals("a/b", uriVariables.get("identifier"));
	}

	@Test
	public void bestMatchingPatternAttribute() {
		PatternsRequestCondition patterns = new PatternsRequestCondition("/{path1}/2", "/**");
		RequestMappingInfo key = new RequestMappingInfo(patterns, null, null, null, null, null, null);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/1/2");

		this.handlerMapping.handleMatch(key, "/1/2", request);

		assertEquals("/{path1}/2", request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
	}

	@Test
	public void bestMatchingPatternAttributeNoPatternsDefined() {
		PatternsRequestCondition patterns = new PatternsRequestCondition();
		RequestMappingInfo key = new RequestMappingInfo(patterns, null, null, null, null, null, null);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/1/2");

		this.handlerMapping.handleMatch(key, "/1/2", request);

		assertEquals("/1/2", request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
	}

	@Test
	public void producibleMediaTypesAttribute() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/content");
		request.addHeader("Accept", "application/xml");
		this.handlerMapping.getHandler(request);

		assertEquals(Collections.singleton(MediaType.APPLICATION_XML),
				request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE));

		request = new MockHttpServletRequest("GET", "/content");
		request.addHeader("Accept", "application/json");
		this.handlerMapping.getHandler(request);

		assertNull("Negated expression should not be listed as a producible type",
				request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE));
	}

	@Test
	public void mappedInterceptors() throws Exception {
		String path = "/foo";
		HandlerInterceptor interceptor = new HandlerInterceptorAdapter() {};
		MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] {path}, interceptor);

		TestRequestMappingInfoHandlerMapping hm = new TestRequestMappingInfoHandlerMapping();
		hm.registerHandler(new TestController());
		hm.setInterceptors(new Object[] { mappedInterceptor });
		hm.setApplicationContext(new StaticWebApplicationContext());

		HandlerExecutionChain chain = hm.getHandler(new MockHttpServletRequest("GET", path));
		assertNotNull(chain);
		assertNotNull(chain.getInterceptors());
		assertSame(interceptor, chain.getInterceptors()[0]);

		chain = hm.getHandler(new MockHttpServletRequest("GET", "/invalid"));
		assertNull(chain);
	}

	@Test
	public void matrixVariables() {

		MockHttpServletRequest request;
		MultiValueMap<String, String> matrixVariables;
		Map<String, String> uriVariables;

		String lookupPath = "/cars;colors=red,blue,green;year=2012";

		// Pattern "/{cars}" : matrix variables stripped from "cars" variable

		request = new MockHttpServletRequest();
		testHandleMatch(request, "/{cars}", lookupPath);

		matrixVariables = getMatrixVariables(request, "cars");
		assertNotNull(matrixVariables);
		assertEquals(Arrays.asList("red", "blue", "green"), matrixVariables.get("colors"));
		assertEquals("2012", matrixVariables.getFirst("year"));

		uriVariables = getUriTemplateVariables(request);
		assertEquals("cars", uriVariables.get("cars"));

		// Pattern "/{cars:[^;]+}{params}" : "cars" and "params" variables unchanged

		request = new MockHttpServletRequest();
		testHandleMatch(request, "/{cars:[^;]+}{params}", lookupPath);

		matrixVariables = getMatrixVariables(request, "params");
		assertNotNull(matrixVariables);
		assertEquals(Arrays.asList("red", "blue", "green"), matrixVariables.get("colors"));
		assertEquals("2012", matrixVariables.getFirst("year"));

		uriVariables = getUriTemplateVariables(request);
		assertEquals("cars", uriVariables.get("cars"));
		assertEquals(";colors=red,blue,green;year=2012", uriVariables.get("params"));

		// matrix variables not present : "params" variable is empty

		request = new MockHttpServletRequest();
		testHandleMatch(request, "/{cars:[^;]+}{params}", "/cars");

		matrixVariables = getMatrixVariables(request, "params");
		assertNull(matrixVariables);

		uriVariables = getUriTemplateVariables(request);
		assertEquals("cars", uriVariables.get("cars"));
		assertEquals("", uriVariables.get("params"));
	}

	private void testHandleMatch(MockHttpServletRequest request, String pattern, String lookupPath) {
		PatternsRequestCondition patterns = new PatternsRequestCondition(pattern);
		RequestMappingInfo info = new RequestMappingInfo(patterns, null, null, null, null, null, null);
		this.handlerMapping.handleMatch(info, lookupPath, request);
	}

	@SuppressWarnings("unchecked")
	private MultiValueMap<String, String> getMatrixVariables(HttpServletRequest request, String uriVarName) {
		String attrName = HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE;
		return ((Map<String, MultiValueMap<String, String>>) request.getAttribute(attrName)).get(uriVarName);
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getUriTemplateVariables(HttpServletRequest request) {
		String attrName = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		return (Map<String, String>) request.getAttribute(attrName);
	}


	@Controller
	private static class TestController {

		@RequestMapping(value = "/foo", method = RequestMethod.GET)
		public void foo() {
		}

		@RequestMapping(value = "/foo", method = RequestMethod.GET, params="p")
		public void fooParam() {
		}

		@RequestMapping(value = "/ba*", method = { RequestMethod.GET, RequestMethod.HEAD })
		public void bar() {
		}

		@RequestMapping(value = "")
		public void empty() {
		}

		@RequestMapping(value = "/person/{id}", method = RequestMethod.PUT, consumes="application/xml")
		public void consumes(@RequestBody String text) {
		}

		@RequestMapping(value = "/persons", produces="application/xml")
		public String produces() {
			return "";
		}

		@RequestMapping(value = "/content", produces="application/xml")
		public String xmlContent() {
			return "";
		}

		@RequestMapping(value = "/content", produces="!application/xml")
		public String nonXmlContent() {
			return "";
		}
	}

	@Controller
	private static class UserController {

		@RequestMapping(value = "/users", method = RequestMethod.GET, produces = "application/json")
		public void getUser() {
		}

		@RequestMapping(value = "/users", method = RequestMethod.PUT)
		public void saveUser() {
		}
	}

	private static class TestRequestMappingInfoHandlerMapping extends RequestMappingInfoHandlerMapping {

		public void registerHandler(Object handler) {
			super.detectHandlerMethods(handler);
		}

		@Override
		protected boolean isHandler(Class<?> beanType) {
			return AnnotationUtils.findAnnotation(beanType, RequestMapping.class) != null;
		}

		@Override
		protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
			RequestMapping annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
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