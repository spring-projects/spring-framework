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

package org.springframework.web.reactive.result.view;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import rx.Completable;
import rx.Observable;
import rx.Single;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.method.ResolvableMethod.on;

/**
 * ViewResolutionResultHandler relying on a canned {@link TestViewResolver}
 * or a (Mockito) "mock".
 *
 * @author Rossen Stoyanchev
 */
public class ViewResolutionResultHandlerTests {

	private MockServerHttpRequest request;

	private final BindingContext bindingContext = new BindingContext();


	@Before
	public void setup() throws Exception {
		this.request = MockServerHttpRequest.get("/path").build();
	}


	@Test
	public void supports() throws Exception {

		testSupports(on(TestController.class).resolveReturnType(String.class));
		testSupports(on(TestController.class).resolveReturnType(View.class));
		testSupports(on(TestController.class).resolveReturnType(Mono.class, String.class));
		testSupports(on(TestController.class).resolveReturnType(Mono.class, View.class));
		testSupports(on(TestController.class).resolveReturnType(Single.class, String.class));
		testSupports(on(TestController.class).resolveReturnType(Single.class, View.class));
		testSupports(on(TestController.class).resolveReturnType(Mono.class, Void.class));
		testSupports(on(TestController.class).resolveReturnType(Completable.class));
		testSupports(on(TestController.class).resolveReturnType(Model.class));
		testSupports(on(TestController.class).resolveReturnType(Map.class));
		testSupports(on(TestController.class).resolveReturnType(TestBean.class));

		testSupports(on(TestController.class).annotated(ModelAttribute.class).resolveReturnType());
	}

	private void testSupports(MethodParameter returnType) {
		ViewResolutionResultHandler resultHandler = resultHandler(mock(ViewResolver.class));
		HandlerResult handlerResult = new HandlerResult(new Object(), null, returnType, this.bindingContext);
		assertTrue(resultHandler.supports(handlerResult));
	}

	@Test
	public void doesNotSupport() throws Exception {
		MethodParameter returnType = on(TestController.class).resolveReturnType(Integer.class);
		ViewResolutionResultHandler resultHandler = resultHandler(mock(ViewResolver.class));
		HandlerResult handlerResult = new HandlerResult(new Object(), null, returnType, this.bindingContext);
		assertFalse(resultHandler.supports(handlerResult));
	}

	@Test
	public void viewResolverOrder() throws Exception {
		TestViewResolver resolver1 = new TestViewResolver("account");
		TestViewResolver resolver2 = new TestViewResolver("profile");
		resolver1.setOrder(2);
		resolver2.setOrder(1);
		List<ViewResolver> resolvers = resultHandler(resolver1, resolver2).getViewResolvers();

		assertEquals(Arrays.asList(resolver2, resolver1), resolvers);
	}

	@Test
	public void handleReturnValueTypes() throws Exception {

		Object returnValue;
		MethodParameter returnType;
		ViewResolver resolver = new TestViewResolver("account");

		returnType = on(TestController.class).resolveReturnType(View.class);
		returnValue = new TestView("account");
		testHandle("/path", returnType, returnValue, "account: {id=123}");

		returnType = on(TestController.class).resolveReturnType(Mono.class, View.class);
		returnValue = Mono.just(new TestView("account"));
		testHandle("/path", returnType, returnValue, "account: {id=123}");

		returnType = on(TestController.class).resolveReturnType(String.class);
		returnValue = "account";
		testHandle("/path", returnType, returnValue, "account: {id=123}", resolver);

		returnType = on(TestController.class).resolveReturnType(Mono.class, String.class);
		returnValue = Mono.just("account");
		testHandle("/path", returnType, returnValue, "account: {id=123}", resolver);

		returnType = on(TestController.class).resolveReturnType(Model.class);
		returnValue = new ConcurrentModel().addAttribute("name", "Joe");
		testHandle("/account", returnType, returnValue, "account: {id=123, name=Joe}", resolver);

		returnType = on(TestController.class).resolveReturnType(Map.class);
		returnValue = Collections.singletonMap("name", "Joe");
		testHandle("/account", returnType, returnValue, "account: {id=123, name=Joe}", resolver);

		returnType = on(TestController.class).resolveReturnType(TestBean.class);
		returnValue = new TestBean("Joe");
		String responseBody = "account: {" +
				"id=123, " +
				"org.springframework.validation.BindingResult.testBean=" +
				"org.springframework.validation.BeanPropertyBindingResult: 0 errors, " +
				"testBean=TestBean[name=Joe]" +
				"}";
		testHandle("/account", returnType, returnValue, responseBody, resolver);

		returnType = on(TestController.class).annotated(ModelAttribute.class).resolveReturnType();
		testHandle("/account", returnType, 99L, "account: {id=123, num=99}", resolver);
	}

	@Test
	public void handleWithMultipleResolvers() throws Exception {
		Object returnValue = "profile";
		MethodParameter returnType = on(TestController.class).resolveReturnType(String.class);
		ViewResolver[] resolvers = {new TestViewResolver("account"), new TestViewResolver("profile")};

		testHandle("/account", returnType, returnValue, "profile: {id=123}", resolvers);
	}

	@Test
	public void defaultViewName() throws Exception {
		testDefaultViewName(null, on(TestController.class).resolveReturnType(String.class));
		testDefaultViewName(Mono.empty(), on(TestController.class).resolveReturnType(Mono.class, String.class));
		testDefaultViewName(Mono.empty(), on(TestController.class).resolveReturnType(Mono.class, Void.class));
		testDefaultViewName(Completable.complete(), on(TestController.class).resolveReturnType(Completable.class));
	}

	private void testDefaultViewName(Object returnValue, MethodParameter returnType) throws URISyntaxException {
		this.bindingContext.getModel().addAttribute("id", "123");
		HandlerResult result = new HandlerResult(new Object(), returnValue, returnType, this.bindingContext);
		ViewResolutionResultHandler handler = resultHandler(new TestViewResolver("account"));

		this.request = MockServerHttpRequest.get("/account").build();
		ServerWebExchange exchange = createExchange();
		handler.handleResult(exchange, result).blockMillis(5000);
		assertResponseBody(exchange, "account: {id=123}");

		this.request = MockServerHttpRequest.get("/account/").build();
		exchange = createExchange();
		handler.handleResult(exchange, result).blockMillis(5000);
		assertResponseBody(exchange, "account: {id=123}");

		this.request = MockServerHttpRequest.get("/account.123").build();
		exchange = createExchange();
		handler.handleResult(exchange, result).blockMillis(5000);
		assertResponseBody(exchange, "account: {id=123}");
	}

	@Test
	public void unresolvedViewName() throws Exception {
		String returnValue = "account";
		MethodParameter returnType = on(TestController.class).resolveReturnType(String.class);
		HandlerResult result = new HandlerResult(new Object(), returnValue, returnType, this.bindingContext);

		this.request = MockServerHttpRequest.get("/path").build();
		ServerWebExchange exchange = createExchange();
		Mono<Void> mono = resultHandler().handleResult(exchange, result);

		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectErrorMessage("Could not resolve view with name 'account'.")
				.verify();
	}

	@Test
	public void contentNegotiation() throws Exception {
		TestBean value = new TestBean("Joe");
		MethodParameter returnType = on(TestController.class).resolveReturnType(TestBean.class);
		HandlerResult handlerResult = new HandlerResult(new Object(), value, returnType, this.bindingContext);

		this.request = MockServerHttpRequest.get("/account").accept(APPLICATION_JSON).build();
		ServerWebExchange exchange = createExchange();

		TestView defaultView = new TestView("jsonView", APPLICATION_JSON);

		resultHandler(Collections.singletonList(defaultView), new TestViewResolver("account"))
				.handleResult(exchange, handlerResult)
				.block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON, exchange.getResponse().getHeaders().getContentType());
		assertResponseBody(exchange, "jsonView: {" +
				"org.springframework.validation.BindingResult.testBean=" +
				"org.springframework.validation.BeanPropertyBindingResult: 0 errors, " +
				"testBean=TestBean[name=Joe]" +
				"}");
	}

	@Test
	public void contentNegotiationWith406() throws Exception {
		TestBean value = new TestBean("Joe");
		MethodParameter returnType = on(TestController.class).resolveReturnType(TestBean.class);
		HandlerResult handlerResult = new HandlerResult(new Object(), value, returnType, this.bindingContext);

		this.request = MockServerHttpRequest.get("/account").accept(APPLICATION_JSON).build();
		ServerWebExchange exchange = createExchange();

		ViewResolutionResultHandler resultHandler = resultHandler(new TestViewResolver("account"));
		Mono<Void> mono = resultHandler.handleResult(exchange, handlerResult);
		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(NotAcceptableStatusException.class)
				.verify();
	}

	@Test
	public void modelWithAsyncAttributes() throws Exception {
		this.bindingContext.getModel()
				.addAttribute("attr1", Mono.just(new TestBean("Bean1")))
				.addAttribute("attr2", Flux.just(new TestBean("Bean1"), new TestBean("Bean2")))
				.addAttribute("attr3", Single.just(new TestBean("Bean2")))
				.addAttribute("attr4", Observable.just(new TestBean("Bean1"), new TestBean("Bean2")))
				.addAttribute("attr5", Mono.empty());

		MethodParameter returnType = on(TestController.class).resolveReturnType(void.class);
		HandlerResult result = new HandlerResult(new Object(), null, returnType, this.bindingContext);
		ViewResolutionResultHandler handler = resultHandler(new TestViewResolver("account"));

		this.request = MockServerHttpRequest.get("/account").build();
		ServerWebExchange exchange = createExchange();

		handler.handleResult(exchange, result).blockMillis(5000);
		assertResponseBody(exchange, "account: {" +
				"attr1=TestBean[name=Bean1], " +
				"attr2=[TestBean[name=Bean1], TestBean[name=Bean2]], " +
				"attr3=TestBean[name=Bean2], " +
				"attr4=[TestBean[name=Bean1], TestBean[name=Bean2]], " +
				"org.springframework.validation.BindingResult.attr1=" +
				"org.springframework.validation.BeanPropertyBindingResult: 0 errors, " +
				"org.springframework.validation.BindingResult.attr3=" +
				"org.springframework.validation.BeanPropertyBindingResult: 0 errors" +
				"}");
	}


	private ServerWebExchange createExchange() {
		return new DefaultServerWebExchange(this.request, new MockServerHttpResponse());
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
			String responseBody, ViewResolver... resolvers) throws URISyntaxException {

		Model model = this.bindingContext.getModel();
		model.asMap().clear();
		model.addAttribute("id", "123");
		HandlerResult result = new HandlerResult(new Object(), returnValue, returnType, this.bindingContext);
		this.request = MockServerHttpRequest.get(path).build();
		ServerWebExchange exchange = createExchange();
		resultHandler(resolvers).handleResult(exchange, result).block(Duration.ofSeconds(5));
		assertResponseBody(exchange, responseBody);
		return exchange;
	}

	private void assertResponseBody(ServerWebExchange exchange, String responseBody) {
		MockServerHttpResponse response = (MockServerHttpResponse) exchange.getResponse();
		StepVerifier.create(response.getBody())
				.consumeNextWith(buf -> assertEquals(responseBody, DataBufferTestUtils.dumpString(buf, UTF_8)))
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

		public String getName() {
			return this.name;
		}

		@Override
		public List<MediaType> getSupportedMediaTypes() {
			return this.mediaTypes;
		}

		@Override
		public Mono<Void> render(Map<String, ?> model, MediaType mediaType, ServerWebExchange exchange) {
			ServerHttpResponse response = exchange.getResponse();
			if (mediaType != null) {
				response.getHeaders().setContentType(mediaType);
			}
			model = new TreeMap<>(model);
			String value = this.name + ": " + model.toString();
			ByteBuffer byteBuffer = ByteBuffer.wrap(value.getBytes(UTF_8));
			DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(byteBuffer);
			return response.writeWith(Flux.just(dataBuffer));
		}
	}


	private static class TestBean {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public String toString() {
			return "TestBean[name=" + this.name + "]";
		}
	}


	@SuppressWarnings("unused")
	private static class TestController {

		String string() {
			return null;
		}

		View view() {
			return null;
		}

		Mono<String> monoString() {
			return null;
		}

		Mono<View> monoView() {
			return null;
		}

		Mono<Void> monoVoid() {
			return null;
		}

		void voidMethod() {
		}

		Single<String> singleString() {
			return null;
		}

		Single<View> singleView() {
			return null;
		}

		Completable completable() {
			return null;
		}

		Model model() {
			return null;
		}

		Map map() {
			return null;
		}

		TestBean testBean() {
			return null;
		}

		Integer integer() {
			return null;
		}

		@ModelAttribute("num")
		Long longAttribute() {
			return null;
		}
	}

}
