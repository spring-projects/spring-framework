/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.nio.file.Path;

import io.netty5.buffer.Buffer;
import io.netty5.channel.ChannelId;
import io.netty5.handler.codec.http.headers.DefaultHttpSetCookie;
import io.netty5.handler.codec.http.headers.HttpSetCookie;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty5.ChannelOperationsId;
import reactor.netty5.http.server.HttpServerResponse;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.Netty5DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.support.Netty5HeadersAdapter;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the {@link HttpServerResponse}.
 *
 * <p>This class is based on {@link ReactorServerHttpResponse}.
 *
 * @author Violeta Georgieva
 * @since 6.0
 */
class ReactorNetty2ServerHttpResponse extends AbstractServerHttpResponse implements ZeroCopyHttpOutputMessage {

	private static final Log logger = LogFactory.getLog(ReactorNetty2ServerHttpResponse.class);


	private final HttpServerResponse response;


	public ReactorNetty2ServerHttpResponse(HttpServerResponse response, DataBufferFactory bufferFactory) {
		super(bufferFactory, new HttpHeaders(new Netty5HeadersAdapter(response.responseHeaders())));
		Assert.notNull(response, "HttpServerResponse must not be null");
		this.response = response;
	}


	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeResponse() {
		return (T) this.response;
	}

	@Override
	public HttpStatusCode getStatusCode() {
		HttpStatusCode status = super.getStatusCode();
		return (status != null ? status : HttpStatusCode.valueOf(this.response.status().code()));
	}

	@Override
	@Deprecated
	public Integer getRawStatusCode() {
		Integer status = super.getRawStatusCode();
		return (status != null ? status : this.response.status().code());
	}

	@Override
	protected void applyStatusCode() {
		HttpStatusCode status = super.getStatusCode();
		if (status != null) {
			this.response.status(status.value());
		}
	}

	@Override
	protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> publisher) {
		return this.response.send(toByteBufs(publisher)).then();
	}

	@Override
	protected Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> publisher) {
		return this.response.sendGroups(Flux.from(publisher).map(this::toByteBufs)).then();
	}

	@Override
	protected void applyHeaders() {
	}

	@Override
	protected void applyCookies() {
		for (String name : getCookies().keySet()) {
			for (ResponseCookie httpCookie : getCookies().get(name)) {
				Long maxAge = (!httpCookie.getMaxAge().isNegative()) ? httpCookie.getMaxAge().getSeconds() : null;
				HttpSetCookie.SameSite sameSite = (httpCookie.getSameSite() != null) ? HttpSetCookie.SameSite.valueOf(httpCookie.getSameSite()) : null;
				// TODO: support Partitioned attribute when available in Netty 5 API
				DefaultHttpSetCookie cookie = new DefaultHttpSetCookie(name, httpCookie.getValue(), httpCookie.getPath(),
						httpCookie.getDomain(), null, maxAge, sameSite, false, httpCookie.isSecure(), httpCookie.isHttpOnly());
				this.response.addCookie(cookie);
			}
		}
	}

	@Override
	public Mono<Void> writeWith(Path file, long position, long count) {
		return doCommit(() -> this.response.sendFile(file, position, count).then());
	}

	private Publisher<Buffer> toByteBufs(Publisher<? extends DataBuffer> dataBuffers) {
		return dataBuffers instanceof Mono ?
				Mono.from(dataBuffers).map(Netty5DataBufferFactory::toBuffer) :
				Flux.from(dataBuffers).map(Netty5DataBufferFactory::toBuffer);
	}

	@Override
	protected void touchDataBuffer(DataBuffer buffer) {
		if (logger.isDebugEnabled()) {
			if (this.response instanceof ChannelOperationsId operationsId) {
				DataBufferUtils.touch(buffer, "Channel id: " + operationsId.asLongText());
			}
			else {
				this.response.withConnection(connection -> {
					ChannelId id = connection.channel().id();
					DataBufferUtils.touch(buffer, "Channel id: " + id.asShortText());
				});
			}
		}
	}

}
