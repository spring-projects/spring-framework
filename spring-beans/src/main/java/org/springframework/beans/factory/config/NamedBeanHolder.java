/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.beans.factory.NamedBean;

/**
 * A simple holder for a given bean name plus bean instance.
 *
 * @author Juergen Hoeller
 * @since 4.3.3
 * @see AutowireCapableBeanFactory#resolveNamedBean(Class)
 */
public class NamedBeanHolder<T> implements NamedBean {

	private final String beanName;

	private final T beanInstance;


	/**
	 * Create a new holder for the given bean name plus instance.
	 */
	public NamedBeanHolder(String beanName, T beanInstance) {
		this.beanName = beanName;
		this.beanInstance = beanInstance;
	}


	@Override
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Return the corresponding bean instance.
	 */
	public T getBeanInstance() {
		return this.beanInstance;
	}

}
