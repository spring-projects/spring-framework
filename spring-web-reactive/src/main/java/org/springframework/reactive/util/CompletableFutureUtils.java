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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.util.Assert;
import reactor.Publishers;
import reactor.core.error.CancelException;
import reactor.core.error.Exceptions;
import reactor.core.support.BackpressureUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * @author Sebastien Deleuze
 * @author Stephane Maldini
 */
public class CompletableFutureUtils {

	public static <T> Publisher<T> toPublisher(CompletableFuture<T> future) {
		return new CompletableFuturePublisher<T>(future);
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

	private static class CompletableFuturePublisher<T> implements Publisher<T> {

		private final CompletableFuture<? extends T> future;
		private final Publisher<? extends T> futurePublisher;

		@SuppressWarnings("unused")
		private volatile long requested;
		private static final AtomicLongFieldUpdater<CompletableFuturePublisher> REQUESTED =
		  AtomicLongFieldUpdater.newUpdater(CompletableFuturePublisher.class, "requested");

		public CompletableFuturePublisher(CompletableFuture<? extends T> future) {
			this.future = future;
			this.futurePublisher = Publishers.createWithDemand((n, sub) -> {

				if (!BackpressureUtils.checkRequest(n, sub)) {
					return;
				}

				if(BackpressureUtils.getAndAdd(REQUESTED, CompletableFuturePublisher.this, n) > 0) {
					return;
				}

				future.whenComplete((result, error) -> {
					if (error != null) {
						sub.onError(error);
					} else {
						sub.onNext(result);
						sub.onComplete();
					}
				});
			}, null, nothing -> {
			  if(!future.isDone()){
				  future.cancel(true);
			  }
			});
		}

		@Override
		public void subscribe(final Subscriber<? super T> subscriber) {
			try {
				if (future.isDone()) {
					Publishers.just(future.get()).subscribe(subscriber);
				}
				else if ( future.isCancelled()){
					Exceptions.publisher(CancelException.get());
				}
				else {
					futurePublisher.subscribe(subscriber);
				}
			}
			catch (Throwable throwable) {
				Exceptions.publisher(throwable);
			}
		}
	}

}