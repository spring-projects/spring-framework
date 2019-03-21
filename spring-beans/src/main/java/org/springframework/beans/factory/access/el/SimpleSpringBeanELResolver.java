/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.access.el;

import javax.el.ELContext;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.Assert;

/**
 * Simple concrete variant of {@link SpringBeanELResolver}, delegating
 * to a given {@link BeanFactory} that the resolver was constructed with.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public class SimpleSpringBeanELResolver extends SpringBeanELResolver {

	private final BeanFactory beanFactory;


	/**
	 * Create a new SimpleSpringBeanELResolver for the given BeanFactory.
	 * @param beanFactory the Spring BeanFactory to delegate to
	 */
	public SimpleSpringBeanELResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	@Override
	protected BeanFactory getBeanFactory(ELContext elContext) {
		return this.beanFactory;
	}

}
