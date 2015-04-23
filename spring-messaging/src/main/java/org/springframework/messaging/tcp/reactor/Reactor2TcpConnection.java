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

import org.reactivestreams.Publisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.util.concurrent.ListenableFuture;
import reactor.fn.Consumer;
import reactor.fn.Function;
import reactor.fn.Functions;
import reactor.io.net.ChannelStream;
import reactor.rx.Promise;
import reactor.rx.Promises;
import reactor.rx.Stream;
import reactor.rx.broadcast.Broadcaster;

import java.lang.reflect.Constructor;

/**
 * An implementation of {@link org.springframework.messaging.tcp.TcpConnection}
 * based on the TCP client support of the Reactor project.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <P> the payload type of Spring Message's read from and written to
 * the TCP stream
 */
public class Reactor2TcpConnection<P> implements TcpConnection<P> {

	private final ChannelStream<Message<P>, Message<P>> channel;
	private final Broadcaster<Message<P>>               sink;


	public Reactor2TcpConnection(ChannelStream<Message<P>, Message<P>> connection) {
		this.channel = connection;
		this.sink = Broadcaster.create();

		channel.sink(sink);
	}


	@Override
	public ListenableFuture<Void> send(Message<P> message) {
		sink.onNext(message);
		//FIXME need to align Reactor with Reactive IPC to have publish/confirm receipt
		return new PassThroughPromiseToListenableFutureAdapter<Void>(Promises.<Void>success(null));
	}

	@Override
	public void onReadInactivity(Runnable runnable, long inactivityDuration) {
		this.channel.on().readIdle(inactivityDuration, Functions.<Void>consumer(runnable));
	}

	@Override
	public void onWriteInactivity(Runnable runnable, long inactivityDuration) {
		this.channel.on().writeIdle(inactivityDuration, Functions.<Void>consumer(runnable));
	}

	@Override
	public void close() {
		sink.onComplete();
	}

}
