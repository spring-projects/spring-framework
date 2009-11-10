/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.context.support;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.context.LifecycleProcessor;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 3.0
 */
public class DefaultLifecycleProcessor implements LifecycleProcessor, BeanFactoryAware {

	private volatile boolean running;

	private volatile ConfigurableListableBeanFactory beanFactory;


	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isTrue(beanFactory instanceof ConfigurableListableBeanFactory,
				"A ConfigurableListableBeanFactory is required.");
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	/*
	 * Lifecycle implementation
	 */

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		for (String beanName : new LinkedHashSet<String>(lifecycleBeans.keySet())) {
			doStart(lifecycleBeans, beanName);
		}
		this.running = true;
	}

	public void stop() {
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		for (String beanName : new LinkedHashSet<String>(lifecycleBeans.keySet())) {
			doStop(lifecycleBeans, beanName);
		}
		this.running = false;
	}

	/**
	 * Start the specified bean as part of the given set of Lifecycle beans,
	 * making sure that any beans that it depends on are started first.
	 * @param lifecycleBeans Map with bean name as key and Lifecycle instance as value
	 * @param beanName the name of the bean to start
	 */
	private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName) {
		Lifecycle bean = lifecycleBeans.get(beanName);
		if (bean != null && !this.equals(bean)) {
			String[] dependenciesForBean = this.beanFactory.getDependenciesForBean(beanName);
			for (String dependency : dependenciesForBean) {
				doStart(lifecycleBeans, dependency);
			}
			if (!bean.isRunning()) {
				bean.start();
			}
			lifecycleBeans.remove(beanName);
		}
	}

	/**
	 * Stop the specified bean as part of the given set of Lifecycle beans,
	 * making sure that any beans that depends on it are stopped first.
	 * @param lifecycleBeans Map with bean name as key and Lifecycle instance as value
	 * @param beanName the name of the bean to stop
	 */
	private void doStop(Map<String, Lifecycle> lifecycleBeans, String beanName) {
		Lifecycle bean = lifecycleBeans.get(beanName);
		if (bean != null && !this.equals(bean)) {
			String[] dependentBeans = this.beanFactory.getDependentBeans(beanName);
			for (String dependentBean : dependentBeans) {
				doStop(lifecycleBeans, dependentBean);
			}
			if (bean.isRunning()) {
				bean.stop();
			}
			lifecycleBeans.remove(beanName);
		}
	}

	private Map<String, Lifecycle> getLifecycleBeans() {
		String[] beanNames = beanFactory.getSingletonNames();
		Map<String, Lifecycle> beans = new LinkedHashMap<String, Lifecycle>();
		for (String beanName : beanNames) {
			Object bean = beanFactory.getSingleton(beanName);
			if (bean instanceof Lifecycle) {
				beans.put(beanName, (Lifecycle) bean);
			}
		}
		return beans;
	}

}
