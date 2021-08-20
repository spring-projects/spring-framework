/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.mvc.method;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.handler.PathPatternsParameterizedTest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test fixture with {@link RequestMappingInfoHandlerMapping}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
class RequestMappingInfoHandlerMappingTests {

	@SuppressWarnings("unused")
	static Stream<?> pathPatternsArguments() {
		TestController controller = new TestController();

		TestRequestMappingInfoHandlerMapping mapping1 = new TestRequestMappingInfoHandlerMapping();
		mapping1.setPatternParser(new PathPatternParser());

		TestRequestMappingInfoHandlerMapping mapping2 = new TestRequestMappingInfoHandlerMapping();
		mapping2.setRemoveSemicolonContent(false);

		return Stream.of(mapping1, mapping2).peek(mapping -> {
			mapping.setApplicationContext(new StaticWebApplicationContext());
			mapping.registerHandler(controller);
			mapping.afterPropertiesSet();
		});
	}


	private HandlerMethod fooMethod;

	private HandlerMethod fooParamMethod;

	private HandlerMethod barMethod;

	private HandlerMethod emptyMethod;


	@BeforeEach
	void setup() throws Exception {
		TestController controller = new TestController();
		this.fooMethod = new HandlerMethod(controller, "foo");
		this.fooParamMethod = new HandlerMethod(controller, "fooParam");
		this.barMethod = new HandlerMethod(controller, "bar");
		this.emptyMethod = new HandlerMethod(controller, "empty");
	}


	@PathPatternsParameterizedTest
	void getMappingPathPatterns(TestRequestMappingInfoHandlerMapping mapping) {
		String[] patterns = {"/foo/*", "/foo", "/bar/*", "/bar"};
		RequestMappingInfo info = mapping.createInfo(patterns);
		Set<String> actual = mapping.getMappingPathPatterns(info);

		assertThat(actual).isEqualTo(new HashSet<>(Arrays.asList(patterns)));
	}

	@PathPatternsParameterizedTest
	void getHandlerDirectMatch(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		HandlerMethod handlerMethod = getHandler(mapping, request);

		assertThat(handlerMethod.getMethod()).isEqualTo(this.fooMethod.getMethod());
	}

	@PathPatternsParameterizedTest
	void getHandlerGlobMatch(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bar");
		HandlerMethod handlerMethod = getHandler(mapping, request);
		assertThat(handlerMethod.getMethod()).isEqualTo(this.barMethod.getMethod());
	}

	@PathPatternsParameterizedTest
	void getHandlerEmptyPathMatch(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		HandlerMethod handlerMethod = getHandler(mapping, request);

		assertThat(handlerMethod.getMethod()).isEqualTo(this.emptyMethod.getMethod());

		request = new MockHttpServletRequest("GET", "/");
		handlerMethod = getHandler(mapping, request);

		assertThat(handlerMethod.getMethod()).isEqualTo(this.emptyMethod.getMethod());
	}

	@PathPatternsParameterizedTest
	void getHandlerBestMatch(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("p", "anything");
		HandlerMethod handlerMethod = getHandler(mapping, request);

		assertThat(handlerMethod.getMethod()).isEqualTo(this.fooParamMethod.getMethod());
	}

	@PathPatternsParameterizedTest
	void getHandlerRequestMethodNotAllowed(TestRequestMappingInfoHandlerMapping mapping) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/bar");
		assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class)
				.isThrownBy(() -> mapping.getHandler(request))
				.satisfies(ex -> assertThat(ex.getSupportedMethods()).containsExactly("GET", "HEAD"));
	}

	@PathPatternsParameterizedTest // SPR-9603
	void getHandlerRequestMethodMatchFalsePositive(TestRequestMappingInfoHandlerMapping mapping) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");
		request.addHeader("Accept", "application/xml");
		mapping.registerHandler(new UserController());
		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
				.isThrownBy(() -> mapping.getHandler(request));
	}

	@PathPatternsParameterizedTest // SPR-8462
	void getHandlerMediaTypeNotSupported(TestRequestMappingInfoHandlerMapping mapping) {
		testHttpMediaTypeNotSupportedException(mapping, "/person/1");
		testHttpMediaTypeNotSupportedException(mapping, "/person/1/");
		testHttpMediaTypeNotSupportedException(mapping, "/person/1.json");
	}

	@PathPatternsParameterizedTest
	void getHandlerHttpOptions(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
		testHttpOptions(mapping, "/foo", "GET,HEAD,OPTIONS", null);
		testHttpOptions(mapping, "/person/1", "PUT,OPTIONS", null);
		testHttpOptions(mapping, "/persons", "GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS", null);
		testHttpOptions(mapping, "/something", "PUT,POST", null);
		testHttpOptions(mapping, "/qux", "PATCH,GET,HEAD,OPTIONS", new MediaType("foo", "bar"));
	}

	@PathPatternsParameterizedTest
	void getHandlerTestInvalidContentType(TestRequestMappingInfoHandlerMapping mapping) {
		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/person/1");
		request.setContentType("bogus");
		assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class)
				.isThrownBy(() -> mapping.getHandler(request))
				.withMessage("Invalid mime type \"bogus\": does not contain '/'");
	}

	@PathPatternsParameterizedTest // SPR-8462
	void getHandlerMediaTypeNotAccepted(TestRequestMappingInfoHandlerMapping mapping) {
		testHttpMediaTypeNotAcceptableException(mapping, "/persons");
		testHttpMediaTypeNotAcceptableException(mapping, "/persons/");
		if (mapping.getPatternParser() == null) {
			testHttpMediaTypeNotAcceptableException(mapping, "/persons.json");
		}
	}

	@PathPatternsParameterizedTest // SPR-12854
	void getHandlerUnsatisfiedServletRequestParameterException(TestRequestMappingInfoHandlerMapping mapping) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/params");
		assertThatExceptionOfType(UnsatisfiedServletRequestParameterException.class)
				.isThrownBy(() -> mapping.getHandler(request))
				.satisfies(ex -> assertThat(ex.getParamConditionGroups().stream().map(group -> group[0]))
						.containsExactlyInAnyOrder("foo=bar", "bar=baz"));
	}

	@PathPatternsParameterizedTest
	void getHandlerProducibleMediaTypesAttribute(TestRequestMappingInfoHandlerMapping mapping) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/content");
		request.addHeader("Accept", "application/xml");
		mapping.getHandler(request);

		String name = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;
		assertThat(request.getAttribute(name)).isEqualTo(Collections.singleton(MediaType.APPLICATION_XML));

		request = new MockHttpServletRequest("GET", "/content");
		request.addHeader("Accept", "application/json");
		mapping.getHandler(request);

		assertThat(request.getAttribute(name)).as("Negated expression shouldn't be listed as producible type").isNull();
	}

	@Test
	void getHandlerMappedInterceptors() throws Exception {
		String path = "/foo";
		HandlerInterceptor interceptor = new HandlerInterceptor() {};
		MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] {path}, interceptor);

		TestRequestMappingInfoHandlerMapping mapping = new TestRequestMappingInfoHandlerMapping();
		mapping.registerHandler(new TestController());
		mapping.setInterceptors(mappedInterceptor);
		mapping.setApplicationContext(new StaticWebApplicationContext());

		HandlerExecutionChain chain = mapping.getHandler(new MockHttpServletRequest("GET", path));
		assertThat(chain).isNotNull();
		assertThat(chain.getInterceptorList().get(0)).isSameAs(interceptor);

		chain = mapping.getHandler(new MockHttpServletRequest("GET", "/invalid"));
		assertThat(chain).isNull();
	}

	@SuppressWarnings("unchecked")
	@PathPatternsParameterizedTest
	void handleMatchUriTemplateVariables(TestRequestMappingInfoHandlerMapping mapping) {
		RequestMappingInfo key = RequestMappingInfo.paths("/{path1}/{path2}").build();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/1/2");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);
		mapping.handleMatch(key, lookupPath, request);

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Map<String, String> uriVariables = (Map<String, String>) request.getAttribute(name);

		assertThat(uriVariables).isNotNull();
		assertThat(uriVariables.get("path1")).isEqualTo("1");
		assertThat(uriVariables.get("path2")).isEqualTo("2");
	}

	@SuppressWarnings("unchecked")
	@PathPatternsParameterizedTest // SPR-9098
	void handleMatchUriTemplateVariablesDecode(TestRequestMappingInfoHandlerMapping mapping) {
		RequestMappingInfo key = RequestMappingInfo.paths("/{group}/{identifier}").build();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/group/a%2Fb");

		UrlPathHelper pathHelper = new UrlPathHelper();
		pathHelper.setUrlDecode(false);
		String lookupPath = pathHelper.getLookupPathForRequest(request);

		mapping.setUrlPathHelper(pathHelper);
		mapping.handleMatch(key, lookupPath, request);

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Map<String, String> uriVariables = (Map<String, String>) request.getAttribute(name);

		assertThat(uriVariables).isNotNull();
		assertThat(uriVariables.get("group")).isEqualTo("group");
		assertThat(uriVariables.get("identifier")).isEqualTo("a/b");
	}

	@PathPatternsParameterizedTest
	void handleMatchBestMatchingPatternAttribute(TestRequestMappingInfoHandlerMapping mapping) {
		RequestMappingInfo key = RequestMappingInfo.paths("/{path1}/2", "/**").build();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/1/2");
		mapping.handleMatch(key, "/1/2", request);

		assertThat(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).isEqualTo("/{path1}/2");
	}

	@PathPatternsParameterizedTest // gh-22543
	void handleMatchBestMatchingPatternAttributeNoPatternsDefined(TestRequestMappingInfoHandlerMapping mapping) {
		String path = "";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		mapping.handleMatch(RequestMappingInfo.paths().build(), path, request);
		assertThat(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).isEqualTo(path);
	}

	@PathPatternsParameterizedTest
	void handleMatchMatrixVariables(TestRequestMappingInfoHandlerMapping mapping) {
		MockHttpServletRequest request;
		MultiValueMap<String, String> matrixVariables;
		Map<String, String> uriVariables;

		// URI var parsed into path variable + matrix params..
		request = new MockHttpServletRequest("GET", "/cars;colors=red,blue,green;year=2012");
		handleMatch(mapping, request, "/{cars}", request.getRequestURI());

		matrixVariables = getMatrixVariables(request, "cars");
		uriVariables = getUriTemplateVariables(request);

		assertThat(matrixVariables).isNotNull();
		assertThat(matrixVariables.get("colors")).isEqualTo(Arrays.asList("red", "blue", "green"));
		assertThat(matrixVariables.getFirst("year")).isEqualTo("2012");
		assertThat(uriVariables.get("cars")).isEqualTo("cars");

		// URI var with regex for path variable, and URI var for matrix params..
		request = new MockHttpServletRequest("GET", "/cars;colors=red,blue,green;year=2012");
		handleMatch(mapping, request, "/{cars:[^;]+}{params}", request.getRequestURI());

		matrixVariables = getMatrixVariables(request, "params");
		uriVariables = getUriTemplateVariables(request);

		assertThat(matrixVariables).isNotNull();
		assertThat(matrixVariables.get("colors")).isEqualTo(Arrays.asList("red", "blue", "green"));
		assertThat(matrixVariables.getFirst("year")).isEqualTo("2012");
		assertThat(uriVariables.get("cars")).isEqualTo("cars");
		if (mapping.getPatternParser() == null) {
			assertThat(uriVariables.get("params")).isEqualTo(";colors=red,blue,green;year=2012");
		}

		// URI var with regex for path variable, and (empty) URI var for matrix params..
		request = new MockHttpServletRequest("GET", "/cars");
		handleMatch(mapping, request, "/{cars:[^;]+}{params}", request.getRequestURI());

		matrixVariables = getMatrixVariables(request, "params");
		uriVariables = getUriTemplateVariables(request);

		assertThat(matrixVariables).isNull();
		assertThat(uriVariables.get("cars")).isEqualTo("cars");
		assertThat(uriVariables.get("params")).isEqualTo("");

		// SPR-11897
		request = new MockHttpServletRequest("GET", "/a=42;b=c");
		handleMatch(mapping, request, "/{foo}", request.getRequestURI());

		matrixVariables = getMatrixVariables(request, "foo");
		uriVariables = getUriTemplateVariables(request);

		assertThat(matrixVariables).isNotNull();
		if (mapping.getPatternParser() != null) {
			assertThat(matrixVariables.size()).isEqualTo(1);
			assertThat(matrixVariables.getFirst("b")).isEqualTo("c");
			assertThat(uriVariables.get("foo")).isEqualTo("a=42");
		}
		else {
			assertThat(matrixVariables.size()).isEqualTo(2);
			assertThat(matrixVariables.getFirst("a")).isEqualTo("42");
			assertThat(matrixVariables.getFirst("b")).isEqualTo("c");
			assertThat(uriVariables.get("foo")).isEqualTo("a=42");
		}
	}

	@PathPatternsParameterizedTest // SPR-10140, SPR-16867
	void handleMatchMatrixVariablesDecoding(TestRequestMappingInfoHandlerMapping mapping) {

		if (mapping.getPatternParser() == null) {
			UrlPathHelper urlPathHelper = new UrlPathHelper();
			urlPathHelper.setUrlDecode(false);
			urlPathHelper.setRemoveSemicolonContent(false);
			mapping.setUrlPathHelper(urlPathHelper);
		}

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/cars;mvar=a%2Fb");
		handleMatch(mapping, request, "/{cars}", request.getRequestURI());

		MultiValueMap<String, String> matrixVariables = getMatrixVariables(request, "cars");
		Map<String, String> uriVariables = getUriTemplateVariables(request);

		assertThat(matrixVariables).isNotNull();
		assertThat(matrixVariables.get("mvar")).isEqualTo(Collections.singletonList("a/b"));
		assertThat(uriVariables.get("cars")).isEqualTo("cars");
	}

	private HandlerMethod getHandler(
			TestRequestMappingInfoHandlerMapping mapping, MockHttpServletRequest request) throws Exception {

		HandlerExecutionChain chain = mapping.getHandler(request);
		assertThat(chain).isNotNull();
		return (HandlerMethod) chain.getHandler();
	}

	private void testHttpMediaTypeNotSupportedException(TestRequestMappingInfoHandlerMapping mapping, String url) {
		MockHttpServletRequest request = new MockHttpServletRequest("PUT", url);
		request.setContentType("application/json");
		assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class)
				.isThrownBy(() -> mapping.getHandler(request))
				.satisfies(ex -> assertThat(ex.getSupportedMediaTypes()).containsExactly(MediaType.APPLICATION_XML));
	}

	private void testHttpOptions(TestRequestMappingInfoHandlerMapping mapping, String requestURI,
			String allowHeader, @Nullable MediaType acceptPatch) throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", requestURI);
		HandlerMethod handlerMethod = getHandler(mapping, request);

		ServletWebRequest webRequest = new ServletWebRequest(request);
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		Object result = new InvocableHandlerMethod(handlerMethod).invokeForRequest(webRequest, mavContainer);

		assertThat(result).isNotNull();
		assertThat(result.getClass()).isEqualTo(HttpHeaders.class);
		HttpHeaders headers = (HttpHeaders) result;
		Set<HttpMethod> allowedMethods = Arrays.stream(allowHeader.split(","))
				.map(HttpMethod::valueOf)
				.collect(Collectors.toSet());
		assertThat(headers.getAllow()).hasSameElementsAs(allowedMethods);

		if (acceptPatch != null && headers.getAllow().contains(HttpMethod.PATCH) ) {
			assertThat(headers.getAcceptPatch()).containsExactly(acceptPatch);
		}
	}

	private void testHttpMediaTypeNotAcceptableException(TestRequestMappingInfoHandlerMapping mapping, String url) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", url);
		request.addHeader("Accept", "application/json");
		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class).isThrownBy(() ->
				mapping.getHandler(request))
			.satisfies(ex -> assertThat(ex.getSupportedMediaTypes()).containsExactly(MediaType.APPLICATION_XML));
	}

	private void handleMatch(TestRequestMappingInfoHandlerMapping mapping,
			MockHttpServletRequest request, String pattern, String lookupPath) {

		if (mapping.getPatternParser() != null) {
			ServletRequestPathUtils.parseAndCache(request);
		}

		mapping.handleMatch(mapping.createInfo(pattern), lookupPath, request);
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


	@SuppressWarnings("unused")
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

		@RequestMapping("")
		public void empty() {
		}

		@RequestMapping(value = "/person/{id}", method = RequestMethod.PUT, consumes="application/xml")
		public void consumes(@RequestBody String text) {
		}

		@RequestMapping(value = "/persons", produces="application/xml")
		public String produces() {
			return "";
		}

		@RequestMapping(value = "/params", params="foo=bar")
		public String param() {
			return "";
		}

		@RequestMapping(value = "/params", params="bar=baz")
		public String param2() {
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

		@RequestMapping(value = "/something", method = RequestMethod.OPTIONS)
		public HttpHeaders fooOptions() {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Allow", "PUT,POST");
			return headers;
		}

		@RequestMapping(value = "/qux", method = RequestMethod.GET, produces = "application/xml")
		public String getBaz() {
			return "";
		}

		@RequestMapping(value = "/qux", method = RequestMethod.PATCH, consumes = "foo/bar")
		public void patchBaz(String value) {
		}
	}


	@SuppressWarnings("unused")
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


		void registerHandler(Object handler) {
			super.detectHandlerMethods(handler);
		}

		@Override
		protected boolean isHandler(Class<?> beanType) {
			return AnnotationUtils.findAnnotation(beanType, RequestMapping.class) != null;
		}

		@Override
		protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
			RequestMapping annot = AnnotationUtils.findAnnotation(method, RequestMapping.class);
			if (annot != null) {
				return RequestMappingInfo.paths(annot.value())
						.methods(annot.method())
						.params(annot.params())
						.headers(annot.headers())
						.consumes(annot.consumes())
						.produces(annot.produces())
						.options(getBuilderConfig())
						.build();
			}
			else {
				return null;
			}
		}

		@SuppressWarnings("deprecation")
		private RequestMappingInfo.BuilderConfiguration getBuilderConfig() {
			RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();
			if (getPatternParser() != null) {
				config.setPatternParser(getPatternParser());
			}
			else {
				config.setSuffixPatternMatch(true);
				config.setRegisteredSuffixPatternMatch(true);
				config.setPathMatcher(getPathMatcher());
			}
			return config;
		}

		RequestMappingInfo createInfo(String... patterns) {
			return RequestMappingInfo.paths(patterns).options(getBuilderConfig()).build();
		}

		@Override
		protected String initLookupPath(HttpServletRequest request) {
			// At runtime this is done by the DispatcherServlet
			if (getPatternParser() != null) {
				RequestPath requestPath = ServletRequestPathUtils.parseAndCache(request);
				return requestPath.pathWithinApplication().value();
			}
			return super.initLookupPath(request);
		}
	}

}
