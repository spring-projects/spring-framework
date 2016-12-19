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
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.ResourceRegionEncoder;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.util.ResourceUtils;

/**
 * Implementation of {@link HttpMessageWriter} that can write
 * {@link ResourceRegion ResourceRegion}s.
 *
 * <p>Note that there is no {@link HttpMessageReader} counterpart.
 *
 * @author Brian Clozel
 * @since 5.0
 */
class ResourceRegionHttpMessageWriter extends EncoderHttpMessageWriter<ResourceRegion> {

	public static final String BOUNDARY_STRING_HINT = ResourceRegionHttpMessageWriter.class.getName() + ".boundaryString";

	public ResourceRegionHttpMessageWriter() {
		super(new ResourceRegionEncoder());
	}

	public ResourceRegionHttpMessageWriter(int bufferSize) {
		super(new ResourceRegionEncoder(bufferSize));
	}

	@Override
	public Mono<Void> write(Publisher<? extends ResourceRegion> inputStream, ResolvableType type,
			MediaType contentType, ReactiveHttpOutputMessage outputMessage, Map<String, Object> hints) {

		if (hints != null && hints.containsKey(BOUNDARY_STRING_HINT)) {
			String boundary = (String) hints.get(BOUNDARY_STRING_HINT);
			hints.put(ResourceRegionEncoder.BOUNDARY_STRING_HINT, boundary);
			outputMessage.getHeaders()
					.setContentType(MediaType.parseMediaType("multipart/byteranges;boundary=" + boundary));
			return super.write(inputStream, type, contentType, outputMessage, hints);
		}
		else {
			return Mono.from(inputStream)
					.then(region -> {
						writeSingleResourceRegionHeaders(region, contentType, outputMessage);
						return writeResourceRegion(region, type, outputMessage);
					});
		}
	}


	private void writeSingleResourceRegionHeaders(ResourceRegion region, MediaType contentType,
			ReactiveHttpOutputMessage outputMessage) {

		OptionalLong resourceLength = contentLength(region.getResource());
		resourceLength.ifPresent(length -> {
			long start = region.getPosition();
			long end = start + region.getCount() - 1;
			end = Math.min(end, length - 1);
			outputMessage.getHeaders().add("Content-Range", "bytes " + start + '-' + end + '/' + length);
			outputMessage.getHeaders().setContentLength(end - start + 1);
		});
		outputMessage.getHeaders().setContentType(contentType);
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

	private Mono<Void> writeResourceRegion(ResourceRegion region,
			ResolvableType type, ReactiveHttpOutputMessage outputMessage) {
		if (outputMessage instanceof ZeroCopyHttpOutputMessage) {
			Optional<File> file = getFile(region.getResource());
			if (file.isPresent()) {
				ZeroCopyHttpOutputMessage zeroCopyResponse =
						(ZeroCopyHttpOutputMessage) outputMessage;

				return zeroCopyResponse.writeWith(file.get(), region.getPosition(), region.getCount());
			}
		}

		// non-zero copy fallback, using ResourceRegionEncoder
		return super.write(Mono.just(region), type,
				outputMessage.getHeaders().getContentType(), outputMessage, Collections.emptyMap());
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
