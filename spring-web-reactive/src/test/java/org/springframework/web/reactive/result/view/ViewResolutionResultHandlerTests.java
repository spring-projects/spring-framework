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

import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.PublisherToFluxConverter;
import org.springframework.core.convert.support.ReactorToRxJava1Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
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
		Object handler = new Object();
		HandlerMethod hm = handlerMethod(new TestController(), "modelAttributeMethod");

		testSupports(handler, ResolvableType.forClass(String.class), true);
		testSupports(handler, ResolvableType.forClass(View.class), true);
		testSupports(handler, ResolvableType.forClassWithGenerics(Mono.class, String.class), true);
		testSupports(handler, ResolvableType.forClassWithGenerics(Mono.class, View.class), true);
		testSupports(handler, ResolvableType.forClassWithGenerics(Single.class, String.class), true);
		testSupports(handler, ResolvableType.forClassWithGenerics(Single.class, View.class), true);
		testSupports(handler, ResolvableType.forClass(Model.class), true);
		testSupports(handler, ResolvableType.forClass(Map.class), true);
		testSupports(handler, ResolvableType.forClass(TestBean.class), true);
		testSupports(handler, ResolvableType.forClass(Integer.class), false);
		testSupports(hm, ResolvableType.forMethodParameter(hm.getReturnType()), true);
	}

	private void testSupports(Object handler, ResolvableType returnType, boolean result) {
		ViewResolutionResultHandler resultHandler = createResultHandler(mock(ViewResolver.class));
		HandlerResult handlerResult = new HandlerResult(handler, null, returnType, new ExtendedModelMap());
		assertEquals(result, resultHandler.supports(handlerResult));
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
		Object handler = new Object();
		Object returnValue;
		ResolvableType returnType;
		ViewResolver resolver = new TestViewResolver("account");

		returnValue = new TestView("account");
		returnType = ResolvableType.forClass(View.class);
		testHandle("/path", handler, returnValue, returnType, "account: {id=123}");

		returnValue = Mono.just(new TestView("account"));
		returnType = ResolvableType.forClassWithGenerics(Mono.class, View.class);
		testHandle("/path", handler, returnValue, returnType, "account: {id=123}");

		returnValue = "account";
		returnType = ResolvableType.forClass(String.class);
		testHandle("/path", handler, returnValue, returnType, "account: {id=123}", resolver);

		returnValue = Mono.just("account");
		returnType = ResolvableType.forClassWithGenerics(Mono.class, String.class);
		testHandle("/path", handler, returnValue, returnType, "account: {id=123}", resolver);

		returnValue = new ExtendedModelMap().addAttribute("name", "Joe");
		returnType = ResolvableType.forClass(Model.class);
		testHandle("/account", handler, returnValue, returnType, "account: {id=123, name=Joe}", resolver);

		returnValue = Collections.singletonMap("name", "Joe");
		returnType = ResolvableType.forClass(Map.class);
		testHandle("/account", handler, returnValue, returnType, "account: {id=123, name=Joe}", resolver);

		HandlerMethod hm = handlerMethod(new TestController(), "modelAttributeMethod");
		returnValue = "Joe";
		returnType = ResolvableType.forMethodParameter(hm.getReturnType());
		testHandle("/account", hm, returnValue, returnType, "account: {id=123, name=Joe}", resolver);

		returnValue = new TestBean("Joe");
		returnType = ResolvableType.forClass(TestBean.class);
		testHandle("/account", handler, returnValue, returnType, "account: {id=123, testBean=TestBean[name=Joe]}", resolver);
	}

	@Test
	public void handleWithMultipleResolvers() throws Exception {
		Object handler = new Object();
		Object returnValue = "profile";
		ResolvableType returnType = ResolvableType.forClass(String.class);
		ViewResolver[] resolvers = {new TestViewResolver("account"), new TestViewResolver("profile")};

		testHandle("/account", handler, returnValue, returnType, "profile: {id=123}", resolvers);
	}

	@Test
	public void defaultViewName() throws Exception {
		testDefaultViewName(null, ResolvableType.forClass(String.class));
		testDefaultViewName(Mono.empty(), ResolvableType.forClassWithGenerics(Mono.class, String.class));
	}

	private void testDefaultViewName(Object returnValue, ResolvableType returnType)
			throws URISyntaxException {

		ModelMap model = new ExtendedModelMap().addAttribute("id", "123");
		HandlerResult result = new HandlerResult(new Object(), returnValue, returnType, model);
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
		ResolvableType returnType = ResolvableType.forClass(String.class);
		ExtendedModelMap model = new ExtendedModelMap();
		HandlerResult handlerResult = new HandlerResult(new Object(), returnValue, returnType, model);

		this.request.setUri(new URI("/path"));
		Mono<Void> mono = createResultHandler().handleResult(this.exchange, handlerResult);

		TestSubscriber.subscribe(mono).assertErrorMessage("Could not resolve view with name 'account'.");
	}

	@Test
	public void contentNegotiation() throws Exception {
		TestBean value = new TestBean("Joe");
		ResolvableType type = ResolvableType.forClass(TestBean.class);
		HandlerResult handlerResult = new HandlerResult(new Object(), value, type, new ExtendedModelMap());

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
		HandlerResult handlerResult = new HandlerResult(new Object(), value, type, new ExtendedModelMap());

		this.request.getHeaders().setAccept(Collections.singletonList(APPLICATION_JSON));
		this.request.setUri(new URI("/account"));

		ViewResolutionResultHandler resultHandler = createResultHandler(new TestViewResolver("account"));
		Mono<Void> mono = resultHandler.handleResult(this.exchange, handlerResult);
		TestSubscriber.subscribe(mono).assertError(NotAcceptableStatusException.class);
	}


	private ViewResolutionResultHandler createResultHandler(ViewResolver... resolvers) {
		return createResultHandler(Collections.emptyList(), resolvers);
	}

	private ViewResolutionResultHandler createResultHandler(List<View> defaultViews, ViewResolver... resolvers) {
		ConfigurableConversionService service = new DefaultConversionService();
		service.addConverter(new ReactorToRxJava1Converter());
		service.addConverter(new PublisherToFluxConverter());
		List<ViewResolver> resolverList = Arrays.asList(resolvers);
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolverList, service);
		handler.setDefaultViews(defaultViews);
		return handler;
	}

	private HandlerMethod handlerMethod(Object controller, String method) throws NoSuchMethodException {
		return new HandlerMethod(controller, controller.getClass().getMethod(method));
	}

	private void testHandle(String path, Object handler, Object returnValue, ResolvableType returnType,
			String responseBody, ViewResolver... resolvers) throws URISyntaxException {

		ModelMap model = new ExtendedModelMap().addAttribute("id", "123");
		HandlerResult result = new HandlerResult(handler, returnValue, returnType, model);
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

	@Controller	@SuppressWarnings("unused")
	private static class TestController {

		@ModelAttribute("name")
		public String modelAttributeMethod() {
			return null;
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

}