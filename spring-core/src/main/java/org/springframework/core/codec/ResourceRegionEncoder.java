/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.codec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.OptionalLong;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

/**
 * Encoder for {@link ResourceRegion ResourceRegions}.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public class ResourceRegionEncoder extends AbstractEncoder<ResourceRegion> {

	/**
	 * The default buffer size used by the encoder.
	 */
	public static final int DEFAULT_BUFFER_SIZE = StreamUtils.BUFFER_SIZE;

	/**
	 * The hint key that contains the boundary string.
	 */
	public static final String BOUNDARY_STRING_HINT = ResourceRegionEncoder.class.getName() + ".boundaryString";

	private final int bufferSize;


	public ResourceRegionEncoder() {
		this(DEFAULT_BUFFER_SIZE);
	}

	public ResourceRegionEncoder(int bufferSize) {
		super(MimeTypeUtils.APPLICATION_OCTET_STREAM, MimeTypeUtils.ALL);
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be larger than 0");
		this.bufferSize = bufferSize;
	}

	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return super.canEncode(elementType, mimeType)
				&& ResourceRegion.class.isAssignableFrom(elementType.toClass());
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<? extends ResourceRegion> inputStream,
			DataBufferFactory bufferFactory, ResolvableType elementType, @Nullable MimeType mimeType,
			@Nullable Map<String, Object> hints) {

		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");

		if (inputStream instanceof Mono) {
			return ((Mono<? extends ResourceRegion>) inputStream)
					.flatMapMany(region -> writeResourceRegion(region, bufferFactory, hints));
		}
		else {
			final String boundaryString = Hints.getRequiredHint(hints, BOUNDARY_STRING_HINT);
			byte[] startBoundary = getAsciiBytes("\r\n--" + boundaryString + "\r\n");
			byte[] contentType =
					(mimeType != null ? getAsciiBytes("Content-Type: " + mimeType + "\r\n") : new byte[0]);

			Flux<DataBuffer> regions = Flux.from(inputStream).
					concatMap(region ->
							Flux.concat(
									getRegionPrefix(bufferFactory, startBoundary, contentType, region),
									writeResourceRegion(region, bufferFactory, hints)
							));
			return Flux.concat(regions, getRegionSuffix(bufferFactory, boundaryString));
		}
	}

	private Flux<DataBuffer> getRegionPrefix(DataBufferFactory bufferFactory, byte[] startBoundary,
			byte[] contentType, ResourceRegion region) {

		return Flux.just(
				bufferFactory.allocateBuffer(startBoundary.length).write(startBoundary),
				bufferFactory.allocateBuffer(contentType.length).write(contentType),
				bufferFactory.wrap(ByteBuffer.wrap(getContentRangeHeader(region)))
		);
	}

	private Flux<DataBuffer> writeResourceRegion(
			ResourceRegion region, DataBufferFactory bufferFactory, @Nullable Map<String, Object> hints) {

		Resource resource = region.getResource();
		long position = region.getPosition();
		long count = region.getCount();

		if (logger.isDebugEnabled() && !Hints.isLoggingSuppressed(hints)) {
			logger.debug(Hints.getLogPrefix(hints) +
					"Writing region " + position + "-" + (position + count) + " of [" + resource + "]");
		}

		Flux<DataBuffer> in = DataBufferUtils.read(resource, position, bufferFactory, this.bufferSize);
		return DataBufferUtils.takeUntilByteCount(in, count);
	}

	private Flux<DataBuffer> getRegionSuffix(DataBufferFactory bufferFactory, String boundaryString) {
		byte[] endBoundary = getAsciiBytes("\r\n--" + boundaryString + "--");
		return Flux.just(bufferFactory.allocateBuffer(endBoundary.length).write(endBoundary));
	}

	private byte[] getAsciiBytes(String in) {
		return in.getBytes(StandardCharsets.US_ASCII);
	}

	private byte[] getContentRangeHeader(ResourceRegion region) {
		long start = region.getPosition();
		long end = start + region.getCount() - 1;
		OptionalLong contentLength = contentLength(region.getResource());
		if (contentLength.isPresent()) {
			long length = contentLength.getAsLong();
			return getAsciiBytes("Content-Range: bytes " + start + '-' + end + '/' + length + "\r\n\r\n");
		}
		else {
			return getAsciiBytes("Content-Range: bytes " + start + '-' + end + "\r\n\r\n");
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

}
