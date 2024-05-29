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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.lang.Nullable;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Internal component which eagerly initializes beans created by {@code @Bean}
 * factory methods annotated with {@link DynamicPropertySource @DynamicPropertySource}.
 *
 * <p>This class implements {@link LoadTimeWeaverAware} since doing so is
 * currently the only way to have a component eagerly initialized before the
 * {@code ConfigurableListableBeanFactory.preInstantiateSingletons()} phase.
 *
 * @author Sam Brannen
 * @since 6.2
 */
class DynamicPropertySourceBeanInitializer implements BeanFactoryAware, InitializingBean, LoadTimeWeaverAware {

	private static final Log logger = LogFactory.getLog(DynamicPropertySourceBeanInitializer.class);

	@Nullable
	private BeanFactory beanFactory;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (!(this.beanFactory instanceof ListableBeanFactory lbf)) {
			throw new IllegalStateException("BeanFactory must be set and must be a ListableBeanFactory");
		}
		for (String name : lbf.getBeanNamesForAnnotation(DynamicPropertySource.class)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Eagerly initializing @DynamicPropertySource bean '%s'".formatted(name));
			}
			this.beanFactory.getBean(name);
		}
	}

	@Override
	public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
		// no-op
	}

}
