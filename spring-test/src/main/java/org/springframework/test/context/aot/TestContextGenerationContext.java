/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.NameGenerator;
import org.springframework.aot.hint.RuntimeHints;

/**
 * Extension of {@link DefaultGenerationContext} with a custom implementation of
 * {@link #withName(String)} that is specific to the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 6.0.12
 */
class TestContextGenerationContext extends DefaultGenerationContext {

	private final @Nullable String featureName;


	/**
	 * Create a new {@link TestContextGenerationContext} instance backed by the
	 * specified {@link NameGenerator}, {@link GeneratedFiles}, and
	 * {@link RuntimeHints}.
	 * @param nameGenerator the naming convention to use for generated classes
	 * and resources
	 * @param generatedFiles the generated files
	 * @param runtimeHints the runtime hints
	 */
	TestContextGenerationContext(
			NameGenerator nameGenerator, GeneratedFiles generatedFiles, RuntimeHints runtimeHints) {

		super(nameGenerator, generatedFiles, runtimeHints);
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
	 * name, the existing feature name will be prepended to the supplied feature name in
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
