/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans;

import org.springframework.beans.factory.InitializingBean;

/**
 * Simple test of BeanFactory initialization
 * @author Rod Johnson
 * @since 12.03.2003
 */
public class MustBeInitialized implements InitializingBean {

	private boolean inited;

	/**
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		this.inited = true;
	}

	/**
	 * Dummy business method that will fail unless the factory
	 * managed the bean's lifecycle correctly
	 */
	public void businessMethod() {
		if (!this.inited) {
			throw new RuntimeException("Factory didn't call afterPropertiesSet() on MustBeInitialized object");
		}
	}

}
