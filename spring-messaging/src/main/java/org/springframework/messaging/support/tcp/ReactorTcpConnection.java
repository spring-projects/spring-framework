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
	public ListenableFuture<Void> send(Message<P> message) {
		Promise<Void> promise = this.reactorTcpConnection.send(message);
		return new PassThroughPromiseToListenableFutureAdapter<Void>(promise);
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

}
