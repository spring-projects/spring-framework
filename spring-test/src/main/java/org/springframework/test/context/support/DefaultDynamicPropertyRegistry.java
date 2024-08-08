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

package org.springframework.test.context.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.util.Assert;

/**
 * Default {@link DynamicPropertyRegistry} implementation.
 *
 * @author Sam Brannen
 * @since 6.2
 */
final class DefaultDynamicPropertyRegistry implements DynamicPropertyRegistry {

	final Map<String, Supplier<Object>> valueSuppliers = Collections.synchronizedMap(new LinkedHashMap<>());

	private final ConfigurableEnvironment environment;

	private final boolean lazilyRegisterPropertySource;

	private final Lock propertySourcesLock = new ReentrantLock();


	DefaultDynamicPropertyRegistry(ConfigurableEnvironment environment, boolean lazilyRegisterPropertySource) {
		this.environment = environment;
		this.lazilyRegisterPropertySource = lazilyRegisterPropertySource;
	}


	@Override
	public void add(String name, Supplier<Object> valueSupplier) {
		Assert.hasText(name, "'name' must not be null or blank");
		Assert.notNull(valueSupplier, "'valueSupplier' must not be null");
		if (this.lazilyRegisterPropertySource) {
			ensurePropertySourceIsRegistered();
		}
		this.valueSuppliers.put(name, valueSupplier);
	}

	private void ensurePropertySourceIsRegistered() {
		MutablePropertySources propertySources = this.environment.getPropertySources();
		this.propertySourcesLock.lock();
		try {
			PropertySource<?> ps = propertySources.get(DynamicValuesPropertySource.PROPERTY_SOURCE_NAME);
			if (ps == null) {
				propertySources.addFirst(new DynamicValuesPropertySource(this.valueSuppliers));
			}
		}
		finally {
			this.propertySourcesLock.unlock();
		}
	}

}
