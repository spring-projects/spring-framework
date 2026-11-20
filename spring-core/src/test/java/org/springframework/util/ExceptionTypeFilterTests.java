/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * @author Chengang Guan
 */
class ExceptionTypeFilterTests {

	ExceptionTypeFilter filter;

	@Test
	void emptyFilter() {
		filter = new ExceptionTypeFilter(null, null);

		assertMatches(new Throwable());
		assertMatches(new Error());
		assertMatches(new Exception());
		assertMatches(new RuntimeException());

		assertMatchesCause(new Throwable());
		assertMatchesCause(new Error());
		assertMatchesCause(new Exception());
		assertMatchesCause(new RuntimeException());
	}

	@Test
	void includes() {
		filter = new ExceptionTypeFilter(List.of(FileNotFoundException.class, IllegalArgumentException.class), null);

		assertMatches(new FileNotFoundException());
		assertMatches(new IllegalArgumentException());
		assertMatches(new NumberFormatException());

		assertDoesNotMatch(new Throwable());
		assertDoesNotMatch(new FileSystemException("test"));
	}

	@Test
	void includesSubtypeMatching() {
		filter = new ExceptionTypeFilter(List.of(RuntimeException.class), null);

		assertMatches(new RuntimeException());
		assertMatches(new IllegalStateException());

		assertDoesNotMatch(new Exception());
	}

	@Test  // gh-35583
	void includesCauseAndSubtypeMatching() {
		filter = new ExceptionTypeFilter(List.of(IOException.class), null);

		assertMatchesCause(new IOException());
		assertMatchesCause(new FileNotFoundException());
		assertMatchesCause(new RuntimeException(new IOException()));
		assertMatchesCause(new RuntimeException(new FileNotFoundException()));
		assertMatchesCause(new Exception(new RuntimeException(new IOException())));
		assertMatchesCause(new Exception(new RuntimeException(new FileNotFoundException())));

		assertDoesNotMatchCause(new Exception());
	}

	@Test
	void excludes() {
		filter = new ExceptionTypeFilter(null, List.of(FileNotFoundException.class, IllegalArgumentException.class));

		assertDoesNotMatch(new FileNotFoundException());
		assertDoesNotMatch(new IllegalArgumentException());

		assertMatches(new Throwable());
		assertMatches(new AssertionError());
		assertMatches(new FileSystemException("test"));
	}

	@Test
	void excludesSubtypeMatching() {
		filter = new ExceptionTypeFilter(null, List.of(IllegalArgumentException.class));

		assertDoesNotMatch(new IllegalArgumentException());
		assertDoesNotMatch(new NumberFormatException());

		assertMatches(new Throwable());
	}

	@Test  // gh-35583
	void excludesCauseAndSubtypeMatching() {
		filter = new ExceptionTypeFilter(null, List.of(IOException.class));

		assertDoesNotMatchCause(new IOException());
		assertDoesNotMatchCause(new FileNotFoundException());
		assertDoesNotMatchCause(new RuntimeException(new IOException()));
		assertDoesNotMatchCause(new RuntimeException(new FileNotFoundException()));
		assertDoesNotMatchCause(new Exception(new RuntimeException(new IOException())));
		assertDoesNotMatchCause(new Exception(new RuntimeException(new FileNotFoundException())));

		assertMatchesCause(new Throwable());
	}

	@Test
	void includesAndExcludes() {
		filter = new ExceptionTypeFilter(List.of(IOException.class), List.of(FileNotFoundException.class));

		assertMatches(new IOException());
		assertMatches(new FileSystemException("test"));

		assertDoesNotMatch(new FileNotFoundException());
		assertDoesNotMatch(new Throwable());
	}

	@Test  // gh-37598
	void constructorAcceptsSubtypeClassCollections() {
		List<Class<? extends RuntimeException>> includes = List.of(RuntimeException.class);
		List<Class<? extends RuntimeException>> excludes = List.of(IllegalStateException.class);

		filter = new ExceptionTypeFilter(includes, excludes);

		assertMatches(new IllegalArgumentException());

		assertDoesNotMatch(new IllegalStateException());
		assertDoesNotMatch(new Exception());
	}


	private void assertMatches(Throwable candidate) {
		assertThat(this.filter.match(candidate))
				.as("filter '" + this.filter + "' should match " + candidate.getClass().getSimpleName())
				.isTrue();
	}

	private void assertDoesNotMatch(Throwable candidate) {
		assertThat(this.filter.match(candidate))
				.as("filter '" + this.filter + "' should not match " + candidate.getClass().getSimpleName())
				.isFalse();
	}

	private void assertMatchesCause(Throwable candidate) {
		assertThat(this.filter.match(candidate, true))
				.as("filter '" + this.filter + "' should match " + candidate.getClass().getSimpleName())
				.isTrue();
	}

	private void assertDoesNotMatchCause(Throwable candidate) {
		assertThat(this.filter.match(candidate, true))
				.as("filter '" + this.filter + "' should not match " + candidate.getClass().getSimpleName())
				.isFalse();
	}

}
