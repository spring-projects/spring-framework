/*
 * Copyright 2002-present the original author or authors.
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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.DefaultLifecycleProcessor;
import org.springframework.core.SpringProperties;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * {@code TestExecutionListener} that coordinates JVM AOT cache recording
 * during integration tests, as proposed in
 * <a href="https://github.com/spring-projects/spring-framework/issues/36774">issue #36774</a>.
 *
 * <p>This listener is registered by default and activates automatically when the test JVM
 * is started with AOT cache recording enabled, that is with the JDK 25+ single-step
 * {@code -XX:AOTCacheOutput=<path>} flag.
 * The flag injection itself is the responsibility of the build tooling used for the test
 * run (for example, the Spring Boot Maven or Gradle plugins), since JVM flags can only be
 * configured at startup.
 *
 * <p>When active, this listener:
 * <ul>
 * <li>Validates that the runtime environment supports AOT cache recording
 * (JDK version and standard class loader usage).</li>
 * <li>Accesses the {@code ApplicationContext} to ensure the context is loaded eagerly so that
 * the training workload from context creation is captured by the JVM.</li>
 * <li>Warns when the {@code -Dspring.context.exit=onRefresh} flag is present, since it would
 * terminate the test JVM mid-run.</li>
 * <li>Verifies, on JVM exit, that the AOT cache file was produced at the path specified by
 * the {@code -XX:AOTCacheOutput} flag.</li>
 * </ul>
 *
 * <p>Requires JDK 25 or later (JEP 514).
 *
 * @author Vasily Pelikh
 * @since 7.1
 * @see <a href="https://openjdk.org/jeps/483">JEP 483: Ahead-of-Time Class Loading &amp; Linking</a>
 * @see <a href="https://openjdk.org/jeps/514">JEP 514: Ahead-of-Time Command-Line Ergonomics</a>
 */
public class AotCacheTestExecutionListener extends AbstractTestExecutionListener {

	/**
	 * The {@link #getOrder() order} value for this listener: {@value}.
	 * Ordered after {@link org.springframework.test.context.support.CommonCachesTestExecutionListener}
	 * and before {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}.
	 * @since 7.1
	 */
	public static final int ORDER = 3006;

	private static final String AOT_CACHE_OUTPUT_FLAG_PREFIX = "-XX:AOTCacheOutput=";

	private static final Log logger = LogFactory.getLog(AotCacheTestExecutionListener.class);

	// Static state: at most one shutdown hook can be registered per JVM. In normal usage, the
	// build tool passes a single -XX:AOTCacheOutput path for the entire test run, so one hook
	// that verifies that path suffices.
	private static final AtomicBoolean shutdownHookRegistered = new AtomicBoolean();

	/**
	 * Returns {@value #ORDER}.
	 */
	@Override
	public final int getOrder() {
		return ORDER;
	}

	@Override
	public void beforeTestClass(TestContext testContext) throws Exception {
		List<String> jvmArguments = getInputArguments();
		if (!isAotRecordingEnabled(jvmArguments)) {
			return;
		}
		logger.info("AOT cache recording is enabled. Preparing the training workload for test class [" +
				testContext.getTestClass().getName() + "].");

		validateJdkVersion();

		// Access the ApplicationContext eagerly so that the training workload from context
		// creation (bean instantiation, @PostConstruct callbacks, etc.) is captured by the
		// JVM's AOT cache mechanism.
		ApplicationContext context = testContext.getApplicationContext();
		validateClassLoader(context);

		warnIfExitOnRefresh();

		String aotCacheOutput = findAotCacheOutput(jvmArguments);
		if (aotCacheOutput != null) {
			logger.info("Expected AOT cache output path: " + aotCacheOutput);
			registerCacheOutputVerification(aotCacheOutput);
		}
	}

	/**
	 * Validate that the JDK version supports AOT cache recording.
	 * @throws IllegalStateException if the JDK version is unsupported
	 */
	protected void validateJdkVersion() {
		int currentVersion = Runtime.version().feature();
		int requiredVersion = getRequiredJavaFeatureVersion();
		if (currentVersion < requiredVersion) {
			throw new IllegalStateException(String.format(
					"AOT cache recording requires JDK %d or later (JEP 514). " +
					"Current JDK version: %d", requiredVersion, currentVersion));
		}
	}

	/**
	 * Validate that the given application context uses a standard JDK class loader.
	 * @param context the application context
	 */
	protected void validateClassLoader(ApplicationContext context) {
		ClassLoader classLoader = context.getClassLoader();
		if (classLoader != null && !isStandardClassLoader(classLoader)) {
			logger.warn(String.format("""
					The ApplicationContext class loader [%s] is not a standard JDK class loader.
					AOT cache only caches classes loaded by JDK built-in class loaders (JEP 483).
					Use an extracted JAR layout with the standard class loader (for example, \
					Spring Boot's executable JAR unpacking) for the cache to be effective.""",
					classLoader.getClass().getName()));
		}
	}

	/**
	 * Determine whether the {@code -Dspring.context.exit=onRefresh} flag is configured.
	 * @return {@code true} if the flag is set to {@code onRefresh}
	 */
	protected boolean isExitOnRefreshConfigured() {
		return "onRefresh".equalsIgnoreCase(SpringProperties.getProperty(DefaultLifecycleProcessor.EXIT_PROPERTY_NAME));
	}

	private void warnIfExitOnRefresh() {
		if (isExitOnRefreshConfigured()) {
			logger.warn("The '" + DefaultLifecycleProcessor.EXIT_PROPERTY_NAME + "=onRefresh' property is set. " +
					"This terminates the JVM when the ApplicationContext refreshes and is not compatible " +
					"with generating an AOT cache from integration tests. Remove it from the test JVM arguments.");
		}
	}

	/**
	 * Return the minimum JDK feature version required for AOT cache recording.
	 * <p>JDK 25 introduced the single-step {@code -XX:AOTCacheOutput} workflow (JEP 514).
	 * Override in tests to simulate unsupported JDK versions.
	 * @return the required JDK feature version (default: 25)
	 */
	protected int getRequiredJavaFeatureVersion() {
		return 25;
	}

	/**
	 * Determine whether the given class loader is a standard JDK class loader
	 * suitable for AOT cache recording.
	 * <p>Leyden only caches classes loaded by JDK built-in class loaders
	 * (such as {@code jdk.internal.loader.BuiltinClassLoader} and its
	 * {@code AppClassLoader} / {@code PlatformClassLoader} subclasses). Custom
	 * class loaders (for example, Spring Boot's {@code LaunchedURLClassLoader})
	 * prevent classes from being cached.
	 * @param classLoader the class loader to check (never {@code null})
	 * @return {@code true} if the class loader is a standard JDK class loader
	 */
	protected boolean isStandardClassLoader(ClassLoader classLoader) {
		String className = classLoader.getClass().getName();
		return className.startsWith("jdk.internal.loader.");
	}

	/**
	 * Determine whether the given JVM arguments enable AOT cache recording via the JDK 25+
	 * single-step {@code -XX:AOTCacheOutput} flag.
	 * @param jvmArguments the JVM command-line arguments
	 * @return {@code true} if AOT cache recording is enabled
	 */
	static boolean isAotRecordingEnabled(List<String> jvmArguments) {
		for (String argument : jvmArguments) {
			if (argument.startsWith(AOT_CACHE_OUTPUT_FLAG_PREFIX)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the value of the {@code -XX:AOTCacheOutput} JVM flag, or {@code null} if
	 * the flag is not present in the given JVM arguments.
	 * @param jvmArguments the JVM command-line arguments
	 * @return the AOT cache output path, or {@code null}
	 */
	static @Nullable String findAotCacheOutput(List<String> jvmArguments) {
		for (String argument : jvmArguments) {
			if (argument.startsWith(AOT_CACHE_OUTPUT_FLAG_PREFIX)) {
				return argument.substring(AOT_CACHE_OUTPUT_FLAG_PREFIX.length());
			}
		}
		return null;
	}

	/**
	 * Return the JVM command-line arguments, excluding the arguments passed to the
	 * {@code main} method.
	 * <p>Exposed for testing purposes.
	 * @return the JVM command-line arguments
	 */
	protected List<String> getInputArguments() {
		return ManagementFactory.getRuntimeMXBean().getInputArguments();
	}

	private void registerCacheOutputVerification(String outputPath) {
		if (shutdownHookRegistered.compareAndSet(false, true)) {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> verifyCacheOutput(outputPath)));
		}
	}

	/**
	 * Verify that the AOT cache file was created at the given output path.
	 * @param outputPath the expected AOT cache output path
	 * @return {@code true} if the cache file exists
	 */
	boolean verifyCacheOutput(String outputPath) {
		File cacheFile = new File(outputPath);
		if (cacheFile.exists()) {
			logger.info("AOT cache file created successfully: " + cacheFile.getAbsolutePath() +
					" (" + cacheFile.length() + " bytes)");
			return true;
		}
		else {
			logger.warn("AOT cache file was NOT created at: " + cacheFile.getAbsolutePath() +
					". Verify that the JVM was started with '-XX:AOTCacheOutput=<path>' (JDK 25+) " +
					"and that the application uses the standard JDK class loader with an extracted JAR layout.");
			return false;
		}
	}

}
