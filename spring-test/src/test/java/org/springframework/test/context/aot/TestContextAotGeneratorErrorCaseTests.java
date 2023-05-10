/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.aot;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.aot.generate.InMemoryGeneratedFiles;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for error cases in {@link TestContextAotGenerator}.
 *
 * @author Sam Brannen
 * @since 6.0.9
 */
class TestContextAotGeneratorErrorCaseTests {

	@ParameterizedTest
	@CsvSource(delimiter = '=', textBlock = """
			'spring.aot.enabled'                = 'true'
			'org.graalvm.nativeimage.imagecode' = 'buildtime'
			'org.graalvm.nativeimage.imagecode' = 'runtime'
			'org.graalvm.nativeimage.imagecode' = 'bogus'
			""")
	void attemptToProcessWhileRunningInAotMode(String property, String value) {
		try {
			System.setProperty(property, value);

			assertThatIllegalStateException()
				.isThrownBy(() -> generator().processAheadOfTime(Stream.empty()))
				.withMessage("Cannot perform AOT processing during AOT run-time execution");
		}
		finally {
			System.clearProperty(property);
		}
	}

	@Test
	void attemptToProcessWhileRunningInGraalVmNativeBuildToolsAgentMode() {
		final String IMAGECODE = "org.graalvm.nativeimage.imagecode";
		try {
			System.setProperty(IMAGECODE, "AgenT");

			assertThatNoException().isThrownBy(() -> generator().processAheadOfTime(Stream.empty()));
		}
		finally {
			System.clearProperty(IMAGECODE);
		}
	}

	private static TestContextAotGenerator generator() {
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		return new TestContextAotGenerator(generatedFiles);
	}

}
