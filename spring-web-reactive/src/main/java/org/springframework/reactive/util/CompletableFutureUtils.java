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

package org.springframework.reactive.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.support.Exceptions;
import reactor.rx.Stream;
import reactor.rx.action.Action;
import reactor.rx.subscription.ReactiveSubscription;

import org.springframework.util.Assert;

/**
 * @author Sebastien Deleuze
 */
public class CompletableFutureUtils {

	public static <T> Publisher<T> toPublisher(CompletableFuture<T> future) {
		return new CompletableFutureStream<T>(future);
	}

	public static <T> CompletableFuture<List<T>> fromPublisher(Publisher<T> publisher) {
		final CompletableFuture<List<T>> future = new CompletableFuture<>();
		publisher.subscribe(new Subscriber<T>() {
			private final List<T> values = new ArrayList<>();

			@Override
			public void onSubscribe(Subscription s) {
				s.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(T t) {
				values.add(t);
			}

			@Override
			public void onError(Throwable t) {
				future.completeExceptionally(t);
			}

			@Override
			public void onComplete() {
				future.complete(values);
			}
		});
		return future;
	}

	public static <T> CompletableFuture<T> fromSinglePublisher(Publisher<T> publisher) {
		final CompletableFuture<T> future = new CompletableFuture<>();
		publisher.subscribe(new Subscriber<T>() {
			private T value;

			@Override
			public void onSubscribe(Subscription s) {
				s.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(T t) {
				Assert.state(value == null, "This publisher should not publish multiple values");
				value = t;
			}

			@Override
			public void onError(Throwable t) {
				future.completeExceptionally(t);
			}

			@Override
			public void onComplete() {
				future.complete(value);
			}
		});
		return future;
	}

	private static class CompletableFutureStream<T> extends Stream<T> {

		private final CompletableFuture<? extends T> future;

		public CompletableFutureStream(CompletableFuture<? extends T> future) {
			this.future = future;
		}

		@Override
		public void subscribe(final Subscriber<? super T> subscriber) {
			try {
				subscriber.onSubscribe(new ReactiveSubscription<T>(this, subscriber) {

					@Override
					public void request(long elements) {
						Action.checkRequest(elements);
						if (isComplete()) return;

						try {
							future.whenComplete((result, error) -> {
								if (error != null) {
									onError(error);
								}
								else {
									subscriber.onNext(result);
									onComplete();
								}
							});

						} catch (Throwable e) {
							onError(e);
						}
					}
				});
			} catch (Throwable throwable) {
				Exceptions.throwIfFatal(throwable);
				subscriber.onError(throwable);
			}
		}

	}

}