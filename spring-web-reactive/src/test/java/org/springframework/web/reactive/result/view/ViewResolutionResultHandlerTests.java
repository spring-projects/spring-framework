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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.test.TestSubscriber;
import rx.Single;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.support.MonoToCompletableFutureConverter;
import org.springframework.core.convert.support.ReactorToRxJava1Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.ResolvableMethod;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Unit tests for {@link ViewResolutionResultHandler}.
 * @author Rossen Stoyanchev
 */
public class ViewResolutionResultHandlerTests {

	private MockServerHttpRequest request;

	private MockServerHttpResponse response = new MockServerHttpResponse();

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		this.request = new MockServerHttpRequest(HttpMethod.GET, new URI("/path"));
		WebSessionManager manager = new DefaultWebSessionManager();
		this.exchange = new DefaultServerWebExchange(this.request, this.response, manager);
	}


	@Test
	public void supports() throws Exception {

		testSupports(ResolvableType.forClass(String.class), true);
		testSupports(ResolvableType.forClass(View.class), true);
		testSupports(ResolvableType.forClassWithGenerics(Mono.class, String.class), true);
		testSupports(ResolvableType.forClassWithGenerics(Mono.class, View.class), true);
		testSupports(ResolvableType.forClassWithGenerics(Single.class, String.class), true);
		testSupports(ResolvableType.forClassWithGenerics(Single.class, View.class), true);
		testSupports(ResolvableType.forClass(Model.class), true);
		testSupports(ResolvableType.forClass(Map.class), true);
		testSupports(ResolvableType.forClass(TestBean.class), true);
		testSupports(ResolvableType.forClass(Integer.class), false);

		testSupports(resolvableMethod().annotated(ModelAttribute.class), true);
	}

	@Test
	public void viewResolverOrder() throws Exception {
		TestViewResolver resolver1 = new TestViewResolver("account");
		TestViewResolver resolver2 = new TestViewResolver("profile");
		resolver1.setOrder(2);
		resolver2.setOrder(1);
		List<ViewResolver> resolvers = createResultHandler(resolver1, resolver2).getViewResolvers();

		assertEquals(Arrays.asList(resolver2, resolver1), resolvers);
	}

	@Test
	public void handleReturnValueTypes() throws Exception {
		Object returnValue;
		ResolvableType returnType;
		ViewResolver resolver = new TestViewResolver("account");

		returnType = ResolvableType.forClass(View.class);
		returnValue = new TestView("account");
		testHandle("/path", returnType, returnValue, "account: {id=123}");

		returnType = ResolvableType.forClassWithGenerics(Mono.class, View.class);
		returnValue = Mono.just(new TestView("account"));
		testHandle("/path", returnType, returnValue, "account: {id=123}");

		returnType = ResolvableType.forClass(String.class);
		returnValue = "account";
		testHandle("/path", returnType, returnValue, "account: {id=123}", resolver);

		returnType = ResolvableType.forClassWithGenerics(Mono.class, String.class);
		returnValue = Mono.just("account");
		testHandle("/path", returnType, returnValue, "account: {id=123}", resolver);

		returnType = ResolvableType.forClass(Model.class);
		returnValue = new ExtendedModelMap().addAttribute("name", "Joe");
		testHandle("/account", returnType, returnValue, "account: {id=123, name=Joe}", resolver);

		returnType = ResolvableType.forClass(Map.class);
		returnValue = Collections.singletonMap("name", "Joe");
		testHandle("/account", returnType, returnValue, "account: {id=123, name=Joe}", resolver);

		returnType = ResolvableType.forClass(TestBean.class);
		returnValue = new TestBean("Joe");
		String responseBody = "account: {id=123, testBean=TestBean[name=Joe]}";
		testHandle("/account", returnType, returnValue, responseBody, resolver);

		testHandle("/account", resolvableMethod().annotated(ModelAttribute.class),
				99L, "account: {id=123, num=99}", resolver);
	}

	@Test
	public void handleWithMultipleResolvers() throws Exception {
		Object returnValue = "profile";
		ResolvableType returnType = ResolvableType.forClass(String.class);
		ViewResolver[] resolvers = {new TestViewResolver("account"), new TestViewResolver("profile")};

		testHandle("/account", returnType, returnValue, "profile: {id=123}", resolvers);
	}

	@Test
	public void defaultViewName() throws Exception {
		testDefaultViewName(null, ResolvableType.forClass(String.class));
		testDefaultViewName(Mono.empty(), ResolvableType.forClassWithGenerics(Mono.class, String.class));
	}

	private void testDefaultViewName(Object returnValue, ResolvableType type)
			throws URISyntaxException {

		ModelMap model = new ExtendedModelMap().addAttribute("id", "123");
		HandlerResult result = new HandlerResult(new Object(), returnValue, returnType(type), model);
		ViewResolutionResultHandler handler =  createResultHandler(new TestViewResolver("account"));

		this.request.setUri(new URI("/account"));
		handler.handleResult(this.exchange, result).block(Duration.ofSeconds(5));
		assertResponseBody("account: {id=123}");

		this.request.setUri(new URI("/account/"));
		handler.handleResult(this.exchange, result).block(Duration.ofSeconds(5));
		assertResponseBody("account: {id=123}");

		this.request.setUri(new URI("/account.123"));
		handler.handleResult(this.exchange, result).block(Duration.ofSeconds(5));
		assertResponseBody("account: {id=123}");
	}

	@Test
	public void unresolvedViewName() throws Exception {
		String returnValue = "account";
		ResolvableType type = ResolvableType.forClass(String.class);
		ExtendedModelMap model = new ExtendedModelMap();
		HandlerResult handlerResult = new HandlerResult(new Object(), returnValue, returnType(type), model);

		this.request.setUri(new URI("/path"));
		Mono<Void> mono = createResultHandler().handleResult(this.exchange, handlerResult);

		TestSubscriber.subscribe(mono).assertErrorMessage("Could not resolve view with name 'account'.");
	}

	@Test
	public void contentNegotiation() throws Exception {
		TestBean value = new TestBean("Joe");
		ResolvableType type = ResolvableType.forClass(TestBean.class);
		ExtendedModelMap model = new ExtendedModelMap();
		HandlerResult handlerResult = new HandlerResult(new Object(), value, returnType(type), model);

		this.request.getHeaders().setAccept(Collections.singletonList(APPLICATION_JSON));
		this.request.setUri(new URI("/account"));

		TestView defaultView = new TestView("jsonView", APPLICATION_JSON);

		createResultHandler(Collections.singletonList(defaultView), new TestViewResolver("account"))
				.handleResult(this.exchange, handlerResult)
				.block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON, this.response.getHeaders().getContentType());
		assertResponseBody("jsonView: {testBean=TestBean[name=Joe]}");
	}

	@Test
	public void contentNegotiationWith406() throws Exception {
		TestBean value = new TestBean("Joe");
		ResolvableType type = ResolvableType.forClass(TestBean.class);
		ExtendedModelMap model = new ExtendedModelMap();
		HandlerResult handlerResult = new HandlerResult(new Object(), value, returnType(type), model);

		this.request.getHeaders().setAccept(Collections.singletonList(APPLICATION_JSON));
		this.request.setUri(new URI("/account"));

		ViewResolutionResultHandler resultHandler = createResultHandler(new TestViewResolver("account"));
		Mono<Void> mono = resultHandler.handleResult(this.exchange, handlerResult);
		TestSubscriber.subscribe(mono).assertError(NotAcceptableStatusException.class);
	}


	private MethodParameter returnType(ResolvableType type) {
		return resolvableMethod().returning(type).resolveReturnType();
	}

	private ResolvableMethod resolvableMethod() {
		return ResolvableMethod.on(TestController.class);
	}

	private void testSupports(ResolvableType type, boolean result) {
		testSupports(resolvableMethod().returning(type), result);
	}

	private void testSupports(ResolvableMethod resolvableMethod, boolean result) {
		ViewResolutionResultHandler resultHandler = createResultHandler(mock(ViewResolver.class));
		MethodParameter returnType = resolvableMethod.resolveReturnType();
		ExtendedModelMap model = new ExtendedModelMap();
		HandlerResult handlerResult = new HandlerResult(new Object(), null, returnType, model);
		assertEquals(result, resultHandler.supports(handlerResult));
	}

	private ViewResolutionResultHandler createResultHandler(ViewResolver... resolvers) {
		return createResultHandler(Collections.emptyList(), resolvers);
	}

	private ViewResolutionResultHandler createResultHandler(List<View> defaultViews, ViewResolver... resolvers) {

		FormattingConversionService service = new DefaultFormattingConversionService();
		service.addConverter(new MonoToCompletableFutureConverter());
		service.addConverter(new ReactorToRxJava1Converter());
		List<ViewResolver> resolverList = Arrays.asList(resolvers);

		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolverList, service);
		handler.setDefaultViews(defaultViews);
		return handler;
	}

	private void testHandle(String path, ResolvableType returnType, Object returnValue,
			String responseBody, ViewResolver... resolvers) throws URISyntaxException {

		testHandle(path, resolvableMethod().returning(returnType), returnValue, responseBody, resolvers);
	}

	private void testHandle(String path, ResolvableMethod resolvableMethod, Object returnValue,
			String responseBody, ViewResolver... resolvers) throws URISyntaxException {

		ModelMap model = new ExtendedModelMap().addAttribute("id", "123");
		MethodParameter returnType = resolvableMethod.resolveReturnType();
		HandlerResult result = new HandlerResult(new Object(), returnValue, returnType, model);
		this.request.setUri(new URI(path));
		createResultHandler(resolvers).handleResult(this.exchange, result).block(Duration.ofSeconds(5));
		assertResponseBody(responseBody);
	}

	private void assertResponseBody(String responseBody) {
		TestSubscriber.subscribe(this.response.getBody())
				.assertValuesWith(buf -> assertEquals(responseBody,
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}


	private static class TestViewResolver implements ViewResolver, Ordered {

		private final Map<String, View> views = new HashMap<>();

		private int order = Ordered.LOWEST_PRECEDENCE;


		public TestViewResolver(String... viewNames) {
			Arrays.stream(viewNames).forEach(name -> this.views.put(name, new TestView(name)));
		}

		public void setOrder(int order) {
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

	public static final class TestView implements View {

		private final String name;

		private final List<MediaType> mediaTypes;


		public TestView(String name) {
			this.name = name;
			this.mediaTypes = Collections.singletonList(MediaType.TEXT_HTML);
		}

		public TestView(String name, MediaType... mediaTypes) {
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
		public Mono<Void> render(HandlerResult result, MediaType mediaType, ServerWebExchange exchange) {
			String value = this.name + ": " + result.getModel().toString();
			assertNotNull(value);
			ServerHttpResponse response = exchange.getResponse();
			if (mediaType != null) {
				response.getHeaders().setContentType(mediaType);
			}
			ByteBuffer byteBuffer = ByteBuffer.wrap(value.getBytes(Charset.forName("UTF-8")));
			DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap(byteBuffer);
			return response.writeWith(Flux.just(dataBuffer));
		}
	}

	private static class TestBean {

		private final String name;

		public TestBean(String name) {
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

		String string() { return null; }

		View view() { return null; }

		Mono<String> monoString() { return null; }

		Mono<View> monoView() { return null; }

		Single<String> singleString() { return null; }

		Single<View> singleView() { return null; }

		Model model() { return null; }

		Map map() { return null; }

		TestBean testBean() { return null; }

		Integer integer() { return null; }

		@ModelAttribute("num")
		Long longAttribute() { return null; }
	}

}