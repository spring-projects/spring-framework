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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;
import static org.junit.platform.engine.discovery.PackageNameFilter.includePackageNames;
import static org.springframework.core.annotation.MergedAnnotation.VALUE;
import static org.springframework.core.annotation.MergedAnnotations.SearchStrategy.INHERITED_ANNOTATIONS;
import static org.springframework.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY;

/**
 * {@code TestClassScanner} scans provided classpath roots for Spring integration
 * test classes using the JUnit Platform {@link Launcher} API which allows all
 * registered {@link org.junit.platform.engine.TestEngine TestEngines} to discover
 * tests according to their own rules.
 *
 * <p>The scanner currently detects the following categories of Spring integration
 * test classes.
 *
 * <ul>
 * <li>JUnit Jupiter: classes that register the {@code SpringExtension} via
 * {@code @ExtendWith}.</li>
 * <li>JUnit 4: classes that register the {@code SpringJUnit4ClassRunner} or
 * {@code SpringRunner} via {@code @RunWith}.</li>
 * <li>Generic: classes that are annotated with {@code @ContextConfiguration} or
 * {@code @BootstrapWith}.</li>
 * </ul>
 *
 * <p>The scanner has been tested with the following
 * {@link org.junit.platform.engine.TestEngine TestEngines}.
 *
 * <ul>
 * <li>JUnit Jupiter</li>
 * <li>JUnit Vintage</li>
 * <li>JUnit Platform Suite Engine</li>
 * <li>TestNG Engine for the JUnit Platform</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 6.0
 */
class TestClassScanner {

	// JUnit Jupiter
	private static final String EXTEND_WITH_ANNOTATION_NAME = "org.junit.jupiter.api.extension.ExtendWith";
	private static final String SPRING_EXTENSION_NAME = "org.springframework.test.context.junit.jupiter.SpringExtension";

	// JUnit 4
	private static final String RUN_WITH_ANNOTATION_NAME = "org.junit.runner.RunWith";
	private static final String SPRING_JUNIT4_CLASS_RUNNER_NAME = "org.springframework.test.context.junit4.SpringJUnit4ClassRunner";
	private static final String SPRING_RUNNER_NAME = "org.springframework.test.context.junit4.SpringRunner";


	private final Log logger = LogFactory.getLog(TestClassScanner.class);

	private final Set<Path> classpathRoots;


	/**
	 * Create a {@code TestClassScanner} for the given classpath roots.
	 * <p>For example, in a Gradle project that only supports Java-based tests,
	 * the supplied set would contain a single {@link Path} representing the
	 * absolute path to the project's {@code build/classes/java/test} folder.
	 * @param classpathRoots the classpath roots to scan
	 */
	TestClassScanner(Set<Path> classpathRoots) {
		this.classpathRoots = assertPreconditions(classpathRoots);
	}


	/**
	 * Scan the configured classpath roots for Spring integration test classes.
	 */
	Stream<Class<?>> scan() {
		return scan(new String[0]);
	}

	/**
	 * Scan the configured classpath roots for Spring integration test classes
	 * in the given packages.
	 * <p>This method is currently only intended to be used within our own test
	 * suite to validate the behavior of this scanner with a limited scope. In
	 * production scenarios one should invoke {@link #scan()} to scan all packages
	 * in the configured classpath roots.
	 */
	Stream<Class<?>> scan(String... packageNames) {
		Assert.noNullElements(packageNames, "'packageNames' must not contain null elements");

		if (logger.isInfoEnabled()) {
			if (packageNames.length > 0) {
				logger.info("Scanning for Spring test classes in packages %s in classpath roots %s"
						.formatted(Arrays.toString(packageNames), this.classpathRoots));
			}
			else {
				logger.info("Scanning for Spring test classes in all packages in classpath roots %s"
						.formatted(this.classpathRoots));
			}
		}

		LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
		builder.selectors(selectClasspathRoots(this.classpathRoots));
		if (packageNames.length > 0) {
			builder.filters(includePackageNames(packageNames));
		}
		LauncherDiscoveryRequest request = builder.build();
		Launcher launcher = LauncherFactory.create();
		TestPlan testPlan = launcher.discover(request);

		return testPlan.getRoots().stream()
				.map(testPlan::getDescendants)
				.flatMap(Set::stream)
				.map(TestIdentifier::getSource)
				.flatMap(Optional::stream)
				.filter(ClassSource.class::isInstance)
				.map(ClassSource.class::cast)
				.map(this::getJavaClass)
				.flatMap(Optional::stream)
				.filter(this::isSpringTestClass)
				.distinct()
				.sorted(Comparator.comparing(Class::getName));
	}

	private Optional<Class<?>> getJavaClass(ClassSource classSource) {
		try {
			return Optional.of(classSource.getJavaClass());
		}
		catch (Exception ex) {
			// ignore exception
			return Optional.empty();
		}
	}

	private boolean isSpringTestClass(Class<?> clazz) {
		boolean isSpringTestClass = (isJupiterSpringTestClass(clazz) || isJUnit4SpringTestClass(clazz) ||
				isGenericSpringTestClass(clazz));
		if (isSpringTestClass && logger.isTraceEnabled()) {
			logger.trace("Found Spring test class: " + clazz.getName());
		}
		return isSpringTestClass;
	}

	private static boolean isJupiterSpringTestClass(Class<?> clazz) {
		return MergedAnnotations.search(TYPE_HIERARCHY)
				.withEnclosingClasses(ClassUtils::isInnerClass)
				.from(clazz)
				.stream(EXTEND_WITH_ANNOTATION_NAME)
				.map(annotation -> annotation.getClassArray(VALUE))
				.flatMap(Arrays::stream)
				.map(Class::getName)
				.anyMatch(SPRING_EXTENSION_NAME::equals);
	}

	private static boolean isJUnit4SpringTestClass(Class<?> clazz) {
		MergedAnnotation<Annotation> mergedAnnotation =
				MergedAnnotations.from(clazz, INHERITED_ANNOTATIONS).get(RUN_WITH_ANNOTATION_NAME);
		if (mergedAnnotation.isPresent()) {
			String name = mergedAnnotation.getClass(VALUE).getName();
			return (SPRING_JUNIT4_CLASS_RUNNER_NAME.equals(name) || SPRING_RUNNER_NAME.equals(name));
		}
		return false;
	}

	private static boolean isGenericSpringTestClass(Class<?> clazz) {
		MergedAnnotations mergedAnnotations = MergedAnnotations.from(clazz, TYPE_HIERARCHY);
		return (mergedAnnotations.isPresent(ContextConfiguration.class) ||
				mergedAnnotations.isPresent(BootstrapWith.class));
	}

	private static Set<Path> assertPreconditions(Set<Path> classpathRoots) {
		Assert.notEmpty(classpathRoots, "'classpathRoots' must not be null or empty");
		Assert.noNullElements(classpathRoots, "'classpathRoots' must not contain null elements");
		classpathRoots.forEach(classpathRoot -> Assert.isTrue(Files.exists(classpathRoot),
				() -> "Classpath root [%s] does not exist".formatted(classpathRoot)));
		return classpathRoots;
	}

}
