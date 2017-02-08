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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import rx.Completable;
import rx.Single;

import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.assertEquals;


/**
 * Unit tests for {@link ResponseBodyResultHandler}.When adding a test also
 * consider whether the logic under test is in a parent class, then see:
 * <ul>
 * 	<li>{@code MessageWriterResultHandlerTests},
 *  <li>{@code ContentNegotiatingResultHandlerSupportTests}
 * </ul>
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class ResponseBodyResultHandlerTests {

	private ResponseBodyResultHandler resultHandler;

	private MockServerHttpResponse response;

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		this.resultHandler = createHandler();
		initExchange();
	}

	private void initExchange() {
		ServerHttpRequest request = MockServerHttpRequest.get("/").build();
		this.response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(request, this.response);
	}


	private ResponseBodyResultHandler createHandler(HttpMessageWriter<?>... writers) {
		List<HttpMessageWriter<?>> writerList;
		if (ObjectUtils.isEmpty(writers)) {
			writerList = new ArrayList<>();
			writerList.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
			writerList.add(new EncoderHttpMessageWriter<>(new CharSequenceEncoder()));
			writerList.add(new ResourceHttpMessageWriter());
			writerList.add(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
			writerList.add(new EncoderHttpMessageWriter<>(new Jackson2JsonEncoder()));
		}
		else {
			writerList = Arrays.asList(writers);
		}
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder().build();
		return new ResponseBodyResultHandler(writerList, resolver);
	}

	@Test
	public void supports() throws NoSuchMethodException {
		Object controller = new TestController();
		testSupports(controller, "handleToString", true);
		testSupports(controller, "doWork", false);

		controller = new TestRestController();
		testSupports(controller, "handleToString", true);
		testSupports(controller, "handleToMonoString", true);
		testSupports(controller, "handleToSingleString", true);
		testSupports(controller, "handleToCompletable", true);
		testSupports(controller, "handleToResponseEntity", false);
		testSupports(controller, "handleToMonoResponseEntity", false);
	}

	private void testSupports(Object controller, String method, boolean result) throws NoSuchMethodException {
		HandlerMethod hm = handlerMethod(controller, method);
		HandlerResult handlerResult = new HandlerResult(hm, null, hm.getReturnType());
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

		public Mono<Void> handleToMonoVoid() { return null;}

		public String handleToString() {
			return null;
		}

		public Mono<String> handleToMonoString() {
			return null;
		}

		public Single<String> handleToSingleString() {
			return null;
		}

		public Completable handleToCompletable() {
			return null;
		}

		public ResponseEntity<String> handleToResponseEntity() {
			return null;
		}

		public Mono<ResponseEntity<String>> handleToMonoResponseEntity() {
			return null;
		}
	}

	@Controller @SuppressWarnings("unused")
	private static class TestController {

		@ResponseBody
		public String handleToString() {
			return null;
		}

		public String doWork() {
			return null;
		}

	}

}
