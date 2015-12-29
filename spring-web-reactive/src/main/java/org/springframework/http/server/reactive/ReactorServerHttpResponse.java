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
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.Publishers;
import reactor.io.buffer.Buffer;
import reactor.io.net.http.HttpChannel;
import reactor.io.net.http.model.Status;

import org.springframework.http.ExtendedHttpHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 */
public class ReactorServerHttpResponse implements ServerHttpResponse {

	private final HttpChannel<?, Buffer> channel;

	private final HttpHeaders headers;


	public ReactorServerHttpResponse(HttpChannel<?, Buffer> response) {
		Assert.notNull("'response', response must not be null.");
		this.channel = response;
		this.headers = initHttpHeaders();
	}

	private HttpHeaders initHttpHeaders() {
		ExtendedHttpHeaders headers = new ExtendedHttpHeaders();
		headers.registerChangeListener(new ReactorHeaderChangeListener());
		return headers;
	}


	@Override
	public void setStatusCode(HttpStatus status) {
		this.channel.responseStatus(Status.valueOf(status.value()));
	}

	@Override
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	@Override
	public Publisher<Void> setBody(Publisher<ByteBuffer> publisher) {
		return Publishers.lift(publisher, new WriteWithOperator<>(writePublisher ->
				this.channel.writeWith(Publishers.map(writePublisher, Buffer::new))));
	}


	private class ReactorHeaderChangeListener implements ExtendedHttpHeaders.HeaderChangeListener {

		@Override
		public void headerAdded(String name, String value) {
			channel.responseHeaders().add(name, value);
		}

		@Override
		public void headerPut(String key, List<String> values) {
			channel.responseHeaders().remove(key);
			channel.responseHeaders().add(key, values);
		}

		@Override
		public void headerRemoved(String key) {
			channel.responseHeaders().remove(key);
		}
	}

}
