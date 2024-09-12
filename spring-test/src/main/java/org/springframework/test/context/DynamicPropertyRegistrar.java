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

package org.springframework.test.context;

/**
 * Registrar that is used to add properties with dynamically resolved values to
 * the {@code Environment} via a {@link DynamicPropertyRegistry}.
 *
 * <p>Any bean in a test's {@code ApplicationContext} that implements the
 * {@code DynamicPropertyRegistrar} interface will be automatically detected and
 * eagerly initialized before the singleton pre-instantiation phase, and the
 * {@link #accept} methods of such beans will be invoked with a
 * {@code DynamicPropertyRegistry} that performs the actual dynamic property
 * registration on behalf of the registrar.
 *
 * <p>This is an alternative to implementing
 * {@link DynamicPropertySource @DynamicPropertySource} methods in integration
 * test classes and supports additional use cases that are not possible with a
 * {@code @DynamicPropertySource} method. For example, since a
 * {@code DynamicPropertyRegistrar} is itself a bean in the {@code ApplicationContext},
 * it can interact with other beans in the context and register dynamic properties
 * that are sourced from those beans. Note, however, that any interaction with
 * other beans results in eager initialization of those other beans and their
 * dependencies.
 *
 * <h3>Precedence</h3>
 *
 * <p>Dynamic properties have higher precedence than those loaded from
 * {@link TestPropertySource @TestPropertySource}, the operating system's
 * environment, Java system properties, or property sources added by the
 * application declaratively by using
 * {@link org.springframework.context.annotation.PropertySource @PropertySource}
 * or programmatically. Thus, dynamic properties can be used to selectively
 * override properties loaded via {@code @TestPropertySource}, system property
 * sources, and application property sources.
 *
 * <h3>Example</h3>
 *
 * <p>The following example demonstrates how to implement a
 * {@code DynamicPropertyRegistrar} as a lambda expression that registers a
 * dynamic property for the {@code ApiServer} bean. Other beans in the
 * {@code ApplicationContext} can access the {@code api.url} property which is
 * dynamically retrieved from the {@code ApiServer} bean &mdash; for example,
 * via {@code @Value("${api.url}")}.
 *
 * <pre class="code">
 * &#064;Configuration
 * class TestConfig {
 *
 *     &#064;Bean
 *     ApiServer apiServer() {
 *         return new ApiServer();
 *     }
 *
 *     &#064;Bean
 *     DynamicPropertyRegistrar apiPropertiesRegistrar(ApiServer apiServer) {
 *         return registry -> registry.add("api.url", apiServer::getUrl);
 *     }
 *
 * }</pre>
 *
 * @author Sam Brannen
 * @since 6.2
 * @see DynamicPropertySource
 * @see DynamicPropertyRegistry
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
 */
@FunctionalInterface
public interface DynamicPropertyRegistrar {

	/**
	 * Register dynamic properties in the supplied registry.
	 */
	void accept(DynamicPropertyRegistry registry);

}
