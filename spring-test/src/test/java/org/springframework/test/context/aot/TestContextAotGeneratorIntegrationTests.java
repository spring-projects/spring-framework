/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.aot.AotDetector;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.test.generate.CompilerFiles;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.ClassName;
import org.springframework.test.context.BootstrapUtils;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterSharedConfigTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringTestNGTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringVintageTests;
import org.springframework.test.context.aot.samples.common.MessageService;
import org.springframework.test.context.aot.samples.jdbc.SqlScriptsSpringJupiterTests;
import org.springframework.test.context.aot.samples.web.WebSpringJupiterTests;
import org.springframework.test.context.aot.samples.web.WebSpringTestNGTests;
import org.springframework.test.context.aot.samples.web.WebSpringVintageTests;
import org.springframework.test.context.aot.samples.xml.XmlSpringJupiterTests;
import org.springframework.test.context.aot.samples.xml.XmlSpringTestNGTests;
import org.springframework.test.context.aot.samples.xml.XmlSpringVintageTests;
import org.springframework.test.context.env.YamlPropertySourceFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.function.ThrowingConsumer;
import org.springframework.web.context.WebApplicationContext;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static org.springframework.aot.hint.predicate.RuntimeHintsPredicates.resource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Tests for {@link TestContextAotGenerator}, {@link AotTestContextInitializers},
 * {@link AotTestAttributes}, {@link AotContextLoader}, and run-time hints.
 *
 * @author Sam Brannen
 * @since 6.0
 */
@CompileWithForkedClassLoader
class TestContextAotGeneratorIntegrationTests extends AbstractAotTests {

	/**
	 * End-to-end tests within the scope of the {@link TestContextAotGenerator}.
	 *
	 * @see AotIntegrationTests
	 */
	@Test
	void endToEndTests() {
		Set<Class<?>> testClasses = Set.of(
				BasicSpringJupiterSharedConfigTests.class,
				BasicSpringJupiterTests.class,
				BasicSpringJupiterTests.NestedTests.class,
				BasicSpringTestNGTests.class,
				BasicSpringVintageTests.class,
				SqlScriptsSpringJupiterTests.class,
				XmlSpringJupiterTests.class,
				WebSpringJupiterTests.class);

		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);

		generator.processAheadOfTime(testClasses.stream().sorted(comparing(Class::getName)));

		assertRuntimeHints(generator.getRuntimeHints());

		List<String> sourceFiles = generatedFiles.getGeneratedFiles(Kind.SOURCE).keySet().stream().toList();
		assertThat(sourceFiles).containsExactlyInAnyOrder(expectedSourceFiles);

		TestCompiler.forSystem().with(CompilerFiles.from(generatedFiles)).compile(ThrowingConsumer.of(compiled -> {
			try {
				System.setProperty(AotDetector.AOT_ENABLED, "true");
				AotTestAttributesFactory.reset();
				AotTestContextInitializersFactory.reset();

				AotTestAttributes aotAttributes = AotTestAttributes.getInstance();
				assertThatExceptionOfType(UnsupportedOperationException.class)
					.isThrownBy(() -> aotAttributes.setAttribute("foo", "bar"))
					.withMessage("AOT attributes cannot be modified during AOT run-time execution");
				String key = "@SpringBootConfiguration-" + BasicSpringVintageTests.class.getName();
				assertThat(aotAttributes.getString(key)).isEqualTo("org.example.Main");
				assertThat(aotAttributes.getBoolean(key + "-active1")).isTrue();
				assertThat(aotAttributes.getBoolean(key + "-active2")).isTrue();
				assertThat(aotAttributes.getString("bogus")).isNull();
				assertThat(aotAttributes.getBoolean("bogus")).isFalse();

				AotTestContextInitializers aotContextInitializers = new AotTestContextInitializers();
				for (Class<?> testClass : testClasses) {
					MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);
					ApplicationContextInitializer<ConfigurableApplicationContext> contextInitializer =
							aotContextInitializers.getContextInitializer(testClass);
					assertThat(contextInitializer).isNotNull();
					ApplicationContext context = ((AotContextLoader) mergedConfig.getContextLoader())
							.loadContextForAotRuntime(mergedConfig, contextInitializer);
					if (context instanceof WebApplicationContext wac) {
						assertContextForWebTests(wac);
					}
					else if (testClass.getPackageName().contains("jdbc")) {
						assertContextForJdbcTests(context);
					}
					else {
						assertContextForBasicTests(context);
					}
				}
			}
			finally {
				System.clearProperty(AotDetector.AOT_ENABLED);
				AotTestAttributesFactory.reset();
			}
		}));
	}

	private static void assertRuntimeHints(RuntimeHints runtimeHints) {
		assertReflectionRegistered(runtimeHints, AotTestContextInitializersCodeGenerator.GENERATED_MAPPINGS_CLASS_NAME, INVOKE_PUBLIC_METHODS);
		assertReflectionRegistered(runtimeHints, AotTestAttributesCodeGenerator.GENERATED_ATTRIBUTES_CLASS_NAME, INVOKE_PUBLIC_METHODS);

		Stream.of(
			"org.opentest4j.TestAbortedException",
			"org.junit.AssumptionViolatedException",
			"org.testng.SkipException"
		).forEach(type -> assertReflectionRegistered(runtimeHints, type));

		Stream.of(
			org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate.class,
			org.springframework.test.context.support.DefaultBootstrapContext.class
		).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_PUBLIC_CONSTRUCTORS));

		Stream.of(
			org.springframework.test.context.support.DefaultTestContextBootstrapper.class,
			org.springframework.test.context.support.DelegatingSmartContextLoader.class,
			org.springframework.test.context.support.GenericGroovyXmlContextLoader.class,
			org.springframework.test.context.web.GenericGroovyXmlWebContextLoader.class,
			org.springframework.test.context.web.WebDelegatingSmartContextLoader.class,
			org.springframework.test.context.web.WebTestContextBootstrapper.class
		).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

		Stream.of(
			org.springframework.test.context.web.WebAppConfiguration.class
		).forEach(type -> assertAnnotationRegistered(runtimeHints, type));

		// TestExecutionListener
		Stream.of(
			// @TestExecutionListeners
			org.springframework.test.context.aot.samples.basic.BasicSpringJupiterTests.DummyTestExecutionListener.class,
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
		Stream.of(
			"org.springframework.test.context.support.DynamicPropertiesContextCustomizerFactory",
			"org.springframework.test.context.web.socket.MockServerContainerContextCustomizerFactory",
			"org.springframework.test.context.aot.samples.basic.ImportsContextCustomizerFactory"
		).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

		Stream.of(
			// @BootstrapWith
			org.springframework.test.context.aot.samples.basic.BasicSpringVintageTests.CustomXmlBootstrapper.class,
			// @ContextConfiguration(initializers = ...)
			org.springframework.test.context.aot.samples.basic.BasicSpringTestNGTests.CustomInitializer.class,
			// @ContextConfiguration(loader = ...)
			org.springframework.test.context.support.AnnotationConfigContextLoader.class,
			// @ActiveProfiles(resolver = ...)
			org.springframework.test.context.aot.samples.basic.SpanishActiveProfilesResolver.class
		).forEach(type -> assertReflectionRegistered(runtimeHints, type, INVOKE_DECLARED_CONSTRUCTORS));

		// @ContextConfiguration(locations = ...)
		assertThat(resource().forResource("org/springframework/test/context/aot/samples/xml/test-config.xml"))
			.accepts(runtimeHints);

		// @TestPropertySource(locations = ...)
		assertThat(resource().forResource("org/springframework/test/context/aot/samples/basic/BasicSpringVintageTests.properties"))
			.as("@TestPropertySource(locations)")
			.accepts(runtimeHints);

		// @YamlTestProperties(...)
		assertThat(resource().forResource("org/springframework/test/context/aot/samples/basic/test1.yaml"))
			.as("@YamlTestProperties: test1.yaml")
			.accepts(runtimeHints);
		assertThat(resource().forResource("org/springframework/test/context/aot/samples/basic/test2.yaml"))
			.as("@YamlTestProperties: test2.yaml")
			.accepts(runtimeHints);

		// @TestPropertySource(factory = ...)
		assertReflectionRegistered(runtimeHints, YamlPropertySourceFactory.class.getName(), INVOKE_DECLARED_CONSTRUCTORS);

		// @WebAppConfiguration(value = ...)
		assertThat(resource().forResource("META-INF/web-resources/resources/Spring.js")).accepts(runtimeHints);
		assertThat(resource().forResource("META-INF/web-resources/WEB-INF/views/home.jsp")).accepts(runtimeHints);

		// @Sql(scripts = ...)
		assertThat(resource().forResource("org/springframework/test/context/jdbc/schema.sql"))
			.accepts(runtimeHints);
		assertThat(resource().forResource("org/springframework/test/context/aot/samples/jdbc/SqlScriptsSpringJupiterTests.test.sql"))
			.accepts(runtimeHints);
	}

	private static void assertReflectionRegistered(RuntimeHints runtimeHints, String type) {
		assertThat(reflection().onType(TypeReference.of(type)))
			.as("Reflection hint for %s", type)
			.accepts(runtimeHints);
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
		ConfigurableApplicationContext cac = (ConfigurableApplicationContext) context;
		String expectedMessage = cac.getEnvironment().matchesProfiles("spanish") ?
				"¡Hola, AOT!" : "Hello, AOT!";
		assertThat(messageService.generateMessage()).isEqualTo(expectedMessage);
	}

	private void assertContextForJdbcTests(ApplicationContext context) {
		assertThat(context.getEnvironment().getProperty("test.engine")).as("Environment").isNotNull();
		assertThat(context.getBean(DataSource.class)).as("DataSource").isNotNull();
	}

	private void assertContextForWebTests(WebApplicationContext wac) throws Exception {
		assertThat(wac.getEnvironment().getProperty("test.engine")).as("Environment").isNotNull();

		MockMvc mockMvc = webAppContextSetup(wac).build();
		mockMvc.perform(get("/hello")).andExpectAll(status().isOk(), content().string("Hello, AOT!"));
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
		TestCompiler.forSystem().with(CompilerFiles.from(generatedFiles)).compile(ThrowingConsumer.of(compiled -> {
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
			MergedContextConfiguration mergedConfig = buildMergedContextConfiguration(testClass);
			ClassName className = generator.processAheadOfTime(mergedConfig, generationContext);
			assertThat(className).isNotNull();
			mappings.add(new Mapping(mergedConfig, className));
			generationContext.writeGeneratedContent();
		});
		return mappings;
	}


	private static MergedContextConfiguration buildMergedContextConfiguration(Class<?> testClass) {
		TestContextBootstrapper testContextBootstrapper = BootstrapUtils.resolveTestContextBootstrapper(testClass);
		return testContextBootstrapper.buildMergedContextConfiguration();
	}

	record Mapping(MergedContextConfiguration mergedConfig, ClassName className) {
	}

	private static final String[] expectedSourceFiles = {
			// Global
			"org/springframework/test/context/aot/AotTestContextInitializers__Generated.java",
			"org/springframework/test/context/aot/AotTestAttributes__Generated.java",
			// BasicSpringJupiterSharedConfigTests
			"org/springframework/context/event/DefaultEventListenerFactory__TestContext001_BeanDefinitions.java",
			"org/springframework/context/event/EventListenerMethodProcessor__TestContext001_BeanDefinitions.java",
			"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterSharedConfigTests__TestContext001_ApplicationContextInitializer.java",
			"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterSharedConfigTests__TestContext001_BeanFactoryRegistrations.java",
			"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterSharedConfigTests__TestContext001_ManagementApplicationContextInitializer.java",
			"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterSharedConfigTests__TestContext001_ManagementBeanFactoryRegistrations.java",
			"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext001_BeanDefinitions.java",
			"org/springframework/test/context/aot/samples/management/ManagementConfiguration__TestContext001_BeanDefinitions.java",
			"org/springframework/test/context/aot/samples/management/ManagementMessageService__TestContext001_ManagementBeanDefinitions.java",
			"org/springframework/test/context/support/DynamicPropertySourceBeanInitializer__TestContext001_BeanDefinitions.java",
			// BasicSpringJupiterTests -- not generated b/c already generated for BasicSpringJupiterSharedConfigTests.
			// BasicSpringJupiterTests.NestedTests
			"org/springframework/context/event/DefaultEventListenerFactory__TestContext002_BeanDefinitions.java",
			"org/springframework/context/event/EventListenerMethodProcessor__TestContext002_BeanDefinitions.java",
			"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterTests_NestedTests__TestContext002_ApplicationContextInitializer.java",
			"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterTests_NestedTests__TestContext002_BeanFactoryRegistrations.java",
			"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterTests_NestedTests__TestContext002_ManagementApplicationContextInitializer.java",
			"org/springframework/test/context/aot/samples/basic/BasicSpringJupiterTests_NestedTests__TestContext002_ManagementBeanFactoryRegistrations.java",
			"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext002_BeanDefinitions.java",
			"org/springframework/test/context/aot/samples/management/ManagementConfiguration__TestContext002_BeanDefinitions.java",
			"org/springframework/test/context/aot/samples/management/ManagementMessageService__TestContext002_ManagementBeanDefinitions.java",
			"org/springframework/test/context/support/DynamicPropertySourceBeanInitializer__TestContext002_BeanDefinitions.java",
			// BasicSpringTestNGTests
			"org/springframework/context/event/DefaultEventListenerFactory__TestContext003_BeanDefinitions.java",
			"org/springframework/context/event/EventListenerMethodProcessor__TestContext003_BeanDefinitions.java",
			"org/springframework/test/context/aot/samples/basic/BasicSpringTestNGTests__TestContext003_ApplicationContextInitializer.java",
			"org/springframework/test/context/aot/samples/basic/BasicSpringTestNGTests__TestContext003_BeanFactoryRegistrations.java",
			"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext003_BeanDefinitions.java",
			"org/springframework/test/context/support/DynamicPropertySourceBeanInitializer__TestContext003_BeanDefinitions.java",
			// BasicSpringVintageTests
			"org/springframework/context/event/DefaultEventListenerFactory__TestContext004_BeanDefinitions.java",
			"org/springframework/context/event/EventListenerMethodProcessor__TestContext004_BeanDefinitions.java",
			"org/springframework/test/context/aot/samples/basic/BasicSpringVintageTests__TestContext004_ApplicationContextInitializer.java",
			"org/springframework/test/context/aot/samples/basic/BasicSpringVintageTests__TestContext004_BeanFactoryRegistrations.java",
			"org/springframework/test/context/aot/samples/basic/BasicTestConfiguration__TestContext004_BeanDefinitions.java",
			"org/springframework/test/context/support/DynamicPropertySourceBeanInitializer__TestContext004_BeanDefinitions.java",
			// SqlScriptsSpringJupiterTests
			"org/springframework/context/event/DefaultEventListenerFactory__TestContext005_BeanDefinitions.java",
			"org/springframework/context/event/EventListenerMethodProcessor__TestContext005_BeanDefinitions.java",
			"org/springframework/test/context/aot/samples/jdbc/SqlScriptsSpringJupiterTests__TestContext005_ApplicationContextInitializer.java",
			"org/springframework/test/context/aot/samples/jdbc/SqlScriptsSpringJupiterTests__TestContext005_BeanFactoryRegistrations.java",
			"org/springframework/test/context/jdbc/EmptyDatabaseConfig__TestContext005_BeanDefinitions.java",
			"org/springframework/test/context/support/DynamicPropertySourceBeanInitializer__TestContext005_BeanDefinitions.java",
			// WebSpringJupiterTests
			"org/springframework/context/event/DefaultEventListenerFactory__TestContext006_BeanDefinitions.java",
			"org/springframework/context/event/EventListenerMethodProcessor__TestContext006_BeanDefinitions.java",
			"org/springframework/test/context/aot/samples/web/WebSpringJupiterTests__TestContext006_ApplicationContextInitializer.java",
			"org/springframework/test/context/aot/samples/web/WebSpringJupiterTests__TestContext006_BeanFactoryRegistrations.java",
			"org/springframework/test/context/aot/samples/web/WebTestConfiguration__TestContext006_BeanDefinitions.java",
			"org/springframework/web/servlet/config/annotation/DelegatingWebMvcConfiguration__TestContext006_Autowiring.java",
			"org/springframework/web/servlet/config/annotation/DelegatingWebMvcConfiguration__TestContext006_BeanDefinitions.java",
			"org/springframework/test/context/support/DynamicPropertySourceBeanInitializer__TestContext006_BeanDefinitions.java",
			// XmlSpringJupiterTests
			"org/springframework/context/event/DefaultEventListenerFactory__TestContext007_BeanDefinitions.java",
			"org/springframework/context/event/EventListenerMethodProcessor__TestContext007_BeanDefinitions.java",
			"org/springframework/test/context/aot/samples/common/DefaultMessageService__TestContext007_BeanDefinitions.java",
			"org/springframework/test/context/aot/samples/xml/XmlSpringJupiterTests__TestContext007_ApplicationContextInitializer.java",
			"org/springframework/test/context/aot/samples/xml/XmlSpringJupiterTests__TestContext007_BeanFactoryRegistrations.java",
			"org/springframework/test/context/support/DynamicPropertySourceBeanInitializer__TestContext007_BeanDefinitions.java",
		};

}
