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

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextLoadException;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;

/**
 * Strategy interface for loading an {@link ApplicationContext} for build-time
 * {@linkplain #loadContextForAotProcessing(MergedContextConfiguration, RuntimeHints)
 * AOT processing} as well as run-time {@linkplain #loadContextForAotRuntime
 * AOT execution} for an integration test managed by the Spring TestContext Framework.
 *
 * <p>{@code AotContextLoader} is an extension of the {@link SmartContextLoader}
 * SPI that allows a context loader to optionally provide ahead-of-time (AOT)
 * support.
 *
 * <p>As of Spring Framework 6.0, AOT infrastructure requires that an {@code AotContextLoader}
 * create a {@link org.springframework.context.support.GenericApplicationContext
 * GenericApplicationContext} for both build-time processing and run-time execution.
 *
 * @author Sam Brannen
 * @since 6.0
 */
public interface AotContextLoader extends SmartContextLoader {

	/**
	 * Load a new {@link ApplicationContext} for AOT build-time processing based
	 * on the supplied {@link MergedContextConfiguration}, configure the context,
	 * and return the context.
	 * <p>The default implementation of this method throws an
	 * {@link UnsupportedOperationException}. Note, however, that the framework
	 * invokes {@link #loadContextForAotProcessing(MergedContextConfiguration, RuntimeHints)}
	 * as of Spring Framework 6.2.4.
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @return a new {@code GenericApplicationContext}
	 * @throws ContextLoadException if context loading failed
	 * @see #loadContextForAotProcessing(MergedContextConfiguration, RuntimeHints)
	 * @see #loadContextForAotRuntime(MergedContextConfiguration, ApplicationContextInitializer)
	 * @deprecated as of Spring Framework 6.2.4, in favor of
	 * {@link #loadContextForAotProcessing(MergedContextConfiguration, RuntimeHints)};
	 * to be removed in Spring Framework 8.0
	 */
	@Deprecated(since = "6.2.4", forRemoval = true)
	default ApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig) throws Exception {
		throw new UnsupportedOperationException(
				"Invoke loadContextForAotProcessing(MergedContextConfiguration, RuntimeHints) instead");
	}

	/**
	 * Load a new {@link ApplicationContext} for AOT build-time processing based
	 * on the supplied {@link MergedContextConfiguration}, configure the context,
	 * and return the context.
	 * <p>In contrast to {@link #loadContext(MergedContextConfiguration)}, this
	 * method must <strong>not</strong>
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#refresh()
	 * refresh} the {@code ApplicationContext} or
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#registerShutdownHook()
	 * register a JVM shutdown hook} for it. Otherwise, this method should implement
	 * behavior identical to {@code loadContext(MergedContextConfiguration)}.
	 * <p>Any exception thrown while attempting to load an {@code ApplicationContext}
	 * should be wrapped in a {@link ContextLoadException}. Concrete implementations
	 * should therefore contain a try-catch block similar to the following.
	 * <pre style="code">
	 * GenericApplicationContext context = // create context
	 * try {
	 *     // configure context
	 * }
	 * catch (Exception ex) {
	 *     throw new ContextLoadException(context, ex);
	 * }
	 * </pre>
	 * <p>For backward compatibility, the default implementation of this method
	 * delegates to {@link #loadContextForAotProcessing(MergedContextConfiguration)}.
	 * Note, however, that the framework only invokes this method as of Spring
	 * Framework 6.2.4.
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @param runtimeHints the runtime hints
	 * @return a new {@code GenericApplicationContext}
	 * @throws ContextLoadException if context loading failed
	 * @since 6.2.4
	 * @see #loadContextForAotRuntime(MergedContextConfiguration, ApplicationContextInitializer)
	 */
	default ApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig,
			RuntimeHints runtimeHints) throws Exception {

		return loadContextForAotProcessing(mergedConfig);
	}

	/**
	 * Load a new {@link ApplicationContext} for AOT run-time execution based on
	 * the supplied {@link MergedContextConfiguration} and
	 * {@link ApplicationContextInitializer}.
	 * <p>This method must instantiate, initialize, and
	 * {@linkplain org.springframework.context.ConfigurableApplicationContext#refresh()
	 * refresh} the {@code ApplicationContext}.
	 * <p>Any exception thrown while attempting to load an {@code ApplicationContext}
	 * should be wrapped in a {@link ContextLoadException}. Concrete implementations
	 * should therefore contain a try-catch block similar to the following.
	 * <pre style="code">
	 * GenericApplicationContext context = // create context
	 * try {
	 *     // configure and refresh context
	 * }
	 * catch (Exception ex) {
	 *     throw new ContextLoadException(context, ex);
	 * }
	 * </pre>
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @param initializer the {@code ApplicationContextInitializer} that should
	 * be applied to the context in order to recreate bean definitions
	 * @return a new {@code GenericApplicationContext}
	 * @throws ContextLoadException if context loading failed
	 * @see #loadContextForAotProcessing(MergedContextConfiguration, RuntimeHints)
	 */
	ApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig,
			ApplicationContextInitializer<ConfigurableApplicationContext> initializer) throws Exception;

}
