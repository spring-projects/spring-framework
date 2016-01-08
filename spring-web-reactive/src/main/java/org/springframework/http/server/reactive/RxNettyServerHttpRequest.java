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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import reactor.Flux;
import reactor.core.publisher.convert.RxJava1Converter;
import rx.Observable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpRequest} to the RxNetty {@link HttpServerRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 */
public class RxNettyServerHttpRequest implements ServerHttpRequest {

	private final HttpServerRequest<ByteBuf> request;

	private URI uri;

	private HttpHeaders headers;


	public RxNettyServerHttpRequest(HttpServerRequest<ByteBuf> request) {
		Assert.notNull("'request', request must not be null.");
		this.request = request;
	}


	public HttpServerRequest<ByteBuf> getRxNettyRequest() {
		return this.request;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.getRxNettyRequest().getHttpMethod().name());
	}

	@Override
	public URI getURI() {
		if (this.uri == null) {
			try {
				this.uri = new URI(this.getRxNettyRequest().getUri());
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException("Could not get URI: " + ex.getMessage(), ex);
			}
		}
		return this.uri;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			for (String name : this.getRxNettyRequest().getHeaderNames()) {
				this.headers.put(name, this.getRxNettyRequest().getAllHeaderValues(name));
			}
		}
		return this.headers;
	}

	@Override
	public Flux<ByteBuffer> getBody() {
		Observable<ByteBuffer> content = this.getRxNettyRequest().getContent().map(ByteBuf::nioBuffer);
		content = content.concatWith(Observable.empty()); // See GH issue #58
		return RxJava1Converter.from(content);
	}

}
