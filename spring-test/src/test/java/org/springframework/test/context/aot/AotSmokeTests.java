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

package org.springframework.test.context.aot;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.test.generator.compile.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for AOT support in the TestContext framework.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class AotSmokeTests extends AbstractAotTests {

	@Test
	// Using @CompileWithTargetClassAccess results in the following exception in classpathRoots():
	// java.lang.NullPointerException: Cannot invoke "java.net.URL.toURI()" because the return
	// value of "java.security.CodeSource.getLocation()" is null
	void scanClassPathThenGenerateSourceFilesAndCompileThem() {
		Stream<Class<?>> testClasses = scan("org.springframework.test.context.aot.samples.basic");
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);

		generator.processAheadOfTime(testClasses);

		List<String> sourceFiles = generatedFiles.getGeneratedFiles(Kind.SOURCE).keySet().stream().toList();
		assertThat(sourceFiles).containsExactlyInAnyOrder(expectedSourceFilesForBasicSpringTests);

		TestCompiler.forSystem().withFiles(generatedFiles)
			// .printFiles(System.out)
			.compile(compiled -> {
				// just make sure compilation completes without errors
			});
	}

}
