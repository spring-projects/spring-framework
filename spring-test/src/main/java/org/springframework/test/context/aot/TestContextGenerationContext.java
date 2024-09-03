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

package org.springframework.test.context.aot;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.hint.RuntimeHints;

/**
 * Extension of {@link DefaultGenerationContext} with a custom implementation of
 * {@link #withName(String)} that is specific to the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 6.0.12
 */
class TestContextGenerationContext extends DefaultGenerationContext {

	private final String featureName;


	/**
	 * Create a new {@link TestContextGenerationContext} instance backed by the
	 * specified {@link ClassNameGenerator}, {@link GeneratedFiles}, and
	 * {@link RuntimeHints}.
	 * @param classNameGenerator the naming convention to use for generated class names
	 * @param generatedFiles the generated files
	 * @param runtimeHints the runtime hints
	 */
	TestContextGenerationContext(ClassNameGenerator classNameGenerator, GeneratedFiles generatedFiles,
			RuntimeHints runtimeHints) {
		super(classNameGenerator, generatedFiles, runtimeHints);
		this.featureName = null;
	}

	/**
	 * Create a new {@link TestContextGenerationContext} instance based on the
	 * supplied {@code existing} context and feature name.
	 * @param existing the existing context upon which to base the new one
	 * @param featureName the feature name to use
	 */
	private TestContextGenerationContext(TestContextGenerationContext existing, String featureName) {
		super(existing, featureName);
		this.featureName = featureName;
	}


	/**
	 * Create a new {@link TestContextGenerationContext} instance using the specified
	 * feature name to qualify generated assets for a dedicated round of code generation.
	 * <p>If <em>this</em> {@code TestContextGenerationContext} has a configured feature
	 * name, the existing feature name will prepended to the supplied feature name in
	 * order to avoid naming collisions.
	 * @param featureName the feature name to use
	 * @return a specialized {@link TestContextGenerationContext} for the specified
	 * feature name
	 */
	@Override
	public TestContextGenerationContext withName(String featureName) {
		if (this.featureName != null) {
			featureName = this.featureName + featureName;
		}
		return new TestContextGenerationContext(this, featureName);
	}

}
