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

package org.springframework.web.reactive.function;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.reactive.result.view.View;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Arjen Poutsma
 */
public class DefaultResponseBuilderTests {

	@Test
	public void from() throws Exception {
		Response<Void> other = Response.ok().header("foo", "bar").build();
		Response<Void> result = Response.from(other).build();
		assertEquals(HttpStatus.OK, result.statusCode());
		assertEquals("bar", result.headers().getFirst("foo"));
	}

	@Test
	public void status() throws Exception {
		Response<Void> result = Response.status(HttpStatus.CREATED).build();
		assertEquals(HttpStatus.CREATED, result.statusCode());
	}

	@Test
	public void statusInt() throws Exception {
		Response<Void> result = Response.status(201).build();
		assertEquals(HttpStatus.CREATED, result.statusCode());
	}

	@Test
	public void ok() throws Exception {
		Response<Void> result = Response.ok().build();
		assertEquals(HttpStatus.OK, result.statusCode());
	}

	@Test
	public void created() throws Exception {
		URI location = URI.create("http://example.com");
		Response<Void> result = Response.created(location).build();
		assertEquals(HttpStatus.CREATED, result.statusCode());
		assertEquals(location, result.headers().getLocation());
	}

	@Test
	public void accepted() throws Exception {
		Response<Void> result = Response.accepted().build();
		assertEquals(HttpStatus.ACCEPTED, result.statusCode());
	}

	@Test
	public void noContent() throws Exception {
		Response<Void> result = Response.noContent().build();
		assertEquals(HttpStatus.NO_CONTENT, result.statusCode());
	}

	@Test
	public void badRequest() throws Exception {
		Response<Void> result = Response.badRequest().build();
		assertEquals(HttpStatus.BAD_REQUEST, result.statusCode());
	}

	@Test
	public void notFound() throws Exception {
		Response<Void> result = Response.notFound().build();
		assertEquals(HttpStatus.NOT_FOUND, result.statusCode());
	}

	@Test
	public void unprocessableEntity() throws Exception {
		Response<Void> result = Response.unprocessableEntity().build();
		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.statusCode());
	}

	@Test
	public void allow() throws Exception {
		Response<Void> result = Response.ok().allow(HttpMethod.GET).build();
		assertEquals(Collections.singleton(HttpMethod.GET), result.headers().getAllow());
	}

	@Test
	public void contentLength() throws Exception {
		Response<Void> result = Response.ok().contentLength(42).build();
		assertEquals(42, result.headers().getContentLength());
	}

	@Test
	public void contentType() throws Exception {
		Response<Void> result = Response.ok().contentType(MediaType.APPLICATION_JSON).build();
		assertEquals(MediaType.APPLICATION_JSON, result.headers().getContentType());
	}

	@Test
	public void eTag() throws Exception {
		Response<Void> result = Response.ok().eTag("foo").build();
		assertEquals("\"foo\"", result.headers().getETag());
	}

	@Test
	public void lastModified() throws Exception {
		ZonedDateTime now = ZonedDateTime.now();
		Response<Void> result = Response.ok().lastModified(now).build();
		assertEquals(now.toInstant().toEpochMilli()/1000, result.headers().getLastModified()/1000);
	}

	@Test
	public void cacheControlTag() throws Exception {
		Response<Void> result = Response.ok().cacheControl(CacheControl.noCache()).build();
		assertEquals("no-cache", result.headers().getCacheControl());
	}

	@Test
	public void varyBy() throws Exception {
		Response<Void> result = Response.ok().varyBy("foo").build();
		assertEquals(Collections.singletonList("foo"), result.headers().getVary());
	}

	@Test
	public void statusCode() throws Exception {
		HttpStatus statusCode = HttpStatus.ACCEPTED;
		Response<Void> result = Response.status(statusCode).build();
		assertSame(statusCode, result.statusCode());
	}

	@Test
	public void headers() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		Response<Void> result = Response.ok().headers(headers).build();
		assertEquals(headers, result.headers());
	}

	@Test
	public void build() throws Exception {
		Response<Void> result = Response.status(201).header("MyKey", "MyValue").build();

		ServerWebExchange exchange = mock(ServerWebExchange.class);
		MockServerHttpResponse response = new MockServerHttpResponse();
		when(exchange.getResponse()).thenReturn(response);
		Configuration configuration = mock(Configuration.class);

		result.writeTo(exchange, configuration).block();
		assertEquals(201, response.getStatusCode().value());
		assertEquals("MyValue", response.getHeaders().getFirst("MyKey"));
		assertNull(response.getBody());

	}

	@Test
	public void buildVoidPublisher() throws Exception {
		Mono<Void> mono = Mono.empty();
		Response<Mono<Void>> result = Response.ok().build(mono);

		ServerWebExchange exchange = mock(ServerWebExchange.class);
		MockServerHttpResponse response = new MockServerHttpResponse();
		when(exchange.getResponse()).thenReturn(response);
		Configuration configuration = mock(Configuration.class);

		result.writeTo(exchange, configuration).block();
		assertNull(response.getBody());
	}

	@Test
	public void bodyInsertor() throws Exception {
		String body = "foo";
		Supplier<String> supplier = () -> body;
		BiFunction<ServerHttpResponse, Configuration, Mono<Void>> writer =
				(response, configuration) -> {
					byte[] bodyBytes = body.getBytes(UTF_8);
					ByteBuffer byteBuffer = ByteBuffer.wrap(bodyBytes);
					DataBuffer buffer = new DefaultDataBufferFactory().wrap(byteBuffer);

					return response.writeWith(Mono.just(buffer));
				};

		Response<String> result = Response.ok().body(writer, supplier);
		assertEquals(body, result.body());

		MockServerHttpRequest request =
				new MockServerHttpRequest(HttpMethod.GET, "http://localhost");
		MockServerHttpResponse response = new MockServerHttpResponse();
		ServerWebExchange exchange =
				new DefaultServerWebExchange(request, response, new MockWebSessionManager());

		List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();
		messageWriters.add(new EncoderHttpMessageWriter<CharSequence>(new CharSequenceEncoder()));

		Configuration configuration = mock(Configuration.class);
		when(configuration.messageWriters()).thenReturn(messageWriters::stream);

		result.writeTo(exchange, configuration).block();
		assertNotNull(response.getBody());
	}

	@Test
	public void render() throws Exception {
		Map<String, Object> model = Collections.singletonMap("foo", "bar");
		Response<Rendering> result = Response.ok().render("view", model);

		assertEquals("view", result.body().name());
		assertEquals(model, result.body().model());

		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, URI.create("http://localhost"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		ServerWebExchange exchange = new DefaultServerWebExchange(request, response, new MockWebSessionManager());
		ViewResolver viewResolver = mock(ViewResolver.class);
		View view = mock(View.class);
		when(viewResolver.resolveViewName("view", Locale.ENGLISH)).thenReturn(Mono.just(view));
		when(view.render(model, null, exchange)).thenReturn(Mono.empty());

		List<ViewResolver> viewResolvers = new ArrayList<>();
		viewResolvers.add(viewResolver);

		Configuration mockConfig = mock(Configuration.class);
		when(mockConfig.viewResolvers()).thenReturn(viewResolvers::stream);

		result.writeTo(exchange, mockConfig).block();
	}

	@Test
	public void renderObjectArray() throws Exception {
		Response<Rendering> result =
				Response.ok().render("name", this, Collections.emptyList(), "foo");
		Map<String, Object> model = result.body().model();
		assertEquals(2, model.size());
		assertEquals(this, model.get("defaultResponseBuilderTests"));
		assertEquals("foo", model.get("string"));
	}

}