/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

/**
 * Base class for {@link BeanPostProcessor} implementations that apply a
 * Spring AOP {@link Advisor} to specific beans.
 *
 * @author Juergen Hoeller
 * @since 3.2
 */
public abstract class AbstractAdvisingBeanPostProcessor extends ProxyConfig
		implements BeanPostProcessor, BeanClassLoaderAware, Ordered {

	protected Advisor advisor;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/**
	 * This should run after all other post-processors, so that it can just add
	 * an advisor to existing proxies rather than double-proxy.
	 */
	private int order = Ordered.LOWEST_PRECEDENCE;

	private final Map<String, Boolean> eligibleBeans = new ConcurrentHashMap<String, Boolean>(64);


	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return this.order;
	}


	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) {
		if (bean instanceof AopInfrastructureBean) {
			// Ignore AOP infrastructure such as scoped proxies.
			return bean;
		}
		if (isEligible(bean, beanName)) {
			if (bean instanceof Advised) {
				((Advised) bean).addAdvisor(0, this.advisor);
				return bean;
			}
			else {
				ProxyFactory proxyFactory = new ProxyFactory(bean);
				// Copy our properties (proxyTargetClass etc) inherited from ProxyConfig.
				proxyFactory.copyFrom(this);
				proxyFactory.addAdvisor(this.advisor);
				return proxyFactory.getProxy(this.beanClassLoader);
			}
		}
		else {
			// No async proxy needed.
			return bean;
		}
	}

	/**
	 * Check whether the given bean is eligible for advising with this
	 * post-processor's {@link Advisor}.
	 * <p>Implements caching of {@code canApply} results per bean name.
	 * @param bean the bean instance
	 * @param beanName the name of the bean
	 * @see AopUtils#canApply(Advisor, Class)
	 */
	protected boolean isEligible(Object bean, String beanName) {
		Boolean eligible = this.eligibleBeans.get(beanName);
		if (eligible != null) {
			return eligible;
		}
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		eligible = AopUtils.canApply(this.advisor, targetClass);
		this.eligibleBeans.put(beanName, eligible);
		return eligible;
	}

}
