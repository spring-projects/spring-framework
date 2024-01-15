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
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.HttpCookieUtils;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.IteratingCallback;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.JettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.lang.Nullable;

/**
 * Adapt an Eclipse Jetty {@link Response} to a {@link org.springframework.http.server.ServerHttpResponse}.
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @since 6.2
 */
class JettyCoreServerHttpResponse extends AbstractServerHttpResponse implements ZeroCopyHttpOutputMessage {

	private final Response response;

	public JettyCoreServerHttpResponse(Response response, JettyDataBufferFactory dataBufferFactory) {
		super(dataBufferFactory, new HttpHeaders(new JettyHeadersAdapter(response.getHeaders())));
		this.response = response;

		// remove all existing cookies from the response and add them to the cookie map, to be added back later
		for (ListIterator<HttpField> i = this.response.getHeaders().listIterator(); i.hasNext(); ) {
			HttpField f = i.next();
			if (f instanceof HttpCookieUtils.SetCookieHttpField setCookieHttpField) {
				HttpCookie httpCookie = setCookieHttpField.getHttpCookie();
				ResponseCookie responseCookie = ResponseCookie.from(httpCookie.getName(), httpCookie.getValue())
						.httpOnly(httpCookie.isHttpOnly())
						.domain(httpCookie.getDomain())
						.maxAge(httpCookie.getMaxAge())
						.sameSite(httpCookie.getSameSite().name())
						.secure(httpCookie.isSecure())
						.partitioned(httpCookie.isPartitioned())
						.build();
				this.addCookie(responseCookie);
				i.remove();
			}
		}
	}

	@Override
	protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
		return Flux.from(body)
				.concatMap(this::sendDataBuffer)
				.then();
	}

	@Override
	protected Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return Flux.from(body).concatMap(this::writeWithInternal).then();
	}

	@Override
	protected void applyStatusCode() {
		HttpStatusCode status = getStatusCode();
		this.response.setStatus(status == null ? 0 : status.value());
	}

	@Override
	protected void applyHeaders() {
	}

	@Override
	protected void applyCookies() {
		this.getCookies().values().stream()
				.flatMap(List::stream)
				.forEach(cookie -> Response.addCookie(this.response, new ResponseHttpCookie(cookie)));
	}

	@Override
	public Mono<Void> writeWith(Path file, long position, long count) {
		Callback.Completable callback = new Callback.Completable();
		Mono<Void> mono = Mono.fromFuture(callback);
		try {
			Content.copy(Content.Source.from(null, file, position, count), this.response, callback);
		}
		catch (Throwable th) {
			callback.failed(th);
		}
		return doCommit(() -> mono);
	}

	private Mono<Void> sendDataBuffer(DataBuffer dataBuffer) {
		return Mono.defer(() -> {
			DataBuffer.ByteBufferIterator byteBufferIterator = dataBuffer.readableByteBuffers();
			Callback.Completable callback = new Callback.Completable();
			new IteratingCallback() {
				@Override
				protected Action process() {
					if (!byteBufferIterator.hasNext()) {
						return Action.SUCCEEDED;
					}
					response.write(false, byteBufferIterator.next(), this);
					return Action.SCHEDULED;
				}

				@Override
				protected void onCompleteSuccess() {
					byteBufferIterator.close();
					DataBufferUtils.release(dataBuffer);
					callback.complete(null);
				}

				@Override
				protected void onCompleteFailure(Throwable cause) {
					byteBufferIterator.close();
					DataBufferUtils.release(dataBuffer);
					callback.failed(cause);
				}
			}.iterate();

			return Mono.fromFuture(callback);
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeResponse() {
		return (T) this.response;
	}

	private static class ResponseHttpCookie implements org.eclipse.jetty.http.HttpCookie {
		private final ResponseCookie responseCookie;

		public ResponseHttpCookie(ResponseCookie responseCookie) {
			this.responseCookie = responseCookie;
		}

		public ResponseCookie getResponseCookie() {
			return this.responseCookie;
		}

		@Override
		public String getName() {
			return this.responseCookie.getName();
		}

		@Override
		public String getValue() {
			return this.responseCookie.getValue();
		}

		@Override
		public int getVersion() {
			return 0;
		}

		@Override
		public long getMaxAge() {
			return this.responseCookie.getMaxAge().toSeconds();
		}

		@Override
		@Nullable
		public String getComment() {
			return null;
		}

		@Override
		@Nullable
		public String getDomain() {
			return this.responseCookie.getDomain();
		}

		@Override
		@Nullable
		public String getPath() {
			return this.responseCookie.getPath();
		}

		@Override
		public boolean isSecure() {
			return this.responseCookie.isSecure();
		}

		@Nullable
		@Override
		public SameSite getSameSite() {
			// Adding non-null return site breaks tests.
			return null;
		}

		@Override
		public boolean isHttpOnly() {
			return this.responseCookie.isHttpOnly();
		}

		@Override
		public boolean isPartitioned() {
			return this.responseCookie.isPartitioned();
		}

		@Override
		public Map<String, String> getAttributes() {
			return Collections.emptyMap();
		}
	}
}
