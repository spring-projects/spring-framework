/*
 * Copyright 2002-2017 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.reactivex.Flowable;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import rx.Completable;
import rx.Observable;

import org.springframework.core.MethodParameter;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerWebExchange;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.core.io.buffer.support.DataBufferTestUtils.dumpString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.web.method.ResolvableMethod.on;
import static org.springframework.web.reactive.HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;

/**
 * Unit tests for {@link AbstractMessageWriterResultHandler}.
 * @author Rossen Stoyanchev
 */
public class MessageWriterResultHandlerTests {

	private final AbstractMessageWriterResultHandler resultHandler = initResultHandler();

	private final MockServerWebExchange exchange = MockServerHttpRequest.get("/path").toExchange();


	private AbstractMessageWriterResultHandler initResultHandler(HttpMessageWriter<?>... writers) {
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
		return new AbstractMessageWriterResultHandler(writerList, resolver) {};
	}


	@Test  // SPR-12894
	public void useDefaultContentType() throws Exception {
		Resource body = new ClassPathResource("logo.png", getClass());
		MethodParameter type = on(TestController.class).resolveReturnType(Resource.class);
		this.resultHandler.writeBody(body, type, this.exchange).block(Duration.ofSeconds(5));

		assertEquals("image/png", this.exchange.getResponse().getHeaders().getFirst("Content-Type"));
	}

	@Test  // SPR-13631
	public void useDefaultCharset() throws Exception {
		this.exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE,
				Collections.singleton(APPLICATION_JSON));

		String body = "foo";
		MethodParameter type = on(TestController.class).resolveReturnType(String.class);
		this.resultHandler.writeBody(body, type, this.exchange).block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON_UTF8, this.exchange.getResponse().getHeaders().getContentType());
	}

	@Test
	public void voidReturnType() throws Exception {
		testVoid(null, on(TestController.class).resolveReturnType(void.class));
		testVoid(Mono.empty(), on(TestController.class).resolveReturnType(Mono.class, Void.class));
		testVoid(Flux.empty(), on(TestController.class).resolveReturnType(Flux.class, Void.class));
		testVoid(Completable.complete(), on(TestController.class).resolveReturnType(Completable.class));
		testVoid(Observable.empty(), on(TestController.class).resolveReturnType(Observable.class, Void.class));

		MethodParameter type = on(TestController.class).resolveReturnType(io.reactivex.Completable.class);
		testVoid(io.reactivex.Completable.complete(), type);

		type = on(TestController.class).resolveReturnType(io.reactivex.Observable.class, Void.class);
		testVoid(io.reactivex.Observable.empty(), type);

		type = on(TestController.class).resolveReturnType(Flowable.class, Void.class);
		testVoid(Flowable.empty(), type);
	}

	private void testVoid(Object body, MethodParameter returnType) {
		this.resultHandler.writeBody(body, returnType, this.exchange).block(Duration.ofSeconds(5));

		assertNull(this.exchange.getResponse().getHeaders().get("Content-Type"));
		StepVerifier.create(this.exchange.getResponse().getBody())
				.expectErrorMatches(ex -> ex.getMessage().startsWith("The body is not set.")).verify();
	}

	@Test  // SPR-13135
	public void unsupportedReturnType() throws Exception {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		MethodParameter type = on(TestController.class).resolveReturnType(OutputStream.class);

		HttpMessageWriter<?> writer = new EncoderHttpMessageWriter<>(new ByteBufferEncoder());
		Mono<Void> mono = initResultHandler(writer).writeBody(body, type, this.exchange);

		StepVerifier.create(mono).expectError(IllegalStateException.class).verify();
	}

	@Test  // SPR-12811
	public void jacksonTypeOfListElement() throws Exception {

		MethodParameter returnType = on(TestController.class).resolveReturnType(List.class, ParentClass.class);
		List<ParentClass> body = Arrays.asList(new Foo("foo"), new Bar("bar"));
		this.resultHandler.writeBody(body, returnType, this.exchange).block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON_UTF8, this.exchange.getResponse().getHeaders().getContentType());
		assertResponseBody("[{\"type\":\"foo\",\"parentProperty\":\"foo\"}," +
				"{\"type\":\"bar\",\"parentProperty\":\"bar\"}]");
	}

	@Test  // SPR-13318
	public void jacksonTypeWithSubType() throws Exception {
		SimpleBean body = new SimpleBean(123L, "foo");
		MethodParameter type = on(TestController.class).resolveReturnType(Identifiable.class);
		this.resultHandler.writeBody(body, type, this.exchange).block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON_UTF8, this.exchange.getResponse().getHeaders().getContentType());
		assertResponseBody("{\"id\":123,\"name\":\"foo\"}");
	}

	@Test  // SPR-13318
	public void jacksonTypeWithSubTypeOfListElement() throws Exception {

		MethodParameter returnType = on(TestController.class).resolveReturnType(List.class, Identifiable.class);

		List<SimpleBean> body = Arrays.asList(new SimpleBean(123L, "foo"), new SimpleBean(456L, "bar"));
		this.resultHandler.writeBody(body, returnType, this.exchange).block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON_UTF8, this.exchange.getResponse().getHeaders().getContentType());
		assertResponseBody("[{\"id\":123,\"name\":\"foo\"},{\"id\":456,\"name\":\"bar\"}]");
	}


	private void assertResponseBody(String responseBody) {
		StepVerifier.create(this.exchange.getResponse().getBody())
				.consumeNextWith(buf -> assertEquals(responseBody, dumpString(buf, StandardCharsets.UTF_8)))
				.expectComplete()
				.verify();
	}


	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@SuppressWarnings("unused")
	private static class ParentClass {

		private String parentProperty;

		public ParentClass() {
		}

		ParentClass(String parentProperty) {
			this.parentProperty = parentProperty;
		}

		public String getParentProperty() {
			return parentProperty;
		}

		public void setParentProperty(String parentProperty) {
			this.parentProperty = parentProperty;
		}
	}


	@JsonTypeName("foo")
	private static class Foo extends ParentClass {

		public Foo(String parentProperty) {
			super(parentProperty);
		}
	}


	@JsonTypeName("bar")
	private static class Bar extends ParentClass {

		Bar(String parentProperty) {
			super(parentProperty);
		}
	}


	private interface Identifiable extends Serializable {

		@SuppressWarnings("unused")
		Long getId();
	}


	@SuppressWarnings({ "serial" })
	private static class SimpleBean implements Identifiable {

		private Long id;

		private String name;

		SimpleBean(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public Long getId() {
			return id;
		}

		@SuppressWarnings("unused")
		public String getName() {
			return name;
		}
	}


	@SuppressWarnings("unused")
	private static class TestController {

		Resource resource() { return null; }

		String string() { return null; }

		void voidReturn() { }

		Mono<Void> monoVoid() { return null; }

		Completable completable() { return null; }

		io.reactivex.Completable rxJava2Completable() { return null; }

		Flux<Void> fluxVoid() { return null; }

		Observable<Void> observableVoid() { return null; }

		io.reactivex.Observable<Void> rxJava2ObservableVoid() { return null; }

		Flowable<Void> flowableVoid() { return null; }

		OutputStream outputStream() { return null; }

		List<ParentClass> listParentClass() { return null; }

		Identifiable identifiable() { return null; }

		List<Identifiable> listIdentifiable() { return null; }
	}

}
