/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.multipart;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class FileStorageTests {

	@Test
	void fromPath() throws IOException {
		Path path = Files.createTempFile("spring", "test");
		FileStorage storage = FileStorage.fromPath(path);

		Mono<Path> directory = storage.directory();
		StepVerifier.create(directory)
				.expectNext(path)
				.verifyComplete();
	}

	@Test
	void tempDirectory() {
		FileStorage storage = FileStorage.tempDirectory(Schedulers::boundedElastic);

		Mono<Path> directory = storage.directory();
		StepVerifier.create(directory)
				.consumeNextWith(path -> {
					assertThat(path).exists();
					StepVerifier.create(directory)
							.expectNext(path)
							.verifyComplete();
				})
				.verifyComplete();
	}

	@Test
	void tempDirectoryDeleted() {
		FileStorage storage = FileStorage.tempDirectory(Schedulers::boundedElastic);

		Mono<Path> directory = storage.directory();
		StepVerifier.create(directory)
				.consumeNextWith(path1 -> {
					try {
						Files.delete(path1);
						StepVerifier.create(directory)
								.consumeNextWith(path2 -> assertThat(path2).isNotEqualTo(path1))
								.verifyComplete();
					}
					catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				})
				.verifyComplete();
	}

}
