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

package org.springframework.messaging.tcp.reactor;

import reactor.core.composable.Promise;
import reactor.net.NetChannel;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * An implementation of {@link org.springframework.messaging.tcp.TcpConnection}
 * based on the TCP client support of the Reactor project.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <P> the payload type of Spring Message's read from and written to
 * the TCP stream
 */
public class Reactor11TcpConnection<P> implements TcpConnection<P> {

	private final NetChannel<Message<P>, Message<P>> channel;


	public Reactor11TcpConnection(NetChannel<Message<P>, Message<P>> connection) {
		this.channel = connection;
	}


	@Override
	public ListenableFuture<Void> send(Message<P> message) {
		Promise<Void> promise = this.channel.send(message);
		return new PassThroughPromiseToListenableFutureAdapter<Void>(promise);
	}

	@Override
	public void onReadInactivity(Runnable runnable, long inactivityDuration) {
		this.channel.on().readIdle(inactivityDuration, runnable);
	}

	@Override
	public void onWriteInactivity(Runnable runnable, long inactivityDuration) {
		this.channel.on().writeIdle(inactivityDuration, runnable);
	}

	@Override
	public void close() {
		this.channel.close();
	}

}
