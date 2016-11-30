/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.messaging.simp.stomp;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import reactor.core.scheduler.Schedulers;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * A STOMP over TCP client that uses
 * {@link ReactorNettyTcpClient}.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class ReactorNettyTcpStompClient extends StompClientSupport {

	private final TcpOperations<byte[]> tcpClient;

	/**
	 * Create an instance with host "127.0.0.1" and port 61613.
	 */
	public ReactorNettyTcpStompClient() {
		this("127.0.0.1", 61613);
	}


	/**
	 * Create an instance with the given host and port.
	 * @param host the host
	 * @param port the port
	 */
	public ReactorNettyTcpStompClient(final String host, final int port) {
		this.tcpClient = create(host, port, new StompDecoder());
	}

	/**
	 * Create an instance with a pre-configured TCP client.
	 * @param tcpClient the client to use
	 */
	public ReactorNettyTcpStompClient(TcpOperations<byte[]> tcpClient) {
		this.tcpClient = tcpClient;
	}

	/**
	 * Connect and notify the given {@link StompSessionHandler} when connected
	 * on the STOMP level.
	 * @param handler the handler for the STOMP session
	 * @return ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(StompSessionHandler handler) {
		return connect(null, handler);
	}


	/**
	 * An overloaded version of {@link #connect(StompSessionHandler)} that
	 * accepts headers to use for the STOMP CONNECT frame.
	 * @param connectHeaders headers to add to the CONNECT frame
	 * @param handler the handler for the STOMP session
	 * @return ListenableFuture for access to the session when ready for use
	 */
	public ListenableFuture<StompSession> connect(StompHeaders connectHeaders, StompSessionHandler handler) {
		ConnectionHandlingStompSession session = createSession(connectHeaders, handler);
		this.tcpClient.connect(session);
		return session.getSessionFuture();
	}

	/**
	 * Shut down the client and release resources.
	 */
	public void shutdown() {
		this.tcpClient.shutdown();
	}

	/**
	 * Create a new {@link ReactorNettyTcpClient} with Stomp specific configuration for
	 * encoding, decoding and hand-off.
	 *
	 * @param relayHost target host
	 * @param relayPort target port
	 * @param decoder {@link StompDecoder} to use
	 * @return a new {@link TcpOperations}
	 */
	protected static TcpOperations<byte[]> create(String relayHost,
			int relayPort,
			StompDecoder decoder) {
		return new ReactorNettyTcpClient<>(relayHost,
				relayPort,
				new ReactorNettyTcpClient.MessageHandlerConfiguration<>(new DecodingFunction(
						decoder),
						new EncodingConsumer(new StompEncoder()),
						128,
						Schedulers.newParallel("StompClient")));
	}

	private static final class EncodingConsumer
			implements BiConsumer<ByteBuf, Message<byte[]>> {

		private final StompEncoder encoder;

		public EncodingConsumer(StompEncoder encoder) {
			this.encoder = encoder;
		}

		@Override
		public void accept(ByteBuf byteBuf, Message<byte[]> message) {
			byteBuf.writeBytes(encoder.encode(message));
		}
	}

	private static final class DecodingFunction
			implements Function<ByteBuf, List<Message<byte[]>>> {

		private final StompDecoder decoder;

		public DecodingFunction(StompDecoder decoder) {
			this.decoder = decoder;
		}

		@Override
		public List<Message<byte[]>> apply(ByteBuf buffer) {
			return this.decoder.decode(buffer.nioBuffer());
		}
	}
}
