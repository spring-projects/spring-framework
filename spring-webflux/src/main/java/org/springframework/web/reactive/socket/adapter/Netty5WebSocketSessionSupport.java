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

import java.util.HashMap;
import java.util.Map;

import io.netty5.buffer.Buffer;
import io.netty5.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty5.handler.codec.http.websocketx.WebSocketFrame;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.Netty5DataBufferFactory;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Base class for Netty-based {@link WebSocketSession} adapters that provides
 * convenience methods to convert Netty {@link WebSocketFrame WebSocketFrames} to and from
 * {@link WebSocketMessage WebSocketMessages}.
 *
 * <p>This class is based on {@link NettyWebSocketSessionSupport}.
 *
 * @author Violeta Georgieva
 * @since 6.0
 * @param <T> the native delegate type
 */
public abstract class Netty5WebSocketSessionSupport<T> extends AbstractWebSocketSession<T> {

	/**
	 * The default max size for inbound WebSocket frames.
	 */
	public static final int DEFAULT_FRAME_MAX_SIZE = 64 * 1024;


	private static final Map<Class<?>, WebSocketMessage.Type> messageTypes;

	static {
		messageTypes = new HashMap<>(8);
		messageTypes.put(TextWebSocketFrame.class, WebSocketMessage.Type.TEXT);
		messageTypes.put(BinaryWebSocketFrame.class, WebSocketMessage.Type.BINARY);
		messageTypes.put(PingWebSocketFrame.class, WebSocketMessage.Type.PING);
		messageTypes.put(PongWebSocketFrame.class, WebSocketMessage.Type.PONG);
	}


	protected Netty5WebSocketSessionSupport(T delegate, HandshakeInfo info, Netty5DataBufferFactory factory) {
		super(delegate, ObjectUtils.getIdentityHexString(delegate), info, factory);
	}


	@Override
	public Netty5DataBufferFactory bufferFactory() {
		return (Netty5DataBufferFactory) super.bufferFactory();
	}


	protected WebSocketMessage toMessage(WebSocketFrame frame) {
		DataBuffer payload = bufferFactory().wrap(frame.binaryData());
		WebSocketMessage.Type messageType = messageTypes.get(frame.getClass());
		Assert.state(messageType != null, "Unexpected message type");
		return new WebSocketMessage(messageType, payload, frame);
	}

	protected WebSocketFrame toFrame(WebSocketMessage message) {
		if (message.getNativeMessage() != null) {
			return message.getNativeMessage();
		}
		Buffer buffer = Netty5DataBufferFactory.toBuffer(message.getPayload());
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			return new TextWebSocketFrame(buffer);
		}
		else if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
			return new BinaryWebSocketFrame(buffer);
		}
		else if (WebSocketMessage.Type.PING.equals(message.getType())) {
			return new PingWebSocketFrame(buffer);
		}
		else if (WebSocketMessage.Type.PONG.equals(message.getType())) {
			return new PongWebSocketFrame(buffer);
		}
		else {
			throw new IllegalArgumentException("Unexpected message type: " + message.getType());
		}
	}

}
