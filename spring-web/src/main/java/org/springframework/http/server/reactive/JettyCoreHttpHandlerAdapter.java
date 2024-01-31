/*
 * Copyright 2002-2023 the original author or authors.
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

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

/**
 * Adapt {@link HttpHandler} to the Jetty {@link org.eclipse.jetty.server.Handler} abstraction.
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @since 6.2
 */
public class JettyCoreHttpHandlerAdapter extends Handler.Abstract.NonBlocking {

	private final HttpHandler httpHandler;

	private final DataBufferFactory dataBufferFactory;

	public JettyCoreHttpHandlerAdapter(HttpHandler httpHandler) {
		this.httpHandler = httpHandler;

		// Currently we do not make a DataBufferFactory over the servers ByteBufferPool,
		// because we mainly use wrap and there should be few allocation done by the factory.
		// But it could be possible to use the servers buffer pool for allocations and to
		// create PooledDataBuffers
		this.dataBufferFactory = new DefaultDataBufferFactory();
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		this.httpHandler.handle(new JettyCoreServerHttpRequest(this.dataBufferFactory, request), new JettyCoreServerHttpResponse(response))
				.subscribe(new Subscriber<>() {
					@Override
					public void onSubscribe(Subscription s) {
						s.request(Long.MAX_VALUE);
					}

					@Override
					public void onNext(Void unused) {
						// we can ignore the void as we only seek onError or onComplete
					}

					@Override
					public void onError(Throwable t) {
						callback.failed(t);
					}

					@Override
					public void onComplete() {
						callback.succeeded();
					}
				});
		return true;
	}

}
