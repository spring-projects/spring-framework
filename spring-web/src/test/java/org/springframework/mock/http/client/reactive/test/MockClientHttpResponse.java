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

package org.springframework.mock.http.client.reactive.test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Mock implementation of {@link ClientHttpResponse}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MockClientHttpResponse implements ClientHttpResponse {

	private final HttpStatus status;

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, ResponseCookie> cookies = new LinkedMultiValueMap<>();

	private Flux<DataBuffer> body = Flux.empty();

	private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


	public MockClientHttpResponse(HttpStatus status) {
		Assert.notNull(status, "HttpStatus is required");
		this.status = status;
	}


	@Override
	public HttpStatus getStatusCode() {
		return this.status;
	}

	@Override
	public int getRawStatusCode() {
		return this.status.value();
	}

	@Override
	public HttpHeaders getHeaders() {
		String headerName = HttpHeaders.SET_COOKIE;
		if (!getCookies().isEmpty() && this.headers.get(headerName) == null) {
			getCookies().values().stream().flatMap(Collection::stream)
					.forEach(cookie -> getHeaders().add(headerName, cookie.toString()));
		}
		return this.headers;
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return this.cookies;
	}

	public void setBody(Publisher<DataBuffer> body) {
		this.body = Flux.from(body);
	}

	public void setBody(String body) {
		setBody(body, StandardCharsets.UTF_8);
	}

	public void setBody(String body, Charset charset) {
		DataBuffer buffer = toDataBuffer(body, charset);
		this.body = Flux.just(buffer);
	}

	private DataBuffer toDataBuffer(String body, Charset charset) {
		byte[] bytes = body.getBytes(charset);
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
		return this.bufferFactory.wrap(byteBuffer);
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	/**
	 * Return the response body aggregated and converted to a String using the
	 * charset of the Content-Type response or otherwise as "UTF-8".
	 */
	public Mono<String> getBodyAsString() {
		Charset charset = getCharset();
		return Flux.from(getBody())
				.reduce(bufferFactory.allocateBuffer(), (previous, current) -> {
					previous.write(current);
					DataBufferUtils.release(current);
					return previous;
				})
				.map(buffer -> dumpString(buffer, charset));
	}

	private static String dumpString(DataBuffer buffer, Charset charset) {
		Assert.notNull(charset, "'charset' must not be null");
		byte[] bytes = new byte[buffer.readableByteCount()];
		buffer.read(bytes);
		return new String(bytes, charset);
	}

	private Charset getCharset() {
		Charset charset = null;
		MediaType contentType = getHeaders().getContentType();
		if (contentType != null) {
			charset = contentType.getCharset();
		}
		return (charset != null ? charset : StandardCharsets.UTF_8);
	}

}
