/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework.autoproxy;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.AbstractAdvisingBeanPostProcessor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Extension of {@link AbstractAdvisingBeanPostProcessor} which implements
 * {@link BeanFactoryAware}, adds exposure of the original target class for each
 * proxied bean ({@link AutoProxyUtils#ORIGINAL_TARGET_CLASS_ATTRIBUTE}),
 * and participates in an externally enforced target-class mode for any given bean
 * ({@link AutoProxyUtils#PRESERVE_TARGET_CLASS_ATTRIBUTE}).
 * This post-processor is therefore aligned with {@link AbstractAutoProxyCreator}.
 *
 * @author Juergen Hoeller
 * @since 4.2.3
 * @see AutoProxyUtils#shouldProxyTargetClass
 * @see AutoProxyUtils#determineTargetClass
 */
@SuppressWarnings("serial")
public abstract class AbstractBeanFactoryAwareAdvisingPostProcessor extends AbstractAdvisingBeanPostProcessor
		implements BeanFactoryAware {

	protected @Nullable ConfigurableListableBeanFactory beanFactory;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = (beanFactory instanceof ConfigurableListableBeanFactory clbf ? clbf : null);
		AutoProxyUtils.applyDefaultProxyConfig(this, beanFactory);
	}

	@Override
	protected ProxyFactory prepareProxyFactory(Object bean, String beanName) {
		if (this.beanFactory != null) {
			AutoProxyUtils.exposeTargetClass(this.beanFactory, beanName, bean.getClass());
		}

		ProxyFactory proxyFactory = super.prepareProxyFactory(bean, beanName);
		if (this.beanFactory != null) {
			if (AutoProxyUtils.shouldProxyTargetClass(this.beanFactory, beanName)) {
				proxyFactory.setProxyTargetClass(true);
			}
			else {
				Class<?>[] ifcs = AutoProxyUtils.determineExposedInterfaces(this.beanFactory, beanName);
				if (ifcs != null) {
					proxyFactory.setProxyTargetClass(false);
					for (Class<?> ifc : ifcs) {
						proxyFactory.addInterface(ifc);
					}
				}
			}
		}
		return proxyFactory;
	}

	@Override
	protected boolean isEligible(Object bean, String beanName) {
		return (!AutoProxyUtils.isOriginalInstance(beanName, bean.getClass()) &&
				super.isEligible(bean, beanName));
	}

}
