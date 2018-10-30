/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.util.concurrent;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Adapts a {@link Mono} into a {@link ListenableFuture}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 5.1
 * @param <T> the object type
 */
public class MonoToListenableFutureAdapter<T> implements ListenableFuture<T> {

	private final MonoProcessor<T> processor;

	private final ListenableFutureCallbackRegistry<T> registry = new ListenableFutureCallbackRegistry<>();


	public MonoToListenableFutureAdapter(Mono<T> mono) {
		Assert.notNull(mono, "Mono must not be null");
		this.processor = mono
				.doOnSuccess(this.registry::success)
				.doOnError(this.registry::failure)
				.toProcessor();
	}


	@Override
	@Nullable
	public T get() {
		return this.processor.block();
	}

	@Override
	@Nullable
	public T get(long timeout, TimeUnit unit) {
		Assert.notNull(unit, "TimeUnit must not be null");
		Duration duration = Duration.ofMillis(TimeUnit.MILLISECONDS.convert(timeout, unit));
		return this.processor.block(duration);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (isCancelled()) {
			return false;
		}
		this.processor.cancel();
		// isCancelled may still return false, if mono completed before the cancel
		return this.processor.isCancelled();
	}

	@Override
	public boolean isCancelled() {
		return this.processor.isCancelled();
	}

	@Override
	public boolean isDone() {
		return this.processor.isTerminated();
	}

	@Override
	public void addCallback(ListenableFutureCallback<? super T> callback) {
		this.registry.addCallback(callback);
	}

	@Override
	public void addCallback(SuccessCallback<? super T> success, FailureCallback failure) {
		this.registry.addSuccessCallback(success);
		this.registry.addFailureCallback(failure);
	}

}
