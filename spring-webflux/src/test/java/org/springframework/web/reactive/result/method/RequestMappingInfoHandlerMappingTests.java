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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.RequestMappingInfo.*;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.support.HttpRequestPathHelper;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import static org.springframework.web.method.MvcAnnotationPredicates.*;
import static org.springframework.web.method.ResolvableMethod.*;
import static org.springframework.web.reactive.result.method.RequestMappingInfo.*;

/**
 * Unit tests for {@link RequestMappingInfoHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoHandlerMappingTests {

	private TestRequestMappingInfoHandlerMapping handlerMapping;


	@Before
	public void setup() throws Exception {
		this.handlerMapping = new TestRequestMappingInfoHandlerMapping();
		this.handlerMapping.registerHandler(new TestController());
	}


	@Test
	public void getMappingPathPatterns() throws Exception {
		String[] patterns = {"/foo/*", "/foo", "/bar/*", "/bar"};
		RequestMappingInfo info = paths(patterns).build();
		Set<String> actual = this.handlerMapping.getMappingPathPatterns(info);

		assertEquals(new HashSet<>(Arrays.asList(patterns)), actual);
	}

	@Test
	public void getHandlerDirectMatch() throws Exception {
		Method expected = on(TestController.class).annot(getMapping("/foo").params()).resolveMethod();
		ServerWebExchange exchange = get("/foo").toExchange();
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();

		assertEquals(expected, hm.getMethod());
	}

	@Test
	public void getHandlerGlobMatch() throws Exception {
		Method expected = on(TestController.class).annot(requestMapping("/ba*").method(GET, HEAD)).resolveMethod();
		ServerWebExchange exchange = get("/bar").toExchange();
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();

		assertEquals(expected, hm.getMethod());
	}

	@Test
	public void getHandlerEmptyPathMatch() throws Exception {
		Method expected = on(TestController.class).annot(requestMapping("")).resolveMethod();
		ServerWebExchange exchange = get("").toExchange();
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();
		assertEquals(expected, hm.getMethod());

		exchange = get("/").toExchange();
		hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();
		assertEquals(expected, hm.getMethod());
	}

	@Test
	public void getHandlerBestMatch() throws Exception {
		Method expected = on(TestController.class).annot(getMapping("/foo").params("p")).resolveMethod();
		ServerWebExchange exchange = get("/foo?p=anything").toExchange();
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();

		assertEquals(expected, hm.getMethod());
	}

	@Test
	public void getHandlerRequestMethodNotAllowed() throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.post("/bar").toExchange();
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, MethodNotAllowedException.class,
				ex -> assertEquals(EnumSet.of(HttpMethod.GET, HttpMethod.HEAD), ex.getSupportedMethods()));
	}

	@Test  // SPR-9603
	public void getHandlerRequestMethodMatchFalsePositive() throws Exception {
		ServerWebExchange exchange = get("/users").accept(MediaType.APPLICATION_XML).toExchange();
		this.handlerMapping.registerHandler(new UserController());
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		StepVerifier.create(mono)
				.expectError(NotAcceptableStatusException.class)
				.verify();
	}

	@Test  // SPR-8462
	public void getHandlerMediaTypeNotSupported() throws Exception {
		testHttpMediaTypeNotSupportedException("/person/1");
		testHttpMediaTypeNotSupportedException("/person/1/");
		testHttpMediaTypeNotSupportedException("/person/1.json");
	}

	@Test
	public void getHandlerTestInvalidContentType() throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.put("/person/1").header("content-type", "bogus").toExchange();
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, UnsupportedMediaTypeStatusException.class,
				ex -> assertEquals("Response status 415 with reason \"Invalid mime type \"bogus\": does not contain '/'\"",
						ex.getMessage()));
	}

	@Test  // SPR-8462
	public void getHandlerTestMediaTypeNotAcceptable() throws Exception {
		testMediaTypeNotAcceptable("/persons");
		testMediaTypeNotAcceptable("/persons/");
		testMediaTypeNotAcceptable("/persons.json");
	}

	@Test  // SPR-12854
	public void getHandlerTestRequestParamMismatch() throws Exception {
		ServerWebExchange exchange = get("/params").toExchange();
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);
		assertError(mono, ServerWebInputException.class, ex -> {
			assertThat(ex.getReason(), containsString("[foo=bar]"));
			assertThat(ex.getReason(), containsString("[bar=baz]"));
		});
	}

	@Test
	public void getHandlerHttpOptions() throws Exception {
		testHttpOptions("/foo", EnumSet.of(HttpMethod.GET, HttpMethod.HEAD));
		testHttpOptions("/person/1", EnumSet.of(HttpMethod.PUT));
		testHttpOptions("/persons", EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE, HttpMethod.OPTIONS));
		testHttpOptions("/something", EnumSet.of(HttpMethod.PUT, HttpMethod.POST));
	}

	@Test
	public void getHandlerProducibleMediaTypesAttribute() throws Exception {
		ServerWebExchange exchange = get("/content").accept(MediaType.APPLICATION_XML).toExchange();
		this.handlerMapping.getHandler(exchange).block();

		String name = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;
		assertEquals(Collections.singleton(MediaType.APPLICATION_XML), exchange.getAttributes().get(name));

		exchange = get("/content").accept(MediaType.APPLICATION_JSON).toExchange();
		this.handlerMapping.getHandler(exchange).block();

		assertNull("Negated expression shouldn't be listed as producible type",
				exchange.getAttributes().get(name));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void handleMatchUriTemplateVariables() throws Exception {
		String lookupPath = "/1/2";
		ServerWebExchange exchange = get(lookupPath).toExchange();

		RequestMappingInfo key = paths("/{path1}/{path2}").build();
		this.handlerMapping.handleMatch(key, lookupPath, exchange);

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Map<String, String> uriVariables = (Map<String, String>) exchange.getAttributes().get(name);

		assertNotNull(uriVariables);
		assertEquals("1", uriVariables.get("path1"));
		assertEquals("2", uriVariables.get("path2"));
	}

	@Test  // SPR-9098
	public void handleMatchUriTemplateVariablesDecode() throws Exception {
		RequestMappingInfo key = paths("/{group}/{identifier}").build();
		URI url = URI.create("/group/a%2Fb");
		ServerWebExchange exchange = MockServerHttpRequest.method(HttpMethod.GET, url).toExchange();

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
		RequestMappingInfo key = paths("/{path1}/2", "/**").build();
		ServerWebExchange exchange = get("/1/2").toExchange();
		this.handlerMapping.handleMatch(key, "/1/2", exchange);

		assertEquals("/{path1}/2", exchange.getAttributes().get(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
	}

	@Test
	public void handleMatchBestMatchingPatternAttributeNoPatternsDefined() throws Exception {
		RequestMappingInfo key = paths().build();
		ServerWebExchange exchange = get("/1/2").toExchange();

		this.handlerMapping.handleMatch(key, "/1/2", exchange);

		assertEquals("/1/2", exchange.getAttributes().get(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
	}

	@Test
	public void handleMatchMatrixVariables() throws Exception {
		MultiValueMap<String, String> matrixVariables;
		Map<String, String> uriVariables;

		ServerWebExchange exchange = get("/").toExchange();
		handleMatch(exchange, "/{cars}", "/cars;colors=red,blue,green;year=2012");

		matrixVariables = getMatrixVariables(exchange, "cars");
		uriVariables = getUriTemplateVariables(exchange);

		assertNotNull(matrixVariables);
		assertEquals(Arrays.asList("red", "blue", "green"), matrixVariables.get("colors"));
		assertEquals("2012", matrixVariables.getFirst("year"));
		assertEquals("cars", uriVariables.get("cars"));

		exchange = get("/").toExchange();
		handleMatch(exchange, "/{cars:[^;]+}{params}", "/cars;colors=red,blue,green;year=2012");

		matrixVariables = getMatrixVariables(exchange, "params");
		uriVariables = getUriTemplateVariables(exchange);

		assertNotNull(matrixVariables);
		assertEquals(Arrays.asList("red", "blue", "green"), matrixVariables.get("colors"));
		assertEquals("2012", matrixVariables.getFirst("year"));
		assertEquals("cars", uriVariables.get("cars"));
		assertEquals(";colors=red,blue,green;year=2012", uriVariables.get("params"));

		exchange = get("/").toExchange();
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
		this.handlerMapping.setPathHelper(urlPathHelper);

		ServerWebExchange exchange = get("/").toExchange();
		handleMatch(exchange, "/path{filter}", "/path;mvar=a%2fb");

		MultiValueMap<String, String> matrixVariables = getMatrixVariables(exchange, "filter");
		Map<String, String> uriVariables = getUriTemplateVariables(exchange);

		assertNotNull(matrixVariables);
		assertEquals(Collections.singletonList("a/b"), matrixVariables.get("mvar"));
		assertEquals(";mvar=a/b", uriVariables.get("filter"));
	}


	@SuppressWarnings("unchecked")
	private <T> void assertError(Mono<Object> mono, final Class<T> exceptionClass, final Consumer<T> consumer)  {
		StepVerifier.create(mono)
				.consumeErrorWith(error -> {
					assertEquals(exceptionClass, error.getClass());
					consumer.accept((T) error);

				})
				.verify();
	}

	private void testHttpMediaTypeNotSupportedException(String url) throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.put(url).contentType(MediaType.APPLICATION_JSON).toExchange();
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, UnsupportedMediaTypeStatusException.class, ex ->
				assertEquals("Invalid supported consumable media types",
						Collections.singletonList(new MediaType("application", "xml")),
						ex.getSupportedMediaTypes()));
	}

	private void testHttpOptions(String requestURI, Set<HttpMethod> allowedMethods) throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.options(requestURI).toExchange();
		HandlerMethod handlerMethod = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();

		BindingContext bindingContext = new BindingContext();
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
		Mono<HandlerResult> mono = invocable.invoke(exchange, bindingContext);

		HandlerResult result = mono.block();
		assertNotNull(result);

		Optional<Object> value = result.getReturnValue();
		assertTrue(value.isPresent());
		assertEquals(HttpHeaders.class, value.get().getClass());
		assertEquals(allowedMethods, ((HttpHeaders) value.get()).getAllow());
	}

	private void testMediaTypeNotAcceptable(String url) throws Exception {
		ServerWebExchange exchange = get(url).accept(MediaType.APPLICATION_JSON).toExchange();
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, NotAcceptableStatusException.class, ex ->
				assertEquals("Invalid supported producible media types",
						Collections.singletonList(new MediaType("application", "xml")),
						ex.getSupportedMediaTypes()));
	}

	private void handleMatch(ServerWebExchange exchange, String pattern, String lookupPath) {
		RequestMappingInfo info = paths(pattern).build();
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

		@GetMapping("/foo")
		public void foo() {
		}

		@GetMapping(path = "/foo", params="p")
		public void fooParam() {
		}

		@RequestMapping(path = "/ba*", method = { GET, HEAD })
		public void bar() {
		}

		@RequestMapping(path = "")
		public void empty() {
		}

		@PutMapping(path = "/person/{id}", consumes="application/xml")
		public void consumes(@RequestBody String text) {
		}

		@RequestMapping(path = "/persons", produces="application/xml")
		public String produces() {
			return "";
		}

		@RequestMapping(path = "/params", params="foo=bar")
		public String param() {
			return "";
		}

		@RequestMapping(path = "/params", params="bar=baz")
		public String param2() {
			return "";
		}

		@RequestMapping(path = "/content", produces="application/xml")
		public String xmlContent() {
			return "";
		}

		@RequestMapping(path = "/content", produces="!application/xml")
		public String nonXmlContent() {
			return "";
		}

		@RequestMapping(path = "/something", method = OPTIONS)
		public HttpHeaders fooOptions() {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Allow", "PUT,POST");
			return headers;
		}
	}


	@SuppressWarnings("unused")
	@Controller
	private static class UserController {

		@GetMapping(path = "/users", produces = "application/json")
		public void getUser() {
		}

		@PutMapping(path = "/users")
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
			RequestMapping annot = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
			if (annot != null) {
				BuilderConfiguration options = new BuilderConfiguration();
				options.setPathHelper(getPathHelper());
				options.setPathMatcher(getPathMatcher());
				options.setSuffixPatternMatch(true);
				options.setTrailingSlashMatch(true);
				return paths(annot.value()).methods(annot.method())
						.params(annot.params()).headers(annot.headers())
						.consumes(annot.consumes()).produces(annot.produces())
						.options(options).build();
			}
			else {
				return null;
			}
		}
	}

}