/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
