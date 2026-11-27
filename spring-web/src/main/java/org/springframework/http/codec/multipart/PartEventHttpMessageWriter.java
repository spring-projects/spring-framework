/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.multipart;

import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.Assert;

/**
 * {@link HttpMessageWriter} for writing {@link PartEvent} objects. Useful for
 * server-side proxies, that relay multipart requests to others services.
 *
 * @author Arjen Poutsma
 * @since 6.0
 * @see PartEvent
 */
public class PartEventHttpMessageWriter extends MultipartWriterSupport implements HttpMessageWriter<PartEvent> {

	public PartEventHttpMessageWriter() {
		super(Collections.singletonList(MediaType.MULTIPART_FORM_DATA));
	}

	@Override
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		if (PartEvent.class.isAssignableFrom(elementType.toClass())) {
			if (mediaType == null) {
				return true;
			}
			for (MediaType supportedMediaType : getWritableMediaTypes()) {
				if (supportedMediaType.isCompatibleWith(mediaType)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Mono<Void> write(Publisher<? extends PartEvent> partDataStream, ResolvableType elementType,
			@Nullable MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
			Map<String, Object> hints) {

		byte[] boundary = generateMultipartBoundary();

		mediaType = getMultipartMediaType(mediaType, boundary);
		outputMessage.getHeaders().setContentType(mediaType);

		if (logger.isDebugEnabled()) {
			logger.debug(Hints.getLogPrefix(hints) + "Encoding Publisher<PartEvent>");
		}

		Flux<Publisher<DataBuffer>> parts = Flux.from(partDataStream)
				.windowUntil(PartEvent::isLast)
				.map(partData -> partData.switchOnFirst((signal, flux) -> {
					if (signal.hasValue()) {
						PartEvent value = signal.get();
						Assert.state(value != null, "Null value");
						Flux<DataBuffer> dataBuffers = flux.map(PartEvent::content)
								.filter(buffer -> buffer.readableByteCount() > 0);
						return generatePartHeaders(value.headers(), outputMessage.bufferFactory())
								.concatWith(dataBuffers);
					}
					else {
						return flux.cast(DataBuffer.class);
					}
				}));

		return writeMultipart(parts, boundary, outputMessage, hints);
	}

}
