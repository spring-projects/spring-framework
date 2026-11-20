/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.easymock;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * {@code TestExecutionListener} that provides support for resetting mocks
 * created via {@link EasyMockBean @EasyMockBean}.
 *
 * @author Sam Brannen
 * @since 6.2
 */
class EasyMockResetTestExecutionListener extends AbstractTestExecutionListener {

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 100;
	}

	@Override
	public void beforeTestMethod(TestContext testContext) throws Exception {
		resetMocks(testContext.getApplicationContext());
	}

	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		resetMocks(testContext.getApplicationContext());
	}

	private void resetMocks(ApplicationContext applicationContext) {
		if (applicationContext instanceof ConfigurableApplicationContext configurableContext) {
			resetMocks(configurableContext);
		}
	}

	private void resetMocks(ConfigurableApplicationContext applicationContext) {
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		try {
			beanFactory.getBean(EasyMockBeans.class).resetAll();
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Continue
		}
		if (applicationContext.getParent() != null) {
			resetMocks(applicationContext.getParent());
		}
	}

}
