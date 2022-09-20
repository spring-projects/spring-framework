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

package org.springframework.aot.test.generate;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.core.test.tools.ClassFile;
import org.springframework.core.test.tools.ResourceFile;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;

/**
 * {@link TestCompiler} utilities for generated files.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public abstract class GeneratedFilesTestCompilerUtils {

	/**
	 * Apply the specified {@link InMemoryGeneratedFiles} to the specified {@link TestCompiler}.
	 * @param testCompiler the compiler to configure
	 * @param generatedFiles the generated files to apply
	 * @return a new {@link TestCompiler} instance configured with the generated files
	 */
	public static TestCompiler configure(TestCompiler testCompiler, InMemoryGeneratedFiles generatedFiles) {
		List<SourceFile> sourceFiles = new ArrayList<>();
		generatedFiles.getGeneratedFiles(Kind.SOURCE).forEach(
				(path, inputStreamSource) -> sourceFiles.add(SourceFile.of(inputStreamSource)));
		List<ResourceFile> resourceFiles = new ArrayList<>();
		generatedFiles.getGeneratedFiles(Kind.RESOURCE).forEach(
				(path, inputStreamSource) -> resourceFiles.add(ResourceFile.of(path, inputStreamSource)));
		List<ClassFile> classFiles = new ArrayList<>();
		generatedFiles.getGeneratedFiles(Kind.CLASS).forEach(
				(path, inputStreamSource) -> classFiles.add(ClassFile.of(
						ClassFile.toClassName(path), inputStreamSource)));
		return testCompiler.withSources(sourceFiles).withResources(resourceFiles).withClasses(classFiles);
	}
}
