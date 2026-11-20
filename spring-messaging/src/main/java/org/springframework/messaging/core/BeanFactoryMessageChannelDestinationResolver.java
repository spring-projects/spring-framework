/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.core;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * An implementation of {@link DestinationResolver} that interprets a destination
 * name as the bean name of a {@link MessageChannel} and looks up the bean in
 * the configured {@link BeanFactory}.
 *
 * @author Mark Fisher
 * @since 4.0
 */
public class BeanFactoryMessageChannelDestinationResolver
		implements DestinationResolver<MessageChannel>, BeanFactoryAware {

	private @Nullable BeanFactory beanFactory;


	/**
	 * A default constructor that can be used when the resolver itself is configured
	 * as a Spring bean and will have the {@code BeanFactory} injected as a result
	 * of ing having implemented {@link BeanFactoryAware}.
	 */
	public BeanFactoryMessageChannelDestinationResolver() {
	}

	/**
	 * A constructor that accepts a {@link BeanFactory} useful if instantiating this
	 * resolver manually rather than having it defined as a Spring-managed bean.
	 * @param beanFactory the bean factory to perform lookups against
	 */
	public BeanFactoryMessageChannelDestinationResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "beanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public MessageChannel resolveDestination(String name) {
		Assert.state(this.beanFactory != null, "No BeanFactory configured");
		try {
			return this.beanFactory.getBean(name, MessageChannel.class);
		}
		catch (BeansException ex) {
			throw new DestinationResolutionException(
					"Failed to find MessageChannel bean with name '" + name + "'", ex);
		}
	}

}
