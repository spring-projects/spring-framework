/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.codec;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.codec.ResourceDecoder;
import org.springframework.core.codec.ResourceEncoder;
import org.springframework.core.codec.ResourceRegionEncoder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeTypeUtils;

/**
 * {@code HttpMessageWriter} that can write a {@link Resource}.
 *
 * <p>Also an implementation of {@code HttpMessageWriter} with support for writing one
 * or more {@link ResourceRegion}'s based on the HTTP ranges specified in the request.
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
public class ResourceHttpMessageWriter implements HttpMessageWriter<Resource> {

	private static final ResolvableType REGION_TYPE = ResolvableType.forClass(ResourceRegion.class);

	private static final Log logger = HttpLogging.forLogName(ResourceHttpMessageWriter.class);


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
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		return this.encoder.canEncode(elementType, mediaType);
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return this.mediaTypes;
	}


	// Client or server: single Resource...

	@Override
	public Mono<Void> write(Publisher<? extends Resource> inputStream, ResolvableType elementType,
			@Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		return Mono.from(inputStream).flatMap(resource ->
				writeResource(resource, elementType, mediaType, message, hints));
	}

	private Mono<Void> writeResource(Resource resource, ResolvableType type, @Nullable MediaType mediaType,
			ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		return addDefaultHeaders(message, resource, mediaType, hints)
				.then(Mono.defer(() -> {
					Mono<Void> result = zeroCopy(resource, null, message, hints);
					if (result != null) {
						return result;
					}
					else {
						Mono<Resource> input = Mono.just(resource);
						DataBufferFactory factory = message.bufferFactory();
						Flux<DataBuffer> body = this.encoder.encode(input, factory, type, message.getHeaders().getContentType(), hints)
								.subscribeOn(Schedulers.boundedElastic());
						if (logger.isDebugEnabled()) {
							body = body.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
						}
						return message.writeWith(body);
					}
				}));
	}

	/**
	 * Adds the default headers for the given resource to the given message.
	 * @since 6.1
	 */
	public Mono<Void> addDefaultHeaders(ReactiveHttpOutputMessage message, Resource resource, @Nullable MediaType contentType, Map<String, Object> hints) {
		return Mono.defer(() -> {
			HttpHeaders headers = message.getHeaders();
			MediaType resourceMediaType = getResourceMediaType(contentType, resource, hints);
			headers.setContentType(resourceMediaType);
			if (message instanceof ServerHttpResponse) {
				// server side
				headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
			}

			if (headers.getContentLength() < 0) {
				return lengthOf(resource)
						.flatMap(contentLength -> {
							headers.setContentLength(contentLength);
							return Mono.empty();
						});
			}
			else {
				return Mono.empty();
			}
		});
	}

	private static MediaType getResourceMediaType(
			@Nullable MediaType mediaType, Resource resource, Map<String, Object> hints) {

		if (mediaType != null && mediaType.isConcrete() && !mediaType.equals(MediaType.APPLICATION_OCTET_STREAM)) {
			return mediaType;
		}
		mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
		if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
			logger.debug(Hints.getLogPrefix(hints) + "Resource associated with '" + mediaType + "'");
		}
		return mediaType;
	}

	private static Mono<Long> lengthOf(Resource resource) {
		// Don't consume InputStream...
		if (InputStreamResource.class != resource.getClass()) {
			return Mono.fromCallable(resource::contentLength)
					.filter(length -> length != -1)
					.onErrorComplete(IOException.class)
					.subscribeOn(Schedulers.boundedElastic());
		}
		else {
			return Mono.empty();
		}
	}

	@Nullable
	private static Mono<Void> zeroCopy(Resource resource, @Nullable ResourceRegion region,
			ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		if (message instanceof ZeroCopyHttpOutputMessage zeroCopyHttpOutputMessage && resource.isFile()) {
			try {
				File file = resource.getFile();
				long pos = region != null ? region.getPosition() : 0;
				long count = region != null ? region.getCount() : file.length();
				if (logger.isDebugEnabled()) {
					String formatted = region != null ? "region " + pos + "-" + (count) + " of " : "";
					logger.debug(Hints.getLogPrefix(hints) + "Zero-copy " + formatted + "[" + resource + "]");
				}
				return zeroCopyHttpOutputMessage.writeWith(file, pos, count);
			}
			catch (IOException ex) {
				// should not happen
			}
		}
		return null;
	}


	// Server-side only: single Resource or sub-regions...

	@Override
	public Mono<Void> write(Publisher<? extends Resource> inputStream, @Nullable ResolvableType actualType,
			ResolvableType elementType, @Nullable MediaType mediaType, ServerHttpRequest request,
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

		return Mono.from(inputStream).flatMap(resource -> {
			if (ranges.isEmpty()) {
				return writeResource(resource, elementType, mediaType, response, hints);
			}
			response.setStatusCode(HttpStatus.PARTIAL_CONTENT);
			List<ResourceRegion> regions = HttpRange.toResourceRegions(ranges, resource);
			MediaType resourceMediaType = getResourceMediaType(mediaType, resource, hints);
			if (regions.size() == 1){
				ResourceRegion region = regions.get(0);
				headers.setContentType(resourceMediaType);
				return lengthOf(resource)
						.flatMap(contentLength -> {
							long start = region.getPosition();
							long end = start + region.getCount() - 1;
							end = Math.min(end, contentLength - 1);
							headers.add("Content-Range", "bytes " + start + '-' + end + '/' + contentLength);
							headers.setContentLength(end - start + 1);
							return Mono.empty();
						})
						.then(writeSingleRegion(region, response, hints));
			}
			else {
				String boundary = MimeTypeUtils.generateMultipartBoundaryString();
				MediaType multipartType = MediaType.parseMediaType("multipart/byteranges;boundary=" + boundary);
				headers.setContentType(multipartType);
				Map<String, Object> allHints = Hints.merge(hints, ResourceRegionEncoder.BOUNDARY_STRING_HINT, boundary);
				return encodeAndWriteRegions(Flux.fromIterable(regions), resourceMediaType, response, allHints);
			}
		});
	}

	private Mono<Void> writeSingleRegion(ResourceRegion region, ReactiveHttpOutputMessage message,
			Map<String, Object> hints) {

		Mono<Void> result = zeroCopy(region.getResource(), region, message, hints);
		if (result != null) {
			return result;
		}
		else {
			Publisher<? extends ResourceRegion> input = Mono.just(region);
			MediaType mediaType = message.getHeaders().getContentType();
			return encodeAndWriteRegions(input, mediaType, message, hints);
		}
	}

	private Mono<Void> encodeAndWriteRegions(Publisher<? extends ResourceRegion> publisher,
			@Nullable MediaType mediaType, ReactiveHttpOutputMessage message, Map<String, Object> hints) {

		Flux<DataBuffer> body = this.regionEncoder
				.encode(publisher, message.bufferFactory(), REGION_TYPE, mediaType,hints)
				.subscribeOn(Schedulers.boundedElastic());

		return message.writeWith(body);
	}

}
