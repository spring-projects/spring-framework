/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.util.Assert;
import org.springframework.util.function.SupplierUtils;

/**
 * {@link MapPropertySource} backed by a map with dynamically supplied values.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 5.2.5
 */
class DynamicValuesPropertySource extends MapPropertySource {

	static final String PROPERTY_SOURCE_NAME = "Dynamic Test Properties";

	final DynamicPropertyRegistry dynamicPropertyRegistry;


	DynamicValuesPropertySource() {
		this(Collections.synchronizedMap(new LinkedHashMap<>()));
	}

	DynamicValuesPropertySource(Map<String, Supplier<Object>> valueSuppliers) {
		super(PROPERTY_SOURCE_NAME, Collections.unmodifiableMap(valueSuppliers));
		this.dynamicPropertyRegistry = (name, valueSupplier) -> {
			Assert.hasText(name, "'name' must not be null or blank");
			Assert.notNull(valueSupplier, "'valueSupplier' must not be null");
			valueSuppliers.put(name, valueSupplier);
		};
	}


	@Override
	public @Nullable Object getProperty(String name) {
		return SupplierUtils.resolve(super.getProperty(name));
	}


	/**
	 * Get the {@code DynamicValuesPropertySource} registered in the environment
	 * or create and register a new {@code DynamicValuesPropertySource} in the
	 * environment.
	 */
	static DynamicValuesPropertySource getOrCreate(ConfigurableEnvironment environment) {
		MutablePropertySources propertySources = environment.getPropertySources();
		PropertySource<?> propertySource = propertySources.get(PROPERTY_SOURCE_NAME);
		if (propertySource instanceof DynamicValuesPropertySource dynamicValuesPropertySource) {
			return dynamicValuesPropertySource;
		}
		else if (propertySource == null) {
			DynamicValuesPropertySource dynamicValuesPropertySource = new DynamicValuesPropertySource();
			propertySources.addFirst(dynamicValuesPropertySource);
			return dynamicValuesPropertySource;
		}
		else {
			throw new IllegalStateException("PropertySource with name '%s' must be a DynamicValuesPropertySource"
					.formatted(PROPERTY_SOURCE_NAME));
		}
	}

}
