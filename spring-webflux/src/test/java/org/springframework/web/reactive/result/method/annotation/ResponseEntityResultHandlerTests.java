/*
 * Copyright 2002-2022 the original author or authors.
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

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
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
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClassWithGenerics;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.web.reactive.HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;
import static org.springframework.web.testfixture.method.ResolvableMethod.on;

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

	private static final String NEWLINE_SYSTEM_PROPERTY = System.lineSeparator();


	private ResponseEntityResultHandler resultHandler;


	@BeforeEach
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
		assertThat(this.resultHandler.supports(handlerResult(value, returnType))).isTrue();

		returnType = on(TestController.class).resolveReturnType(Mono.class, entity(String.class));
		assertThat(this.resultHandler.supports(handlerResult(value, returnType))).isTrue();

		returnType = on(TestController.class).resolveReturnType(Single.class, entity(String.class));
		assertThat(this.resultHandler.supports(handlerResult(value, returnType))).isTrue();

		returnType = on(TestController.class).resolveReturnType(CompletableFuture.class, entity(String.class));
		assertThat(this.resultHandler.supports(handlerResult(value, returnType))).isTrue();

		returnType = on(TestController.class).resolveReturnType(HttpHeaders.class);
		assertThat(this.resultHandler.supports(handlerResult(value, returnType))).isTrue();

		// SPR-15785
		value = ResponseEntity.ok("testing");
		returnType = on(TestController.class).resolveReturnType(Object.class);
		assertThat(this.resultHandler.supports(handlerResult(value, returnType))).isTrue();
	}

	@Test
	public void doesNotSupport() throws Exception {
		Object value = null;

		MethodParameter returnType = on(TestController.class).resolveReturnType(String.class);
		assertThat(this.resultHandler.supports(handlerResult(value, returnType))).isFalse();

		returnType = on(TestController.class).resolveReturnType(Completable.class);
		assertThat(this.resultHandler.supports(handlerResult(value, returnType))).isFalse();

		// SPR-15464
		returnType = on(TestController.class).resolveReturnType(Flux.class);
		assertThat(this.resultHandler.supports(handlerResult(value, returnType))).isFalse();
	}

	@Test
	public void defaultOrder() throws Exception {
		assertThat(this.resultHandler.getOrder()).isEqualTo(0);
	}

	@Test
	public void responseEntityStatusCode() throws Exception {
		ResponseEntity<Void> value = ResponseEntity.noContent().build();
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(Void.class));
		HandlerResult result = handlerResult(value, returnType);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(exchange.getResponse().getHeaders().size()).isEqualTo(0);
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

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(exchange.getResponse().getHeaders().size()).isEqualTo(1);
		assertThat(exchange.getResponse().getHeaders().getFirst("Allow")).isEqualTo("GET,POST,OPTIONS");
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

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(exchange.getResponse().getHeaders().size()).isEqualTo(1);
		assertThat(exchange.getResponse().getHeaders().getLocation()).isEqualTo(location);
		assertResponseBodyIsEmpty(exchange);
	}

	@Test
	public void handleResponseEntityWithNullBody() {
		Object returnValue = Mono.just(notFound().build());
		MethodParameter type = on(TestController.class).resolveReturnType(Mono.class, entity(String.class));
		HandlerResult result = handlerResult(returnValue, type);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertResponseBodyIsEmpty(exchange);
	}

	@Test
	public void handleReturnTypes() {
		Object returnValue = ResponseEntity.ok("abc");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		testHandle(returnValue, returnType);

		returnType = on(TestController.class).resolveReturnType(Object.class);
		testHandle(returnValue, returnType);

		returnValue = Mono.just(ResponseEntity.ok("abc"));
		returnType = on(TestController.class).resolveReturnType(Mono.class, entity(String.class));
		testHandle(returnValue, returnType);

		returnValue = Mono.just(ResponseEntity.ok("abc"));
		returnType = on(TestController.class).resolveReturnType(Single.class, entity(String.class));
		testHandle(returnValue, returnType);

		returnValue = Mono.just(ResponseEntity.ok("abc"));
		returnType = on(TestController.class).resolveReturnType(CompletableFuture.class, entity(String.class));
		testHandle(returnValue, returnType);
	}

	@Test
	public void handleReturnValueLastModified() throws Exception {
		Instant currentTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		Instant oneMinAgo = currentTime.minusSeconds(60);
		long timestamp = currentTime.toEpochMilli();
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path").ifModifiedSince(timestamp));

		ResponseEntity<String> entity = ResponseEntity.ok().lastModified(oneMinAgo.toEpochMilli()).body("body");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		HandlerResult result = handlerResult(entity, returnType);
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertConditionalResponse(exchange, HttpStatus.NOT_MODIFIED, null, null, oneMinAgo);
	}

	@Test
	public void handleReturnValueEtag() throws Exception {
		String etagValue = "\"deadb33f8badf00d\"";
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path").ifNoneMatch(etagValue));

		ResponseEntity<String> entity = ResponseEntity.ok().eTag(etagValue).body("body");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		HandlerResult result = handlerResult(entity, returnType);
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertConditionalResponse(exchange, HttpStatus.NOT_MODIFIED, null, etagValue, Instant.MIN);
	}

	@Test  // SPR-14559
	public void handleReturnValueEtagInvalidIfNoneMatch() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path").ifNoneMatch("unquoted"));

		ResponseEntity<String> entity = ResponseEntity.ok().eTag("\"deadb33f8badf00d\"").body("body");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		HandlerResult result = handlerResult(entity, returnType);
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
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

		ResponseEntity<String> entity = ResponseEntity.ok().eTag(eTag).lastModified(oneMinAgo.toEpochMilli()).body("body");
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

		ResponseEntity<String> entity = ResponseEntity.ok().eTag(newEtag).lastModified(oneMinAgo.toEpochMilli()).body("body");
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
		HandlerResult result = new HandlerResult(new TestController(), Mono.just(ResponseEntity.ok().body("body")), type);

		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
		assertResponseBody(exchange, "body");
	}

	@Test  // SPR-14877
	public void handleMonoWithWildcardBodyTypeAndNullBody() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(APPLICATION_JSON));

		MethodParameter returnType = on(TestController.class).resolveReturnType(Mono.class, ResponseEntity.class);
		HandlerResult result = new HandlerResult(new TestController(), Mono.just(notFound().build()), returnType);

		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertResponseBodyIsEmpty(exchange);
	}

	@Test // SPR-17082
	public void handleWithPresetContentType() {
		ResponseEntity<Void> value = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).build();
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(Void.class));
		HandlerResult result = handlerResult(value, returnType);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(exchange.getResponse().getHeaders().size()).isEqualTo(1);
		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertResponseBodyIsEmpty(exchange);
	}

	@Test // gh-23205
	public void handleWithPresetContentTypeShouldFailWithServerError() {
		ResponseEntity<String> value = ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body("<foo/>");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		HandlerResult result = handlerResult(value, returnType);

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		ResponseEntityResultHandler resultHandler = new ResponseEntityResultHandler(
				Collections.singletonList(new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly())),
				new RequestedContentTypeResolverBuilder().build()
		);

		StepVerifier.create(resultHandler.handleResult(exchange, result))
				.consumeErrorWith(ex -> assertThat(ex)
						.isInstanceOf(HttpMessageNotWritableException.class)
						.hasMessageContaining("with preset Content-Type"))
				.verify();
	}

	@Test // gh-23287
	public void handleWithProducibleContentTypeShouldFailWithServerError() {
		ResponseEntity<String> value = ResponseEntity.ok().body("<foo/>");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		HandlerResult result = handlerResult(value, returnType);

		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		Set<MediaType> mediaTypes = Collections.singleton(MediaType.APPLICATION_XML);
				exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, mediaTypes);

		ResponseEntityResultHandler resultHandler = new ResponseEntityResultHandler(
				Collections.singletonList(new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly())),
				new RequestedContentTypeResolverBuilder().build()
		);

		StepVerifier.create(resultHandler.handleResult(exchange, result))
				.consumeErrorWith(ex -> assertThat(ex)
						.isInstanceOf(HttpMessageNotWritableException.class)
						.hasMessageContaining("with preset Content-Type"))
				.verify();
	}

	@Test // gh-26212
	public void handleWithObjectMapperByTypeRegistration() {
		MediaType halFormsMediaType = MediaType.parseMediaType("application/prs.hal-forms+json");
		MediaType halMediaType = MediaType.parseMediaType("application/hal+json");

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

		Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();
		encoder.registerObjectMappersForType(Person.class, map -> map.put(halMediaType, objectMapper));
		EncoderHttpMessageWriter<?> writer = new EncoderHttpMessageWriter<>(encoder);

		ResponseEntityResultHandler handler = new ResponseEntityResultHandler(
				Collections.singletonList(writer), new RequestedContentTypeResolverBuilder().build());

		MockServerWebExchange exchange = MockServerWebExchange.from(
				get("/path").header("Accept", halFormsMediaType + "," + halMediaType));

		ResponseEntity<Person> value = ResponseEntity.ok().body(new Person("Jason"));
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(Person.class));
		HandlerResult result = handlerResult(value, returnType);

		handler.handleResult(exchange, result).block();

		assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(halMediaType);
		assertThat(exchange.getResponse().getBodyAsString().block()).isEqualTo(
				"{" + NEWLINE_SYSTEM_PROPERTY +
						"  \"name\" : \"Jason\"" + NEWLINE_SYSTEM_PROPERTY +
						"}");
	}

	@Test  // gh-24539
	public void malformedAcceptHeader() {
		ResponseEntity<String> value = ResponseEntity.badRequest().body("Foo");
		MethodParameter returnType = on(TestController.class).resolveReturnType(entity(String.class));
		HandlerResult result = handlerResult(value, returnType);
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path").header("Accept", "null"));

		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));
		MockServerHttpResponse response = exchange.getResponse();
		response.setComplete().block();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getHeaders().getContentType()).isNull();
		assertResponseBodyIsEmpty(exchange);
	}


	private void testHandle(Object returnValue, MethodParameter returnType) {
		MockServerWebExchange exchange = MockServerWebExchange.from(get("/path"));
		HandlerResult result = handlerResult(returnValue, returnType);
		this.resultHandler.handleResult(exchange, result).block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(exchange.getResponse().getHeaders().getFirst("Content-Type")).isEqualTo("text/plain;charset=UTF-8");
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
				.consumeNextWith(buf -> assertThat(buf.toString(UTF_8)).isEqualTo(responseBody))
				.expectComplete()
				.verify();
	}

	private void assertResponseBodyIsEmpty(MockServerWebExchange exchange) {
		StepVerifier.create(exchange.getResponse().getBody()).expectComplete().verify();
	}

	private void assertConditionalResponse(MockServerWebExchange exchange, HttpStatus status,
			String body, String etag, Instant lastModified) throws Exception {

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(status);
		if (body != null) {
			assertResponseBody(exchange, body);
		}
		else {
			assertResponseBodyIsEmpty(exchange);
		}
		if (etag != null) {
			assertThat(exchange.getResponse().getHeaders().get(HttpHeaders.ETAG).size()).isEqualTo(1);
			assertThat(exchange.getResponse().getHeaders().getETag()).isEqualTo(etag);
		}
		if (lastModified.isAfter(Instant.EPOCH)) {
			assertThat(exchange.getResponse().getHeaders().get(HttpHeaders.LAST_MODIFIED).size()).isEqualTo(1);
			assertThat(exchange.getResponse().getHeaders().getLastModified()).isEqualTo(lastModified.toEpochMilli());
		}
	}


	@SuppressWarnings("unused")
	private static class TestController {

		ResponseEntity<String> responseEntityString() { return null; }

		ResponseEntity<Void> responseEntityVoid() { return null; }

		ResponseEntity<Person> responseEntityPerson() { return null; }

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


	@SuppressWarnings("unused")
	private static class Person {

		private String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
