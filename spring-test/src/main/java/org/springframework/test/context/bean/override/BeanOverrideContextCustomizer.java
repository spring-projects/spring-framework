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

package org.springframework.test.context.bean.override;

import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} implementation that registers the necessary
 * infrastructure to support {@linkplain BeanOverride bean overriding}.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
class BeanOverrideContextCustomizer implements ContextCustomizer {

	static final String REGISTRAR_BEAN_NAME =
			"org.springframework.test.context.bean.override.internalBeanOverrideRegistrar";

	private static final String INFRASTRUCTURE_BEAN_NAME =
			"org.springframework.test.context.bean.override.internalBeanOverridePostProcessor";

	private static final String EARLY_INFRASTRUCTURE_BEAN_NAME =
			"org.springframework.test.context.bean.override.internalWrapEarlyBeanPostProcessor";


	private final Set<OverrideMetadata> metadata;

	BeanOverrideContextCustomizer(Set<OverrideMetadata> metadata) {
		this.metadata = metadata;
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		ConfigurableBeanFactory beanFactory = context.getBeanFactory();
		// Since all three Bean Override infrastructure beans are never injected as
		// dependencies into other beans within the ApplicationContext, it is sufficient
		// to register them as manual singleton instances. In addition, registration of
		// the BeanOverrideBeanFactoryPostProcessor as a singleton is a requirement for
		// AOT processing, since a bean definition cannot be generated for the
		// Set<OverrideMetadata> argument that it accepts in its constructor.
		BeanOverrideRegistrar beanOverrideRegistrar = new BeanOverrideRegistrar(beanFactory);
		beanFactory.registerSingleton(REGISTRAR_BEAN_NAME, beanOverrideRegistrar);
		beanFactory.registerSingleton(INFRASTRUCTURE_BEAN_NAME,
				new BeanOverrideBeanFactoryPostProcessor(this.metadata, beanOverrideRegistrar));
		beanFactory.registerSingleton(EARLY_INFRASTRUCTURE_BEAN_NAME,
				new WrapEarlyBeanPostProcessor(beanOverrideRegistrar));
	}

	Set<OverrideMetadata> getMetadata() {
		return this.metadata;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		BeanOverrideContextCustomizer that = (BeanOverrideContextCustomizer) other;
		return this.metadata.equals(that.metadata);
	}

	@Override
	public int hashCode() {
		return this.metadata.hashCode();
	}

}
