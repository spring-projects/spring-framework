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

package org.springframework.aot.generate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.util.Assert;

/**
 * Default {@link GenerationContext} implementation.
 *
 * <p>Generated classes can be flushed out using {@link #writeGeneratedContent()}
 * which should be called only once after the generation process using this instance
 * has completed.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.0
 */
public class DefaultGenerationContext implements GenerationContext {

	private final Map<String, AtomicInteger> sequenceGenerator;

	private final GeneratedClasses generatedClasses;

	private final GeneratedFiles generatedFiles;

	private final RuntimeHints runtimeHints;


	/**
	 * Create a new {@link DefaultGenerationContext} instance backed by the
	 * specified {@link ClassNameGenerator} and {@link GeneratedFiles}.
	 * @param classNameGenerator the naming convention to use for generated
	 * class names
	 * @param generatedFiles the generated files
	 */
	public DefaultGenerationContext(ClassNameGenerator classNameGenerator, GeneratedFiles generatedFiles) {
		this(classNameGenerator, generatedFiles, new RuntimeHints());
	}

	/**
	 * Create a new {@link DefaultGenerationContext} instance backed by the
	 * specified {@link ClassNameGenerator}, {@link GeneratedFiles}, and
	 * {@link RuntimeHints}.
	 * @param classNameGenerator the naming convention to use for generated
	 * class names
	 * @param generatedFiles the generated files
	 * @param runtimeHints the runtime hints
	 */
	public DefaultGenerationContext(ClassNameGenerator classNameGenerator, GeneratedFiles generatedFiles,
			RuntimeHints runtimeHints) {
		this(new GeneratedClasses(classNameGenerator), generatedFiles, runtimeHints);
	}

	/**
	 * Create a new {@link DefaultGenerationContext} instance backed by the
	 * specified items.
	 * @param generatedClasses the generated classes
	 * @param generatedFiles the generated files
	 * @param runtimeHints the runtime hints
	 */
	DefaultGenerationContext(GeneratedClasses generatedClasses,
			GeneratedFiles generatedFiles, RuntimeHints runtimeHints) {

		Assert.notNull(generatedClasses, "'generatedClasses' must not be null");
		Assert.notNull(generatedFiles, "'generatedFiles' must not be null");
		Assert.notNull(runtimeHints, "'runtimeHints' must not be null");
		this.sequenceGenerator = new ConcurrentHashMap<>();
		this.generatedClasses = generatedClasses;
		this.generatedFiles = generatedFiles;
		this.runtimeHints = runtimeHints;
	}

	/**
	 * Create a new {@link DefaultGenerationContext} instance based on the
	 * supplied {@code existing} context and feature name.
	 * @param existing the existing context upon which to base the new one
	 * @param featureName the feature name to use
	 * @since 6.0.12
	 */
	protected DefaultGenerationContext(DefaultGenerationContext existing, String featureName) {
		int sequence = existing.sequenceGenerator.computeIfAbsent(featureName, key -> new AtomicInteger()).getAndIncrement();
		if (sequence > 0) {
			featureName += sequence;
		}
		this.sequenceGenerator = existing.sequenceGenerator;
		this.generatedClasses = existing.generatedClasses.withFeatureNamePrefix(featureName);
		this.generatedFiles = existing.generatedFiles;
		this.runtimeHints = existing.runtimeHints;
	}


	@Override
	public GeneratedClasses getGeneratedClasses() {
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

	@Override
	public DefaultGenerationContext withName(String name) {
		return new DefaultGenerationContext(this, name);
	}

	/**
	 * Write any generated content out to the generated files.
	 */
	public void writeGeneratedContent() {
		this.generatedClasses.writeTo(this.generatedFiles);
	}

}
