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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;

/**
 * Exception thrown when code cannot compile. Expose the {@linkplain Problem
 * problems} for further inspection.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
@SuppressWarnings("serial")
public class CompilationException extends RuntimeException {

	private final List<Problem> problems;

	CompilationException(List<Problem> problems, SourceFiles sourceFiles, ResourceFiles resourceFiles) {
		super(buildMessage(problems, sourceFiles, resourceFiles));
		this.problems = problems;
	}

	private static String buildMessage(List<Problem> problems, SourceFiles sourceFiles,
			ResourceFiles resourceFiles) {
		StringWriter out = new StringWriter();
		PrintWriter writer = new PrintWriter(out);
		writer.println("Unable to compile source");
		Function<List<Problem>, String> createBulletList = elements -> elements.stream()
				.map(warning -> "- %s".formatted(warning.message()))
				.collect(Collectors.joining("\n"));

		List<Problem> errors = problems.stream()
				.filter(problem -> problem.kind == Diagnostic.Kind.ERROR).toList();
		if (!errors.isEmpty()) {
			writer.println();
			writer.println("Errors:");
			writer.println(createBulletList.apply(errors));
		}
		List<Problem> warnings = problems.stream()
				.filter(problem -> problem.kind == Diagnostic.Kind.WARNING ||
						problem.kind == Diagnostic.Kind.MANDATORY_WARNING).toList();
		if (!warnings.isEmpty()) {
			writer.println();
			writer.println("Warnings:");
			writer.println(createBulletList.apply(warnings));
		}
		if (!sourceFiles.isEmpty()) {
			for (SourceFile sourceFile : sourceFiles) {
				writer.println();
				writer.printf("---- source: %s%n".formatted(sourceFile.getPath()));
				writer.println(sourceFile.getContent());
			}
		}
		if (!resourceFiles.isEmpty()) {
			for (ResourceFile resourceFile : resourceFiles) {
				writer.println();
				writer.printf("---- resource: %s%n".formatted(resourceFile.getPath()));
				writer.println(resourceFile.getContent());
			}
		}
		return out.toString();
	}

	/**
	 * Return the {@linkplain Problem problems} that lead to this exception.
	 * @return the problems
	 * @since 7.0.3
	 */
	public List<Problem> getProblems() {
		return this.problems;
	}

	/**
	 * Return the {@linkplain Problem problems} of the given {@code kinds}.
	 * @param kinds the {@linkplain Diagnostic.Kind kinds} to filter on
	 * @return the problems with the given kinds, or an empty list
	 * @since 7.0.3
	 */
	public List<Problem> getProblems(Diagnostic.Kind... kinds) {
		List<Diagnostic.Kind> toMatch = Arrays.asList(kinds);
		return this.problems.stream().filter(problem -> toMatch.contains(problem.kind())).toList();
	}

	/**
	 * Description of a problem that lead to a compilation failure.
	 * <p>{@linkplain Diagnostic.Kind#ERROR errors} are the most important, but
	 * they might not be enough in case an error is triggered by the presence
	 * of a warning, see {@link Diagnostic.Kind#MANDATORY_WARNING}.
	 * @since 7.0.3
	 * @param kind the kind of problem
	 * @param message the description of the problem
	 */
	public record Problem(Diagnostic.Kind kind, String message) {

	}

}
