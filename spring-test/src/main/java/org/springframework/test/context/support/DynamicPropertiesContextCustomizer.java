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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ContextCustomizer} to support
 * {@link DynamicPropertySource @DynamicPropertySource} methods.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2.5
 * @see DynamicPropertiesContextCustomizerFactory
 */
class DynamicPropertiesContextCustomizer implements ContextCustomizer {

	private static final String PROPERTY_SOURCE_NAME = "Dynamic Test Properties";

	private final Set<Method> methods;


	DynamicPropertiesContextCustomizer(Set<Method> methods) {
		methods.forEach(this::assertValid);
		this.methods = methods;
	}


	private void assertValid(Method method) {
		Assert.state(Modifier.isStatic(method.getModifiers()),
				() -> "@DynamicPropertySource method '" + method.getName() + "' must be static");
		Class<?>[] types = method.getParameterTypes();
		Assert.state(types.length == 1 && types[0] == DynamicPropertyRegistry.class,
				() -> "@DynamicPropertySource method '" + method.getName() + "' must accept a single DynamicPropertyRegistry argument");
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		MutablePropertySources sources = context.getEnvironment().getPropertySources();
		sources.addFirst(new DynamicValuesPropertySource(PROPERTY_SOURCE_NAME, buildDynamicPropertiesMap()));
	}

	private Map<String, Supplier<Object>> buildDynamicPropertiesMap() {
		Map<String, Supplier<Object>> map = new LinkedHashMap<>();
		DynamicPropertyRegistry dynamicPropertyRegistry = (name, valueSupplier) -> {
			Assert.hasText(name, "'name' must not be null or blank");
			Assert.notNull(valueSupplier, "'valueSupplier' must not be null");
			map.put(name, valueSupplier);
		};
		this.methods.forEach(method -> {
			ReflectionUtils.makeAccessible(method);
			ReflectionUtils.invokeMethod(method, null, dynamicPropertyRegistry);
		});
		return Collections.unmodifiableMap(map);
	}

	Set<Method> getMethods() {
		return this.methods;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof DynamicPropertiesContextCustomizer that &&
				this.methods.equals(that.methods)));
	}

	@Override
	public int hashCode() {
		return this.methods.hashCode();
	}

}
