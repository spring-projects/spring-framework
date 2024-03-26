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
 * <p>The {@code SmartContextLoader} SPI supersedes the {@link ContextLoader} SPI
 * introduced in Spring 2.5: a {@code SmartContextLoader} can choose to process
 * resource locations, component classes, or a combination of both. Furthermore, a
 * {@code SmartContextLoader} can configure the context that it
 * {@linkplain #loadContext(MergedContextConfiguration) loads} based on any
 * properties available in the provided {@link MergedContextConfiguration}.
 * For example, active bean definition profiles can be configured for the context
 * based on {@link MergedContextConfiguration#getActiveProfiles()}.
 *
 * <p>See the Javadoc for {@link ContextConfiguration @ContextConfiguration}
 * for a definition of <em>component classes</em>.
 *
 * <p>Clients of a {@code SmartContextLoader} should call
 * {@link #processContextConfiguration(ContextConfigurationAttributes)
 * processContextConfiguration()} prior to calling
 * {@link #loadContext(MergedContextConfiguration) loadContext()}. This gives a
 * {@code SmartContextLoader} the opportunity to provide custom support for
 * modifying resource locations or detecting default resource locations or
 * default configuration classes. The results of
 * {@link #processContextConfiguration(ContextConfigurationAttributes)
 * processContextConfiguration()} should be merged for all classes in the
 * hierarchy of the root test class and then supplied to
 * {@link #loadContext(MergedContextConfiguration) loadContext()}.
 *
 * <p>NOTE: As of Spring Framework 6.0, {@code SmartContextLoader} no longer
 * supports methods defined in the {@code ContextLoader} SPI.
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor.
 *
 * <p>Spring provides the following {@code SmartContextLoader} implementations.
 * <ul>
 * <li>{@link org.springframework.test.context.support.DelegatingSmartContextLoader DelegatingSmartContextLoader}</li>
 * <li>{@link org.springframework.test.context.support.AnnotationConfigContextLoader AnnotationConfigContextLoader}</li>
 * <li>{@link org.springframework.test.context.support.GenericXmlContextLoader GenericXmlContextLoader}</li>
 * <li>{@link org.springframework.test.context.support.GenericGroovyXmlContextLoader GenericGroovyXmlContextLoader}</li>
 * <li>{@link org.springframework.test.context.web.WebDelegatingSmartContextLoader WebDelegatingSmartContextLoader}</li>
 * <li>{@link org.springframework.test.context.web.AnnotationConfigWebContextLoader AnnotationConfigWebContextLoader}</li>
 * <li>{@link org.springframework.test.context.web.GenericXmlWebContextLoader GenericXmlWebContextLoader}</li>
 * <li>{@link org.springframework.test.context.web.GenericGroovyXmlWebContextLoader GenericGroovyXmlWebContextLoader}</li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 3.1
 * @see ContextConfiguration
 * @see ActiveProfiles
 * @see TestPropertySource
 * @see ContextConfigurationAttributes
 * @see MergedContextConfiguration
 */
public interface SmartContextLoader extends ContextLoader {

	/**
	 * Process the {@link ContextConfigurationAttributes} for a given test class.
	 * <p>Concrete implementations may choose to <em>modify</em> the {@code locations}
	 * or {@code classes} in the supplied {@code ContextConfigurationAttributes},
	 * <em>generate</em> default configuration locations, or <em>detect</em>
	 * default configuration classes if the supplied values are {@code null}
	 * or empty.
	 * <p><b>Note</b>: a {@code SmartContextLoader} must <em>preemptively</em>
	 * verify that a generated or detected default actually exists before setting
	 * the corresponding {@code locations} or {@code classes} property in the
	 * supplied {@code ContextConfigurationAttributes}. Consequently, leaving the
	 * {@code locations} or {@code classes} property empty signals that this
	 * {@code SmartContextLoader} was not able to generate or detect defaults.
	 * @param configAttributes the context configuration attributes to process
	 */
	void processContextConfiguration(ContextConfigurationAttributes configAttributes);

	/**
	 * Load a new {@link ApplicationContext} based on the supplied
	 * {@link MergedContextConfiguration}, configure the context, and return the
	 * context in a fully <em>refreshed</em> state.
	 * <p>Concrete implementations should register annotation configuration
	 * processors with bean factories of
	 * {@link ApplicationContext application contexts} loaded by this
	 * {@code SmartContextLoader}. Beans will therefore automatically be
	 * candidates for annotation-based dependency injection using
	 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired},
	 * {@link jakarta.annotation.Resource @Resource}, and
	 * {@link jakarta.inject.Inject @Inject}. In addition, concrete implementations
	 * should perform the following actions.
	 * <ul>
	 * <li>Set the parent {@code ApplicationContext} if appropriate (see
	 * {@link MergedContextConfiguration#getParent()}).</li>
	 * <li>Set the active bean definition profiles in the context's
	 * {@link org.springframework.core.env.Environment Environment} (see
	 * {@link MergedContextConfiguration#getActiveProfiles()}).</li>
	 * <li>Add test {@link org.springframework.core.env.PropertySource PropertySources}
	 * to the {@code Environment} (see
	 * {@link MergedContextConfiguration#getPropertySourceLocations()},
	 * {@link MergedContextConfiguration#getPropertySourceProperties()}, and
	 * {@link org.springframework.test.context.support.TestPropertySourceUtils
	 * TestPropertySourceUtils}).</li>
	 * <li>Invoke {@link org.springframework.context.ApplicationContextInitializer
	 * ApplicationContextInitializers} (see
	 * {@link MergedContextConfiguration#getContextInitializerClasses()}).</li>
	 * <li>Invoke {@link ContextCustomizer ContextCustomizers} (see
	 * {@link MergedContextConfiguration#getContextCustomizers()}).</li>
	 * <li>Register a JVM shutdown hook for the {@link ApplicationContext}. Unless
	 * the context gets closed early, all context instances will be automatically
	 * closed on JVM shutdown. This allows for freeing of external resources held
	 * by beans within the context &mdash; for example, temporary files.</li>
	 * </ul>
	 * <p>As of Spring Framework 6.0, any exception thrown while attempting to
	 * load an {@code ApplicationContext} should be wrapped in a
	 * {@link ContextLoadException}. Concrete implementations should therefore
	 * contain a try-catch block similar to the following.
	 * <pre style="code">
	 * ApplicationContext context = // create context
	 * try {
	 *     // configure and refresh context
	 * }
	 * catch (Exception ex) {
	 *     throw new ContextLoadException(context, ex);
	 * }
	 * </pre>
	 * @param mergedConfig the merged context configuration to use to load the
	 * application context
	 * @return a new application context
	 * @throws ContextLoadException if context loading failed
	 * @see #processContextConfiguration(ContextConfigurationAttributes)
	 * @see org.springframework.context.annotation.AnnotationConfigUtils#registerAnnotationConfigProcessors(org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment()
	 */
	ApplicationContext loadContext(MergedContextConfiguration mergedConfig) throws Exception;

	/**
	 * {@code SmartContextLoader} does not support deprecated {@link ContextLoader} methods.
	 * Call {@link #processContextConfiguration(ContextConfigurationAttributes)} instead.
	 * @throws UnsupportedOperationException in this implementation
	 * @since 6.0
	 */
	@Override
	@SuppressWarnings("deprecation")
	default String[] processLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException("""
				SmartContextLoader does not support the ContextLoader SPI. \
				Call processContextConfiguration(ContextConfigurationAttributes) instead.""");
	}

	/**
	 * {@code SmartContextLoader} does not support deprecated {@link ContextLoader} methods.
	 * <p>Call {@link #loadContext(MergedContextConfiguration)} instead.
	 * @throws UnsupportedOperationException in this implementation
	 * @since 6.0
	 */
	@Override
	@SuppressWarnings("deprecation")
	default ApplicationContext loadContext(String... locations) throws Exception {
		throw new UnsupportedOperationException("""
				SmartContextLoader does not support the ContextLoader SPI. \
				Call loadContext(MergedContextConfiguration) instead.""");
	}

}
