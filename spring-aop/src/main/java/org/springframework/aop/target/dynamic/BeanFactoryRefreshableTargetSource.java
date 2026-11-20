/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.target.dynamic;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.Assert;

/**
 * Refreshable TargetSource that fetches fresh target beans from a BeanFactory.
 *
 * <p>Can be subclassed to override {@code requiresRefresh()} to suppress
 * unnecessary refreshes. By default, a refresh will be performed every time
 * the "refreshCheckDelay" has elapsed.
 *
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.0
 * @see org.springframework.beans.factory.BeanFactory
 * @see #requiresRefresh()
 * @see #setRefreshCheckDelay
 */
public class BeanFactoryRefreshableTargetSource extends AbstractRefreshableTargetSource {

	private final BeanFactory beanFactory;

	private final String beanName;


	/**
	 * Create a new BeanFactoryRefreshableTargetSource for the given
	 * bean factory and bean name.
	 * <p>Note that the passed-in BeanFactory should have an appropriate
	 * bean definition set up for the given bean name.
	 * @param beanFactory the BeanFactory to fetch beans from
	 * @param beanName the name of the target bean
	 */
	public BeanFactoryRefreshableTargetSource(BeanFactory beanFactory, String beanName) {
		Assert.notNull(beanFactory, "BeanFactory is required");
		Assert.notNull(beanName, "Bean name is required");
		this.beanFactory = beanFactory;
		this.beanName = beanName;
	}


	/**
	 * Retrieve a fresh target object.
	 */
	@Override
	protected final Object freshTarget() {
		return obtainFreshBean(this.beanFactory, this.beanName);
	}

	/**
	 * A template method that subclasses may override to provide a
	 * fresh target object for the given bean factory and bean name.
	 * <p>This default implementation fetches a new target bean
	 * instance from the bean factory.
	 * @see org.springframework.beans.factory.BeanFactory#getBean
	 */
	protected Object obtainFreshBean(BeanFactory beanFactory, String beanName) {
		return beanFactory.getBean(beanName);
	}

}
