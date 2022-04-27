/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Decoder for {@link Resource Resources}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ResourceDecoder extends AbstractDataBufferDecoder<Resource> {

	/** Name of hint with a filename for the resource(e.g. from "Content-Disposition" HTTP header). */
	public static String FILENAME_HINT = ResourceDecoder.class.getName() + ".filename";


	public ResourceDecoder() {
		super(MimeTypeUtils.ALL);
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return (Resource.class.isAssignableFrom(elementType.toClass()) &&
				super.canDecode(elementType, mimeType));
	}

	@Override
	public Flux<Resource> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(decodeToMono(inputStream, elementType, mimeType, hints));
	}

	@Override
	public Resource decode(DataBuffer dataBuffer, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		byte[] bytes = new byte[dataBuffer.readableByteCount()];
		dataBuffer.read(bytes);
		DataBufferUtils.release(dataBuffer);

		if (logger.isDebugEnabled()) {
			logger.debug(Hints.getLogPrefix(hints) + "Read " + bytes.length + " bytes");
		}

		Class<?> clazz = elementType.toClass();
		String filename = hints != null ? (String) hints.get(FILENAME_HINT) : null;
		if (clazz == InputStreamResource.class) {
			return new InputStreamResource(new ByteArrayInputStream(bytes)) {
				@Override
				public String getFilename() {
					return filename;
				}
				@Override
				public long contentLength() {
					return bytes.length;
				}
			};
		}
		else if (Resource.class.isAssignableFrom(clazz)) {
			return new ByteArrayResource(bytes) {
				@Override
				public String getFilename() {
					return filename;
				}
			};
		}
		else {
			throw new IllegalStateException("Unsupported resource class: " + clazz);
		}
	}

}
