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

package org.springframework.web.reactive.result.view;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import io.reactivex.rxjava3.core.Completable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;
import static org.springframework.web.testfixture.method.ResolvableMethod.on;

/**
 * ViewResolutionResultHandler relying on a canned {@link TestViewResolver}
 * or a (Mockito) "mock".
 *
 * @author Rossen Stoyanchev
 */
class ViewResolutionResultHandlerTests {

	private final BindingContext bindingContext = new BindingContext();


	@Test
	void supports() {
		testSupports(on(Handler.class).annotPresent(ModelAttribute.class).resolveReturnType(String.class));
		testSupports(on(Handler.class).annotNotPresent(ModelAttribute.class).resolveReturnType(String.class));
		testSupports(on(Handler.class).resolveReturnType(Mono.class, String.class));

		testSupports(on(Handler.class).resolveReturnType(Rendering.class));
		testSupports(on(Handler.class).resolveReturnType(Mono.class, Rendering.class));

		testSupports(on(Handler.class).resolveReturnType(FragmentsRendering.class));
		testSupports(on(Handler.class).resolveReturnType(Flux.class, Fragment.class));
		testSupports(on(Handler.class).resolveReturnType(List.class, Fragment.class));
		testSupports(on(Handler.class).resolveReturnType(
				Mono.class, ResolvableType.forClassWithGenerics(List.class, Fragment.class)));

		testSupports(on(Handler.class).resolveReturnType(View.class));
		testSupports(on(Handler.class).resolveReturnType(Mono.class, View.class));

		testSupports(on(Handler.class).resolveReturnType(void.class));
		testSupports(on(Handler.class).resolveReturnType(Mono.class, Void.class));
		testSupports(on(Handler.class).resolveReturnType(Completable.class));

		testSupports(on(Handler.class).resolveReturnType(Model.class));

		testSupports(on(Handler.class).annotPresent(ModelAttribute.class).resolveReturnType(Map.class));
		testSupports(on(Handler.class).annotNotPresent(ModelAttribute.class).resolveReturnType(Map.class));

		testSupports(on(Handler.class).resolveReturnType(TestBean.class));

		testSupports(on(Handler.class).annotPresent(ModelAttribute.class).resolveReturnType(Long.class));
		testDoesNotSupport(on(Handler.class).annotNotPresent(ModelAttribute.class).resolveReturnType(Long.class));

		// SPR-15464
		testSupports(on(Handler.class).resolveReturnType(Mono.class));
	}

	private void testSupports(MethodParameter returnType) {
		testSupports(returnType, true);
	}

	private void testDoesNotSupport(MethodParameter returnType) {
		testSupports(returnType, false);
	}

	private void testSupports(MethodParameter returnType, boolean supports) {
		ViewResolutionResultHandler resultHandler = resultHandler(mock(ViewResolver.class));
		HandlerResult handlerResult = new HandlerResult(new Object(), null, returnType, this.bindingContext);
		if (supports) {
			assertThat(resultHandler.supports(handlerResult)).as("return type [" + returnType + "] should be supported").isTrue();
		}
		else {
			assertThat(resultHandler.supports(handlerResult)).as("return type [" + returnType + "] should not be supported").isFalse();
		}
	}

	@Test
	void viewResolverOrder() {
		TestViewResolver resolver1 = new TestViewResolver("account");
		TestViewResolver resolver2 = new TestViewResolver("profile");
		resolver1.setOrder(2);
		resolver2.setOrder(1);
		List<ViewResolver> resolvers = resultHandler(resolver1, resolver2).getViewResolvers();

		assertThat(resolvers).isEqualTo(Arrays.asList(resolver2, resolver1));
	}

	@Test
	void handleReturnValueTypes() {
		Object returnValue;
		MethodParameter returnType;
		ViewResolver resolver = new TestViewResolver("account");

		returnType = on(Handler.class).resolveReturnType(View.class);
		returnValue = new TestView("account");
		testHandle("/path", returnType, returnValue, "account: {id=123}");

		returnType = on(Handler.class).resolveReturnType(Mono.class, View.class);
		returnValue = Mono.just(new TestView("account"));
		testHandle("/path", returnType, returnValue, "account: {id=123}");

		returnType = on(Handler.class).annotNotPresent(ModelAttribute.class).resolveReturnType(String.class);
		returnValue = "account";
		testHandle("/path", returnType, returnValue, "account: {id=123}", resolver);

		returnType = on(Handler.class).annotPresent(ModelAttribute.class).resolveReturnType(String.class);
		returnValue = "123";
		testHandle("/account", returnType, returnValue, "account: {id=123, myString=123}", resolver);

		returnType = on(Handler.class).resolveReturnType(Mono.class, String.class);
		returnValue = Mono.just("account");
		testHandle("/path", returnType, returnValue, "account: {id=123}", resolver);

		returnType = on(Handler.class).resolveReturnType(Model.class);
		returnValue = new ConcurrentModel().addAttribute("name", "Joe").addAttribute("ignore", null);
		testHandle("/account", returnType, returnValue, "account: {id=123, name=Joe}", resolver);

		// Work around  caching issue...
		ResolvableType.clearCache();

		returnType = on(Handler.class).annotNotPresent(ModelAttribute.class).resolveReturnType(Map.class);
		returnValue = Collections.singletonMap("name", "Joe");
		testHandle("/account", returnType, returnValue, "account: {id=123, name=Joe}", resolver);

		// Work around  caching issue...
		ResolvableType.clearCache();

		returnType = on(Handler.class).annotPresent(ModelAttribute.class).resolveReturnType(Map.class);
		returnValue = Collections.singletonMap("name", "Joe");
		testHandle("/account", returnType, returnValue, "account: {id=123, myMap={name=Joe}}", resolver);

		returnType = on(Handler.class).resolveReturnType(TestBean.class);
		returnValue = new TestBean("Joe");
		String responseBody = "account: {id=123, " +
				"org.springframework.validation.BindingResult.testBean=" +
				"org.springframework.validation.BeanPropertyBindingResult: 0 errors, " +
				"testBean=TestBean[name=Joe]}";
		testHandle("/account", returnType, returnValue, responseBody, resolver);

		returnType = on(Handler.class).annotPresent(ModelAttribute.class).resolveReturnType(Long.class);
		testHandle("/account", returnType, 99L, "account: {id=123, myLong=99}", resolver);

		returnType = on(Handler.class).resolveReturnType(Rendering.class);
		HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
		returnValue = Rendering.view("account").modelAttribute("a", "a1").status(status).header("h", "h1").build();
		String expected = "account: {a=a1, id=123}";
		ServerWebExchange exchange = testHandle("/path", returnType, returnValue, expected, resolver);
		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(status);
		assertThat(exchange.getResponse().getHeaders().getFirst("h")).isEqualTo("h1");
	}

	@Test
	void handleWithMultipleResolvers() {
		testHandle("/account",
				on(Handler.class).annotNotPresent(ModelAttribute.class).resolveReturnType(String.class),
				"profile", "profile: {id=123}",
				new TestViewResolver("account"), new TestViewResolver("profile"));
	}

	@Test
	void defaultViewName() {
		testDefaultViewName(null, on(Handler.class).annotPresent(ModelAttribute.class).resolveReturnType(String.class));
		testDefaultViewName(Mono.empty(), on(Handler.class).resolveReturnType(Mono.class, String.class));
		testDefaultViewName(Mono.empty(), on(Handler.class).resolveReturnType(Mono.class, Void.class));
		testDefaultViewName(Completable.complete(), on(Handler.class).resolveReturnType(Completable.class));
	}

	private void testDefaultViewName(Object returnValue, MethodParameter returnType) {
		this.bindingContext.getModel().addAttribute("id", "123");
		HandlerResult result = new HandlerResult(new Object(), returnValue, returnType, this.bindingContext);
		ViewResolutionResultHandler handler = resultHandler(new TestViewResolver("account"));

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/account"));
		handler.handleResult(exchange, result).block(Duration.ofMillis(5000));
		assertResponseBody(exchange, "account: {id=123}");

		exchange = MockServerWebExchange.from(get("/account/"));
		handler.handleResult(exchange, result).block(Duration.ofMillis(5000));
		assertResponseBody(exchange, "account: {id=123}");

		exchange = MockServerWebExchange.from(get("/account.123"));
		handler.handleResult(exchange, result).block(Duration.ofMillis(5000));
		assertResponseBody(exchange, "account: {id=123}");
	}

	@Test
	void unresolvedViewName() {
		String returnValue = "account";
		MethodParameter returnType = on(Handler.class).annotPresent(ModelAttribute.class).resolveReturnType(String.class);
		HandlerResult result = new HandlerResult(new Object(), returnValue, returnType, this.bindingContext);

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		Mono<Void> mono = resultHandler().handleResult(exchange, result);

		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectErrorMessage("Could not resolve view with name 'path'.")
				.verify();
	}

	@Test
	void contentNegotiation() {
		TestBean value = new TestBean("Joe");
		MethodParameter returnType = on(Handler.class).resolveReturnType(TestBean.class);
		HandlerResult handlerResult = new HandlerResult(new Object(), value, returnType, this.bindingContext);

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/account").accept(APPLICATION_JSON));

		TestView defaultView = new TestView("jsonView", APPLICATION_JSON);

		resultHandler(Collections.singletonList(defaultView), new TestViewResolver("account"))
				.handleResult(exchange, handlerResult)
				.block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(APPLICATION_JSON);
		assertResponseBody(exchange, "jsonView: {" +
				"org.springframework.validation.BindingResult.testBean=" +
				"org.springframework.validation.BeanPropertyBindingResult: 0 errors, " +
				"testBean=TestBean[name=Joe]" +
				"}");
	}

	@Test
	void contentNegotiationWith406() {
		TestBean value = new TestBean("Joe");
		MethodParameter returnType = on(Handler.class).resolveReturnType(TestBean.class);
		HandlerResult handlerResult = new HandlerResult(new Object(), value, returnType, this.bindingContext);

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/account").accept(APPLICATION_JSON));

		ViewResolutionResultHandler resultHandler = resultHandler(new TestViewResolver("account"));
		Mono<Void> mono = resultHandler.handleResult(exchange, handlerResult);
		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(NotAcceptableStatusException.class)
				.verify();
	}

	@Test  // SPR-15291
	void contentNegotiationWithRedirect() {
		HandlerResult handlerResult = new HandlerResult(new Object(), "redirect:/",
				on(Handler.class).annotNotPresent(ModelAttribute.class).resolveReturnType(String.class),
				this.bindingContext);

		UrlBasedViewResolver viewResolver = new UrlBasedViewResolver();
		viewResolver.setApplicationContext(new StaticApplicationContext());
		ViewResolutionResultHandler resultHandler = resultHandler(viewResolver);

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/account").accept(APPLICATION_JSON));
		resultHandler.handleResult(exchange, handlerResult).block(Duration.ZERO);

		MockServerHttpResponse response = exchange.getResponse();
		assertThat(response.getStatusCode().value()).isEqualTo(303);
		assertThat(response.getHeaders().getLocation().toString()).isEqualTo("/");
	}


	private ViewResolutionResultHandler resultHandler(ViewResolver... resolvers) {
		return resultHandler(Collections.emptyList(), resolvers);
	}

	private ViewResolutionResultHandler resultHandler(List<View> defaultViews, ViewResolver... resolvers) {
		List<ViewResolver> resolverList = Arrays.asList(resolvers);
		RequestedContentTypeResolver contentTypeResolver = new HeaderContentTypeResolver();
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolverList, contentTypeResolver);
		handler.setDefaultViews(defaultViews);
		return handler;
	}

	private ServerWebExchange testHandle(String path, MethodParameter returnType, Object returnValue,
			String responseBody, ViewResolver... resolvers) {

		Model model = this.bindingContext.getModel();
		model.asMap().clear();
		model.addAttribute("id", "123");
		HandlerResult result = new HandlerResult(new Object(), returnValue, returnType, this.bindingContext);
		MockServerWebExchange exchange = MockServerWebExchange.from(get(path));
		resultHandler(resolvers).handleResult(exchange, result).block(Duration.ofSeconds(5));
		assertResponseBody(exchange, responseBody);
		return exchange;
	}

	private void assertResponseBody(MockServerWebExchange exchange, String responseBody) {
		StepVerifier.create(exchange.getResponse().getBody())
				.consumeNextWith(buf -> assertThat(buf.toString(UTF_8)).isEqualTo(responseBody))
				.expectComplete()
				.verify();
	}


	private static class TestViewResolver implements ViewResolver, Ordered {

		private final Map<String, View> views = new HashMap<>();

		private int order = Ordered.LOWEST_PRECEDENCE;

		TestViewResolver(String... viewNames) {
			Arrays.stream(viewNames).forEach(name -> this.views.put(name, new TestView(name)));
		}

		void setOrder(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public Mono<View> resolveViewName(String viewName, Locale locale) {
			View view = this.views.get(viewName);
			return Mono.justOrEmpty(view);
		}
	}


	private static final class TestView implements View {

		private final String name;

		private final List<MediaType> mediaTypes;


		TestView(String name) {
			this.name = name;
			this.mediaTypes = Collections.singletonList(MediaType.TEXT_HTML);
		}

		TestView(String name, MediaType... mediaTypes) {
			this.name = name;
			this.mediaTypes = Arrays.asList(mediaTypes);
		}

		@SuppressWarnings("unused")
		public String getName() {
			return this.name;
		}

		@Override
		public List<MediaType> getSupportedMediaTypes() {
			return this.mediaTypes;
		}

		@Override
		public Mono<Void> render(@Nullable Map<String, ?> model, @Nullable MediaType mediaType, ServerWebExchange exchange) {
			ServerHttpResponse response = exchange.getResponse();
			if (mediaType != null) {
				response.getHeaders().setContentType(mediaType);
			}
			model = new TreeMap<>(model);
			String value = this.name + ": " + model;
			ByteBuffer byteBuffer = ByteBuffer.wrap(value.getBytes(UTF_8));
			DataBuffer dataBuffer = DefaultDataBufferFactory.sharedInstance.wrap(byteBuffer);
			return response.writeWith(Flux.just(dataBuffer));
		}
	}


	private static class TestBean {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}

		@SuppressWarnings("unused")
		public String getName() {
			return this.name;
		}

		@Override
		public String toString() {
			return "TestBean[name=" + this.name + "]";
		}
	}


	@SuppressWarnings("unused")
	private static class Handler {

		String string() { return null; }
		Mono<String> monoString() { return null; }
		@ModelAttribute("myString") String stringWithAnnotation() { return null; }

		Rendering rendering() { return null; }
		Mono<Rendering> monoRendering() { return null; }

		FragmentsRendering fragmentsRendering() { return null; }
		Flux<Fragment> fragmentFlux() { return null; }
		Mono<List<Fragment>> monoFragmentList() { return null; }
		List<Fragment> fragmentList() { return null; }

		View view() { return null; }
		Mono<View> monoView() { return null; }

		void voidMethod() { }
		Mono<Void> monoVoid() { return null; }
		Completable completable() { return null; }

		Model model() { return null; }

		Map<?,?> map() { return null; }
		@ModelAttribute("myMap") Map<?,?> mapWithAnnotation() { return null; }

		TestBean testBean() { return null; }

		Long longValue() { return null; }
		@ModelAttribute("myLong") Long longModelAttribute() { return null; }

		Mono<?> monoWildcard() { return null; }
	}

}
