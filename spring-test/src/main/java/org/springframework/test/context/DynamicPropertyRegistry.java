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

import java.util.function.Supplier;

/**
 * Registry that is used to add properties with dynamically resolved values to
 * the {@code Environment}.
 *
 * <p>A {@code DynamicPropertyRegistry} is supplied as an argument to static
 * {@link DynamicPropertySource @DynamicPropertySource} methods in integration
 * test classes.
 *
 * <p>As of Spring Framework 6.2, a {@code DynamicPropertyRegistry} is also
 * registered as a singleton bean in the test's {@code ApplicationContext}. This
 * allows a {@code DynamicPropertyRegistry} to be autowired into a
 * {@code @Configuration} class or supplied to a {@code @Bean} method as an
 * argument, making it possible to register a dynamic property from within a test's
 * {@code ApplicationContext}. For example, a {@code @Bean} method can register
 * a property whose value is dynamically sourced from the bean that the method
 * returns. Note that such a {@code @Bean} method can optionally be annotated
 * with {@code @DynamicPropertySource} to enforce eager initialization of the
 * bean within the context, thereby ensuring that any dynamic properties sourced
 * from that bean are available to other singleton beans within the context.
 * See {@link DynamicPropertySource @DynamicPropertySource} for an example.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2.5
 * @see DynamicPropertySource
 */
public interface DynamicPropertyRegistry {

	/**
	 * Add a {@link Supplier} for the given property name to this registry.
	 * @param name the name of the property for which the supplier should be added
	 * @param valueSupplier a supplier that will provide the property value on demand
	 */
	void add(String name, Supplier<Object> valueSupplier);

}
