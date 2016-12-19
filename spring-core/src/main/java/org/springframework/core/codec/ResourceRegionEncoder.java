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

package org.springframework.core.codec;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
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
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

/**
 * Encoder for {@link ResourceRegion}s.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public class ResourceRegionEncoder extends AbstractEncoder<ResourceRegion> {

	public static final int DEFAULT_BUFFER_SIZE = StreamUtils.BUFFER_SIZE;

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
	public boolean canEncode(ResolvableType elementType, MimeType mimeType) {

		return super.canEncode(elementType, mimeType)
				&& ResourceRegion.class.isAssignableFrom(elementType.getRawClass());
	}

	@Override
	@SuppressWarnings("unchecked")
	public Flux<DataBuffer> encode(Publisher<? extends ResourceRegion> inputStream,
			DataBufferFactory bufferFactory, ResolvableType elementType, MimeType mimeType, Map<String, Object> hints) {

		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");

		if (inputStream instanceof Mono) {
			return ((Mono<? extends ResourceRegion>) inputStream)
					.flatMap(region -> writeResourceRegion(region, bufferFactory));
		}
		else {
			Assert.notNull(hints, "'hints' must not be null");
			Assert.isTrue(hints.containsKey(BOUNDARY_STRING_HINT), "'hints' must contain boundaryString hint");
			final String boundaryString = (String) hints.get(BOUNDARY_STRING_HINT);

			byte[] startBoundary = getAsciiBytes("\r\n--" + boundaryString + "\r\n");
			byte[] contentType = getAsciiBytes("Content-Type: " + mimeType.toString() + "\r\n");

			Flux<DataBuffer> regions = Flux.from(inputStream).
					concatMap(region ->
							Flux.concat(
									getRegionPrefix(bufferFactory, startBoundary, contentType, region),
									writeResourceRegion(region, bufferFactory)
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

	private Flux<DataBuffer> writeResourceRegion(ResourceRegion region, DataBufferFactory bufferFactory) {
		try {
			ReadableByteChannel resourceChannel = region.getResource().readableChannel();
			Flux<DataBuffer> in = DataBufferUtils.read(resourceChannel, bufferFactory, this.bufferSize);
			Flux<DataBuffer> skipped = DataBufferUtils.skipUntilByteCount(in, region.getPosition());
			return DataBufferUtils.takeUntilByteCount(skipped, region.getCount());
		}
		catch (IOException exc) {
			return Flux.error(exc);
		}
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
			return getAsciiBytes("Content-Range: bytes " + start + '-' + end + '/' + contentLength.getAsLong() + "\r\n\r\n");
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
