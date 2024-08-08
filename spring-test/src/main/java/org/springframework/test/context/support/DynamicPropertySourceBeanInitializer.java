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
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Internal component which eagerly initializes beans created by {@code @Bean}
 * factory methods annotated with {@link DynamicPropertySource @DynamicPropertySource}.
 *
 * @author Sam Brannen
 * @since 6.2
 */
class DynamicPropertySourceBeanInitializer implements BeanFactoryInitializer<ListableBeanFactory> {

	private static final Log logger = LogFactory.getLog(DynamicPropertySourceBeanInitializer.class);


	@Override
	public void initialize(ListableBeanFactory beanFactory) {
		for (String name : beanFactory.getBeanNamesForAnnotation(DynamicPropertySource.class)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Eagerly initializing @DynamicPropertySource bean '%s'".formatted(name));
			}
			beanFactory.getBean(name);
		}
	}

}
