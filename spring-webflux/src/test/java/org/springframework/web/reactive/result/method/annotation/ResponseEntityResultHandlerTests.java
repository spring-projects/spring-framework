/*
 * Copyright 2002-2018 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import rx.Completable;
import rx.Single;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;

import static org.junit.Assert.*;
import static org.springframework.core.ResolvableType.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.http.ResponseEntity.*;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.*;
import static org.springframework.web.method.ResolvableMethod.*;
import static org.springframework.web.reactive.HandlerMapping.*;

/**
 * Unit tests for {@link ResponseEntityResultHandler}. When adding a test also
 * consider whether the logic under test is in a parent class, then see:
 * <ul>
 * <li>{@code MessageWriterResultHandlerTests},
 * <li>{@code ContentNegotiatingResultHandlerSupportTests}
 * </ul>
 * @author Rossen Stoyanchev
 */
public class ResponseEntityResultHandlerTests {

	private ResponseEntityResultHandler resultHandler;


	@Before
	public void setup() throws Exception {
		this.resultHandler = createHandler();
	}

	private ResponseEntityResultHandler createHandler(HttpMessageWriter<?>... writers) {
		List<HttpMessageWriter<?>> writerList;
		if (ObjectUtils.isEmpty(writers)) {
			writerList = new ArrayList<>();
			writerList.add(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
			writerList.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
			writerList.add(new ResourceHttpMessageWriter());
			writerList.add(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
			writerList.add(new EncoderHttpMessageWriter<>(new Jackson2JsonEncoder()));
			writerList.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
		}
		else {
			writerList = Arrays.asList(writers);
		}
		RequestedContentTypeResolver resolver = new RequestedContentTypeResolverBuilder().build();
		return new ResponseEntityResultHandler(writerList, resolver);
	}


	@Test
	public void supports() throws Exception {
		Object value = null;

		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		assertTrue(this.resultHandler.supports(handlerResult(value, returnType)));

		returnType = on(TestController.class).resolveReturnType(Mono.class, entity(String.class));
		assertTrue(this.resultHandler.supports(handlerResult(value, returnType)));

		returnType = on(TestController.class).resolveReturnType(Single.class, entity(String.class));
		assertTrue(this.resultHandler.supports(handlerResult(value, returnType)));

		returnType = on(TestController.class).resolveReturnType(CompletableFuture.class, entity(String.class));
		assertTrue(this.resultHandler.supports(handlerResult(value, returnType)));

		returnType = on(TestController.class).resolveReturnType(HttpHeaders.class);
		assertTrue(this.resultHandler.supports(handlerResult(value, returnType)));

		// SPR-15785
		value = ResponseEntity.ok("testing");
		returnType = on(TestController.class).resolveReturnType(Object.class);
		assertTrue(this.resultHandler.supports(handlerResult(value, returnType)));
	}

	@Test
	public void doesNotSupport() throws Exception {
		Object value = null;

		MethodParameter returnType = on(TestController.class).resolveReturnType(String.class);
		assertFalse(this.resultHandler.supports(handlerResult(value, returnType)));

		returnType = on(TestController.class).resolveReturnType(Completable.class);
		assertFalse(this.resultHandler.supports(handlerResult(value, returnType)));

		// SPR-15464
		returnType = on(TestController.class).resolveReturnType(Flux.class);
		assertFalse(this.resultHandler.supports(handlerResult(value, returnType)));
	}

	@Test
	public void defaultOrder() throws Exception {
		assertEquals(0, this.resultHandler.getOrder());
	}

	@Test
	public void responseEntityStatusCode() throws Exception {
		ResponseEntity<Void> value = ResponseEntity.noContent().build();
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(Void.class));
		HandlerResult result = handlerResult(value, returnType);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.NO_CONTENT, exchange.getResponse().getStatusCode());
		assertEquals(0, exchange.getResponse().getHeaders().size());
		assertResponseBodyIsEmpty(exchange);
	}

	@Test
	public void httpHeaders() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAllow(new LinkedHashSet<>(Arrays.asList(HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS)));
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(Void.class));
		HandlerResult result = handlerResult(headers, returnType);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
		assertEquals(1, exchange.getResponse().getHeaders().size());
		assertEquals("GET,POST,OPTIONS", exchange.getResponse().getHeaders().getFirst("Allow"));
		assertResponseBodyIsEmpty(exchange);
	}

	@Test
	public void responseEntityHeaders() throws Exception {
		URI location = new URI("/path");
		ResponseEntity<Void> value = ResponseEntity.created(location).build();
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(Void.class));
		HandlerResult result = handlerResult(value, returnType);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.CREATED, exchange.getResponse().getStatusCode());
		assertEquals(1, exchange.getResponse().getHeaders().size());
		assertEquals(location, exchange.getResponse().getHeaders().getLocation());
		assertResponseBodyIsEmpty(exchange);
	}

	@Test
	public void handleResponseEntityWithNullBody() throws Exception {
		Object returnValue = Mono.just(notFound().build());
		MethodParameter type = on(TestController.class).resolveReturnType(Mono.class, entity(String.class));
		HandlerResult result = handlerResult(returnValue, type);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
		assertResponseBodyIsEmpty(exchange);
	}

	@Test
	public void handleReturnTypes() throws Exception {
		Object returnValue = ok("abc");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		testHandle(returnValue, returnType);

		returnType = on(TestController.class).resolveReturnType(Object.class);
		testHandle(returnValue, returnType);

		returnValue = Mono.just(ok("abc"));
		returnType = on(TestController.class).resolveReturnType(Mono.class, entity(String.class));
		testHandle(returnValue, returnType);

		returnValue = Mono.just(ok("abc"));
		returnType = on(TestController.class).resolveReturnType(Single.class, entity(String.class));
		testHandle(returnValue, returnType);

		returnValue = Mono.just(ok("abc"));
		returnType = on(TestController.class).resolveReturnType(CompletableFuture.class, entity(String.class));
		testHandle(returnValue, returnType);
	}

	@Test
	public void handleReturnValueLastModified() throws Exception {
		Instant currentTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinAgo = currentTime.minusSeconds(60);
		long timestamp = currentTime.toEpochMilli();
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path").ifModifiedSince(timestamp));

		ResponseEntity<String> entity = ok().lastModified(oneMinAgo.toEpochMilli()).body("body");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		HandlerResult result = handlerResult(entity, returnType);
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertConditionalResponse(exchange, HttpStatus.NOT_MODIFIED, null, null, oneMinAgo);
	}

	@Test
	public void handleReturnValueEtag() throws Exception {
		String etagValue = "\"deadb33f8badf00d\"";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path").ifNoneMatch(etagValue));

		ResponseEntity<String> entity = ok().eTag(etagValue).body("body");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		HandlerResult result = handlerResult(entity, returnType);
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertConditionalResponse(exchange, HttpStatus.NOT_MODIFIED, null, etagValue, Instant.MIN);
	}

	@Test  // SPR-14559
	public void handleReturnValueEtagInvalidIfNoneMatch() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path").ifNoneMatch("unquoted"));

		ResponseEntity<String> entity = ok().eTag("\"deadb33f8badf00d\"").body("body");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		HandlerResult result = handlerResult(entity, returnType);
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
		assertResponseBody(exchange, "body");
	}

	@Test
	public void handleReturnValueETagAndLastModified() throws Exception {
		String eTag = "\"deadb33f8badf00d\"";

		Instant currentTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinAgo = currentTime.minusSeconds(60);

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path")
				.ifNoneMatch(eTag)
				.ifModifiedSince(currentTime.toEpochMilli())
				);

		ResponseEntity<String> entity = ok().eTag(eTag).lastModified(oneMinAgo.toEpochMilli()).body("body");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		HandlerResult result = handlerResult(entity, returnType);
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertConditionalResponse(exchange, HttpStatus.NOT_MODIFIED, null, eTag, oneMinAgo);
	}

	@Test
	public void handleReturnValueChangedETagAndLastModified() throws Exception {
		String etag = "\"deadb33f8badf00d\"";
		String newEtag = "\"changed-etag-value\"";

		Instant currentTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinAgo = currentTime.minusSeconds(60);

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path")
				.ifNoneMatch(etag)
				.ifModifiedSince(currentTime.toEpochMilli())
				);

		ResponseEntity<String> entity = ok().eTag(newEtag).lastModified(oneMinAgo.toEpochMilli()).body("body");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		HandlerResult result = handlerResult(entity, returnType);
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertConditionalResponse(exchange, HttpStatus.OK, "body", newEtag, oneMinAgo);
	}

	@Test  // SPR-14877
	public void handleMonoWithWildcardBodyType() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(APPLICATION_JSON));

		MethodParameter type = on(TestController.class).resolveReturnType(Mono.class, ResponseEntity.class);
		HandlerResult result = new HandlerResult(new TestController(), Mono.just(ok().body("body")), type);

		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
		assertResponseBody(exchange, "body");
	}

	@Test  // SPR-14877
	public void handleMonoWithWildcardBodyTypeAndNullBody() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(APPLICATION_JSON));

		MethodParameter returnType = on(TestController.class).resolveReturnType(Mono.class, ResponseEntity.class);
		HandlerResult result = new HandlerResult(new TestController(), Mono.just(notFound().build()), returnType);

		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
		assertResponseBodyIsEmpty(exchange);
	}

	@Test // SPR-17082
	public void handleResponseEntityWithExistingResponseHeaders() throws Exception {
		ResponseEntity<Void> value = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).build();
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(Void.class));
		HandlerResult result = handlerResult(value, returnType);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
		assertEquals(1, exchange.getResponse().getHeaders().size());
		assertEquals(MediaType.APPLICATION_JSON, exchange.getResponse().getHeaders().getContentType());
		assertResponseBodyIsEmpty(exchange);
	}



	private void testHandle(Object returnValue, MethodParameter returnType) {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		HandlerResult result = handlerResult(returnValue, returnType);
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
		assertEquals("text/plain;charset=UTF-8", exchange.getResponse().getHeaders().getFirst("Content-Type"));
		assertResponseBody(exchange, "abc");
	}

	private ResolvableType entity(Class<?> bodyType) {
		return forClassWithGenerics(ResponseEntity.class, bodyType);
	}

	private HandlerResult handlerResult(Object returnValue, MethodParameter returnType) {
		return new HandlerResult(new TestController(), returnValue, returnType);
	}

	private void assertResponseBody(MockServerWebExchange exchange, String responseBody) {
		StepVerifier.create(exchange.getResponse().getBody())
				.consumeNextWith(buf -> assertEquals(responseBody,
						DataBufferTestUtils.dumpString(buf, StandardCharsets.UTF_8)))
				.expectComplete()
				.verify();
	}

	private void assertResponseBodyIsEmpty(MockServerWebExchange exchange) {
		StepVerifier.create(exchange.getResponse().getBody()).expectComplete().verify();
	}

	private void assertConditionalResponse(MockServerWebExchange exchange, HttpStatus status,
			String body, String etag, Instant lastModified) throws Exception {

		assertEquals(status, exchange.getResponse().getStatusCode());
		if (body != null) {
			assertResponseBody(exchange, body);
		}
		else {
			assertResponseBodyIsEmpty(exchange);
		}
		if (etag != null) {
			assertEquals(1, exchange.getResponse().getHeaders().get(HttpHeaders.ETAG).size());
			assertEquals(etag, exchange.getResponse().getHeaders().getETag());
		}
		if (lastModified.isAfter(Instant.EPOCH)) {
			assertEquals(1, exchange.getResponse().getHeaders().get(HttpHeaders.LAST_MODIFIED).size());
			assertEquals(lastModified.toEpochMilli(), exchange.getResponse().getHeaders().getLastModified());
		}
	}


	@SuppressWarnings("unused")
	private static class TestController {

		ResponseEntity<String> responseEntityString() { return null; }

		ResponseEntity<Void> responseEntityVoid() { return null; }

		HttpHeaders httpHeaders() { return null; }

		Mono<ResponseEntity<String>> mono() { return null; }

		Single<ResponseEntity<String>> single() { return null; }

		CompletableFuture<ResponseEntity<String>> completableFuture() { return null; }

		String string() { return null; }

		Completable completable() { return null; }

		Mono<ResponseEntity<?>> monoResponseEntityWildcard() { return null; }

		Flux<?> fluxWildcard() { return null; }

		Object object() { return null; }
	}

}
