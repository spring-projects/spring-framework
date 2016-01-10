/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.nio.ByteBuffer;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import org.reactivestreams.Publisher;
import reactor.Flux;
import reactor.Mono;
import reactor.core.publisher.convert.RxJava1Converter;
import rx.Observable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the RxNetty {@link HttpServerResponse}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 */
public class RxNettyServerHttpResponse extends AbstractServerHttpResponse {

	private final HttpServerResponse<?> response;


	public RxNettyServerHttpResponse(HttpServerResponse<?> response) {
		Assert.notNull("'response', response must not be null.");
		this.response = response;
	}


	public HttpServerResponse<?> getRxNettyResponse() {
		return this.response;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		getRxNettyResponse().setStatus(HttpResponseStatus.valueOf(status.value()));
	}

	@Override
	protected Mono<Void> setBodyInternal(Publisher<ByteBuffer> publisher) {
		Observable<byte[]> content = RxJava1Converter.from(publisher).map(this::toBytes);
		Observable<Void> completion = getRxNettyResponse().writeBytes(content);
		return RxJava1Converter.from(completion).after();
	}

	private byte[] toBytes(ByteBuffer buffer) {
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		return bytes;
	}

	@Override
	protected void writeHeadersInternal() {
		for (String name : getHeaders().keySet()) {
			for (String value : getHeaders().get(name))
				this.response.addHeader(name, value);
		}
	}

}