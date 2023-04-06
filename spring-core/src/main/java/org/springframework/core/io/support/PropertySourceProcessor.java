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

package org.springframework.core.io.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Contribute {@link PropertySource property sources} to the {@link Environment}.
 *
 * <p>This class is stateful and merges descriptors with the same name in a
 * single {@link PropertySource} rather than creating dedicated ones.
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @see PropertySourceDescriptor
 */
public class PropertySourceProcessor {

	private static final PropertySourceFactory DEFAULT_PROPERTY_SOURCE_FACTORY = new DefaultPropertySourceFactory();

	private static final Log logger = LogFactory.getLog(PropertySourceProcessor.class);

	private final ConfigurableEnvironment environment;

	private final ResourceLoader resourceLoader;

	private final List<String> propertySourceNames;

	public PropertySourceProcessor(ConfigurableEnvironment environment, ResourceLoader resourceLoader) {
		this.environment = environment;
		this.resourceLoader = resourceLoader;
		this.propertySourceNames = new ArrayList<>();
	}

	/**
	 * Process the specified {@link PropertySourceDescriptor} against the
	 * environment managed by this instance.
	 * @param descriptor the descriptor to process
	 * @throws IOException if loading the properties failed
	 */
	public void processPropertySource(PropertySourceDescriptor descriptor) throws IOException {
		String name = descriptor.name();
		String encoding = descriptor.encoding();
		List<String> locations = descriptor.locations();
		Assert.isTrue(locations.size() > 0, "At least one @PropertySource(value) location is required");
		boolean ignoreResourceNotFound = descriptor.ignoreResourceNotFound();
		PropertySourceFactory factory = (descriptor.propertySourceFactory() != null ?
				instantiateClass(descriptor.propertySourceFactory()) : DEFAULT_PROPERTY_SOURCE_FACTORY);

		for (String location : locations) {
			try {
				String resolvedLocation = this.environment.resolveRequiredPlaceholders(location);
				Resource resource = this.resourceLoader.getResource(resolvedLocation);
				addPropertySource(factory.createPropertySource(name, new EncodedResource(resource, encoding)));
			}
			catch (IllegalArgumentException | FileNotFoundException | UnknownHostException | SocketException ex) {
				// Placeholders not resolvable or resource not found when trying to open it
				if (ignoreResourceNotFound) {
					if (logger.isInfoEnabled()) {
						logger.info("Properties location [" + location + "] not resolvable: " + ex.getMessage());
					}
				}
				else {
					throw ex;
				}
			}
		}
	}

	private void addPropertySource(org.springframework.core.env.PropertySource<?> propertySource) {
		String name = propertySource.getName();
		MutablePropertySources propertySources = this.environment.getPropertySources();

		if (this.propertySourceNames.contains(name)) {
			// We've already added a version, we need to extend it
			org.springframework.core.env.PropertySource<?> existing = propertySources.get(name);
			if (existing != null) {
				PropertySource<?> newSource = (propertySource instanceof ResourcePropertySource rps ?
						rps.withResourceName() : propertySource);
				if (existing instanceof CompositePropertySource cps) {
					cps.addFirstPropertySource(newSource);
				}
				else {
					if (existing instanceof ResourcePropertySource rps) {
						existing = rps.withResourceName();
					}
					CompositePropertySource composite = new CompositePropertySource(name);
					composite.addPropertySource(newSource);
					composite.addPropertySource(existing);
					propertySources.replace(name, composite);
				}
				return;
			}
		}

		if (this.propertySourceNames.isEmpty()) {
			propertySources.addLast(propertySource);
		}
		else {
			String firstProcessed = this.propertySourceNames.get(this.propertySourceNames.size() - 1);
			propertySources.addBefore(firstProcessed, propertySource);
		}
		this.propertySourceNames.add(name);
	}

	private PropertySourceFactory instantiateClass(Class<? extends PropertySourceFactory> type) {
		try {
			Constructor<? extends PropertySourceFactory> constructor = type.getDeclaredConstructor();
			ReflectionUtils.makeAccessible(constructor);
			return constructor.newInstance();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to instantiate " + type, ex);
		}
	}

}
