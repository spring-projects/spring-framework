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
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
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
import org.springframework.web.reactive.result.ResolvableMethod;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.springframework.core.ResolvableType.forClass;
import static org.springframework.core.ResolvableType.forClassWithGenerics;
import static org.springframework.core.io.buffer.support.DataBufferTestUtils.dumpString;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Unit tests for {@link ViewResolutionResultHandler}.
 * @author Rossen Stoyanchev
 */
public class ViewResolutionResultHandlerTests {

	private MockServerHttpRequest request;

	private final BindingContext bindingContext = new BindingContext();


	@Before
	public void setUp() throws Exception {
		this.request = MockServerHttpRequest.get("/path").build();
	}


	@Test
	public void supports() throws Exception {
		testSupports(forClass(String.class), true);
		testSupports(forClass(View.class), true);
		testSupports(forClassWithGenerics(Mono.class, String.class), true);
		testSupports(forClassWithGenerics(Mono.class, View.class), true);
		testSupports(forClassWithGenerics(Single.class, String.class), true);
		testSupports(forClassWithGenerics(Single.class, View.class), true);
		testSupports(forClassWithGenerics(Mono.class, Void.class), true);
		testSupports(forClass(Completable.class), true);
		testSupports(forClass(Model.class), true);
		testSupports(forClass(Map.class), true);
		testSupports(forClass(TestBean.class), true);
		testSupports(forClass(Integer.class), false);
		testSupports(resolvableMethod().annotated(ModelAttribute.class), true);
	}

	private void testSupports(ResolvableType type, boolean result) {
		testSupports(resolvableMethod().returning(type), result);
	}

	private void testSupports(ResolvableMethod resolvableMethod, boolean result) {
		ViewResolutionResultHandler resultHandler = resultHandler(mock(ViewResolver.class));
		MethodParameter returnType = resolvableMethod.resolveReturnType();
		HandlerResult handlerResult = new HandlerResult(new Object(), null, returnType, this.bindingContext);
		assertEquals(result, resultHandler.supports(handlerResult));
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
		ResolvableType returnType;
		ViewResolver resolver = new TestViewResolver("account");

		returnType = forClass(View.class);
		returnValue = new TestView("account");
		testHandle("/path", returnType, returnValue, "account: {id=123}");

		returnType = forClassWithGenerics(Mono.class, View.class);
		returnValue = Mono.just(new TestView("account"));
		testHandle("/path", returnType, returnValue, "account: {id=123}");

		returnType = forClass(String.class);
		returnValue = "account";
		testHandle("/path", returnType, returnValue, "account: {id=123}", resolver);

		returnType = forClassWithGenerics(Mono.class, String.class);
		returnValue = Mono.just("account");
		testHandle("/path", returnType, returnValue, "account: {id=123}", resolver);

		returnType = forClass(Model.class);
		returnValue = new ConcurrentModel().addAttribute("name", "Joe");
		testHandle("/account", returnType, returnValue, "account: {id=123, name=Joe}", resolver);

		returnType = forClass(Map.class);
		returnValue = Collections.singletonMap("name", "Joe");
		testHandle("/account", returnType, returnValue, "account: {id=123, name=Joe}", resolver);

		returnType = forClass(TestBean.class);
		returnValue = new TestBean("Joe");
		String responseBody = "account: {" +
				"id=123, " +
				"org.springframework.validation.BindingResult.testBean=" +
				"org.springframework.validation.BeanPropertyBindingResult: 0 errors, " +
				"testBean=TestBean[name=Joe]" +
				"}";
		testHandle("/account", returnType, returnValue, responseBody, resolver);

		testHandle("/account", resolvableMethod().annotated(ModelAttribute.class),
				99L, "account: {id=123, num=99}", resolver);
	}

	@Test
	public void handleWithMultipleResolvers() throws Exception {
		Object returnValue = "profile";
		ResolvableType returnType = forClass(String.class);
		ViewResolver[] resolvers = {new TestViewResolver("account"), new TestViewResolver("profile")};

		testHandle("/account", returnType, returnValue, "profile: {id=123}", resolvers);
	}

	@Test
	public void defaultViewName() throws Exception {
		testDefaultViewName(null, forClass(String.class));
		testDefaultViewName(Mono.empty(), forClassWithGenerics(Mono.class, String.class));
		testDefaultViewName(Mono.empty(), forClassWithGenerics(Mono.class, Void.class));
		testDefaultViewName(Completable.complete(), forClass(Completable.class));
	}

	private void testDefaultViewName(Object returnValue, ResolvableType type) throws URISyntaxException {
		this.bindingContext.getModel().addAttribute("id", "123");
		HandlerResult result = new HandlerResult(new Object(), returnValue, returnType(type), this.bindingContext);
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
		MethodParameter returnType = returnType(forClass(String.class));
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
		MethodParameter returnType = returnType(forClass(TestBean.class));
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
		MethodParameter returnType = returnType(forClass(TestBean.class));
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

		ResolvableType type = forClass(void.class);
		HandlerResult result = new HandlerResult(new Object(), null, returnType(type), this.bindingContext);
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

	private MethodParameter returnType(ResolvableType type) {
		return resolvableMethod().returning(type).resolveReturnType();
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

	private ResolvableMethod resolvableMethod() {
		return ResolvableMethod.onClass(TestController.class);
	}

	private ServerWebExchange testHandle(String path, ResolvableType returnType, Object returnValue,
			String responseBody, ViewResolver... resolvers) throws URISyntaxException {

		return testHandle(path, resolvableMethod().returning(returnType), returnValue, responseBody, resolvers);
	}

	private ServerWebExchange testHandle(String path, ResolvableMethod resolvableMethod, Object returnValue,
			String responseBody, ViewResolver... resolvers) throws URISyntaxException {

		Model model = this.bindingContext.getModel();
		model.asMap().clear();
		model.addAttribute("id", "123");
		MethodParameter returnType = resolvableMethod.resolveReturnType();
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
				.consumeNextWith(buf -> assertEquals(responseBody, dumpString(buf, UTF_8)))
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
