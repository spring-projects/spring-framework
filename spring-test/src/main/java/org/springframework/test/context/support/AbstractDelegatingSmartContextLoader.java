/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.aot.AotContextLoader;
import org.springframework.util.Assert;

/**
 * {@code AbstractDelegatingSmartContextLoader} serves as an abstract base class
 * for implementations of the {@link SmartContextLoader} SPI that delegate to a
 * set of <em>candidate</em> SmartContextLoaders (i.e., one that supports XML
 * configuration files or Groovy scripts and one that supports annotated classes)
 * to determine which context loader is appropriate for a given test class's
 * configuration. Each candidate is given a chance to
 * {@linkplain #processContextConfiguration process} the
 * {@link ContextConfigurationAttributes} for each class in the test class hierarchy
 * and enclosing class hierarchy that is annotated with
 * {@link ContextConfiguration @ContextConfiguration}, and the candidate that
 * supports the merged, processed configuration will be used to actually
 * {@linkplain #loadContext(MergedContextConfiguration) load} the context.
 *
 * <p>Any reference to an <em>XML-based loader</em> can be interpreted to mean
 * a context loader that supports only XML configuration files or one that
 * supports both XML configuration files and Groovy scripts simultaneously.
 *
 * <p>Placing an empty {@code @ContextConfiguration} annotation on a test class signals
 * that default resource locations (e.g., XML configuration files or Groovy scripts)
 * or default
 * {@linkplain org.springframework.context.annotation.Configuration configuration classes}
 * should be detected. Furthermore, if a specific {@link ContextLoader} or
 * {@link SmartContextLoader} is not explicitly declared via
 * {@code @ContextConfiguration}, a concrete subclass of
 * {@code AbstractDelegatingSmartContextLoader} will be used as the default loader,
 * thus providing automatic support for either path-based resource locations
 * (e.g., XML configuration files and Groovy scripts) or annotated classes,
 * but not both simultaneously.
 *
 * <p>A test class may optionally declare neither path-based resource locations
 * nor annotated classes and instead declare only {@linkplain
 * ContextConfiguration#initializers application context initializers}. In such
 * cases, an attempt will still be made to detect defaults, but their absence will
 * not result in an exception.
 *
 * <p>As of Spring Framework 6.0, this class implements {@link AotContextLoader}.
 * Consequently, the candidate {@link #getXmlLoader()} and
 * {@link #getAnnotationConfigLoader()} must also implement {@code AotContextLoader}
 * in order to provide AOT support.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 3.2
 * @see SmartContextLoader
 */
public abstract class AbstractDelegatingSmartContextLoader implements AotContextLoader {

	private static final Log logger = LogFactory.getLog(AbstractDelegatingSmartContextLoader.class);


	/**
	 * Get the delegate {@code SmartContextLoader} that supports XML configuration
	 * files and/or Groovy scripts.
	 */
	protected abstract SmartContextLoader getXmlLoader();

	/**
	 * Get the delegate {@code SmartContextLoader} that supports annotated classes.
	 */
	protected abstract SmartContextLoader getAnnotationConfigLoader();


	/**
	 * Delegates to candidate {@code SmartContextLoaders} to process the supplied
	 * {@link ContextConfigurationAttributes}.
	 * <p>Delegation is based on explicit knowledge of the implementations of the
	 * default loaders for {@linkplain #getXmlLoader() XML configuration files and
	 * Groovy scripts} and {@linkplain #getAnnotationConfigLoader() annotated classes}.
	 * Specifically, the delegation algorithm is as follows:
	 * <ul>
	 * <li>If the resource locations or annotated classes in the supplied
	 * {@code ContextConfigurationAttributes} are not empty, the appropriate
	 * candidate loader will be allowed to process the configuration <em>as is</em>,
	 * without any checks for detection of defaults.</li>
	 * <li>Otherwise, the XML-based loader will be allowed to process
	 * the configuration in order to detect default resource locations. If
	 * the XML-based loader detects default resource locations,
	 * an {@code info} message will be logged.</li>
	 * <li>Subsequently, the annotation-based loader will be allowed to
	 * process the configuration in order to detect default configuration classes.
	 * If the annotation-based loader detects default configuration
	 * classes, an {@code info} message will be logged.</li>
	 * </ul>
	 * @param configAttributes the context configuration attributes to process
	 * @throws IllegalArgumentException if the supplied configuration attributes are
	 * {@code null}, or if the supplied configuration attributes include both
	 * resource locations and annotated classes
	 * @throws IllegalStateException if the XML-based loader detects default
	 * configuration classes; if the annotation-based loader detects default
	 * resource locations; if neither candidate loader detects defaults for the supplied
	 * context configuration; or if both candidate loaders detect defaults for the
	 * supplied context configuration
	 */
	@Override
	public void processContextConfiguration(final ContextConfigurationAttributes configAttributes) {
		Assert.notNull(configAttributes, "configAttributes must not be null");
		Assert.isTrue(!(configAttributes.hasLocations() && configAttributes.hasClasses()),
				() -> String.format("Cannot process locations AND classes for context configuration %s: " +
						"configure one or the other, but not both.", configAttributes));

		// If the original locations or classes were not empty, there's no
		// need to bother with default detection checks; just let the
		// appropriate loader process the configuration.
		if (configAttributes.hasLocations()) {
			delegateProcessing(getXmlLoader(), configAttributes);
		}
		else if (configAttributes.hasClasses()) {
			delegateProcessing(getAnnotationConfigLoader(), configAttributes);
		}
		else {
			// Else attempt to detect defaults...

			// Let the XML loader process the configuration.
			delegateProcessing(getXmlLoader(), configAttributes);
			boolean xmlLoaderDetectedDefaults = configAttributes.hasLocations();

			if (xmlLoaderDetectedDefaults) {
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("%s detected default locations for context configuration %s",
							name(getXmlLoader()), configAttributes));
				}
			}

			Assert.state(!configAttributes.hasClasses(), () -> String.format(
					"%s should NOT have detected default configuration classes for context configuration %s",
					name(getXmlLoader()), configAttributes));

			// Now let the annotation config loader process the configuration.
			delegateProcessing(getAnnotationConfigLoader(), configAttributes);

			if (configAttributes.hasClasses()) {
				if (logger.isTraceEnabled()) {
					logger.trace(String.format("%s detected default configuration classes for context configuration %s",
							name(getAnnotationConfigLoader()), configAttributes));
				}
			}

			Assert.state(xmlLoaderDetectedDefaults || !configAttributes.hasLocations(), () -> String.format(
					"%s should NOT have detected default locations for context configuration %s",
					name(getAnnotationConfigLoader()), configAttributes));

			if (configAttributes.hasLocations() && configAttributes.hasClasses()) {
				String msg = String.format(
						"Configuration error: both default locations AND default configuration classes " +
						"were detected for context configuration %s; configure one or the other, but not both.",
						configAttributes);
				logger.error(msg);
				throw new IllegalStateException(msg);
			}
		}
	}

	/**
	 * Delegates to an appropriate candidate {@code SmartContextLoader} to load
	 * an {@link ApplicationContext}.
	 * <p>Delegation is based on explicit knowledge of the implementations of the
	 * default loaders for {@linkplain #getXmlLoader() XML configuration files and
	 * Groovy scripts} and {@linkplain #getAnnotationConfigLoader() annotated classes}.
	 * Specifically, the delegation algorithm is as follows:
	 * <ul>
	 * <li>If the resource locations in the supplied {@code MergedContextConfiguration}
	 * are not empty and the annotated classes are empty,
	 * the XML-based loader will load the {@code ApplicationContext}.</li>
	 * <li>If the annotated classes in the supplied {@code MergedContextConfiguration}
	 * are not empty and the resource locations are empty,
	 * the annotation-based loader will load the {@code ApplicationContext}.</li>
	 * </ul>
	 * @param mergedConfig the merged context configuration to use to load the application context
	 * @return a new application context
	 * @throws IllegalStateException if neither candidate loader is capable of loading an
	 * {@code ApplicationContext} from the supplied merged context configuration
	 */
	@Override
	public final ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception {
		SmartContextLoader loader = getContextLoader(mergedConfig);
		if (logger.isTraceEnabled()) {
			logger.trace("Delegating to %s to load context for %s".formatted(name(loader), mergedConfig));
		}
		return loader.loadContext(mergedConfig);
	}

	/**
	 * Delegates to an appropriate candidate {@code SmartContextLoader} to load
	 * an {@link ApplicationContext} for AOT processing.
	 * <p>Delegation is based on explicit knowledge of the implementations of the
	 * default loaders. See {@link #loadContext(MergedContextConfiguration)} for
	 * details.
	 * @param mergedConfig the merged context configuration to use to load the application context
	 * @return a new application context
	 * @throws IllegalArgumentException if the supplied merged configuration is {@code null}
	 * @throws IllegalStateException if neither candidate loader is capable of loading an
	 * {@code ApplicationContext} from the supplied merged context configuration
	 * @since 6.0
	 * @see AotContextLoader#loadContextForAotProcessing(MergedContextConfiguration)
	 */
	@Override
	public final ApplicationContext loadContextForAotProcessing(MergedContextConfiguration mergedConfig) throws Exception {
		AotContextLoader loader = getAotContextLoader(mergedConfig);
		if (logger.isTraceEnabled()) {
			logger.trace("Delegating to %s to load context for AOT processing for %s"
					.formatted(name(loader), mergedConfig));
		}
		return loader.loadContextForAotProcessing(mergedConfig);
	}

	/**
	 * Delegates to an appropriate candidate {@code SmartContextLoader} to load
	 * an {@link ApplicationContext} for AOT run-time execution.
	 * <p>Delegation is based on explicit knowledge of the implementations of the
	 * default loaders. See {@link #loadContext(MergedContextConfiguration)} for
	 * details.
	 * @param mergedConfig the merged context configuration to use to load the application context
	 * @param initializer the {@code ApplicationContextInitializer} that should
	 * be applied to the context in order to recreate bean definitions
	 * @return a new application context
	 * @throws IllegalStateException if neither candidate loader is capable of loading an
	 * {@code ApplicationContext} from the supplied merged context configuration
	 * @since 6.0
	 * @see AotContextLoader#loadContextForAotRuntime(MergedContextConfiguration, ApplicationContextInitializer)
	 */
	@Override
	public final ApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig,
			ApplicationContextInitializer<ConfigurableApplicationContext> initializer) throws Exception {

		AotContextLoader loader = getAotContextLoader(mergedConfig);
		if (logger.isTraceEnabled()) {
			logger.trace("Delegating to %s to load context for AOT execution for %s"
					.formatted(name(loader), mergedConfig));
		}
		return loader.loadContextForAotRuntime(mergedConfig, initializer);
	}

	private SmartContextLoader getContextLoader(MergedContextConfiguration mergedConfig) {
		Assert.notNull(mergedConfig, "MergedContextConfiguration must not be null");
		Assert.state(!(mergedConfig.hasLocations() && mergedConfig.hasClasses()), () -> """
				Neither %s nor %s is able to load an ApplicationContext for %s: \
				declare either 'locations' or 'classes' but not both.""".formatted(
						name(getXmlLoader()), name(getAnnotationConfigLoader()), mergedConfig));

		SmartContextLoader[] candidates = {getXmlLoader(), getAnnotationConfigLoader()};
		for (SmartContextLoader loader : candidates) {
			// Determine if each loader can load a context from the mergedConfig. If it
			// can, let it; otherwise, keep iterating.
			if (supports(loader, mergedConfig)) {
				return loader;
			}
		}

		// If neither of the candidates supports the mergedConfig based on resources but
		// ACIs or customizers were declared, then delegate to the annotation config loader.
		if (hasInitializersOrCustomizers(mergedConfig)) {
			return getAnnotationConfigLoader();
		}

		// else...
		throw new IllegalStateException(
				"Neither %s nor %s is able to load an ApplicationContext for %s.".formatted(
					name(getXmlLoader()), name(getAnnotationConfigLoader()), mergedConfig));
	}

	private AotContextLoader getAotContextLoader(MergedContextConfiguration mergedConfig) {
		SmartContextLoader loader = getContextLoader(mergedConfig);
		if (!(loader instanceof AotContextLoader aotContextLoader)) {
			throw new IllegalStateException("%s must be an AotContextLoader".formatted(name(loader)));
		}
		return aotContextLoader;
	}

	private boolean supports(SmartContextLoader loader, MergedContextConfiguration mergedConfig) {
		if (loader == getAnnotationConfigLoader()) {
			return (mergedConfig.hasClasses() && !mergedConfig.hasLocations());
		}
		else {
			return (mergedConfig.hasLocations() && !mergedConfig.hasClasses());
		}
	}


	private static void delegateProcessing(SmartContextLoader loader, ContextConfigurationAttributes configAttributes) {
		if (logger.isTraceEnabled()) {
			logger.trace("Delegating to %s to process context configuration %s"
					.formatted(name(loader), configAttributes));
		}
		loader.processContextConfiguration(configAttributes);
	}

	private static boolean hasInitializersOrCustomizers(MergedContextConfiguration mergedConfig) {
		return !(mergedConfig.getContextInitializerClasses().isEmpty() &&
				mergedConfig.getContextCustomizers().isEmpty());
	}

	private static String name(SmartContextLoader loader) {
		return loader.getClass().getSimpleName();
	}

}
