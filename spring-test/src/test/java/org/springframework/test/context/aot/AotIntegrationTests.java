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


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.launcher.listeners.TestExecutionSummary.Failure;
import org.opentest4j.MultipleFailuresError;

import org.springframework.aot.AotDetector;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterSharedConfigTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringJupiterTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringTestNGTests;
import org.springframework.test.context.aot.samples.basic.BasicSpringVintageTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.TagFilter.excludeTags;

/**
 * End-to-end integration tests for AOT support in the TestContext framework.
 *
 * @author Sam Brannen
 * @since 6.0
 */
@CompileWithForkedClassLoader
class AotIntegrationTests extends AbstractAotTests {

	private static final String CLASSPATH_ROOT = "AotSmokeTests.classpath_root";

	// We have to determine the classpath root and store it in a system property
	// since @CompileWithTargetClassAccess uses a custom ClassLoader that does
	// not support CodeSource.
	//
	// The system property will only be set when this class is loaded by the
	// original ClassLoader used to launch the JUnit Platform. The attempt to
	// access the CodeSource will fail when the tests are executed in the
	// nested JUnit Platform launched by the CompileWithTargetClassAccessExtension.
	static {
		try {
			Path classpathRoot = Paths.get(AotIntegrationTests.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			System.setProperty(CLASSPATH_ROOT, classpathRoot.toFile().getCanonicalPath());
		}
		catch (Exception ex) {
			// ignore
		}
	}


	@Test
	void endToEndTests() {
		// AOT BUILD-TIME: CLASSPATH SCANNING
		Stream<Class<?>> testClasses = createTestClassScanner().scan("org.springframework.test.context.aot.samples.basic");

		// AOT BUILD-TIME: PROCESSING
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);
		generator.processAheadOfTime(testClasses);

		List<String> sourceFiles = generatedFiles.getGeneratedFiles(Kind.SOURCE).keySet().stream().toList();
		assertThat(sourceFiles).containsExactlyInAnyOrder(expectedSourceFilesForBasicSpringTests);

		// AOT BUILD-TIME: COMPILATION
		TestCompiler.forSystem().withFiles(generatedFiles)
			// .printFiles(System.out)
			.compile(compiled ->
				// AOT RUN-TIME: EXECUTION
				runTestsInAotMode(5, List.of(
					BasicSpringJupiterSharedConfigTests.class,
					BasicSpringJupiterTests.class, // NestedTests get executed automatically
					BasicSpringTestNGTests.class,
					BasicSpringVintageTests.class)));
	}

	@Disabled("Uncomment to run all Spring integration tests in `spring-test`")
	@Test
	void endToEndTestsForEntireSpringTestModule() {
		// AOT BUILD-TIME: CLASSPATH SCANNING
		List<Class<?>> testClasses =
				// FYI: you can limit execution to a particular set of test classes as follows.
				// List.of(DirtiesContextTransactionalTestNGSpringContextTests.class, ...);
				createTestClassScanner()
				.scan()
				// FYI: you can limit execution to a particular package and its subpackages as follows.
				// .scan("org.springframework.test.context.junit.jupiter")
				.toList();


		// AOT BUILD-TIME: PROCESSING
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);
		generator.processAheadOfTime(testClasses.stream());

		// AOT BUILD-TIME: COMPILATION
		TestCompiler.forSystem().withFiles(generatedFiles)
			// .printFiles(System.out)
			.compile(compiled ->
				// AOT RUN-TIME: EXECUTION
				runTestsInAotMode(testClasses));
	}

	private static void runTestsInAotMode(List<Class<?>> testClasses) {
		runTestsInAotMode(-1, testClasses);
	}

	private static void runTestsInAotMode(long expectedNumTests, List<Class<?>> testClasses) {
		try {
			System.setProperty(AotDetector.AOT_ENABLED, "true");

			LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request()
					.filters(ClassNameFilter.includeClassNamePatterns(".*Tests?$"))
					.filters(excludeTags("failing-test-case"));
			testClasses.forEach(testClass -> builder.selectors(selectClass(testClass)));
			LauncherDiscoveryRequest request = builder.build();
			SummaryGeneratingListener listener = new SummaryGeneratingListener();
			LauncherFactory.create().execute(request, listener);
			TestExecutionSummary summary = listener.getSummary();
			if (summary.getTotalFailureCount() > 0) {
				List<Throwable> exceptions = summary.getFailures().stream().map(Failure::getException).toList();
				throw new MultipleFailuresError("Test execution failures", exceptions);
			}
			if (expectedNumTests >= 0) {
				assertThat(summary.getTestsSucceededCount()).isEqualTo(expectedNumTests);
			}
		}
		finally {
			System.clearProperty(AotDetector.AOT_ENABLED);
		}
	}

	private static TestClassScanner createTestClassScanner() {
		String classpathRoot = System.getProperty(CLASSPATH_ROOT);
		assertThat(classpathRoot).as(CLASSPATH_ROOT).isNotNull();
		Set<Path> classpathRoots = Set.of(Paths.get(classpathRoot));
		return new TestClassScanner(classpathRoots);
	}

}
