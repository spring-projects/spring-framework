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

package org.springframework.test.context;

import org.springframework.context.ApplicationContext;

/**
 * Strategy interface for loading an {@link ApplicationContext} for an integration
 * test managed by the Spring TestContext Framework.
 *
 * <p><strong>NOTE</strong>: as of Spring Framework 6.0, {@code ContextLoader} is
 * effectively a marker interface and should not be implemented directly. Implement
 * {@link SmartContextLoader} instead of this interface in order to provide support
 * for annotated classes, active bean definition profiles, application context
 * initializers, and various other features not supported by methods defined in
 * the {@code ContextLoader} SPI.
 *
 * <p>Clients of a {@code ContextLoader} should call
 * {@link #processLocations(Class, String...) processLocations()} prior to
 * calling {@link #loadContext(String...) loadContext()} in case the
 * {@code ContextLoader} provides custom support for modifying or generating locations.
 * The results of {@link #processLocations(Class, String...) processLocations()}
 * should then be supplied to {@link #loadContext(String...) loadContext()}.
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 2.5
 * @see SmartContextLoader
 */
public interface ContextLoader {

	/**
	 * Processes application context resource locations for a specified class.
	 * <p>Concrete implementations may choose to modify the supplied locations,
	 * generate new locations, or simply return the supplied locations unchanged.
	 * @param clazz the class with which the locations are associated: used to
	 * determine how to process the supplied locations
	 * @param locations the unmodified locations to use for loading the
	 * application context (can be {@code null} or empty)
	 * @return an array of application context resource locations
	 * @deprecated as of Spring Framework 6.0, in favor of methods defined in the
	 * {@link SmartContextLoader} SPI
	 */
	@Deprecated(since = "6.0")
	String[] processLocations(Class<?> clazz, String... locations);

	/**
	 * Loads a new {@link ApplicationContext} based on the supplied
	 * {@code locations}, configures the context, and finally returns
	 * the context in fully <em>refreshed</em> state.
	 * <p>Configuration locations are generally considered to be classpath
	 * resources by default.
	 * <p>Concrete implementations should register annotation configuration
	 * processors with bean factories of application contexts loaded by this
	 * {@code ContextLoader}. Beans will therefore automatically be candidates
	 * for annotation-based dependency injection using
	 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired},
	 * {@link jakarta.annotation.Resource @Resource}, and
	 * {@link jakarta.inject.Inject @Inject}.
	 * <p>Any {@code ApplicationContext} loaded by a {@code ContextLoader}
	 * <strong>must</strong> register a JVM shutdown hook for itself. Unless the
	 * context gets closed early, all context instances will be automatically
	 * closed on JVM shutdown. This allows for freeing external resources held by
	 * beans within the context, e.g. temporary files.
	 * @param locations the resource locations to use to load the application context
	 * @return a new application context
	 * @throws Exception if context loading failed
	 * @deprecated as of Spring Framework 6.0, in favor of methods defined in the
	 * {@link SmartContextLoader} SPI
	 */
	@Deprecated(since = "6.0")
	ApplicationContext loadContext(String... locations) throws Exception;

}
