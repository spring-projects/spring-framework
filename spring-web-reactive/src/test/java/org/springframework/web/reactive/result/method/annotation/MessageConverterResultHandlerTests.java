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
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.test.TestSubscriber;
import rx.Observable;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.support.ByteBufferEncoder;
import org.springframework.core.codec.support.StringEncoder;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.convert.support.ReactiveStreamsToCompletableFutureConverter;
import org.springframework.core.convert.support.ReactiveStreamsToRxJava1Converter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.codec.xml.Jaxb2Encoder;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.converter.reactive.ResourceHttpMessageConverter;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.web.reactive.HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;

/**
 * Unit tests for {@link AbstractMessageConverterResultHandler}.
 * @author Rossen Stoyanchev
 */
public class MessageConverterResultHandlerTests {

	private AbstractMessageConverterResultHandler resultHandler;

	private MockServerHttpResponse response = new MockServerHttpResponse();

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		this.resultHandler = createResultHandler();
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/path"));
		this.exchange = new DefaultServerWebExchange(request, this.response, new MockWebSessionManager());
	}


	@Test // SPR-12894
	public void useDefaultContentType() throws Exception {
		Resource body = new ClassPathResource("logo.png", getClass());
		ResolvableType bodyType = ResolvableType.forType(Resource.class);
		this.resultHandler.writeBody(this.exchange, body, bodyType).block(Duration.ofSeconds(5));

		assertEquals("image/x-png", this.response.getHeaders().getFirst("Content-Type"));
	}

	@Test // SPR-13631
	public void useDefaultCharset() throws Exception {
		this.exchange.getAttributes().put(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE,
				Collections.singleton(APPLICATION_JSON));

		String body = "foo";
		ResolvableType bodyType = ResolvableType.forType(String.class);
		this.resultHandler.writeBody(this.exchange, body, bodyType).block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON_UTF8, this.response.getHeaders().getContentType());
	}

	@Test
	public void voidReturnType() throws Exception {
		testVoidReturnType(null, ResolvableType.forType(Void.class));
		testVoidReturnType(Mono.empty(), ResolvableType.forClassWithGenerics(Mono.class, Void.class));
		testVoidReturnType(Flux.empty(), ResolvableType.forClassWithGenerics(Flux.class, Void.class));
		testVoidReturnType(Observable.empty(), ResolvableType.forClassWithGenerics(Observable.class, Void.class));
	}

	private void testVoidReturnType(Object body, ResolvableType bodyType) {
		this.resultHandler.writeBody(this.exchange, body, bodyType).block(Duration.ofSeconds(5));

		assertNull(this.response.getHeaders().get("Content-Type"));
		assertNull(this.response.getBody());
	}

	@Test // SPR-13135
	public void unsupportedReturnType() throws Exception {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		ResolvableType bodyType = ResolvableType.forType(OutputStream.class);

		HttpMessageConverter<?> converter = new CodecHttpMessageConverter<>(new ByteBufferEncoder());
		Mono<Void> mono = createResultHandler(converter).writeBody(this.exchange, body, bodyType);

		TestSubscriber.subscribe(mono).assertError(IllegalStateException.class);
	}

	@Test // SPR-12811
	@Ignore
	public void jacksonTypeOfListElement() throws Exception {
		List<ParentClass> body = Arrays.asList(new Foo("foo"), new Bar("bar"));
		ResolvableType bodyType = ResolvableType.forClassWithGenerics(List.class, ParentClass.class);
		this.resultHandler.writeBody(this.exchange, body, bodyType).block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON_UTF8, this.response.getHeaders().getContentType());
		assertResponseBody("[{\"type\":\"foo\",\"parentProperty\":\"foo\"}," +
				"{\"type\":\"bar\",\"parentProperty\":\"bar\"}]");
	}

	@Test // SPR-13318
	@Ignore
	public void jacksonTypeWithSubType() throws Exception {
		SimpleBean body = new SimpleBean(123L, "foo");
		ResolvableType bodyType = ResolvableType.forClass(Identifiable.class);
		this.resultHandler.writeBody(this.exchange, body, bodyType).block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON_UTF8, this.response.getHeaders().getContentType());
		assertResponseBody("{\"id\":123,\"name\":\"foo\"}");
	}

	@Test // SPR-13318
	@Ignore
	public void jacksonTypeWithSubTypeOfListElement() throws Exception {
		List<SimpleBean> body = Arrays.asList(new SimpleBean(123L, "foo"), new SimpleBean(456L, "bar"));
		ResolvableType bodyType = ResolvableType.forClassWithGenerics(List.class, Identifiable.class);
		this.resultHandler.writeBody(this.exchange, body, bodyType).block(Duration.ofSeconds(5));

		assertEquals(APPLICATION_JSON_UTF8, this.response.getHeaders().getContentType());
		assertResponseBody("[{\"id\":123,\"name\":\"foo\"},{\"id\":456,\"name\":\"bar\"}]");
	}


	private AbstractMessageConverterResultHandler createResultHandler(HttpMessageConverter<?>... converters) {
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

		return new AbstractMessageConverterResultHandler(converterList, service, resolver) {};
	}

	private void assertResponseBody(String responseBody) {
		TestSubscriber.subscribe(this.response.getBody())
				.assertValuesWith(buf -> assertEquals(responseBody,
						DataBufferTestUtils.dumpString(buf, Charset.forName("UTF-8"))));
	}


	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@SuppressWarnings("unused")
	private static class ParentClass {

		private String parentProperty;

		public ParentClass() {
		}

		public ParentClass(String parentProperty) {
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

		public Bar(String parentProperty) {
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

		public SimpleBean(Long id, String name) {
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

}
