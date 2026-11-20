/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.codec;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.MimeType;

/**
 * Abstract base class for {@link org.springframework.core.codec.Encoder}
 * classes that can only deal with a single value.
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @param <T> the element type
 */
public abstract class AbstractSingleValueEncoder<T> extends AbstractEncoder<T> {


	public AbstractSingleValueEncoder(MimeType... supportedMimeTypes) {
		super(supportedMimeTypes);
	}


	@Override
	public final Flux<DataBuffer> encode(Publisher<? extends T> inputStream, DataBufferFactory bufferFactory,
			ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(inputStream)
				.take(1)
				.concatMap(value -> encode(value, bufferFactory, elementType, mimeType, hints))
				.doOnDiscard(DataBuffer.class, DataBufferUtils::release);
	}

	/**
	 * Encode {@code T} to an output {@link DataBuffer} stream.
	 * @param t the value to process
	 * @param dataBufferFactory a buffer factory used to create the output
	 * @param type the stream element type to process
	 * @param mimeType the mime type to process
	 * @param hints additional information about how to do decode, optional
	 * @return the output stream
	 */
	protected abstract Flux<DataBuffer> encode(T t, DataBufferFactory dataBufferFactory,
			ResolvableType type, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints);

}
