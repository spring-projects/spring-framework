/*
 * Copyright 2002-2022 the original author or authors.
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
		assertThat(this.sourceFile).isEqualTo(SAMPLE);
	}

	@Test
	void isEqualToWhenNotEqualThrowsException() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(this.sourceFile).isEqualTo("no")).withMessageContaining(
						"expected", "but was");
	}

}
