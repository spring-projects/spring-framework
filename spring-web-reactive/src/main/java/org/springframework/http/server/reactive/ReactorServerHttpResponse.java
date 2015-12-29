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
import reactor.Publishers;
import reactor.io.buffer.Buffer;
import reactor.io.net.http.HttpChannel;
import reactor.io.net.http.model.Status;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * @author Stephane Maldini
 */
public class ReactorServerHttpResponse implements ServerHttpResponse {

	private final HttpChannel<?, Buffer> channel;

	private final HttpHeaders headers;

	private boolean headersWritten = false;


	public ReactorServerHttpResponse(HttpChannel<?, Buffer> response) {
		Assert.notNull("'response', response must not be null.");
		this.channel = response;
		this.headers = new HttpHeaders();
	}


	@Override
	public void setStatusCode(HttpStatus status) {
		this.channel.responseStatus(Status.valueOf(status.value()));
	}

	@Override
	public HttpHeaders getHeaders() {
		return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	@Override
	public Publisher<Void> writeHeaders() {
		if (this.headersWritten) {
			return Publishers.empty();
		}
		applyHeaders();
		return this.channel.writeHeaders();
	}

	@Override
	public Publisher<Void> setBody(Publisher<ByteBuffer> publisher) {
		return Publishers.lift(publisher, new WriteWithOperator<>(writePublisher -> {
			applyHeaders();
			return this.channel.writeWith(Publishers.map(writePublisher, Buffer::new));
		}));
	}

	private void applyHeaders() {
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
