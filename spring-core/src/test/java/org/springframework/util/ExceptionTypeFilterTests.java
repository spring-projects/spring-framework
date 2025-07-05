/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ExceptionTypeFilter}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class ExceptionTypeFilterTests {

	@Test
	void emptyFilter() {
		var filter = new ExceptionTypeFilter(null, null);

		assertMatches(filter, Throwable.class);
		assertMatches(filter, Error.class);
		assertMatches(filter, Exception.class);
		assertMatches(filter, RuntimeException.class);
	}

	@Test
	void includes() {
		var filter = new ExceptionTypeFilter(List.of(FileNotFoundException.class, IllegalArgumentException.class), null);

		assertMatches(filter, FileNotFoundException.class);
		assertMatches(filter, IllegalArgumentException.class);
		assertMatches(filter, NumberFormatException.class);

		assertDoesNotMatch(filter, Throwable.class);
		assertDoesNotMatch(filter, FileSystemException.class);
	}

	@Test
	void includesSubtypeMatching() {
		var filter = new ExceptionTypeFilter(List.of(RuntimeException.class), null);

		assertMatches(filter, RuntimeException.class);
		assertMatches(filter, IllegalStateException.class);

		assertDoesNotMatch(filter, Exception.class);
	}

	@Test
	void excludes() {
		var filter = new ExceptionTypeFilter(null, List.of(FileNotFoundException.class, IllegalArgumentException.class));

		assertDoesNotMatch(filter, FileNotFoundException.class);
		assertDoesNotMatch(filter, IllegalArgumentException.class);

		assertMatches(filter, Throwable.class);
		assertMatches(filter, AssertionError.class);
		assertMatches(filter, FileSystemException.class);
	}

	@Test
	void excludesSubtypeMatching() {
		var filter = new ExceptionTypeFilter(null, List.of(IllegalArgumentException.class));

		assertDoesNotMatch(filter, IllegalArgumentException.class);
		assertDoesNotMatch(filter, NumberFormatException.class);

		assertMatches(filter, Throwable.class);
	}

	@Test
	void includesAndExcludes() {
		var filter = new ExceptionTypeFilter(List.of(IOException.class), List.of(FileNotFoundException.class));

		assertMatches(filter, IOException.class);
		assertMatches(filter, FileSystemException.class);

		assertDoesNotMatch(filter, FileNotFoundException.class);
		assertDoesNotMatch(filter, Throwable.class);
	}


	private static void assertMatches(ExceptionTypeFilter filter, Class<? extends Throwable> candidate) {
		assertThat(filter.match(candidate))
				.as("filter '" + filter + "' should match " + candidate.getSimpleName())
				.isTrue();
	}

	private static void assertDoesNotMatch(ExceptionTypeFilter filter, Class<? extends Throwable> candidate) {
		assertThat(filter.match(candidate))
				.as("filter '" + filter + "' should not match " + candidate.getSimpleName())
				.isFalse();
	}

}
