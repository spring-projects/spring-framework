/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.messaging.core;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;


/**
 * @author Mark Fisher
 * @since 4.0
 */
public class BeanFactoryMessageChannelDestinationResolver implements DestinationResolver<MessageChannel>, BeanFactoryAware {

	private volatile BeanFactory beanFactory;

	/**
	 * Create a new instance of the {@link
	 * BeanFactoryMessageChannelDestinationResolver} class.
	 * <p>The BeanFactory to access must be set via <code>setBeanFactory</code>.
	 * This will happen automatically if this resolver is defined within an
	 * ApplicationContext thereby receiving the callback upon initialization.
	 *
	 * @see #setBeanFactory
	 */
	public BeanFactoryMessageChannelDestinationResolver() {

	}

	/**
	 * Create a new instance of the {@link
	 * BeanFactoryMessageChannelDestinationResolver} class.
	 * <p>Use of this constructor is redundant if this object is being created
	 * by a Spring IoC container as the supplied {@link BeanFactory} will be
	 * replaced by the {@link BeanFactory} that creates it (c.f. the
	 * {@link BeanFactoryAware} contract). So only use this constructor if you
	 * are instantiating this object explicitly rather than defining a bean.
	 *
	 * @param beanFactory the bean factory to be used to lookup {@link MessageChannel}s.
	 */
	public BeanFactoryMessageChannelDestinationResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "beanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	@Override
	public MessageChannel resolveDestination(String name) {
		Assert.state(this.beanFactory != null, "BeanFactory is required");
		try {
			return this.beanFactory.getBean(name, MessageChannel.class);
		}
		catch (BeansException e) {
			throw new DestinationResolutionException(
					"failed to look up MessageChannel bean with name '" + name + "'", e);
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
