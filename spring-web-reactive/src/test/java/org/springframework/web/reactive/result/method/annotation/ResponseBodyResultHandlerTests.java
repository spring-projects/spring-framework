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

package org.springframework.web.reactive.result.method.annotation;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.support.ByteBufferEncoder;
import org.springframework.core.codec.support.JacksonJsonEncoder;
import org.springframework.core.codec.support.Jaxb2Encoder;
import org.springframework.core.codec.support.StringEncoder;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.convert.support.ReactiveStreamsToCompletableFutureConverter;
import org.springframework.core.convert.support.ReactiveStreamsToRxJava1Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.converter.reactive.ResourceHttpMessageConverter;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;


/**
 * Unit tests for {@link ResponseBodyResultHandler}.
 *
 * consider whether the logic under test is in a parent class, then see:
 * <ul>
 * 	<li>{@code MessageConverterResultHandlerTests},
 *  <li>{@code ContentNegotiatingResultHandlerSupportTests}
 * </ul>
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class ResponseBodyResultHandlerTests {

	private ResponseBodyResultHandler resultHandler;

	private MockServerHttpResponse response = new MockServerHttpResponse();

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		this.resultHandler = createHandler();
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/path"));
		this.exchange = new DefaultServerWebExchange(request, this.response, mock(WebSessionManager.class));
	}


	private ResponseBodyResultHandler createHandler(HttpMessageConverter<?>... converters) {
		List<HttpMessageConverter<?>> converterList;
		if (ObjectUtils.isEmpty(converters)) {
			converterList = new ArrayList<>();
			converterList.add(new CodecHttpMessageConverter<>(new ByteBufferEncoder()));
			converterList.add(new CodecHttpMessageConverter<>(new StringEncoder()));
			converterList.add(new ResourceHttpMessageConverter());
			converterList.add(new CodecHttpMessageConverter<>(new Jaxb2Encoder()));
			converterList.add(new CodecHttpMessageConverter<>(new JacksonJsonEncoder()));
		}
		else {
			converterList = Arrays.asList(converters);
		}
		GenericConversionService service = new GenericConversionService();
		service.addConverter(new ReactiveStreamsToCompletableFutureConverter());
		service.addConverter(new ReactiveStreamsToRxJava1Converter());
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder().build();

		return new ResponseBodyResultHandler(converterList, new DefaultConversionService(), resolver);
	}

	@Test
	public void supports() throws NoSuchMethodException {
		TestController controller = new TestController();
		testSupports(controller, "handleReturningString", true);
		testSupports(controller, "handleReturningVoid", true);
		testSupports(controller, "doWork", false);

		TestRestController restController = new TestRestController();
		testSupports(restController, "handleReturningString", true);
		testSupports(restController, "handleReturningVoid", true);
	}

	private void testSupports(Object controller, String method, boolean result) throws NoSuchMethodException {
		HandlerMethod hm = handlerMethod(controller, method);
		ResolvableType type = ResolvableType.forMethodParameter(hm.getReturnType());
		HandlerResult handlerResult = new HandlerResult(hm, null, type, new ExtendedModelMap());
		assertEquals(result, this.resultHandler.supports(handlerResult));
	}

	@Test
	public void defaultOrder() throws Exception {
		assertEquals(100, this.resultHandler.getOrder());
	}


	private HandlerMethod handlerMethod(Object controller, String method) throws NoSuchMethodException {
		return new HandlerMethod(controller, controller.getClass().getMethod(method));
	}


	@RestController @SuppressWarnings("unused")
	private static class TestRestController {

		public String handleReturningString() {
			return null;
		}

		public Void handleReturningVoid() {
			return null;
		}
	}

	@Controller @SuppressWarnings("unused")
	private static class TestController {

		@ResponseBody
		public String handleReturningString() {
			return null;
		}

		@ResponseBody
		public Void handleReturningVoid() {
			return null;
		}

		public String doWork() {
			return null;
		}

	}

}
