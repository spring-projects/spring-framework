/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.test.tools;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link SourceFileAssert}.
 *
 * @author Phillip Webb
 */
class SourceFileAssertTests {

	private static final String SAMPLE = """
			package com.example;

			import java.lang.Runnable;

			public class Sample implements Runnable {

				void run() {
					run("Hello World!");
				}

				void run(String message) {
					System.out.println(message);
				}

				public static void main(String[] args) {
					new Sample().run();
				}
			}
			""";

	private final SourceFile sourceFile = SourceFile.of(SAMPLE);

	@Test
	void containsWhenContainsAll() {
		assertThat(this.sourceFile).contains("Sample", "main");
	}

	@Test
	void containsWhenMissingOneThrowsException() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(this.sourceFile).contains("Sample",
						"missing")).withMessageContaining("to contain");
	}

	@Test
	void isEqualToWhenEqual() {
		assertThat(this.sourceFile).hasContent(SAMPLE);
	}

	@Test
	void isEqualToWhenNotEqualThrowsException() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(this.sourceFile).hasContent("no")).withMessageContaining(
						"expected", "but was");
	}

}
