/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.codec;

import java.util.Map;

import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Encoder for {@link ByteBuf ByteBufs}.
 *
 * @author Vladislav Kisel
 * @since 5.3
 */
public class NettyByteBufEncoder extends AbstractEncoder<ByteBuf> {

	public NettyByteBufEncoder() {
		super(MimeTypeUtils.ALL);
	}


	@Override
	public boolean canEncode(ResolvableType type, @Nullable MimeType mimeType) {
		Class<?> clazz = type.toClass();
		return super.canEncode(type, mimeType) && ByteBuf.class.isAssignableFrom(clazz);
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<? extends ByteBuf> inputStream,
			DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints) {

		return Flux.from(inputStream).map(byteBuffer ->
				encodeValue(byteBuffer, bufferFactory, elementType, mimeType, hints));
	}

	@Override
	public DataBuffer encodeValue(ByteBuf byteBuf, DataBufferFactory bufferFactory,
			ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
			String logPrefix = Hints.getLogPrefix(hints);
			logger.debug(logPrefix + "Writing " + byteBuf.readableBytes() + " bytes");
		}
		if (bufferFactory instanceof NettyDataBufferFactory nettyDataBufferFactory) {
			return nettyDataBufferFactory.wrap(byteBuf);
		}
		byte[] bytes = new byte[byteBuf.readableBytes()];
		byteBuf.readBytes(bytes);
		byteBuf.release();
		return bufferFactory.wrap(bytes);
	}
}
