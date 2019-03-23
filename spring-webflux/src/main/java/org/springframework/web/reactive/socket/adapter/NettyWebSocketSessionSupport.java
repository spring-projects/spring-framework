/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Base class for Netty-based {@link WebSocketSession} adapters that provides
 * convenience methods to convert Netty {@link WebSocketFrame}s to and from
 * {@link WebSocketMessage}s.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class NettyWebSocketSessionSupport<T> extends AbstractWebSocketSession<T> {

	/**
	 * The default max size for aggregating inbound WebSocket frames.
	 */
	protected static final int DEFAULT_FRAME_MAX_SIZE = 64 * 1024;


	private static final Map<Class<?>, WebSocketMessage.Type> messageTypes;

	static {
		messageTypes = new HashMap<>(8);
		messageTypes.put(TextWebSocketFrame.class, WebSocketMessage.Type.TEXT);
		messageTypes.put(BinaryWebSocketFrame.class, WebSocketMessage.Type.BINARY);
		messageTypes.put(PingWebSocketFrame.class, WebSocketMessage.Type.PING);
		messageTypes.put(PongWebSocketFrame.class, WebSocketMessage.Type.PONG);
	}


	protected NettyWebSocketSessionSupport(T delegate, HandshakeInfo info, NettyDataBufferFactory factory) {
		super(delegate, ObjectUtils.getIdentityHexString(delegate), info, factory);
	}


	@Override
	public NettyDataBufferFactory bufferFactory() {
		return (NettyDataBufferFactory) super.bufferFactory();
	}


	protected WebSocketMessage toMessage(WebSocketFrame frame) {
		DataBuffer payload = bufferFactory().wrap(frame.content());
		return new WebSocketMessage(messageTypes.get(frame.getClass()), payload);
	}

	protected WebSocketFrame toFrame(WebSocketMessage message) {
		ByteBuf byteBuf = NettyDataBufferFactory.toByteBuf(message.getPayload());
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			return new TextWebSocketFrame(byteBuf);
		}
		else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
			return new BinaryWebSocketFrame(byteBuf);
		}
		else if (WebSocketMessage.Type.PING.equals(message.getType())) {
			return new PingWebSocketFrame(byteBuf);
		}
		else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
			return new PongWebSocketFrame(byteBuf);
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message.getType());
		}
	}

}
