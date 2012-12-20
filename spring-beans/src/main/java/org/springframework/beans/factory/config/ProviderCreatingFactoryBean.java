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

package org.springframework.beans.factory.config;

import java.io.Serializable;
import javax.inject.Provider;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.beans.factory.FactoryBean} implementation that
 * returns a value which is a JSR-330 {@link javax.inject.Provider} that in turn
 * returns a bean sourced from a {@link org.springframework.beans.factory.BeanFactory}.
 *
 * <p>This is basically a JSR-330 compliant variant of Spring's good old
 * {@link ObjectFactoryCreatingFactoryBean}. It can be used for traditional
 * external dependency injection configuration that targets a property or
 * constructor argument of type <code>javax.inject.Provider</code>, as an
 * alternative to JSR-330's <code>@Inject</code> annotation-driven approach.
 *
 * @author Juergen Hoeller
 * @since 3.0.2
 * @see javax.inject.Provider
 * @see ObjectFactoryCreatingFactoryBean
 */
public class ProviderCreatingFactoryBean<T> extends AbstractFactoryBean<Provider<T>> {

	private String targetBeanName;


	/**
	 * Set the name of the target bean.
	 * <p>The target does not <i>have</i> to be a non-singleton bean, but realistically
	 * always will be (because if the target bean were a singleton, then said singleton
	 * bean could simply be injected straight into the dependent object, thus obviating
	 * the need for the extra level of indirection afforded by this factory approach).
	 */
	public void setTargetBeanName(String targetBeanName) {
		this.targetBeanName = targetBeanName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasText(this.targetBeanName, "Property 'targetBeanName' is required");
		super.afterPropertiesSet();
	}


	@Override
	public Class<?> getObjectType() {
		return Provider.class;
	}

	@Override
	protected Provider<T> createInstance() {
		return new TargetBeanProvider<T>(getBeanFactory(), this.targetBeanName);
	}


	/**
	 * Independent inner class - for serialization purposes.
	 */
	private static class TargetBeanProvider<T> implements Provider<T>, Serializable {

		private final BeanFactory beanFactory;

		private final String targetBeanName;

		public TargetBeanProvider(BeanFactory beanFactory, String targetBeanName) {
			this.beanFactory = beanFactory;
			this.targetBeanName = targetBeanName;
		}

		@SuppressWarnings("unchecked")
		public T get() throws BeansException {
			return (T) this.beanFactory.getBean(this.targetBeanName);
		}
	}

}
