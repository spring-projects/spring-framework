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

package org.springframework.javapoet.support;

import java.io.IOException;
import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;

/**
 * A code snippet using tabs indentation that is fully processed by JavaPoet so
 * that imports are resolved.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class CodeSnippet {

	private static final String START_SNIPPET = "// start-snippet\n";

	private static final String END_SNIPPET = "// end-snippet";

	private final String fileContent;

	private final String snippet;


	CodeSnippet(String fileContent, String snippet) {
		this.fileContent = fileContent;
		this.snippet = snippet;
	}


	String getFileContent() {
		return this.fileContent;
	}

	/**
	 * Return the rendered code snippet.
	 * @return a code snippet where imports have been resolved
	 */
	public String getSnippet() {
		return this.snippet;
	}

	/**
	 * Specify if an import statement for the specified type is present.
	 * @param type the type to check
	 * @return true if this type has an import statement, false otherwise
	 */
	public boolean hasImport(Class<?> type) {
		return hasImport(type.getName());
	}

	/**
	 * Specify if an import statement for the specified class name is present.
	 * @param className the name of the class to check
	 * @return true if this type has an import statement, false otherwise
	 */
	public boolean hasImport(String className) {
		return getFileContent().lines().anyMatch(candidate ->
				candidate.equals(String.format("import %s;", className)));
	}

	/**
	 * Return a new {@link CodeSnippet} where the specified number of indentations
	 * have been removed.
	 * @param indent the number of indent to remove
	 * @return a CodeSnippet instance with the number of indentations removed
	 */
	public CodeSnippet removeIndent(int indent) {
		return new CodeSnippet(this.fileContent, this.snippet.lines().map(line ->
				removeIndent(line, indent)).collect(Collectors.joining("\n")));
	}

	/**
	 * Create a {@link CodeSnippet} using the specified code.
	 * @param code the code snippet
	 * @return a {@link CodeSnippet} instance
	 */
	public static CodeSnippet of(CodeBlock code) {
		return new Builder().build(code);
	}

	/**
	 * Process the specified code and return a fully-processed code snippet
	 * as a String.
	 * @param code a consumer to use to generate the code snippet
	 * @return a resolved code snippet
	 */
	public static String process(Consumer<CodeBlock.Builder> code) {
		CodeBlock.Builder body = CodeBlock.builder();
		code.accept(body);
		return process(body.build());
	}

	/**
	 * Process the specified {@link CodeBlock code} and return a
	 * fully-processed code snippet as a String.
	 * @param code the code snippet
	 * @return a resolved code snippet
	 */
	public static String process(CodeBlock code) {
		return of(code).getSnippet();
	}

	private String removeIndent(String line, int indent) {
		for (int i = 0; i < indent; i++) {
			if (line.startsWith("\t")) {
				line = line.substring(1);
			}
		}
		return line;
	}

	private static final class Builder {

		private static final String INDENT = "\t";

		private static final String SNIPPET_INDENT = INDENT + INDENT;

		public CodeSnippet build(CodeBlock code) {
			MethodSpec.Builder method = MethodSpec.methodBuilder("test")
					.addModifiers(Modifier.PUBLIC);
			CodeBlock.Builder body = CodeBlock.builder();
			body.add(START_SNIPPET);
			body.add(code);
			body.add(END_SNIPPET);
			method.addCode(body.build());
			String fileContent = write(createTestJavaFile(method.build()));
			String snippet = isolateGeneratedContent(fileContent);
			return new CodeSnippet(fileContent, snippet);
		}

		private String isolateGeneratedContent(String javaFile) {
			int start = javaFile.indexOf(START_SNIPPET);
			String tmp = javaFile.substring(start + START_SNIPPET.length());
			int end = tmp.indexOf(END_SNIPPET);
			tmp = tmp.substring(0, end);
			// Remove indent
			return tmp.lines().map(line -> {
				if (!line.startsWith(SNIPPET_INDENT)) {
					throw new IllegalStateException("Missing indent for " + line);
				}
				return line.substring(SNIPPET_INDENT.length());
			}).collect(Collectors.joining("\n"));
		}

		private JavaFile createTestJavaFile(MethodSpec method) {
			return JavaFile.builder("example", TypeSpec.classBuilder("Test")
					.addModifiers(Modifier.PUBLIC)
					.addMethod(method).build()).indent(INDENT).build();
		}

		private String write(JavaFile file) {
			try {
				StringWriter out = new StringWriter();
				file.writeTo(out);
				return out.toString();
			}
			catch (IOException ex) {
				throw new IllegalStateException("Failed to write " + file, ex);
			}
		}

	}
}
