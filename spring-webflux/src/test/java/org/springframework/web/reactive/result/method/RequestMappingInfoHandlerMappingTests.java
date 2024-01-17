/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.RequestMappingInfo.BuilderConfiguration;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsatisfiedRequestParameterException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import static org.springframework.web.reactive.HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE;
import static org.springframework.web.reactive.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;
import static org.springframework.web.reactive.result.method.RequestMappingInfo.paths;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.method;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.post;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.put;
import static org.springframework.web.testfixture.method.MvcAnnotationPredicates.getMapping;
import static org.springframework.web.testfixture.method.MvcAnnotationPredicates.requestMapping;
import static org.springframework.web.testfixture.method.ResolvableMethod.on;

/**
 * Tests for {@link RequestMappingInfoHandlerMapping}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class RequestMappingInfoHandlerMappingTests {

	private static final HandlerMethod handlerMethod = new HandlerMethod(new TestController(),
			ClassUtils.getMethod(TestController.class, "dummy"));

	private TestRequestMappingInfoHandlerMapping handlerMapping;


	@BeforeEach
	void setup() {
		this.handlerMapping = new TestRequestMappingInfoHandlerMapping();
		this.handlerMapping.registerHandler(new TestController());
	}


	@Test
	void getHandlerDirectMatch() {
		Method expected = on(TestController.class).annot(getMapping("/foo").params()).resolveMethod();
		ServerWebExchange exchange = MockServerWebExchange.from(get("/foo"));
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();

		assertThat(hm.getMethod()).isEqualTo(expected);
	}

	@Test
	void getHandlerGlobMatch() {
		Method expected = on(TestController.class).annot(requestMapping("/ba*").method(GET, HEAD)).resolveMethod();
		ServerWebExchange exchange = MockServerWebExchange.from(get("/bar"));
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();

		assertThat(hm.getMethod()).isEqualTo(expected);
	}

	@Test
	void getHandlerEmptyPathMatch() {
		Method expected = on(TestController.class).annot(requestMapping("")).resolveMethod();
		ServerWebExchange exchange = MockServerWebExchange.from(get(""));
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();
		assertThat(hm.getMethod()).isEqualTo(expected);
	}

	@Test
	void getHandlerBestMatch() {
		Method expected = on(TestController.class).annot(getMapping("/foo").params("p")).resolveMethod();
		ServerWebExchange exchange = MockServerWebExchange.from(get("/foo?p=anything"));
		HandlerMethod hm = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();

		assertThat(hm.getMethod()).isEqualTo(expected);
	}

	@Test
	void getHandlerRequestMethodNotAllowed() {
		ServerWebExchange exchange = MockServerWebExchange.from(post("/bar"));
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, MethodNotAllowedException.class,
				ex -> assertThat(ex.getSupportedMethods()).isEqualTo(Set.of(HttpMethod.GET, HttpMethod.HEAD)));
	}

	@Test  // SPR-9603
	void getHandlerRequestMethodMatchFalsePositive() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/users").accept(MediaType.APPLICATION_XML));
		this.handlerMapping.registerHandler(new UserController());
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		StepVerifier.create(mono)
				.expectError(NotAcceptableStatusException.class)
				.verify();
	}

	@Test  // SPR-8462
	void getHandlerMediaTypeNotSupported() {
		testHttpMediaTypeNotSupportedException("/person/1");
		testHttpMediaTypeNotSupportedException("/person/1.json");
	}

	@Test
	void getHandlerTestInvalidContentType() {
		MockServerHttpRequest request = put("/person/1").header("content-type", "bogus").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, UnsupportedMediaTypeStatusException.class,
				ex -> assertThat(ex.getMessage()).isEqualTo(("415 UNSUPPORTED_MEDIA_TYPE " +
										"\"Invalid mime type \"bogus\": does not contain '/'\"")));
	}

	@Test  // SPR-8462
	void getHandlerTestMediaTypeNotAcceptable() {
		testMediaTypeNotAcceptable("/persons");
	}

	@Test  // SPR-12854
	void getHandlerTestRequestParamMismatch() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/params"));
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);
		assertError(mono, UnsatisfiedRequestParameterException.class, ex -> {
			assertThat(ex.getReason()).contains("[foo=bar]");
			assertThat(ex.getReason()).contains("[bar=baz]");
		});
	}

	@Test
	void getHandlerHttpOptions() {
		List<HttpMethod> allMethodExceptTrace = new ArrayList<>(Arrays.asList(HttpMethod.values()));
		allMethodExceptTrace.remove(HttpMethod.TRACE);

		testHttpOptions("/foo", Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS), null);
		testHttpOptions("/person/1", Set.of(HttpMethod.PUT, HttpMethod.OPTIONS), null);
		testHttpOptions("/persons", Set.copyOf(allMethodExceptTrace), null);
		testHttpOptions("/something", Set.of(HttpMethod.PUT, HttpMethod.POST), null);
		testHttpOptions("/qux", Set.of(HttpMethod.PATCH,HttpMethod.GET,HttpMethod.HEAD,HttpMethod.OPTIONS),
				new MediaType("foo", "bar"));
	}

	@Test
	void getHandlerProducibleMediaTypesAttribute() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/content").accept(MediaType.APPLICATION_XML));
		this.handlerMapping.getHandler(exchange).block();

		String name = HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;
		assertThat(exchange.getAttributes().get(name)).isEqualTo(Collections.singleton(MediaType.APPLICATION_XML));

		exchange = MockServerWebExchange.from(get("/content").accept(MediaType.APPLICATION_JSON));
		this.handlerMapping.getHandler(exchange).block();

		assertThat(exchange.getAttributes().get(name))
				.as("Negated expression shouldn't be listed as producible type")
				.isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	void handleMatchUriTemplateVariables() {
		ServerWebExchange exchange = MockServerWebExchange.from(get("/1/2"));

		RequestMappingInfo key = paths("/{path1}/{path2}").build();
		this.handlerMapping.handleMatch(key, handlerMethod, exchange);

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		Map<String, String> uriVariables = (Map<String, String>) exchange.getAttributes().get(name);

		assertThat(uriVariables).isNotNull();
		assertThat(uriVariables.get("path1")).isEqualTo("1");
		assertThat(uriVariables.get("path2")).isEqualTo("2");
	}

	@Test  // SPR-9098
	void handleMatchUriTemplateVariablesDecode() {
		RequestMappingInfo key = paths("/{group}/{identifier}").build();
		URI url = URI.create("/group/a%2Fb");
		ServerWebExchange exchange = MockServerWebExchange.from(method(HttpMethod.GET, url));

		this.handlerMapping.handleMatch(key, handlerMethod, exchange);

		String name = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;
		@SuppressWarnings("unchecked")
		Map<String, String> uriVariables = (Map<String, String>) exchange.getAttributes().get(name);

		assertThat(uriVariables).isNotNull();
		assertThat(uriVariables.get("group")).isEqualTo("group");
		assertThat(uriVariables.get("identifier")).isEqualTo("a/b");
	}

	@Test
	void handleMatchBestMatchingPatternAttribute() {
		RequestMappingInfo key = paths("/{path1}/2", "/**").build();
		ServerWebExchange exchange = MockServerWebExchange.from(get("/1/2"));
		this.handlerMapping.handleMatch(key, handlerMethod, exchange);

		PathPattern bestMatch = (PathPattern) exchange.getAttributes().get(BEST_MATCHING_PATTERN_ATTRIBUTE);
		assertThat(bestMatch.getPatternString()).isEqualTo("/{path1}/2");

		HandlerMethod mapped = (HandlerMethod) exchange.getAttributes().get(BEST_MATCHING_HANDLER_ATTRIBUTE);
		assertThat(mapped).isSameAs(handlerMethod);
	}

	@Test
	void handleMatchBestMatchingPatternAttributeInObservationContext() {
		RequestMappingInfo key = paths("/{path1}/2", "/**").build();
		ServerWebExchange exchange = MockServerWebExchange.from(get("/1/2"));
		ServerRequestObservationContext observationContext = new ServerRequestObservationContext(exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		exchange.getAttributes().put(ServerRequestObservationContext.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observationContext);
		this.handlerMapping.handleMatch(key, handlerMethod, exchange);

		assertThat(observationContext.getPathPattern()).isEqualTo("/{path1}/2");
	}

	@Test // gh-22543
	void handleMatchBestMatchingPatternAttributeNoPatternsDefined() {
		ServerWebExchange exchange = MockServerWebExchange.from(get(""));
		this.handlerMapping.handleMatch(paths().build(), handlerMethod, exchange);
		PathPattern pattern = (PathPattern) exchange.getAttributes().get(BEST_MATCHING_PATTERN_ATTRIBUTE);
		assertThat(pattern.getPatternString()).isEmpty();
	}

	@Test
	void handleMatchMatrixVariables() {
		MultiValueMap<String, String> matrixVariables;
		Map<String, String> uriVariables;

		ServerWebExchange exchange = MockServerWebExchange.from(get("/cars;colors=red,blue,green;year=2012"));
		handleMatch(exchange, "/{cars}");

		matrixVariables = getMatrixVariables(exchange, "cars");
		uriVariables = getUriTemplateVariables(exchange);

		assertThat(matrixVariables).isNotNull();
		assertThat(matrixVariables.get("colors")).isEqualTo(Arrays.asList("red", "blue", "green"));
		assertThat(matrixVariables.getFirst("year")).isEqualTo("2012");
		assertThat(uriVariables.get("cars")).isEqualTo("cars");

		// SPR-11897
		exchange = MockServerWebExchange.from(get("/a=42;b=c"));
		handleMatch(exchange, "/{foo}");

		matrixVariables = getMatrixVariables(exchange, "foo");
		uriVariables = getUriTemplateVariables(exchange);

		// Unlike Spring MVC, WebFlux currently does not support APIs like
		// "/foo/{ids}" and URL "/foo/id=1;id=2;id=3" where the whole path
		// segment is a sequence of name-value pairs.

		assertThat(matrixVariables).isNotNull();
		assertThat(matrixVariables).hasSize(1);
		assertThat(matrixVariables.getFirst("b")).isEqualTo("c");
		assertThat(uriVariables.get("foo")).isEqualTo("a=42");
	}

	@Test
	void handleMatchMatrixVariablesDecoding() {
		MockServerHttpRequest request = method(HttpMethod.GET, URI.create("/cars;mvar=a%2Fb")).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		handleMatch(exchange, "/{cars}");

		MultiValueMap<String, String> matrixVariables = getMatrixVariables(exchange, "cars");
		Map<String, String> uriVariables = getUriTemplateVariables(exchange);

		assertThat(matrixVariables).isNotNull();
		assertThat(matrixVariables.get("mvar")).isEqualTo(Collections.singletonList("a/b"));
		assertThat(uriVariables.get("cars")).isEqualTo("cars");
	}

	@Test
	void handlePatchUnsupportedMediaType() {
		MockServerHttpRequest request = MockServerHttpRequest.patch("/qux")
				.header("content-type", "application/xml")
				.build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		StepVerifier.create(mono)
				.expectErrorSatisfies(ex -> {
					assertThat(ex).isInstanceOf(UnsupportedMediaTypeStatusException.class);
					UnsupportedMediaTypeStatusException umtse = (UnsupportedMediaTypeStatusException) ex;
					MediaType mediaType = new MediaType("foo", "bar");
					assertThat(umtse.getSupportedMediaTypes()).containsExactly(mediaType);
					assertThat(umtse.getHeaders().getAcceptPatch()).containsExactly(mediaType);
				})
				.verify();

	}

	@Test // gh-29611
	void handleNoMatchWithoutPartialMatches() throws Exception {
		ServerWebExchange exchange = MockServerWebExchange.from(post("/non-existent"));

		HandlerMethod handlerMethod = this.handlerMapping.handleNoMatch(new HashSet<>(), exchange);
		assertThat(handlerMethod).isNull();

		handlerMethod = this.handlerMapping.handleNoMatch(null, exchange);
		assertThat(handlerMethod).isNull();
	}


	@SuppressWarnings("unchecked")
	private <T> void assertError(Mono<Object> mono, final Class<T> exceptionClass, final Consumer<T> consumer) {
		StepVerifier.create(mono)
				.consumeErrorWith(error -> {
					assertThat(error.getClass()).isEqualTo(exceptionClass);
					consumer.accept((T) error);
				})
				.verify();
	}

	private void testHttpMediaTypeNotSupportedException(String url) {
		MockServerHttpRequest request = put(url).contentType(MediaType.APPLICATION_JSON).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, UnsupportedMediaTypeStatusException.class, ex -> assertThat(ex.getSupportedMediaTypes())
				.as("Invalid supported consumable media types")
				.isEqualTo(Collections.singletonList(new MediaType("application", "xml"))));
	}

	private void testHttpOptions(String requestURI, Set<HttpMethod> allowedMethods, @Nullable MediaType acceptPatch) {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options(requestURI));
		HandlerMethod handlerMethod = (HandlerMethod) this.handlerMapping.getHandler(exchange).block();

		BindingContext bindingContext = new BindingContext();
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handlerMethod);
		Mono<HandlerResult> mono = invocable.invoke(exchange, bindingContext);

		HandlerResult result = mono.block();
		assertThat(result).isNotNull();

		Object value = result.getReturnValue();
		assertThat(value).isNotNull();
		assertThat(value.getClass()).isEqualTo(HttpHeaders.class);

		HttpHeaders headers = (HttpHeaders) value;
		assertThat(headers.getAllow()).hasSameElementsAs(allowedMethods);

		if (acceptPatch != null && headers.getAllow().contains(HttpMethod.PATCH) ) {
			assertThat(headers.getAcceptPatch()).containsExactly(acceptPatch);
		}
	}

	private void testMediaTypeNotAcceptable(String url) {
		ServerWebExchange exchange = MockServerWebExchange.from(get(url).accept(MediaType.APPLICATION_JSON));
		Mono<Object> mono = this.handlerMapping.getHandler(exchange);

		assertError(mono, NotAcceptableStatusException.class, ex -> assertThat(ex.getSupportedMediaTypes())
				.as("Invalid supported producible media types")
				.isEqualTo(Collections.singletonList(new MediaType("application", "xml"))));
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

		@RequestMapping(value = "/qux", method = RequestMethod.GET, produces = "application/xml")
		public String getBaz() {
			return "";
		}

		@RequestMapping(value = "/qux", method = RequestMethod.PATCH, consumes = "foo/bar")
		public void patchBaz(String value) {
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
