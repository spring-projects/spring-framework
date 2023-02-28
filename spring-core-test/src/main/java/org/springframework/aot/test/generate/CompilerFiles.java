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
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.test.tools.ClassFile;
import org.springframework.core.test.tools.ResourceFile;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;

/**
 * Adapter class that can be used to apply AOT {@link GeneratedFiles} to the
 * {@link TestCompiler}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 6.0
 */
public final class CompilerFiles implements UnaryOperator<TestCompiler> {

	private final InMemoryGeneratedFiles generatedFiles;

	private CompilerFiles(InMemoryGeneratedFiles generatedFiles) {
		this.generatedFiles = generatedFiles;
	}

	public static UnaryOperator<TestCompiler> from(
			InMemoryGeneratedFiles generatedFiles) {
		return new CompilerFiles(generatedFiles);
	}

	@Override
	public TestCompiler apply(TestCompiler testCompiler) {
		return testCompiler
				.withSources(adapt(Kind.SOURCE, (path, inputStreamSource) ->
						SourceFile.of(inputStreamSource)))
				.withResources(adapt(Kind.RESOURCE, ResourceFile::of))
				.withClasses(adapt(Kind.CLASS, (path, inputStreamSource) ->
						ClassFile.of(ClassFile.toClassName(path), inputStreamSource)));
	}

	private <T> List<T> adapt(Kind kind,
			BiFunction<String, InputStreamSource, T> adapter) {
		List<T> result = new ArrayList<>();
		this.generatedFiles.getGeneratedFiles(kind)
				.forEach((k, v) -> result.add(adapter.apply(k, v)));
		return result;
	}

}
