/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.core.test.TestSubscriber;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.RequestMappingInfo.BuilderConfiguration;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.WebSessionManager;
import org.springframework.web.util.HttpRequestPathHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


/**
 * Unit tests for {@link RequestMappingInfoHandlerMapping}.
 *
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
	}


	@Test
	public void getMappingPathPatterns() throws Exception {
		String[] patterns = {"/foo/*", "/foo", "/bar/*", "/bar"};
		RequestMappingInfo info = RequestMappingInfo.paths(patterns).build();
		Set<String> actual = this.handlerMapping.getMappingPathPatterns(info);

		assertEquals(new HashSet<>(Arrays.asList(patterns)), actual);
	}

	@Test
	public void getHandlerDirectMatch() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/foo");
		HandlerMethod handlerMethod = getHandler(exchange);
		assertEquals(this.fooMethod.getMethod(), handlerMethod.getMethod());
	}

	@Test
	public void getHandlerGlobMatch() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/bar");
		HandlerMethod handlerMethod = getHandler(exchange);
		assertEquals(this.barMethod.getMethod(), handlerMethod.getMethod());
	}

	@Test
	public void getHandlerEmptyPathMatch() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "");
		HandlerMethod handlerMethod = getHandler(exchange);

		assertEquals(this.emptyMethod.getMethod(), handlerMethod.getMethod());

		exchange = createExchange(HttpMethod.GET, "/");
		handlerMethod = getHandler(exchange);

		assertEquals(this.emptyMethod.getMethod(), handlerMethod.getMethod());
	}

	@Test
	public void getHandlerBestMatch() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/foo");
		exchange.getRequest().getQueryParams().add("p", "anything");
		HandlerMethod handlerMethod = getHandler(exchange);

		assertEquals(this.fooParamMethod.getMethod(), handlerMethod.getMethod());
	}

	@Test
	public void getHandlerRequestMethodNotAllowed() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.POST, "/bar");
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);
		assertError(mono, MethodNotAllowedException.class,
				ex -> assertEquals(new HashSet<>(Arrays.asList("GET", "HEAD")), ex.getSupportedMethods()));
	}

	// SPR-9603

	@Test
	public void getHandlerRequestMethodMatchFalsePositive() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/users");
		exchange.getRequest().getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
		this.handlerMapping.registerHandler(new UserController());
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		TestSubscriber<Object> subscriber = new TestSubscriber<>();
		mono.subscribeWith(subscriber);
		subscriber.assertError(NotAcceptableStatusException.class);
	}

	// SPR-8462

	@Test
	public void getHandlerMediaTypeNotSupported() throws Exception {
		testHttpMediaTypeNotSupportedException("/person/1");
		testHttpMediaTypeNotSupportedException("/person/1/");
		testHttpMediaTypeNotSupportedException("/person/1.json");
	}

	@Test
	public void getHandlerHttpOptions() throws Exception {
		testHttpOptions("/foo", "GET,HEAD");
		testHttpOptions("/person/1", "PUT");
		testHttpOptions("/persons", "GET,HEAD,POST,PUT,PATCH,DELETE,OPTIONS");
		testHttpOptions("/something", "PUT,POST");
	}

	@Test
	public void getHandlerTestInvalidContentType() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.PUT, "/person/1");
		exchange.getRequest().getHeaders().add("Content-Type", "bogus");
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);
		assertError(mono, UnsupportedMediaTypeStatusException.class,
				ex -> assertEquals("Request failure [status: 415, " +
						"reason: \"Invalid mime type \"bogus\": does not contain '/'\"]",
						ex.getMessage()));
	}

	// SPR-8462

	@Test
	public void getHandlerMediaTypeNotAccepted() throws Exception {
		testHttpMediaTypeNotAcceptableException("/persons");
		testHttpMediaTypeNotAcceptableException("/persons/");
		testHttpMediaTypeNotAcceptableException("/persons.json");
	}

	// SPR-12854

	@Test
	public void getHandlerUnsatisfiedServletRequestParameterException() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/params");
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);
		assertError(mono, ServerWebInputException.class, ex -> {
			assertEquals(ex.getReason(), "Unsatisfied query parameter conditions: " +
					"[[bar=baz], [foo=bar]], actual parameters: {}");
		});
	}

	@Test
	public void getHandlerProducibleMediaTypesAttribute() throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/content");
		exchange.getRequest().getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_XML));
		getHandler(exchange);

		String name = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;
		assertEquals(Collections.singleton(MediaType.APPLICATION_XML), exchange.getAttributes().get(name));

		exchange = createExchange(HttpMethod.GET, "/content");
		exchange.getRequest().getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		getHandler(exchange);

		assertNull("Negated expression shouldn't be listed as producible type",
				exchange.getAttributes().get(name));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void handleMatchUriTemplateVariables() throws Exception {
		RequestMappingInfo key = RequestMappingInfo.paths("/{path1}/{path2}").build();
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/1/2");
		String lookupPath = exchange.getRequest().getURI().getPath();
		this.handlerMapping.handleMatch(key, lookupPath, exchange);

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Map<String, String> uriVariables = (Map<String, String>) exchange.getAttributes().get(name);

		assertNotNull(uriVariables);
		assertEquals("1", uriVariables.get("path1"));
		assertEquals("2", uriVariables.get("path2"));
	}

	// SPR-9098

	@Test
	public void handleMatchUriTemplateVariablesDecode() throws Exception {
		RequestMappingInfo key = RequestMappingInfo.paths("/{group}/{identifier}").build();
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/group/a%2Fb");

		HttpRequestPathHelper pathHelper = new HttpRequestPathHelper();
		pathHelper.setUrlDecode(false);
		String lookupPath = pathHelper.getLookupPathForRequest(exchange);

		this.handlerMapping.setPathHelper(pathHelper);
		this.handlerMapping.handleMatch(key, lookupPath, exchange);

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		@SuppressWarnings("unchecked")
		Map<String, String> uriVariables = (Map<String, String>) exchange.getAttributes().get(name);

		assertNotNull(uriVariables);
		assertEquals("group", uriVariables.get("group"));
		assertEquals("a/b", uriVariables.get("identifier"));
	}

	@Test
	public void handleMatchBestMatchingPatternAttribute() throws Exception {
		RequestMappingInfo key = RequestMappingInfo.paths("/{path1}/2", "/**").build();
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/1/2");
		this.handlerMapping.handleMatch(key, "/1/2", exchange);

		assertEquals("/{path1}/2", exchange.getAttributes().get(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
	}

	@Test
	public void handleMatchBestMatchingPatternAttributeNoPatternsDefined() throws Exception {
		RequestMappingInfo key = RequestMappingInfo.paths().build();
		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/1/2");

		this.handlerMapping.handleMatch(key, "/1/2", exchange);

		assertEquals("/1/2", exchange.getAttributes().get(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
	}

	@Test
	public void handleMatchMatrixVariables() throws Exception {
		ServerWebExchange exchange;
		MultiValueMap<String, String> matrixVariables;
		Map<String, String> uriVariables;

		exchange = createExchange(HttpMethod.GET, "/");
		handleMatch(exchange, "/{cars}", "/cars;colors=red,blue,green;year=2012");

		matrixVariables = getMatrixVariables(exchange, "cars");
		uriVariables = getUriTemplateVariables(exchange);

		assertNotNull(matrixVariables);
		assertEquals(Arrays.asList("red", "blue", "green"), matrixVariables.get("colors"));
		assertEquals("2012", matrixVariables.getFirst("year"));
		assertEquals("cars", uriVariables.get("cars"));

		exchange = createExchange(HttpMethod.GET, "/");
		handleMatch(exchange, "/{cars:[^;]+}{params}", "/cars;colors=red,blue,green;year=2012");

		matrixVariables = getMatrixVariables(exchange, "params");
		uriVariables = getUriTemplateVariables(exchange);

		assertNotNull(matrixVariables);
		assertEquals(Arrays.asList("red", "blue", "green"), matrixVariables.get("colors"));
		assertEquals("2012", matrixVariables.getFirst("year"));
		assertEquals("cars", uriVariables.get("cars"));
		assertEquals(";colors=red,blue,green;year=2012", uriVariables.get("params"));

		exchange = createExchange(HttpMethod.GET, "/");
		handleMatch(exchange, "/{cars:[^;]+}{params}", "/cars");

		matrixVariables = getMatrixVariables(exchange, "params");
		uriVariables = getUriTemplateVariables(exchange);

		assertNull(matrixVariables);
		assertEquals("cars", uriVariables.get("cars"));
		assertEquals("", uriVariables.get("params"));
	}

	@Test
	public void handleMatchMatrixVariablesDecoding() throws Exception {
		HttpRequestPathHelper urlPathHelper = new HttpRequestPathHelper();
		urlPathHelper.setUrlDecode(false);

		this.handlerMapping.setPathHelper(urlPathHelper );

		ServerWebExchange exchange = createExchange(HttpMethod.GET, "/");
		handleMatch(exchange, "/path{filter}", "/path;mvar=a%2fb");

		MultiValueMap<String, String> matrixVariables = getMatrixVariables(exchange, "filter");
		Map<String, String> uriVariables = getUriTemplateVariables(exchange);

		assertNotNull(matrixVariables);
		assertEquals(Collections.singletonList("a/b"), matrixVariables.get("mvar"));
		assertEquals(";mvar=a/b", uriVariables.get("filter"));
	}


	private ServerWebExchange createExchange(HttpMethod method, String url) throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(method, new URI(url));
		WebSessionManager sessionManager = mock(WebSessionManager.class);
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);

	}

	@SuppressWarnings("unchecked")
	private <T> void assertError(Mono<Object> mono, final Class<T> exceptionClass, final Consumer<T> consumer)  {
		TestSubscriber<Object> subscriber = new TestSubscriber<>();
		mono.subscribeWith(subscriber);
		subscriber.assertErrorWith(ex -> {
			assertEquals(exceptionClass, ex.getClass());
			consumer.accept((T) ex);
		});
	}

	@SuppressWarnings("ConstantConditions")
	private HandlerMethod getHandler(ServerWebExchange exchange) throws Exception {
		Mono<Object> handler = this.handlerMapping.getHandler(exchange);
		return (HandlerMethod) handler.get();
	}

	private void testHttpMediaTypeNotSupportedException(String url) throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.PUT, url);
		exchange.getRequest().getHeaders().setContentType(MediaType.APPLICATION_JSON);
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, UnsupportedMediaTypeStatusException.class, ex ->
				assertEquals("Invalid supported consumable media types",
						Collections.singletonList(new MediaType("application", "xml")),
						ex.getSupportedMediaTypes()));
	}

	private void testHttpOptions(String requestURI, String allowHeader) throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.OPTIONS, requestURI);
		HandlerMethod handlerMethod = getHandler(exchange);

		ModelMap model = new ExtendedModelMap();
		Mono<HandlerResult> mono = new InvocableHandlerMethod(handlerMethod).invokeForRequest(exchange, model);

		HandlerResult result = mono.get();
		assertNotNull(result);

		Optional<Object> value = result.getReturnValue();
		assertTrue(value.isPresent());
		assertEquals(HttpHeaders.class, value.get().getClass());
		assertEquals(allowHeader, ((HttpHeaders) value.get()).getFirst("Allow"));
	}

	private void testHttpMediaTypeNotAcceptableException(String url) throws Exception {
		ServerWebExchange exchange = createExchange(HttpMethod.GET, url);
		exchange.getRequest().getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, NotAcceptableStatusException.class, ex ->
				assertEquals("Invalid supported producible media types",
						Collections.singletonList(new MediaType("application", "xml")),
						ex.getSupportedMediaTypes()));
	}

	private void handleMatch(ServerWebExchange exchange, String pattern, String lookupPath) {
		RequestMappingInfo info = RequestMappingInfo.paths(pattern).build();
		this.handlerMapping.handleMatch(info, lookupPath, exchange);
	}

	@SuppressWarnings("unchecked")
	private MultiValueMap<String, String> getMatrixVariables(ServerWebExchange exchange, String uriVarName) {
		String attrName = HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE;
		return ((Map<String, MultiValueMap<String, String>>) exchange.getAttributes().get(attrName)).get(uriVarName);
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getUriTemplateVariables(ServerWebExchange exchange) {
		String attrName = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		return (Map<String, String>) exchange.getAttributes().get(attrName);
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
			RequestMapping annotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
			if (annotation != null) {
				BuilderConfiguration options = new BuilderConfiguration();
				options.setPathHelper(getPathHelper());
				options.setPathMatcher(getPathMatcher());
				options.setSuffixPatternMatch(true);
				options.setTrailingSlashMatch(true);
				return RequestMappingInfo.paths(annotation.value()).methods(annotation.method())
						.params(annotation.params()).headers(annotation.headers())
						.consumes(annotation.consumes()).produces(annotation.produces())
						.options(options).build();
			}
			else {
				return null;
			}
		}
	}

}