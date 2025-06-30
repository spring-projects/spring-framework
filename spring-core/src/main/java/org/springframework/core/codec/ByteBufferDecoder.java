/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core.codec;

import java.nio.ByteBuffer;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Decoder for {@link ByteBuffer ByteBuffers}.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ByteBufferDecoder extends AbstractDataBufferDecoder<ByteBuffer> {

	public ByteBufferDecoder() {
		super(MimeTypeUtils.ALL);
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return (ByteBuffer.class.isAssignableFrom(elementType.toClass()) &&
				super.canDecode(elementType, mimeType));
	}

	@Override
	public ByteBuffer decode(DataBuffer dataBuffer, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		int len = dataBuffer.readableByteCount();
		ByteBuffer result = ByteBuffer.allocate(len);
		dataBuffer.toByteBuffer(result);
		if (logger.isDebugEnabled()) {
			logger.debug(Hints.getLogPrefix(hints) + "Read " + len + " bytes");
		}
		DataBufferUtils.release(dataBuffer);
		return result;
	}

}
