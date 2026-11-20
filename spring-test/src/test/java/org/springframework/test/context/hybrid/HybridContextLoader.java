/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.hybrid;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.support.AbstractGenericContextLoader;
import org.springframework.util.Assert;

import static org.springframework.test.context.support.AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses;

/**
 * Hybrid {@link SmartContextLoader} that supports path-based and class-based
 * resources simultaneously.
 * <p>This test loader is inspired by Spring Boot.
 * <p>Detects defaults for XML configuration and annotated classes.
 * <p>Beans from XML configuration always override those from annotated classes.
 *
 * @author Sam Brannen
 * @since 4.0.4
 */
public class HybridContextLoader extends AbstractGenericContextLoader {

	@Override
	protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		Assert.isTrue(mergedConfig.hasResources(),
				() -> getClass().getSimpleName() + " requires either classes or locations");
	}

	@Override
	public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
		// Detect default XML configuration files:
		super.processContextConfiguration(configAttributes);

		// Detect default configuration classes:
		if (!configAttributes.hasClasses() && isGenerateDefaultLocations()) {
			configAttributes.setClasses(detectDefaultConfigurationClasses(configAttributes.getDeclaringClass()));
		}
	}

	@Override
	protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
		// Order doesn't matter: <bean> always wins over @Bean.
		new XmlBeanDefinitionReader(context).loadBeanDefinitions(mergedConfig.getLocations());
		new AnnotatedBeanDefinitionReader(context).register(mergedConfig.getClasses());
	}

	@Override
	protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " doesn't support this");
	}

	@Override
	protected String getResourceSuffix() {
		return "-context.xml";
	}

}
