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

import javax.tools.Diagnostic;

import org.springframework.lang.Nullable;

/**
 * Exception thrown when code cannot compile.
 *
 * @author Phillip Webb
 * @author Yanming Zhou
 * @since 6.0
 */
@SuppressWarnings("serial")
public class CompilationException extends RuntimeException {

	@Nullable
	private final String diagnosticCode;

	CompilationException(@Nullable String diagnosticCode, String errors, SourceFiles sourceFiles, ResourceFiles resourceFiles) {
		super(buildMessage(errors, sourceFiles, resourceFiles));
		this.diagnosticCode = diagnosticCode;
	}

	CompilationException(String errors, SourceFiles sourceFiles, ResourceFiles resourceFiles) {
		this(null, errors, sourceFiles, resourceFiles);
	}

	/**
	 * Return the diagnostic code reported by compiler.
	 * @return diagnostic code reported by compiler, might be null.
	 * @since 6.1
	 * @see Diagnostic#getCode()
	 */
	@Nullable
	public String getDiagnosticCode() {
		return this.diagnosticCode;
	}

	private static String buildMessage(String errors, SourceFiles sourceFiles,
			ResourceFiles resourceFiles) {
		StringBuilder message = new StringBuilder();
		message.append("Unable to compile source\n\n");
		message.append(errors);
		message.append("\n\n");
		for (SourceFile sourceFile : sourceFiles) {
			message.append("---- source:   ").append(sourceFile.getPath()).append("\n\n");
			message.append(sourceFile.getContent());
			message.append("\n\n");
		}
		for (ResourceFile resourceFile : resourceFiles) {
			message.append("---- resource: ").append(resourceFile.getPath()).append("\n\n");
			message.append(resourceFile.getContent());
			message.append("\n\n");
		}
		return message.toString();
	}

}
