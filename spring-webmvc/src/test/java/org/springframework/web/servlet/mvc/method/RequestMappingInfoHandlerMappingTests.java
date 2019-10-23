/*
 * Copyright 2002-2019 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;
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
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.HeadersRequestCondition;
import org.springframework.web.servlet.mvc.condition.ParamsRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.util.UrlPathHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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


	@BeforeEach
	public void setup() throws Exception {
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
		String[] patterns = {"/foo/*", "/foo", "/bar/*", "/bar"};
		RequestMappingInfo info = RequestMappingInfo.paths(patterns).build();
		Set<String> actual = this.handlerMapping.getMappingPathPatterns(info);

		assertThat(actual).isEqualTo(new HashSet<>(Arrays.asList(patterns)));
	}

	@Test
	public void getHandlerDirectMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		HandlerMethod handlerMethod = getHandler(request);

		assertThat(handlerMethod.getMethod()).isEqualTo(this.fooMethod.getMethod());
	}

	@Test
	public void getHandlerGlobMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/bar");
		HandlerMethod handlerMethod = getHandler(request);
		assertThat(handlerMethod.getMethod()).isEqualTo(this.barMethod.getMethod());
	}

	@Test
	public void getHandlerEmptyPathMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "");
		HandlerMethod handlerMethod = getHandler(request);

		assertThat(handlerMethod.getMethod()).isEqualTo(this.emptyMethod.getMethod());

		request = new MockHttpServletRequest("GET", "/");
		handlerMethod = getHandler(request);

		assertThat(handlerMethod.getMethod()).isEqualTo(this.emptyMethod.getMethod());
	}

	@Test
	public void getHandlerBestMatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo");
		request.setParameter("p", "anything");
		HandlerMethod handlerMethod = getHandler(request);

		assertThat(handlerMethod.getMethod()).isEqualTo(this.fooParamMethod.getMethod());
	}

	@Test
	public void getHandlerRequestMethodNotAllowed() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/bar");
		assertThatExceptionOfType(HttpRequestMethodNotSupportedException.class).isThrownBy(() ->
				this.handlerMapping.getHandler(request))
			.satisfies(ex -> assertThat(ex.getSupportedMethods()).containsExactly("GET", "HEAD"));
	}

	@Test // SPR-9603
	public void getHandlerRequestMethodMatchFalsePositive() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users");
		request.addHeader("Accept", "application/xml");
		this.handlerMapping.registerHandler(new UserController());
		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class).isThrownBy(() ->
				this.handlerMapping.getHandler(request));
	}

	@Test  // SPR-8462
	public void getHandlerMediaTypeNotSupported() throws Exception {
		testHttpMediaTypeNotSupportedException("/person/1");
		testHttpMediaTypeNotSupportedException("/person/1/");
		testHttpMediaTypeNotSupportedException("/person/1.json");
	}

	@Test
	public void getHandlerHttpOptions() throws Exception {
		testHttpOptions("/foo", "GET,HEAD,OPTIONS");
		testHttpOptions("/person/1", "PUT,OPTIONS");
		testHttpOptions("/persons", "GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS");
		testHttpOptions("/something", "PUT,POST");
	}

	@Test
	public void getHandlerTestInvalidContentType() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/person/1");
		request.setContentType("bogus");
		assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class).isThrownBy(() ->
				this.handlerMapping.getHandler(request))
			.withMessage("Invalid mime type \"bogus\": does not contain '/'");
	}

	@Test  // SPR-8462
	public void getHandlerMediaTypeNotAccepted() throws Exception {
		testHttpMediaTypeNotAcceptableException("/persons");
		testHttpMediaTypeNotAcceptableException("/persons/");
		testHttpMediaTypeNotAcceptableException("/persons.json");
	}

	@Test  // SPR-12854
	public void getHandlerUnsatisfiedServletRequestParameterException() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/params");
		assertThatExceptionOfType(UnsatisfiedServletRequestParameterException.class).isThrownBy(() ->
				this.handlerMapping.getHandler(request))
			.satisfies(ex -> assertThat(ex.getParamConditionGroups().stream().map(group -> group[0]))
					.containsExactlyInAnyOrder("foo=bar", "bar=baz"));
	}

	@Test
	public void getHandlerProducibleMediaTypesAttribute() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/content");
		request.addHeader("Accept", "application/xml");
		this.handlerMapping.getHandler(request);

		String name = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;
		assertThat(request.getAttribute(name)).isEqualTo(Collections.singleton(MediaType.APPLICATION_XML));

		request = new MockHttpServletRequest("GET", "/content");
		request.addHeader("Accept", "application/json");
		this.handlerMapping.getHandler(request);

		assertThat(request.getAttribute(name)).as("Negated expression shouldn't be listed as producible type").isNull();
	}

	@Test
	public void getHandlerMappedInterceptors() throws Exception {
		String path = "/foo";
		HandlerInterceptor interceptor = new HandlerInterceptorAdapter() {};
		MappedInterceptor mappedInterceptor = new MappedInterceptor(new String[] {path}, interceptor);

		TestRequestMappingInfoHandlerMapping mapping = new TestRequestMappingInfoHandlerMapping();
		mapping.registerHandler(new TestController());
		mapping.setInterceptors(new Object[] { mappedInterceptor });
		mapping.setApplicationContext(new StaticWebApplicationContext());

		HandlerExecutionChain chain = mapping.getHandler(new MockHttpServletRequest("GET", path));
		assertThat(chain).isNotNull();
		assertThat(chain.getInterceptors()).isNotNull();
		assertThat(chain.getInterceptors()[0]).isSameAs(interceptor);

		chain = mapping.getHandler(new MockHttpServletRequest("GET", "/invalid"));
		assertThat(chain).isNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void handleMatchUriTemplateVariables() {
		RequestMappingInfo key = RequestMappingInfo.paths("/{path1}/{path2}").build();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/1/2");
		String lookupPath = new UrlPathHelper().getLookupPathForRequest(request);
		this.handlerMapping.handleMatch(key, lookupPath, request);

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Map<String, String> uriVariables = (Map<String, String>) request.getAttribute(name);

		assertThat(uriVariables).isNotNull();
		assertThat(uriVariables.get("path1")).isEqualTo("1");
		assertThat(uriVariables.get("path2")).isEqualTo("2");
	}

	@SuppressWarnings("unchecked")
	@Test  // SPR-9098
	public void handleMatchUriTemplateVariablesDecode() {
		RequestMappingInfo key = RequestMappingInfo.paths("/{group}/{identifier}").build();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/group/a%2Fb");

		UrlPathHelper pathHelper = new UrlPathHelper();
		pathHelper.setUrlDecode(false);
		String lookupPath = pathHelper.getLookupPathForRequest(request);

		this.handlerMapping.setUrlPathHelper(pathHelper);
		this.handlerMapping.handleMatch(key, lookupPath, request);

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Map<String, String> uriVariables = (Map<String, String>) request.getAttribute(name);

		assertThat(uriVariables).isNotNull();
		assertThat(uriVariables.get("group")).isEqualTo("group");
		assertThat(uriVariables.get("identifier")).isEqualTo("a/b");
	}

	@Test
	public void handleMatchBestMatchingPatternAttribute() {
		RequestMappingInfo key = RequestMappingInfo.paths("/{path1}/2", "/**").build();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/1/2");
		this.handlerMapping.handleMatch(key, "/1/2", request);

		assertThat(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).isEqualTo("/{path1}/2");
	}

	@Test // gh-22543
	public void handleMatchBestMatchingPatternAttributeNoPatternsDefined() {
		String path = "";
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		this.handlerMapping.handleMatch(RequestMappingInfo.paths().build(), path, request);
		assertThat(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).isEqualTo(path);
	}

	@Test
	public void handleMatchMatrixVariables() {
		MockHttpServletRequest request;
		MultiValueMap<String, String> matrixVariables;
		Map<String, String> uriVariables;

		// URI var parsed into path variable + matrix params..
		request = new MockHttpServletRequest();
		handleMatch(request, "/{cars}", "/cars;colors=red,blue,green;year=2012");

		matrixVariables = getMatrixVariables(request, "cars");
		uriVariables = getUriTemplateVariables(request);

		assertThat(matrixVariables).isNotNull();
		assertThat(matrixVariables.get("colors")).isEqualTo(Arrays.asList("red", "blue", "green"));
		assertThat(matrixVariables.getFirst("year")).isEqualTo("2012");
		assertThat(uriVariables.get("cars")).isEqualTo("cars");

		// URI var with regex for path variable, and URI var for matrix params..
		request = new MockHttpServletRequest();
		handleMatch(request, "/{cars:[^;]+}{params}", "/cars;colors=red,blue,green;year=2012");

		matrixVariables = getMatrixVariables(request, "params");
		uriVariables = getUriTemplateVariables(request);

		assertThat(matrixVariables).isNotNull();
		assertThat(matrixVariables.get("colors")).isEqualTo(Arrays.asList("red", "blue", "green"));
		assertThat(matrixVariables.getFirst("year")).isEqualTo("2012");
		assertThat(uriVariables.get("cars")).isEqualTo("cars");
		assertThat(uriVariables.get("params")).isEqualTo(";colors=red,blue,green;year=2012");

		// URI var with regex for path variable, and (empty) URI var for matrix params..
		request = new MockHttpServletRequest();
		handleMatch(request, "/{cars:[^;]+}{params}", "/cars");

		matrixVariables = getMatrixVariables(request, "params");
		uriVariables = getUriTemplateVariables(request);

		assertThat(matrixVariables).isNull();
		assertThat(uriVariables.get("cars")).isEqualTo("cars");
		assertThat(uriVariables.get("params")).isEqualTo("");

		// SPR-11897
		request = new MockHttpServletRequest();
		handleMatch(request, "/{foo}", "/a=42;b=c");

		matrixVariables = getMatrixVariables(request, "foo");
		uriVariables = getUriTemplateVariables(request);

		assertThat(matrixVariables).isNotNull();
		assertThat(matrixVariables.size()).isEqualTo(2);
		assertThat(matrixVariables.getFirst("a")).isEqualTo("42");
		assertThat(matrixVariables.getFirst("b")).isEqualTo("c");
		assertThat(uriVariables.get("foo")).isEqualTo("a=42");
	}

	@Test // SPR-10140, SPR-16867
	public void handleMatchMatrixVariablesDecoding() {

		MockHttpServletRequest request;

		UrlPathHelper urlPathHelper = new UrlPathHelper();
		urlPathHelper.setUrlDecode(false);
		urlPathHelper.setRemoveSemicolonContent(false);

		this.handlerMapping.setUrlPathHelper(urlPathHelper);

		request = new MockHttpServletRequest();
		handleMatch(request, "/{cars}", "/cars;mvar=a%2Fb");

		MultiValueMap<String, String> matrixVariables = getMatrixVariables(request, "cars");
		Map<String, String> uriVariables = getUriTemplateVariables(request);

		assertThat(matrixVariables).isNotNull();
		assertThat(matrixVariables.get("mvar")).isEqualTo(Collections.singletonList("a/b"));
		assertThat(uriVariables.get("cars")).isEqualTo("cars");
	}


	private HandlerMethod getHandler(MockHttpServletRequest request) throws Exception {
		HandlerExecutionChain chain = this.handlerMapping.getHandler(request);
		assertThat(chain).isNotNull();
		return (HandlerMethod) chain.getHandler();
	}

	private void testHttpMediaTypeNotSupportedException(String url) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("PUT", url);
		request.setContentType("application/json");
		assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class).isThrownBy(() ->
				this.handlerMapping.getHandler(request))
			.satisfies(ex -> assertThat(ex.getSupportedMediaTypes()).containsExactly(MediaType.APPLICATION_XML));
	}

	private void testHttpOptions(String requestURI, String allowHeader) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", requestURI);
		HandlerMethod handlerMethod = getHandler(request);

		ServletWebRequest webRequest = new ServletWebRequest(request);
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		Object result = new InvocableHandlerMethod(handlerMethod).invokeForRequest(webRequest, mavContainer);

		assertThat(result).isNotNull();
		assertThat(result.getClass()).isEqualTo(HttpHeaders.class);
		assertThat(((HttpHeaders) result).getFirst("Allow")).isEqualTo(allowHeader);
	}

	private void testHttpMediaTypeNotAcceptableException(String url) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", url);
		request.addHeader("Accept", "application/json");
		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class).isThrownBy(() ->
				this.handlerMapping.getHandler(request))
			.satisfies(ex -> assertThat(ex.getSupportedMediaTypes()).containsExactly(MediaType.APPLICATION_XML));
	}

	private void handleMatch(MockHttpServletRequest request, String pattern, String lookupPath) {
		RequestMappingInfo info = RequestMappingInfo.paths(pattern).build();
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

		public void registerHandler(Object handler) {
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
				return new RequestMappingInfo(
					new PatternsRequestCondition(annot.value(), getUrlPathHelper(), getPathMatcher(), true, true),
					new RequestMethodsRequestCondition(annot.method()),
					new ParamsRequestCondition(annot.params()),
					new HeadersRequestCondition(annot.headers()),
					new ConsumesRequestCondition(annot.consumes(), annot.headers()),
					new ProducesRequestCondition(annot.produces(), annot.headers()), null);
			}
			else {
				return null;
			}
		}
	}

}
