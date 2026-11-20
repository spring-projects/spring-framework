/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.StringUtils;

/**
 * {@link SmartInstantiationAwareBeanPostProcessor} implementation that wraps
 * beans in order to support the {@link BeanOverrideStrategy#WRAP WRAP} bean
 * override strategy.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @since 6.2
 */
class WrapEarlyBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor, PriorityOrdered {

	private final Map<String, Object> earlyReferences = new ConcurrentHashMap<>(16);

	private final BeanOverrideRegistry beanOverrideRegistry;

	WrapEarlyBeanPostProcessor(BeanOverrideRegistry beanOverrideRegistry) {
		this.beanOverrideRegistry = beanOverrideRegistry;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
		if (bean instanceof FactoryBean) {
			return bean;
		}
		this.earlyReferences.put(getCacheKey(bean, beanName), bean);
		return this.beanOverrideRegistry.wrapBeanIfNecessary(bean, beanName);
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof FactoryBean) {
			return bean;
		}
		if (this.earlyReferences.remove(getCacheKey(bean, beanName)) != bean) {
			return this.beanOverrideRegistry.wrapBeanIfNecessary(bean, beanName);
		}
		return bean;
	}

	private String getCacheKey(Object bean, String beanName) {
		return (StringUtils.hasLength(beanName) ? beanName : bean.getClass().getName());
	}

}
