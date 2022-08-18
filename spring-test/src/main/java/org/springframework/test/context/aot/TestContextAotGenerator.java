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

package org.springframework.test.context.aot;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.test.context.BootstrapUtils;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@code TestContextAotGenerator} generates AOT artifacts for integration tests
 * that depend on support from the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see ApplicationContextAotGenerator
 */
class TestContextAotGenerator {

	private static final Log logger = LogFactory.getLog(TestClassScanner.class);

	private final ApplicationContextAotGenerator aotGenerator = new ApplicationContextAotGenerator();

	private final AtomicInteger sequence = new AtomicInteger();

	private final GeneratedFiles generatedFiles;

	private final RuntimeHints runtimeHints;


	/**
	 * Create a new {@link TestContextAotGenerator} that uses the supplied
	 * {@link GeneratedFiles}.
	 * @param generatedFiles the {@code GeneratedFiles} to use
	 */
	public TestContextAotGenerator(GeneratedFiles generatedFiles) {
		this(generatedFiles, new RuntimeHints());
	}

	/**
	 * Create a new {@link TestContextAotGenerator} that uses the supplied
	 * {@link GeneratedFiles} and {@link RuntimeHints}.
	 * @param generatedFiles the {@code GeneratedFiles} to use
	 * @param runtimeHints the {@code RuntimeHints} to use
	 */
	public TestContextAotGenerator(GeneratedFiles generatedFiles, RuntimeHints runtimeHints) {
		this.generatedFiles = generatedFiles;
		this.runtimeHints = runtimeHints;
	}


	/**
	 * Get the {@link RuntimeHints} gathered during {@linkplain #processAheadOfTime(Stream)
	 * AOT processing}.
	 */
	public final RuntimeHints getRuntimeHints() {
		return this.runtimeHints;
	}

	/**
	 * Process each of the supplied Spring integration test classes and generate
	 * AOT artifacts.
	 * @throws TestContextAotException if an error occurs during AOT processing
	 */
	public void processAheadOfTime(Stream<Class<?>> testClasses) throws TestContextAotException {
		MultiValueMap<MergedContextConfiguration, Class<?>> map = new LinkedMultiValueMap<>();
		testClasses.forEach(testClass -> map.add(buildMergedContextConfiguration(testClass), testClass));

		map.forEach((mergedConfig, classes) -> {
			// System.err.println(mergedConfig + " -> " + classes);
			if (logger.isDebugEnabled()) {
				logger.debug("Generating AOT artifacts for test classes [%s]"
						.formatted(classes.stream().map(Class::getCanonicalName).toList()));
			}
			try {
				// Use first test class discovered for a given unique MergedContextConfiguration.
				Class<?> testClass = classes.get(0);
				DefaultGenerationContext generationContext = createGenerationContext(testClass);
				ClassName className = processAheadOfTime(mergedConfig, generationContext);
				// TODO Store ClassName in a map analogous to TestContextAotProcessor in Spring Native.
				generationContext.writeGeneratedContent();
			}
			catch (Exception ex) {
				if (logger.isWarnEnabled()) {
					logger.warn("Failed to generate AOT artifacts for test classes [%s]"
							.formatted(classes.stream().map(Class::getCanonicalName).toList()), ex);
				}
			}
		});
	}

	/**
	 * Process the specified {@link MergedContextConfiguration} ahead-of-time
	 * using the specified {@link GenerationContext}.
	 * <p>Return the {@link ClassName} of the {@link ApplicationContextInitializer}
	 * to use to restore an optimized state of the test application context for
	 * the given {@code MergedContextConfiguration}.
	 * @param mergedConfig the {@code MergedContextConfiguration} to process
	 * @param generationContext the generation context to use
	 * @return the {@link ClassName} for the generated {@code ApplicationContextInitializer}
	 * @throws TestContextAotException if an error occurs during AOT processing
	 */
	ClassName processAheadOfTime(MergedContextConfiguration mergedConfig,
			GenerationContext generationContext) throws TestContextAotException {

		GenericApplicationContext gac = loadContextForAotProcessing(mergedConfig);
		try {
			return this.aotGenerator.processAheadOfTime(gac, generationContext);
		}
		catch (Throwable ex) {
			throw new TestContextAotException("Failed to process test class [%s] for AOT"
					.formatted(mergedConfig.getTestClass().getCanonicalName()), ex);
		}
	}

	/**
	 * Load the {@code GenericApplicationContext} for the supplied merged context
	 * configuration for AOT processing.
	 * <p>Only supports {@link SmartContextLoader SmartContextLoaders} that
	 * create {@link GenericApplicationContext GenericApplicationContexts}.
	 * @throws TestContextAotException if an error occurs while loading the application
	 * context or if one of the prerequisites is not met
	 * @see SmartContextLoader#loadContextForAotProcessing(MergedContextConfiguration)
	 */
	private GenericApplicationContext loadContextForAotProcessing(
			MergedContextConfiguration mergedConfig) throws TestContextAotException {

		Class<?> testClass = mergedConfig.getTestClass();
		ContextLoader contextLoader = mergedConfig.getContextLoader();
		Assert.notNull(contextLoader, """
				Cannot load an ApplicationContext with a NULL 'contextLoader'. \
				Consider annotating test class [%s] with @ContextConfiguration or \
				@ContextHierarchy.""".formatted(testClass.getCanonicalName()));

		if (contextLoader instanceof AotContextLoader aotContextLoader) {
			try {
				ApplicationContext context = aotContextLoader.loadContextForAotProcessing(mergedConfig);
				if (context instanceof GenericApplicationContext gac) {
					return gac;
				}
			}
			catch (Exception ex) {
				throw new TestContextAotException(
						"Failed to load ApplicationContext for AOT processing for test class [%s]"
							.formatted(testClass.getCanonicalName()), ex);
			}
		}
		throw new TestContextAotException("""
				Cannot generate AOT artifacts for test class [%s]. The configured \
				ContextLoader [%s] must be an AotContextLoader and must create a \
				GenericApplicationContext.""".formatted(testClass.getCanonicalName(),
					contextLoader.getClass().getName()));
	}

	MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass) {
		TestContextBootstrapper testContextBootstrapper =
				BootstrapUtils.resolveTestContextBootstrapper(testClass);
		return testContextBootstrapper.buildMergedContextConfiguration();
	}

	DefaultGenerationContext createGenerationContext(Class<?> testClass) {
		ClassNameGenerator classNameGenerator = new ClassNameGenerator(testClass);
		DefaultGenerationContext generationContext =
				new DefaultGenerationContext(classNameGenerator, this.generatedFiles, this.runtimeHints);
		return generationContext.withName(nextTestContextId());
	}

	private String nextTestContextId() {
		return "TestContext%03d_".formatted(this.sequence.incrementAndGet());
	}

}
