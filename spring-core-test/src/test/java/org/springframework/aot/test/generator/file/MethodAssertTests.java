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

package org.springframework.aot.test.generator.file;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link MethodAssert}.
 *
 * @author Phillip Webb
 */
class MethodAssertTests {

	private static final String SAMPLE = """
			package com.example;

			public class Sample {

				public void run() {
					System.out.println("Hello World!");
				}

			}
			""";

	private final SourceFile sourceFile = SourceFile.of(SAMPLE);

	@Test
	void withBodyWhenMatches() {
		assertThat(this.sourceFile).hasMethodNamed("run").withBody("""
				System.out.println("Hello World!");""");
	}

	@Test
	void withBodyWhenDoesNotMatchThrowsException() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(this.sourceFile).hasMethodNamed("run").withBody("""
						System.out.println("Hello Spring!");""")).withMessageContaining(
						"to be equal to");
	}

	@Test
	void withBodyContainingWhenContainsAll() {
		assertThat(this.sourceFile).hasMethodNamed("run").withBodyContaining("Hello",
				"World!");
	}

	@Test
	void withBodyWhenDoesNotContainOneThrowsException() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(
				() -> assertThat(this.sourceFile).hasMethodNamed(
						"run").withBodyContaining("Hello",
								"Spring!")).withMessageContaining("to contain");
	}

}
