/*
 * Copyright 2002-2008 the original author or authors.
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

package test.beans;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Simple test of BeanFactory initialization and lifecycle callbacks.
 *
 * @author Rod Johnson
 * @author Colin Sampaleanu
 * @author Chris Beams
 * @since 12.03.2003
 */
public class LifecycleBean implements BeanNameAware, BeanFactoryAware, InitializingBean, DisposableBean {

	protected boolean initMethodDeclared = false;

	protected String beanName;

	protected BeanFactory owningFactory;

	protected boolean postProcessedBeforeInit;

	protected boolean inited;

	protected boolean initedViaDeclaredInitMethod;

	protected boolean postProcessedAfterInit;

	protected boolean destroyed;


	public void setInitMethodDeclared(boolean initMethodDeclared) {
		this.initMethodDeclared = initMethodDeclared;
	}

	public boolean isInitMethodDeclared() {
		return initMethodDeclared;
	}

	public void setBeanName(String name) {
		this.beanName = name;
	}

	public String getBeanName() {
		return beanName;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.owningFactory = beanFactory;
	}

	public void postProcessBeforeInit() {
		if (this.inited || this.initedViaDeclaredInitMethod) {
			throw new RuntimeException("Factory called postProcessBeforeInit after afterPropertiesSet");
		}
		if (this.postProcessedBeforeInit) {
			throw new RuntimeException("Factory called postProcessBeforeInit twice");
		}
		this.postProcessedBeforeInit = true;
	}

	public void afterPropertiesSet() {
		if (this.owningFactory == null) {
			throw new RuntimeException("Factory didn't call setBeanFactory before afterPropertiesSet on lifecycle bean");
		}
		if (!this.postProcessedBeforeInit) {
			throw new RuntimeException("Factory didn't call postProcessBeforeInit before afterPropertiesSet on lifecycle bean");
		}
		if (this.initedViaDeclaredInitMethod) {
			throw new RuntimeException("Factory initialized via declared init method before initializing via afterPropertiesSet");
		}
		if (this.inited) {
			throw new RuntimeException("Factory called afterPropertiesSet twice");
		}
		this.inited = true;
	}

	public void declaredInitMethod() {
		if (!this.inited) {
			throw new RuntimeException("Factory didn't call afterPropertiesSet before declared init method");
		}

		if (this.initedViaDeclaredInitMethod) {
			throw new RuntimeException("Factory called declared init method twice");
		}
		this.initedViaDeclaredInitMethod = true;
	}

	public void postProcessAfterInit() {
		if (!this.inited) {
			throw new RuntimeException("Factory called postProcessAfterInit before afterPropertiesSet");
		}
		if (this.initMethodDeclared && !this.initedViaDeclaredInitMethod) {
			throw new RuntimeException("Factory called postProcessAfterInit before calling declared init method");
		}
		if (this.postProcessedAfterInit) {
			throw new RuntimeException("Factory called postProcessAfterInit twice");
		}
		this.postProcessedAfterInit = true;
	}

	/**
	 * Dummy business method that will fail unless the factory
	 * managed the bean's lifecycle correctly
	 */
	public void businessMethod() {
		if (!this.inited || (this.initMethodDeclared && !this.initedViaDeclaredInitMethod) ||
				!this.postProcessedAfterInit) {
			throw new RuntimeException("Factory didn't initialize lifecycle object correctly");
		}
	}

	public void destroy() {
		if (this.destroyed) {
			throw new IllegalStateException("Already destroyed");
		}
		this.destroyed = true;
	}

	public boolean isDestroyed() {
		return destroyed;
	}


	public static class PostProcessor implements BeanPostProcessor {

		public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
			if (bean instanceof LifecycleBean) {
				((LifecycleBean) bean).postProcessBeforeInit();
			}
			return bean;
		}

		public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
			if (bean instanceof LifecycleBean) {
				((LifecycleBean) bean).postProcessAfterInit();
			}
			return bean;
		}
	}

}
