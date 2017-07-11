/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jms.support.destination;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link DestinationResolver} implementation based on a Spring {@link BeanFactory}.
 *
 * <p>Will lookup Spring managed beans identified by bean name,
 * expecting them to be of type {@code javax.jms.Destination}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.springframework.beans.factory.BeanFactory
 */
public class BeanFactoryDestinationResolver implements DestinationResolver, BeanFactoryAware {

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * Create a new instance of the {@link BeanFactoryDestinationResolver} class.
	 * <p>The BeanFactory to access must be set via {@code setBeanFactory}.
	 * @see #setBeanFactory
	 */
	public BeanFactoryDestinationResolver() {
	}

	/**
	 * Create a new instance of the {@link BeanFactoryDestinationResolver} class.
	 * <p>Use of this constructor is redundant if this object is being created
	 * by a Spring IoC container, as the supplied {@link BeanFactory} will be
	 * replaced by the {@link BeanFactory} that creates it (c.f. the
	 * {@link BeanFactoryAware} contract). So only use this constructor if you
	 * are using this class outside the context of a Spring IoC container.
	 * @param beanFactory the bean factory to be used to lookup {@link javax.jms.Destination Destinatiosn}
	 */
	public BeanFactoryDestinationResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory is required");
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public Destination resolveDestinationName(@Nullable Session session, String destinationName, boolean pubSubDomain)
			throws JMSException {

		Assert.state(this.beanFactory != null, "BeanFactory is required");
		try {
			return this.beanFactory.getBean(destinationName, Destination.class);
		}
		catch (BeansException ex) {
			throw new DestinationResolutionException(
					"Failed to look up Destinaton bean with name '" + destinationName + "'", ex);
		}
	}

}
