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
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import rx.Completable;
import rx.Observable;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.ResolvableMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.web.reactive.HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;

/**
 * Unit tests for {@link AbstractMessageWriterResultHandler}.
 * @author Rossen Stoyanchev
 */
public class MessageWriterResultHandlerTests {

	private AbstractMessageWriterResultHandler resultHandler;

	private MockServerHttpResponse response = new MockServerHttpResponse();

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		this.resultHandler = createResultHandler();
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "/path");
		this.exchange = new DefaultServerWebExchange(request, this.response, new MockWebSessionManager());
	}


	@Test  // SPR-12894
	public void useDefaultContentType() throws Exception {
		Resource body = new ClassPathResource("logo.png", getClass());
		ResolvableType type = ResolvableType.forType(Resource.class);
		this.resultHandler.writeBody(body, returnType(type), this.exchange).block(Duration.ofSeconds(5));

		assertEquals("image/x-png", this.response.getHeaders().getFirst("Content-Type"));
	}

	@Test  // SPR-13631
	public void useDefaultCharset() throws Exception {
		this.exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE,
				Collections.singleton(APPLICATION_JSON));

		String body = "foo";
		ResolvableType type = ResolvableType.forType(String.class);
		this.resultHandler.writeBody(body, returnType(type), this.exchange).block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON_UTF8, this.response.getHeaders().getContentType());
	}

	@Test
	public void voidReturnType() throws Exception {
		testVoidReturnType(null, ResolvableType.forType(void.class));
		testVoidReturnType(Mono.empty(), ResolvableType.forClassWithGenerics(Mono.class, Void.class));
		testVoidReturnType(Completable.complete(), ResolvableType.forClass(Completable.class));
		testVoidReturnType(io.reactivex.Completable.complete(), ResolvableType.forClass(io.reactivex.Completable.class));
		testVoidReturnType(Flux.empty(), ResolvableType.forClassWithGenerics(Flux.class, Void.class));
		testVoidReturnType(Observable.empty(), ResolvableType.forClassWithGenerics(Observable.class, Void.class));
		testVoidReturnType(io.reactivex.Observable.empty(), ResolvableType.forClassWithGenerics(io.reactivex.Observable.class, Void.class));
		testVoidReturnType(Flowable.empty(), ResolvableType.forClassWithGenerics(Flowable.class, Void.class));
	}

	private void testVoidReturnType(Object body, ResolvableType type) {
		this.resultHandler.writeBody(body, returnType(type), this.exchange).block(Duration.ofSeconds(5));

		assertNull(this.response.getHeaders().get("Content-Type"));
		assertNull(this.response.getBody());
	}

	@Test  // SPR-13135
	public void unsupportedReturnType() throws Exception {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		ResolvableType type = ResolvableType.forType(OutputStream.class);

		HttpMessageWriter<?> writer = new EncoderHttpMessageWriter<>(new ByteBufferEncoder());
		Mono<Void> mono = createResultHandler(writer).writeBody(body, returnType(type), this.exchange);

		StepVerifier.create(mono).expectError(IllegalStateException.class).verify();
	}

	@Test  // SPR-12811
	public void jacksonTypeOfListElement() throws Exception {
		List<ParentClass> body = Arrays.asList(new Foo("foo"), new Bar("bar"));
		ResolvableType type = ResolvableType.forClassWithGenerics(List.class, ParentClass.class);
		this.resultHandler.writeBody(body, returnType(type), this.exchange).block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON_UTF8, this.response.getHeaders().getContentType());
		assertResponseBody("[{\"type\":\"foo\",\"parentProperty\":\"foo\"}," +
				"{\"type\":\"bar\",\"parentProperty\":\"bar\"}]");
	}

	@Test  // SPR-13318
	public void jacksonTypeWithSubType() throws Exception {
		SimpleBean body = new SimpleBean(123L, "foo");
		ResolvableType type = ResolvableType.forClass(Identifiable.class);
		this.resultHandler.writeBody(body, returnType(type), this.exchange).block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON_UTF8, this.response.getHeaders().getContentType());
		assertResponseBody("{\"id\":123,\"name\":\"foo\"}");
	}

	@Test  // SPR-13318
	public void jacksonTypeWithSubTypeOfListElement() throws Exception {
		List<SimpleBean> body = Arrays.asList(new SimpleBean(123L, "foo"), new SimpleBean(456L, "bar"));
		ResolvableType type = ResolvableType.forClassWithGenerics(List.class, Identifiable.class);
		this.resultHandler.writeBody(body, returnType(type), this.exchange).block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON_UTF8, this.response.getHeaders().getContentType());
		assertResponseBody("[{\"id\":123,\"name\":\"foo\"},{\"id\":456,\"name\":\"bar\"}]");
	}


	private MethodParameter returnType(ResolvableType bodyType) {
		return ResolvableMethod.onClass(TestController.class).returning(bodyType).resolveReturnType();
	}

	private AbstractMessageWriterResultHandler createResultHandler(HttpMessageWriter<?>... writers) {
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

	private void assertResponseBody(String responseBody) {
		StepVerifier.create(this.response.getBody())
				.consumeNextWith(buf -> assertEquals(responseBody,
						DataBufferTestUtils.dumpString(buf, StandardCharsets.UTF_8)))
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
