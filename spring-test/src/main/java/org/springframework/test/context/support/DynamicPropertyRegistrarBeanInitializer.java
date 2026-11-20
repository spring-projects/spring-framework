/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactoryInitializer;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * {@link BeanFactoryInitializer} that eagerly initializes {@link DynamicPropertyRegistrar}
 * beans.
 *
 * <p>Primarily intended for internal use within the Spring TestContext Framework.
 *
 * @author Sam Brannen
 * @since 6.2
 */
public class DynamicPropertyRegistrarBeanInitializer implements BeanFactoryInitializer<ListableBeanFactory>, EnvironmentAware {

	private static final Log logger = LogFactory.getLog(DynamicPropertyRegistrarBeanInitializer.class);

	/**
	 * The bean name of the internally managed {@code DynamicPropertyRegistrarBeanInitializer}.
	 */
	static final String BEAN_NAME =
			"org.springframework.test.context.support.internalDynamicPropertyRegistrarBeanInitializer";


	private @Nullable ConfigurableEnvironment environment;


	@Override
	public void setEnvironment(Environment environment) {
		if (!(environment instanceof ConfigurableEnvironment configurableEnvironment)) {
			throw new IllegalArgumentException("Environment must be a ConfigurableEnvironment");
		}
		this.environment = configurableEnvironment;
	}

	@Override
	public void initialize(ListableBeanFactory beanFactory) {
		if (this.environment == null) {
			throw new IllegalStateException("Environment is required");
		}
		String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
				beanFactory, DynamicPropertyRegistrar.class);
		if (beanNames.length > 0) {
			DynamicValuesPropertySource propertySource = DynamicValuesPropertySource.getOrCreate(this.environment);
			DynamicPropertyRegistry registry = propertySource.dynamicPropertyRegistry;
			for (String name : beanNames) {
				if (logger.isDebugEnabled()) {
					logger.debug("Eagerly initializing DynamicPropertyRegistrar bean '%s'".formatted(name));
				}
				DynamicPropertyRegistrar registrar = beanFactory.getBean(name, DynamicPropertyRegistrar.class);
				registrar.accept(registry);
			}
		}
	}

}
