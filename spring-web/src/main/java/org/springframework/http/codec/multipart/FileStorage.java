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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Represents a directory used to store parts larger than
 * {@link DefaultPartHttpMessageReader#setMaxInMemorySize(int)}.
 *
 * @author Arjen Poutsma
 * @since 5.3.7
 */
abstract class FileStorage {

	private static final Log logger = LogFactory.getLog(FileStorage.class);


	protected FileStorage() {
	}

	/**
	 * Get the mono of the directory to store files in.
	 */
	public abstract Mono<Path> directory();


	/**
	 * Create a new {@code FileStorage} from a user-specified path. Creates the
	 * path if it does not exist.
	 */
	public static FileStorage fromPath(Path path) throws IOException {
		if (!Files.exists(path)) {
			Files.createDirectory(path);
		}
		return new PathFileStorage(path);
	}

	/**
	 * Create a new {@code FileStorage} based on a temporary directory.
	 * @param scheduler the scheduler to use for blocking operations
	 */
	public static FileStorage tempDirectory(Supplier<Scheduler> scheduler) {
		return new TempFileStorage(scheduler);
	}


	private static final class PathFileStorage extends FileStorage {

		private final Mono<Path> directory;

		public PathFileStorage(Path directory) {
			this.directory = Mono.just(directory);
		}

		@Override
		public Mono<Path> directory() {
			return this.directory;
		}
	}


	private static final class TempFileStorage extends FileStorage {

		private static final String IDENTIFIER = "spring-multipart-";

		private final Supplier<Scheduler> scheduler;

		private volatile Mono<Path> directory = tempDirectory();


		public TempFileStorage(Supplier<Scheduler> scheduler) {
			this.scheduler = scheduler;
		}

		@Override
		public Mono<Path> directory() {
			return this.directory
					.flatMap(this::createNewDirectoryIfDeleted)
					.subscribeOn(this.scheduler.get());
		}

		private Mono<Path> createNewDirectoryIfDeleted(Path directory) {
			if (!Files.exists(directory)) {
				// Some daemons remove temp directories. Let's create a new one.
				Mono<Path> newDirectory = tempDirectory();
				this.directory = newDirectory;
				return newDirectory;
			}
			else {
				return Mono.just(directory);
			}
		}

		private static Mono<Path> tempDirectory() {
			return Mono.fromCallable(() -> {
				Path directory = Files.createTempDirectory(IDENTIFIER);
				if (logger.isDebugEnabled()) {
					logger.debug("Created temporary storage directory: " + directory);
				}
				return directory;
			}).cache();
		}
	}

}
