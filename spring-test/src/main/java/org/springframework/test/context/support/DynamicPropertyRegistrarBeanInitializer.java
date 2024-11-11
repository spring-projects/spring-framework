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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactoryInitializer;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.lang.Nullable;
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


	@Nullable
	private ConfigurableEnvironment environment;


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
