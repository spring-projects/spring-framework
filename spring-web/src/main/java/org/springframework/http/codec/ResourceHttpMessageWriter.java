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

package org.springframework.http.codec;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.ResourceEncoder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ZeroCopyHttpOutputMessage;

/**
 * Implementation of {@link HttpMessageWriter} that can write
 * {@link Resource Resources}.
 *
 * <p>For a Resource reader simply use {@link ResourceDecoder} wrapped with
 * {@link DecoderHttpMessageReader}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class ResourceHttpMessageWriter extends EncoderHttpMessageWriter<Resource> {


	public ResourceHttpMessageWriter() {
		super(new ResourceEncoder());
	}

	public ResourceHttpMessageWriter(int bufferSize) {
		super(new ResourceEncoder(bufferSize));
	}


	@Override
	public Mono<Void> write(Publisher<? extends Resource> inputStream, ResolvableType elementType,
			MediaType mediaType, ReactiveHttpOutputMessage outputMessage, Map<String, Object> hints) {

		return Mono.from(Flux.from(inputStream).
				take(1).
				concatMap(resource -> {
					HttpHeaders headers = outputMessage.getHeaders();
					addHeaders(headers, resource, mediaType);
					return writeContent(resource, elementType, outputMessage, hints);
				}));
	}

	protected void addHeaders(HttpHeaders headers, Resource resource, MediaType mediaType) {
		if (headers.getContentType() == null) {
			if (mediaType == null || !mediaType.isConcrete() ||
					MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
				mediaType = Optional.ofNullable(MediaTypeFactory.getMediaType(resource)).
						orElse(MediaType.APPLICATION_OCTET_STREAM);
			}
			headers.setContentType(mediaType);
		}
		if (headers.getContentLength() < 0) {
			contentLength(resource).ifPresent(headers::setContentLength);
		}
	}

	private Mono<Void> writeContent(Resource resource, ResolvableType type, ReactiveHttpOutputMessage outputMessage, Map<String, Object> hints) {
		if (outputMessage instanceof ZeroCopyHttpOutputMessage) {
			Optional<File> file = getFile(resource);
			if (file.isPresent()) {
				ZeroCopyHttpOutputMessage zeroCopyResponse =
						(ZeroCopyHttpOutputMessage) outputMessage;

				return zeroCopyResponse.writeWith(file.get(), 0, file.get().length());
			}
		}

		// non-zero copy fallback, using ResourceEncoder
		return super.write(Mono.just(resource), type,
				outputMessage.getHeaders().getContentType(), outputMessage, hints);
	}

	private static Optional<Long> contentLength(Resource resource) {
		// Don't try to determine contentLength on InputStreamResource - cannot be read afterwards...
		// Note: custom InputStreamResource subclasses could provide a pre-calculated content length!
		if (InputStreamResource.class != resource.getClass()) {
			try {
				return Optional.of(resource.contentLength());
			}
			catch (IOException ignored) {
			}
		}
		return Optional.empty();
	}

	private static Optional<File> getFile(Resource resource) {
		if (resource.isFile()) {
			try {
				return Optional.of(resource.getFile());
			}
			catch (IOException ex) {
				// should not happen
			}
		}
		return Optional.empty();
	}

}
