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

package org.springframework.aot.generate;

import java.io.IOException;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link GenerationContext}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
public class DefaultGenerationContext implements GenerationContext {

	private final ClassNameGenerator classNameGenerator;

	private final GeneratedClasses generatedClasses;

	private final GeneratedFiles generatedFiles;

	private final RuntimeHints runtimeHints;


	/**
	 * Create a new {@link DefaultGenerationContext} instance backed by the
	 * specified {@code generatedFiles}.
	 * @param generatedFiles the generated files
	 */
	public DefaultGenerationContext(GeneratedFiles generatedFiles) {
		this(new ClassNameGenerator(), generatedFiles, new RuntimeHints());
	}

	/**
	 * Create a new {@link DefaultGenerationContext} instance backed by the
	 * specified items.
	 * @param classNameGenerator the class name generator
	 * @param generatedFiles the generated files
	 * @param runtimeHints the runtime hints
	 */
	public DefaultGenerationContext(ClassNameGenerator classNameGenerator,
			GeneratedFiles generatedFiles, RuntimeHints runtimeHints) {
		Assert.notNull(classNameGenerator, "'classNameGenerator' must not be null");
		Assert.notNull(generatedFiles, "'generatedFiles' must not be null");
		Assert.notNull(runtimeHints, "'runtimeHints' must not be null");
		this.classNameGenerator = classNameGenerator;
		this.generatedClasses = new GeneratedClasses(classNameGenerator);
		this.generatedFiles = generatedFiles;
		this.runtimeHints = runtimeHints;
	}


	@Override
	public ClassNameGenerator getClassNameGenerator() {
		return this.classNameGenerator;
	}

	@Override
	public GeneratedClasses getClassGenerator() {
		return this.generatedClasses;
	}

	@Override
	public GeneratedFiles getGeneratedFiles() {
		return this.generatedFiles;
	}

	@Override
	public RuntimeHints getRuntimeHints() {
		return this.runtimeHints;
	}

	/**
	 * Write any generated content out to the generated files.
	 */
	public void writeGeneratedContent() {
		try {
			this.generatedClasses.writeTo(this.generatedFiles);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
