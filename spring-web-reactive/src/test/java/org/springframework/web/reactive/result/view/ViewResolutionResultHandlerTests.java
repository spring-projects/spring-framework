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
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.ReactiveStreamsToRxJava1Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponse;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ViewResolutionResultHandler}.
 * @author Rossen Stoyanchev
 */
public class ViewResolutionResultHandlerTests {

	private MockServerHttpRequest request;

	private MockServerHttpResponse response;

	private ModelMap model;


	@Before
	public void setUp() throws Exception {
		this.model = new ExtendedModelMap().addAttribute("id", "123");
		this.request = new MockServerHttpRequest(HttpMethod.GET, new URI("/path"));
		this.response = new MockServerHttpResponse();
	}


	@Test
	public void supports() throws Exception {
		testSupports("handleString", true);
		testSupports("handleView", true);
		testSupports("handleMonoString", true);
		testSupports("handleMonoView", true);
		testSupports("handleSingleString", true);
		testSupports("handleSingleView", true);
		testSupports("handleModel", true);
		testSupports("handleMap", true);
		testSupports("handleModelAttributeAnnotation", true);
		testSupports("handleTestBean", true);
		testSupports("handleInteger", false);
	}

	@Test
	public void order() throws Exception {
		TestViewResolver resolver1 = new TestViewResolver(new String[] {});
		TestViewResolver resolver2 = new TestViewResolver(new String[] {});
		resolver1.setOrder(2);
		resolver2.setOrder(1);

		assertEquals(Arrays.asList(resolver2, resolver1), new ViewResolutionResultHandler(
				Arrays.asList(resolver1, resolver2), new DefaultConversionService())
				.getViewResolvers());
	}

	@Test
	public void viewReference() throws Exception {
		Object value = new TestView("account");
		handle("/path", value, "handleView");

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}

	@Test
	public void viewReferenceInMono() throws Exception {
		Object value = Mono.just(new TestView("account"));
		handle("/path", value, "handleMonoView");

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}

	@Test
	public void viewName() throws Exception {
		Object value = "account";
		handle("/path", value, "handleString", new TestViewResolver("account"));

		TestSubscriber<DataBuffer> subscriber = new TestSubscriber<>();
		subscriber.bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}

	@Test
	public void viewNameMono() throws Exception {
		Object value = Mono.just("account");
		handle("/path", value, "handleMonoString", new TestViewResolver("account"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}

	@Test
	public void viewNameWithMultipleResolvers() throws Exception {
		String value = "profile";
		handle("/path", value, "handleString",
				new TestViewResolver("account"), new TestViewResolver("profile"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("profile: {id=123}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}

	@Test
	public void viewNameUnresolved() throws Exception {
		handle("/path", "account", "handleString")
				.assertErrorMessage("Could not resolve view with name 'account'.");
	}

	@Test
	public void viewNameIsNull() throws Exception {
		ViewResolver resolver = new TestViewResolver("account");

		handle("/account", null, "handleString", resolver);
		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));

		handle("/account/", null, "handleString", resolver);
		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));

		handle("/account.123", null, "handleString", resolver);
		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}

	@Test
	public void viewNameIsEmptyMono() throws Exception {
		Object value = Mono.empty();
		handle("/account", value, "handleMonoString", new TestViewResolver("account"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}

	@Test
	public void modelReturnValue() throws Exception {
		Model value = new ExtendedModelMap().addAttribute("name", "Joe");
		handle("/account", value, "handleModel", new TestViewResolver("account"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123, name=Joe}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}

	@Test
	public void mapReturnValue() throws Exception {
		Map<String, String> value = Collections.singletonMap("name", "Joe");
		handle("/account", value, "handleMap", new TestViewResolver("account"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123, name=Joe}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}

	@Test
	public void modelAttributeAnnotationReturnValue() throws Exception {
		String value = "Joe";
		handle("/account", value, "handleModelAttributeAnnotation", new TestViewResolver("account"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123, name=Joe}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}

	@Test
	public void objectReturnValue() throws Exception {
		Object value = new TestBean("Joe");
		handle("/account", value, "handleTestBean", new TestViewResolver("account"));

		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("account: {id=123, testBean=TestBean[name=Joe]}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}

	@Test
	public void contentNegotiation() throws Exception {
		TestView htmlView = new TestView("account");
		htmlView.setMediaTypes(Collections.singletonList(MediaType.TEXT_HTML));

		TestView jsonView = new TestView("defaultView");
		jsonView.setMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));

		this.request.getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		handle("/account", "account", "handleString",
				Collections.singletonList(new TestViewResolver(htmlView)),
				Collections.singletonList(jsonView));

		assertEquals(MediaType.APPLICATION_JSON, this.response.getHeaders().getContentType());
		new TestSubscriber<DataBuffer>().bindTo(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("defaultView: {id=123}",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}

	@Test
	public void contentNegotiationNotAcceptable() throws Exception {
		TestView htmlView = new TestView("account");
		htmlView.setMediaTypes(Collections.singletonList(MediaType.TEXT_HTML));

		this.request.getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		handle("/account", "account", "handleString", new TestViewResolver(htmlView))
				.assertError(NotAcceptableStatusException.class);

	}

	private void testSupports(String methodName, boolean supports) throws NoSuchMethodException {
		Method method = TestController.class.getMethod(methodName);
		ResolvableType returnType = ResolvableType.forMethodParameter(method, -1);
		HandlerResult result = new HandlerResult(new Object(), null, returnType, this.model);
		List<ViewResolver> resolvers = Collections.singletonList(mock(ViewResolver.class));
		ConfigurableConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new ReactiveStreamsToRxJava1Converter());
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, conversionService);
		if (supports) {
			assertTrue(handler.supports(result));
		}
		else {
			assertFalse(handler.supports(result));
		}
	}

	private TestSubscriber<Void> handle(String path, Object value, String methodName,
			ViewResolver... resolvers) throws Exception {

		return handle(path, value, methodName, Arrays.asList(resolvers), Collections.emptyList());
	}

	private TestSubscriber<Void> handle(String path, Object value, String methodName,
			List<ViewResolver> resolvers, List<View> defaultViews) throws Exception {

		ConversionService conversionService = new DefaultConversionService();
		ViewResolutionResultHandler handler = new ViewResolutionResultHandler(resolvers, conversionService);
		handler.setDefaultViews(defaultViews);

		Method method = TestController.class.getMethod(methodName);
		HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);
		ResolvableType type = ResolvableType.forMethodReturnType(method);
		HandlerResult handlerResult = new HandlerResult(handlerMethod, value, type, this.model);

		this.request.setUri(new URI(path));
		WebSessionManager sessionManager = new DefaultWebSessionManager();
		ServerWebExchange exchange = new DefaultServerWebExchange(this.request, this.response, sessionManager);

		Mono<Void> mono = handler.handleResult(exchange, handlerResult);

		TestSubscriber<Void> subscriber = new TestSubscriber<>();
		return subscriber.bindTo(mono).await(Duration.ofSeconds(1));
	}


	private static class TestViewResolver implements ViewResolver, Ordered {

		private final Map<String, View> views = new HashMap<>();

		private int order = Ordered.LOWEST_PRECEDENCE;


		public TestViewResolver(String... viewNames) {
			Arrays.stream(viewNames).forEach(name -> this.views.put(name, new TestView(name)));
		}

		public TestViewResolver(TestView... views) {
			Arrays.stream(views).forEach(view -> this.views.put(view.getName(), view));
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

		private List<MediaType> mediaTypes = Collections.singletonList(MediaType.TEXT_PLAIN);


		public TestView(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public void setMediaTypes(List<MediaType> mediaTypes) {
			this.mediaTypes = mediaTypes;
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

		public Model handleModel() {
			return null;
		}

		public Map<String, String> handleMap() {
			return null;
		}

		@ModelAttribute("name")
		public String handleModelAttributeAnnotation() {
			return null;
		}

		public TestBean handleTestBean() {
			return null;
		}

		public int handleInteger() {
			return 0;
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