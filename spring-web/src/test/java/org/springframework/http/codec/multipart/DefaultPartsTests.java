/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.http.HttpHeaders;
import org.springframework.util.FileSystemUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for  {@link DefaultParts.FileContent}.
 * @author Maksim Sasnouski
 */
public class DefaultPartsTests {

	private static Path tempDir;

	@BeforeAll
	static void setup() throws IOException {
		tempDir = Files.createTempDirectory("DefaultPartsTests");
	}

	@AfterAll
	static void cleanup() throws IOException {
		FileSystemUtils.deleteRecursively(tempDir);
	}

	@Test
	void tempFilesDeletedBlocking() throws IOException {
		List<Path> tempFiles = createTempFiles(10);
		List<Part> parts = createParts(tempFiles);
		parts.forEach(part -> part.delete().block());
		tempFiles.forEach(file -> assertTrue(Files.notExists(file)));
	}

	@Test
	void tempFilesDeletedNonBlocking() throws IOException {
		List<Path> tempFiles = createTempFiles(10);
		List<Part> parts = createParts(tempFiles);

		List<Mono<Void>> deletionTasks = parts.stream()
				.map(Part::delete)
				.toList();

		// Emulate subscribe downstream (do not use .blockLast())
		Flux.concat(deletionTasks).subscribe();

		tempFiles.forEach(file -> assertTrue(Files.notExists(file)));
	}

	private List<Part> createParts(List<Path> tempFiles) {
		return tempFiles
				.stream()
				.map(this::createPart)
				.toList();
	}

	private Part createPart(Path file) {
		return DefaultParts.part(HttpHeaders.EMPTY, file, Schedulers.boundedElastic());
	}

	private List<Path> createTempFiles(int count) {
		return IntStream.range(0, count)
				.mapToObj(i -> createTempFile())
				.toList();
	}

	private Path createTempFile() {
		try {
			return Files.createTempFile(tempDir, "part", ".tmp");
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
