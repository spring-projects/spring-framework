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
 * supplied to {@link DynamicPropertyRegistrar} beans in the test's
 * {@code ApplicationContext}, making it possible to register dynamic properties
 * based on beans in the context. For example, a {@code @Bean} method can return
 * a {@code DynamicPropertyRegistrar} that registers a property whose value is
 * dynamically sourced from another bean in the context. See the documentation
 * for {@code DynamicPropertyRegistrar} for an example.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2.5
 * @see DynamicPropertySource
 * @see DynamicPropertyRegistrar
 */
public interface DynamicPropertyRegistry {

	/**
	 * Add a {@link Supplier} for the given property name to this registry.
	 * @param name the name of the property for which the supplier should be added
	 * @param valueSupplier a supplier that will provide the property value on demand
	 */
	void add(String name, Supplier<Object> valueSupplier);

}
