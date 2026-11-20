/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.support.PropertySourceDescriptor;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.core.io.support.PropertySourceProcessor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Registry of {@link PropertySource} processed on configuration classes.
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @see PropertySourceDescriptor
 */
class PropertySourceRegistry {

	private final PropertySourceProcessor propertySourceProcessor;

	private final List<PropertySourceDescriptor> descriptors;


	public PropertySourceRegistry(PropertySourceProcessor propertySourceProcessor) {
		this.propertySourceProcessor = propertySourceProcessor;
		this.descriptors = new ArrayList<>();
	}


	/**
	 * Process the given <code>@PropertySource</code> annotation metadata.
	 * @param propertySource metadata for the <code>@PropertySource</code> annotation found
	 * @throws IOException if loading a property source failed
	 */
	void processPropertySource(AnnotationAttributes propertySource) throws IOException {
		String name = propertySource.getString("name");
		if (!StringUtils.hasLength(name)) {
			name = null;
		}
		String encoding = propertySource.getString("encoding");
		if (!StringUtils.hasLength(encoding)) {
			encoding = null;
		}
		String[] locations = propertySource.getStringArray("value");
		Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");
		boolean ignoreResourceNotFound = propertySource.getBoolean("ignoreResourceNotFound");

		Class<? extends PropertySourceFactory> factoryClass = propertySource.getClass("factory");
		Class<? extends PropertySourceFactory> factoryClassToUse =
				(factoryClass != PropertySourceFactory.class ? factoryClass : null);
		PropertySourceDescriptor descriptor = new PropertySourceDescriptor(Arrays.asList(locations),
				ignoreResourceNotFound, name, factoryClassToUse, encoding);
		this.propertySourceProcessor.processPropertySource(descriptor);
		this.descriptors.add(descriptor);
	}

	public List<PropertySourceDescriptor> getDescriptors() {
		return Collections.unmodifiableList(this.descriptors);
	}

}
