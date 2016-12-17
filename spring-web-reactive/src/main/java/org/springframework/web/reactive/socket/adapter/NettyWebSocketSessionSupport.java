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
package org.springframework.web.reactive.socket.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Base class for Netty-based {@link WebSocketSession} adapters.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class NettyWebSocketSessionSupport<T> extends WebSocketSessionSupport<T> {

	private static final Map<Class<?>, WebSocketMessage.Type> MESSAGE_TYPES;

	static {
		MESSAGE_TYPES = new HashMap<>(4);
		MESSAGE_TYPES.put(TextWebSocketFrame.class, WebSocketMessage.Type.TEXT);
		MESSAGE_TYPES.put(BinaryWebSocketFrame.class, WebSocketMessage.Type.BINARY);
		MESSAGE_TYPES.put(PingWebSocketFrame.class, WebSocketMessage.Type.PING);
		MESSAGE_TYPES.put(PongWebSocketFrame.class, WebSocketMessage.Type.PONG);
	}


	protected NettyWebSocketSessionSupport(T delegate, HandshakeInfo info, NettyDataBufferFactory factory) {
		super(delegate, ObjectUtils.getIdentityHexString(delegate), info, factory);
	}


	@Override
	public NettyDataBufferFactory bufferFactory() {
		return (NettyDataBufferFactory) super.bufferFactory();
	}


	protected Flux<WebSocketMessage> toMessageFlux(Flux<WebSocketFrame> frameFlux) {
		return frameFlux
				.filter(frame -> !(frame instanceof CloseWebSocketFrame))
				.window()
				.concatMap(flux -> flux.takeUntil(WebSocketFrame::isFinalFragment).buffer())
				.map(this::toMessage);
	}

	@SuppressWarnings("OptionalGetWithoutIsPresent")
	private WebSocketMessage toMessage(List<WebSocketFrame> frames) {
		Class<?> frameType = frames.get(0).getClass();
		if (frames.size() == 1) {
			NettyDataBuffer buffer = bufferFactory().wrap(frames.get(0).content());
			return new WebSocketMessage(MESSAGE_TYPES.get(frameType), buffer);
		}
		return frames.stream()
				.map(socketFrame -> bufferFactory().wrap(socketFrame.content()))
				.reduce(NettyDataBuffer::write)
				.map(buffer -> new WebSocketMessage(MESSAGE_TYPES.get(frameType), buffer))
				.get();
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
