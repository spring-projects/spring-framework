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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.JdkProxyHint;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.test.generator.compile.CompileWithTargetClassAccess;
import org.springframework.aot.test.generator.compile.TestCompiler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.SynthesizedAnnotation;
import org.springframework.javapoet.ClassName;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterSharedConfigTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringTestNGTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringVintageTests;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.aot.samples.web.WebSpringJupiterTests;
import org.springframework.test.context.aot.samples.web.WebSpringTestNGTests;
import org.springframework.test.context.aot.samples.web.WebSpringVintageTests;
import org.springframework.test.context.aot.samples.xml.XmlSpringJupiterTests;
import org.springframework.test.context.aot.samples.xml.XmlSpringTestNGTests;
import org.springframework.test.context.aot.samples.xml.XmlSpringVintageTests;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.function.ThrowingConsumer;
import org.springframework.web.context.WebApplicationContext;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Tests for {@link TestContextAotGenerator}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
@CompileWithTargetClassAccess
class TestContextAotGeneratorTests extends AbstractAotTests {

	/**
	 * @see AotSmokeTests#scanClassPathThenGenerateSourceFilesAndCompileThem()
	 */
	@Test
	void processAheadOfTimeAndGenerateAotTestMappings() {
		Set<Class<?>> testClasses = Set.of(
				BasicSpringJupiterSharedConfigTests.class,
				BasicSpringJupiterTests.class,
				BasicSpringJupiterTests.NestedTests.class,
				BasicSpringTestNGTests.class,
				BasicSpringVintageTests.class);

		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);

		generator.processAheadOfTime(testClasses.stream().sorted(comparing(Class::getName)));

		assertRuntimeHints(generator.getRuntimeHints());

		List<String> sourceFiles = generatedFiles.getGeneratedFiles(Kind.SOURCE).keySet().stream().toList();
		assertThat(sourceFiles).containsExactlyInAnyOrder(expectedSourceFilesForBasicSpringTests);

		TestCompiler.forSystem().withFiles(generatedFiles).compile(ThrowingConsumer.of(compiled -> {
			AotTestMappings aotTestMappings = new AotTestMappings();
			for (Class<?> testClass : testClasses) {
				MergedContextConfiguration mergedConfig = generator.buildMergedContextConfiguration(testClass);
				ApplicationContextInitializer<ConfigurableApplicationContext> contextInitializer =
						aotTestMappings.getContextInitializer(testClass);
				assertThat(contextInitializer).isNotNull();
				ApplicationContext context = ((AotContextLoader) mergedConfig.getContextLoader())
						.loadContextForAotRuntime(mergedConfig, contextInitializer);
				assertContextForBasicTests(context);
			}
		}));
	}

	private static void assertRuntimeHints(RuntimeHints runtimeHints) {
		assertReflectionRegistered(runtimeHints, AotTestMappings.GENERATED_MAPPINGS_CLASS_NAME, INVOKE_PUBLIC_METHODS);

		Set.of(
			org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.class,
			org.springframework.test.context.support.DefaultBootstrapContext.class,
			org.springframework.test.context.support.DelegatingSmartContextLoader.class,
			org.springframework.test.context.web.WebDelegatingSmartContextLoader.class
		).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_PUBLIC_CONSTRUCTORS));

		Set.of(
			org.springframework.test.context.support.DefaultTestContextBootstrapper.class,
			org.springframework.test.context.web.WebTestContextBootstrapper.class,
			org.springframework.test.context.support.GenericGroovyXmlContextLoader.class,
			org.springframework.test.context.web.GenericGroovyXmlWebContextLoader.class
		).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

		Set.of(
			// Legacy and JUnit 4
			org.springframework.test.annotation.Commit.class,
			org.springframework.test.annotation.DirtiesContext.class,
			org.springframework.test.annotation.IfProfileValue.class,
			org.springframework.test.annotation.ProfileValueSourceConfiguration.class,
			org.springframework.test.annotation.Repeat.class,
			org.springframework.test.annotation.Rollback.class,
			org.springframework.test.annotation.Timed.class,

			// Core TestContext framework
			org.springframework.test.context.ActiveProfiles.class,
			org.springframework.test.context.BootstrapWith.class,
			org.springframework.test.context.ContextConfiguration.class,
			org.springframework.test.context.ContextHierarchy.class,
			org.springframework.test.context.DynamicPropertySource.class,
			org.springframework.test.context.NestedTestConfiguration.class,
			org.springframework.test.context.TestConstructor.class,
			org.springframework.test.context.TestExecutionListeners.class,
			org.springframework.test.context.TestPropertySource.class,
			org.springframework.test.context.TestPropertySources.class,

			// Application Events
			org.springframework.test.context.event.RecordApplicationEvents.class,

			// Test execution events
			org.springframework.test.context.event.annotation.AfterTestClass.class,
			org.springframework.test.context.event.annotation.AfterTestExecution.class,
			org.springframework.test.context.event.annotation.AfterTestMethod.class,
			org.springframework.test.context.event.annotation.BeforeTestClass.class,
			org.springframework.test.context.event.annotation.BeforeTestExecution.class,
			org.springframework.test.context.event.annotation.BeforeTestMethod.class,
			org.springframework.test.context.event.annotation.PrepareTestInstance.class,

			// JUnit Jupiter
			org.springframework.test.context.junit.jupiter.EnabledIf.class,
			org.springframework.test.context.junit.jupiter.DisabledIf.class,
			org.springframework.test.context.junit.jupiter.SpringJUnitConfig.class,
			org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig.class,

			// Web
			org.springframework.test.context.web.WebAppConfiguration.class
		).forEach(type -> assertAnnotationRegistered(runtimeHints, type));

		// TestExecutionListener
		Set.of(
			org.springframework.test.context.event.ApplicationEventsTestExecutionListener.class,
			org.springframework.test.context.event.EventPublishingTestExecutionListener.class,
			org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener.class,
			org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class,
			org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener.class,
			org.springframework.test.context.support.DirtiesContextTestExecutionListener.class,
			org.springframework.test.context.transaction.TransactionalTestExecutionListener.class,
			org.springframework.test.context.web.ServletTestExecutionListener.class
		).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

		// ContextCustomizerFactory
		Set.of(
			"org.springframework.test.context.support.DynamicPropertiesContextCustomizerFactory",
			"org.springframework.test.context.web.socket.MockServerContainerContextCustomizerFactory"
		).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));
	}

	private static void assertReflectionRegistered(RuntimeHints runtimeHints, String type, MemberCategory memberCategory) {
		assertThat(reflection().onType(TypeReference.of(type)).withMemberCategory(memberCategory))
			.as("Reflection hint for %s with category %s", type, memberCategory)
			.accepts(runtimeHints);
	}

	private static void assertReflectionRegistered(RuntimeHints runtimeHints, Class<?> type, MemberCategory memberCategory) {
		assertThat(reflection().onType(type).withMemberCategory(memberCategory))
			.as("Reflection hint for %s with category %s", type.getSimpleName(), memberCategory)
			.accepts(runtimeHints);
	}

	private static void assertAnnotationRegistered(RuntimeHints runtimeHints, Class<? extends Annotation> annotationType) {
		assertReflectionRegistered(runtimeHints, annotationType, INVOKE_DECLARED_METHODS);
		assertThat(runtimeHints.proxies().jdkProxies())
			.as("Proxy hint for annotation @%s", annotationType.getSimpleName())
			.anySatisfy(annotationProxy(annotationType));
	}

	private static Consumer<JdkProxyHint> annotationProxy(Class<? extends Annotation> type) {
		return jdkProxyHint -> assertThat(jdkProxyHint.getProxiedInterfaces())
				.containsExactly(TypeReference.of(type), TypeReference.of(SynthesizedAnnotation.class));
	}

	@Test
	void processAheadOfTimeWithBasicTests() {
		// We cannot parameterize with the test classes, since @CompileWithTargetClassAccess
		// cannot support @ParameterizedTest methods.
		Set<Class<?>> testClasses = Set.of(
				BasicSpringJupiterSharedConfigTests.class,
				BasicSpringJupiterTests.class,
				BasicSpringJupiterTests.NestedTests.class,
				BasicSpringTestNGTests.class,
				BasicSpringVintageTests.class);

		processAheadOfTime(testClasses, this::assertContextForBasicTests);
	}

	private void assertContextForBasicTests(ApplicationContext context) {
		assertThat(context.getEnvironment().getProperty("test.engine")).as("Environment").isNotNull();

		MessageService messageService = context.getBean(MessageService.class);
		assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
	}

	@Test
	void processAheadOfTimeWithXmlTests() {
		// We cannot parameterize with the test classes, since @CompileWithTargetClassAccess
		// cannot support @ParameterizedTest methods.
		Set<Class<?>> testClasses = Set.of(
				XmlSpringJupiterTests.class,
				XmlSpringTestNGTests.class,
				XmlSpringVintageTests.class);

		processAheadOfTime(testClasses, context -> {
			assertThat(context.getEnvironment().getProperty("test.engine"))
				.as("Environment").isNotNull();

			MessageService messageService = context.getBean(MessageService.class);
			assertThat(messageService.generateMessage()).isEqualTo("Hello, AOT!");
		});
	}

	@Test
	void processAheadOfTimeWithWebTests() {
		// We cannot parameterize with the test classes, since @CompileWithTargetClassAccess
		// cannot support @ParameterizedTest methods.
		Set<Class<?>> testClasses = Set.of(
				WebSpringJupiterTests.class,
				WebSpringTestNGTests.class,
				WebSpringVintageTests.class);

		processAheadOfTime(testClasses, context -> {
			assertThat(context.getEnvironment().getProperty("test.engine"))
				.as("Environment").isNotNull();

			MockMvc mockMvc = webAppContextSetup((WebApplicationContext) context).build();
			mockMvc.perform(get("/hello"))
				.andExpectAll(status().isOk(), content().string("Hello, AOT!"));
		});
	}


	@SuppressWarnings("unchecked")
	private void processAheadOfTime(Set<Class<?>> testClasses, ThrowingConsumer<ApplicationContext> result) {
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);
		List<Mapping> mappings = processAheadOfTime(generator, testClasses);
		TestCompiler.forSystem().withFiles(generatedFiles).compile(ThrowingConsumer.of(compiled -> {
			for (Mapping mapping : mappings) {
				MergedContextConfiguration mergedConfig = mapping.mergedConfig();
				ApplicationContextInitializer<ConfigurableApplicationContext> contextInitializer =
						compiled.getInstance(ApplicationContextInitializer.class, mapping.className().reflectionName());
				ApplicationContext context = ((AotContextLoader) mergedConfig.getContextLoader())
						.loadContextForAotRuntime(mergedConfig, contextInitializer);
				result.accept(context);
			}
		}));
	}

	private List<Mapping> processAheadOfTime(TestContextAotGenerator generator, Set<Class<?>> testClasses) {
		List<Mapping> mappings = new ArrayList<>();
		testClasses.forEach(testClass -> {
			DefaultGenerationContext generationContext = generator.createGenerationContext(testClass);
			MergedContextConfiguration mergedConfig = generator.buildMergedContextConfiguration(testClass);
			ClassName className = generator.processAheadOfTime(mergedConfig, generationContext);
			assertThat(className).isNotNull();
			mappings.add(new Mapping(mergedConfig, className));
			generationContext.writeGeneratedContent();
		});
		return mappings;
	}


	record Mapping(MergedContextConfiguration mergedConfig, ClassName className) {
	}

}
