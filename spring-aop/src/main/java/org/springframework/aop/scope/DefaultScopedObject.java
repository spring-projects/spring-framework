/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.scope;

import java.io.Serializable;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link ScopedObject} interface.
 *
 * <p>Simply delegates the calls to the underlying
 * {@link ConfigurableBeanFactory bean factory}
 * ({@link ConfigurableBeanFactory#getBean(String)}/
 * {@link ConfigurableBeanFactory#destroyScopedBean(String)}).
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.beans.factory.BeanFactory#getBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory#destroyScopedBean
 */
@SuppressWarnings("serial")
public class DefaultScopedObject implements ScopedObject, Serializable {

	private final ConfigurableBeanFactory beanFactory;

	private final String targetBeanName;


	/**
	 * Creates a new instance of the {@link DefaultScopedObject} class.
	 * @param beanFactory the {@link ConfigurableBeanFactory} that holds the scoped target object
	 * @param targetBeanName the name of the target bean
	 */
	public DefaultScopedObject(ConfigurableBeanFactory beanFactory, String targetBeanName) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.hasText(targetBeanName, "'targetBeanName' must not be empty");
		this.beanFactory = beanFactory;
		this.targetBeanName = targetBeanName;
	}


	@Override
	public Object getTargetObject() {
		return this.beanFactory.getBean(this.targetBeanName);
	}

	@Override
	public void removeFromScope() {
		this.beanFactory.destroyScopedBean(this.targetBeanName);
	}

}
