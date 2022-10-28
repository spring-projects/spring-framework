/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.socket.adapter;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import jakarta.websocket.Session;
import org.apache.tomcat.websocket.WsSession;
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Spring {@link WebSocketSession} adapter for Tomcat's
 * {@link jakarta.websocket.Session}.
 *
 * @author Violeta Georgieva
 * @since 5.0
 */
public class TomcatWebSocketSession extends StandardWebSocketSession {

	private static final AtomicIntegerFieldUpdater<TomcatWebSocketSession> SUSPENDED =
			AtomicIntegerFieldUpdater.newUpdater(TomcatWebSocketSession.class, "suspended");

	@SuppressWarnings("unused")
	private volatile int suspended;


	public TomcatWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory) {
		super(session, info, factory);
	}

	public TomcatWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
			Sinks.Empty<Void> completionSink) {

		super(session, info, factory, completionSink);
		suspendReceiving();
	}


	@Override
	protected boolean canSuspendReceiving() {
		return true;
	}

	@Override
	protected void suspendReceiving() {
		if (SUSPENDED.compareAndSet(this, 0, 1)) {
			((WsSession) getDelegate()).suspend();
		}
	}

	@Override
	protected void resumeReceiving() {
		if (SUSPENDED.compareAndSet(this, 1, 0)) {
			((WsSession) getDelegate()).resume();
		}
	}

}
