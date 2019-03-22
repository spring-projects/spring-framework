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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
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
import org.springframework.web.util.pattern.PathPattern;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import static org.springframework.web.method.MvcAnnotationPredicates.*;
import static org.springframework.web.method.ResolvableMethod.*;
import static org.springframework.web.reactive.HandlerMapping.*;
import static org.springframework.web.reactive.result.method.RequestMappingInfo.*;

/**
 * Unit tests for {@link RequestMappingInfoHandlerMapping}.
 * @author Rossen Stoyanchev
 */
public class RequestMappingInfoHandlerMappingTests {

	private static final HandlerMethod handlerMethod = new HandlerMethod(new TestController(),
			ClassUtils.getMethod(TestController.class, "dummy"));

	private TestRequestMappingInfoHandlerMapping handlerMapping;


	@Before
	public void setup() {
		this.handlerMapping = new TestRequestMappingInfoHandlerMapping();
		this.handlerMapping.registerHandler(new TestController());
	}


	@Test
	public void getHandlerDirectMatch() {
		Method expected = on(TestController.class).annot(getMapping("/foo").params()).resolveMethod();
		ServerWebExchange exchange = MockServerWebExchange.from(get("/foo"));
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();

		assertEquals(expected, hm.getMethod());
	}

	@Test
	public void getHandlerGlobMatch() {
		Method expected = on(TestController.class).annot(requestMapping("/ba*").method(GET, HEAD)).resolveMethod();
		ServerWebExchange exchange = MockServerWebExchange.from(get("/bar"));
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();

		assertEquals(expected, hm.getMethod());
	}

	@Test
	public void getHandlerEmptyPathMatch() {
		Method expected = on(TestController.class).annot(requestMapping("")).resolveMethod();
		ServerWebExchange exchange = MockServerWebExchange.from(get(""));
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();
		assertEquals(expected, hm.getMethod());

		exchange = MockServerWebExchange.from(get("/"));
		hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();
		assertEquals(expected, hm.getMethod());
	}

	@Test
	public void getHandlerBestMatch() {
		Method expected = on(TestController.class).annot(getMapping("/foo").params("p")).resolveMethod();
		ServerWebExchange exchange = MockServerWebExchange.from(get("/foo?p=anything"));
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();

		assertEquals(expected, hm.getMethod());
	}

	@Test
	public void getHandlerRequestMethodNotAllowed() {
		ServerWebExchange exchange = MockServerWebExchange.from(post("/bar"));
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, MethodNotAllowedException.class,
				ex -> assertEquals(EnumSet.of(HttpMethod.GET, HttpMethod.HEAD), ex.getSupportedMethods()));
	}

	@Test  // SPR-9603
	public void getHandlerRequestMethodMatchFalsePositive() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/users").accept(MediaType.APPLICATION_XML));
		this.handlerMapping.registerHandler(new UserController());
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		StepVerifier.create(mono)
				.expectError(NotAcceptableStatusException.class)
				.verify();
	}

	@Test  // SPR-8462
	public void getHandlerMediaTypeNotSupported() {
		testHttpMediaTypeNotSupportedException("/person/1");
		testHttpMediaTypeNotSupportedException("/person/1/");
		testHttpMediaTypeNotSupportedException("/person/1.json");
	}

	@Test
	public void getHandlerTestInvalidContentType() {
		MockServerHttpRequest request = put("/person/1").header("content-type", "bogus").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, UnsupportedMediaTypeStatusException.class,
				ex -> assertEquals("415 UNSUPPORTED_MEDIA_TYPE " +
						"\"Invalid mime type \"bogus\": does not contain '/'\"", ex.getMessage()));
	}

	@Test  // SPR-8462
	public void getHandlerTestMediaTypeNotAcceptable() {
		testMediaTypeNotAcceptable("/persons");
		testMediaTypeNotAcceptable("/persons/");
	}

	@Test  // SPR-12854
	public void getHandlerTestRequestParamMismatch() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/params"));
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);
		assertError(mono, ServerWebInputException.class, ex -> {
			assertThat(ex.getReason(), containsString("[foo=bar]"));
			assertThat(ex.getReason(), containsString("[bar=baz]"));
		});
	}

	@Test
	public void getHandlerHttpOptions() {
		List<HttpMethod> allMethodExceptTrace = new ArrayList<>(Arrays.asList(HttpMethod.values()));
		allMethodExceptTrace.remove(HttpMethod.TRACE);

		testHttpOptions("/foo", EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS));
		testHttpOptions("/person/1", EnumSet.of(HttpMethod.PUT, HttpMethod.OPTIONS));
		testHttpOptions("/persons", EnumSet.copyOf(allMethodExceptTrace));
		testHttpOptions("/something", EnumSet.of(HttpMethod.PUT, HttpMethod.POST));
	}

	@Test
	public void getHandlerProducibleMediaTypesAttribute() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/content").accept(MediaType.APPLICATION_XML));
		this.handlerMapping.getHandler(exchange).block();

		String name = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;
		assertEquals(Collections.singleton(MediaType.APPLICATION_XML), exchange.getAttributes().get(name));

		exchange = MockServerWebExchange.from(get("/content").accept(MediaType.APPLICATION_JSON));
		this.handlerMapping.getHandler(exchange).block();

		assertNull("Negated expression shouldn't be listed as producible type",
				exchange.getAttributes().get(name));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void handleMatchUriTemplateVariables() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/1/2"));

		RequestMappingInfo key = paths("/{path1}/{path2}").build();
		this.handlerMapping.handleMatch(key, handlerMethod, exchange);

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Map<String, String> uriVariables = (Map<String, String>) exchange.getAttributes().get(name);

		assertNotNull(uriVariables);
		assertEquals("1", uriVariables.get("path1"));
		assertEquals("2", uriVariables.get("path2"));
	}

	@Test  // SPR-9098
	public void handleMatchUriTemplateVariablesDecode() {
		RequestMappingInfo key = paths("/{group}/{identifier}").build();
		URI url = URI.create("/group/a%2Fb");
		ServerWebExchange exchange = MockServerWebExchange.from(method(HttpMethod.GET, url));

		this.handlerMapping.handleMatch(key, handlerMethod, exchange);

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		@SuppressWarnings("unchecked")
		Map<String, String> uriVariables = (Map<String, String>) exchange.getAttributes().get(name);

		assertNotNull(uriVariables);
		assertEquals("group", uriVariables.get("group"));
		assertEquals("a/b", uriVariables.get("identifier"));
	}

	@Test
	public void handleMatchBestMatchingPatternAttribute() {
		RequestMappingInfo key = paths("/{path1}/2", "/**").build();
		ServerWebExchange exchange = MockServerWebExchange.from(get("/1/2"));
		this.handlerMapping.handleMatch(key, handlerMethod, exchange);

		PathPattern bestMatch = (PathPattern) exchange.getAttributes().get(BEST_MATCHING_PATTERN_ATTRIBUTE);
		assertEquals("/{path1}/2", bestMatch.getPatternString());

		HandlerMethod mapped = (HandlerMethod) exchange.getAttributes().get(BEST_MATCHING_HANDLER_ATTRIBUTE);
		assertSame(handlerMethod, mapped);
	}

	@Test
	public void handleMatchBestMatchingPatternAttributeNoPatternsDefined() {
		RequestMappingInfo key = paths().build();
		ServerWebExchange exchange = MockServerWebExchange.from(get("/1/2"));
		this.handlerMapping.handleMatch(key, handlerMethod, exchange);

		PathPattern bestMatch = (PathPattern) exchange.getAttributes().get(BEST_MATCHING_PATTERN_ATTRIBUTE);
		assertEquals("/1/2", bestMatch.getPatternString());
	}

	@Test
	public void handleMatchMatrixVariables() {
		MultiValueMap<String, String> matrixVariables;
		Map<String, String> uriVariables;

		ServerWebExchange exchange = MockServerWebExchange.from(get("/cars;colors=red,blue,green;year=2012"));
		handleMatch(exchange, "/{cars}");

		matrixVariables = getMatrixVariables(exchange, "cars");
		uriVariables = getUriTemplateVariables(exchange);

		assertNotNull(matrixVariables);
		assertEquals(Arrays.asList("red", "blue", "green"), matrixVariables.get("colors"));
		assertEquals("2012", matrixVariables.getFirst("year"));
		assertEquals("cars", uriVariables.get("cars"));

		// SPR-11897
		exchange = MockServerWebExchange.from(get("/a=42;b=c"));
		handleMatch(exchange, "/{foo}");

		matrixVariables = getMatrixVariables(exchange, "foo");
		uriVariables = getUriTemplateVariables(exchange);

		// Unlike Spring MVC, WebFlux currently does not support APIs like
		// "/foo/{ids}" and URL "/foo/id=1;id=2;id=3" where the whole path
		// segment is a sequence of name-value pairs.

		assertNotNull(matrixVariables);
		assertEquals(1, matrixVariables.size());
		assertEquals("c", matrixVariables.getFirst("b"));
		assertEquals("a=42", uriVariables.get("foo"));
	}

	@Test
	public void handleMatchMatrixVariablesDecoding() {
		MockServerHttpRequest request = method(HttpMethod.GET, URI.create("/cars;mvar=a%2Fb")).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		handleMatch(exchange, "/{cars}");

		MultiValueMap<String, String> matrixVariables = getMatrixVariables(exchange, "cars");
		Map<String, String> uriVariables = getUriTemplateVariables(exchange);

		assertNotNull(matrixVariables);
		assertEquals(Collections.singletonList("a/b"), matrixVariables.get("mvar"));
		assertEquals("cars", uriVariables.get("cars"));
	}


	@SuppressWarnings("unchecked")
	private <T> void assertError(Mono<Object> mono, final Class<T> exceptionClass, final Consumer<T> consumer) {
		StepVerifier.create(mono)
				.consumeErrorWith(error -> {
					assertEquals(exceptionClass, error.getClass());
					consumer.accept((T) error);
				})
				.verify();
	}

	private void testHttpMediaTypeNotSupportedException(String url) {
		MockServerHttpRequest request = put(url).contentType(MediaType.APPLICATION_JSON).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, UnsupportedMediaTypeStatusException.class, ex ->
				assertEquals("Invalid supported consumable media types",
						Collections.singletonList(new MediaType("application", "xml")),
						ex.getSupportedMediaTypes()));
	}

	private void testHttpOptions(String requestURI, Set<HttpMethod> allowedMethods) {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options(requestURI));
		HandlerMethod handlerMethod = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();

		BindingContext bindingContext = new BindingContext();
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
		Mono<HandlerResult> mono = invocable.invoke(exchange, bindingContext);

		HandlerResult result = mono.block();
		assertNotNull(result);

		Object value = result.getReturnValue();
		assertNotNull(value);
		assertEquals(HttpHeaders.class, value.getClass());
		assertEquals(allowedMethods, ((HttpHeaders) value).getAllow());
	}

	private void testMediaTypeNotAcceptable(String url) {
		ServerWebExchange exchange = MockServerWebExchange.from(get(url).accept(MediaType.APPLICATION_JSON));
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, NotAcceptableStatusException.class, ex ->
				assertEquals("Invalid supported producible media types",
						Collections.singletonList(new MediaType("application", "xml")),
						ex.getSupportedMediaTypes()));
	}

	private void handleMatch(ServerWebExchange exchange, String pattern) {
		RequestMappingInfo info = paths(pattern).build();
		this.handlerMapping.handleMatch(info, handlerMethod, exchange);
	}

	@SuppressWarnings("unchecked")
	private MultiValueMap<String, String> getMatrixVariables(ServerWebExchange exchange, String uriVarName) {
		return ((Map<String, MultiValueMap<String, String>>) exchange.getAttributes()
				.get(HandlerMapping.MATRIX_VARIABLES_ATTRIBUTE)).get(uriVarName);
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getUriTemplateVariables(ServerWebExchange exchange) {
		return (Map<String, String>) exchange.getAttributes()
				.get(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
	}


	@SuppressWarnings("unused")
	@Controller
	private static class TestController {

		@GetMapping("/foo")
		public void foo() {
		}

		@GetMapping(path = "/foo", params = "p")
		public void fooParam() {
		}

		@RequestMapping(path = "/ba*", method = {GET, HEAD})
		public void bar() {
		}

		@RequestMapping(path = "")
		public void empty() {
		}

		@PutMapping(path = "/person/{id}", consumes = "application/xml")
		public void consumes(@RequestBody String text) {
		}

		@RequestMapping(path = "/persons", produces = "application/xml")
		public String produces() {
			return "";
		}

		@RequestMapping(path = "/params", params = "foo=bar")
		public String param() {
			return "";
		}

		@RequestMapping(path = "/params", params = "bar=baz")
		public String param2() {
			return "";
		}

		@RequestMapping(path = "/content", produces = "application/xml")
		public String xmlContent() {
			return "";
		}

		@RequestMapping(path = "/content", produces = "!application/xml")
		public String nonXmlContent() {
			return "";
		}

		@RequestMapping(path = "/something", method = OPTIONS)
		public HttpHeaders fooOptions() {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Allow", "PUT,POST");
			return headers;
		}

		public void dummy() { }
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
				options.setPatternParser(getPathPatternParser());
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
