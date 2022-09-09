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

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedClasses;
import org.springframework.aot.generate.GeneratedFiles;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.log.LogMessage;
import org.springframework.javapoet.ClassName;
import org.springframework.test.context.BootstrapUtils;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;

/**
 * {@code TestContextAotGenerator} generates AOT artifacts for integration tests
 * that depend on support from the <em>Spring TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see ApplicationContextAotGenerator
 */
public class TestContextAotGenerator {

	private static final Log logger = LogFactory.getLog(TestContextAotGenerator.class);

	private final ApplicationContextAotGenerator aotGenerator = new ApplicationContextAotGenerator();

	private final AotServices<TestRuntimeHintsRegistrar> testRuntimeHintsRegistrars;

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
		this.testRuntimeHintsRegistrars = AotServices.factories().load(TestRuntimeHintsRegistrar.class);
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
		MultiValueMap<MergedContextConfiguration, Class<?>> mergedConfigMappings = new LinkedMultiValueMap<>();
		testClasses.forEach(testClass -> mergedConfigMappings.add(buildMergedContextConfiguration(testClass), testClass));
		processAheadOfTime(mergedConfigMappings);
	}

	private void processAheadOfTime(MultiValueMap<MergedContextConfiguration, Class<?>> mergedConfigMappings) {
		MultiValueMap<ClassName, Class<?>> initializerClassMappings = new LinkedMultiValueMap<>();
		mergedConfigMappings.forEach((mergedConfig, testClasses) -> {
			logger.debug(LogMessage.format("Generating AOT artifacts for test classes %s",
					testClasses.stream().map(Class::getName).toList()));
			try {
				this.testRuntimeHintsRegistrars.forEach(registrar -> registrar.registerHints(mergedConfig,
						Collections.unmodifiableList(testClasses), this.runtimeHints, getClass().getClassLoader()));

				// Use first test class discovered for a given unique MergedContextConfiguration.
				Class<?> testClass = testClasses.get(0);
				DefaultGenerationContext generationContext = createGenerationContext(testClass);
				ClassName initializer = processAheadOfTime(mergedConfig, generationContext);
				Assert.state(!initializerClassMappings.containsKey(initializer),
						() -> "ClassName [%s] already encountered".formatted(initializer.reflectionName()));
				initializerClassMappings.addAll(initializer, testClasses);
				generationContext.writeGeneratedContent();
			}
			catch (Exception ex) {
				logger.warn(LogMessage.format("Failed to generate AOT artifacts for test classes %s",
						testClasses.stream().map(Class::getName).toList()), ex);
			}
		});

		generateTestAotMappings(initializerClassMappings);
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
					.formatted(mergedConfig.getTestClass().getName()), ex);
		}
	}

	/**
	 * Load the {@code GenericApplicationContext} for the supplied merged context
	 * configuration for AOT processing.
	 * <p>Only supports {@link SmartContextLoader SmartContextLoaders} that
	 * create {@link GenericApplicationContext GenericApplicationContexts}.
	 * @throws TestContextAotException if an error occurs while loading the application
	 * context or if one of the prerequisites is not met
	 * @see AotContextLoader#loadContextForAotProcessing(MergedContextConfiguration)
	 */
	private GenericApplicationContext loadContextForAotProcessing(
			MergedContextConfiguration mergedConfig) throws TestContextAotException {

		Class<?> testClass = mergedConfig.getTestClass();
		ContextLoader contextLoader = mergedConfig.getContextLoader();
		Assert.notNull(contextLoader, """
				Cannot load an ApplicationContext with a NULL 'contextLoader'. \
				Consider annotating test class [%s] with @ContextConfiguration or \
				@ContextHierarchy.""".formatted(testClass.getName()));

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
							.formatted(testClass.getName()), ex);
			}
		}
		throw new TestContextAotException("""
				Cannot generate AOT artifacts for test class [%s]. The configured \
				ContextLoader [%s] must be an AotContextLoader and must create a \
				GenericApplicationContext.""".formatted(testClass.getName(),
					contextLoader.getClass().getName()));
	}

	private MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass) {
		TestContextBootstrapper testContextBootstrapper =
				BootstrapUtils.resolveTestContextBootstrapper(testClass);
		// @BootstrapWith
		registerDeclaredConstructors(testContextBootstrapper.getClass());
		// @TestExecutionListeners
		testContextBootstrapper.getTestExecutionListeners().forEach(listener -> {
			registerDeclaredConstructors(listener.getClass());
			if (listener instanceof AotTestExecutionListener aotListener) {
				aotListener.processAheadOfTime(testClass, this.runtimeHints, getClass().getClassLoader());
			}
		});
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

	private void generateTestAotMappings(MultiValueMap<ClassName, Class<?>> initializerClassMappings) {
		ClassNameGenerator classNameGenerator = new ClassNameGenerator(TestAotMappings.class);
		DefaultGenerationContext generationContext =
				new DefaultGenerationContext(classNameGenerator, this.generatedFiles, this.runtimeHints);
		GeneratedClasses generatedClasses = generationContext.getGeneratedClasses();

		TestAotMappingsCodeGenerator codeGenerator =
				new TestAotMappingsCodeGenerator(initializerClassMappings, generatedClasses);
		generationContext.writeGeneratedContent();
		String className = codeGenerator.getGeneratedClass().getName().reflectionName();
		this.runtimeHints.reflection()
				.registerType(TypeReference.of(className), INVOKE_PUBLIC_METHODS);
	}

	private void registerDeclaredConstructors(Class<?> type) {
		ReflectionHints reflectionHints = this.runtimeHints.reflection();
		reflectionHints.registerType(type, INVOKE_DECLARED_CONSTRUCTORS);
	}

}
