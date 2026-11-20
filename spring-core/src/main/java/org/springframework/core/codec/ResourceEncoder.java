/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.codec;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

/**
 * Encoder for {@link Resource Resources}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ResourceEncoder extends AbstractSingleValueEncoder<Resource> {

	/**
	 * The default buffer size used by the encoder.
	 */
	public static final int DEFAULT_BUFFER_SIZE = StreamUtils.BUFFER_SIZE;

	private final int bufferSize;


	public ResourceEncoder() {
		this(DEFAULT_BUFFER_SIZE);
	}

	public ResourceEncoder(int bufferSize) {
		super(MimeTypeUtils.APPLICATION_OCTET_STREAM, MimeTypeUtils.ALL);
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be larger than 0");
		this.bufferSize = bufferSize;
	}


	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		Class<?> clazz = elementType.toClass();
		return (super.canEncode(elementType, mimeType) && Resource.class.isAssignableFrom(clazz));
	}

	@Override
	protected Flux<DataBuffer> encode(Resource resource, DataBufferFactory bufferFactory,
			ResolvableType type, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
			String logPrefix = Hints.getLogPrefix(hints);
			logger.debug(logPrefix + "Writing [" + resource + "]");
		}
		return DataBufferUtils.read(resource, bufferFactory, this.bufferSize);
	}

}
