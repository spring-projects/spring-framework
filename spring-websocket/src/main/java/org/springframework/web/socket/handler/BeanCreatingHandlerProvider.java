/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.handler;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;

/**
 * Instantiates a target handler through a Spring {@link BeanFactory} and also provides
 * an equivalent destroy method. Mainly for internal use to assist with initializing and
 * destroying handlers with per-connection lifecycle.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <T> the handler type
 */
public class BeanCreatingHandlerProvider<T> implements BeanFactoryAware {

	private final Class<? extends T> handlerType;

	private @Nullable AutowireCapableBeanFactory beanFactory;


	public BeanCreatingHandlerProvider(Class<? extends T> handlerType) {
		Assert.notNull(handlerType, "handlerType must not be null");
		this.handlerType = handlerType;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof AutowireCapableBeanFactory autowireCapableBeanFactory) {
			this.beanFactory = autowireCapableBeanFactory;
		}
	}

	public void destroy(T handler) {
		if (this.beanFactory != null) {
			this.beanFactory.destroyBean(handler);
		}
	}


	public Class<? extends T> getHandlerType() {
		return this.handlerType;
	}

	public T getHandler() {
		if (this.beanFactory != null) {
			return this.beanFactory.createBean(this.handlerType);
		}
		else {
			return BeanUtils.instantiateClass(this.handlerType);
		}
	}

	@Override
	public String toString() {
		return "BeanCreatingHandlerProvider[handlerType=" + this.handlerType + "]";
	}

}
