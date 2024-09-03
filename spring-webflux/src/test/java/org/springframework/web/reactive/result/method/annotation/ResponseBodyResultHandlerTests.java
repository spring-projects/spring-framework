/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;
import static org.springframework.web.testfixture.method.ResolvableMethod.on;

/**
 * Unit tests for {@link ResponseBodyResultHandler}.When adding a test also
 * consider whether the logic under test is in a parent class, then see:
 * <ul>
 * <li>{@code MessageWriterResultHandlerTests},
 * <li>{@code ContentNegotiatingResultHandlerSupportTests}
 * </ul>
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class ResponseBodyResultHandlerTests {

	private ResponseBodyResultHandler resultHandler;


	@BeforeEach
	public void setup() throws Exception {
		List<HttpMessageWriter<?>> writerList = new ArrayList<>(5);
		writerList.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		writerList.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
		writerList.add(new ResourceHttpMessageWriter());
		writerList.add(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
		writerList.add(new EncoderHttpMessageWriter<>(new Jackson2JsonEncoder()));
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder().build();
		this.resultHandler = new ResponseBodyResultHandler(writerList, resolver);
	}


	@Test
	public void supports() {
		Object controller = new TestController();
		Method method;

		method = on(TestController.class).annotPresent(ResponseBody.class).resolveMethod();
		testSupports(controller, method);

		method = on(TestController.class).annotNotPresent(ResponseBody.class).resolveMethod("doWork");
		HandlerResult handlerResult = getHandlerResult(controller, null, method);
		assertThat(this.resultHandler.supports(handlerResult)).isFalse();
	}

	@Test
	public void supportsRestController() {
		Object controller = new TestRestController();
		Method method;

		method = on(TestRestController.class).returning(String.class).resolveMethod();
		testSupports(controller, method);

		method = on(TestRestController.class).returning(Mono.class, String.class).resolveMethod();
		testSupports(controller, method);

		method = on(TestRestController.class).returning(Single.class, String.class).resolveMethod();
		testSupports(controller, method);

		method = on(TestRestController.class).returning(Completable.class).resolveMethod();
		testSupports(controller, method);
	}

	private void testSupports(Object controller, Method method) {
		HandlerResult handlerResult = getHandlerResult(controller, null, method);
		assertThat(this.resultHandler.supports(handlerResult)).isTrue();
	}

	@Test
	void problemDetailContentNegotiation() {

		// Default
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		testProblemDetailMediaType(exchange, MediaType.APPLICATION_PROBLEM_JSON);

		// JSON requested
		exchange = MockServerWebExchange.from(get("/path").accept(MediaType.APPLICATION_JSON));
		testProblemDetailMediaType(exchange, MediaType.APPLICATION_PROBLEM_JSON);

		// JSON & Problem Detail requested (gh-29588)
		exchange = MockServerWebExchange.from(get("/path").accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON));
		testProblemDetailMediaType(exchange, MediaType.APPLICATION_PROBLEM_JSON);

		// No match fallback
		exchange = MockServerWebExchange.from(get("/path").accept(MediaType.APPLICATION_PDF));
		testProblemDetailMediaType(exchange, MediaType.APPLICATION_PROBLEM_JSON);
	}

	private void testProblemDetailMediaType(MockServerWebExchange exchange, MediaType expectedMediaType) {
		MyProblemDetail problemDetail = new MyProblemDetail(HttpStatus.BAD_REQUEST);

		Method method = on(TestRestController.class).returning(MyProblemDetail.class).resolveMethod();
		HandlerResult result = getHandlerResult(new TestRestController(), problemDetail, method);

		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(expectedMediaType);
		assertResponseBody(exchange,
				"{\"type\":\"about:blank\"," +
						"\"title\":\"Bad Request\"," +
						"\"status\":400," +
						"\"instance\":\"/path\"}");
	}

	@Test
	public void defaultOrder() {
		assertThat(this.resultHandler.getOrder()).isEqualTo(100);
	}

	private HandlerResult getHandlerResult(Object controller, @Nullable Object returnValue, Method method) {
		HandlerMethod handlerMethod = new HandlerMethod(controller, method);
		return new HandlerResult(handlerMethod, returnValue, handlerMethod.getReturnType());
	}

	@SuppressWarnings("SameParameterValue")
	private void assertResponseBody(MockServerWebExchange exchange, @Nullable String responseBody) {
		StepVerifier.create(exchange.getResponse().getBody())
				.consumeNextWith(buf -> assertThat(buf.toString(UTF_8)).isEqualTo(responseBody))
				.expectComplete()
				.verify();
	}


	@RestController
	@SuppressWarnings({"unused", "ConstantConditions"})
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

		public MyProblemDetail handleToProblemDetail() {
			return null;
		}

	}


	@Controller
	@SuppressWarnings({"unused", "ConstantConditions"})
	private static class TestController {

		@ResponseBody
		public String handleToString() {
			return null;
		}

		public String doWork() {
			return null;
		}
	}


	private static class MyProblemDetail extends ProblemDetail {

		public MyProblemDetail(HttpStatus status) {
			super(status.value());
		}

	}

}
