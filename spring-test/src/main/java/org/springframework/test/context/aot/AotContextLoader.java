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

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextLoadException;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;

/**
 * Strategy interface for loading an {@link ApplicationContext} for build-time
 * {@linkplain #loadContextForAotProcessing AOT processing} as well as run-time
 * {@linkplain #loadContextForAotRuntime AOT execution} for an integration test
 * managed by the Spring TestContext Framework.
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
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @return a new {@code GenericApplicationContext}
	 * @throws ContextLoadException if context loading failed
	 * @see #loadContextForAotRuntime(MergedContextConfiguration, ApplicationContextInitializer)
	 */
	ApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig) throws Exception;

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
	 * @see #loadContextForAotProcessing(MergedContextConfiguration)
	 */
	ApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig,
			ApplicationContextInitializer<ConfigurableApplicationContext> initializer) throws Exception;

}
