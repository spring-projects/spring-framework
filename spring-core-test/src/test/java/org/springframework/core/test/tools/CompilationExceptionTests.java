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

package org.springframework.core.test.tools;

import java.util.List;

import javax.tools.Diagnostic;

import org.junit.jupiter.api.Test;

import org.springframework.core.test.tools.CompilationException.Problem;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompilationException}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class CompilationExceptionTests {

	@Test
	void exceptionMessageReportsSingleError() {
		CompilationException exception = new CompilationException(
				List.of(new Problem(Diagnostic.Kind.ERROR, "error message")),
				SourceFiles.none(), ResourceFiles.none());
		assertThat(exception.getMessage().lines()).containsExactly(
				"Unable to compile source", "", "Errors:", "- error message");
	}

	@Test
	void exceptionMessageReportsSingleWarning() {
		CompilationException exception = new CompilationException(
				List.of(new Problem(Diagnostic.Kind.MANDATORY_WARNING, "warning message")),
				SourceFiles.none(), ResourceFiles.none());
		assertThat(exception.getMessage().lines()).containsExactly(
				"Unable to compile source", "", "Warnings:", "- warning message");
	}

	@Test
	void exceptionMessageReportsProblems() {
		CompilationException exception = new CompilationException(List.of(
				new Problem(Diagnostic.Kind.MANDATORY_WARNING, "warning message"),
				new Problem(Diagnostic.Kind.ERROR, "error message"),
				new Problem(Diagnostic.Kind.WARNING, "warning message2"),
				new Problem(Diagnostic.Kind.ERROR, "error message2")), SourceFiles.none(), ResourceFiles.none());
		assertThat(exception.getMessage().lines()).containsExactly(
				"Unable to compile source", "", "Errors:", "- error message", "- error message2", "" ,
				"Warnings:", "- warning message","- warning message2");
	}

	@Test
	void exceptionMessageReportsSourceCode() {
		CompilationException exception = new CompilationException(
				List.of(new Problem(Diagnostic.Kind.ERROR, "error message")),
				SourceFiles.of(SourceFile.of("public class Hello {}")), ResourceFiles.none());
		assertThat(exception.getMessage().lines()).containsExactly(
				"Unable to compile source", "", "Errors:", "- error message", "",
				"---- source: Hello.java", "public class Hello {}");
	}

	@Test
	void exceptionMessageReportsResource() {
		CompilationException exception = new CompilationException(
				List.of(new Problem(Diagnostic.Kind.ERROR, "error message")),
				SourceFiles.none(), ResourceFiles.of(ResourceFile.of("application.properties", "test=value")));
		assertThat(exception.getMessage().lines()).containsExactly(
				"Unable to compile source", "", "Errors:", "- error message", "",
				"---- resource: application.properties", "test=value");
	}

}
