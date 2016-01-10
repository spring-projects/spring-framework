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

import org.reactivestreams.Publisher;
import reactor.Flux;
import reactor.Mono;
import reactor.io.buffer.Buffer;
import reactor.io.net.http.HttpChannel;
import reactor.io.net.http.model.Status;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Reactor Net {@link HttpChannel}.
 *
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 */
public class ReactorServerHttpResponse implements ServerHttpResponse {

	private final HttpChannel<?, Buffer> channel;

	private final HttpHeaders headers;

	private boolean headersWritten = false;


	public ReactorServerHttpResponse(HttpChannel<?, Buffer> response) {
		Assert.notNull("'response' must not be null.");
		this.channel = response;
		this.headers = new HttpHeaders();
	}


	public HttpChannel<?, Buffer> getReactorChannel() {
		return this.channel;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		getReactorChannel().responseStatus(Status.valueOf(status.value()));
	}

	@Override
	public HttpHeaders getHeaders() {
		return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public Mono<Void> setBody(Publisher<ByteBuffer> publisher) {
		return Flux.from(publisher).lift(new WriteWithOperator<>(this::setBodyInternal)).after();
	}

	protected Mono<Void> setBodyInternal(Publisher<ByteBuffer> publisher) {
		writeHeaders();
		return Mono.from(getReactorChannel().writeWith(Flux.from(publisher).map(Buffer::new)));
	}

	@Override
	public void writeHeaders() {
		if (!this.headersWritten) {
			for (String name : this.headers.keySet()) {
				for (String value : this.headers.get(name)) {
					this.channel.responseHeaders().add(name, value);
				}
			}
			this.headersWritten = true;
		}
	}

}
