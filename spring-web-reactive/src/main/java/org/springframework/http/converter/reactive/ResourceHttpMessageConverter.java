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

package org.springframework.http.converter.reactive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.support.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRangeResource;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.support.MediaTypeUtils;
import org.springframework.util.MimeTypeUtils2;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;

/**
 * @author Arjen Poutsma
 */
public class ResourceHttpMessageConverter implements HttpMessageConverter<Resource> {

	private static final int BUFFER_SIZE = StreamUtils.BUFFER_SIZE;

	private static final List<MediaType> SUPPORTED_MEDIA_TYPES =
			Collections.singletonList(MediaType.ALL);

	@Override
	public boolean canRead(ResolvableType type, MediaType mediaType) {
		return Resource.class.isAssignableFrom(type.getRawClass());
	}

	@Override
	public boolean canWrite(ResolvableType type, MediaType mediaType) {
		return Resource.class.isAssignableFrom(type.getRawClass());
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return SUPPORTED_MEDIA_TYPES;
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return SUPPORTED_MEDIA_TYPES;
	}

	@Override
	public Flux<Resource> read(ResolvableType type,
			ReactiveHttpInputMessage inputMessage) {
		Class<?> clazz = type.getRawClass();

		Flux<DataBuffer> body = inputMessage.getBody();

		if (InputStreamResource.class.equals(clazz)) {
			InputStream is = DataBufferUtils.toInputStream(body);
			return Flux.just(new InputStreamResource(is));
		}
		else if (clazz.isAssignableFrom(ByteArrayResource.class)) {
			Mono<DataBuffer> singleBuffer = body.reduce(DataBuffer::write);
			return Flux.from(singleBuffer.map(buffer -> {
				byte[] bytes = new byte[buffer.readableByteCount()];
				buffer.read(bytes);
				return new ByteArrayResource(bytes);
			}));
		}
		else {
			return Flux.error(new IllegalStateException(
					"Unsupported resource class: " + clazz));
		}
	}

	@Override
	public Mono<Void> write(Publisher<? extends Resource> inputStream,
			ResolvableType type, MediaType contentType,
			ReactiveHttpOutputMessage outputMessage) {

		if (inputStream instanceof Mono) {
			// single resource
			return Mono.from(Flux.from(inputStream).
					flatMap(resource -> {
						HttpHeaders headers = outputMessage.getHeaders();
						addHeaders(headers, resource, contentType);

						if (resource instanceof HttpRangeResource) {
							return writePartialContent((HttpRangeResource) resource,
									outputMessage);
						}
						else {
							return writeContent(resource, outputMessage, 0, -1);
						}


					}));
		}
		else {
			// multiple resources, not supported!
			return Mono.error(new IllegalArgumentException(
					"Multiple resources not yet supported"));
		}
	}

	protected void addHeaders(HttpHeaders headers, Resource resource,
			MediaType contentType) {
		if (headers.getContentType() == null) {
			if (contentType == null ||
					!contentType.isConcrete() ||
					MediaType.APPLICATION_OCTET_STREAM.equals(contentType)) {
				contentType = MimeTypeUtils2.getMimeType(resource.getFilename()).
						map(MediaTypeUtils::toMediaType).
						orElse(MediaType.APPLICATION_OCTET_STREAM);
			}
			headers.setContentType(contentType);
		}
		if (headers.getContentLength() < 0) {
			contentLength(resource).ifPresent(headers::setContentLength);
		}
		headers.add(HttpHeaders.ACCEPT_RANGES, "bytes");
	}

	private Mono<Void> writeContent(Resource resource,
			ReactiveHttpOutputMessage outputMessage, long position, long count) {
		if (outputMessage instanceof ZeroCopyHttpOutputMessage) {
			Optional<File> file = getFile(resource);
			if (file.isPresent()) {
				ZeroCopyHttpOutputMessage zeroCopyResponse =
						(ZeroCopyHttpOutputMessage) outputMessage;

				if (count < 0) {
					count = file.get().length();
				}

				return zeroCopyResponse.setBody(file.get(), position, count);
			}
		}

		// non-zero copy fallback
		try {
			InputStream is = resource.getInputStream();
			long skipped = is.skip(position);
			if (skipped < position) {
				return Mono.error(new IOException(
						"Skipped only " + skipped + " bytes out of " + count +
								" required."));
			}

			Flux<DataBuffer> responseBody =
					DataBufferUtils.read(is, outputMessage.allocator(), BUFFER_SIZE);
			if (count > 0) {
				responseBody = DataBufferUtils.takeUntilByteCount(responseBody, count);
			}

			return outputMessage.setBody(responseBody);
		}
		catch (IOException ex) {
			return Mono.error(ex);
		}
	}

	protected Mono<Void> writePartialContent(HttpRangeResource resource,
			ReactiveHttpOutputMessage outputMessage) {

		// TODO: implement

		return Mono.empty();
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
		// TODO: introduce Resource.hasFile() property to bypass the potential IOException thrown in Resource.getFile()
		// the following Resource implementations do not support getURI/getFile
		if (!(resource instanceof ByteArrayResource ||
				resource instanceof DescriptiveResource ||
				resource instanceof InputStreamResource)) {
			try {
				URI resourceUri = resource.getURI();
				if (ResourceUtils.URL_PROTOCOL_FILE.equals(resourceUri.getScheme())) {
					return Optional.of(ResourceUtils.getFile(resourceUri));
				}
			}
			catch (IOException ignored) {
			}
		}
		return Optional.empty();
	}


}
