/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.testfixture.beans.LifecycleBean;

/**
 * Simple bean to test ApplicationContext lifecycle methods for beans
 *
 * @author Colin Sampaleanu
 * @since 03.07.2004
 */
public class LifecycleContextBean extends LifecycleBean implements ApplicationContextAware {

	protected ApplicationContext owningContext;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		if (this.owningContext != null) {
			throw new RuntimeException("Factory called setBeanFactory after setApplicationContext");
		}
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (this.owningContext == null) {
			throw new RuntimeException("Factory didn't call setApplicationContext before afterPropertiesSet on lifecycle bean");
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (this.owningFactory == null) {
			throw new RuntimeException("Factory called setApplicationContext before setBeanFactory");
		}

		this.owningContext = applicationContext;
	}

}
