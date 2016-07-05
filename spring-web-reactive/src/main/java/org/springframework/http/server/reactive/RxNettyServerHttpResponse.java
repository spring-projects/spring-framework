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

package org.springframework.http.server.reactive;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import org.reactivestreams.Publisher;
import reactor.core.converter.RxJava1ObservableConverter;
import reactor.core.publisher.Mono;
import rx.Observable;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.FlushingDataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the RxNetty {@link HttpServerResponse}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 */
public class RxNettyServerHttpResponse extends AbstractServerHttpResponse {

	private final HttpServerResponse<ByteBuf> response;


	public RxNettyServerHttpResponse(HttpServerResponse<ByteBuf> response,
			NettyDataBufferFactory dataBufferFactory) {
		super(dataBufferFactory);
		Assert.notNull("'response', response must not be null.");

		this.response = response;
	}


	public HttpServerResponse<?> getRxNettyResponse() {
		return this.response;
	}


	@Override
	protected void writeStatusCode() {
		HttpStatus statusCode = this.getStatusCode();
		if (statusCode != null) {
			this.response.setStatus(HttpResponseStatus.valueOf(statusCode.value()));
		}
	}

	@Override
	protected Mono<Void> writeWithInternal(Publisher<DataBuffer> body) {
		Observable<ByteBuf> content = RxJava1ObservableConverter.from(body).map(this::toByteBuf);
		return RxJava1ObservableConverter.from(this.response.write(content, bb -> bb instanceof FlushingByteBuf)).then();
	}

	private ByteBuf toByteBuf(DataBuffer buffer) {
		ByteBuf byteBuf = (buffer instanceof NettyDataBuffer ? ((NettyDataBuffer) buffer).getNativeBuffer() :  Unpooled.wrappedBuffer(buffer.asByteBuffer()));
		return (buffer instanceof FlushingDataBuffer ? new FlushingByteBuf(byteBuf) : byteBuf);
	}

	@Override
	protected void writeHeaders() {
		for (String name : getHeaders().keySet()) {
			for (String value : getHeaders().get(name))
				this.response.addHeader(name, value);
		}
	}

	@Override
	protected void writeCookies() {
		for (String name : getCookies().keySet()) {
			for (ResponseCookie httpCookie : getCookies().get(name)) {
				Cookie cookie = new DefaultCookie(name, httpCookie.getValue());
				if (!httpCookie.getMaxAge().isNegative()) {
					cookie.setMaxAge(httpCookie.getMaxAge().getSeconds());
				}
				httpCookie.getDomain().ifPresent(cookie::setDomain);
				httpCookie.getPath().ifPresent(cookie::setPath);
				cookie.setSecure(httpCookie.isSecure());
				cookie.setHttpOnly(httpCookie.isHttpOnly());
				this.response.addCookie(cookie);
			}
		}
	}

	private class FlushingByteBuf extends CompositeByteBuf {

		public FlushingByteBuf(ByteBuf byteBuf) {
			super(byteBuf.alloc(), byteBuf.isDirect(), 1);
			this.addComponent(true, byteBuf);
		}
	}

/*
	While the underlying implementation of {@link ZeroCopyHttpOutputMessage} seems to
	work; it does bypass {@link #applyBeforeCommit} and more importantly it doesn't change
	its {@linkplain #state()). Therefore it's commented out, for now.

	We should revisit this code once
	https://github.com/ReactiveX/RxNetty/issues/194 has been fixed.
	

	@Override
	public Mono<Void> writeWith(File file, long position, long count) {
		Channel channel = this.response.unsafeNettyChannel();

		HttpResponse httpResponse =
				new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		io.netty.handler.codec.http.HttpHeaders headers = httpResponse.headers();

		for (Map.Entry<String, List<String>> header : getHeaders().entrySet()) {
			String headerName = header.getKey();
			for (String headerValue : header.getValue()) {
				headers.add(headerName, headerValue);
			}
		}
		Mono<Void> responseWrite = MonoChannelFuture.from(channel.write(httpResponse));

		FileRegion fileRegion = new DefaultFileRegion(file, position, count);
		Mono<Void> fileWrite = MonoChannelFuture.from(channel.writeAndFlush(fileRegion));

		return Flux.concat(applyBeforeCommit(), responseWrite, fileWrite).then();
	}
*/
}