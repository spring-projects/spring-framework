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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @DynamicPropertySource} is an annotation that can be applied to static
 * methods in integration test classes or to {@code @Bean} methods in test
 * {@code @Configuration} classes in order to add properties with dynamic values
 * to the {@code Environment}'s set of {@code PropertySources}.
 *
 * <p>This annotation and its supporting infrastructure were originally designed
 * to allow properties from
 * <a href="https://www.testcontainers.org/">Testcontainers</a> based tests to be
 * exposed easily to Spring integration tests. However, this feature may be used
 * with any form of external resource whose lifecycle is managed outside the
 * test's {@code ApplicationContext} or with beans whose lifecycle is managed by
 * the test's {@code ApplicationContext}.
 *
 * <p>{@code @DynamicPropertySource}-annotated methods use a
 * {@code DynamicPropertyRegistry} to add <em>name-value</em> pairs to the
 * {@code Environment}'s set of {@code PropertySources}. Values are dynamic and
 * provided via a {@link java.util.function.Supplier} which is only invoked when
 * the property is resolved. Typically, method references are used to supply values,
 * as in the example below.
 *
 * <p>Methods in integration test classes that are annotated with
 * {@code @DynamicPropertySource} must be {@code static} and must accept a single
 * {@link DynamicPropertyRegistry} argument.
 *
 * <p>{@code @Bean} methods annotated with {@code @DynamicPropertySource} may
 * either accept an argument of type {@code DynamicPropertyRegistry} or access a
 * {@code DynamicPropertyRegistry} instance autowired into their enclosing
 * {@code @Configuration} class. Note, however, that {@code @Bean} methods which
 * interact with a {@code DynamicPropertyRegistry} are not required to be annotated
 * with {@code @DynamicPropertySource} unless they need to enforce eager
 * initialization of the bean within the context.
 * See {@link DynamicPropertyRegistry} for details.
 *
 * <p>Dynamic properties from methods annotated with {@code @DynamicPropertySource}
 * will be <em>inherited</em> from enclosing test classes, analogous to inheritance
 * from superclasses and interfaces.
 * See {@link NestedTestConfiguration @NestedTestConfiguration} for details.
 *
 * <p><strong>NOTE</strong>: if you use {@code @DynamicPropertySource} in a base
 * class and discover that tests in subclasses fail because the dynamic properties
 * change between subclasses, you may need to annotate your base class with
 * {@link org.springframework.test.annotation.DirtiesContext @DirtiesContext} to
 * ensure that each subclass gets its own {@code ApplicationContext} with the
 * correct dynamic properties.
 *
 * <h3>Precedence</h3>
 * <p>Dynamic properties have higher precedence than those loaded from
 * {@link TestPropertySource @TestPropertySource}, the operating system's
 * environment, Java system properties, or property sources added by the
 * application declaratively by using
 * {@link org.springframework.context.annotation.PropertySource @PropertySource}
 * or programmatically. Thus, dynamic properties can be used to selectively
 * override properties loaded via {@code @TestPropertySource}, system property
 * sources, and application property sources.
 *
 * <h3>Examples</h3>
 *
 * <p>The following example demonstrates how to use {@code @DynamicPropertySource}
 * in an integration test class. Beans in the {@code ApplicationContext} can
 * access the {@code redis.host} and {@code redis.port} properties which are
 * dynamically retrieved from the Redis container.
 *
 * <pre class="code">
 * &#064;SpringJUnitConfig(...)
 * &#064;Testcontainers
 * class ExampleIntegrationTests {
 *
 *     &#064;Container
 *     static GenericContainer redis =
 *         new GenericContainer("redis:5.0.3-alpine").withExposedPorts(6379);
 *
 *     // ...
 *
 *     &#064;DynamicPropertySource
 *     static void redisProperties(DynamicPropertyRegistry registry) {
 *         registry.add("redis.host", redis::getHost);
 *         registry.add("redis.port", redis::getFirstMappedPort);
 *     }
 * }</pre>
 *
 * <p>The following example demonstrates how to use {@code @DynamicPropertySource}
 * with a {@code @Bean} method. Beans in the {@code ApplicationContext} can
 * access the {@code api.url} property which is dynamically retrieved from the
 * {@code ApiServer} bean.
 *
 * <pre class="code">
 * &#064;Configuration
 * class TestConfig {
 *
 *     &#064;Bean
 *     &#064;DynamicPropertySource
 *     ApiServer apiServer(DynamicPropertyRegistry registry) {
 *         ApiServer apiServer = new ApiServer();
 *         registry.add("api.url", apiServer::getUrl);
 *         return apiServer;
 *     }
 * }</pre>
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2.5
 * @see DynamicPropertyRegistry
 * @see ContextConfiguration
 * @see TestPropertySource
 * @see org.springframework.core.env.PropertySource
 * @see org.springframework.test.annotation.DirtiesContext
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DynamicPropertySource {
}
