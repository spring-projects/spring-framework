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

package org.springframework.http.codec.multipart;

import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.lang.Nullable;

/**
 * {@link HttpMessageWriter} for writing with {@link Part}. This can be useful
 * on the server side to write a {@code Flux<Part>} received from a client to
 * some remote service.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class PartHttpMessageWriter extends MultipartWriterSupport implements HttpMessageWriter<Part> {


	public PartHttpMessageWriter() {
		super(MultipartHttpMessageReader.MIME_TYPES);
	}


	@Override
	public Mono<Void> write(Publisher<? extends Part> parts,
			ResolvableType elementType, @Nullable MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
			Map<String, Object> hints) {

		byte[] boundary = generateMultipartBoundary();

		mediaType = getMultipartMediaType(mediaType, boundary);
		outputMessage.getHeaders().setContentType(mediaType);

		if (logger.isDebugEnabled()) {
			logger.debug(Hints.getLogPrefix(hints) + "Encoding Publisher<Part>");
		}

		Flux<DataBuffer> body = Flux.from(parts)
				.concatMap(part -> encodePart(boundary, part, outputMessage.bufferFactory()))
				.concatWith(generateLastLine(boundary, outputMessage.bufferFactory()))
				.doOnDiscard(PooledDataBuffer.class, PooledDataBuffer::release);

		return outputMessage.writeWith(body);
	}

	private <T> Flux<DataBuffer> encodePart(byte[] boundary, Part part, DataBufferFactory bufferFactory) {
		HttpHeaders headers = new HttpHeaders(part.headers());

		String name = part.name();
		if (!headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
			headers.setContentDispositionFormData(name,
					(part instanceof FilePart ? ((FilePart) part).filename() : null));
		}

		return Flux.concat(
				generateBoundaryLine(boundary, bufferFactory),
				generatePartHeaders(headers, bufferFactory),
				part.content(),
				generateNewLine(bufferFactory));
	}

}
