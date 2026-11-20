/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override;

import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * {@link ContextCustomizer} implementation that registers the necessary
 * infrastructure to support {@linkplain BeanOverride Bean Overrides}.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
class BeanOverrideContextCustomizer implements ContextCustomizer {

	private static final String INFRASTRUCTURE_BEAN_NAME =
			"org.springframework.test.context.bean.override.internalBeanOverridePostProcessor";

	private static final String EARLY_INFRASTRUCTURE_BEAN_NAME =
			"org.springframework.test.context.bean.override.internalWrapEarlyBeanPostProcessor";


	private final Set<BeanOverrideHandler> handlers;

	BeanOverrideContextCustomizer(Set<BeanOverrideHandler> handlers) {
		this.handlers = handlers;
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		ConfigurableBeanFactory beanFactory = context.getBeanFactory();
		// Since all three Bean Override infrastructure beans are never injected as
		// dependencies into other beans within the ApplicationContext, it is sufficient
		// to register them as manual singleton instances. In addition, registration of
		// the BeanOverrideBeanFactoryPostProcessor as a singleton is a requirement for
		// AOT processing, since a bean definition cannot be generated for the
		// Set<BeanOverrideHandler> argument that it accepts in its constructor.
		BeanOverrideRegistry beanOverrideRegistry = new BeanOverrideRegistry(beanFactory);
		beanFactory.registerSingleton(BeanOverrideRegistry.BEAN_NAME, beanOverrideRegistry);
		beanFactory.registerSingleton(INFRASTRUCTURE_BEAN_NAME,
				new BeanOverrideBeanFactoryPostProcessor(this.handlers, beanOverrideRegistry));
		beanFactory.registerSingleton(EARLY_INFRASTRUCTURE_BEAN_NAME,
				new WrapEarlyBeanPostProcessor(beanOverrideRegistry));
	}

	Set<BeanOverrideHandler> getBeanOverrideHandlers() {
		return this.handlers;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (other == this) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		BeanOverrideContextCustomizer that = (BeanOverrideContextCustomizer) other;
		return this.handlers.equals(that.handlers);
	}

	@Override
	public int hashCode() {
		return this.handlers.hashCode();
	}

}
