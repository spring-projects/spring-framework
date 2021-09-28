/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.testfixture.io.buffer.LeakAwareDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;

/**
 * Response that subscribes to the writes source but never posts demand and also
 * offers method to then cancel the subscription, and check of leaks in the end.
 *
 * @author Rossen Stoyanchev
 */
public class ZeroDemandResponse implements ServerHttpResponse {

	private final LeakAwareDataBufferFactory bufferFactory;

	private final ZeroDemandSubscriber writeSubscriber = new ZeroDemandSubscriber();


	public ZeroDemandResponse() {
		this.bufferFactory = new LeakAwareDataBufferFactory();
	}


	public void checkForLeaks() {
		this.bufferFactory.checkForLeaks();
	}

	public void cancelWrite() {
		this.writeSubscriber.cancel();
	}


	@Override
	public DataBufferFactory bufferFactory() {
		return this.bufferFactory;
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		body.subscribe(this.writeSubscriber);
		return Mono.never();
	}


	@Override
	public boolean setStatusCode(HttpStatus status) {
		throw new UnsupportedOperationException();
	}

	@Override
	public HttpStatus getStatusCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addCookie(ResponseCookie cookie) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCommitted() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Mono<Void> setComplete() {
		throw new UnsupportedOperationException();
	}

	@Override
	public HttpHeaders getHeaders() {
		throw new UnsupportedOperationException();
	}


	private static class ZeroDemandSubscriber extends BaseSubscriber<DataBuffer> {

		@Override
		protected void hookOnSubscribe(Subscription subscription) {
			// Just subscribe without requesting
		}
	}
}
