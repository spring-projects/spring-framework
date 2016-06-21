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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import reactor.core.test.TestSubscriber;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.support.JacksonJsonEncoder;
import org.springframework.core.codec.support.StringEncoder;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.method.HandlerMethod;
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
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;

/**
 * Unit tests for {@link ResponseEntityResultHandler}.
 * @author Rossen Stoyanchev
 */
public class ResponseEntityResultHandlerTests {

	private MockServerHttpResponse response = new MockServerHttpResponse();


	@Test
	public void supports() throws NoSuchMethodException {
		ResponseEntityResultHandler handler = createHandler(new StringEncoder());
		TestController controller = new TestController();

		HandlerMethod hm = new HandlerMethod(controller, TestController.class.getMethod("responseString"));
		ResolvableType type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(handler.supports(new HandlerResult(hm, null, type, new ExtendedModelMap())));

		hm = new HandlerMethod(controller, TestController.class.getMethod("responseVoid"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertTrue(handler.supports(new HandlerResult(hm, null, type, new ExtendedModelMap())));

		hm = new HandlerMethod(controller, TestController.class.getMethod("string"));
		type = ResolvableType.forMethodParameter(hm.getReturnType());
		assertFalse(handler.supports(new HandlerResult(hm, null, type, new ExtendedModelMap())));
	}

	@Test
	public void defaultOrder() throws Exception {
		ResponseEntityResultHandler handler = createHandler(new StringEncoder());
		assertEquals(0, handler.getOrder());
	}

	@Test
	public void jsonResponseBody() throws Exception {
		RequestedContentTypeResolver resolver = new FixedContentTypeResolver(APPLICATION_JSON_UTF8);
		HandlerResultHandler handler = createHandler(resolver, new StringEncoder(), new JacksonJsonEncoder());

		TestController controller = new TestController();
		HandlerMethod hm = new HandlerMethod(controller, controller.getClass().getMethod("responseString"));
		ResolvableType type = ResolvableType.forMethodParameter(hm.getReturnType());
		HandlerResult result = new HandlerResult(hm, ResponseEntity.ok("fooValue"), type);

		ServerWebExchange exchange = createExchange("/foo");
		handler.handleResult(exchange, result).block();

		assertEquals(HttpStatus.OK, this.response.getStatus());
		assertEquals(APPLICATION_JSON_UTF8, this.response.getHeaders().getContentType());
		TestSubscriber.subscribe(this.response.getBody())
				.assertValuesWith(buf -> assertEquals("\"fooValue\"",
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}


	private ResponseEntityResultHandler createHandler(Encoder<?>... encoders) {
		return createHandler(new HeaderContentTypeResolver(), encoders);
	}

	private ResponseEntityResultHandler createHandler(RequestedContentTypeResolver resolver,
			Encoder<?>... encoders) {

		List<HttpMessageConverter<?>> converters = Arrays.stream(encoders)
				.map(encoder -> new CodecHttpMessageConverter<>(encoder, null))
				.collect(Collectors.toList());
		return new ResponseEntityResultHandler(converters, new DefaultConversionService(), resolver);
	}

	private ServerWebExchange createExchange(String path) throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI(path));
		WebSessionManager sessionManager = mock(WebSessionManager.class);
		return new DefaultServerWebExchange(request, this.response, sessionManager);
	}


	@SuppressWarnings("unused")
	private static class TestController {

		public ResponseEntity<String> responseString() {
			return null;
		}

		public ResponseEntity<Void> responseVoid() {
			return null;
		}

		public String string() {
			return null;
		}
	}

}
