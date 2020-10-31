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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default {@code HttpMessageReader} for parsing {@code "multipart/form-data"}
 * requests to a stream of {@link Part}s.
 *
 * <p>In default, non-streaming mode, this message reader stores the
 * {@linkplain Part#content() contents} of parts smaller than
 * {@link #setMaxInMemorySize(int) maxInMemorySize} in memory, and parts larger
 * than that to a temporary file in
 * {@link #setFileStorageDirectory(Path) fileStorageDirectory}.
 * <p>In {@linkplain #setStreaming(boolean) streaming} mode, the contents of the
 * part is streamed directly from the parsed input buffer stream, and not stored
 * in memory nor file.
 *
 * <p>This reader can be provided to {@link MultipartHttpMessageReader} in order
 * to aggregate all parts into a Map.
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
public class DefaultPartHttpMessageReader extends LoggingCodecSupport implements HttpMessageReader<Part> {

	private static final String IDENTIFIER = "spring-multipart";

	private int maxInMemorySize = 256 * 1024;

	private int maxHeadersSize = 8 * 1024;

	private long maxDiskUsagePerPart = -1;

	private int maxParts = -1;

	private boolean streaming;

	private Scheduler blockingOperationScheduler = Schedulers.newBoundedElastic(Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE,
			Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE, IDENTIFIER, 60, true);

	private Mono<Path> fileStorageDirectory = Mono.defer(this::defaultFileStorageDirectory).cache();


	/**
	 * Configure the maximum amount of memory that is allowed per headers section of each part.
	 * When the limit
	 * @param byteCount the maximum amount of memory for headers
	 */
	public void setMaxHeadersSize(int byteCount) {
		this.maxHeadersSize = byteCount;
	}

	/**
	 * Get the {@link #setMaxInMemorySize configured} maximum in-memory size.
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}

	/**
	 * Configure the maximum amount of memory allowed per part.
	 * When the limit is exceeded:
	 * <ul>
	 * <li>file parts are written to a temporary file.
	 * <li>non-file parts are rejected with {@link DataBufferLimitException}.
	 * </ul>
	 * <p>By default this is set to 256K.
	 * <p>Note that this property is ignored when
	 * {@linkplain #setStreaming(boolean) streaming} is enabled.
	 * @param maxInMemorySize the in-memory limit in bytes; if set to -1 the entire
	 * contents will be stored in memory
	 */
	public void setMaxInMemorySize(int maxInMemorySize) {
		this.maxInMemorySize = maxInMemorySize;
	}

	/**
	 * Configure the maximum amount of disk space allowed for file parts.
	 * <p>By default this is set to -1, meaning that there is no maximum.
	 * <p>Note that this property is ignored when
	 * {@linkplain #setStreaming(boolean) streaming} is enabled, , or when
	 * {@link #setMaxInMemorySize(int) maxInMemorySize} is set to -1.
	 */
	public void setMaxDiskUsagePerPart(long maxDiskUsagePerPart) {
		this.maxDiskUsagePerPart = maxDiskUsagePerPart;
	}

	/**
	 * Specify the maximum number of parts allowed in a given multipart request.
	 * <p>By default this is set to -1, meaning that there is no maximum.
	 */
	public void setMaxParts(int maxParts) {
		this.maxParts = maxParts;
	}

	/**
	 * Sets the directory used to store parts larger than
	 * {@link #setMaxInMemorySize(int) maxInMemorySize}. By default, a directory
	 * named {@code spring-webflux-multipart} is created under the system
	 * temporary directory.
	 * <p>Note that this property is ignored when
	 * {@linkplain #setStreaming(boolean) streaming} is enabled, or when
	 * {@link #setMaxInMemorySize(int) maxInMemorySize} is set to -1.
	 * @throws IOException if an I/O error occurs, or the parent directory
	 * does not exist
	 */
	public void setFileStorageDirectory(Path fileStorageDirectory) throws IOException {
		Assert.notNull(fileStorageDirectory, "FileStorageDirectory must not be null");
		if (!Files.exists(fileStorageDirectory)) {
			Files.createDirectory(fileStorageDirectory);
		}
		this.fileStorageDirectory = Mono.just(fileStorageDirectory);
	}

	/**
	 * Sets the Reactor {@link Scheduler} to be used for creating files and
	 * directories, and writing to files. By default, a bounded scheduler is
	 * created with default properties.
	 * <p>Note that this property is ignored when
	 * {@linkplain #setStreaming(boolean) streaming} is enabled, or when
	 * {@link #setMaxInMemorySize(int) maxInMemorySize} is set to -1.
	 * @see Schedulers#newBoundedElastic
	 */
	public void setBlockingOperationScheduler(Scheduler blockingOperationScheduler) {
		Assert.notNull(blockingOperationScheduler, "FileCreationScheduler must not be null");
		this.blockingOperationScheduler = blockingOperationScheduler;
	}

	/**
	 * When set to {@code true}, the {@linkplain Part#content() part content}
	 * is streamed directly from the parsed input buffer stream, and not stored
	 * in memory nor file.
	 * When {@code false}, parts are backed by
	 * in-memory and/or file storage. Defaults to {@code false}.
	 *
	 * <p><strong>NOTE</strong> that with streaming enabled, the
	 * {@code Flux<Part>} that is produced by this message reader must be
	 * consumed in the original order, i.e. the order of the HTTP message.
	 * Additionally, the {@linkplain Part#content() body contents} must either
	 * be completely consumed or canceled before moving to the next part.
	 *
	 * <p>Also note that enabling this property effectively ignores
	 * {@link #setMaxInMemorySize(int) maxInMemorySize},
	 * {@link #setMaxDiskUsagePerPart(long) maxDiskUsagePerPart},
	 * {@link #setFileStorageDirectory(Path) fileStorageDirectory}, and
	 * {@link #setBlockingOperationScheduler(Scheduler) fileCreationScheduler}.
	 */
	public void setStreaming(boolean streaming) {
		this.streaming = streaming;
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		return Part.class.equals(elementType.toClass()) &&
				(mediaType == null || MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType));
	}

	@Override
	public Mono<Part> readMono(ResolvableType elementType, ReactiveHttpInputMessage message,
			Map<String, Object> hints) {
		return Mono.error(new UnsupportedOperationException("Cannot read multipart request body into single Part"));
	}

	@Override
	public Flux<Part> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		return Flux.defer(() -> {
			byte[] boundary = boundary(message);
			if (boundary == null) {
				return Flux.error(new DecodingException("No multipart boundary found in Content-Type: \"" +
						message.getHeaders().getContentType() + "\""));
			}
			Flux<MultipartParser.Token> tokens = MultipartParser.parse(message.getBody(), boundary,
					this.maxHeadersSize);

			return PartGenerator.createParts(tokens, this.maxParts, this.maxInMemorySize, this.maxDiskUsagePerPart,
					this.streaming, this.fileStorageDirectory, this.blockingOperationScheduler);
		});
	}

	@Nullable
	private static byte[] boundary(HttpMessage message) {
		MediaType contentType = message.getHeaders().getContentType();
		if (contentType != null) {
			String boundary = contentType.getParameter("boundary");
			if (boundary != null) {
				return boundary.getBytes(StandardCharsets.ISO_8859_1);
			}
		}
		return null;
	}

	@SuppressWarnings("BlockingMethodInNonBlockingContext")
	private Mono<Path> defaultFileStorageDirectory() {
		return Mono.fromCallable(() -> {
			Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"), IDENTIFIER);
			if (!Files.exists(tempDirectory)) {
				Files.createDirectory(tempDirectory);
			}
			return tempDirectory;
		}).subscribeOn(this.blockingOperationScheduler);

	}

}
