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

import java.lang.reflect.Method;
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
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.ReactiveStreamsToRxJava1Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;


/**
 * Unit tests for {@link ViewResolutionResultHandler}.
 * @author Rossen Stoyanchev
 */
public class ViewResolutionResultHandlerTests {

	private static final Charset UTF_8 = Charset.forName("UTF-8");


	private MockServerHttpResponse response;

	private ServerWebExchange exchange;

	private ModelMap model;

	private DefaultConversionService conversionService;


	@Before
	public void setUp() throws Exception {
		this.exchange = createExchange("/path");
		this.model = new ExtendedModelMap().addAttribute("id", "123");
		this.conversionService = new DefaultConversionService();
		this.conversionService.addConverter(new ReactiveStreamsToRxJava1Converter());
	}


	@Test
	public void supportsWithNullReturnValue() throws Exception {
		testSupports("handleString", null);
		testSupports("handleView", null);
		testSupports("handleMonoString", null);
		testSupports("handleMonoView", null);
		testSupports("handleSingleString", null);
		testSupports("handleSingleView", null);
	}

	private void testSupports(String methodName, Object returnValue) throws NoSuchMethodException {
		Method method = TestController.class.getMethod(methodName);
		ResolvableType returnType = ResolvableType.forMethodParameter(method, -1);
		HandlerResult result = new HandlerResult(new Object(), returnValue, returnType, this.model);
		List<ViewResolver> resolvers = Collections.singletonList(mock(ViewResolver.class));
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, this.conversionService);
		assertTrue(handler.supports(result));
	}

	@Test
	public void viewReference() throws Exception {
		TestView view = new TestView("account");
		List<ViewResolver> resolvers = Collections.singletonList(mock(ViewResolver.class));
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, this.conversionService);
		handle(this.exchange, handler, view, ResolvableType.forClass(View.class));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));
	}

	@Test
	public void viewReferenceMono() throws Exception {
		TestView view = new TestView("account");
		List<ViewResolver> resolvers = Collections.singletonList(mock(ViewResolver.class));
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, this.conversionService);
		handle(this.exchange, handler, Mono.just(view), methodReturnType("handleMonoView"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));
	}

	@Test
	public void viewName() throws Exception {
		TestView view = new TestView("account");
		TestViewResolver resolver = new TestViewResolver().addView(view);
		List<ViewResolver> resolvers = Collections.singletonList(resolver);
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, this.conversionService);
		handle(this.exchange, handler, "account", ResolvableType.forClass(String.class));

		TestSubscriber<DataBuffer> subscriber = new TestSubscriber<>();
		subscriber.bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));
	}

	@Test
	public void viewNameMono() throws Exception {
		TestView view = new TestView("account");
		TestViewResolver resolver = new TestViewResolver().addView(view);
		List<ViewResolver> resolvers = Collections.singletonList(resolver);
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, this.conversionService);
		handle(this.exchange, handler, Mono.just("account"), methodReturnType("handleMonoString"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));
	}

	@Test
	public void viewNameWithMultipleResolvers() throws Exception {
		TestView view1 = new TestView("account");
		TestView view2 = new TestView("profile");
		TestViewResolver resolver1 = new TestViewResolver().addView(view1);
		TestViewResolver resolver2 = new TestViewResolver().addView(view2);
		List<ViewResolver> resolvers = Arrays.asList(resolver1, resolver2);
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, this.conversionService);
		handle(this.exchange, handler, "profile", ResolvableType.forClass(String.class));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("profile: {id=123}", asString(buf)));
	}

	@Test
	public void viewNameWithNoMatch() throws Exception {
		List<ViewResolver> resolvers = Collections.singletonList(mock(ViewResolver.class));
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, this.conversionService);
		TestSubscriber<Void> subscriber = handle(this.exchange, handler, "account", ResolvableType.forClass(String.class));

		subscriber.assertNoValues();
	}

	@Test
	public void viewNameNotSpecified() throws Exception {
		TestView view = new TestView("account");
		TestViewResolver resolver = new TestViewResolver().addView(view);
		List<ViewResolver> resolvers = Collections.singletonList(resolver);
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, this.conversionService);

		ServerWebExchange exchange = createExchange("/account");
		handle(exchange, handler, null, ResolvableType.forClass(String.class));
		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));

		exchange = createExchange("/account/");
		handle(exchange, handler, null, ResolvableType.forClass(String.class));
		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));

		exchange = createExchange("/account.123");
		handle(exchange, handler, null, ResolvableType.forClass(String.class));
		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));
	}

	@Test
	public void viewNameMonoEmpty() throws Exception {
		TestView view = new TestView("account");
		TestViewResolver resolver = new TestViewResolver().addView(view);
		List<ViewResolver> resolvers = Collections.singletonList(resolver);
		HandlerResultHandler handler = new ViewResolutionResultHandler(resolvers, this.conversionService);
		ServerWebExchange exchange = createExchange("/account");
		handle(exchange, handler, Mono.empty(), methodReturnType("handleMonoString"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}", asString(buf)));
	}


	@Test
	public void ordered() throws Exception {
		TestViewResolver resolver1 = new TestViewResolver();
		TestViewResolver resolver2 = new TestViewResolver();
		List<ViewResolver> resolvers = Arrays.asList(resolver1, resolver2);

		resolver1.setOrder(2);
		resolver2.setOrder(1);

		ViewResolutionResultHandler resultHandler =
				new ViewResolutionResultHandler(resolvers, this.conversionService);

		assertEquals(Arrays.asList(resolver2, resolver1), resultHandler.getViewResolvers());
	}


	private ServerWebExchange createExchange(String path) throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI(path));
		this.response = new MockServerHttpResponse();
		WebSessionManager sessionManager = new DefaultWebSessionManager();
		return new DefaultServerWebExchange(request, this.response, sessionManager);
	}

	private TestSubscriber<Void> handle(ServerWebExchange exchange, HandlerResultHandler handler,
			Object value, ResolvableType type) {

		HandlerResult result = new HandlerResult(new Object(), value, type, this.model);
		Mono<Void> mono = handler.handleResult(exchange, result);
		TestSubscriber<Void> subscriber = new TestSubscriber<>();
		return subscriber.bindTo(mono).await(Duration.ofSeconds(1));
	}

	private ResolvableType methodReturnType(String methodName, Class<?>... args) throws NoSuchMethodException {
		Method method = TestController.class.getDeclaredMethod(methodName, args);
		return ResolvableType.forMethodReturnType(method);
	}

	private static DataBuffer asDataBuffer(String value) {
		ByteBuffer byteBuffer = ByteBuffer.wrap(value.getBytes(UTF_8));
		return new DefaultDataBufferFactory().wrap(byteBuffer);
	}

	private static String asString(DataBuffer dataBuffer) {
		ByteBuffer byteBuffer = dataBuffer.asByteBuffer();
		final byte[] bytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(bytes);
		return new String(bytes, UTF_8);
	}


	private static class TestViewResolver implements ViewResolver, Ordered {

		private final Map<String, View> views = new HashMap<>();

		private int order = Ordered.LOWEST_PRECEDENCE;


		public void setOrder(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return this.order;
		}

		public TestViewResolver addView(TestView view) {
			this.views.put(view.getName(), view);
			return this;
		}

		@Override
		public Mono<View> resolveViewName(String viewName, Locale locale) {
			View view = this.views.get(viewName);
			return Mono.justOrEmpty(view);
		}

	}

	public static final class TestView implements View {

		private final String name;


		public TestView(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public List<MediaType> getSupportedMediaTypes() {
			return null;
		}

		@Override
		public Flux<DataBuffer> render(HandlerResult result, MediaType mediaType, ServerWebExchange exchange) {
			String value = this.name + ": " + result.getModel().toString();
			assertNotNull(value);
			return Flux.just(asDataBuffer(value));
		}
	}

	@SuppressWarnings("unused")
	private static class TestController {

		public String handleString() {
			return null;
		}

		public Mono<String> handleMonoString() {
			return null;
		}

		public Single<String> handleSingleString() {
			return null;
		}

		public View handleView() {
			return null;
		}

		public Mono<View> handleMonoView() {
			return null;
		}

		public Single<View> handleSingleView() {
			return null;
		}
	}

}