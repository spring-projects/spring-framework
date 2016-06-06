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
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.reactivestreams.Publisher;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.support.JacksonJsonEncoder;
import org.springframework.core.codec.support.StringEncoder;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.FixedContentTypeResolver;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


/**
 * Unit tests for {@link ResponseBodyResultHandler}.
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class ResponseBodyResultHandlerTests {


	@Test
	public void supports() throws NoSuchMethodException {
		ResponseBodyResultHandler handler = createHandler(new StringEncoder());
		TestController controller = new TestController();

		HandlerMethod hm = new HandlerMethod(controller, TestController.class.getMethod("notAnnotated"));
		ResolvableType type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertFalse(handler.supports(new HandlerResult(hm, null, type, new ExtendedModelMap())));

		hm = new HandlerMethod(controller, TestController.class.getMethod("publisherString"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(handler.supports(new HandlerResult(hm, null, type, new ExtendedModelMap())));

		hm = new HandlerMethod(controller, TestController.class.getMethod("publisherVoid"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(handler.supports(new HandlerResult(hm, null, type, new ExtendedModelMap())));
	}

	@Test
	public void defaultOrder() throws Exception {
		ResponseBodyResultHandler handler = createHandler(new StringEncoder());
		assertEquals(0, handler.getOrder());
	}

	@Test
	public void usesContentTypeResolver() throws Exception {
		MediaType contentType = MediaType.APPLICATION_JSON_UTF8;
		RequestedContentTypeResolver resolver = new FixedContentTypeResolver(contentType);
		HandlerResultHandler handler = createHandler(resolver, new StringEncoder(), new JacksonJsonEncoder());

		ServerWebExchange exchange = createExchange("/foo");
		HandlerResult result = new HandlerResult(new Object(), "fooValue", ResolvableType.forClass(String.class));
		handler.handleResult(exchange, result).block();

		assertEquals(contentType, exchange.getResponse().getHeaders().getContentType());
	}

	@Test
	public void detectsProducibleMediaTypesAttribute() throws Exception {
		ServerWebExchange exchange = createExchange("/foo");
		Set<MediaType> mediaTypes = Collections.singleton(MediaType.APPLICATION_JSON);
		exchange.getAttributes().put(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);

		HandlerResultHandler handler = createHandler(new StringEncoder(), new JacksonJsonEncoder());

		HandlerResult result = new HandlerResult(new Object(), "fooValue", ResolvableType.forClass(String.class));
		handler.handleResult(exchange, result).block();

		assertEquals(MediaType.APPLICATION_JSON, exchange.getResponse().getHeaders().getContentType());
	}


	private ResponseBodyResultHandler createHandler(Encoder<?>... encoders) {
		return createHandler(new HeaderContentTypeResolver(), encoders);
	}

	private ResponseBodyResultHandler createHandler(RequestedContentTypeResolver resolver,
			Encoder<?>... encoders) {

		List<HttpMessageConverter<?>> converters = Arrays.stream(encoders)
				.map(encoder -> new CodecHttpMessageConverter<>(encoder, null))
				.collect(Collectors.toList());
		return new ResponseBodyResultHandler(converters, new DefaultConversionService(), resolver);
	}

	private ServerWebExchange createExchange(String path) throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI(path));
		WebSessionManager sessionManager = mock(WebSessionManager.class);
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}


	@SuppressWarnings("unused")
	private static class TestController {

		public Publisher<String> notAnnotated() {
			return null;
		}

		@ResponseBody
		public Publisher<String> publisherString() {
			return null;
		}

		@ResponseBody
		public Publisher<Void> publisherVoid() {
			return null;
		}
	}

}
