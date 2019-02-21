/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.tests.sample.beans;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Simple factory to allow testing of FactoryBean support in AbstractBeanFactory.
 * Depending on whether its singleton property is set, it will return a singleton
 * or a prototype instance.
 *
 * <p>Implements InitializingBean interface, so we can check that
 * factories get this lifecycle callback if they want.
 *
 * @author Rod Johnson
 * @author Chris Beams
 * @since 10.03.2003
 */
public class DummyFactory
		implements FactoryBean<Object>, BeanNameAware, BeanFactoryAware, InitializingBean, DisposableBean {

	public static final String SINGLETON_NAME = "Factory singleton";

	private static boolean prototypeCreated;

	/**
	 * Clear static state.
	 */
	public static void reset() {
		prototypeCreated = false;
	}


	/**
	 * Default is for factories to return a singleton instance.
	 */
	private boolean singleton = true;

	private String beanName;

	private AutowireCapableBeanFactory beanFactory;

	private boolean postProcessed;

	private boolean initialized;

	private TestBean testBean;

	private TestBean otherTestBean;


	public DummyFactory() {
		this.testBean = new TestBean();
		this.testBean.setName(SINGLETON_NAME);
		this.testBean.setAge(25);
	}

	/**
	 * Return if the bean managed by this factory is a singleton.
	 * @see FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	/**
	 * Set if the bean managed by this factory is a singleton.
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public String getBeanName() {
		return beanName;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = (AutowireCapableBeanFactory) beanFactory;
		this.beanFactory.applyBeanPostProcessorsBeforeInitialization(this.testBean, this.beanName);
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setPostProcessed(boolean postProcessed) {
		this.postProcessed = postProcessed;
	}

	public boolean isPostProcessed() {
		return postProcessed;
	}

	public void setOtherTestBean(TestBean otherTestBean) {
		this.otherTestBean = otherTestBean;
		this.testBean.setSpouse(otherTestBean);
	}

	public TestBean getOtherTestBean() {
		return otherTestBean;
	}

	@Override
	public void afterPropertiesSet() {
		if (initialized) {
			throw new RuntimeException("Cannot call afterPropertiesSet twice on the one bean");
		}
		this.initialized = true;
	}

	/**
	 * Was this initialized by invocation of the
	 * afterPropertiesSet() method from the InitializingBean interface?
	 */
	public boolean wasInitialized() {
		return initialized;
	}

	public static boolean wasPrototypeCreated() {
		return prototypeCreated;
	}


	/**
	 * Return the managed object, supporting both singleton
	 * and prototype mode.
	 * @see FactoryBean#getObject()
	 */
	@Override
	public Object getObject() throws BeansException {
		if (isSingleton()) {
			return this.testBean;
		}
		else {
			TestBean prototype = new TestBean("prototype created at " + System.currentTimeMillis(), 11);
			if (this.beanFactory != null) {
				this.beanFactory.applyBeanPostProcessorsBeforeInitialization(prototype, this.beanName);
			}
			prototypeCreated = true;
			return prototype;
		}
	}

	@Override
	public Class<?> getObjectType() {
		return TestBean.class;
	}


	@Override
	public void destroy() {
		if (this.testBean != null) {
			this.testBean.setName(null);
		}
	}

}
