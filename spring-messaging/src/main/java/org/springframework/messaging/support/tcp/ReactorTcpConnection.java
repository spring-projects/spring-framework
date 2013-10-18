/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.support.tcp;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.ListenableFutureCallbackRegistry;

import reactor.core.composable.Deferred;
import reactor.core.composable.Promise;
import reactor.core.composable.spec.DeferredPromiseSpec;
import reactor.function.Consumer;


public class ReactorTcpConnection<P> implements TcpConnection<P> {

	private final reactor.tcp.TcpConnection<Message<P>, Message<P>> reactorTcpConnection;


	public ReactorTcpConnection(reactor.tcp.TcpConnection<Message<P>, Message<P>> connection) {
		this.reactorTcpConnection = connection;
	}

	@Override
	public ListenableFuture<Boolean> send(Message<P> message) {
		ConsumerListenableFuture future = new ConsumerListenableFuture();
		this.reactorTcpConnection.send(message, future);
		return future;
	}

	@Override
	public void onReadInactivity(Runnable runnable, long inactivityDuration) {
		this.reactorTcpConnection.on().readIdle(inactivityDuration, runnable);
	}

	@Override
	public void onWriteInactivity(Runnable runnable, long inactivityDuration) {
		this.reactorTcpConnection.on().writeIdle(inactivityDuration, runnable);
	}

	@Override
	public void close() {
		this.reactorTcpConnection.close();
	}


	// Use this temporarily until reactor provides a send method returning a Promise


	private static class ConsumerListenableFuture implements ListenableFuture<Boolean>, Consumer<Boolean> {

		final Deferred<Boolean, Promise<Boolean>> deferred = new DeferredPromiseSpec<Boolean>().get();

		private final ListenableFutureCallbackRegistry<Boolean> registry =
				new ListenableFutureCallbackRegistry<Boolean>();

		@Override
		public void accept(Boolean result) {

			this.deferred.accept(result);

			if (result == null) {
				this.registry.failure(new TimeoutException());
			}
			else if (result) {
				this.registry.success(result);
			}
			else {
				this.registry.failure(new Exception("Failed send message"));
			}
		}

		@Override
		public Boolean get() {
			try {
				return this.deferred.compose().await();
			}
			catch (InterruptedException e) {
				return Boolean.FALSE;
			}
		}

		@Override
		public Boolean get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {

			Boolean result = this.deferred.compose().await(timeout, unit);
			if (result == null) {
				throw new TimeoutException();
			}
			return result;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return this.deferred.compose().isComplete();
		}

		@Override
		public void addCallback(ListenableFutureCallback<? super Boolean> callback) {
			this.registry.addCallback(callback);
		}
	}

}
