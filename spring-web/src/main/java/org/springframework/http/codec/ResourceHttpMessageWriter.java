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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.ResourceEncoder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MimeTypeUtils;

/**
 * Implementation of {@link HttpMessageWriter} that can write
 * {@link Resource Resources}.
 *
 * <p>For a Resource reader simply use {@link ResourceDecoder} wrapped with
 * {@link DecoderHttpMessageReader}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
public class ResourceHttpMessageWriter extends AbstractServerHttpMessageWriter<Resource> {

	public static final String HTTP_RANGE_REQUEST_HINT = ResourceHttpMessageWriter.class.getName() + ".httpRange";

	private ResourceRegionHttpMessageWriter resourceRegionHttpMessageWriter;


	public ResourceHttpMessageWriter() {
		super(new EncoderHttpMessageWriter<>(new ResourceEncoder()));
		this.resourceRegionHttpMessageWriter = new ResourceRegionHttpMessageWriter();
	}

	public ResourceHttpMessageWriter(int bufferSize) {
		super(new EncoderHttpMessageWriter<>(new ResourceEncoder(bufferSize)));
		this.resourceRegionHttpMessageWriter = new ResourceRegionHttpMessageWriter(bufferSize);
	}


	@Override
	protected Map<String, Object> resolveWriteHints(ResolvableType streamType, ResolvableType elementType,
			MediaType mediaType, ServerHttpRequest request) {

		List<HttpRange> httpRanges = request.getHeaders().getRange();
		if (!httpRanges.isEmpty()) {
			return Collections.singletonMap(ResourceHttpMessageWriter.HTTP_RANGE_REQUEST_HINT, httpRanges);
		}
		return Collections.emptyMap();
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

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> write(Publisher<? extends Resource> inputStream, ResolvableType streamType,
			ResolvableType elementType, MediaType mediaType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints) {
		try {
			response.getHeaders().set(HttpHeaders.ACCEPT_RANGES, "bytes");
			Map<String, Object> mergedHints = new HashMap<>(hints);
			mergedHints.putAll(resolveWriteHints(streamType, elementType, mediaType, request));
			if (mergedHints.containsKey(HTTP_RANGE_REQUEST_HINT)) {
				response.setStatusCode(HttpStatus.PARTIAL_CONTENT);
				List<HttpRange> httpRanges = (List<HttpRange>) mergedHints.get(HTTP_RANGE_REQUEST_HINT);
				if (httpRanges.size() > 1) {
					final String boundary = MimeTypeUtils.generateMultipartBoundaryString();
					mergedHints.put(ResourceRegionHttpMessageWriter.BOUNDARY_STRING_HINT, boundary);
				}
				Flux<ResourceRegion> regions = Flux.from(inputStream)
						.flatMap(resource -> Flux.fromIterable(HttpRange.toResourceRegions(httpRanges, resource)));

				return this.resourceRegionHttpMessageWriter
						.write(regions, ResolvableType.forClass(ResourceRegion.class), mediaType, response, mergedHints);
			}
			else {
				return write(inputStream, elementType, mediaType, response, mergedHints);
			}
		}
		catch (IllegalArgumentException exc) {
			response.setStatusCode(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
			return response.setComplete();
		}
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

	/**
	 * Determine, if possible, the contentLength of the given resource without reading it.
	 * @param resource the resource instance
	 * @return the contentLength of the resource
	 */
	private OptionalLong contentLength(Resource resource) {
		// Don't try to determine contentLength on InputStreamResource - cannot be read afterwards...
		// Note: custom InputStreamResource subclasses could provide a pre-calculated content length!
		if (InputStreamResource.class != resource.getClass()) {
			try {
				return OptionalLong.of(resource.contentLength());
			}
			catch (IOException ignored) {
			}
		}
		return OptionalLong.empty();
	}

	private Mono<Void> writeContent(Resource resource, ResolvableType type,
			ReactiveHttpOutputMessage outputMessage, Map<String, Object> hints) {

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
