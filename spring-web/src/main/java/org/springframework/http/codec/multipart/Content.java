/*
 * Copyright 2002-2021 the original author or authors.
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

/**
 * Part content abstraction used by {@link DefaultParts}.
 *
 * @author Arjen Poutsma
 * @since 5.3.13
 */
abstract class Content {


	protected Content() {
	}

	/**
	 * Return the content.
	 */
	public abstract Flux<DataBuffer> content();

	/**
	 * Delete this content. Default implementation does nothing.
	 */
	public Mono<Void> delete() {
		return Mono.empty();
	}

	/**
	 * Returns a new {@code Content} based on the given flux of data buffers.
	 */
	public static Content fromFlux(Flux<DataBuffer> content) {
		return new FluxContent(content);
	}

	/**
	 * Return a new {@code Content} based on the given file path.
	 */
	public static Content fromFile(Path file, Scheduler scheduler) {
		return new FileContent(file, scheduler);
	}


	/**
	 * {@code Content} implementation based on a flux of data buffers.
	 */
	private static final class FluxContent extends Content {

		private final Flux<DataBuffer> content;


		public FluxContent(Flux<DataBuffer> content) {
			this.content = content;
		}


		@Override
		public Flux<DataBuffer> content() {
			return this.content;
		}
	}


	/**
	 * {@code Content} implementation based on a file.
	 */
	private static final class FileContent extends Content {

		private final Path file;

		private final Scheduler scheduler;


		public FileContent(Path file, Scheduler scheduler) {
			this.file = file;
			this.scheduler = scheduler;
		}


		@Override
		public Flux<DataBuffer> content() {
			return DataBufferUtils.readByteChannel(
					() -> Files.newByteChannel(this.file, StandardOpenOption.READ),
							DefaultDataBufferFactory.sharedInstance, 1024)
					.subscribeOn(this.scheduler);
		}

		@Override
		public Mono<Void> delete() {
			return Mono.<Void>fromRunnable(() -> {
						try {
							Files.delete(this.file);
						}
						catch (IOException ex) {
							throw new UncheckedIOException(ex);
						}
					})
					.subscribeOn(this.scheduler);
		}
	}
}
