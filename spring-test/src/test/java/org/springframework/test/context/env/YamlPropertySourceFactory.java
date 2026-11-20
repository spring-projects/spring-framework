/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.env;

import java.util.Properties;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.util.StringUtils;

/**
 * Demo {@link PropertySourceFactory} that provides YAML support.
 *
 * @author Sam Brannen
 * @since 6.1
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

	@Override
	public PropertySource<?> createPropertySource(String name, EncodedResource encodedResource) {
		Resource resource = encodedResource.getResource();
		if (!StringUtils.hasText(name)) {
			name = getNameForResource(resource);
		}
		YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
		factoryBean.setResources(resource);
		factoryBean.afterPropertiesSet();
		Properties properties = factoryBean.getObject();
		return new PropertiesPropertySource(name, properties);
	}

	/**
	 * Return the description for the given Resource; if the description is
	 * empty, return the class name of the resource plus its identity hash code.
	 */
	private static String getNameForResource(Resource resource) {
		String name = resource.getDescription();
		if (!StringUtils.hasText(name)) {
			name = resource.getClass().getSimpleName() + "@" + System.identityHashCode(resource);
		}
		return name;
	}

}
