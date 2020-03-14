/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.client.reactive;

import org.apache.hc.core5.concurrent.FutureCallback;
import reactor.core.publisher.MonoSink;

/**
 * Transforms {@link FutureCallback} events to {@link reactor.core.publisher.Mono} events.
 *
 * @author Martin Tarjányi
 * @since 5.3
 * @param <T> the result type
 */
class MonoFutureCallbackAdapter<T> implements FutureCallback<T> {
	private final MonoSink<T> sink;

	public MonoFutureCallbackAdapter(MonoSink<T> sink) {
		this.sink = sink;
	}

	@Override
	public void completed(T result) {
		this.sink.success(result);
	}

	@Override
	public void failed(Exception ex) {
		this.sink.error(ex);
	}

	@Override
	public void cancelled() {
	}
}
