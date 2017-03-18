/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.core.codec.ResourceRegionEncoder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
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

import static java.util.Collections.emptyMap;

/**
 * {@code HttpMessageWriter} that can write a {@link Resource}.
 *
 * <p>Also an implementation of {@code ServerHttpMessageWriter} with support
 * for writing one or more {@link ResourceRegion}'s based on the HTTP ranges
 * specified in the request.
 *
 * <p>For reading to a Resource, use {@link ResourceDecoder} wrapped with
 * {@link DecoderHttpMessageReader}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see ResourceEncoder
 * @see ResourceRegionEncoder
 * @see HttpRange
 */
public class ResourceHttpMessageWriter implements ServerHttpMessageWriter<Resource> {

	private static final ResolvableType REGION_TYPE = ResolvableType.forClass(ResourceRegion.class);


	private final ResourceEncoder encoder;

	private final ResourceRegionEncoder regionEncoder;

	private final List<MediaType> mediaTypes;


	public ResourceHttpMessageWriter() {
		this(ResourceEncoder.DEFAULT_BUFFER_SIZE);
	}

	public ResourceHttpMessageWriter(int bufferSize) {
		this.encoder = new ResourceEncoder(bufferSize);
		this.regionEncoder = new ResourceRegionEncoder(bufferSize);
		this.mediaTypes = MediaType.asMediaTypes(this.encoder.getEncodableMimeTypes());
	}


	@Override
	public boolean canWrite(ResolvableType elementType, MediaType mediaType) {
		return this.encoder.canEncode(elementType, mediaType);
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return this.mediaTypes;
	}


	// HttpMessageWriter (client and server): single Resource

	@Override
	public Mono<Void> write(Publisher<? extends Resource> inputStream, ResolvableType elementType,
			MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		return Mono.from(inputStream).then(resource ->
				writeResource(resource, elementType, mediaType, message, hints));
	}

	private Mono<Void> writeResource(Resource resource, ResolvableType type, MediaType mediaType,
			ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		HttpHeaders headers = message.getHeaders();
		MediaType resourceMediaType = getResourceMediaType(mediaType, resource);
		headers.setContentType(resourceMediaType);

		if (headers.getContentLength() < 0) {
			lengthOf(resource).ifPresent(headers::setContentLength);
		}

		return zeroCopy(resource, null, message)
				.orElseGet(() -> {
					Mono<Resource> input = Mono.just(resource);
					DataBufferFactory factory = message.bufferFactory();
					Flux<DataBuffer> body = this.encoder.encode(input, factory, type, resourceMediaType, hints);
					return message.writeWith(body);
				});
	}

	private static MediaType getResourceMediaType(MediaType type, Resource resource) {
		if (type != null && type.isConcrete() && !type.equals(MediaType.APPLICATION_OCTET_STREAM)) {
			return type;
		}
		type = MediaTypeFactory.getMediaType(resource);
		return type != null ? type : MediaType.APPLICATION_OCTET_STREAM;
	}

	private static OptionalLong lengthOf(Resource resource) {
		// Don't consume InputStream...
		if (InputStreamResource.class != resource.getClass()) {
			try {
				return OptionalLong.of(resource.contentLength());
			}
			catch (IOException ignored) {
			}
		}
		return OptionalLong.empty();
	}

	private static Optional<Mono<Void>> zeroCopy(Resource resource, ResourceRegion region,
			ReactiveHttpOutputMessage message) {

		if (message instanceof ZeroCopyHttpOutputMessage) {
			if (resource.isFile()) {
				try {
					File file = resource.getFile();
					long pos = region != null ? region.getPosition() : 0;
					long count = region != null ? region.getCount() : file.length();
					return Optional.of(((ZeroCopyHttpOutputMessage) message).writeWith(file, pos, count));
				}
				catch (IOException ex) {
					// should not happen
				}
			}
		}
		return Optional.empty();
	}


	// ServerHttpMessageWriter (server only): single Resource or sub-regions

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> write(Publisher<? extends Resource> inputStream, ResolvableType streamType,
			ResolvableType elementType, MediaType mediaType, ServerHttpRequest request,
			ServerHttpResponse response, Map<String, Object> hints) {

		HttpHeaders headers = response.getHeaders();
		headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

		List<HttpRange> ranges;
		try {
			ranges = request.getHeaders().getRange();
		}
		catch (IllegalArgumentException ex) {
			response.setStatusCode(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
			return response.setComplete();
		}

		return Mono.from(inputStream).then(resource -> {

			if (ranges.isEmpty()) {
				return writeResource(resource, elementType, mediaType, response, hints);
			}

			response.setStatusCode(HttpStatus.PARTIAL_CONTENT);
			List<ResourceRegion> regions = HttpRange.toResourceRegions(ranges, resource);
			MediaType resourceMediaType = getResourceMediaType(mediaType, resource);

			if (regions.size() == 1){
				ResourceRegion region = regions.get(0);
				headers.setContentType(resourceMediaType);
				lengthOf(resource).ifPresent(length -> {
					long start = region.getPosition();
					long end = start + region.getCount() - 1;
					end = Math.min(end, length - 1);
					headers.add("Content-Range", "bytes " + start + '-' + end + '/' + length);
					headers.setContentLength(end - start + 1);
				});
				return writeSingleRegion(region, response);
			}
			else {
				String boundary = MimeTypeUtils.generateMultipartBoundaryString();
				MediaType multipartType = MediaType.parseMediaType("multipart/byteranges;boundary=" + boundary);
				headers.setContentType(multipartType);
				Map<String, Object> theHints = new HashMap<>(hints);
				theHints.put(ResourceRegionEncoder.BOUNDARY_STRING_HINT, boundary);
				return encodeAndWriteRegions(Flux.fromIterable(regions), resourceMediaType, response, theHints);
			}
		});
	}

	private Mono<Void> writeSingleRegion(ResourceRegion region, ReactiveHttpOutputMessage message) {

		return zeroCopy(region.getResource(), region, message)
				.orElseGet(() -> {
					Publisher<? extends ResourceRegion> input = Mono.just(region);
					MediaType mediaType = message.getHeaders().getContentType();
					return encodeAndWriteRegions(input, mediaType, message, emptyMap());
				});
	}

	private Mono<Void> encodeAndWriteRegions(Publisher<? extends ResourceRegion> publisher,
			MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		Flux<DataBuffer> body = this.regionEncoder.encode(
				publisher, message.bufferFactory(), REGION_TYPE, mediaType, hints);

		return message.writeWith(body);
	}

}
